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
import java.util.Arrays;
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
import org.apache.maven.plugin.eclipse.writers.EclipseWtpSettingsWriter;
import org.apache.maven.plugin.eclipse.writers.EclipseWtpmodulesWriter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * A Maven2 plugin which integrates the use of Maven2 with Eclipse.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:fgiust@apache.org">Fabrizio Giustina</a>
 * @version $Id$
 * @goal eclipse
 * @requiresDependencyResolution test
 * @execute phase="generate-test-resources"
 */
public class EclipsePlugin
    extends AbstractMojo
{

    private static final String NATURE_WST_FACET_CORE_NATURE = "org.eclipse.wst.common.project.facet.core.nature"; //$NON-NLS-1$

    private static final String BUILDER_WST_COMPONENT_STRUCTURAL_DEPENDENCY_RESOLVER = "org.eclipse.wst.common.modulecore.ComponentStructuralBuilderDependencyResolver"; //$NON-NLS-1$

    private static final String BUILDER_WST_VALIDATION = "org.eclipse.wst.validation.validationbuilder"; //$NON-NLS-1$

    private static final String BUILDER_JDT_CORE_JAVA = "org.eclipse.jdt.core.javabuilder"; //$NON-NLS-1$

    private static final String BUILDER_WST_COMPONENT_STRUCTURAL = "org.eclipse.wst.common.modulecore.ComponentStructuralBuilder"; //$NON-NLS-1$

    private static final String NATURE_WST_MODULE_CORE_NATURE = "org.eclipse.wst.common.modulecore.ModuleCoreNature"; //$NON-NLS-1$

    private static final String NATURE_JDT_CORE_JAVA = "org.eclipse.jdt.core.javanature"; //$NON-NLS-1$

    private static final String NATURE_JEM_WORKBENCH_JAVA_EMF = "org.eclipse.jem.workbench.JavaEMFNature"; //$NON-NLS-1$

    private static final String COMMON_PATH_JDT_LAUNCHING_JRE_CONTAINER = "org.eclipse.jdt.launching.JRE_CONTAINER"; //$NON-NLS-1$

    //  warning, order is important for binary search
    public static final String[] WTP_SUPPORTED_VERSIONS = new String[] { "1.0", "R7" }; //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Constant for 'artifactId' element in POM.xml.
     */
    private static final String POM_ELT_ARTIFACT_ID = "artifactId"; //$NON-NLS-1$

    /**
     * Constant for 'groupId' element in POM.xml.
     */
    private static final String POM_ELT_GROUP_ID = "groupId"; //$NON-NLS-1$

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
     * If the executed project is a reactor project, this will contains the full
     * list of projects in the reactor.
     * 
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    /**
     * Artifact resolver, needed to download source jars for inclusion in
     * classpath.
     * 
     * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
     * @required
     * @readonly
     */
    private ArtifactResolver artifactResolver;

    /**
     * Artifact factory, needed to download source jars for inclusion in
     * classpath.
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
     * List of eclipse project natures. By default the
     * <code>org.eclipse.jdt.core.javanature</code> nature plus the needed WTP
     * natures are added.
     * 
     * <pre>
     * &lt;projectnatures&gt;
     *    &lt;projectnature&gt;org.eclipse.jdt.core.javanature&lt;/projectnature&gt;
     *    &lt;projectnature&gt;org.eclipse.wst.common.modulecore.ModuleCoreNature&lt;/projectnature&gt;
     * &lt;/projectnatures&gt;
     * </pre>
     * 
     * @parameter
     */
    private List projectnatures;

    /**
     * List of eclipse build commands. By default the <code>org.eclipse.jdt.core.javabuilder</code> builder plus the needed
     * WTP builders are added. Configuration example:
     * 
     * <pre>
     * &lt;buildcommands&gt;
     *    &lt;java.lang.String&gt;org.eclipse.wst.common.modulecore.ComponentStructuralBuilder&lt;/java.lang.String&gt;
     *    &lt;java.lang.String&gt;org.eclipse.jdt.core.javabuilder&lt;/java.lang.String&gt;
     *    &lt;java.lang.String&gt;org.eclipse.wst.common.modulecore.ComponentStructuralBuilderDependencyResolver&lt;/java.lang.String&gt;
     * &lt;/buildcommands&gt;
     * </pre>
     * 
     * @parameter
     */
    private List buildcommands;

    /**
     * List of container classpath entries. By default the <code>org.eclipse.jdt.launching.JRE_CONTAINER</code> classpath
     * container is added. Configuration example: 
     * <pre>
     * &lt;classpathContainers&gt;
     *    &lt;buildcommand&gt;org.eclipse.jdt.launching.JRE_CONTAINER&lt;/buildcommand&gt;
     *    &lt;buildcommand&gt;org.eclipse.jst.server.core.container/org.eclipse.jst.server.tomcat.runtimeTarget/Apache Tomcat v5.5&lt;/buildcommand&gt;
     *    &lt;buildcommand&gt;org.eclipse.jst.j2ee.internal.web.container/artifact&lt;/buildcommand&gt;
     * &lt;/classpathContainers&gt;
     * </pre>
     * 
     * @parameter
     */
    private List classpathContainers;

    /**
     * Enables/disables the downloading of source attachments. Defaults to
     * false.
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
     * When set to false, the plugin will not create sub-projects and instead
     * reference those sub-projects using the installed package in the local
     * repository
     * 
     * @parameter expression="${eclipse.useProjectReferences}"
     *            default-value="true"
     * @required
     */
    private boolean useProjectReferences;

    /**
     * The default output directory
     * 
     * @parameter expression="${project.build.outputDirectory}"
     *            alias="outputDirectory"
     */
    private File buildOutputDirectory;

    /**
     * The version of WTP for which configuration files will be generated.
     * The default value is "R7", supported versions are "R7" and "1.0"
     * 
     * @parameter expression="${wtpversion}" default-value="R7"
     */
    private String wtpversion;

    /**
     * Not a plugin parameter. Collect missing source artifact for the final
     * report.
     */
    private List missingSourceArtifacts = new ArrayList();

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        if ( Arrays.binarySearch( WTP_SUPPORTED_VERSIONS, wtpversion ) < 0 )
        {
            throw new MojoExecutionException( Messages
                .getString( "EclipsePlugin.unsupportedwtp", new Object[] { //$NON-NLS-1$
                            wtpversion, StringUtils.join( WTP_SUPPORTED_VERSIONS, " " ) } ) ); //$NON-NLS-1$
        }

        if ( executedProject == null )
        {
            // backwards compat with alpha-2 only
            executedProject = project;
        }

        String packaging = executedProject.getPackaging();

        // validate sanity of the current m2 project
        assertNotEmpty( executedProject.getGroupId(), POM_ELT_GROUP_ID ); //$NON-NLS-1$
        assertNotEmpty( executedProject.getArtifactId(), POM_ELT_ARTIFACT_ID ); //$NON-NLS-1$

        if ( executedProject.getFile() == null || !executedProject.getFile().exists() )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.missingpom" ) ); //$NON-NLS-1$
        }

        if ( "pom".equals( packaging ) && eclipseProjectDir == null ) //$NON-NLS-1$
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

        // end validate

        // defaults
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
        else if ( !classpathContainers.contains( COMMON_PATH_JDT_LAUNCHING_JRE_CONTAINER ) )
        {
            getLog()
                .warn(
                       "You did specify a list of classpath containers without the base org.eclipse.jdt.launching.JRE_CONTAINER.\n"
                           + "If you specify custom classpath containers you should also add org.eclipse.jdt.launching.JRE_CONTAINER to the list" );
            classpathContainers.add( 0, COMMON_PATH_JDT_LAUNCHING_JRE_CONTAINER );
        }

        // end defaults

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

        // build a list of UNIQUE source dirs (both src and resources) to be
        // used in classpath and wtpmodules
        EclipseSourceDir[] sourceDirs = EclipseUtils.buildDirectoryList( executedProject, eclipseProjectDir, getLog(),
                                                                         buildOutputDirectory );

        Collection artifacts = prepareArtifacts();

        downloadSourceArtifacts( artifacts, reactorArtifacts );

        if ( "R7".equalsIgnoreCase( wtpversion ) ) //$NON-NLS-1$
        {
            new EclipseWtpmodulesWriter( getLog(), eclipseProjectDir, project, artifacts ).write( reactorArtifacts,
                                                                                                  sourceDirs,
                                                                                                  localRepository,
                                                                                                  buildOutputDirectory );
        }
        else if ( "1.0".equals( wtpversion ) ) //$NON-NLS-1$
        {
            // Check and write out a WTP Project if this was required.
            if ( "war".equalsIgnoreCase( project.getPackaging() ) || "ear".equalsIgnoreCase( project.getPackaging() ) //$NON-NLS-1$ //$NON-NLS-2$
                || "ejb".equalsIgnoreCase( project.getPackaging() ) ) //$NON-NLS-1$
            {
                // we assume we have a version 1.0 for WTP
                getLog().info( "Generating Eclipse web facet assuming version 1.0 for WTP..." );
                new EclipseWtpSettingsWriter( getLog(), eclipseProjectDir, project, artifacts )
                    .write( reactorArtifacts, sourceDirs, localRepository, buildOutputDirectory );
            }
        }

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

        projectnatures.add( NATURE_JEM_WORKBENCH_JAVA_EMF ); // WTP nature

        projectnatures.add( NATURE_JDT_CORE_JAVA );

        projectnatures.add( NATURE_WST_MODULE_CORE_NATURE ); // WTP nature
        if ( !"R7".equalsIgnoreCase( wtpversion ) ) //$NON-NLS-1$
        {
            projectnatures.add( NATURE_WST_FACET_CORE_NATURE ); // WTP nature
        }
    }

    private void fillDefaultClasspathContainers( String packaging )
    {
        classpathContainers = new ArrayList();
        classpathContainers.add( COMMON_PATH_JDT_LAUNCHING_JRE_CONTAINER );
    }

    private void fillDefaultBuilders( String packaging )
    {
        buildcommands = new ArrayList();

        buildcommands.add( BUILDER_WST_COMPONENT_STRUCTURAL ); // WTP builder

        buildcommands.add( BUILDER_JDT_CORE_JAVA );

        buildcommands.add( BUILDER_WST_VALIDATION ); // WTP builder
        buildcommands.add( BUILDER_WST_COMPONENT_STRUCTURAL_DEPENDENCY_RESOLVER ); // WTP builder
    }

    private void downloadSourceArtifacts( Collection artifacts, Collection reactorArtifacts )
        throws MojoExecutionException
    {
        // if downloadSources is off, just check local repository for reporting
        // missing jars
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
            Artifact sourceArtifact = EclipseUtils.resolveArtifactWithClassifier( artifact, "sources", localRepository, //$NON-NLS-1$
                                                                                  artifactResolver, artifactFactory,
                                                                                  remoteRepos );

            if ( !sourceArtifact.isResolved() )
            {
                // try using a plain javadoc jar if the source jar is not available
                EclipseUtils.resolveArtifactWithClassifier( artifact, "javadoc", localRepository, artifactResolver, //$NON-NLS-1$
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
            msg.append( Messages.getString( "EclipseClasspathWriter.sourcesmissingitem", art.getId() ) ); //$NON-NLS-1$
        }
        msg.append( "\n" ); //$NON-NLS-1$

        getLog().info( msg ); //$NON-NLS-1$

    }

    public ArtifactFactory getArtifactFactory()
    {
        return artifactFactory;
    }

    public void setArtifactFactory( ArtifactFactory artifactFactory )
    {
        this.artifactFactory = artifactFactory;
    }

    public ArtifactResolver getArtifactResolver()
    {
        return artifactResolver;
    }

    public void setArtifactResolver( ArtifactResolver artifactResolver )
    {
        this.artifactResolver = artifactResolver;
    }

    public List getBuildcommands()
    {
        return buildcommands;
    }

    public void setBuildcommands( List buildcommands )
    {
        this.buildcommands = buildcommands;
    }

    public File getBuildOutputDirectory()
    {
        return buildOutputDirectory;
    }

    public void setBuildOutputDirectory( File buildOutputDirectory )
    {
        this.buildOutputDirectory = buildOutputDirectory;
    }

    public List getClasspathContainers()
    {
        return classpathContainers;
    }

    public void setClasspathContainers( List classpathContainers )
    {
        this.classpathContainers = classpathContainers;
    }

    public boolean isDownloadSources()
    {
        return downloadSources;
    }

    public void setDownloadSources( boolean downloadSources )
    {
        this.downloadSources = downloadSources;
    }

    public File getEclipseProjectDir()
    {
        return eclipseProjectDir;
    }

    public void setEclipseProjectDir( File eclipseProjectDir )
    {
        this.eclipseProjectDir = eclipseProjectDir;
    }

    public MavenProject getExecutedProject()
    {
        return executedProject;
    }

    public void setExecutedProject( MavenProject executedProject )
    {
        this.executedProject = executedProject;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    public List getMissingSourceArtifacts()
    {
        return missingSourceArtifacts;
    }

    public void setMissingSourceArtifacts( List missingSourceArtifacts )
    {
        this.missingSourceArtifacts = missingSourceArtifacts;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public List getProjectnatures()
    {
        return projectnatures;
    }

    public void setProjectnatures( List projectnatures )
    {
        this.projectnatures = projectnatures;
    }

    public List getReactorProjects()
    {
        return reactorProjects;
    }

    public void setReactorProjects( List reactorProjects )
    {
        this.reactorProjects = reactorProjects;
    }

    public List getRemoteArtifactRepositories()
    {
        return remoteArtifactRepositories;
    }

    public void setRemoteArtifactRepositories( List remoteArtifactRepositories )
    {
        this.remoteArtifactRepositories = remoteArtifactRepositories;
    }

    public boolean isUseProjectReferences()
    {
        return useProjectReferences;
    }

    public void setUseProjectReferences( boolean useProjectReferences )
    {
        this.useProjectReferences = useProjectReferences;
    }

    public String getWtpversion()
    {
        return wtpversion;
    }

    public void setWtpversion( String wtpversion )
    {
        this.wtpversion = wtpversion;
    }

}
