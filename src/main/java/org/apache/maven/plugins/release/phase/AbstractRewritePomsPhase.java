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
import org.apache.maven.plugins.release.config.ReleaseConfiguration;
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
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.Text;
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
    extends AbstractLogEnabled
    implements ReleasePhase
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

    public void execute( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        transform( releaseConfiguration, false );
    }

    private void transform( ReleaseConfiguration releaseConfiguration, boolean simulate )
        throws ReleaseExecutionException
    {
        for ( Iterator it = releaseConfiguration.getReactorProjects().iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            getLogger().info( "Transforming " + projectId + " to release" );

            transformProject( project, releaseConfiguration, simulate );
        }
    }

    private void transformProject( MavenProject project, ReleaseConfiguration releaseConfiguration, boolean simulate )
        throws ReleaseExecutionException
    {
        Document document;
        String intro = null;
        String outtro = null;
        try
        {
            String content = FileUtils.fileRead( project.getFile() );

            SAXBuilder builder = new SAXBuilder();
            document = builder.build( new StringReader( content ) );

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
            scmRepository = scmRepositoryConfigurator.getConfiguredRepository( releaseConfiguration );

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

        transformDocument( project, document.getRootElement(), releaseConfiguration, scmRepository );

        if ( simulate )
        {
            File outputFile =
                new File( project.getFile().getParentFile(), project.getFile().getName() + "." + pomSuffix );
            writePom( outputFile, document, releaseConfiguration, project.getModelVersion(), intro, outtro );
        }
        else
        {
            writePom( project.getFile(), document, releaseConfiguration, project.getModelVersion(), intro, outtro,
                      scmRepository, provider );
        }
    }

    private void transformDocument( MavenProject project, Element rootElement,
                                    ReleaseConfiguration releaseConfiguration, ScmRepository scmRepository )
        throws ReleaseExecutionException
    {
        Namespace namespace = rootElement.getNamespace();
        Map mappedVersions = getNextVersionMap( releaseConfiguration );
        Map originalVersions = getOriginalVersionMap( releaseConfiguration );

        String parentVersion = rewriteParent( project, rootElement, namespace, mappedVersions, originalVersions );

        String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

        rewriteVersion( rootElement, namespace, mappedVersions, projectId, project, parentVersion );

        rewriteDependencies( project.getDependencies(), rootElement, mappedVersions, originalVersions );

        if ( project.getDependencyManagement() != null )
        {
            Element dependencyRoot = rootElement.getChild( "dependencyManagement", namespace );
            if ( dependencyRoot != null )
            {
                rewriteDependencies( project.getDependencyManagement().getDependencies(), dependencyRoot,
                                     mappedVersions, originalVersions );
            }
        }

        if ( project.getBuild() != null )
        {
            Element pluginsRoot = rootElement.getChild( "build", namespace );
            if ( pluginsRoot != null )
            {
                rewritePlugins( project.getBuildPlugins(), pluginsRoot, mappedVersions, originalVersions );
                if ( project.getPluginManagement() != null )
                {
                    pluginsRoot = pluginsRoot.getChild( "pluginManagement", namespace );
                    if ( pluginsRoot != null )
                    {
                        rewritePlugins( project.getPluginManagement().getPlugins(), pluginsRoot, mappedVersions,
                                        originalVersions );
                    }
                }
            }
            rewriteExtensions( project.getBuildExtensions(), pluginsRoot, mappedVersions, originalVersions );
        }

        if ( project.getReporting() != null )
        {
            Element pluginsRoot = rootElement.getChild( "reporting", namespace );
            if ( pluginsRoot != null )
            {
                rewriteReportPlugins( project.getReportPlugins(), pluginsRoot, mappedVersions, originalVersions );
            }
        }

        transformScm( project, rootElement, namespace, releaseConfiguration, projectId, scmRepository );
    }

    private void rewriteVersion( Element rootElement, Namespace namespace, Map mappedVersions, String projectId,
                                 MavenProject project, String parentVersion )
        throws ReleaseExecutionException
    {
        Element versionElement = rootElement.getChild( "version", namespace );
        String version = (String) mappedVersions.get( projectId );
        if ( version == null )
        {
            throw new ReleaseExecutionException( "Version for '" + project.getName() + "' was not mapped" );
        }
        else
        {

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
    }

    private String rewriteParent( MavenProject project, Element rootElement, Namespace namespace, Map mappedVersions,
                                  Map originalVersions )
        throws ReleaseExecutionException
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
                    throw new ReleaseExecutionException(
                        "Version for parent '" + parent.getName() + "' was not mapped" );
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
                                      Map originalVersions )
        throws ReleaseExecutionException
    {
        if ( dependencies != null )
        {
            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                Dependency dep = (Dependency) i.next();

                updateDomVersion( dep.getGroupId(), dep.getArtifactId(), mappedVersions, dep.getVersion(),
                                  originalVersions, "dependencies", "dependency", dependencyRoot );
            }
        }
    }

    private void rewritePlugins( List plugins, Element pluginRoot, Map mappedVersions, Map originalVersions )
        throws ReleaseExecutionException
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
                                      originalVersions, "plugins", "plugin", pluginRoot );
                }
            }
        }
    }

    private void rewriteExtensions( List extensions, Element extensionRoot, Map mappedVersions, Map originalVersions )
        throws ReleaseExecutionException
    {
        if ( extensions != null )
        {
            for ( Iterator i = extensions.iterator(); i.hasNext(); )
            {
                Extension extension = (Extension) i.next();

                updateDomVersion( extension.getGroupId(), extension.getArtifactId(), mappedVersions,
                                  extension.getVersion(), originalVersions, "extensions", "extension", extensionRoot );
            }
        }
    }

    private void rewriteReportPlugins( List plugins, Element pluginRoot, Map mappedVersions, Map originalVersions )
        throws ReleaseExecutionException
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
                                      originalVersions, "plugins", "plugin", pluginRoot );
                }
            }
        }
    }

    private void updateDomVersion( String groupId, String artifactId, Map mappedVersions, String version,
                                   Map originalVersions, String groupTagName, String tagName, Element dependencyRoot )
        throws ReleaseExecutionException
    {
        String key = ArtifactUtils.versionlessKey( groupId, artifactId );
        String mappedVersion = (String) mappedVersions.get( key );

        if ( version.equals( originalVersions.get( key ) ) )
        {
            if ( mappedVersion != null )
            {
                getLogger().debug( "Updating " + artifactId + " to " + mappedVersion );

                try
                {
                    XPath xpath = XPath.newInstance( "./" + groupTagName + "/" + tagName + "[groupId='" + groupId +
                        "' and artifactId='" + artifactId + "']" );

                    Element dependency = (Element) xpath.selectSingleNode( dependencyRoot );
                    Element versionElement = dependency.getChild( "version" );

                    // avoid if in management
                    if ( versionElement != null )
                    {
                        versionElement.setText( mappedVersion );
                    }
                }
                catch ( JDOMException e )
                {
                    throw new ReleaseExecutionException( "Unable to locate " + tagName + " to process in document", e );
                }
            }
            else
            {
                throw new ReleaseExecutionException(
                    "Version '" + version + "' for " + tagName + " '" + key + "' was not mapped" );
            }
        }
    }

    private void writePom( File pomFile, Document document, ReleaseConfiguration releaseConfiguration,
                           String modelVersion, String intro, String outtro, ScmRepository repository,
                           ScmProvider provider )
        throws ReleaseExecutionException
    {
        try
        {
            if ( releaseConfiguration.isUseEditMode() || provider.requiresEditMode() )
            {
                EditScmResult result =
                    provider.edit( repository, new ScmFileSet( releaseConfiguration.getWorkingDirectory(), pomFile ) );

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

        writePom( pomFile, document, releaseConfiguration, modelVersion, intro, outtro );
    }

    private void writePom( File pomFile, Document document, ReleaseConfiguration releaseConfiguration,
                           String modelVersion, String intro, String outtro )
        throws ReleaseExecutionException
    {
        Element rootElement = document.getRootElement();

        if ( releaseConfiguration.isAddSchema() )
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

    public void simulate( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        transform( releaseConfiguration, true );
    }

    protected abstract Map getOriginalVersionMap( ReleaseConfiguration releaseConfiguration );

    protected abstract Map getNextVersionMap( ReleaseConfiguration releaseConfiguration );

    protected abstract void transformScm( MavenProject project, Element rootElement, Namespace namespace,
                                          ReleaseConfiguration releaseConfiguration, String projectId,
                                          ScmRepository scmRepository )
        throws ReleaseExecutionException;
}
