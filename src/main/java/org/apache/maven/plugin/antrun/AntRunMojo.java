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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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
import org.codehaus.plexus.util.StringUtils;

/**
 * Maven AntRun Mojo.
 *
 * This plugin provides the capability of calling Ant tasks
 * from a POM by running the nested ant tasks inside the &lt;tasks/&gt;
 * parameter. It is encouraged to move the actual tasks to
 * a separate build.xml file and call that file with an
 * &lt;ant/&gt; task.
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal run
 * @threadSafe
 * @requiresDependencyResolution test
 */
public class AntRunMojo
    extends AbstractMojo
{
    /**
     * The refid used to store the Maven project object in the Ant build.
     */
    public final static String DEFAULT_MAVEN_PROJECT_REFID = "maven.project";

    /**
     * The refid used to store the Maven project object in the Ant build.
     */
    public final static String DEFAULT_MAVEN_PROJECT_HELPER_REFID = "maven.project.helper";

    /**
     * The default target name.
     */
    public final static String DEFAULT_ANT_TARGET_NAME = "main";

    /**
     * The name used for the ant target
     */
    private String antTargetName;

    /**
     * The path to The XML file containing the definition of the Maven tasks.
     */
    public final static String ANTLIB = "org/apache/maven/ant/tasks/antlib.xml";

    /**
     * The URI which defines the built in Ant tasks
     */
    public final static String TASK_URI = "antlib:org.apache.maven.ant.tasks";

    /**
     * The Maven project object
     * 
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;

    /**
     * The Maven project helper object
     * 
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * The plugin dependencies.
     * 
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    private List pluginArtifacts;

    /**
     * The local Maven repository
     * 
     * @parameter expression="${localRepository}"
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * String to prepend to project and dependency property names.
     * 
     * @parameter default-value=""
     */
    private String propertyPrefix;

    /**
     * The xml namespace to use for the built in Ant tasks.
     * 
     * @parameter default-value="mvn"
     */
    private String taskNamespace;

    /**
     * The name of a property containing the list of all dependency versions. This is used for the removing the versions
     * from the filenames.
     * 
     * @parameter default-value="maven.project.dependencies.versions"
     */
    private String versionsPropertyName;

    /**
     * The XML for the Ant task. You can add anything you can add between &lt;target&gt; and &lt;/target&gt; in a
     * build.xml.
     * 
     * @deprecated Use target instead
     * @parameter
     */
    private PlexusConfiguration tasks;

    /**
     * The XML for the Ant target. You can add anything you can add between &lt;target&gt; and &lt;/target&gt; in a
     * build.xml.
     * 
     * @parameter
     */
    private PlexusConfiguration target;

    /**
     * This folder is added to the list of those folders containing source to be compiled. Use this if your ant script
     * generates source code.
     * 
     * @parameter expression="${sourceRoot}"
     * @deprecated Use the build-helper-maven-plugin to bind source directories
     */
    private File sourceRoot;

    /**
     * This folder is added to the list of those folders containing source to be compiled for testing. Use this if your
     * ant script generates test source code.
     * 
     * @parameter expression="${testSourceRoot}"
     * @deprecated Use the build-helper-maven-plugin to bind test source directories
     */
    private File testSourceRoot;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        MavenProject mavenProject = getMavenProject();

        if ( tasks != null )
        {
            getLog().warn( "Parameter tasks is deprecated, use target instead" );
            target = tasks;
        }
        
        if ( target == null )
        {
            getLog().info( "No ant target defined - SKIPPED" );
            return;
        }

        if ( propertyPrefix == null )
        {
            propertyPrefix = "";
        }

        try
        {
            Project antProject = new Project();
            File antBuildFile = this.writeTargetToProjectFile( );
            ProjectHelper.configureProject( antProject, antBuildFile );
            antProject.init();

            DefaultLogger antLogger = new DefaultLogger();
            antLogger.setOutputPrintStream( System.out );
            antLogger.setErrorPrintStream( System.err );
            antLogger.setMessageOutputLevel( getLog().isDebugEnabled() ? Project.MSG_DEBUG : Project.MSG_INFO );

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

            // The ant project needs actual properties vs. using expression evaluator when calling an external build
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
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "DependencyResolutionRequiredException: " + e.getMessage(), e );
        }
        catch ( BuildException e )
        {
            throw new MojoExecutionException( "An Ant BuildException has occured: " + e.getMessage(), e );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error executing ant tasks: " + e.getMessage(), e );
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
     * @param artifacts
     * @param antProject
     * @return a path
     * @throws DependencyResolutionRequiredException
     */
    public Path getPathFromArtifacts( Collection artifacts, Project antProject )
        throws DependencyResolutionRequiredException
    {
        if ( artifacts == null )
        {
            return new Path( antProject );
        }

        List list = new ArrayList( artifacts.size() );
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();
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
     * Copy properties from the maven project to the ant project.
     * 
     * @param mavenProject
     * @param antProject
     */
    public void copyProperties( MavenProject mavenProject, Project antProject )
    {
        Properties mavenProps = mavenProject.getProperties();
        Iterator iter = mavenProps.keySet().iterator();
        while ( iter.hasNext() )
        {
            String key = (String) iter.next();
            antProject.setProperty( key, mavenProps.getProperty( key ) );
        }

        // Set the POM file as the ant.file for the tasks run directly in Maven.
        antProject.setProperty( "ant.file", mavenProject.getFile().getAbsolutePath() );

        // Add some of the common maven properties
        getLog().debug("Setting properties with prefix: " + propertyPrefix );
        antProject.setProperty( ( propertyPrefix + "project.groupId" ), mavenProject.getGroupId() );
        antProject.setProperty( ( propertyPrefix + "project.artifactId" ), mavenProject.getArtifactId() );
        antProject.setProperty( ( propertyPrefix + "project.name" ), mavenProject.getName() );
        antProject.setProperty( ( propertyPrefix + "project.description" ), mavenProject.getDescription() );
        antProject.setProperty( ( propertyPrefix + "project.version" ), mavenProject.getVersion() );
        antProject.setProperty( ( propertyPrefix + "project.packaging" ), mavenProject.getPackaging() );
        antProject.setProperty( ( propertyPrefix + "project.build.directory" ), mavenProject.getBuild().getDirectory() );
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
        Set depArtifacts = mavenProject.getArtifacts();
        for ( Iterator it = depArtifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            String propName = artifact.getDependencyConflictId();

            antProject.setProperty( propertyPrefix + propName, artifact.getFile().getPath() );
        }

        // Add a property containing the list of versions for the mapper
        StringBuffer versionsBuffer = new StringBuffer();
        for ( Iterator it = depArtifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            versionsBuffer.append( artifact.getVersion() + File.pathSeparator );
        }
        antProject.setProperty( versionsPropertyName, versionsBuffer.toString() );

        // Add properties in deprecated format to depenedency artifacts
        // This should be removed in future versions of the antrun plugin.
        for ( Iterator it = depArtifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            String propName = AntPropertyHelper.getDependencyArtifactPropertyName( artifact );

            antProject.setProperty( propName, artifact.getFile().getPath() );
        }
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

    public void initMavenTasks( Project antProject )
    {
        getLog().debug( "Initialize Maven Ant Tasks" );
        Typedef typedef = new Typedef();
        typedef.setProject( antProject );
        typedef.setResource( ANTLIB );
        // typedef.setURI( TASK_URI );
        typedef.execute();
    }

    /**
     * Write the ant target and surrounding tags to a temporary file
     * 
     * @throws PlexusConfigurationException
     */
    private File writeTargetToProjectFile()
        throws IOException, PlexusConfigurationException
    {
        // Have to use an XML writer because in Maven 2.x the PlexusConfig toString() method loses XML attributes
        StringWriter writer = new StringWriter();
        AntrunXmlPlexusConfigurationWriter xmlWriter = new AntrunXmlPlexusConfigurationWriter();
        xmlWriter.write( writer, target );
        
        StringBuffer antProjectConfig = writer.getBuffer();

        // replace deprecated tasks tag with standard Ant target
        stringReplace( antProjectConfig, "<tasks", "<target" );
        stringReplace( antProjectConfig, "</tasks", "</target" );

        antTargetName = target.getAttribute( "name" );

        if ( antTargetName == null )
        {
            antTargetName = DEFAULT_ANT_TARGET_NAME;
            stringReplace( antProjectConfig, "<target", "<target name=\"" + antTargetName + "\"" );
        }

        final String projectOpen = "<project name=\"maven-antrun-\" default=\"" + antTargetName + "\">\n";
        int index = antProjectConfig.indexOf( "<target" );
        antProjectConfig.insert( index, projectOpen );

        final String projectClose = "\n</project>";
        antProjectConfig.append( projectClose );

        // The fileName should probably use the plugin executionId instead of the targetName
        String fileName = "build-" + antTargetName + ".xml";
        File buildFile = new File( project.getBuild().getDirectory(), "/antrun/" + fileName );

        buildFile.getParentFile().mkdirs();
        FileUtils.fileWrite( buildFile.getAbsolutePath(), antProjectConfig.toString() );
        return buildFile;
    }

    /**
     * Replace text in a StringBuffer.  If the match text is not found, the StringBuffer 
     * is returned unchanged.
     * 
     * @param text The string buffer containing the text
     * @param match The string to match and remove
     * @param with The string to insert
     */
    public void stringReplace( StringBuffer text, String match, String with )
    {
        int index = text.indexOf( match );
        if ( index != -1 )
        {
            text.replace( index, index + match.length(), with );
        }
    }

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

}
