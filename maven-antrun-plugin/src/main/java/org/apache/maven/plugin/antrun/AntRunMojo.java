package org.apache.maven.plugin.antrun;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.taskdefs.Typedef;
import org.apache.tools.ant.types.Path;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

/**
 * Maven AntRun Mojo. <br/>
 * This plugin provides the capability of calling Ant tasks from a POM by running the nested Ant tasks inside the
 * &lt;tasks/&gt; parameter. It is encouraged to move the actual tasks to a separate build.xml file and call that file
 * with an &lt;ant/&gt; task.
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
@Mojo( name = "run", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST )
public class AntRunMojo
    extends AbstractMojo
{
    /**
     * The refid used to store the Maven project object in the Ant build.
     */
    public static final String DEFAULT_MAVEN_PROJECT_REFID = "maven.project";

    /**
     * The refid used to store the Maven project object in the Ant build.
     */
    public static final String DEFAULT_MAVEN_PROJECT_HELPER_REFID = "maven.project.helper";

    /**
     * The default target name.
     */
    public static final String DEFAULT_ANT_TARGET_NAME = "main";

    /**
     * The default encoding to use for the generated Ant build.
     */
    public static final String UTF_8 = "UTF-8";

    /**
     * The name used for the Ant target
     */
    private String antTargetName;

    /**
     * The path to The XML file containing the definition of the Maven tasks.
     */
    public static final String ANTLIB = "org/apache/maven/ant/tasks/antlib.xml";

    /**
     * The URI which defines the built in Ant tasks
     */
    public static final String TASK_URI = "antlib:org.apache.maven.ant.tasks";

    /**
     * The Maven project object
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    /**
     * The Maven project helper object
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The plugin dependencies.
     */
    @Parameter( property = "plugin.artifacts", required = true, readonly = true )
    private List<Artifact> pluginArtifacts;

    /**
     * The local Maven repository
     */
    @Parameter( property = "localRepository", readonly = true )
    protected ArtifactRepository localRepository;

    /**
     * String to prepend to project and dependency property names.
     *
     * @since 1.4
     */
    @Parameter( defaultValue = "" )
    private String propertyPrefix;

    /**
     * The xml tag prefix to use for the built in Ant tasks. This prefix needs to be prepended to each task referenced
     * in the antrun target config. For example, a prefix of "mvn" means that the attachartifact task is referenced by
     * "&lt;mvn:attachartifact&gt;" The default value of an empty string means that no prefix is used for the tasks.
     *
     * @since 1.5
     */
    @Parameter( defaultValue = "" )
    private String customTaskPrefix = "";

    /**
     * The name of a property containing the list of all dependency versions. This is used for the removing the versions
     * from the filenames.
     */
    @Parameter( defaultValue = "maven.project.dependencies.versions" )
    private String versionsPropertyName;

    /**
     * The XML for the Ant task. You can add anything you can add between &lt;target&gt; and &lt;/target&gt; in a
     * build.xml.
     *
     * @deprecated Use target instead
     */
    @Parameter
    private PlexusConfiguration tasks;

    /**
     * The XML for the Ant target. You can add anything you can add between &lt;target&gt; and &lt;/target&gt; in a
     * build.xml.
     *
     * @since 1.5
     */
    @Parameter
    private PlexusConfiguration target;

    /**
     * This folder is added to the list of those folders containing source to be compiled. Use this if your Ant script
     * generates source code.
     *
     * @deprecated Use the build-helper-maven-plugin to bind source directories
     */
    @Parameter( property = "sourceRoot" )
    private File sourceRoot;

    /**
     * This folder is added to the list of those folders containing source to be compiled for testing. Use this if your
     * Ant script generates test source code.
     *
     * @deprecated Use the build-helper-maven-plugin to bind test source directories
     */
    @Parameter( property = "testSourceRoot" )
    private File testSourceRoot;

    /**
     * Specifies whether the Antrun execution should be skipped.
     *
     * @since 1.7
     */
    @Parameter( property = "maven.antrun.skip", defaultValue = "false" )
    private boolean skip;

    /**
     * Specifies whether the Ant properties should be propagated to the Maven properties.
     *
     * @since 1.7
     */
    @Parameter( defaultValue = "false" )
    private boolean exportAntProperties;

    /**
     * Specifies whether a failure in the Ant build leads to a failure of the Maven build. If this
     * value is {@code false}, the Maven build will proceed even if the Ant build fails. If it is
     * {@code true}, then the Maven build fails if the Ant build fails.
     *
     * @since 1.7
     */
    @Parameter( defaultValue = "true" )
    private boolean failOnError;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        if ( skip )
        {
            getLog().info( "Skipping Antrun execution" );
            return;
        }

        MavenProject mavenProject = getMavenProject();

        if ( tasks != null )
        {
            getLog().warn( "Parameter tasks is deprecated, use target instead" );
            target = tasks;
        }

        if ( target == null )
        {
            getLog().info( "No Ant target defined - SKIPPED" );
            return;
        }

        if ( propertyPrefix == null )
        {
            propertyPrefix = "";
        }

        try
        {
            Project antProject = new Project();
            File antBuildFile = this.writeTargetToProjectFile();
            ProjectHelper.configureProject( antProject, antBuildFile );
            antProject.init();

            DefaultLogger antLogger = new DefaultLogger();
            antLogger.setOutputPrintStream( System.out );
            antLogger.setErrorPrintStream( System.err );

            if ( getLog().isDebugEnabled() )
            {
                antLogger.setMessageOutputLevel( Project.MSG_DEBUG );
            }
            else if ( getLog().isInfoEnabled() )
            {
                antLogger.setMessageOutputLevel( Project.MSG_INFO );
            }
            else if ( getLog().isWarnEnabled() )
            {
                antLogger.setMessageOutputLevel( Project.MSG_WARN );
            }
            else if ( getLog().isErrorEnabled() )
            {
                antLogger.setMessageOutputLevel( Project.MSG_ERR );
            }
            else
            {
                antLogger.setMessageOutputLevel( Project.MSG_VERBOSE );
            }

            antProject.addBuildListener( antLogger );
            antProject.setBaseDir( mavenProject.getBasedir() );

            Path p = new Path( antProject );
            p.setPath( StringUtils.join( mavenProject.getCompileClasspathElements().iterator(), File.pathSeparator ) );

            /* maven.dependency.classpath it's deprecated as it's equal to maven.compile.classpath */
            antProject.addReference( "maven.dependency.classpath", p );
            antProject.addReference( "maven.compile.classpath", p );

            p = new Path( antProject );
            p.setPath( StringUtils.join( mavenProject.getRuntimeClasspathElements().iterator(), File.pathSeparator ) );
            antProject.addReference( "maven.runtime.classpath", p );

            p = new Path( antProject );
            p.setPath( StringUtils.join( mavenProject.getTestClasspathElements().iterator(), File.pathSeparator ) );
            antProject.addReference( "maven.test.classpath", p );

            /* set maven.plugin.classpath with plugin dependencies */
            antProject.addReference( "maven.plugin.classpath", getPathFromArtifacts( pluginArtifacts, antProject ) );

            antProject.addReference( DEFAULT_MAVEN_PROJECT_REFID, getMavenProject() );
            antProject.addReference( DEFAULT_MAVEN_PROJECT_HELPER_REFID, projectHelper );
            antProject.addReference( "maven.local.repository", localRepository );
            initMavenTasks( antProject );

            // The Ant project needs actual properties vs. using expression evaluator when calling an external build
            // file.
            copyProperties( mavenProject, antProject );

            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "Executing tasks" );
            }

            antProject.executeTarget( antTargetName );

            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "Executed tasks" );
            }

            copyProperties( antProject, mavenProject );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "DependencyResolutionRequiredException: " + e.getMessage(), e );
        }
        catch ( BuildException e )
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "An Ant BuildException has occured: " ).append( e.getMessage() );
            String fragment = findFragment( e );
            if ( fragment != null )
            {
                sb.append( "\n" ).append( fragment );
            }
            if ( !failOnError )
            {
                getLog().info( sb.toString(), e );
                return; // do not register roots.
            }
            else
            {
                throw new MojoExecutionException( sb.toString(), e );
            }
        }
        catch ( Throwable e )
        {
            throw new MojoExecutionException( "Error executing Ant tasks: " + e.getMessage(), e );
        }

        if ( sourceRoot != null )
        {
            getLog().info( "Registering compile source root " + sourceRoot );
            getMavenProject().addCompileSourceRoot( sourceRoot.toString() );
        }

        if ( testSourceRoot != null )
        {
            getLog().info( "Registering compile test source root " + testSourceRoot );
            getMavenProject().addTestCompileSourceRoot( testSourceRoot.toString() );
        }
    }

    /**
     * @param artifacts {@link Artifact} collection.
     * @param antProject {@link Project}
     * @return {@link Path}
     * @throws DependencyResolutionRequiredException In case of a failure.
     *
     */
    public Path getPathFromArtifacts( Collection<Artifact> artifacts, Project antProject )
        throws DependencyResolutionRequiredException
    {
        if ( artifacts == null )
        {
            return new Path( antProject );
        }

        List<String> list = new ArrayList<String>( artifacts.size() );
        for ( Artifact a : artifacts )
        {
            File file = a.getFile();
            if ( file == null )
            {
                throw new DependencyResolutionRequiredException( a );
            }
            list.add( file.getPath() );
        }

        Path p = new Path( antProject );
        p.setPath( StringUtils.join( list.iterator(), File.pathSeparator ) );

        return p;
    }

    /**
     * Copy properties from the Maven project to the Ant project.
     *
     * @param mavenProject {@link MavenProject}
     * @param antProject {@link Project}
     */
    public void copyProperties( MavenProject mavenProject, Project antProject )
    {
        Properties mavenProps = mavenProject.getProperties();
        for ( Map.Entry<?, ?> entry : mavenProps.entrySet() )
        {
            antProject.setProperty( (String) entry.getKey(), (String) entry.getValue() );
        }

        // Set the POM file as the ant.file for the tasks run directly in Maven.
        antProject.setProperty( "ant.file", mavenProject.getFile().getAbsolutePath() );

        // Add some of the common Maven properties
        getLog().debug( "Setting properties with prefix: " + propertyPrefix );
        antProject.setProperty( ( propertyPrefix + "project.groupId" ), mavenProject.getGroupId() );
        antProject.setProperty( ( propertyPrefix + "project.artifactId" ), mavenProject.getArtifactId() );
        antProject.setProperty( ( propertyPrefix + "project.name" ), mavenProject.getName() );
        if ( mavenProject.getDescription() != null )
        {
            antProject.setProperty( ( propertyPrefix + "project.description" ), mavenProject.getDescription() );
        }
        antProject.setProperty( ( propertyPrefix + "project.version" ), mavenProject.getVersion() );
        antProject.setProperty( ( propertyPrefix + "project.packaging" ), mavenProject.getPackaging() );
        antProject.setProperty( ( propertyPrefix + "project.build.directory" ),
                                mavenProject.getBuild().getDirectory() );
        antProject.setProperty( ( propertyPrefix + "project.build.outputDirectory" ),
                                mavenProject.getBuild().getOutputDirectory() );
        antProject.setProperty( ( propertyPrefix + "project.build.testOutputDirectory" ),
                                mavenProject.getBuild().getTestOutputDirectory() );
        antProject.setProperty( ( propertyPrefix + "project.build.sourceDirectory" ),
                                mavenProject.getBuild().getSourceDirectory() );
        antProject.setProperty( ( propertyPrefix + "project.build.testSourceDirectory" ),
                                mavenProject.getBuild().getTestSourceDirectory() );
        antProject.setProperty( ( propertyPrefix + "localRepository" ), localRepository.toString() );
        antProject.setProperty( ( propertyPrefix + "settings.localRepository" ), localRepository.getBasedir() );

        // Add properties for depenedency artifacts
        @SuppressWarnings( "unchecked" )
        Set<Artifact> depArtifacts = mavenProject.getArtifacts();
        for ( Artifact artifact : depArtifacts )
        {
            String propName = artifact.getDependencyConflictId();

            antProject.setProperty( propertyPrefix + propName, artifact.getFile().getPath() );
        }

        // Add a property containing the list of versions for the mapper
        StringBuilder versionsBuffer = new StringBuilder();
        for ( Artifact artifact : depArtifacts )
        {
            versionsBuffer.append( artifact.getVersion() ).append( File.pathSeparator );
        }
        antProject.setProperty( versionsPropertyName, versionsBuffer.toString() );

        // Add properties in deprecated format to depenedency artifacts
        // This should be removed in future versions of the antrun plugin.
        for ( Artifact artifact : depArtifacts )
        {
            String propName = getDependencyArtifactPropertyName( artifact );

            antProject.setProperty( propName, artifact.getFile().getPath() );
        }
    }

    /**
     * Copy properties from the Ant project to the Maven project.
     *
     * @param antProject   not null
     * @param mavenProject not null
     * @since 1.7
     */
    public void copyProperties( Project antProject, MavenProject mavenProject )
    {
        if ( !exportAntProperties )
        {
            return;
        }

        getLog().debug( "Propagated Ant properties to Maven properties" );
        Map<?, ?> antProps = antProject.getProperties();
        Properties mavenProperties = mavenProject.getProperties();

        for ( Map.Entry<?, ?> entry : antProps.entrySet() )
        {
            String key = (String) entry.getKey();
            if ( mavenProperties.getProperty( key ) != null )
            {
                getLog().debug( "Ant property '" + key + "=" + mavenProperties.getProperty( key )
                                    + "' clashs with an existing Maven property, "
                                    + "SKIPPING this Ant property propagation." );
                continue;
            }
            mavenProperties.setProperty( key, (String) entry.getValue() );
        }
    }

    /**
     * Prefix for legacy property format.
     *
     * @deprecated This should only be used for generating the old property format.
     */
    public static final String DEPENDENCY_PREFIX = "maven.dependency.";

    /**
     * Returns a property name for a dependency artifact. The name is in the format
     * maven.dependency.groupId.artifactId[.classifier].type.path
     *
     * @param artifact {@link Artifact}
     * @return property name
     * @deprecated The dependency conflict ID should be used as the property name.
     */
    public static String getDependencyArtifactPropertyName( Artifact artifact )
    {
        return DEPENDENCY_PREFIX + artifact.getGroupId() + "." + artifact.getArtifactId()
            + ( artifact.getClassifier() != null ? "." + artifact.getClassifier() : "" )
            + ( artifact.getType() != null ? "." + artifact.getType() : "" ) + ".path";
    }

    /**
     * Get the current Maven project
     *
     * @return current Maven project
     */
    public MavenProject getMavenProject()
    {
        return this.project;
    }

    /**
     * @param antProject {@link Project}
     */
    public void initMavenTasks( Project antProject )
    {
        getLog().debug( "Initialize Maven Ant Tasks" );
        Typedef typedef = new Typedef();
        typedef.setProject( antProject );
        typedef.setResource( ANTLIB );
        if ( !customTaskPrefix.equals( "" ) )
        {
            typedef.setURI( TASK_URI );
        }
        typedef.execute();
    }

    /**
     * Write the Ant target and surrounding tags to a temporary file
     *
     * @throws PlexusConfigurationException
     */
    private File writeTargetToProjectFile()
        throws IOException, PlexusConfigurationException
    {
        // Have to use an XML writer because in Maven 2.x the PlexusConfig toString() method loses XML attributes
        StringWriter writer = new StringWriter();
        AntrunXmlPlexusConfigurationWriter xmlWriter = new AntrunXmlPlexusConfigurationWriter();
        xmlWriter.write( target, writer );

        StringBuilder antProjectConfig = new StringBuilder( writer.getBuffer() );

        // replace deprecated tasks tag with standard Ant target
        stringReplace( antProjectConfig, "<tasks", "<target" );
        stringReplace( antProjectConfig, "</tasks", "</target" );

        antTargetName = target.getAttribute( "name" );

        if ( antTargetName == null )
        {
            antTargetName = DEFAULT_ANT_TARGET_NAME;
            stringReplace( antProjectConfig, "<target", "<target name=\"" + DEFAULT_ANT_TARGET_NAME + "\"" );
        }

        String xmlns = "";
        if ( !customTaskPrefix.trim().equals( "" ) )
        {
            xmlns = "xmlns:" + customTaskPrefix + "=\"" + TASK_URI + "\"";
        }

        final String xmlHeader = "<?xml version=\"1.0\" encoding=\"" + UTF_8 + "\" ?>\n";
        antProjectConfig.insert( 0, xmlHeader );
        final String projectOpen =
            "<project name=\"maven-antrun-\" default=\"" + antTargetName + "\" " + xmlns + " >\n";
        int index = antProjectConfig.indexOf( "<target" );
        antProjectConfig.insert( index, projectOpen );

        final String projectClose = "\n</project>";
        antProjectConfig.append( projectClose );

        // The fileName should probably use the plugin executionId instead of the targetName
        String fileName = "build-" + antTargetName + ".xml";
        File buildFile = new File( project.getBuild().getDirectory(), "/antrun/" + fileName );

        //noinspection ResultOfMethodCallIgnored
        buildFile.getParentFile().mkdirs();
        FileUtils.fileWrite( buildFile.getAbsolutePath(), UTF_8, antProjectConfig.toString() );
        return buildFile;
    }

    /**
     * Replace text in a StringBuilder. If the match text is not found, the StringBuilder is returned unchanged.
     *
     * @param text  The string buffer containing the text
     * @param match The string to match and remove
     * @param with  The string to insert
     */
    public void stringReplace( StringBuilder text, String match, String with )
    {
        int index = text.indexOf( match );
        if ( index != -1 )
        {
            text.replace( index, index + match.length(), with );
        }
    }

    /**
     * @param antTargetConfig {@link PlexusConfiguration}
     * @return The target name.
     * @throws PlexusConfigurationException in case of not existing attribute.
     */
    public String checkTargetName( PlexusConfiguration antTargetConfig )
        throws PlexusConfigurationException
    {
        String targetName = antTargetConfig.getAttribute( "name" );
        if ( targetName == null )
        {
            targetName = DEFAULT_ANT_TARGET_NAME;
        }
        return targetName;
    }

    /**
     * @param buildException not null
     * @return the fragment XML part where the buildException occurs.
     * @since 1.7
     */
    private String findFragment( BuildException buildException )
    {
        if ( buildException == null || buildException.getLocation() == null
            || buildException.getLocation().getFileName() == null )
        {
            return null;
        }

        File antFile = new File( buildException.getLocation().getFileName() );
        if ( !antFile.exists() )
        {
            return null;
        }

        LineNumberReader reader = null;
        try
        {
            reader = new LineNumberReader( ReaderFactory.newXmlReader( antFile ) );
            String line;
            while ( ( line = reader.readLine() ) != null )
            {
                if ( reader.getLineNumber() == buildException.getLocation().getLineNumber() )
                {
                    return "around Ant part ..." + line.trim() + "... @ " + buildException.getLocation().getLineNumber()
                        + ":" + buildException.getLocation().getColumnNumber() + " in " + antFile.getAbsolutePath();
                }
            }
        }
        catch ( Exception e )
        {
            getLog().debug( e.getMessage(), e );
            return null;
        }
        finally
        {
            IOUtil.close( reader );
        }

        return null;
    }
}
