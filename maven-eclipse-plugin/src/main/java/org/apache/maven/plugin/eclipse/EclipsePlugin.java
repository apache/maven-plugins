package org.apache.maven.plugin.eclipse;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * A Maven2 plugin which integrates the use of Maven2 with Eclipse.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:fgiust@apache.org">Fabrizio Giustina</a>
 * @version $Id$
 * @goal eclipse
 * @requiresDependencyResolution test
 * @execute phase="generate-sources"
 */
public class EclipsePlugin
    extends AbstractMojo
{
    /**
     * The project whose project files to create.
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * The currently executed project (can be a reactor project).
     *
     * @parameter expression="${executedProject}"
     */
    private MavenProject executedProject;

    /**
     * Local maven repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * If the executed project is a reactor project, this will contains the full list of projects in the reactor.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    /**
     * Artifact resolver, needed to download source jars for inclusion in classpath.
     *
     * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
     * @required
     * @readonly
     */
    private ArtifactResolver artifactResolver;

    /**
     * Artifact factory, needed to download source jars for inclusion in classpath.
     *
     * @component role="org.apache.maven.artifact.factory.ArtifactFactory"
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * Remote repositories which will be searched for source attachments.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    private List remoteArtifactRepositories;

    /**
     * List of eclipse project natures. By default the <code>org.eclipse.jdt.core.javanature</code> nature plus the
     * needed WTP natures are added.
     * <pre>
     *    &lt;projectnatures>
     *      &lt;projectnature>org.eclipse.jdt.core.javanature&lt;/projectnature>
     *      &lt;projectnature>org.eclipse.wst.common.modulecore.ModuleCoreNature&lt;/projectnature>
     *    &lt;/projectnatures>
     * </pre>
     *
     * @parameter
     */
    private List projectnatures;

    /**
     * List of eclipse build commands. By default the <code>org.eclipse.jdt.core.javabuilder</code> builder plus the
     * needed WTP builders are added.
     * Configuration example:
     * <pre>
     *    &lt;buildcommands>
     *      &lt;java.lang.String>org.eclipse.wst.common.modulecore.ComponentStructuralBuilder&lt;/java.lang.String>
     *      &lt;java.lang.String>org.eclipse.jdt.core.javabuilder&lt;/java.lang.String>
     *      &lt;java.lang.String>org.eclipse.wst.common.modulecore.ComponentStructuralBuilderDependencyResolver&lt;/java.lang.String>
     *    &lt;/buildcommands>
     * </pre>
     *
     * @parameter
     */
    private List buildcommands;

    /**
     * List of container classpath entries. By default the <code>org.eclipse.jdt.launching.JRE_CONTAINER</code> classpath
     * container is added.
     * Configuration example:
     * <pre>
     *    &lt;classpathContainers>
     *      &lt;buildcommand>org.eclipse.jdt.launching.JRE_CONTAINER&lt;/buildcommand>
     *      &lt;buildcommand>org.eclipse.jst.server.core.container/org.eclipse.jst.server.tomcat.runtimeTarget/Apache Tomcat v5.5&lt;/buildcommand>
     *      &lt;buildcommand>org.eclipse.jst.j2ee.internal.web.container/artifact&lt;/buildcommand>
     *    &lt;/classpathContainers>
     * </pre>
     *
     * @parameter
     */
    private List classpathContainers;

    /**
     * Enables/disables the downloading of source attachments. Defaults to false.
     *
     * @parameter expression="${eclipse.downloadSources}"
     */
    private boolean downloadSources;

    /**
     * Eclipse workspace directory.
     *
     * @parameter expression="${eclipse.workspace}" alias="outputDir"
     */
    private File eclipseWorkspaceDir;

    /**
     * When set to false, the plugin will not create sub-projects and instead reference those sub-projects 
     * using the installed package in the local repository
     *
     * @parameter expression="${eclipse.useProjectReferences}" default-value="true"
     * @required
     */
    private boolean useProjectReferences;

    /**
     * The default output directory
     *
     * @parameter expression="${project.build.outputDirectory}" alias="outputDirectory"
     */
    private String buildOutputDirectory;

    /**
     * The version of WTP for which configuration files will be generated. At the moment the only supported version is "R7",
     * and generated files will not be compatible with the upcoming 1.0 release and with WTP milestones &gt; M8.
     * As soon as WTP 1.0 will be released the default will be switched to 1.0.
     *
     * @parameter expression="${wtpversion}" default-value="R7"
     */
    private String wtpversion;

    /**
     * Setter for <code>project</code>. Needed for tests.
     *
     * @param project The MavenProject to set.
     */
    protected void setProject( MavenProject project )
    {
        this.project = project;
    }

    /**
     * Setter for <code>localRepository</code>. Needed for tests.
     *
     * @param localRepository The ArtifactRepository to set.
     */
    protected void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    /**
     * Setter for <code>artifactFactory</code>. Needed for tests.
     *
     * @param artifactFactory The artifactFactory to set.
     */
    protected void setArtifactFactory( ArtifactFactory artifactFactory )
    {
        this.artifactFactory = artifactFactory;
    }

    /**
     * Setter for <code>artifactResolver</code>. Needed for tests.
     *
     * @param artifactResolver The artifactResolver to set.
     */
    protected void setArtifactResolver( ArtifactResolver artifactResolver )
    {
        this.artifactResolver = artifactResolver;
    }

    /**
     * Setter for <code>remoteArtifactRepositories</code>. Needed for tests.
     *
     * @param remoteArtifactRepositories The remoteArtifactRepositories to set.
     */
    protected void setRemoteArtifactRepositories( List remoteArtifactRepositories )
    {
        this.remoteArtifactRepositories = remoteArtifactRepositories;
    }

    /**
     * Setter for <code>buildcommands</code>. Needed for tests.
     *
     * @param buildcommands The buildcommands to set.
     */
    protected void setBuildcommands( List buildcommands )
    {
        this.buildcommands = buildcommands;
    }

    /**
     * Setter for <code>classpathContainers</code>. Needed for tests.
     *
     * @param classpathContainers The classpathContainers to set.
     */
    protected void setClasspathContainers( List classpathContainers )
    {
        this.classpathContainers = classpathContainers;
    }

    /**
     * Setter for <code>projectnatures</code>. Needed for tests.
     *
     * @param projectnatures The projectnatures to set.
     */
    protected void setProjectnatures( List projectnatures )
    {
        this.projectnatures = projectnatures;
    }

    /**
     * Setter for <code>outputDir</code>. Needed for tests.
     *
     * @param outputDir The outputDir to set.
     */
    public void setEclipseWorkspaceDir( File outputDir )
    {
        this.eclipseWorkspaceDir = outputDir;
    }

    /**
     * Getter for <code>outputDir</code>. Needed for tests.
     * @return Returns the outputDir.
     */
    public File getEclipseWorkspaceDir()
    {
        return this.eclipseWorkspaceDir;
    }

    /**
     * Setter for <code>downloadSources</code>.
     * @param downloadSources The downloadSources to set.
     */
    public void setDownloadSources( boolean downloadSources )
    {
        this.downloadSources = downloadSources;
    }

    /**
     * Setter for <code>outputDirectory</code>.
     * @param outputDirectory The outputDirectory to set.
     */
    public void setBuildOutputDirectory( String outputDirectory )
    {
        this.buildOutputDirectory = outputDirectory;
    }

    /**
     * Getter for <code>buildOutputDirectory</code>.
     * @return Returns the buildOutputDirectory.
     */
    public String getBuildOutputDirectory()
    {
        return this.buildOutputDirectory;
    }

    /**
     * Setter for <code>wtpversion</code>.
     * @param wtpversion The wtpversion to set.
     */
    public void setWtpversion( String wtpversion )
    {
        this.wtpversion = wtpversion;
    }

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        if ( !"R7".equalsIgnoreCase( wtpversion ) )
        {
            throw new MojoExecutionException( Messages
                .getString( "EclipsePlugin.unsupportedwtp", new Object[] { wtpversion, "R7" } ) ); //$NON-NLS-1$
        }

        if ( executedProject == null )
        {
            // backwards compat with alpha-2 only
            executedProject = project;
        }

        assertNotEmpty( executedProject.getGroupId(), "groupId" ); //$NON-NLS-1$
        assertNotEmpty( executedProject.getArtifactId(), "artifactId" ); //$NON-NLS-1$

        // defaults
        String packaging = executedProject.getPackaging();
        if ( projectnatures == null )
        {
            fillDefaultNatures( packaging );
        }

        if ( buildcommands == null )
        {
            fillDefaultBuilders( packaging );
        }

        if ( classpathContainers == null )
        {
            fillDefaultClasspathContainers( packaging );
        }
        else if ( !classpathContainers.contains( "org.eclipse.jdt.launching.JRE_CONTAINER" ) )
        {
            getLog()
                .warn(
                       "You did specify a list of classpath containers without the base org.eclipse.jdt.launching.JRE_CONTAINER.\n"
                           + "If you specify custom classpath containers you should also add org.eclipse.jdt.launching.JRE_CONTAINER to the list" );
            classpathContainers.add( 0, "org.eclipse.jdt.launching.JRE_CONTAINER" );
        }

        // end defaults

        if ( executedProject.getFile() == null || !executedProject.getFile().exists() )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.missingpom" ) ); //$NON-NLS-1$
        }

        if ( "pom".equals( executedProject.getPackaging() ) && eclipseWorkspaceDir == null ) //$NON-NLS-1$
        {
            getLog().info( Messages.getString( "EclipsePlugin.pompackaging" ) ); //$NON-NLS-1$
            return;
        }

        if ( eclipseWorkspaceDir == null )
        {
            eclipseWorkspaceDir = executedProject.getFile().getParentFile();
        }
        else if ( !eclipseWorkspaceDir.equals( executedProject.getFile().getParentFile() ) )
        {
            if ( !eclipseWorkspaceDir.isDirectory() )
            {
                throw new MojoExecutionException( Messages.getString( "EclipsePlugin.notadir", eclipseWorkspaceDir ) ); //$NON-NLS-1$
            }

            eclipseWorkspaceDir = new File( eclipseWorkspaceDir, executedProject.getArtifactId() );

            if ( !eclipseWorkspaceDir.isDirectory() && !eclipseWorkspaceDir.mkdir() )
            {
                throw new MojoExecutionException( Messages
                    .getString( "EclipsePlugin.cantcreatedir", eclipseWorkspaceDir ) ); //$NON-NLS-1$
            }
        }

        // ready to start
        write();
    }

    public void write()
        throws MojoExecutionException
    {
        File projectBaseDir = executedProject.getFile().getParentFile();

        // build the list of referenced ARTIFACTS produced by reactor projects
        List reactorArtifacts;
        if ( useProjectReferences )
            reactorArtifacts = EclipseUtils.resolveReactorArtifacts( project, reactorProjects );
        else
            reactorArtifacts = Collections.EMPTY_LIST;

        // build a list of UNIQUE source dirs (both src and resources) to be used in classpath and wtpmodules
        EclipseSourceDir[] sourceDirs = EclipseUtils.buildDirectoryList( executedProject, eclipseWorkspaceDir,
                                                                         getLog(), buildOutputDirectory );

        // use project since that one has all artifacts resolved.
        new EclipseClasspathWriter( getLog() ).write( projectBaseDir, eclipseWorkspaceDir, project, reactorArtifacts,
                                                      sourceDirs, classpathContainers, localRepository,
                                                      artifactResolver, artifactFactory, remoteArtifactRepositories,
                                                      downloadSources, buildOutputDirectory );

        new EclipseProjectWriter( getLog() ).write( projectBaseDir, eclipseWorkspaceDir, project, executedProject,
                                                    reactorArtifacts, projectnatures, buildcommands );

        new EclipseSettingsWriter( getLog() ).write( projectBaseDir, eclipseWorkspaceDir, project );

        new EclipseWtpmodulesWriter( getLog() ).write( eclipseWorkspaceDir, project, reactorArtifacts, sourceDirs,
                                                       localRepository, artifactResolver, remoteArtifactRepositories );

        getLog().info(
                       Messages.getString( "EclipsePlugin.wrote", //$NON-NLS-1$
                                           new Object[] {
                                               project.getArtifactId(),
                                               eclipseWorkspaceDir.getAbsolutePath() } ) );
    }

    private void assertNotEmpty( String string, String elementName )
        throws MojoFailureException
    {
        if ( string == null )
        {
            throw new MojoFailureException( Messages.getString( "EclipsePlugin.missingelement", elementName ) ); //$NON-NLS-1$
        }
    }

    private void fillDefaultBuilders( String packaging )
    {
        buildcommands = new ArrayList();
        // default builders for WTP R7, 1.0 may change
        buildcommands.add( "org.eclipse.wst.common.modulecore.ComponentStructuralBuilder" );
        buildcommands.add( "org.eclipse.jdt.core.javabuilder" );
        buildcommands.add( "org.eclipse.wst.validation.validationbuilder" );
        buildcommands.add( "org.eclipse.wst.common.modulecore.ComponentStructuralBuilderDependencyResolver" );
    }

    private void fillDefaultNatures( String packaging )
    {
        projectnatures = new ArrayList();
        // default natures for WTP R7, 1.0 may change
        projectnatures.add( "org.eclipse.jem.workbench.JavaEMFNature" );
        projectnatures.add( "org.eclipse.jdt.core.javanature" );
        projectnatures.add( "org.eclipse.wst.common.modulecore.ModuleCoreNature" );
    }

    private void fillDefaultClasspathContainers( String packaging )
    {
        classpathContainers = new ArrayList();
        classpathContainers.add( "org.eclipse.jdt.launching.JRE_CONTAINER" );
    }

}
