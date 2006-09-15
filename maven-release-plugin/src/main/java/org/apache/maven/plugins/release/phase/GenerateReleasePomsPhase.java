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

import org.apache.maven.plugins.release.ReleaseExecutionException;
import org.apache.maven.plugins.release.ReleaseResult;
import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.apache.maven.settings.Settings;

import java.util.List;

/**
 * Generate release POMs.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class GenerateReleasePomsPhase
    extends AbstractReleasePhase
{
    public ReleaseResult execute( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
        throws ReleaseExecutionException
    {
        ReleaseResult result = new ReleaseResult();

        if ( releaseDescriptor.isGenerateReleasePoms() )
        {
            logInfo( result, "Generating release POMs..." );

            generateReleasePoms();
        }

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }

    private void generateReleasePoms()
    {
/* TODO [!]: implement
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

    public ReleaseResult simulate( ReleaseDescriptor releaseDescriptor, Settings settings, List reactorProjects )
    {
        // TODO [!]: implement
        ReleaseResult result = new ReleaseResult();

        result.setResultCode( ReleaseResult.SUCCESS );

        return result;
    }
}
