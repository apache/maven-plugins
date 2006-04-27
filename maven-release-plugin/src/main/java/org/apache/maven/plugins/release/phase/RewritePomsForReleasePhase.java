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
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

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
 * Rewrite POMs for release.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RewritePomsForReleasePhase
    extends AbstractLogEnabled
    implements ReleasePhase
{
    /**
     * Tool that gets a configured SCM repository from release configuration.
     */
    private ScmRepositoryConfigurator scmRepositoryConfigurator;

    public void execute( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        for ( Iterator it = releaseConfiguration.getReactorProjects().iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

            getLogger().info( "Transforming " + projectId + " to release" );

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
                format.setLineSeparator( System.getProperty( "line.separator" ) );
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

            transformPomToReleaseVersionPom( project, document.getRootElement(),
                                             releaseConfiguration.getReleaseVersions() );

            writePom( project.getFile(), releaseConfiguration, document, intro, outtro, project.getModelVersion() );
        }

        // TODO: separate release POM generation into a separate phase?

        if ( releaseConfiguration.isGenerateReleasePoms() )
        {
            generateReleasePoms();
        }

    }

    private void transformPomToReleaseVersionPom( MavenProject project, Element rootElement, Map mappedVersions )
        throws ReleaseExecutionException
    {
        // TODO: what about if version is inherited? shouldn't prompt...
        Element versionElement = rootElement.getChild( "version", rootElement.getNamespace() );
        String version = (String) mappedVersions.get(
            ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() ) );
        if ( version == null )
        {
            throw new ReleaseExecutionException( "Version for '" + project.getName() + "' was not mapped" );
        }
        versionElement.setText( version );

        if ( project.hasParent() )
        {
            Element parentElement = rootElement.getChild( "parent", rootElement.getNamespace() );
            versionElement = parentElement.getChild( "version", rootElement.getNamespace() );
            MavenProject parent = project.getParent();
            version = (String) mappedVersions.get(
                ArtifactUtils.versionlessKey( parent.getGroupId(), parent.getArtifactId() ) );
            if ( version == null )
            {
                throw new ReleaseExecutionException( "Version for parent '" + parent.getName() + "' was not mapped" );
            }
            versionElement.setText( version );
        }

        // TODO: rewrite SCM
        // TODO: rewrite dependencies
        // TODO: rewrite dependency management
        // TODO: rewrite extensions
        // TODO: rewrite plugins, plugin management
        // TODO: rewrite reporting plugins

/*
        ProjectScmRewriter scmRewriter = getScmRewriter();
        scmRewriter.rewriteScmInfo( model, projectId, getTagLabel() );

        //Rewrite parent version
        if ( model.getParent() != null )
        {
            if ( ArtifactUtils.isSnapshot( parentArtifact.getBaseVersion() ) )
            {
                String version = resolveVersion( parentArtifact, "parent", pluginArtifactRepositories );

                model.getParent().setVersion( version );
            }
        }

        //Rewrite dependencies section
        List dependencies = model.getDependencies();

        ProjectVersionResolver versionResolver = getVersionResolver();
        if ( dependencies != null )
        {
            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                Dependency dep = (Dependency) i.next();
                // Avoid in dep mgmt
                if ( dep.getVersion() != null )
                {
                    String resolvedVersion =
                        versionResolver.getResolvedVersion( dep.getGroupId(), dep.getArtifactId() );

                    if ( resolvedVersion != null )
                    {
                        getLog().info( "Updating " + dep.getArtifactId() + " to " + resolvedVersion );
                        dep.setVersion( resolvedVersion );
                    }
                }
            }
        }

        Build build = model.getBuild();

        if ( build != null )
        {
            //Rewrite plugins section
            List plugins = build.getPlugins();

            if ( plugins != null )
            {
                for ( Iterator i = plugins.iterator(); i.hasNext(); )
                {
                    Plugin plugin = (Plugin) i.next();

                    // Avoid in plugin mgmt
                    if ( plugin.getVersion() != null )
                    {
                        String resolvedVersion =
                            versionResolver.getResolvedVersion( plugin.getGroupId(), plugin.getArtifactId() );

                        if ( resolvedVersion != null )
                        {
                            plugin.setVersion( resolvedVersion );
                        }
                    }
                }
            }

            PluginManagement pluginManagement = build.getPluginManagement();
            plugins = pluginManagement != null ? pluginManagement.getPlugins() : null;

            if ( plugins != null )
            {
                for ( Iterator i = plugins.iterator(); i.hasNext(); )
                {
                    Plugin plugin = (Plugin) i.next();

                    if ( plugin.getVersion() != null )
                    {
                        String resolvedVersion =
                            versionResolver.getResolvedVersion( plugin.getGroupId(), plugin.getArtifactId() );

                        if ( resolvedVersion != null )
                        {
                            plugin.setVersion( resolvedVersion );
                        }
                    }
                }
            }

            //Rewrite extensions section
            List extensions = build.getExtensions();

            for ( Iterator i = extensions.iterator(); i.hasNext(); )
            {
                Extension ext = (Extension) i.next();

                String resolvedVersion = versionResolver
                    .getResolvedVersion( ext.getGroupId(), ext.getArtifactId() );

                if ( resolvedVersion != null )
                {
                    ext.setVersion( resolvedVersion );
                }
            }
        }

        Reporting reporting = model.getReporting();

        if ( reporting != null )
        {
            //Rewrite reports section
            List reports = reporting.getPlugins();

            for ( Iterator i = reports.iterator(); i.hasNext(); )
            {
                ReportPlugin plugin = (ReportPlugin) i.next();

                String resolvedVersion =
                    versionResolver.getResolvedVersion( plugin.getGroupId(), plugin.getArtifactId() );

                if ( resolvedVersion != null )
                {
                    plugin.setVersion( resolvedVersion );
                }
            }
        }
            //Rewrite dependencyManagement section
            List dependencies =
                model.getDependencyManagement() != null ? model.getDependencyManagement().getDependencies() : null;

            if ( dependencies != null )
            {
                for ( Iterator i = dependencies.iterator(); i.hasNext(); )
                {
                    Dependency dep = (Dependency) i.next();

                    // If our dependency specifies an explicit released version, do NOT update
                    // it to the latest released version.  If we depend on a SNAPSHOT that is
                    // being released, we update the version to reflect the newly released version.
                    // TODO Cleaner way to determine snapshot?
                    if ( dep.getVersion() != null && dep.getVersion().endsWith( "-SNAPSHOT" ) )
                    {
                        String version = versionResolver.getResolvedVersion( dep.getGroupId(), dep.getArtifactId() );

                        if ( version != null )
                        {
                            getLog().info( "Updating DepMgmt " + dep.getArtifactId() + " to " + version );
                            dep.setVersion( version );
                        }
                    }
                }
            }

*/

    }

    private void writePom( File pomFile, ReleaseConfiguration releaseConfiguration, Document document, String intro,
                           String outtro, String modelVersion )
        throws ReleaseExecutionException
    {
        ScmRepository repository;
        ScmProvider provider;
        try
        {
            repository = scmRepositoryConfigurator.getConfiguredRepository( releaseConfiguration );

            provider = scmRepositoryConfigurator.getRepositoryProvider( repository );
        }
        catch ( ScmRepositoryException e )
        {
            throw new ReleaseScmRepositoryException( e.getMessage(), e.getValidationMessages() );
        }
        catch ( NoSuchScmProviderException e )
        {
            throw new ReleaseExecutionException( "Unable to configure SCM repository: " + e.getMessage(), e );
        }

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
            List elements = rootElement.getContent( new ElementFilter( Namespace.getNamespace( "" ) ) );
            for ( Iterator i = elements.iterator(); i.hasNext(); )
            {
                Element e = (Element) i.next();
                e.setNamespace( pomNamespace );
            }
        }

        Writer writer = null;
        try
        {
            // TODO: better handling of encoding. Currently the definition is not written out and is embedded in the intro if it already existed
            // TODO: the XMLOutputter and Writer need to have their encodings aligned
            writer = new FileWriter( pomFile );

            if ( intro != null )
            {
                writer.write( intro );
            }

            Format format = Format.getRawFormat();
            format.setLineSeparator( System.getProperty( "line.separator" ) );
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

    private void generateReleasePoms()
    {
/*
        String canonicalBasedir;

        try
        {
            canonicalBasedir = trimPathForScmCalculation( basedir );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot canonicalize basedir: " + basedir.getAbsolutePath(), e );
        }

        ProjectVersionResolver versionResolver = getVersionResolver();
        for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            MavenProject releaseProject = new MavenProject( project );
            Model releaseModel = releaseProject.getModel();
            fixNullValueInModel( releaseModel, project.getModel() );

            // the release POM should reflect bits of these which were injected at build time...
            // we don't need these polluting the POM.
            releaseModel.setProfiles( Collections.EMPTY_LIST );
            releaseModel.setDependencyManagement( null );
            releaseProject.getBuild().setPluginManagement( null );

            String projectVersion = releaseModel.getVersion();
            if ( ArtifactUtils.isSnapshot( projectVersion ) )
            {
                String snapshotVersion = projectVersion;

                projectVersion = versionResolver.getResolvedVersion( project.getGroupId(), project.getArtifactId() );

                if ( ArtifactUtils.isSnapshot( projectVersion ) )
                {
                    throw new MojoExecutionException(
                        "MAJOR PROBLEM!!! Cannot find resolved version to be used in releasing project: " +
                            releaseProject.getId() );
                }

                releaseModel.setVersion( projectVersion );

                String finalName = releaseModel.getBuild().getFinalName();

                if ( finalName.equals( releaseModel.getArtifactId() + "-" + snapshotVersion ) )
                {
                    releaseModel.getBuild().setFinalName( null );
                }
                else if ( finalName.indexOf( "SNAPSHOT" ) > -1 )
                {
                    throw new MojoExecutionException(
                        "Cannot reliably adjust the finalName of project: " + releaseProject.getId() );
                }
            }

            releaseModel.setParent( null );

            Set artifacts = releaseProject.getArtifacts();

            if ( artifacts != null )
            {
                //Rewrite dependencies section
                List newdeps = new ArrayList();

                Map oldDeps = new HashMap();

                List deps = releaseProject.getDependencies();
                if ( deps != null )
                {
                    for ( Iterator depIterator = deps.iterator(); depIterator.hasNext(); )
                    {
                        Dependency dep = (Dependency) depIterator.next();

                        oldDeps.put( ArtifactUtils.artifactId( dep.getGroupId(), dep.getArtifactId(), dep.getType(),
                                                               dep.getVersion() ), dep );
                    }
                }

                for ( Iterator i = releaseProject.getArtifacts().iterator(); i.hasNext(); )
                {
                    Artifact artifact = (Artifact) i.next();

                    String key = artifact.getId();

                    Dependency newdep = new Dependency();

                    newdep.setArtifactId( artifact.getArtifactId() );
                    newdep.setGroupId( artifact.getGroupId() );

                    String version = artifact.getVersion();
                    if ( artifact.isSnapshot() )
                    {
                        version = versionResolver.getResolvedVersion( artifact.getGroupId(), artifact.getArtifactId() );

                        if ( ArtifactUtils.isSnapshot( version ) )
                        {
                            throw new MojoExecutionException(
                                "Unresolved SNAPSHOT version of: " + artifact + ". Cannot proceed with release." );
                        }
                    }

                    newdep.setVersion( version );
                    newdep.setType( artifact.getType() );
                    newdep.setScope( artifact.getScope() );
                    newdep.setClassifier( artifact.getClassifier() );

                    Dependency old = (Dependency) oldDeps.get( key );

                    if ( old != null )
                    {
                        newdep.setSystemPath( old.getSystemPath() );
                        newdep.setExclusions( old.getExclusions() );
                        newdep.setOptional( old.isOptional() );
                    }

                    newdeps.add( newdep );
                }

                releaseModel.setDependencies( newdeps );
            }

            // Use original - don't want the lifecycle introduced ones
            Build build = releaseProject.getOriginalModel().getBuild();
            List plugins = build != null ? build.getPlugins() : null;

            if ( plugins != null )
            {
                //Rewrite plugins version
                for ( Iterator i = plugins.iterator(); i.hasNext(); )
                {
                    Plugin plugin = (Plugin) i.next();

                    String version;
                    try
                    {
                        version = pluginVersionManager.resolvePluginVersion( plugin.getGroupId(), plugin
                            .getArtifactId(), releaseProject, getSettings(), localRepository );
                    }
                    catch ( PluginVersionResolutionException e )
                    {
                        throw new MojoExecutionException(
                            "Cannot resolve version for plugin '" + plugin.getKey() + "': " + e.getMessage(), e );
                    }
                    catch ( InvalidPluginException e )
                    {
                        throw new MojoExecutionException(
                            "Cannot resolve version for plugin '" + plugin.getKey() + "': " + e.getMessage(), e );
                    }
                    catch ( PluginVersionNotFoundException e )
                    {
                        throw new MojoFailureException( e.getMessage() );
                    }

                    if ( ArtifactUtils.isSnapshot( version ) )
                    {
                        throw new MojoFailureException(
                            "Resolved version of plugin is a snapshot. Please release this plugin before releasing this project.\n\nGroupId: " +
                                plugin.getGroupId() + "\nArtifactId: " + plugin.getArtifactId() +
                                "\nResolved Version: " + version + "\n\n" );
                    }

                    plugin.setVersion( version );
                }
            }

            Reporting reporting = releaseModel.getReporting();
            List reports = reporting != null ? reporting.getPlugins() : null;

            if ( reports != null )
            {
                //Rewrite report version
                for ( Iterator i = reports.iterator(); i.hasNext(); )
                {
                    ReportPlugin plugin = (ReportPlugin) i.next();

                    String version;
                    try
                    {
                        version = pluginVersionManager.resolveReportPluginVersion( plugin.getGroupId(), plugin
                            .getArtifactId(), releaseProject, getSettings(), localRepository );
                    }
                    catch ( PluginVersionResolutionException e )
                    {
                        throw new MojoExecutionException(
                            "Cannot resolve version for report '" + plugin.getKey() + "': " + e.getMessage(), e );
                    }
                    catch ( InvalidPluginException e )
                    {
                        throw new MojoExecutionException(
                            "Cannot resolve version for plugin '" + plugin.getKey() + "': " + e.getMessage(), e );
                    }
                    catch ( PluginVersionNotFoundException e )
                    {
                        throw new MojoFailureException( e.getMessage() );
                    }

                    if ( ArtifactUtils.isSnapshot( version ) )
                    {
                        throw new MojoFailureException(
                            "Resolved version of report is a snapshot. Please release this report plugin before releasing this project.\n\nGroupId: " +
                                plugin.getGroupId() + "\nArtifactId: " + plugin.getArtifactId() +
                                "\nResolved Version: " + version + "\n\n" );
                    }

                    plugin.setVersion( version );
                }
            }

            List extensions = build != null ? build.getExtensions() : null;

            if ( extensions != null )
            {
                //Rewrite extension version
                Map extensionArtifacts = releaseProject.getExtensionArtifactMap();

                for ( Iterator i = extensions.iterator(); i.hasNext(); )
                {
                    Extension ext = (Extension) i.next();

                    String extensionId = ArtifactUtils.versionlessKey( ext.getGroupId(), ext.getArtifactId() );

                    Artifact artifact = (Artifact) extensionArtifacts.get( extensionId );

                    String version = resolveVersion( artifact, "extension", releaseProject
                        .getPluginArtifactRepositories() );

                    ext.setVersion( version );
                }
            }

            pathTranslator.unalignFromBaseDirectory( releaseProject.getModel(), project.getFile().getParentFile() );

            File releasePomFile = new File( releaseProject.getFile().getParentFile(), RELEASE_POM );

            Writer writer = null;

            try
            {
                writePom( releasePomFile, releaseProject.getModel(), rootElement );

                writer = new FileWriter( releasePomFile );

                releaseProject.writeModel( writer );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot write release-pom to: " + releasePomFile, e );
            }
            finally
            {
                IOUtil.close( writer );
            }

            try
            {
                String releasePomPath = trimPathForScmCalculation( releasePomFile );

                releasePomPath = releasePomPath.substring( canonicalBasedir.length() + 1 );

                ScmHelper scm = getScm( basedir.getAbsolutePath() );

                if ( !testmode )
                {
                    scm.add( releasePomPath );
                }
                else
                {
                    getLog().info( "[TESTMODE] adding file: " + releasePomPath );
                }
            }
            catch ( ScmException e )
            {
                throw new MojoExecutionException( "Error adding the release-pom.xml: " + releasePomFile, e );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error adding the release-pom.xml: " + releasePomFile, e );
            }
        }
*/
    }

    /*
    private String resolveVersion( Artifact artifact, String artifactUsage, List pluginArtifactRepositories )
        throws MojoExecutionException
    {
        ProjectVersionResolver versionResolver = getVersionResolver();
        String resolvedVersion = versionResolver.getResolvedVersion( artifact.getGroupId(), artifact.getArtifactId() );

        if ( resolvedVersion == null )
        {
            if ( artifact.getFile() == null )
            {
                try
                {
                    artifactMetadataSource.retrieve( artifact, localRepository, pluginArtifactRepositories );
                }
                catch ( ArtifactMetadataRetrievalException e )
                {
                    throw new MojoExecutionException( "Cannot resolve " + artifactUsage + ": " + artifact, e );
                }
            }

            resolvedVersion = artifact.getVersion();
        }

        return resolvedVersion;
    }

    private String trimPathForScmCalculation( File file )
        throws IOException
    {
        String path = file.getCanonicalPath();

        path = path.replace( File.separatorChar, '/' );

        if ( path.endsWith( "/" ) )
        {
            path = path.substring( path.length() - 1 );
        }

        return path;
    }
*/

    /**
     * Returns the tag name to be used when tagging the release in the scm repository.
     * <p/>
     * If the userTag is already assigned, that value is returned.
     * Else if the releaseProperties already has the value, then use that value.
     * Else if we are interactive then prompt the user for a tag name.
     */
/*
    private String getTagLabel()
        throws MojoExecutionException
    {
        if ( userTag == null )
        {
            if ( StringUtils.isNotEmpty( releaseProgress.getScmTag() ) )
            {
                userTag = releaseProgress.getScmTag();
            }
            else
            {
                try
                {
                    if ( tag == null && interactive )
                    {
                        String prompt = "What tag name should be used? ";

                        String defaultTag = getDefaultReleaseTag();

                        if ( defaultTag != null )
                        {
                            prompt = prompt + "[" + defaultTag + "]";
                        }

                        getLog().info( prompt );

                        String inputTag = getInputHandler().readLine();

                        userTag = ( StringUtils.isEmpty( inputTag ) ) ? defaultTag : inputTag;
                    }
                    else
                    {
                        userTag = tag;
                    }
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "An error has occurred while reading user input.", e );
                }

                // If we were able to get a userTag from the user, save it to our release.properties file
                if ( userTag != null )
                {
                    ReleaseProgressTracker releaseProgress = getReleaseProgress();
                    releaseProgress.setScmTag( userTag );
                    try
                    {
                        releaseProgress.store();
                    }
                    catch ( IOException e )
                    {
                        getLog().warn( "An error occurred while saving the release progress file", e );
                    }

                }
            }
        }

        if ( userTag == null )
        {
            throw new MojoExecutionException( "A release tag must be specified" );
        }

        return userTag;
    }
*/
    public void simulate( ReleaseConfiguration releaseConfiguration )
    {
        // TODO: implement

    }
}
