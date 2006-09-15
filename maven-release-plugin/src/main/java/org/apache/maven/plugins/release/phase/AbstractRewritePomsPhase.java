package org.apache.maven.plugins.release.phase;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.plugins.release.ReleaseExecutionException;
import org.apache.maven.plugins.release.ReleaseFailureException;
import org.apache.maven.plugins.release.ReleaseResult;
import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.apache.maven.plugins.release.scm.ReleaseScmCommandException;
import org.apache.maven.plugins.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.plugins.release.scm.ScmRepositoryConfigurator;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.edit.EditScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.filter.ContentFilter;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Base class for rewriting phases.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractRewritePomsPhase
    extends AbstractReleasePhase
{
    /**
     * Tool that gets a configured SCM repository from release configuration.
     */
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    /**
     * The line separator to use.
     */
    private static final String LS = System.getProperty( "line.separator" );

    /**
     * Configuration item for the suffix to add to rewritten POMs when simulating.
     */
    private String pomSuffix;

    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        transform( releaseDescriptor, settings, reactorProjects, false, result );

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    private void transform( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects,
                            boolean simulate, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            logInfo( result, "Transforming '" + project.getName() + "'..." );

            transformProject( project, releaseDescriptor, settings, reactorProjects, simulate, result );
        }
    }

    private void transformProject( MavenProject project, ReleaseDescriptor releaseDescriptor, Settings settings,
                                   List reactorProjects, boolean simulate, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        Document document;
        String intro = null;
        String outtro = null;
        try
        {
            String content = FileUtils.fileRead( project.getFile() );
            // we need to eliminate any extra whitespace inside elements, as JDOM will nuke it
            content = content.replaceAll( "<([^!][^>]*?)\\s{2,}([^>]*?)>", "<$1 $2>" );
            content = content.replaceAll( "(\\s{2,}|[^\\s])/>", "$1 />" );

            SAXBuilder builder = new SAXBuilder();
            document = builder.build( new StringReader( content ) );

            // Normalise line endings. For some reason, JDOM replaces \r\n inside a comment with \n.
            normaliseLineEndings( document );

            // rewrite DOM as a string to find differences, since text outside the root element is not tracked
            StringWriter w = new StringWriter();
            Format format = Format.getRawFormat();
            format.setLineSeparator( LS );
            XMLOutputter out = new XMLOutputter( format );
            out.output( document.getRootElement(), w );

            int index = content.indexOf( w.toString() );
            if ( index >= 0 )
            {
                intro = content.substring( 0, index );
                outtro = content.substring( index + w.toString().length() );
            }
        }
        catch ( JDOMException e )
        {
            throw new ReleaseExecutionException( "Error reading POM: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new ReleaseExecutionException( "Error reading POM: " + e.getMessage(), e );
        }

        ScmRepository scmRepository;
        ScmProvider provider;
        try
        {
            scmRepository = scmRepositoryConfigurator.getConfiguredRepository( releaseDescriptor, settings );

            provider = scmRepositoryConfigurator.getRepositoryProvider( scmRepository );
        }
        catch ( ScmRepositoryException e )
        {
            throw new ReleaseScmRepositoryException( e.getMessage(), e.getValidationMessages() );
        }
        catch ( NoSuchScmProviderException e )
        {
            throw new ReleaseExecutionException( "Unable to configure SCM repository: " + e.getMessage(), e );
        }

        transformDocument( project, document.getRootElement(), releaseDescriptor,
                           reactorProjects, scmRepository, result );

        if ( simulate )
        {
            File outputFile =
                new File( project.getFile().getParentFile(), project.getFile().getName() + "." + pomSuffix );
            writePom( outputFile, document, releaseDescriptor, project.getModelVersion(), intro, outtro );
        }
        else
        {
            writePom( project.getFile(), document, releaseDescriptor, project.getModelVersion(), intro, outtro,
                      scmRepository, provider );
        }
    }

    private void normaliseLineEndings( Document document )
    {
        for ( Iterator i = document.getDescendants( new ContentFilter( ContentFilter.COMMENT ) ); i.hasNext(); )
        {
            Comment c = (Comment) i.next();
            c.setText( c.getText().replaceAll( "\n", LS ) );
        }
    }

    private void transformDocument( MavenProject project, Element rootElement, ReleaseDescriptor releaseDescriptor,
                                    List reactorProjects, ScmRepository scmRepository, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        Namespace namespace = rootElement.getNamespace();
        Map mappedVersions = getNextVersionMap( releaseDescriptor );
        Map originalVersions = getOriginalVersionMap( releaseDescriptor, reactorProjects );

        String parentVersion = rewriteParent( project, rootElement, namespace, mappedVersions, originalVersions );

        String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

        rewriteVersion( rootElement, namespace, mappedVersions, projectId, project, parentVersion );

        rewriteDependencies( project.getDependencies(), rootElement, mappedVersions, originalVersions,
                             projectId, result );

        if ( project.getDependencyManagement() != null )
        {
            Element dependencyRoot = rootElement.getChild( "dependencyManagement", namespace );
            if ( dependencyRoot != null )
            {
                rewriteDependencies( project.getDependencyManagement().getDependencies(), dependencyRoot,
                                     mappedVersions, originalVersions, projectId, result );
            }
        }

        if ( project.getBuild() != null )
        {
            Element buildRoot = rootElement.getChild( "build", namespace );
            if ( buildRoot != null )
            {
                rewritePlugins( project.getBuildPlugins(), buildRoot, mappedVersions,
                                originalVersions, projectId, result );
                if ( project.getPluginManagement() != null )
                {
                    Element pluginsRoot = buildRoot.getChild( "pluginManagement", namespace );
                    if ( pluginsRoot != null )
                    {
                        rewritePlugins( project.getPluginManagement().getPlugins(), pluginsRoot, mappedVersions,
                                        originalVersions, projectId, result );
                    }
                }
                rewriteExtensions( project.getBuildExtensions(), buildRoot, mappedVersions, originalVersions,
                                   projectId, result );
            }
        }

        if ( project.getReporting() != null )
        {
            Element pluginsRoot = rootElement.getChild( "reporting", namespace );
            if ( pluginsRoot != null )
            {
                rewriteReportPlugins( project.getReportPlugins(), pluginsRoot, mappedVersions, originalVersions,
                                      projectId, result );
            }
        }

        transformScm( project, rootElement, namespace, releaseDescriptor, projectId, scmRepository, result );
    }

    private void rewriteVersion( Element rootElement, Namespace namespace, Map mappedVersions, String projectId,
                                 MavenProject project, String parentVersion )
        throws ReleaseFailureException
    {
        Element versionElement = rootElement.getChild( "version", namespace );
        String version = (String) mappedVersions.get( projectId );
        if ( version == null )
        {
            throw new ReleaseFailureException( "Version for '" + project.getName() + "' was not mapped" );
        }

        if ( versionElement == null )
        {
            if ( !version.equals( parentVersion ) )
            {
                // we will add this after artifactId, since it was missing but different from the inherited version
                Element artifactIdElement = rootElement.getChild( "artifactId", namespace );
                int index = rootElement.indexOf( artifactIdElement );

                versionElement = new Element( "version", namespace );
                versionElement.setText( version );
                rootElement.addContent( index + 1, new Text( "\n  " ) );
                rootElement.addContent( index + 2, versionElement );
            }
        }
        else
        {
            versionElement.setText( version );
        }
    }

    private String rewriteParent( MavenProject project, Element rootElement, Namespace namespace, Map mappedVersions,
                                  Map originalVersions )
        throws ReleaseFailureException
    {
        String parentVersion = null;
        if ( project.hasParent() )
        {
            Element parentElement = rootElement.getChild( "parent", namespace );
            Element versionElement = parentElement.getChild( "version", namespace );
            MavenProject parent = project.getParent();
            String key = ArtifactUtils.versionlessKey( parent.getGroupId(), parent.getArtifactId() );
            parentVersion = (String) mappedVersions.get( key );
            if ( parentVersion == null )
            {
                if ( parent.getVersion().equals( originalVersions.get( key ) ) )
                {
                    throw new ReleaseFailureException( "Version for parent '" + parent.getName() + "' was not mapped" );
                }
            }
            else
            {
                versionElement.setText( parentVersion );
            }
        }
        return parentVersion;
    }

    private void rewriteDependencies( List dependencies, Element dependencyRoot, Map mappedVersions,
                                      Map originalVersions, String projectId, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        if ( dependencies != null )
        {
            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                Dependency dep = (Dependency) i.next();

                updateDomVersion( dep.getGroupId(), dep.getArtifactId(), mappedVersions, dep.getVersion(),
                                  originalVersions, "dependencies", "dependency", dependencyRoot, projectId, result );
            }
        }
    }

    private void rewritePlugins( List plugins, Element pluginRoot, Map mappedVersions, Map originalVersions,
                                 String projectId, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        if ( plugins != null )
        {
            for ( Iterator i = plugins.iterator(); i.hasNext(); )
            {
                Plugin plugin = (Plugin) i.next();

                // We can ignore plugins whose version is assumed, they are only written into the release pom
                if ( plugin.getVersion() != null )
                {
                    updateDomVersion( plugin.getGroupId(), plugin.getArtifactId(), mappedVersions, plugin.getVersion(),
                                      originalVersions, "plugins", "plugin", pluginRoot, projectId, result );
                }
            }
        }
    }

    private void rewriteExtensions( List extensions, Element extensionRoot, Map mappedVersions, Map originalVersions,
                                    String projectId, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        if ( extensions != null )
        {
            for ( Iterator i = extensions.iterator(); i.hasNext(); )
            {
                Extension extension = (Extension) i.next();

                updateDomVersion( extension.getGroupId(), extension.getArtifactId(), mappedVersions,
                                  extension.getVersion(), originalVersions, "extensions", "extension", extensionRoot,
                                  projectId, result );
            }
        }
    }

    private void rewriteReportPlugins( List plugins, Element pluginRoot, Map mappedVersions, Map originalVersions,
                                       String projectId, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        if ( plugins != null )
        {
            for ( Iterator i = plugins.iterator(); i.hasNext(); )
            {
                ReportPlugin plugin = (ReportPlugin) i.next();

                // We can ignore plugins whose version is assumed, they are only written into the release pom
                if ( plugin.getVersion() != null )
                {
                    updateDomVersion( plugin.getGroupId(), plugin.getArtifactId(), mappedVersions, plugin.getVersion(),
                                      originalVersions, "plugins", "plugin", pluginRoot, projectId, result );
                }
            }
        }
    }

    private void updateDomVersion( String groupId, String artifactId, Map mappedVersions, String version,
                                   Map originalVersions, String groupTagName, String tagName, Element dependencyRoot,
                                   String projectId, ReleaseResult result )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        String key = ArtifactUtils.versionlessKey( groupId, artifactId );
        String mappedVersion = (String) mappedVersions.get( key );

        Object originalVersion = originalVersions.get( key );
        if ( version.equals( originalVersion ) )
        {
            if ( mappedVersion != null )
            {
                logInfo( result, "Updating " + artifactId + " to " + mappedVersion );

                try
                {
                    XPath xpath;
                    if ( !StringUtils.isEmpty( dependencyRoot.getNamespaceURI() ) )
                    {
                        xpath = XPath.newInstance( "./pom:" + groupTagName + "/pom:" + tagName + "[pom:groupId='" +
                            groupId + "' and pom:artifactId='" + artifactId + "']" );
                        xpath.addNamespace( "pom", dependencyRoot.getNamespaceURI() );
                    }
                    else
                    {
                        xpath = XPath.newInstance( "./" + groupTagName + "/" + tagName + "[groupId='" + groupId +
                            "' and artifactId='" + artifactId + "']" );
                    }

                    Element dependency = (Element) xpath.selectSingleNode( dependencyRoot );
                    // If it was inherited, nothing to do
                    if ( dependency != null )
                    {
                        Element versionElement = dependency.getChild( "version", dependencyRoot.getNamespace() );

                        // avoid if in management
                        if ( versionElement != null )
                        {
                            // avoid if it was not originally set to the original value (it may be an expression), unless mapped version differs
                            if ( originalVersion.equals( versionElement.getTextTrim() ) ||
                                !mappedVersion.equals( mappedVersions.get( projectId ) ) )
                            {
                                versionElement.setText( mappedVersion );
                            }
                        }
                    }
                }
                catch ( JDOMException e )
                {
                    throw new ReleaseExecutionException( "Unable to locate " + tagName + " to process in document", e );
                }
            }
            else
            {
                throw new ReleaseFailureException(
                    "Version '" + version + "' for " + tagName + " '" + key + "' was not mapped" );
            }
        }
    }

    private void writePom( File pomFile, Document document, ReleaseDescriptor releaseDescriptor, String modelVersion,
                           String intro, String outtro, ScmRepository repository, ScmProvider provider )
        throws ReleaseExecutionException, ReleaseScmCommandException
    {
        try
        {
            if ( releaseDescriptor.isScmUseEditMode() || provider.requiresEditMode() )
            {
                EditScmResult result = provider.edit( repository, new ScmFileSet(
                    new File( releaseDescriptor.getWorkingDirectory() ), pomFile ) );

                if ( !result.isSuccess() )
                {
                    throw new ReleaseScmCommandException( "Unable to enable editing on the POM", result );
                }
            }
        }
        catch ( ScmException e )
        {
            throw new ReleaseExecutionException( "An error occurred enabling edit mode: " + e.getMessage(), e );
        }

        writePom( pomFile, document, releaseDescriptor, modelVersion, intro, outtro );
    }

    private void writePom( File pomFile, Document document, ReleaseDescriptor releaseDescriptor, String modelVersion,
                           String intro, String outtro )
        throws ReleaseExecutionException
    {
        Element rootElement = document.getRootElement();

        if ( releaseDescriptor.isAddSchema() )
        {
            Namespace pomNamespace = Namespace.getNamespace( "", "http://maven.apache.org/POM/" + modelVersion );
            rootElement.setNamespace( pomNamespace );
            Namespace xsiNamespace = Namespace.getNamespace( "xsi", "http://www.w3.org/2001/XMLSchema-instance" );
            rootElement.addNamespaceDeclaration( xsiNamespace );

            if ( rootElement.getAttribute( "schemaLocation", xsiNamespace ) == null )
            {
                rootElement.setAttribute( "schemaLocation", "http://maven.apache.org/POM/" + modelVersion +
                    " http://maven.apache.org/maven-v" + modelVersion.replace( '.', '_' ) + ".xsd", xsiNamespace );
            }

            // the empty namespace is considered equal to the POM namespace, so match them up to avoid extra xmlns=""
            ElementFilter elementFilter = new ElementFilter( Namespace.getNamespace( "" ) );
            for ( Iterator i = rootElement.getDescendants( elementFilter ); i.hasNext(); )
            {
                Element e = (Element) i.next();
                e.setNamespace( pomNamespace );
            }
        }

        Writer writer = null;
        try
        {
            // TODO: better handling of encoding. Currently the definition is not written out and is embedded in the intro if it already existed
            // TODO: the XMLOutputter and Writer need to have their encodings aligned.
            writer = new FileWriter( pomFile );

            if ( intro != null )
            {
                writer.write( intro );
            }

            Format format = Format.getRawFormat();
            format.setLineSeparator( LS );
            XMLOutputter out = new XMLOutputter( format );
            out.output( document.getRootElement(), writer );

            if ( outtro != null )
            {
                writer.write( outtro );
            }
        }
        catch ( IOException e )
        {
            throw new ReleaseExecutionException( "Error writing POM: " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException, ReleaseFailureException
    {
        ReleaseResult result = new ReleaseResult();

        transform( releaseDescriptor, settings, reactorProjects, true, result );

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    public ReleaseResult clean( List reactorProjects )
    {
        ReleaseResult result = new ReleaseResult();

        super.clean( reactorProjects );

        if ( reactorProjects != null )
        {
            for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
            {
                MavenProject project = (MavenProject) i.next();

                File file =
                    new File( project.getFile().getParentFile(), project.getFile().getName() + "." + pomSuffix );
                if ( file.exists() )
                {
                    file.delete();
                }
            }
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    protected abstract Map getOriginalVersionMap( ReleaseDescriptor releaseDescriptor, List reactorProjects );

    protected abstract Map getNextVersionMap( ReleaseDescriptor releaseDescriptor );

    protected abstract void transformScm( MavenProject project, Element rootElement, Namespace namespace,
                                          ReleaseDescriptor releaseDescriptor, String projectId,
                                          ScmRepository scmRepository, ReleaseResult result )
        throws ReleaseExecutionException;

    protected Element rewriteElement( String name, String value, Element root, Namespace namespace )
    {
        Element tagElement = root.getChild( name, namespace );
        if ( tagElement != null )
        {
            if ( value != null )
            {
                tagElement.setText( value );
            }
            else
            {
                int index = root.indexOf( tagElement );
                root.removeContent( index );
                for ( int i = index - 1; i >= 0; i-- )
                {
                    if ( root.getContent( i ) instanceof Text )
                    {
                        root.removeContent( i );
                    }
                    else
                    {
                        break;
                    }
                }
            }
        }
        else
        {
            if ( value != null )
            {
                Element element = new Element( name, namespace );
                element.setText( value );
                root.addContent( "  " ).addContent( element ).addContent( "\n  " );
                tagElement = element;
            }
        }
        return tagElement;
    }
}
