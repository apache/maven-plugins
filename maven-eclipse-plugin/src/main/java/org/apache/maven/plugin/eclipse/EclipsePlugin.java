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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.writers.EclipseClasspathWriter;
import org.apache.maven.plugin.eclipse.writers.EclipseOSGiManifestWriter;
import org.apache.maven.plugin.eclipse.writers.EclipseProjectWriter;
import org.apache.maven.plugin.eclipse.writers.EclipseSettingsWriter;
import org.apache.maven.plugin.eclipse.writers.EclipseWriterConfig;
import org.apache.maven.plugin.eclipse.writers.EclipseWtpComponent15Writer;
import org.apache.maven.plugin.eclipse.writers.EclipseWtpComponentWriter;
import org.apache.maven.plugin.eclipse.writers.EclipseWtpFacetsWriter;
import org.apache.maven.plugin.eclipse.writers.EclipseWtpmodulesWriter;
import org.apache.maven.plugin.ide.AbstractIdeSupportMojo;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Generates the following eclipse configuration files:
 * <ul>
 *   <li><code>.project</code> and <code>.classpath</code> files</li>
 *   <li><code>.setting/org.eclipse.jdt.core.prefs</code> with project specific compiler settings</li>
 *   <li>various configuration files for WTP (Web Tools Project), if the parameter <code>wtpversion</code> is set to a
 *   valid version (WTP configuration is not generated by default)</li>
 * </ul>
 * If this goal is run on a multiproject root, dependencies between modules will be configured as direct project
 * dependencies in Eclipse (unless <code>useProjectReferences</code> is set to <code>false</code>).
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:fgiust@apache.org">Fabrizio Giustina</a>
 * @version $Id$
 * @goal eclipse
 * @execute phase="generate-resources"
 */
