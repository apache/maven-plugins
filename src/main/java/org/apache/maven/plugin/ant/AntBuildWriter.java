package org.apache.maven.plugin.ant;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.PathUtils;
import org.apache.tools.ant.Main;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Write an <code>build.xml<code> for <a href="http://ant.apache.org">Ant</a> 1.6.2 or above.
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
    protected final static int DEFAULT_INDENTATION_SIZE = 2;

    private MavenProject project;

    private File localRepository;

    /**
     * @param project
     * @param localRepository
     */
    public AntBuildWriter( MavenProject project, File localRepository )
    {
        this.project = project;
        this.localRepository = localRepository;
    }

    // ----------------------------------------------------------------------
    // build.xml
    // ----------------------------------------------------------------------

    protected void writeBuildXml()
        throws IOException
    {
        // TODO: parameter
        FileWriter w = new FileWriter( new File( project.getBasedir(), Main.DEFAULT_BUILD_FILENAME ) );

        XMLWriter writer = new PrettyPrintXMLWriter( w, StringUtils.repeat( " ", DEFAULT_INDENTATION_SIZE ), "UTF-8",
                                                     null );

        // ----------------------------------------------------------------------
        // <!-- comments -->
        // ----------------------------------------------------------------------

        writeHeader( writer );

        // ----------------------------------------------------------------------
        // <project/>
        // ----------------------------------------------------------------------

        writer.startElement( "project" );
        writer.addAttribute( "name", project.getArtifactId() );
        writer.addAttribute( "default", "jar" );
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

        List compileSourceRoots = removeEmptyCompileSourceRoots( project.getCompileSourceRoots() );
        writeCompileTarget( writer, compileSourceRoots );

        // ----------------------------------------------------------------------
        // <target name="jar" />
        // ----------------------------------------------------------------------
        // TODO: what if type is not JAR?
        writeJarTarget( writer );

        // ----------------------------------------------------------------------
        // <target name="compile-tests" />
        // ----------------------------------------------------------------------

        List testCompileSourceRoots = removeEmptyCompileSourceRoots( project.getTestCompileSourceRoots() );
        writeCompileTestsTarget( writer, testCompileSourceRoots );

        // ----------------------------------------------------------------------
        // <target name="test" />
        // ----------------------------------------------------------------------

        writeTestTargets( writer, testCompileSourceRoots );

        // ----------------------------------------------------------------------
        // <target name="get-deps" />
        // ----------------------------------------------------------------------
        writeGetDepsTarget( writer );

        writer.endElement(); // project

        IOUtil.close( w );
    }

    private void writeCompileTestsTarget( XMLWriter writer, List testCompileSourceRoots )
    {
        AntBuildWriterUtil.writeCommentText( writer, "Test-compilation target", 1 );

        writer.startElement( "target" );
        writer.addAttribute( "name", "compile-tests" );
        AntBuildWriterUtil.addWrapAttribute( writer, "target", "depends", "junit-present, compile", 2 );
        AntBuildWriterUtil.addWrapAttribute( writer, "target", "description", "Compile the test code", 2 );
        AntBuildWriterUtil.addWrapAttribute( writer, "target", "if", "junit.present", 2 );

        writeCompileTasks( writer, project.getBasedir(), "${maven.test.output}", testCompileSourceRoots, project
            .getBuild().getTestResources(), "${maven.build.output}" );

        writer.endElement(); // target

        AntBuildWriterUtil.writeLineBreak( writer );
    }

    private void writeTestTargets( XMLWriter writer, List testCompileSourceRoots )
    {
        AntBuildWriterUtil.writeCommentText( writer, "Run all tests", 1 );

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
            writer.addAttribute( "refid", "build.classpath" );
            writer.endElement(); // path
            writer.startElement( "pathelement" );
            writer.addAttribute( "location", "${maven.build.output}" );
            writer.endElement(); // pathelement
            writer.startElement( "pathelement" );
            writer.addAttribute( "location", "${maven.test.output}" );
            writer.endElement(); // pathelement
            writer.endElement(); // classpath

            writer.startElement( "batchtest" );
            writer.addAttribute( "todir", "${maven.test.reports}" );
            for ( Iterator i = testCompileSourceRoots.iterator(); i.hasNext(); )
            {
                writer.startElement( "fileset" );
                String testSrcDir = (String) i.next();
                writer.addAttribute( "dir", PathUtils.toRelative( project.getBasedir(), testSrcDir ) );
                /* TODO: need to get these from the test plugin somehow?
                 UnitTest unitTest = project.getBuild().getUnitTest();
                 writeIncludesExcludes( writer, unitTest.getIncludes(), unitTest.getExcludes() );
                 // TODO: m1 allows additional test exclusions via maven.ant.excludeTests
                 */
                writeIncludesExcludes( writer, Collections.singletonList( "**/*Test.java" ), Collections
                    .singletonList( "**/*Abstract*Test.java" ) );
                writer.endElement(); // fileset
            }
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

        AntBuildWriterUtil.writeLineBreak( writer );
    }

    private void writeJarTarget( XMLWriter writer )
    {
        AntBuildWriterUtil.writeCommentText( writer, "Creation target", 1 );

        writer.startElement( "target" );
        writer.addAttribute( "name", "jar" );
        writer.addAttribute( "depends", "compile,test" );
        writer.addAttribute( "description", "Create the JAR" );

        writer.startElement( "jar" );
        writer.addAttribute( "jarfile", "${maven.build.directory}/${maven.build.final.name}.jar" );
        AntBuildWriterUtil.addWrapAttribute( writer, "jar", "basedir", "${maven.build.output}", 3 );
        AntBuildWriterUtil.addWrapAttribute( writer, "jar", "excludes", "**/package.html", 3 );
        writer.endElement(); // jar

        writer.endElement(); // target

        AntBuildWriterUtil.writeLineBreak( writer );
    }

    private void writeCleanTarget( XMLWriter writer )
    {
        AntBuildWriterUtil.writeCommentText( writer, "Cleaning up target", 1 );

        writer.startElement( "target" );
        writer.addAttribute( "name", "clean" );
        writer.addAttribute( "description", "Clean the output directory" );

        writer.startElement( "delete" );
        writer.addAttribute( "dir", "${maven.build.directory}" );
        writer.endElement(); // delete

        writer.endElement(); // target

        AntBuildWriterUtil.writeLineBreak( writer );
    }

    private void writeCompileTarget( XMLWriter writer, List compileSourceRoots )
    {
        AntBuildWriterUtil.writeCommentText( writer, "Compilation target", 1 );

        writer.startElement( "target" );
        writer.addAttribute( "name", "compile" );
        writer.addAttribute( "depends", "get-deps" );
        writer.addAttribute( "description", "Compile the code" );

        writeCompileTasks( writer, project.getBasedir(), "${maven.build.output}", compileSourceRoots, project
            .getBuild().getResources(), null );

        writer.endElement(); // target

        AntBuildWriterUtil.writeLineBreak( writer );
    }

    private static void writeCompileTasks( XMLWriter writer, File basedir, String outputDirectory,
                                          List compileSourceRoots, List resources, String additionalClassesDirectory )
    {
        writer.startElement( "mkdir" );
        writer.addAttribute( "dir", outputDirectory );
        writer.endElement(); // mkdir

        if ( !compileSourceRoots.isEmpty() )
        {
            writer.startElement( "javac" );
            writer.addAttribute( "destdir", outputDirectory );
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "excludes", "**/package.html", 3 );
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "debug", "true", 3 ); // TODO: use compiler setting
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "deprecation", "true", 3 ); // TODO: use compiler setting
            AntBuildWriterUtil.addWrapAttribute( writer, "javac", "optimize", "false", 3 ); // TODO: use compiler setting

            for ( Iterator i = compileSourceRoots.iterator(); i.hasNext(); )
            {
                String srcDir = (String) i.next();

                writer.startElement( "src" );
                writer.startElement( "pathelement" );
                writer.addAttribute( "location", PathUtils.toRelative( basedir, srcDir ) );
                writer.endElement(); // pathelement
                writer.endElement(); // src
            }

            if ( additionalClassesDirectory == null )
            {
                writer.startElement( "classpath" );
                writer.addAttribute( "refid", "build.classpath" );
                writer.endElement(); // classpath
            }
            else
            {
                writer.startElement( "classpath" );
                writer.startElement( "path" );
                writer.addAttribute( "refid", "build.classpath" );
                writer.endElement(); // path
                writer.startElement( "pathelement" );
                writer.addAttribute( "location", additionalClassesDirectory );
                writer.endElement(); // pathelement
                writer.endElement(); // classpath

            }

            writer.endElement(); // javac
        }

        for ( Iterator i = resources.iterator(); i.hasNext(); )
        {
            Resource resource = (Resource) i.next();

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
                writer.addAttribute( "dir", PathUtils.toRelative( basedir, resource.getDirectory() ) );

                writeIncludesExcludes( writer, resource.getIncludes(), resource.getExcludes() );

                writer.endElement(); // fileset

                writer.endElement(); // copy
            }
        }
    }

    private static List removeEmptyCompileSourceRoots( List compileSourceRoots )
    {
        List newCompileSourceRootsList = new ArrayList();
        if ( compileSourceRoots != null )
        {
            // copy as I may be modifying it
            for ( Iterator i = compileSourceRoots.iterator(); i.hasNext(); )
            {
                String srcDir = (String) i.next();
                if ( new File( srcDir ).exists() )
                {
                    newCompileSourceRootsList.add( srcDir );
                }
            }
        }
        return newCompileSourceRootsList;
    }

    private static void writeIncludesExcludes( XMLWriter writer, List includes, List excludes )
    {
        for ( Iterator i = includes.iterator(); i.hasNext(); )
        {
            String include = (String) i.next();
            writer.startElement( "include" );
            writer.addAttribute( "name", include );
            writer.endElement(); // include
        }
        for ( Iterator i = excludes.iterator(); i.hasNext(); )
        {
            String exclude = (String) i.next();
            writer.startElement( "exclude" );
            writer.addAttribute( "name", exclude );
            writer.endElement(); // exclude
        }
    }

    private void writeGetDepsTarget( XMLWriter writer )
    {
        AntBuildWriterUtil.writeCommentText( writer, "Download dependencies target", 1 );

        writer.startElement( "target" );
        writer.addAttribute( "name", "test-offline" );

        writer.startElement( "condition" );
        writer.addAttribute( "property", "maven.mode.offline" );
        writer.startElement( "equals" );
        writer.addAttribute( "arg1", "${build.sysclasspath}" );
        writer.addAttribute( "arg2", "only" );
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

        // TODO: proxy - probably better to use wagon!

        for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            // TODO: should the artifacthandler be used instead?
            String path = PathUtils.toRelative( localRepository, artifact.getFile().getPath() );

            File parentDirs = new File( path ).getParentFile();
            if ( parentDirs != null )
            {
                writer.startElement( "mkdir" );
                writer.addAttribute( "dir", "${maven.repo.local}/" + parentDirs.getPath() );
                writer.endElement(); // mkdir
            }

            for ( Iterator j = project.getRepositories().iterator(); j.hasNext(); )
            {
                Repository repository = (Repository) j.next();

                writer.startElement( "get" );
                writer.addAttribute( "src", repository.getUrl() + "/" + path );
                AntBuildWriterUtil.addWrapAttribute( writer, "get", "dest", "${maven.repo.local}/" + path, 3 );
                AntBuildWriterUtil.addWrapAttribute( writer, "get", "usetimestamp", "true", 3 );
                AntBuildWriterUtil.addWrapAttribute( writer, "get", "ignoreerrors", "true", 3 );
                writer.endElement(); // get
            }
        }

        writer.endElement(); // target

        AntBuildWriterUtil.writeLineBreak( writer );
    }

    private void writeBuildPathDefinition( XMLWriter writer )
    {
        AntBuildWriterUtil.writeCommentText( writer, "Defining classpaths", 1 );

        writer.startElement( "path" );
        writer.addAttribute( "id", "build.classpath" );
        writer.startElement( "fileset" );
        writer.addAttribute( "dir", "${maven.repo.local}" );
        for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            writer.startElement( "include" );
            writer.addAttribute( "name", PathUtils.toRelative( localRepository, artifact.getFile().getPath() ) );
            writer.endElement(); // include
        }
        writer.endElement(); // fileset
        writer.endElement(); // path

        AntBuildWriterUtil.writeLineBreak( writer );
    }

    private void writeProperties( XMLWriter writer )
    {
        // TODO: optional in m1
        // TODO: USD properties
        AntBuildWriterUtil.writeCommentText( writer, "Build environnement properties", 1 );

        writer.startElement( "property" );
        writer.addAttribute( "file", "${user.home}/.m2/maven.properties" );
        writer.endElement(); // property

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.build.output" );
        writer.addAttribute( "value", PathUtils.toRelative( project.getBasedir(), project.getBuild()
            .getOutputDirectory() ) );
        writer.endElement(); // property

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.build.directory" );
        writer.addAttribute( "value", PathUtils.toRelative( project.getBasedir(), project.getBuild().getDirectory() ) );
        writer.endElement(); // property

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.build.final.name" );
        writer.addAttribute( "value", project.getBuild().getFinalName() );
        writer.endElement(); // property

        // TODO: property?
        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.test.reports" );
        writer.addAttribute( "value", "${maven.build.directory}/test-reports" );
        writer.endElement(); // property

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.test.output" );
        writer.addAttribute( "value", PathUtils.toRelative( project.getBasedir(), project.getBuild()
            .getTestOutputDirectory() ) );
        writer.endElement(); // property

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.repo.local" );
        writer.addAttribute( "value", "${user.home}/.m2/repository" );
        writer.endElement(); // property

        /* TODO: offline setting
         writer.startElement( "property" );
         writer.addAttribute( "name", "maven.mode.offline" );
         writer.addAttribute( "value", project.getBuild().getOutput() );
         writer.endElement(); // property
         */

        AntBuildWriterUtil.writeLineBreak( writer );
    }

    /**
     * Write comment in the file header
     *
     * @param writer
     */
    private static void writeHeader( XMLWriter writer )
    {
        AntBuildWriterUtil.writeLineBreak( writer );

        AntBuildWriterUtil.writeCommentLineBreak( writer );
        AntBuildWriterUtil.writeComment( writer, "Ant build file (http://ant.apache.org/) for Ant 1.6.2 or above." );
        AntBuildWriterUtil.writeCommentLineBreak( writer );

        AntBuildWriterUtil.writeLineBreak( writer );

        AntBuildWriterUtil.writeCommentLineBreak( writer );
        AntBuildWriterUtil.writeComment( writer, StringUtils.repeat( "=", 21 ) + " - DO NOT EDIT THIS FILE! - "
            + StringUtils.repeat( "=", 21 ) );
        AntBuildWriterUtil.writeCommentLineBreak( writer );
        AntBuildWriterUtil.writeComment( writer, " " );
        AntBuildWriterUtil.writeComment( writer, "Any modifications will be overwritten." );
        AntBuildWriterUtil.writeComment( writer, " " );
        DateFormat dateFormat = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.SHORT, Locale.US );
        AntBuildWriterUtil.writeComment( writer, "Generated by Maven Ant Plugin on "
            + dateFormat.format( new Date( System.currentTimeMillis() ) ) );
        AntBuildWriterUtil.writeComment( writer, "See: http://maven.apache.org/plugins/maven-ant-plugin/" );
        AntBuildWriterUtil.writeComment( writer, " " );
        AntBuildWriterUtil.writeCommentLineBreak( writer );

        AntBuildWriterUtil.writeLineBreak( writer );
    }
}
