package org.apache.maven.plugin.ant;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.PathUtils;
import org.apache.tools.ant.Main;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import sun.security.action.GetIntegerAction;

/**
 * Write Ant build files from <code>Maven Project</code> for <a href="http://ant.apache.org">Ant</a> 1.6.2 or above:
 * <ul>
 * <li>build.xml</li>
 * <li>maven-build.xml</li>
 * <li>maven-build.properties</li>
 * </ul>
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class AntBuildWriter
{
    /**
     * The default line indenter
     */
    protected static final int DEFAULT_INDENTATION_SIZE = 2;

    /**
     * The default build file name (build.xml)
     */
    protected static final String DEFAULT_BUILD_FILENAME = Main.DEFAULT_BUILD_FILENAME;

    /**
     * The default generated build file name
     */
    protected static final String DEFAULT_MAVEN_BUILD_FILENAME = "maven-build.xml";

    /**
     * The default build properties file name
     */
    protected static final String DEFAULT_MAVEN_PROPERTIES_FILENAME = "maven-build.properties";

    private MavenProject project;

    private ArtifactResolverWrapper artifactResolverWrapper;

    private File localRepository;

    private Settings settings;

    private boolean overwrite;

    /**
     * @param project
     * @param artifactResolverWrapper
     * @param settings
     * @param overwrite
     */
    public AntBuildWriter( MavenProject project, ArtifactResolverWrapper artifactResolverWrapper, Settings settings,
                          boolean overwrite )
    {
        this.project = project;
        this.artifactResolverWrapper = artifactResolverWrapper;
        this.localRepository = new File( artifactResolverWrapper.getLocalRepository().getBasedir() );
        this.settings = settings;
        this.overwrite = overwrite;
    }

    /**
     * Generate Ant build XML files
     *
     * @throws IOException
     */
    protected void writeBuildXmls()
        throws IOException
    {
        writeGeneratedBuildXml();
        writeBuildXml();
    }

    /**
     * Generate <code>maven-build.properties</code> only for a non-POM project
     *
     * @see #DEFAULT_MAVEN_PROPERTIES_FILENAME
     * @throws IOException
     */
    protected void writeBuildProperties()
        throws IOException
    {
        if ( AntBuildWriterUtil.isPomPackaging( project ) )
        {
            return;
        }

        FileOutputStream os = new FileOutputStream( new File( project.getBasedir(), DEFAULT_MAVEN_PROPERTIES_FILENAME ) );
        Properties properties = new Properties();

        // ----------------------------------------------------------------------
        // Build properties
        // ----------------------------------------------------------------------

        addProperty( properties, "maven.build.finalName", PathUtils.toRelative( project.getBasedir(), project
            .getBuild().getFinalName() ) );

        // target
        addProperty( properties, "maven.build.dir", PathUtils.toRelative( project.getBasedir(), project.getBuild()
            .getDirectory() ) );
        addProperty( properties, "project.build.directory", "${maven.build.dir}" );

        // ${maven.build.dir}/classes
        addProperty( properties, "maven.build.outputDir", "${maven.build.dir}/"
            + PathUtils.toRelative( new File( project.getBasedir(), properties.getProperty( "maven.build.dir" ) ),
                                    project.getBuild().getOutputDirectory() ) );
        addProperty( properties, "project.build.outputDirectory", "${maven.build.outputDir}" );

        // src/main/java
        if ( !project.getCompileSourceRoots().isEmpty() )
        {
            String[] compileSourceRoots = (String[]) project.getCompileSourceRoots().toArray( new String[0] );
            for ( int i = 0; i < compileSourceRoots.length; i++ )
            {
                addProperty( properties, "maven.build.srcDir." + i, PathUtils.toRelative( project.getBasedir(),
                                                                                          compileSourceRoots[i] ) );
            }
        }
        // src/main/resources
        if ( project.getBuild().getResources() != null )
        {
            Resource[] array = (Resource[]) project.getBuild().getResources().toArray( new Resource[0] );
            for ( int i = 0; i < array.length; i++ )
            {
                addProperty( properties, "maven.build.resourceDir." + i, PathUtils.toRelative( project.getBasedir(),
                                                                                               array[i].getDirectory() ) );
            }
        }

        // ${maven.build.dir}/test-classes
        addProperty( properties, "maven.build.testOutputDir", "${maven.build.dir}/"
            + PathUtils.toRelative( new File( project.getBasedir(), properties.getProperty( "maven.build.dir" ) ),
                                    project.getBuild().getTestOutputDirectory() ) );
        // src/test/java
        if ( !project.getTestCompileSourceRoots().isEmpty() )
        {
            String[] compileSourceRoots = (String[]) project.getTestCompileSourceRoots().toArray( new String[0] );
            for ( int i = 0; i < compileSourceRoots.length; i++ )
            {
                addProperty( properties, "maven.build.testDir." + i, PathUtils.toRelative( project.getBasedir(),
                                                                                           compileSourceRoots[i] ) );
            }
        }
        // src/test/resources
        if ( project.getBuild().getTestResources() != null )
        {
            Resource[] array = (Resource[]) project.getBuild().getTestResources().toArray( new Resource[0] );
            for ( int i = 0; i < array.length; i++ )
            {
                addProperty( properties, "maven.build.testResourceDir." + i, PathUtils
                    .toRelative( project.getBasedir(), array[i].getDirectory() ) );
            }
        }

        addProperty( properties, "maven.test.reports", "${maven.build.dir}/test-reports" );

        addProperty( properties, "maven.reporting.outputDirectory", "${maven.build.dir}/site" );

        // ----------------------------------------------------------------------
        // Settings properties
        // ----------------------------------------------------------------------

        addProperty( properties, "maven.settings.offline", String.valueOf( settings.isOffline() ) );
        addProperty( properties, "maven.settings.interactiveMode", String.valueOf( settings.isInteractiveMode() ) );
        addProperty( properties, "maven.repo.local", getLocalRepositoryPath() );

        // ----------------------------------------------------------------------
        // Project properties
        // ----------------------------------------------------------------------

        if ( project.getProperties() != null )
        {
            for ( Iterator it = project.getProperties().entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry property = (Map.Entry) it.next();
                addProperty( properties, property.getKey().toString(), property.getValue().toString() );
            }
        }

        properties.store( os, "Generated by Maven Ant Plugin - DO NOT EDIT THIS FILE!" );
    }

    /**
     * Generate an <code>maven-build.xml</code>
     *
     * @see #DEFAULT_MAVEN_BUILD_FILENAME
     * @throws IOException
     */
    private void writeGeneratedBuildXml()
        throws IOException
    {
        // TODO: parameter
        File outputFile = new File( project.getBasedir(), DEFAULT_MAVEN_BUILD_FILENAME );

        String encoding = "UTF-8";

        OutputStreamWriter w = new OutputStreamWriter( new FileOutputStream( outputFile ), encoding );

        XMLWriter writer = new PrettyPrintXMLWriter( w, StringUtils.repeat( " ", DEFAULT_INDENTATION_SIZE ), encoding,
                                                     null );

        // ----------------------------------------------------------------------
        // <!-- comments -->
        // ----------------------------------------------------------------------

        AntBuildWriterUtil.writeHeader( writer );

        // ----------------------------------------------------------------------
        // <project/>
        // ----------------------------------------------------------------------

        writer.startElement( "project" );
        writer.addAttribute( "name", project.getArtifactId() + "-from-maven" );
        writer.addAttribute( "default", "package" );
        writer.addAttribute( "basedir", "." );

        AntBuildWriterUtil.writeLineBreak( writer );

        // ----------------------------------------------------------------------
        // <property/>
        // ----------------------------------------------------------------------

        writeProperties( writer );

        // ----------------------------------------------------------------------
        // <path/>
        // ----------------------------------------------------------------------

        writeBuildPathDefinition( writer );

        // ----------------------------------------------------------------------
        // <target name="clean" />
        // ----------------------------------------------------------------------

        writeCleanTarget( writer );

        // ----------------------------------------------------------------------
        // <target name="compile" />
        // ----------------------------------------------------------------------

        List compileSourceRoots = AntBuildWriterUtil.removeEmptyCompileSourceRoots( project.getCompileSourceRoots() );
        writeCompileTarget( writer, compileSourceRoots );

        // ----------------------------------------------------------------------
        // <target name="compile-tests" />
        // ----------------------------------------------------------------------

        List testCompileSourceRoots = AntBuildWriterUtil.removeEmptyCompileSourceRoots( project
            .getTestCompileSourceRoots() );
        writeCompileTestsTarget( writer, testCompileSourceRoots );

        // ----------------------------------------------------------------------
        // <target name="test" />
        // ----------------------------------------------------------------------

        writeTestTargets( writer, testCompileSourceRoots );

        // ----------------------------------------------------------------------
        // <target name="javadoc" />
        // ----------------------------------------------------------------------
        writeJavadocTarget( writer );

        // ----------------------------------------------------------------------
        // <target name="package" />
        // ----------------------------------------------------------------------
        writePackageTarget( writer );

        // ----------------------------------------------------------------------
        // <target name="get-deps" />
        // ----------------------------------------------------------------------
        writeGetDepsTarget( writer );

        writer.endElement(); // project

        IOUtil.close( w );
    }

    /**
     * Generate an generic <code>build.xml</code> if not already exist
     *
     * @see #DEFAULT_BUILD_FILENAME
     * @throws IOException
     */
    private void writeBuildXml()
        throws IOException
    {
        File outputFile = new File( project.getBasedir(), DEFAULT_BUILD_FILENAME );

        if ( outputFile.exists() && !overwrite )
        {
            return;
        }

        String encoding = "UTF-8";

        OutputStreamWriter w = new OutputStreamWriter( new FileOutputStream( outputFile ), encoding );

        XMLWriter writer = new PrettyPrintXMLWriter( w, StringUtils.repeat( " ", DEFAULT_INDENTATION_SIZE ), encoding,
                                                     null );

        // ----------------------------------------------------------------------
        // <!-- comments -->
        // ----------------------------------------------------------------------

        AntBuildWriterUtil.writeAntVersionHeader( writer );

        // ----------------------------------------------------------------------
        // <project/>
        // ----------------------------------------------------------------------

        writer.startElement( "project" );
        writer.addAttribute( "name", project.getArtifactId() );
        writer.addAttribute( "default", "package" );
        writer.addAttribute( "basedir", "." );

        AntBuildWriterUtil.writeLineBreak( writer );

        AntBuildWriterUtil.writeCommentText( writer, "Import " + DEFAULT_MAVEN_BUILD_FILENAME
            + " into the current project", 1 );

        writer.startElement( "import" );
        writer.addAttribute( "file", DEFAULT_MAVEN_BUILD_FILENAME );
        writer.endElement(); // import

        AntBuildWriterUtil.writeLineBreak( writer, 1, 1 );

        AntBuildWriterUtil.writeCommentText( writer, "Help target", 1 );

        writer.startElement( "target" );
        writer.addAttribute( "name", "help" );

        writer.startElement( "echo" );
        writer.addAttribute( "message", "Please run: $ant -projecthelp" );
        writer.endElement(); // echo

        writer.endElement(); // target

        AntBuildWriterUtil.writeLineBreak( writer );

        writer.endElement(); // project

        IOUtil.close( w );
    }

    /**
     * Write properties in the writer only for a non-POM project.
     *
     * @param writer
     */
    private void writeProperties( XMLWriter writer )
    {
        if ( AntBuildWriterUtil.isPomPackaging( project ) )
        {
            return;
        }

        // TODO: optional in m1
        // TODO: USD properties
        AntBuildWriterUtil.writeCommentText( writer, "Build environment properties", 1 );

        // ----------------------------------------------------------------------
        // File properties to override local properties
        // ----------------------------------------------------------------------

        writer.startElement( "property" );
        writer.addAttribute( "file", "${user.home}/.m2/maven.properties" );
        writer.endElement(); // property

        writer.startElement( "property" );
        writer.addAttribute( "file", DEFAULT_MAVEN_PROPERTIES_FILENAME );
        writer.endElement(); // property

        // ----------------------------------------------------------------------
        // Build properties
        // ----------------------------------------------------------------------

        AntBuildWriterUtil.writeLineBreak( writer, 2, 1 );

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.build.finalName" );
        writer.addAttribute( "value", project.getBuild().getFinalName() );
        writer.endElement(); // property

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.build.dir" );
        writer.addAttribute( "value", PathUtils.toRelative( project.getBasedir(), project.getBuild().getDirectory() ) );
        writer.endElement(); // property

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.build.outputDir" );
        writer.addAttribute( "value", "${maven.build.dir}/"
            + PathUtils.toRelative( new File( project.getBuild().getDirectory() ), project.getBuild()
                .getOutputDirectory() ) );
        writer.endElement(); // property

        if ( !project.getCompileSourceRoots().isEmpty() )
        {
            String[] compileSourceRoots = (String[]) project.getCompileSourceRoots().toArray( new String[0] );
            for ( int i = 0; i < compileSourceRoots.length; i++ )
            {
                writer.startElement( "property" );
                writer.addAttribute( "name", "maven.build.srcDir." + i );
                writer.addAttribute( "value", PathUtils.toRelative( project.getBasedir(), compileSourceRoots[i] ) );
                writer.endElement(); // property
            }
        }

        if ( project.getBuild().getResources() != null )
        {
            Resource[] array = (Resource[]) project.getBuild().getResources().toArray( new Resource[0] );
            for ( int i = 0; i < array.length; i++ )
            {
                writer.startElement( "property" );
                writer.addAttribute( "name", "maven.build.resourceDir." + i );
                writer.addAttribute( "value", PathUtils.toRelative( project.getBasedir(), array[i].getDirectory() ) );
                writer.endElement(); // property
            }
        }

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.build.testOutputDir" );
        writer.addAttribute( "value", "${maven.build.dir}/"
            + PathUtils.toRelative( new File( project.getBuild().getDirectory() ), project.getBuild()
                .getTestOutputDirectory() ) );
        writer.endElement(); // property

        if ( !project.getTestCompileSourceRoots().isEmpty() )
        {
            String[] compileSourceRoots = (String[]) project.getTestCompileSourceRoots().toArray( new String[0] );
            for ( int i = 0; i < compileSourceRoots.length; i++ )
            {
                writer.startElement( "property" );
                writer.addAttribute( "name", "maven.build.testDir." + i );
                writer.addAttribute( "value", PathUtils.toRelative( project.getBasedir(), compileSourceRoots[i] ) );
                writer.endElement(); // property
            }
        }

        if ( project.getBuild().getTestResources() != null )
        {
            Resource[] array = (Resource[]) project.getBuild().getTestResources().toArray( new Resource[0] );
            for ( int i = 0; i < array.length; i++ )
            {
                writer.startElement( "property" );
                writer.addAttribute( "name", "maven.build.testResourceDir." + i );
                writer.addAttribute( "value", PathUtils.toRelative( project.getBasedir(), array[i].getDirectory() ) );
                writer.endElement(); // property
            }
        }

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.test.reports" );
        writer.addAttribute( "value", "${maven.build.dir}/test-reports" );
        writer.endElement(); // property

        String reportingOutputDir = project.getReporting().getOutputDirectory();
        // workaround for MNG-3475
        if ( !new File( reportingOutputDir ).isAbsolute() )
        {
            reportingOutputDir = new File( project.getBasedir(), reportingOutputDir ).getAbsolutePath();
        }
        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.reporting.outputDirectory" );
        writer.addAttribute( "value", "${maven.build.dir}/"
            + PathUtils.toRelative( new File( project.getBuild().getDirectory() ), reportingOutputDir ) );
        writer.endElement(); // property

        // ----------------------------------------------------------------------
        // Setting properties
        // ----------------------------------------------------------------------

        AntBuildWriterUtil.writeLineBreak( writer, 2, 1 );

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.repo.local" );
        writer.addAttribute( "value", "${user.home}/.m2/repository" );
        writer.endElement(); // property

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.settings.offline" );
        writer.addAttribute( "value", String.valueOf( settings.isOffline() ) );
        writer.endElement(); // property

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.settings.interactiveMode" );
        writer.addAttribute( "value", String.valueOf( settings.isInteractiveMode() ) );
        writer.endElement(); // property

        AntBuildWriterUtil.writeLineBreak( writer );
    }

    /**
     * Check if the local repository is in the default location:
     * <code>${user.home}/.m2/repository</code>. If that is the case then return
     * the path with the system property "user.home" in it. If not then just
     * return the absolute path to the local repository.
     */
    private String getLocalRepositoryPath() {
        String userHome = System.getProperty( "user.home" );
        String defaultPath = ( userHome + "/.m2/repository" ).replace( '\\', '/' );
        String actualPath = localRepository.getAbsolutePath().replace( '\\', '/' );
        if( actualPath.equals( defaultPath ) )
        {
            return "${user.home}/.m2/repository";
        }
        else
        {
            return localRepository.getAbsolutePath();
        }
    }

    /**
     * Write path definition in the writer only for a non-POM project.
     *
     * @param writer
     */
    private void writeBuildPathDefinition( XMLWriter writer )
    {
        if ( AntBuildWriterUtil.isPomPackaging( project ) )
        {
            return;
        }

        AntBuildWriterUtil.writeCommentText( writer, "Defining classpaths", 1 );

        writer.startElement( "path" );
        writer.addAttribute( "id", "build.classpath" );
        writer.startElement( "fileset" );
        writer.addAttribute( "dir", "${maven.repo.local}" );
        if ( !project.getCompileArtifacts().isEmpty() )
        {
            for ( Iterator i = project.getCompileArtifacts().iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();
                writer.startElement( "include" );
                writer.addAttribute( "name", PathUtils.toRelative( localRepository, artifact.getFile().getPath() ) );
                writer.endElement(); // include
            }
        }
        else
        {
            writer.startElement( "include" );
            writer.addAttribute( "name", "*.jar" );
            writer.endElement(); // include
        }
        writer.endElement(); // fileset
        writer.endElement(); // path

        writer.startElement( "path" );
        writer.addAttribute( "id", "build.test.classpath" );
        writer.startElement( "fileset" );
        writer.addAttribute( "dir", "${maven.repo.local}" );
        if ( !project.getTestArtifacts().isEmpty() )
        {
            for ( Iterator i = project.getTestArtifacts().iterator(); i.hasNext(); )
            {
                Artifact artifact = (Artifact) i.next();
                writer.startElement( "include" );
                writer.addAttribute( "name", PathUtils.toRelative( localRepository, artifact.getFile().getPath() ) );
                writer.endElement(); // include
            }
        }
        else
        {
            writer.startElement( "include" );
            writer.addAttribute( "name", "*.jar" );
            writer.endElement(); // include
        }
        writer.endElement(); // fileset
        writer.endElement(); // path

        AntBuildWriterUtil.writeLineBreak( writer );
    }

    /**
     * Write clean target in the writer depending the packaging of the project.
     *
     * @param writer
     */
    private void writeCleanTarget( XMLWriter writer )
    {
        AntBuildWriterUtil.writeCommentText( writer, "Cleaning up target", 1 );

        writer.startElement( "target" );
        writer.addAttribute( "name", "clean" );
        writer.addAttribute( "description", "Clean the output directory" );

        if ( AntBuildWriterUtil.isPomPackaging( project ) )
        {
            if ( project.getModules() != null )
            {
                for ( Iterator it = project.getModules().iterator(); it.hasNext(); )
                {
                    String moduleSubPath = (String) it.next();
                    AntBuildWriterUtil.writeAntTask( writer, project, moduleSubPath, "clean" );
                }
            }
        }
        else
        {
            writer.startElement( "delete" );
            writer.addAttribute( "dir", "${maven.build.dir}" );
            writer.endElement(); // delete
        }

        writer.endElement(); // target

        AntBuildWriterUtil.writeLineBreak( writer );
    }

    /**
     * Write compile target in the writer depending the packaging of the project.
     *
     * @param writer
     * @param compileSourceRoots
     * @throws IOException if any
     */
    private void writeCompileTarget( XMLWriter writer, List compileSourceRoots )
        throws IOException
    {
        AntBuildWriterUtil.writeCommentText( writer, "Compilation target", 1 );

        if ( AntBuildWriterUtil.isPomPackaging( project ) )
        {
            writer.startElement( "target" );
            writer.addAttribute( "name", "compile" );
            writer.addAttribute( "description", "Compile the code" );
            if ( project.getModules() != null )
            {
                for ( Iterator it = project.getModules().iterator(); it.hasNext(); )
                {
                    String moduleSubPath = (String) it.next();
                    AntBuildWriterUtil.writeAntTask( writer, project, moduleSubPath, "compile" );
                }
            }
            writer.endElement(); // target
        }
        else
        {
            writer.startElement( "target" );
            writer.addAttribute( "name", "compile" );
            writer.addAttribute( "depends", "get-deps" );
            writer.addAttribute( "description", "Compile the code" );

            writeCompileTasks( writer, project.getBasedir(), "${maven.build.outputDir}", compileSourceRoots, project
                .getBuild().getResources(), null, false );

            writer.endElement(); // target
        }

        AntBuildWriterUtil.writeLineBreak( writer );
    }

    /**
     * Write compile-test target in the writer depending the packaging of the project.
     *
     * @param writer
     * @param testCompileSourceRoots
     * @throws IOException if any
     */
    private void writeCompileTestsTarget( XMLWriter writer, List testCompileSourceRoots )
        throws IOException
    {
        AntBuildWriterUtil.writeCommentText( writer, "Test-compilation target", 1 );

        if ( AntBuildWriterUtil.isPomPackaging( project ) )
        {
            writer.startElement( "target" );
            writer.addAttribute( "name", "compile-tests" );
            writer.addAttribute( "description", "Compile the test code" );
            if ( project.getModules() != null )
            {
                for ( Iterator it = project.getModules().iterator(); it.hasNext(); )
                {
                    String moduleSubPath = (String) it.next();
                    AntBuildWriterUtil.writeAntTask( writer, project, moduleSubPath, "compile-tests" );
                }
            }
            writer.endElement(); // target
        }
        else
        {
            writer.startElement( "target" );
            writer.addAttribute( "name", "compile-tests" );
            AntBuildWriterUtil.addWrapAttribute( writer, "target", "depends", "junit-present, compile", 2 );
            AntBuildWriterUtil.addWrapAttribute( writer, "target", "description", "Compile the test code", 2 );
            AntBuildWriterUtil.addWrapAttribute( writer, "target", "if", "junit.present", 2 );

            writeCompileTasks( writer, project.getBasedir(), "${maven.build.testOutputDir}", testCompileSourceRoots,
                               project.getBuild().getTestResources(), "${maven.build.outputDir}", true );

            writer.endElement(); // target
        }

        AntBuildWriterUtil.writeLineBreak( writer );
    }

    /**
     * Write test target in the writer depending the packaging of the project.
     *
     * @param writer
     * @param testCompileSourceRoots
     */
    private void writeTestTargets( XMLWriter writer, List testCompileSourceRoots )
        throws IOException
    {
        AntBuildWriterUtil.writeCommentText( writer, "Run all tests", 1 );

        if ( AntBuildWriterUtil.isPomPackaging( project ) )
        {
            writer.startElement( "target" );
            writer.addAttribute( "name", "test" );
            writer.addAttribute( "description", "Run the test cases" );
            if ( project.getModules() != null )
            {
                for ( Iterator it = project.getModules().iterator(); it.hasNext(); )
                {
                    String moduleSubPath = (String) it.next();
                    AntBuildWriterUtil.writeAntTask( writer, project, moduleSubPath, "test" );
                }
            }
            writer.endElement(); // target
        }
        else
        {
            writer.startElement( "target" );
            writer.addAttribute( "name", "test" );
            AntBuildWriterUtil.addWrapAttribute( writer, "target", "depends", "junit-present, compile-tests", 2 );
            AntBuildWriterUtil.addWrapAttribute( writer, "target", "if", "junit.present", 2 );
            AntBuildWriterUtil.addWrapAttribute( writer, "target", "description", "Run the test cases", 2 );

            if ( !testCompileSourceRoots.isEmpty() )
            {
                writer.startElement( "mkdir" );
                writer.addAttribute( "dir", "${maven.test.reports}" );
                writer.endElement(); //mkdir

                writer.startElement( "junit" );
                writer.addAttribute( "printSummary", "yes" );
                writer.addAttribute( "haltonerror", "true" );
                writer.addAttribute( "haltonfailure", "true" );
                writer.addAttribute( "fork", "true" );
                writer.addAttribute( "dir", "." );

                writer.startElement( "sysproperty" );
                writer.addAttribute( "key", "basedir" );
                writer.addAttribute( "value", "." );
                writer.endElement(); // sysproperty

                writer.startElement( "formatter" );
                writer.addAttribute( "type", "xml" );
                writer.endElement(); // formatter

                writer.startElement( "formatter" );
                writer.addAttribute( "type", "plain" );
                writer.addAttribute( "usefile", "false" );
                writer.endElement(); // formatter

                writer.startElement( "classpath" );
                writer.startElement( "path" );
                writer.addAttribute( "refid", "build.test.classpath" );
                writer.endElement(); // path
                writer.startElement( "pathelement" );
                writer.addAttribute( "location", "${maven.build.outputDir}" );
                writer.endElement(); // pathelement
                writer.startElement( "pathelement" );
                writer.addAttribute( "location", "${maven.build.testOutputDir}" );
                writer.endElement(); // pathelement
                writer.endElement(); // classpath

                writer.startElement( "batchtest" );
                writer.addAttribute( "todir", "${maven.test.reports}" );
                writer.addAttribute( "unless", "test" );

                List includes = getTestIncludes();
                List excludes = getTestExcludes();

                writeTestFilesets( writer, testCompileSourceRoots, includes, excludes );

                writer.endElement(); // batchtest

                writer.startElement( "batchtest" );
                writer.addAttribute( "todir", "${maven.test.reports}" );
                writer.addAttribute( "if", "test" );

                includes = Arrays.asList( new String[] { "**/${test}.java" } );

                writeTestFilesets( writer, testCompileSourceRoots, includes, excludes );

                writer.endElement(); // batchtest

                writer.endElement(); // junit
            }
            writer.endElement(); // target

            AntBuildWriterUtil.writeLineBreak( writer, 2, 1 );

            writer.startElement( "target" );
            writer.addAttribute( "name", "test-junit-present" );

            writer.startElement( "available" );
            writer.addAttribute( "classname", "junit.framework.Test" );
            writer.addAttribute( "property", "junit.present" );
            writer.endElement(); // available

            writer.endElement(); // target

            AntBuildWriterUtil.writeLineBreak( writer, 2, 1 );

            writer.startElement( "target" );
            writer.addAttribute( "name", "junit-present" );
            AntBuildWriterUtil.addWrapAttribute( writer, "target", "depends", "test-junit-present", 2 );
            AntBuildWriterUtil.addWrapAttribute( writer, "target", "unless", "junit.present", 2 );

            writer.startElement( "echo" );
            writer.writeText( StringUtils.repeat( "=", 35 ) + " WARNING " + StringUtils.repeat( "=", 35 ) );
            writer.endElement(); // echo

            writer.startElement( "echo" );
            writer.writeText( " Junit isn't present in your $ANT_HOME/lib directory. Tests not executed. " );
            writer.endElement(); // echo

            writer.startElement( "echo" );
            writer.writeText( StringUtils.repeat( "=", 79 ) );
            writer.endElement(); // echo

            writer.endElement(); // target
        }

        AntBuildWriterUtil.writeLineBreak( writer );
    }

    /**
     * Gets the include patterns for the unit tests.
     * 
     * @return A list of strings with include patterns, might be empty but never <code>null</code>.
     */
    private List getTestIncludes()
        throws IOException
    {
        List includes = getSelectorList( AntBuildWriterUtil.getMavenSurefirePluginOptions( project, "includes", null ) );
        if ( includes == null || includes.isEmpty() )
        {
            includes = Arrays.asList( new String[] { "**/Test*.java", "**/*Test.java", "**/*TestCase.java" } );
        }
        return includes;
    }

    /**
     * Gets the exclude patterns for the unit tests.
     * 
     * @return A list of strings with exclude patterns, might be empty but never <code>null</code>.
     */
    private List getTestExcludes()
        throws IOException
    {
        List excludes = getSelectorList( AntBuildWriterUtil.getMavenSurefirePluginOptions( project, "excludes", null ) );
        if ( excludes == null || excludes.isEmpty() )
        {
            excludes = Arrays.asList( new String[] { "**/*Abstract*Test.java" } );
        }
        return excludes;
    }

    /**
     * Write the <code>&lt;fileset&gt;</code> elements for the test compile source roots.
     * 
     * @param writer
     * @param testCompileSourceRoots
     * @param includes
     * @param excludes
     */
    private void writeTestFilesets( XMLWriter writer, List testCompileSourceRoots, List includes, List excludes )
    {
        for ( int i = 0; i < testCompileSourceRoots.size(); i++ )
        {
            writer.startElement( "fileset" );
            writer.addAttribute( "dir", "${maven.build.testDir." + i + "}" );
            // TODO: m1 allows additional test exclusions via maven.ant.excludeTests
            AntBuildWriterUtil.writeIncludesExcludes( writer, includes, excludes );
            writer.endElement(); // fileset
        }
    }

    /**
     * Write javadoc target in the writer depending the packaging of the project.
     *
     * @param writer
     * @throws IOException if any
     */
    private void writeJavadocTarget( XMLWriter writer )
        throws IOException
    {
        AntBuildWriterUtil.writeCommentText( writer, "Javadoc target", 1 );

        writer.startElement( "target" );
        writer.addAttribute( "name", "javadoc" );
        writer.addAttribute( "description", "Generates the Javadoc of the application" );

        if ( AntBuildWriterUtil.isPomPackaging( project ) )
        {
            if ( project.getModules() != null )
            {
                for ( Iterator it = project.getModules().iterator(); it.hasNext(); )
                {
                    String moduleSubPath = (String) it.next();
                    AntBuildWriterUtil.writeAntTask( writer, project, moduleSubPath, "javadoc" );
                }
            }
        }
        else
        {
            AntBuildWriterUtil.writeJavadocTask( writer, project, artifactResolverWrapper );
        }

        writer.endElement(); // target

        AntBuildWriterUtil.writeLineBreak( writer );
    }

    /**
     * Write package target in the writer depending the packaging of the project.
     *
     * @param writer
     * @throws IOException if any
     */
    private void writePackageTarget( XMLWriter writer )
        throws IOException
    {
        String synonym = null; // type of the package we are creating (for example jar)
        AntBuildWriterUtil.writeCommentText( writer, "Package target", 1 );

        writer.startElement( "target" );
        writer.addAttribute( "name", "package" );

        if ( !AntBuildWriterUtil.isPomPackaging( project ) )
        {
            writer.addAttribute( "depends", "compile,test" );
        }
        writer.addAttribute( "description", "Package the application" );

        if ( AntBuildWriterUtil.isPomPackaging( project ) )
        {
            if ( project.getModules() != null )
            {
                for ( Iterator it = project.getModules().iterator(); it.hasNext(); )
                {
                    String moduleSubPath = (String) it.next();
                    AntBuildWriterUtil.writeAntTask( writer, project, moduleSubPath, "package" );
                }
            }
        }
        else
        {
            if ( AntBuildWriterUtil.isJarPackaging( project ) )
            {
                AntBuildWriterUtil.writeJarTask( writer, project );
                synonym = "jar";
            }
            else if ( AntBuildWriterUtil.isEarPackaging( project ) )
            {
                AntBuildWriterUtil.writeEarTask( writer, project );
                synonym = "ear";
            }
            else if ( AntBuildWriterUtil.isWarPackaging( project ) )
            {
                AntBuildWriterUtil.writeWarTask( writer, project, localRepository );
                synonym = "war";
            }
            else
            {
                writer.startElement( "echo" );
                writer.addAttribute( "message", "No Ant task exists for the packaging '" + project.getPackaging()
                    + "'. " + "You could overrided the Ant package target in your build.xml." );
                writer.endElement(); // echo
            }
        }

        writer.endElement(); // target

        AntBuildWriterUtil.writeLineBreak( writer );

        if ( synonym != null )
        {
            AntBuildWriterUtil.writeCommentText( writer,
                                                 "A dummy target for the package named after the type it creates", 1 );
            writer.startElement( "target" );
            writer.addAttribute( "name", synonym );
            writer.addAttribute( "depends", "package" );
            writer.addAttribute( "description", "Builds the " + synonym + " for the application" );
            writer.endElement(); //target

            AntBuildWriterUtil.writeLineBreak( writer );
        }
    }

    private void writeCompileTasks( XMLWriter writer, File basedir, String outputDirectory, List compileSourceRoots,
                                   List resources, String additionalClassesDirectory, boolean isTest )
        throws IOException
    {
        writer.startElement( "mkdir" );
        writer.addAttribute( "dir", outputDirectory );
        writer.endElement(); // mkdir

        if ( !compileSourceRoots.isEmpty() )
        {
            writer.startElement( "javac" );
            writer.addAttribute( "destdir", outputDirectory );
            Map[] includes = AntBuildWriterUtil.getMavenCompilerPluginOptions( project, "includes", null );
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "includes", getCommaSeparatedList( includes,
                                                                                                     "include" ), 3 );
            Map[] excludes = AntBuildWriterUtil.getMavenCompilerPluginOptions( project, "excludes", null );
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "excludes", getCommaSeparatedList( excludes,
                                                                                                     "exclude" ), 3 );
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "encoding", AntBuildWriterUtil
                .getMavenCompilerPluginBasicOption( project, "encoding", null ), 3 );
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "nowarn", AntBuildWriterUtil
                .getMavenCompilerPluginBasicOption( project, "showWarnings", "false" ), 3 );
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "debug", AntBuildWriterUtil
                .getMavenCompilerPluginBasicOption( project, "debug", "true" ), 3 );
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "optimize", AntBuildWriterUtil
                .getMavenCompilerPluginBasicOption( project, "optimize", "false" ), 3 );
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "deprecation", AntBuildWriterUtil
                .getMavenCompilerPluginBasicOption( project, "showDeprecation", "true" ), 3 );
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "target", AntBuildWriterUtil
                .getMavenCompilerPluginBasicOption( project, "target", "1.1" ), 3 );
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "verbose", AntBuildWriterUtil
                .getMavenCompilerPluginBasicOption( project, "verbose", "false" ), 3 );
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "fork", AntBuildWriterUtil
                .getMavenCompilerPluginBasicOption( project, "fork", "false" ), 3 );
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "memoryMaximumSize", AntBuildWriterUtil
                .getMavenCompilerPluginBasicOption( project, "meminitial", null ), 3 );
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "memoryInitialSize", AntBuildWriterUtil
                .getMavenCompilerPluginBasicOption( project, "maxmem", null ), 3 );
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "source", AntBuildWriterUtil
                .getMavenCompilerPluginBasicOption( project, "source", "1.3" ), 3 );

            String[] compileSourceRootsArray = (String[]) compileSourceRoots.toArray( new String[0] );
            for ( int i = 0; i < compileSourceRootsArray.length; i++ )
            {
                writer.startElement( "src" );
                writer.startElement( "pathelement" );
                if ( isTest )
                {
                    writer.addAttribute( "location", "${maven.build.testDir." + i + "}" );
                }
                else
                {
                    writer.addAttribute( "location", "${maven.build.srcDir." + i + "}" );
                }
                writer.endElement(); // pathelement
                writer.endElement(); // src
            }

            if ( additionalClassesDirectory == null )
            {
                writer.startElement( "classpath" );
                if ( isTest )
                {
                    writer.addAttribute( "refid", "build.test.classpath" );
                }
                else
                {
                    writer.addAttribute( "refid", "build.classpath" );
                }
                writer.endElement(); // classpath
            }
            else
            {
                writer.startElement( "classpath" );
                writer.startElement( "path" );
                if ( isTest )
                {
                    writer.addAttribute( "refid", "build.test.classpath" );
                }
                else
                {
                    writer.addAttribute( "refid", "build.classpath" );
                }
                writer.endElement(); // path
                writer.startElement( "pathelement" );
                writer.addAttribute( "location", additionalClassesDirectory );
                writer.endElement(); // pathelement
                writer.endElement(); // classpath
            }

            writer.endElement(); // javac
        }

        Resource[] array = (Resource[]) resources.toArray( new Resource[0] );
        for ( int i = 0; i < array.length; i++ )
        {
            Resource resource = array[i];

            if ( new File( resource.getDirectory() ).exists() )
            {
                String outputDir = outputDirectory;
                if ( resource.getTargetPath() != null && resource.getTargetPath().length() > 0 )
                {
                    outputDir = outputDir + "/" + resource.getTargetPath();

                    writer.startElement( "mkdir" );
                    writer.addAttribute( "dir", outputDir );
                    writer.endElement(); // mkdir
                }

                writer.startElement( "copy" );
                writer.addAttribute( "todir", outputDir );

                writer.startElement( "fileset" );
                if ( isTest )
                {
                    writer.addAttribute( "dir", "${maven.build.testResourceDir." + i + "}" );
                }
                else
                {
                    writer.addAttribute( "dir", "${maven.build.resourceDir." + i + "}" );
                }

                AntBuildWriterUtil.writeIncludesExcludes( writer, resource.getIncludes(), resource.getExcludes() );

                writer.endElement(); // fileset

                writer.endElement(); // copy
            }
        }
    }

    /**
     * Write get-deps target in the writer only for a non-POM project
     *
     * @param writer
     */
    private void writeGetDepsTarget( XMLWriter writer )
    {
        if ( AntBuildWriterUtil.isPomPackaging( project ) )
        {
            return;
        }

        AntBuildWriterUtil.writeCommentText( writer, "Download dependencies target", 1 );

        writer.startElement( "target" );
        writer.addAttribute( "name", "test-offline" );

        writer.startElement( "condition" );
        writer.addAttribute( "property", "maven.mode.offline" );
        writer.startElement( "equals" );
        writer.addAttribute( "arg1", "${maven.settings.offline}" );
        writer.addAttribute( "arg2", "true" );
        writer.endElement(); // equals
        writer.endElement(); // condition
        writer.endElement(); // target

        AntBuildWriterUtil.writeLineBreak( writer, 2, 1 );

        writer.startElement( "target" );
        writer.addAttribute( "name", "get-deps" );
        AntBuildWriterUtil.addWrapAttribute( writer, "target", "depends", "test-offline", 2 );
        AntBuildWriterUtil.addWrapAttribute( writer, "target", "description", "Download all dependencies", 2 );
        AntBuildWriterUtil.addWrapAttribute( writer, "target", "unless", "maven.mode.offline", 2 ); // TODO: check, and differs from m1

        writer.startElement( "mkdir" );
        writer.addAttribute( "dir", "${maven.repo.local}" );
        writer.endElement(); // mkdir

        String basedir = project.getBasedir().getAbsolutePath();

        // TODO: proxy - probably better to use wagon!
        for ( Iterator i = project.getTestArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            // TODO: should the artifacthandler be used instead?
            String path = PathUtils.toRelative( localRepository, artifact.getFile().getPath() );

            if ( !new File( path ).exists() )
            {
                File parentDirs = new File( path ).getParentFile();
                if ( parentDirs != null )
                {
                    writer.startElement( "mkdir" );
                    // Replace \ with / in the parent dir path
                    writer.addAttribute( "dir", "${maven.repo.local}/" + parentDirs.getPath().replace( '\\', '/' ) );
                    writer.endElement(); // mkdir
                }

                for ( Iterator j = project.getRepositories().iterator(); j.hasNext(); )
                {
                    Repository repository = (Repository) j.next();
                    String url = repository.getUrl();

                    if ( url.regionMatches( true, 0, "file:", 0, 5 ) && url.indexOf( basedir ) > 0 )
                    {
                        url = url.substring( url.indexOf( basedir ) + basedir.length() );
                        if ( url.startsWith( "/" ) )
                        {
                            url = url.substring( 1 );
                        }
                        if ( !url.endsWith( "/" ) && url.length() > 0 )
                        {
                            url += '/';
                        }

                        writer.startElement( "copy" );
                        writer.addAttribute( "file", url + path );
                        AntBuildWriterUtil.addWrapAttribute( writer, "copy", "tofile", "${maven.repo.local}/" + path, 3 );
                        AntBuildWriterUtil.addWrapAttribute( writer, "copy", "failonerror", "false", 3 );
                        writer.endElement(); // copy
                    }
                    else
                    {
                        writer.startElement( "get" );
                        writer.addAttribute( "src", url + '/' + path );
                        AntBuildWriterUtil.addWrapAttribute( writer, "get", "dest", "${maven.repo.local}/" + path, 3 );
                        AntBuildWriterUtil.addWrapAttribute( writer, "get", "usetimestamp", "false", 3 );
                        AntBuildWriterUtil.addWrapAttribute( writer, "get", "ignoreerrors", "true", 3 );
                        writer.endElement(); // get
                    }
                }
            }
        }

        writer.endElement(); // target

        AntBuildWriterUtil.writeLineBreak( writer );
    }

    // ----------------------------------------------------------------------
    // Convenience methods
    // ----------------------------------------------------------------------

    /**
     * Put a property in properties defined by a name and a value
     *
     * @param properties not null
     * @param name
     * @param value not null
     */
    private static void addProperty( Properties properties, String name, String value )
    {
        properties.put( name, StringUtils.isNotEmpty( value ) ? value : "" );
    }

    /**
     * @param includes an array of includes or exludes map
     * @param key a key wanted in the map, like <code>include</code> or <code>exclude</code>
     * @return a String with comma-separated value of a key in each map
     */
    private static String getCommaSeparatedList( Map[] includes, String key )
    {
        if ( ( includes == null ) || ( includes.length == 0 ) )
        {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < includes.length; i++ )
        {
            String s = (String) includes[i].get( key );
            if ( StringUtils.isEmpty( s ) )
            {
                continue;
            }

            sb.append( s );

            if ( i < ( includes.length - 1 ) )
            {
                sb.append( "," );
            }
        }

        if ( sb.length() == 0 )
        {
            return null;
        }

        return sb.toString();
    }
    
    /**
     * Flattens the specified file selector options into a simple string list. For instance, the input
     * 
     * <pre>
     * [ {include=&quot;*Test.java&quot;}, {include=&quot;*TestCase.java&quot;} ]
     * </pre>
     * 
     * is converted to
     * 
     * <pre>
     * [ &quot;*Test.java&quot;, &quot;*TestCase.java&quot; ]
     * </pre>
     * 
     * @param options The file selector options to flatten, may be <code>null</code>.
     * @return The string list, might be empty but never <code>null</code>.
     */
    private static List getSelectorList( Map[] options )
    {
        List list = new ArrayList();
        if ( options != null && options.length > 0 )
        {
            for ( int i = 0; i < options.length; i++ )
            {
                Map option = options[i];
                list.addAll( option.values() );
            }
        }
        return list;
    }

}