public class EclipsePlugin
    extends AbstractIdeSupportMojo
{

    private static final String NATURE_WST_FACET_CORE_NATURE = "org.eclipse.wst.common.project.facet.core.nature"; //$NON-NLS-1$

    private static final String BUILDER_WST_COMPONENT_STRUCTURAL_DEPENDENCY_RESOLVER = "org.eclipse.wst.common.modulecore.ComponentStructuralBuilderDependencyResolver"; //$NON-NLS-1$

    private static final String BUILDER_WST_VALIDATION = "org.eclipse.wst.validation.validationbuilder"; //$NON-NLS-1$

    private static final String BUILDER_JDT_CORE_JAVA = "org.eclipse.jdt.core.javabuilder"; //$NON-NLS-1$

    private static final String BUILDER_WST_COMPONENT_STRUCTURAL = "org.eclipse.wst.common.modulecore.ComponentStructuralBuilder"; //$NON-NLS-1$

    private static final String BUILDER_WST_FACET = "org.eclipse.wst.common.project.facet.core.builder"; //$NON-NLS-1$

    private static final String BUILDER_PDE_MANIFEST = "org.eclipse.pde.ManifestBuilder"; //$NON-NLS-1$

    private static final String BUILDER_PDE_SCHEMA = "org.eclipse.pde.SchemaBuilder"; //$NON-NLS-1$

    private static final String NATURE_WST_MODULE_CORE_NATURE = "org.eclipse.wst.common.modulecore.ModuleCoreNature"; //$NON-NLS-1$

    private static final String NATURE_JDT_CORE_JAVA = "org.eclipse.jdt.core.javanature"; //$NON-NLS-1$

    private static final String NATURE_JEM_WORKBENCH_JAVA_EMF = "org.eclipse.jem.workbench.JavaEMFNature"; //$NON-NLS-1$

    private static final String NATURE_PDE_PLUGIN = "org.eclipse.pde.PluginNature"; //$NON-NLS-1$

    private static final String COMMON_PATH_JDT_LAUNCHING_JRE_CONTAINER = "org.eclipse.jdt.launching.JRE_CONTAINER"; //$NON-NLS-1$

    private static final String REQUIRED_PLUGINS_CONTAINER = "org.eclipse.pde.core.requiredPlugins"; //$NON-NLS-1$  

    //  warning, order is important for binary search
    public static final String[] WTP_SUPPORTED_VERSIONS = new String[] { "1.0", "1.5", "R7", "none" }; //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$

    /**
     * Constant for 'artifactId' element in POM.xml.
     */
    private static final String POM_ELT_ARTIFACT_ID = "artifactId"; //$NON-NLS-1$

    /**
     * Constant for 'groupId' element in POM.xml.
     */
    private static final String POM_ELT_GROUP_ID = "groupId"; //$NON-NLS-1$

    /**
     * List of eclipse project natures. By default the
     * <code>org.eclipse.jdt.core.javanature</code> nature plus the needed WTP
     * natures are added. Natures added using this property <strong>replace</strong> the default list.
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
     * List of eclipse project natures to be added to the default ones.
     * 
     * <pre>
     * &lt;additionalProjectnatures&gt;
     *    &lt;projectnature&gt;org.springframework.ide.eclipse.core.springnature&lt;/projectnature&gt;
     * &lt;/additionalProjectnatures&gt;
     * </pre>
     * 
     * @parameter
     */
    private List additionalProjectnatures;

    /**
     * List of eclipse build commands. By default the <code>org.eclipse.jdt.core.javabuilder</code> builder plus the needed
     * WTP builders are added. Configuration example:
     * 
     * <pre>
     * &lt;buildcommands&gt;
     *    &lt;buildcommand&gt;org.eclipse.wst.common.modulecore.ComponentStructuralBuilder&lt;/buildcommand&gt;
     *    &lt;buildcommand&gt;org.eclipse.jdt.core.javabuilder&lt;/buildcommand&gt;
     *    &lt;buildcommand&gt;org.eclipse.wst.common.modulecore.ComponentStructuralBuilderDependencyResolver&lt;/buildcommand&gt;
     * &lt;/buildcommands&gt;
     * </pre>
     * 
     * @parameter
     */
    private List buildcommands;

    /**
     * List of eclipse build commands to be added to the default ones.
     * 
     * <pre>
     * &lt;additionalBuildcommands&gt;
     *    &lt;buildcommand&gt;org.springframework.ide.eclipse.core.springbuilder&lt;/buildcommand&gt;
     * &lt;/additionalBuildcommands&gt;
     * </pre>
     * 
     * @parameter
     */
    private List additionalBuildcommands;

    /**
     * List of container classpath entries. By default the <code>org.eclipse.jdt.launching.JRE_CONTAINER</code> classpath
     * container is added. Configuration example: 
     * <pre>
     * &lt;classpathContainers&gt;
     *    &lt;classpathContainer&gt;org.eclipse.jdt.launching.JRE_CONTAINER&lt;/classpathContainer&gt;
     *    &lt;classpathContainer&gt;org.eclipse.jst.server.core.container/org.eclipse.jst.server.tomcat.runtimeTarget/Apache Tomcat v5.5&lt;/classpathContainer&gt;
     *    &lt;classpathContainer&gt;org.eclipse.jst.j2ee.internal.web.container/artifact&lt;/classpathContainer&gt;
     * &lt;/classpathContainers&gt;
     * </pre>
     * 
     * @parameter
     */
    private List classpathContainers;

    /**
     * Enables/disables the downloading of source attachments. Defaults to false.
     *
     * @parameter expression="${eclipse.downloadSources}"
     * @deprecated use downloadSources
     */
    private boolean eclipseDownloadSources;

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
     * @parameter expression="${eclipse.useProjectReferences}" default-value="true"
     * @required
     */
    private boolean useProjectReferences;

    /**
     * The default output directory
     * 
     * @parameter expression="${outputDirectory}" alias="outputDirectory" default-value="${project.build.outputDirectory}"
     * @required
     */
    private File buildOutputDirectory;

    /**
     * The version of WTP for which configuration files will be generated.
     * The default value is "none" (don't generate WTP configuration), supported versions are "R7" and "1.0"
     * 
     * @parameter expression="${wtpversion}" default-value="none"
     */
    private String wtpversion;

    /**
     * Is it an PDE project? If yes, the plugin adds the necessary natures and build commands to
     * the .project file. Additionally it copies all libraries to a project local directory and
     * references them instead of referencing the files in the local Maven repository. It also
     * ensured that the "Bundle-Classpath" in META-INF/MANIFEST.MF is synchronized.
     * 
     * @parameter expression="${eclipse.pde}" default-value="false"
     */
    private boolean pde;

    /**
     * The relative path of the manifest file
     * 
     * @parameter expression="${eclipse.manifest}" default-value="${basedir}/META-INF/MANIFEST.MF"
     */
    private File manifest;

    /**
     * Allow to configure additional generic configuration files for eclipse that will be written out to disk when
     * running eclipse:eclipse. FOr each file you can specify the name and the text content.
     * 
     * <pre>
     * &lt;additionalConfig&gt;
     *    &lt;file&gt;
     *      &lt;name&gt;.checkstyle&lt;/name&gt;
     *      &lt;content&gt;
     *        &lt;![CDATA[&lt;fileset-config file-format-version="1.2.0" simple-config="true"&gt;
     *          &lt;fileset name="all" enabled="true" check-config-name="acme corporate style" local="false"&gt;
     *              &lt;file-match-pattern match-pattern="." include-pattern="true"/&gt;
     *          &lt;/fileset&gt;
     *          &lt;filter name="NonSrcDirs" enabled="true"/&gt;
     *        &lt;/fileset-config&gt;]]&gt;
     *      &lt;/content&gt;
     *    &lt;/file&gt;
     * &lt;/additionalConfig&gt;
     * </pre>
     * 
     * @parameter
     */
    private EclipseConfigFile[] additionalConfig;

    /**
     * Parsed wtp version.
     */
    private float wtpVersionFloat;

    /**
     * Not a plugin parameter. Is this a java project?
     */
    private boolean isJavaProject;

    /**
     * Getter for <code>buildcommands</code>.
     * @return Returns the buildcommands.
     */
    public List getBuildcommands()
    {
        return this.buildcommands;
    }

    /**
     * Setter for <code>buildcommands</code>.
     * @param buildcommands The buildcommands to set.
     */
    public void setBuildcommands( List buildcommands )
    {
        this.buildcommands = buildcommands;
    }

    /**
     * Getter for <code>buildOutputDirectory</code>.
     * @return Returns the buildOutputDirectory.
     */
    public File getBuildOutputDirectory()
    {
        return this.buildOutputDirectory;
    }

    /**
     * Setter for <code>buildOutputDirectory</code>.
     * @param buildOutputDirectory The buildOutputDirectory to set.
     */
    public void setBuildOutputDirectory( File buildOutputDirectory )
    {
        this.buildOutputDirectory = buildOutputDirectory;
    }

    /**
     * Getter for <code>classpathContainers</code>.
     * @return Returns the classpathContainers.
     */
    public List getClasspathContainers()
    {
        return this.classpathContainers;
    }

    /**
     * Setter for <code>classpathContainers</code>.
     * @param classpathContainers The classpathContainers to set.
     */
    public void setClasspathContainers( List classpathContainers )
    {
        this.classpathContainers = classpathContainers;
    }

    /**
     * Getter for <code>eclipseProjectDir</code>.
     * @return Returns the eclipseProjectDir.
     */
    public File getEclipseProjectDir()
    {
        return this.eclipseProjectDir;
    }

    /**
     * Setter for <code>eclipseProjectDir</code>.
     * @param eclipseProjectDir The eclipseProjectDir to set.
     */
    public void setEclipseProjectDir( File eclipseProjectDir )
    {
        this.eclipseProjectDir = eclipseProjectDir;
    }

    /**
     * Getter for <code>projectnatures</code>.
     * @return Returns the projectnatures.
     */
    public List getProjectnatures()
    {
        return this.projectnatures;
    }

    /**
     * Setter for <code>projectnatures</code>.
     * @param projectnatures The projectnatures to set.
     */
    public void setProjectnatures( List projectnatures )
    {
        this.projectnatures = projectnatures;
    }

    /**
     * Getter for <code>useProjectReferences</code>.
     * @return Returns the useProjectReferences.
     */
    public boolean getUseProjectReferences()
    {
        return this.useProjectReferences;
    }

    /**
     * Setter for <code>useProjectReferences</code>.
     * @param useProjectReferences The useProjectReferences to set.
     */
    public void setUseProjectReferences( boolean useProjectReferences )
    {
        this.useProjectReferences = useProjectReferences;
    }

    /**
     * Getter for <code>wtpversion</code>.
     * @return Returns the wtpversion.
     */
    public String getWtpversion()
    {
        return this.wtpversion;
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
     * Getter for <code>additionalBuildcommands</code>.
     * @return Returns the additionalBuildcommands.
     */
    public List getAdditionalBuildcommands()
    {
        return this.additionalBuildcommands;
    }

    /**
     * Setter for <code>additionalBuildcommands</code>.
     * @param additionalBuildcommands The additionalBuildcommands to set.
     */
    public void setAdditionalBuildcommands( List additionalBuildcommands )
    {
        this.additionalBuildcommands = additionalBuildcommands;
    }

    /**
     * Getter for <code>additionalProjectnatures</code>.
     * @return Returns the additionalProjectnatures.
     */
    public List getAdditionalProjectnatures()
    {
        return this.additionalProjectnatures;
    }

    /**
     * Setter for <code>additionalProjectnatures</code>.
     * @param additionalProjectnatures The additionalProjectnatures to set.
     */
    public void setAdditionalProjectnatures( List additionalProjectnatures )
    {
        this.additionalProjectnatures = additionalProjectnatures;
    }

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public boolean setup()
        throws MojoExecutionException
    {

        if ( eclipseDownloadSources )
        {
            // deprecated warning
            getLog().warn( Messages.getString( "EclipsePlugin.deprecatedpar", new Object[] { //$NON-NLS-1$
                                               "eclipse.downloadSources", //$NON-NLS-1$
                                                   "downloadSources" } ) ); //$NON-NLS-1$
            downloadSources = true;
        }

        if ( Arrays.binarySearch( WTP_SUPPORTED_VERSIONS, wtpversion ) < 0 )
        {
            throw new MojoExecutionException( Messages
                .getString( "EclipsePlugin.unsupportedwtp", new Object[] { //$NON-NLS-1$
                            wtpversion, StringUtils.join( WTP_SUPPORTED_VERSIONS, " " ) } ) ); //$NON-NLS-1$
        }

        if ( "R7".equalsIgnoreCase( wtpversion ) ) //$NON-NLS-1$
        {
            wtpVersionFloat = 0.7f;
        }
        else if ( "1.0".equalsIgnoreCase( wtpversion ) ) //$NON-NLS-1$
        {
            wtpVersionFloat = 1.0f;
        }
        else if ( "1.5".equalsIgnoreCase( wtpversion ) ) //$NON-NLS-1$
        {
            wtpVersionFloat = 1.5f;
        }
        if ( !"none".equalsIgnoreCase( wtpversion ) )
        {
            getLog().info( Messages.getString( "EclipsePlugin.wtpversion", wtpversion ) );
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
            return false;
        }

        if ( "eclipse-plugin".equals( packaging ) )
        {
            pde = true;
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

        ArtifactHandler artifactHandler = this.project.getArtifact().getArtifactHandler();
        // ear projects don't contain java sources
        isJavaProject = "java".equals( artifactHandler.getLanguage() ) && !"ear".equals( packaging );

        // defaults
        if ( projectnatures == null )
        {
            fillDefaultNatures( packaging );
        }

        if ( additionalProjectnatures != null )
        {
            projectnatures.addAll( additionalProjectnatures );
        }

        if ( buildcommands == null )
        {
            fillDefaultBuilders( packaging );
        }

        if ( additionalBuildcommands != null )
        {
            buildcommands.addAll( additionalBuildcommands );
        }

        if ( classpathContainers == null )
        {
            fillDefaultClasspathContainers( packaging );
        }
        else if ( !classpathContainers.contains( COMMON_PATH_JDT_LAUNCHING_JRE_CONTAINER ) ) //$NON-NLS-1$
        {
            getLog().warn( Messages.getString( "EclipsePlugin.missingjrecontainer" ) ); //$NON-NLS-1$
            classpathContainers.add( 0, COMMON_PATH_JDT_LAUNCHING_JRE_CONTAINER );
        }

        // ready to start
        return true;
    }

    public void writeConfiguration( IdeDependency[] deps )
        throws MojoExecutionException
    {
        File projectBaseDir = executedProject.getFile().getParentFile();

        // build a list of UNIQUE source dirs (both src and resources) to be
        // used in classpath and wtpmodules
        EclipseSourceDir[] sourceDirs = buildDirectoryList( executedProject, eclipseProjectDir, buildOutputDirectory );

        EclipseWriterConfig config = new EclipseWriterConfig();
        config.setBuildCommands( buildcommands );
        config.setBuildOutputDirectory( buildOutputDirectory );
        config.setClasspathContainers( classpathContainers );
        config.setDeps( deps );
        config.setEclipseProjectDirectory( eclipseProjectDir );
        config.setLocalRepository( localRepository );
        config.setManifestFile( manifest );
        config.setPde( pde );
        config.setProject( project );
        config.setProjectBaseDir( projectBaseDir );
        config.setProjectnatures( projectnatures );
        config.setSourceDirs( sourceDirs );

        if ( wtpVersionFloat == 0.7f )
        {
            new EclipseWtpmodulesWriter().init( getLog(), config ).write();
        }

        if ( wtpVersionFloat >= 1.0f )
        {
            new EclipseWtpFacetsWriter().init( getLog(), config ).write();
        }
        if ( wtpVersionFloat == 1.0f )
        {

            new EclipseWtpComponentWriter().init( getLog(), config ).write();
        }
        if ( wtpVersionFloat >= 1.5 )
        {
            new EclipseWtpComponent15Writer().init( getLog(), config ).write();
        }

        new EclipseProjectWriter().init( getLog(), config ).write();

        new EclipseSettingsWriter().init( getLog(), config ).write();

        if ( isJavaProject )
        {
            new EclipseClasspathWriter().init( getLog(), config ).write();
        }

        if ( pde )
        {
            this.getLog().info( "The Maven Eclipse plugin runs in 'pde'-mode." );
            new EclipseOSGiManifestWriter().init( getLog(), config ).write();
        }

        if ( additionalConfig != null )
        {
            for ( int j = 0; j < additionalConfig.length; j++ )
            {
                EclipseConfigFile file = additionalConfig[j];
                File projectRelativeFile = new File( this.eclipseProjectDir, file.getName() );
                if ( projectRelativeFile.isDirectory() )
                {
                    // just ignore?
                    getLog().warn( Messages.getString( "EclipsePlugin.foundadir", //$NON-NLS-1$
                                                       projectRelativeFile.getAbsolutePath() ) );
                }

                try
                {
                    FileUtils.fileWrite( projectRelativeFile.getAbsolutePath(), file.getContent() );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( Messages.getString( "EclipsePlugin.cantwritetofile", //$NON-NLS-1$
                                                                          projectRelativeFile.getAbsolutePath() ) );
                }
            }
        }

        getLog().info( Messages.getString( "EclipsePlugin.wrote", new Object[] { //$NON-NLS-1$
                                           project.getArtifactId(), eclipseProjectDir.getAbsolutePath() } ) );
    }

    private void assertNotEmpty( String string, String elementName )
        throws MojoExecutionException
    {
        if ( string == null )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.missingelement", elementName ) ); //$NON-NLS-1$
        }
    }

    private void fillDefaultNatures( String packaging )
    {
        projectnatures = new ArrayList();

        if ( wtpVersionFloat >= 1.0f )
        {
            projectnatures.add( NATURE_WST_FACET_CORE_NATURE ); // WTP 1.0 nature
        }

        if ( isJavaProject )
        {
            projectnatures.add( NATURE_JDT_CORE_JAVA );
        }

        if ( wtpVersionFloat >= 0.7f )
        {
            projectnatures.add( NATURE_WST_MODULE_CORE_NATURE ); // WTP 0.7/1.0 nature

            if ( isJavaProject )
            {
                projectnatures.add( NATURE_JEM_WORKBENCH_JAVA_EMF ); // WTP 0.7/1.0 nature
            }
        }

        if ( pde )
        {
            projectnatures.add( NATURE_PDE_PLUGIN );
        }

    }

    private void fillDefaultClasspathContainers( String packaging )
    {
        classpathContainers = new ArrayList();
        classpathContainers.add( COMMON_PATH_JDT_LAUNCHING_JRE_CONTAINER );

        if ( pde )
        {
            classpathContainers.add( REQUIRED_PLUGINS_CONTAINER );
        }
    }

    private void fillDefaultBuilders( String packaging )
    {
        buildcommands = new ArrayList();

        if ( wtpVersionFloat == 0.7f )
        {
            buildcommands.add( BUILDER_WST_COMPONENT_STRUCTURAL ); // WTP 0.7 builder
        }

        if ( isJavaProject )
        {
            buildcommands.add( BUILDER_JDT_CORE_JAVA );
        }

        if ( wtpVersionFloat >= 1.5f )
        {
            buildcommands.add( BUILDER_WST_FACET ); // WTP 1.5 builder
        }

        if ( wtpVersionFloat >= 0.7f )
        {
            buildcommands.add( BUILDER_WST_VALIDATION ); // WTP 0.7/1.0 builder
        }

        if ( wtpVersionFloat == 0.7f )
        {
            buildcommands.add( BUILDER_WST_COMPONENT_STRUCTURAL_DEPENDENCY_RESOLVER ); // WTP 0.7 builder
        }

        if ( pde )
        {
            buildcommands.add( BUILDER_PDE_MANIFEST );
            buildcommands.add( BUILDER_PDE_SCHEMA );
        }
    }

    public EclipseSourceDir[] buildDirectoryList( MavenProject project, File basedir, File buildOutputDirectory )
        throws MojoExecutionException
    {
        File projectBaseDir = project.getFile().getParentFile();

        // avoid duplicated entries
        Set directories = new TreeSet();

        extractSourceDirs( directories, project.getCompileSourceRoots(), basedir, projectBaseDir, false, null );

        String relativeOutput = IdeUtils.toRelativeAndFixSeparator( projectBaseDir, buildOutputDirectory, false );

        extractResourceDirs( directories, project.getBuild().getResources(), project, basedir, projectBaseDir, false,
                             relativeOutput );

        // If using the standard output location, don't mix the test output into it.
        String testOutput = null;
        boolean useFixedOutputDir = !buildOutputDirectory.equals( new File( project.getBuild().getOutputDirectory() ) );
        if ( !useFixedOutputDir )
        {
            testOutput = IdeUtils.toRelativeAndFixSeparator( projectBaseDir, new File( project.getBuild()
                .getTestOutputDirectory() ), false );
        }

        extractSourceDirs( directories, project.getTestCompileSourceRoots(), basedir, projectBaseDir, true, testOutput );

        extractResourceDirs( directories, project.getBuild().getTestResources(), project, basedir, projectBaseDir,
                             true, testOutput );

        return (EclipseSourceDir[]) directories.toArray( new EclipseSourceDir[directories.size()] );
    }

    private static void extractSourceDirs( Set directories, List sourceRoots, File basedir, File projectBaseDir,
                                           boolean test, String output )
        throws MojoExecutionException
    {
        for ( Iterator it = sourceRoots.iterator(); it.hasNext(); )
        {

            File sourceRootFile = new File( (String) it.next() );

            if ( sourceRootFile.isDirectory() )
            {
                String sourceRoot = IdeUtils.toRelativeAndFixSeparator( projectBaseDir, sourceRootFile, !projectBaseDir
                    .equals( basedir ) );

                directories.add( new EclipseSourceDir( sourceRoot, output, true, test, null, null ) );
            }
        }
    }

    private void extractResourceDirs( Set directories, List resources, MavenProject project, File basedir,
                                      File projectBaseDir, boolean test, String output )
        throws MojoExecutionException
    {
        for ( Iterator it = resources.iterator(); it.hasNext(); )
        {
            Resource resource = (Resource) it.next();
            String includePattern = null;
            String excludePattern = null;

            if ( resource.getIncludes().size() != 0 )
            {
                // @todo includePattern = ?
                getLog().warn( Messages.getString( "EclipsePlugin.includenotsupported" ) ); //$NON-NLS-1$
            }

            if ( resource.getExcludes().size() != 0 )
            {
                // @todo excludePattern = ?
                getLog().warn( Messages.getString( "EclipsePlugin.excludenotsupported" ) ); //$NON-NLS-1$
            }

            // Example of setting include/exclude patterns for future reference.
            //
            // TODO: figure out how to merge if the same dir is specified twice
            // with different in/exclude patterns. We can't write them now,
            // since only the the first one would be included.
            //
            // if ( resource.getIncludes().size() != 0 )
            // {
            // writer.addAttribute(
            // "including", StringUtils.join( resource.getIncludes().iterator(),
            // "|" )
            // );
            // }
            //
            // if ( resource.getExcludes().size() != 0 )
            // {
            // writer.addAttribute(
            // "excluding", StringUtils.join( resource.getExcludes().iterator(),
            // "|" )
            // );
            // }

            File resourceDirectory = new File( resource.getDirectory() );

            if ( !resourceDirectory.exists() || !resourceDirectory.isDirectory() )
            {
                continue;
            }

            String resourceDir = IdeUtils.toRelativeAndFixSeparator( projectBaseDir, resourceDirectory, !projectBaseDir
                .equals( basedir ) );

            if ( output != null )
            {
                File outputFile = new File( projectBaseDir, output );
                // create output dir if it doesn't exist
                outputFile.mkdirs();

                if ( !StringUtils.isEmpty( resource.getTargetPath() ) )
                {
                    outputFile = new File( outputFile, resource.getTargetPath() );
                    // create output dir if it doesn't exist
                    outputFile.mkdirs();
                }

                output = IdeUtils.toRelativeAndFixSeparator( projectBaseDir, outputFile, false );
            }

            directories.add( new EclipseSourceDir( resourceDir, output, true, test, includePattern, excludePattern ) );
        }
    }

}
