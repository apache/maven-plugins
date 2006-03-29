package org.apache.maven.plugins.release;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.version.PluginVersionManager;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugins.release.helpers.ProjectScmRewriter;
import org.apache.maven.plugins.release.helpers.ProjectVersionResolver;
import org.apache.maven.plugins.release.helpers.ReleaseProgressTracker;
import org.apache.maven.plugins.release.helpers.ScmHelper;
import org.apache.maven.plugins.release.versions.VersionInfo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ModelUtils;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Prepare for a release in SCM.
 *
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @aggregator
 * @goal prepare
 * @requiresDependencyResolution test
 * @todo check how this works with version ranges
 */
public class PrepareReleaseMojo
    extends AbstractReleaseMojo
{
    private static final String RELEASE_POM = "release-pom.xml";

    private static final String POM = "pom.xml";

    /**
     * @parameter expression="${settings.interactiveMode}"
     * @required
     * @readonly
     */
    private boolean interactive;

    /**
     * @component
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * @component
     */
    private PluginVersionManager pluginVersionManager;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    /**
     * @parameter expression="${project.scm.developerConnection}"
     * @readonly
     */
    private String urlScm;

    /**
     * @parameter expression="${username}"
     */
    private String username;

    /**
     * @parameter expression="${password}"
     */
    private String password;

    /**
     * @parameter expression="${tag}"
     */
    private String tag;

    /**
     * The tag base directory, you must define it if you don't use the standard svn layout (trunk/tags/branches).
     * @parameter expression="${tagBase}"
     */
    private String tagBase;

    /**
     * @parameter expression="${resume}" default-value="true"
     */
    private boolean resume;

    /**
     * @parameter default-value="false" expression="${generateReleasePoms}"
     */
    private boolean generateReleasePoms;

    /**
     * @parameter expression="${useEditMode}" default-value="false"
     */
    private boolean useEditMode;

    /**
     * Test mode: don't checkin or tag anything in the scm repository.
     * Running <code>mvn -Dtestmode=true release:prepare</code> could be useful in order to check that modifications to
     * poms and scm operations (only listed in console) are working as expected.
     * Warning: running this goal in test mode will not checkin anything, but it will modificate your POMs! You will have
     * to manually rollback any change performed during the test, so be sure to commit everything before!
     *
     * @parameter expression="${testmode}" default-value="false"
     */
    private boolean testmode;

    /**
     * @component
     */
    private PathTranslator pathTranslator;

    private String userTag;

    private ReleaseProgressTracker releaseProgress;

    private ProjectVersionResolver versionResolver;

    private ProjectScmRewriter scmRewriter;

    private List pomFiles;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // ----------------------------------------------------------------------
        // Path of clarity
        //
        // You should be able to easily see what the path is that this will follow
        // in order to release a plugin.
        // ----------------------------------------------------------------------

        validateConfiguration();

        if ( testmode )
        {
            getLog()
                .info( "\n*****\n" + "Warning, release:perform is run in TEST MODE.\n" +
                    "Nothing will be committed or tagged in the repository, but you pom files will be updated!\n" +
                    "*****" );
        }

        // checkForInitialization()

        // checkForReleasedPrepared()

        checkpoint( ReleaseProgressTracker.CP_INITIALIZED );

        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_PREPARED_RELEASE ) )
        {
            checkForLocalModifications();

            // ----------------------------------------------------------------------
            // Walk through all the projects in the reactor so that we can check
            // up-front that we don't have any snapshot dependencies hiding in one
            // of the POMs.
            // ----------------------------------------------------------------------

            getLog().info( "Checking dependencies and plugins for snapshots ..." );

            for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
            {
                MavenProject project = (MavenProject) it.next();

                checkDependenciesForSnapshots( project, createReactorProjectSet( reactorProjects ) );
            }

            if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_POM_TRANSFORMED_FOR_RELEASE ) )
            {
                Map releasedProjects = new HashMap();

                for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
                {
                    MavenProject project = (MavenProject) it.next();

                    String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

                    if ( !ArtifactUtils.isSnapshot( project.getVersion() ) )
                    {
                        throw new MojoExecutionException(
                            "The project " + projectId + " isn't a snapshot (" + project.getVersion() + ")." );
                    }

                    getVersionResolver().resolveVersion( project.getOriginalModel(), projectId );

                    Model model = ModelUtils.cloneModel( project.getOriginalModel() );

                    transformPomToReleaseVersionPom( model, projectId, project.getFile(), project.getParentArtifact(),
                                                     project.getPluginArtifactRepositories() );
                    releasedProjects.put( projectId, model );
                }

                // We want to pick up the version changes in the child projects so we defer
                // the update of the dependencyManagement section until after we have
                // transformed all the projects and know their updated version numbers.
                for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
                {
                    MavenProject project = (MavenProject) it.next();

                    String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

                    Model model = (Model) releasedProjects.get( projectId );

                    updateDependencyManagement( model, project.getFile(), "release" );
                }

                checkpoint( ReleaseProgressTracker.CP_POM_TRANSFORMED_FOR_RELEASE );
            }

            if ( generateReleasePoms )
            {
                generateReleasePoms();
            }

            checkInRelease();

            tagRelease();

            if ( testmode )
            {
                getLog().info( "[TESTMODE] You can now verify how POMs have been transformed for release." );
                getLog()
                    .info(
                        "[TESTMODE] Press [return] in order to proceed and to see how POMs are transformed for next development iteration." );
                try
                {
                    getInputHandler().readLine();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }

            if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_POM_TRANSORMED_FOR_DEVELOPMENT ) )
            {
                for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
                {
                    MavenProject project = (MavenProject) it.next();

                    String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

                    Model model = ModelUtils.cloneModel( project.getOriginalModel() );

                    getVersionResolver().incrementVersion( model, projectId );

                    getScmRewriter().restoreScmInfo( model );

                    transformPomToSnapshotVersionPom( model, project.getFile() );
                }

                checkpoint( ReleaseProgressTracker.CP_POM_TRANSORMED_FOR_DEVELOPMENT );
            }

            if ( generateReleasePoms )
            {
                removeReleasePoms();
            }

            checkInNextSnapshot();

            checkpoint( ReleaseProgressTracker.CP_PREPARED_RELEASE );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private void validateConfiguration()
        throws MojoExecutionException
    {
        if ( StringUtils.isEmpty( urlScm ) )
        {
            Model model = ( (MavenProject) reactorProjects.get( 0 ) ).getModel();
            if ( model.getScm() != null )
            {
                urlScm = model.getScm().getConnection();
                if ( StringUtils.isEmpty( urlScm ) )
                {
                    throw new MojoExecutionException(
                        "Missing required setting: scm connection or developerConnection must be specified." );
                }
            }
        }
    }

    private void checkpoint( String pointName )
        throws MojoExecutionException
    {
        try
        {
            getReleaseProgress().checkpoint( pointName );
        }
        catch ( IOException e )
        {
            getLog().warn( "Error writing checkpoint.", e );
        }
    }

    private Set createReactorProjectSet( List reactorProjects )
    {
        Set reactorProjectSet = new HashSet();

        for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            String versionlessArtifactKey = ArtifactUtils
                .versionlessKey( project.getGroupId(), project.getArtifactId() );

            reactorProjectSet.add( versionlessArtifactKey );
        }

        return reactorProjectSet;
    }

    private void updateDependencyManagement( Model model, File file, String type )
        throws MojoExecutionException
    {
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

        File pomFile = new File( file.getParentFile(), POM );

        writePom( pomFile, model, type );
    }

    private void transformPomToSnapshotVersionPom( Model model, File file )
        throws MojoExecutionException
    {
        getLog().info( "Transforming " + model.getArtifactId() + " to snapshot" );
        ProjectVersionResolver versionResolver = getVersionResolver();

        Parent parent = model.getParent();

        //Rewrite parent version
        if ( parent != null )
        {
            String incrementedVersion = versionResolver
                .getResolvedVersion( parent.getGroupId(), parent.getArtifactId() );

            if ( incrementedVersion != null )
            {
                parent.setVersion( incrementedVersion );
            }
        }

        //Rewrite dependencies section
        List dependencies = model.getDependencies();

        if ( dependencies != null )
        {
            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                Dependency dep = (Dependency) i.next();

                if ( dep.getVersion() != null )
                {
                    String version = versionResolver.getResolvedVersion( dep.getGroupId(), dep.getArtifactId() );

                    if ( version != null )
                    {
                        getLog().info( "Updating " + dep.getArtifactId() + " to " + version );
                        dep.setVersion( version );
                    }
                }
            }
        }

        //Rewrite plugins section
        Build build = model.getBuild();

        if ( build != null )
        {
            List plugins = build.getPlugins();

            if ( plugins != null )
            {
                for ( Iterator i = plugins.iterator(); i.hasNext(); )
                {
                    Plugin plugin = (Plugin) i.next();

                    String version = versionResolver.getResolvedVersion( plugin.getGroupId(), plugin.getArtifactId() );

                    if ( version != null )
                    {
                        plugin.setVersion( version );
                    }
                }
            }

            //Rewrite extensions section
            List extensions = build.getExtensions();

            for ( Iterator i = extensions.iterator(); i.hasNext(); )
            {
                Extension ext = (Extension) i.next();

                String version = versionResolver.getResolvedVersion( ext.getGroupId(), ext.getArtifactId() );

                if ( version != null )
                {
                    ext.setVersion( version );
                }
            }
        }

        Reporting reporting = model.getReporting();

        if ( reporting != null )
        {
            //Rewrite reports section
            List reports = reporting.getPlugins();

            if ( reports != null )
            {
                for ( Iterator i = reports.iterator(); i.hasNext(); )
                {
                    ReportPlugin plugin = (ReportPlugin) i.next();

                    String version = versionResolver.getResolvedVersion( plugin.getGroupId(), plugin.getArtifactId() );

                    if ( version != null )
                    {
                        plugin.setVersion( version );
                    }
                }
            }
        }

        File pomFile = new File( file.getParentFile(), POM );

        writePom( pomFile, model, "development" );
    }

    protected ReleaseProgressTracker getReleaseProgress()
        throws MojoExecutionException
    {
        if ( releaseProgress == null )
        {
            try
            {
                releaseProgress = ReleaseProgressTracker.loadOrCreate( basedir );
            }
            catch ( IOException e )
            {
                getLog().warn(
                    "Cannot read existing release progress file from directory: " + basedir.getAbsolutePath() + "." );
                getLog().debug( "Cause", e );

                releaseProgress = ReleaseProgressTracker.create( basedir );
            }

            if ( resume )
            {
                releaseProgress.setResumeAtCheckpoint( true );
            }

            if ( releaseProgress.getScmUrl() == null )
            {
                releaseProgress.setScmUrl( urlScm );
            }

            if ( releaseProgress.getUsername() == null )
            {
                if ( username == null )
                {
                    username = System.getProperty( "user.name" );
                }
                releaseProgress.setUsername( username );
            }

            if ( releaseProgress.getPassword() == null && password != null )
            {
                releaseProgress.setPassword( password );
            }

            if ( releaseProgress.getScmTagBase() == null )
            {
                releaseProgress.setScmTagBase( tagBase );
            }

            if ( releaseProgress.getUsername() == null || releaseProgress.getScmUrl() == null )
            {
                throw new MojoExecutionException( "Missing release preparation information (Scm url)." );
            }
        }
        return releaseProgress;
    }

    protected ProjectVersionResolver getVersionResolver()
    {
        if ( versionResolver == null )
        {
            versionResolver = new ProjectVersionResolver( getLog(), getInputHandler(), interactive );
        }

        return versionResolver;
    }

    protected ProjectScmRewriter getScmRewriter()
        throws MojoExecutionException
    {
        if ( scmRewriter == null )
        {
            scmRewriter = new ProjectScmRewriter( getReleaseProgress() );
        }

        return scmRewriter;
    }

    private void checkForLocalModifications()
        throws MojoExecutionException
    {
        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_LOCAL_MODIFICATIONS_CHECKED ) )
        {
            getLog().info( "Verifying there are no local modifications ..." );

            List changedFiles;

            try
            {
                ScmHelper scm = getScm( basedir.getAbsolutePath() );

                changedFiles = scm.getStatus();
            }
            catch ( ScmException e )
            {
                throw new MojoExecutionException( "An error is occurred in the status process.", e );
            }

            String releaseProgressFilename = ReleaseProgressTracker.getReleaseProgressFilename();

            for ( Iterator i = changedFiles.iterator(); i.hasNext(); )
            {
                ScmFile f = (ScmFile) i.next();
                if ( "pom.xml.backup".equals( f.getPath() ) || f.getPath().equals( releaseProgressFilename ) )
                {
                    i.remove();
                }
            }

            if ( !changedFiles.isEmpty() )
            {
                StringBuffer message = new StringBuffer();

                for ( Iterator i = changedFiles.iterator(); i.hasNext(); )
                {
                    ScmFile file = (ScmFile) i.next();

                    message.append( file.toString() );

                    message.append( "\n" );
                }

                throw new MojoExecutionException(
                    "Cannot prepare the release because you have local modifications : \n" + message );
            }

            checkpoint( ReleaseProgressTracker.CP_LOCAL_MODIFICATIONS_CHECKED );
        }
    }

    /**
     * Check the POM in an attempt to remove all instances of SNAPSHOTs in preparation for a release. The goal
     * is to make the build reproducable so the removal of SNAPSHOTs is a necessary one.
     * <p/>
     * A check is made to ensure any parents in the lineage are released, that all the dependencies are
     * released and that any plugins utilized by this project are released.
     *
     * @throws MojoExecutionException
     */
    private void checkForPresenceOfSnapshots( MavenProject project )
        throws MojoExecutionException
    {
        getLog().info( "Checking lineage for snapshots ..." );

        MavenProject currentProject = project;

        while ( currentProject.hasParent() )
        {
            MavenProject parentProject = currentProject.getParent();

            String parentVersion;

            if ( ArtifactUtils.isSnapshot( parentProject.getVersion() ) )
            {
                parentVersion = getVersionResolver().getResolvedVersion( parentProject.getGroupId(),
                                                                         parentProject.getArtifactId() );

                if ( parentVersion == null )
                {
                    parentVersion = parentProject.getVersion();
                }

                if ( ArtifactUtils.isSnapshot( parentVersion ) )
                {
                    throw new MojoExecutionException( "Can't release project due to non released parent (" +
                        parentProject.getGroupId() + ":" + parentProject.getArtifactId() + parentVersion + "." );
                }
            }

            currentProject = parentProject;
        }
    }

    private void checkDependenciesForSnapshots( MavenProject project, Set reactorProjectSet )
        throws MojoExecutionException
    {
        Set snapshotDependencies = new HashSet();

        for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            String versionlessArtifactKey = ArtifactUtils.versionlessKey( artifact.getGroupId(), artifact
                .getArtifactId() );

            // ----------------------------------------------------------------------
            // We only care about dependencies that we are not processing as part
            // of the release. Projects in the reactor will be dealt with so we
            // don't need to worry about them here. We are strictly looking at
            // dependencies that are external to this project.
            // ----------------------------------------------------------------------

            if ( !reactorProjectSet.contains( versionlessArtifactKey ) &&
                ArtifactUtils.isSnapshot( artifact.getVersion() ) )
            {
                snapshotDependencies.add( artifact );
            }
        }

        for ( Iterator i = project.getPluginArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            String artifactVersion =
                getVersionResolver().getResolvedVersion( artifact.getGroupId(), artifact.getArtifactId() );

            if ( artifactVersion == null )
            {
                artifactVersion = artifact.getVersion();
            }

            if ( ArtifactUtils.isSnapshot( artifactVersion ) )
            {
                snapshotDependencies.add( artifact );
            }
        }

        if ( !snapshotDependencies.isEmpty() )
        {
            List snapshotsList = new ArrayList( snapshotDependencies );

            Collections.sort( snapshotsList );

            StringBuffer message = new StringBuffer();

            for ( Iterator i = snapshotsList.iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();

                message.append( "    " );

                message.append( artifact );

                message.append( "\n" );
            }

            throw new MojoExecutionException( "Can't release project due to non released dependencies :\n" + message );
        }
    }

    private void transformPomToReleaseVersionPom( Model model, String projectId, File file, Artifact parentArtifact,
                                                  List pluginArtifactRepositories )
        throws MojoExecutionException
    {
        getLog().info( "Transforming " + projectId + " to release" );
        getScmRewriter().rewriteScmInfo( model, projectId, getTagLabel() );

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

        if ( dependencies != null )
        {
            for ( Iterator i = dependencies.iterator(); i.hasNext(); )
            {
                Dependency dep = (Dependency) i.next();
                // Avoid in dep mgmt
                if ( dep.getVersion() != null )
                {
                    String resolvedVersion =
                        getVersionResolver().getResolvedVersion( dep.getGroupId(), dep.getArtifactId() );

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
                            getVersionResolver().getResolvedVersion( plugin.getGroupId(), plugin.getArtifactId() );

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
                            getVersionResolver().getResolvedVersion( plugin.getGroupId(), plugin.getArtifactId() );

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

                String resolvedVersion = getVersionResolver()
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
                    getVersionResolver().getResolvedVersion( plugin.getGroupId(), plugin.getArtifactId() );

                if ( resolvedVersion != null )
                {
                    plugin.setVersion( resolvedVersion );
                }
            }
        }

    }

    private void generateReleasePoms()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_GENERATED_RELEASE_POM ) )
        {
            String canonicalBasedir;

            try
            {
                canonicalBasedir = trimPathForScmCalculation( basedir );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot canonicalize basedir: " + basedir.getAbsolutePath(), e );
            }

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

                    projectVersion =
                        getVersionResolver().getResolvedVersion( project.getGroupId(), project.getArtifactId() );

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
                            version = getVersionResolver().getResolvedVersion( artifact.getGroupId(),
                                                                               artifact.getArtifactId() );

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
                    writePom( releasePomFile, releaseProject.getModel(), "release" );

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

                checkpoint( ReleaseProgressTracker.CP_GENERATED_RELEASE_POM );
            }
        }
    }

    private void fixNullValueInModel( Model modelToFix, Model correctModel )
    {
        if ( modelToFix.getModelVersion() != null )
        {
            modelToFix.setModelVersion( correctModel.getModelVersion() );
        }

        if ( modelToFix.getName() != null )
        {
            modelToFix.setName( correctModel.getName() );
        }

        if ( modelToFix.getParent() != null )
        {
            modelToFix.setParent( cloneParent( correctModel.getParent() ) );
        }

        if ( modelToFix.getVersion() != null )
        {
            modelToFix.setVersion( correctModel.getVersion() );
        }

        if ( modelToFix.getArtifactId() != null )
        {
            modelToFix.setArtifactId( correctModel.getArtifactId() );
        }

        if ( modelToFix.getProperties() != null && modelToFix.getProperties().isEmpty() )
        {
            modelToFix.setProperties( new Properties( correctModel.getProperties() ) );
        }

        if ( modelToFix.getGroupId() != null )
        {
            modelToFix.setGroupId( correctModel.getGroupId() );
        }

        if ( modelToFix.getPackaging() != null )
        {
            modelToFix.setPackaging( correctModel.getPackaging() );
        }

        if ( modelToFix.getModules() != null && !modelToFix.getModules().isEmpty() )
        {
            modelToFix.setModules( cloneModules( correctModel.getModules() ) );
        }

        if ( modelToFix.getDistributionManagement() != null )
        {
            modelToFix.setDistributionManagement( correctModel.getDistributionManagement() );
        }
    }

    private static List cloneModules( List modules )
    {
        if ( modules == null )
        {
            return modules;
        }
        return new ArrayList( modules );
    }

    private static Parent cloneParent( Parent parent )
    {
        if ( parent == null )
        {
            return parent;
        }

        Parent newParent = new Parent();
        newParent.setArtifactId( parent.getArtifactId() );
        newParent.setGroupId( parent.getGroupId() );
        newParent.setRelativePath( parent.getRelativePath() );
        newParent.setVersion( parent.getVersion() );
        return newParent;
    }

    private String resolveVersion( Artifact artifact, String artifactUsage, List pluginArtifactRepositories )
        throws MojoExecutionException
    {
        String resolvedVersion =
            getVersionResolver().getResolvedVersion( artifact.getGroupId(), artifact.getArtifactId() );

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

    /**
     * Check in the POM to SCM after it has been transformed where the version has been
     * set to the release version.
     *
     * @throws MojoExecutionException
     */
    private void checkInRelease()
        throws MojoExecutionException
    {
        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_CHECKED_IN_RELEASE_VERSION ) )
        {
            getLog().info( "Checking in modified POMs" );

            checkIn( pomFiles, "[maven-release-plugin] prepare release " + getTagLabel() );

            checkpoint( ReleaseProgressTracker.CP_CHECKED_IN_RELEASE_VERSION );
        }
    }

    private void removeReleasePoms()
        throws MojoExecutionException
    {
        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_REMOVED_RELEASE_POM ) )
        {
            getLog().info( "Removing release POMs" );

            File currentReleasePomFile = null;

            try
            {
                String canonicalBasedir = trimPathForScmCalculation( basedir );

                for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
                {
                    MavenProject project = (MavenProject) it.next();

                    currentReleasePomFile = new File( project.getFile().getParentFile(), RELEASE_POM );

                    String releasePomPath = trimPathForScmCalculation( currentReleasePomFile );

                    releasePomPath = releasePomPath.substring( canonicalBasedir.length() + 1 );

                    ScmHelper scm = getScm( basedir.getAbsolutePath() );
                    if ( !testmode )
                    {
                        scm.remove( "Removing for next development iteration.", releasePomPath );
                    }
                    else
                    {
                        getLog().info( "[TESTMODE] Removing for next development iteration. " + releasePomPath );
                    }

                    pomFiles.remove( currentReleasePomFile );

                    currentReleasePomFile.delete();
                }
            }
            catch ( ScmException e )
            {
                throw new MojoExecutionException( "Cannot remove " + currentReleasePomFile + " from development HEAD.",
                                                  e );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot remove " + currentReleasePomFile + " from development HEAD.",
                                                  e );
            }

            checkpoint( ReleaseProgressTracker.CP_REMOVED_RELEASE_POM );
        }
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

    private void checkInNextSnapshot()
        throws MojoExecutionException
    {
        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_CHECKED_IN_DEVELOPMENT_VERSION ) )
        {
            getLog().info( "Checking in development POMs" );

            checkIn( pomFiles, "[maven-release-plugin] prepare for next development iteration" );

            checkpoint( ReleaseProgressTracker.CP_CHECKED_IN_DEVELOPMENT_VERSION );
        }
    }

    private void checkIn( List pomFiles, String message )
        throws MojoExecutionException
    {
        ScmHelper scm = getScm( basedir.getAbsolutePath() );

        String tag = scm.getTag();

        // No tag here - we suppose user works on correct branch
        scm.setTag( null );

        try
        {
            if ( !testmode )
            {
                scm.checkin( pomFiles, message );
            }
            else
            {
                getLog().info(
                    "[TESTMODE] Checking in " + pomFiles.size() + " pom.xml files with message: " + message );
            }
        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( "An error is occurred in the checkin process.", e );
        }

        scm.setTag( tag );
    }

    /**
     * Creates a default tag name to suggest when prompting the user for a release tag name.
     * The tag name returned is the artifactId-resolvedVersion from the first project
     * in the reactorProjects list.
     * <p/>
     * Returns null if unable to determine a default tag name.
     *
     * @return
     * @throws MojoExecutionException
     */
    private String getDefaultReleaseTag()
        throws MojoExecutionException
    {
        MavenProject project = null;
        if ( reactorProjects.size() == 0 )
        {
            return null;
        }

        project = (MavenProject) reactorProjects.get( 0 );
        for ( int i = 1; i < reactorProjects.size(); i++ )
        {
            MavenProject parent = ( (MavenProject) reactorProjects.get( i ) ).getParent();
            if ( parent != null && !parent.equals( project ) )
            {
                // We have multiple projects, some of which are not descendants of the 0th project in the list.
                // rather than guess which one we should use for a default tag name, just return null
                return null;
            }
        }

        try
        {
            String version = getVersionResolver().getResolvedVersion( project.getGroupId(), project.getArtifactId() );

            if ( version == null )
            {
                VersionInfo info = getVersionResolver().getVersionInfo( project.getVersion() );
                if ( info != null )
                {
                    version = info.getReleaseVersionString();
                }
            }

            String defaultTag = project.getArtifactId() + "-" + version;

            ScmHelper scm = getScm( basedir.getAbsolutePath() );
            String provider = scm.getProvider();

            // Really each of the scm providers should support something which returns the supported
            // characters & lengths supported in tag names.  For now, we'll just assume that CVS is the
            // only one with the problem with periods.
            if ( "cvs".equals( provider ) )
            {
                defaultTag = defaultTag.replace( '.', '_' );
            }

            return defaultTag;
        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( "Unable to determine scm repository provider", e );
        }
    }

    /**
     * Returns the tag name to be used when tagging the release in the scm repository.
     * <p/>
     * If the userTag is already assigned, that value is returned.
     * Else if the releaseProperties already has the value, then use that value.
     * Else if we are interactive then prompt the user for a tag name.
     *
     * @return
     * @throws MojoExecutionException
     */
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

    /**
     * Tag the release in preparation for performing the release.
     * <p/>
     * We will provide the user with a default tag name based on the artifact id
     * and the version of the project being released.
     * <p/>
     * where artifactId is <code>plexus-action</code> and the version is <code>1.0-beta-4</code>, the
     * the suggested tag will be <code>PLEXUS_ACTION_1_0_BETA_4</code>.
     *
     * @throws MojoExecutionException
     */
    private void tagRelease()
        throws MojoExecutionException
    {
        if ( !getReleaseProgress().verifyCheckpoint( ReleaseProgressTracker.CP_TAGGED_RELEASE ) )
        {
            String tag = getTagLabel();

            try
            {
                ScmHelper scm = getScm( basedir.getAbsolutePath() );

                scm.setTag( tag );

                if ( !testmode )
                {
                    getLog().info( "Tagging release with the label " + tag + "." );
                    scm.tag();
                }
                else
                {
                    getLog().info( "[TESTMODE] Tagging release with the label " + tag + "." );
                }
            }
            catch ( ScmException e )
            {
                throw new MojoExecutionException( "An error is occurred in the tag process.", e );
            }

            checkpoint( ReleaseProgressTracker.CP_TAGGED_RELEASE );
        }
    }

    private void writePom( File pomFile, Model model, String versionName )
        throws MojoExecutionException
    {
        if ( pomFiles == null )
        {
            pomFiles = new ArrayList();
        }

        if ( !pomFiles.contains( pomFile ) )
        {
            pomFiles.add( pomFile );
        }

        ScmHelper scm = getScm( basedir.getAbsolutePath() );

        try
        {
            if ( useEditMode || scm.requiresEditMode() )
            {
                scm.edit( pomFile );
            }
        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( "An error occurred in the edit process.", e );
        }

        Writer writer = null;
        Writer tempOutput = null;

        try
        {

            MavenXpp3Writer pomWriter = new MavenXpp3Writer();

            // temporary hack to add namespace declaration, not supported by modello/MavenXpp3Writer
            // MavenXpp3Writer doesn't support writing the xsd declaration, do it manually
            tempOutput = new StringWriter();
            pomWriter.write( tempOutput, model );
            String pomString = tempOutput.toString();
            pomString = StringUtils
                .replaceOnce( pomString, "<project>",
                              "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                                  " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">" );

            writer = new FileWriter( pomFile );
            writer.write( pomString );

            // pomWriter.write( writer, model );

        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot write " + versionName + " version of pom to: " + pomFile, e );
        }
        finally
        {
            IOUtil.close( tempOutput );
            IOUtil.close( writer );
        }
    }
}
