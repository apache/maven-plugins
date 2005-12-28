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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.eclipse.writers.EclipseClasspathWriter;
import org.apache.maven.plugin.eclipse.writers.EclipseProjectWriter;
import org.apache.maven.plugin.eclipse.writers.EclipseSettingsWriter;
import org.apache.maven.plugin.eclipse.writers.EclipseWtpmodulesWriter;
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
    private File eclipseProjectDir;

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
    private File buildOutputDirectory;

    /**
     * The version of WTP for which configuration files will be generated. At the moment the only supported version is "R7",
     * and generated files will not be compatible with the upcoming 1.0 release and with WTP milestones &gt; M8.
     * As soon as WTP 1.0 will be released the default will be switched to 1.0.
     *
     * @parameter expression="${wtpversion}" default-value="R7"
     */
    private String wtpversion;

    /**
     * Not a plugin parameter. Collect missing source artifact for the final report.
     */
    private List missingSourceArtifacts = new ArrayList();

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

        if ( "pom".equals( executedProject.getPackaging() ) && eclipseProjectDir == null ) //$NON-NLS-1$
        {
            getLog().info( Messages.getString( "EclipsePlugin.pompackaging" ) ); //$NON-NLS-1$
            return;
        }

        if ( eclipseProjectDir == null )
        {
            eclipseProjectDir = executedProject.getFile().getParentFile();
        }
        else if ( !eclipseProjectDir.equals( executedProject.getFile().getParentFile() ) )
        {
            if ( !eclipseProjectDir.isDirectory() )
            {
                throw new MojoExecutionException( Messages.getString( "EclipsePlugin.notadir", eclipseProjectDir ) ); //$NON-NLS-1$
            }

            eclipseProjectDir = new File( eclipseProjectDir, executedProject.getArtifactId() );

            if ( !eclipseProjectDir.isDirectory() && !eclipseProjectDir.mkdirs() )
            {
                throw new MojoExecutionException( Messages.getString( "EclipsePlugin.cantcreatedir", eclipseProjectDir ) ); //$NON-NLS-1$
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
        {
            reactorArtifacts = EclipseUtils.resolveReactorArtifacts( project, reactorProjects );
        }
        else
        {
            reactorArtifacts = Collections.EMPTY_LIST;
        }

        // build a list of UNIQUE source dirs (both src and resources) to be used in classpath and wtpmodules
        EclipseSourceDir[] sourceDirs = EclipseUtils.buildDirectoryList( executedProject, eclipseProjectDir, getLog(),
                                                                         buildOutputDirectory );

        Collection artifacts = prepareArtifacts();

        downloadSourceArtifacts( artifacts, reactorArtifacts );

        new EclipseProjectWriter( getLog(), eclipseProjectDir, project ).write( projectBaseDir, executedProject,
                                                                                reactorArtifacts, projectnatures,
                                                                                buildcommands );

        new EclipseSettingsWriter( getLog(), eclipseProjectDir, project ).write();

        new EclipseClasspathWriter( getLog(), eclipseProjectDir, project, artifacts ).write( projectBaseDir,
                                                                                             reactorArtifacts,
                                                                                             sourceDirs,
                                                                                             classpathContainers,
                                                                                             localRepository,
                                                                                             artifactResolver,
                                                                                             artifactFactory,
                                                                                             buildOutputDirectory );

        new EclipseWtpmodulesWriter( getLog(), eclipseProjectDir, project, artifacts ).write( reactorArtifacts,
                                                                                              sourceDirs,
                                                                                              localRepository,
                                                                                              buildOutputDirectory );

        reportMissingSources();

        getLog().info(
                       Messages
                           .getString( "EclipsePlugin.wrote", //$NON-NLS-1$
                                       new Object[] { project.getArtifactId(), eclipseProjectDir.getAbsolutePath() } ) );
    }

    private Collection prepareArtifacts()
    {
        Collection artifacts = project.getTestArtifacts();
        EclipseUtils.fixMissingOptionalArtifacts( artifacts, project.getDependencyArtifacts(), localRepository,
                                                  artifactResolver, remoteArtifactRepositories, getLog() );
        EclipseUtils.fixSystemScopeArtifacts( artifacts, project.getDependencies() );
        return artifacts;
    }

    private void assertNotEmpty( String string, String elementName )
        throws MojoFailureException
    {
        if ( string == null )
        {
            throw new MojoFailureException( Messages.getString( "EclipsePlugin.missingelement", elementName ) ); //$NON-NLS-1$
        }
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

    private void fillDefaultBuilders( String packaging )
    {
        buildcommands = new ArrayList();
        // default builders for WTP R7, 1.0 may change
        buildcommands.add( "org.eclipse.wst.common.modulecore.ComponentStructuralBuilder" );
        buildcommands.add( "org.eclipse.jdt.core.javabuilder" );
        buildcommands.add( "org.eclipse.wst.validation.validationbuilder" );
        buildcommands.add( "org.eclipse.wst.common.modulecore.ComponentStructuralBuilderDependencyResolver" );
    }

    private void downloadSourceArtifacts( Collection artifacts, Collection reactorArtifacts )
        throws MojoExecutionException
    {
        // if downloadSources is off, just check local repository for reporting missing jars
        List remoteRepos = downloadSources ? remoteArtifactRepositories : new ArrayList( 0 );
        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            if ( reactorArtifacts.contains( artifact ) )
            {
                // source artifact not needed
                continue;
            }

            // source artifact: use the "sources" classifier added by the source plugin
            Artifact sourceArtifact = EclipseUtils.resolveArtifactWithClassifier( artifact, "sources", localRepository,
                                                                                  artifactResolver, artifactFactory,
                                                                                  remoteRepos );

            if ( !sourceArtifact.isResolved() )
            {
                // try using a plain javadoc jar if the source jar is not available
                EclipseUtils.resolveArtifactWithClassifier( artifact, "javadoc", localRepository, artifactResolver,
                                                            artifactFactory, remoteRepos );

                missingSourceArtifacts.add( artifact );
            }
        }
    }

    private void reportMissingSources()
    {
        if ( missingSourceArtifacts.isEmpty() )
        {
            return;
        }

        StringBuffer msg = new StringBuffer();

        if ( downloadSources )
        {
            msg.append( Messages.getString( "EclipseClasspathWriter.sourcesnotavailable" ) ); //$NON-NLS-1$
        }
        else
        {
            msg.append( Messages.getString( "EclipseClasspathWriter.sourcesnotdownloaded" ) ); //$NON-NLS-1$
        }

        for ( Iterator it = missingSourceArtifacts.iterator(); it.hasNext(); )
        {
            Artifact art = (Artifact) it.next();
            msg.append( Messages.getString( "EclipseClasspathWriter.sourcesmissingitem", art.getId() ) );
        }
        msg.append( "\n" );

        getLog().info( msg ); //$NON-NLS-1$

    }

}
