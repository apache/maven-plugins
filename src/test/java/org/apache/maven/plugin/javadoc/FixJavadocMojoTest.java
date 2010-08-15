package org.apache.maven.plugin.javadoc;

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
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import junitx.util.PrivateAccessor;

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.AbstractInheritableJavaEntity;
import com.thoughtworks.qdox.model.AbstractJavaEntity;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class FixJavadocMojoTest
    extends AbstractMojoTestCase
{
    /** The vm line separator */
    private static final String EOL = System.getProperty( "line.separator" );

    /** flag to copy repo only one time */
    private static boolean TEST_REPO_CREATED = false;

    /** {@inheritDoc} */
    protected void setUp()
        throws Exception
    {
        super.setUp();

        createTestRepo();
    }

    /**
     * Create test repository in target directory.
     *
     * @throws IOException if any
     */
    private void createTestRepo()
        throws IOException
    {
        if ( TEST_REPO_CREATED )
        {
            return;
        }

        File localRepo = new File( getBasedir(), "target/local-repo/" );
        localRepo.mkdirs();

        // ----------------------------------------------------------------------
        // fix-test-1.0.jar
        // ----------------------------------------------------------------------

        File sourceDir = new File( getBasedir(), "src/test/resources/unit/fix-test/repo/" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // ----------------------------------------------------------------------
        // fix-jdk5-test-1.0.jar
        // ----------------------------------------------------------------------

        sourceDir = new File( getBasedir(), "src/test/resources/unit/fix-jdk5-test/repo/" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // ----------------------------------------------------------------------
        // fix-jdk6-test-1.0.jar
        // ----------------------------------------------------------------------

        sourceDir = new File( getBasedir(), "src/test/resources/unit/fix-jdk6-test/repo/" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // Remove SCM files
        List<String> files =
            FileUtils.getFileAndDirectoryNames( localRepo, FileUtils.getDefaultExcludesAsString(), null, true,
                                                true, true, true );
        for ( String filename : files )
        {
            File file = new File( filename );

            if ( file.isDirectory() )
            {
                FileUtils.deleteDirectory( file );
            }
            else
            {
                file.delete();
            }
        }

        TEST_REPO_CREATED = true;
    }

    /**
     * @throws Exception if any
     */
    public void testFix()
        throws Exception
    {
        File testPomBasedir = new File( getBasedir(), "target/test/unit/fix-test" );

        executeMojoAndTest( testPomBasedir, new String[] { "ClassWithJavadoc.java", "ClassWithNoJavadoc.java",
            "InterfaceWithJavadoc.java", "InterfaceWithNoJavadoc.java" } );
    }

    /**
     * @throws Exception if any
     */
    public void testFixJdk5()
        throws Exception
    {
        if ( !SystemUtils.isJavaVersionAtLeast( 1.5f ) )
        {
            getContainer().getLogger().warn(
                                             "JDK 5.0 or more is required to run fix for '" + getClass().getName()
                                                 + "#" + getName() + "()'." );
            return;
        }

        File testPomBasedir = new File( getBasedir(), "target/test/unit/fix-jdk5-test" );
        executeMojoAndTest( testPomBasedir, new String[] { "ClassWithJavadoc.java", "ClassWithNoJavadoc.java",
            "InterfaceWithJavadoc.java", "InterfaceWithNoJavadoc.java", "SubClassWithJavadoc.java" } );
    }

    /**
     * @throws Exception if any
     */
    public void testFixJdk6()
        throws Exception
    {
        if ( !SystemUtils.isJavaVersionAtLeast( 1.6f ) )
        {
            getContainer().getLogger().warn(
                                             "JDK 6.0 or more is required to run fix for '" + getClass().getName()
                                                 + "#" + getName() + "()'." );
            return;
        }

        File testPomBasedir = new File( getBasedir(), "target/test/unit/fix-jdk6-test" );
        executeMojoAndTest( testPomBasedir, new String[] { "ClassWithJavadoc.java", "InterfaceWithJavadoc.java" } );
    }

    // ----------------------------------------------------------------------
    // Test private static methods
    // ----------------------------------------------------------------------

    /**
     * @throws Throwable if any
     */
    public void testAutodetectIndentation()
        throws Throwable
    {
        String s = null;
        assertEquals( "", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "autodetectIndentation",
                                                  new Class[] { String.class }, new Object[] { s } ) );

        s = "no indentation";
        assertEquals( "", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "autodetectIndentation",
                                                  new Class[] { String.class }, new Object[] { s } ) );

        s = "no indentation with right spaces  ";
        assertEquals( "", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "autodetectIndentation",
                                                  new Class[] { String.class }, new Object[] { s } ) );

        s = "    indentation";
        assertEquals( "    ", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "autodetectIndentation",
                                                      new Class[] { String.class }, new Object[] { s } ) );

        s = "    indentation with right spaces  ";
        assertEquals( "    ", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "autodetectIndentation",
                                                      new Class[] { String.class }, new Object[] { s } ) );

        s = "\ttab indentation";
        assertEquals( "\t", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "autodetectIndentation",
                                                    new Class[] { String.class }, new Object[] { s } ) );

        s = "  \n  indentation with right spaces  ";
        assertEquals( "  \n  ", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "autodetectIndentation",
                                                        new Class[] { String.class }, new Object[] { s } ) );
    }

    /**
     * @throws Throwable if any
     */
    public void testTrimLeft()
        throws Throwable
    {
        assertEquals( "", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "trimLeft",
                                                  new Class[] { String.class }, new Object[] { null } ) );
        assertEquals( "", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "trimLeft",
                                                  new Class[] { String.class }, new Object[] { "  " } ) );
        assertEquals( "", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "trimLeft",
                                                  new Class[] { String.class }, new Object[] { "  \t  " } ) );
        assertEquals( "a", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "trimLeft",
                                                   new Class[] { String.class }, new Object[] { "a" } ) );
        assertEquals( "a", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "trimLeft",
                                                   new Class[] { String.class }, new Object[] { "  a" } ) );
        assertEquals( "a", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "trimLeft",
                                                   new Class[] { String.class }, new Object[] { "\ta" } ) );
        assertEquals( "a  ", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "trimLeft",
                                                     new Class[] { String.class }, new Object[] { "  a  " } ) );
        assertEquals( "a\t", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "trimLeft",
                                                     new Class[] { String.class }, new Object[] { "\ta\t" } ) );
    }

    /**
     * @throws Throwable if any
     */
    public void testTrimRight()
        throws Throwable
    {
        assertEquals( "", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "trimRight",
                                                  new Class[] { String.class }, new Object[] { null } ) );
        assertEquals( "", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "trimRight",
                                                  new Class[] { String.class }, new Object[] { "  " } ) );
        assertEquals( "", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "trimRight",
                                                  new Class[] { String.class }, new Object[] { "  \t  " } ) );
        assertEquals( "a", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "trimRight",
                                                   new Class[] { String.class }, new Object[] { "a" } ) );
        assertEquals( "a", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "trimRight",
                                                   new Class[] { String.class }, new Object[] { "a  " } ) );
        assertEquals( "a", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "trimRight",
                                                   new Class[] { String.class }, new Object[] { "a\t" } ) );
        assertEquals( "  a", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "trimRight",
                                                     new Class[] { String.class }, new Object[] { "  a  " } ) );
        assertEquals( "\ta", PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "trimRight",
                                                     new Class[] { String.class }, new Object[] { "\ta\t" } ) );
    }

    /**
     * @throws Throwable if any
     */
    public void testHasInheritedTag()
        throws Throwable
    {
        String content = "/** {@inheritDoc} */";
        Boolean has =
            (Boolean) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "hasInheritedTag",
                                              new Class[] { String.class }, new Object[] { content } );
        assertEquals( Boolean.TRUE, has );

        content = "/**{@inheritDoc}*/";
        has =
            (Boolean) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "hasInheritedTag",
                                              new Class[] { String.class }, new Object[] { content } );
        assertEquals( Boolean.TRUE, has );

        content = "/**{@inheritDoc  }  */";
        has =
            (Boolean) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "hasInheritedTag",
                                              new Class[] { String.class }, new Object[] { content } );
        assertEquals( Boolean.TRUE, has );

        content = "/**  {@inheritDoc  }  */";
        has =
            (Boolean) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "hasInheritedTag",
                                              new Class[] { String.class }, new Object[] { content } );
        assertEquals( Boolean.TRUE, has );

        content = "/** */";
        has =
            (Boolean) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "hasInheritedTag",
                                              new Class[] { String.class }, new Object[] { content } );
        assertEquals( Boolean.FALSE, has );

        content = "/**{  @inheritDoc  }*/";
        has =
            (Boolean) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "hasInheritedTag",
                                              new Class[] { String.class }, new Object[] { content } );
        assertEquals( Boolean.FALSE, has );

        content = "/**{@ inheritDoc}*/";
        has =
            (Boolean) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "hasInheritedTag",
                                              new Class[] { String.class }, new Object[] { content } );
        assertEquals( Boolean.FALSE, has );
    }

    /**
     * @throws Throwable if any
     */
    public void testJavadocComment()
        throws Throwable
    {
        String content = "/**" + EOL +
                " * Dummy Class." + EOL +
                " */" + EOL +
                "public class DummyClass" + EOL +
                "{" + EOL +
                "    /**" + EOL +
                "     *" + EOL +
                "     * Dummy" + EOL +
                "     *" + EOL +
                "     *      Method." + EOL +
                "     *" + EOL +
                "     * @param args not" + EOL +
                "     *" + EOL +
                "     * null" + EOL +
                "     * @param i non negative" + EOL +
                "     * @param object could" + EOL +
                "     * be" + EOL +
                "     *      null" + EOL +
                "     * @return a" + EOL +
                "     * String" + EOL +
                "     *" + EOL +
                "     * @throws Exception if" + EOL +
                "     * any" + EOL +
                "     *" + EOL +
                "     */" + EOL +
                "    public static String dummyMethod( String[] args, int i, Object object )" + EOL +
                "        throws Exception" + EOL +
                "    {" + EOL +
                "        return null;" + EOL +
                "    }" + EOL +
                "}";

        JavaDocBuilder builder = new JavaDocBuilder();
        builder.setEncoding( "UTF-8" );
        builder.addSource( new StringReader( content ) );

        JavaClass[] classes = builder.getClasses();
        JavaClass clazz = classes[0];

        JavaMethod javaMethod = clazz.getMethods()[0];

        String javadoc =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "extractOriginalJavadoc", new Class[] {
                String.class, AbstractJavaEntity.class }, new Object[] { content, javaMethod } );
        assertEquals( "    /**" + EOL +
                "     *" + EOL +
                "     * Dummy" + EOL +
                "     *" + EOL +
                "     *      Method." + EOL +
                "     *" + EOL +
                "     * @param args not" + EOL +
                "     *" + EOL +
                "     * null" + EOL +
                "     * @param i non negative" + EOL +
                "     * @param object could" + EOL +
                "     * be" + EOL +
                "     *      null" + EOL +
                "     * @return a" + EOL +
                "     * String" + EOL +
                "     *" + EOL +
                "     * @throws Exception if" + EOL +
                "     * any" + EOL +
                "     *" + EOL +
                "     */", javadoc );

        String javadocContent =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "extractOriginalJavadocContent",
                                             new Class[] { String.class, AbstractJavaEntity.class }, new Object[] {
                                                 content, javaMethod } );
        assertEquals( "     *" + EOL +
                      "     * Dummy" + EOL +
                      "     *" + EOL +
                      "     *      Method." + EOL +
                      "     *" + EOL +
                      "     * @param args not" + EOL +
                      "     *" + EOL +
                      "     * null" + EOL +
                      "     * @param i non negative" + EOL +
                      "     * @param object could" + EOL +
                      "     * be" + EOL +
                      "     *      null" + EOL +
                      "     * @return a" + EOL +
                      "     * String" + EOL +
                      "     *" + EOL +
                      "     * @throws Exception if" + EOL +
                      "     * any" + EOL +
                      "     *", javadocContent );

        String withoutEmptyJavadocLines =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "removeLastEmptyJavadocLines",
                                             new Class[] { String.class }, new Object[] { javadocContent } );
        assertTrue( withoutEmptyJavadocLines.endsWith( "any" ) );

        String methodJavadoc =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "getJavadocComment", new Class[] {
                String.class, AbstractJavaEntity.class }, new Object[] { content, javaMethod } );
        assertEquals( "     *" + EOL +
                "     * Dummy" + EOL +
                "     *" + EOL +
                "     *      Method." + EOL +
                "     *", methodJavadoc );
        withoutEmptyJavadocLines =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "removeLastEmptyJavadocLines",
                                             new Class[] { String.class }, new Object[] { methodJavadoc } );
        assertTrue( withoutEmptyJavadocLines.endsWith( "Method." ) );

        assertEquals( 5, javaMethod.getTags().length );

        DocletTag tag = javaMethod.getTags()[0];
        String tagJavadoc =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "getJavadocComment", new Class[] {
                String.class, AbstractInheritableJavaEntity.class, DocletTag.class }, new Object[] { content,
                javaMethod, tag } );
        assertEquals( "     * @param args not" + EOL +
                "     *" + EOL +
                "     * null", tagJavadoc );
        withoutEmptyJavadocLines =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "removeLastEmptyJavadocLines",
                                             new Class[] { String.class }, new Object[] { tagJavadoc } );
        assertTrue( withoutEmptyJavadocLines.endsWith( "null" ) );

        tag = javaMethod.getTags()[1];
        tagJavadoc =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "getJavadocComment", new Class[] {
                String.class, AbstractInheritableJavaEntity.class, DocletTag.class }, new Object[] { content,
                javaMethod, tag } );
        assertEquals( "     * @param i non negative", tagJavadoc );
        withoutEmptyJavadocLines =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "removeLastEmptyJavadocLines",
                                             new Class[] { String.class }, new Object[] { tagJavadoc } );
        assertTrue( withoutEmptyJavadocLines.endsWith( "negative" ) );

        tag = javaMethod.getTags()[2];
        tagJavadoc =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "getJavadocComment", new Class[] {
                String.class, AbstractInheritableJavaEntity.class, DocletTag.class }, new Object[] { content,
                javaMethod, tag } );
        assertEquals( "     * @param object could" + EOL +
                "     * be" + EOL +
                "     *      null", tagJavadoc );
        withoutEmptyJavadocLines =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "removeLastEmptyJavadocLines",
                                             new Class[] { String.class }, new Object[] { tagJavadoc } );
        assertTrue( withoutEmptyJavadocLines.endsWith( "null" ) );

        tag = javaMethod.getTags()[3];
        tagJavadoc =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "getJavadocComment", new Class[] {
                String.class, AbstractInheritableJavaEntity.class, DocletTag.class }, new Object[] { content,
                javaMethod, tag } );
        assertEquals( "     * @return a" + EOL +
                "     * String" + EOL +
                "     *", tagJavadoc );
        withoutEmptyJavadocLines =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "removeLastEmptyJavadocLines",
                                             new Class[] { String.class }, new Object[] { tagJavadoc } );
        assertTrue( withoutEmptyJavadocLines.endsWith( "String" ) );

        tag = javaMethod.getTags()[4];
        tagJavadoc =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "getJavadocComment", new Class[] {
                String.class, AbstractInheritableJavaEntity.class, DocletTag.class }, new Object[] { content,
                javaMethod, tag } );
        assertEquals( "     * @throws Exception if" + EOL +
                "     * any" + EOL +
                "     *", tagJavadoc );
        withoutEmptyJavadocLines =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "removeLastEmptyJavadocLines",
                                             new Class[] { String.class }, new Object[] { tagJavadoc } );
        assertTrue( withoutEmptyJavadocLines.endsWith( "any" ) );
    }

    /**
     * @throws Throwable if any
     */
    public void testJavadocCommentJdk5()
        throws Throwable
    {
        if ( !SystemUtils.isJavaVersionAtLeast( 1.5f ) )
        {
            getContainer().getLogger().warn(
                                             "JDK 5.0 or more is required to run fix for '" + getClass().getName()
                                                 + "#" + getName() + "()'." );
            return;
        }

        String content = "/**" + EOL +
                " * Dummy Class." + EOL +
                " */" + EOL +
                "public class DummyClass" + EOL +
                "{" + EOL +
                "    /**" + EOL +
                "     * Dummy method." + EOL +
                "     *" + EOL +
                "     * @param <K>  The Key type for the method" + EOL +
                "     * @param <V>  The Value type for the method" + EOL +
                "     * @param name The name." + EOL +
                "     * @return A map configured." + EOL +
                "     */" + EOL +
                "    public <K, V> java.util.Map<K, V> dummyMethod( String name )" + EOL +
                "    {" + EOL +
                "        return null;" + EOL +
                "    }" + EOL +
                "}";

        JavaDocBuilder builder = new JavaDocBuilder();
        builder.setEncoding( "UTF-8" );
        builder.addSource( new StringReader( content ) );

        JavaClass[] classes = builder.getClasses();
        JavaClass clazz = classes[0];

        JavaMethod javaMethod = clazz.getMethods()[0];

        String methodJavadoc =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "getJavadocComment", new Class[] {
                String.class, AbstractJavaEntity.class }, new Object[] { content, javaMethod } );
        assertEquals( "     * Dummy method." + EOL +
                "     *", methodJavadoc );

        assertEquals( 4, javaMethod.getTags().length );

        DocletTag tag = javaMethod.getTags()[0];
        String tagJavadoc =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "getJavadocComment", new Class[] {
                String.class, AbstractInheritableJavaEntity.class, DocletTag.class }, new Object[] { content,
                javaMethod, tag } );
        assertEquals( "     * @param <K>  The Key type for the method", tagJavadoc );

        tag = javaMethod.getTags()[1];
        tagJavadoc =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "getJavadocComment", new Class[] {
                String.class, AbstractInheritableJavaEntity.class, DocletTag.class }, new Object[] { content,
                javaMethod, tag } );
        assertEquals( "     * @param <V>  The Value type for the method", tagJavadoc );

        tag = javaMethod.getTags()[2];
        tagJavadoc =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "getJavadocComment", new Class[] {
                String.class, AbstractInheritableJavaEntity.class, DocletTag.class }, new Object[] { content,
                javaMethod, tag } );
        assertEquals( "     * @param name The name.", tagJavadoc );

        tag = javaMethod.getTags()[3];
        tagJavadoc =
            (String) PrivateAccessor.invoke( AbstractFixJavadocMojo.class, "getJavadocComment", new Class[] {
                String.class, AbstractInheritableJavaEntity.class, DocletTag.class }, new Object[] { content,
                javaMethod, tag } );
        assertEquals( "     * @return A map configured.", tagJavadoc );
    }

    // ----------------------------------------------------------------------
    // private methods
    // ----------------------------------------------------------------------

    /**
     * @param testPomBasedir the basedir for the test project
     * @param clazzToCompare an array of the classes name to compare
     * @throws Exception if any
     */
    private void executeMojoAndTest( File testPomBasedir, String[] clazzToCompare )
        throws Exception
    {
        prepareTestProjects( testPomBasedir.getName() );

        File testPom = new File( testPomBasedir, "pom.xml" );
        assertTrue( testPom.getAbsolutePath() + " should exist", testPom.exists() );

        FixJavadocMojo mojo = (FixJavadocMojo) lookupMojo( "fix", testPom );
        assertNotNull( mojo );

        // compile the test project
        invokeCompileGoal( testPom, mojo.getLog() );
        assertTrue( new File( testPomBasedir, "target/classes" ).exists() );

        mojo.execute();

        File expectedDir = new File( testPomBasedir, "expected/src/main/java/fix/test" );
        assertTrue( expectedDir.exists() );

        File generatedDir = new File( testPomBasedir, "target/generated/fix/test" );
        assertTrue( generatedDir.exists() );

        for ( int i = 0; i < clazzToCompare.length; i++ )
        {
            String className = clazzToCompare[i];
            assertEquals( new File( expectedDir, className ), new File( generatedDir, className ) );
        }
    }

    /**
     * Invoke the compilation on the given pom file.
     *
     * @param testPom not null
     * @param log not null
     * @throws MavenInvocationException if any
     */
    private void invokeCompileGoal( File testPom, Log log )
        throws MavenInvocationException
    {
        List<String> goals = new ArrayList<String>();
        goals.add( "clean" );
        goals.add( "compile" );
        File invokerDir = new File( getBasedir(), "target/invoker" );
        invokerDir.mkdirs();
        File invokerLogFile = FileUtils.createTempFile( "FixJavadocMojoTest", ".txt", invokerDir );
        JavadocUtil.invokeMaven( log, new File( getBasedir(), "target/local-repo" ), testPom, goals, null,
                                 invokerLogFile );
    }

    // ----------------------------------------------------------------------
    // static methods
    // ----------------------------------------------------------------------

    /**
     * Asserts that files are equal. If they are not an AssertionFailedError is thrown.
     *
     * @throws IOException if any
     */
    private static void assertEquals( File expected, File actual )
        throws IOException
    {
        assertTrue( expected.exists() );
        String expectedContent = StringUtils.unifyLineSeparators( readFile( expected ) );

        assertTrue( actual.exists() );
        String actualContent = StringUtils.unifyLineSeparators( readFile( actual ) );

        assertEquals( "Expected file: " + expected.getAbsolutePath() + ", actual file: "
            + actual.getAbsolutePath(), expectedContent, actualContent );
    }

    /**
     * @param testProjectDirName not null
     * @throws IOException if any
     */
    private static void prepareTestProjects( String testProjectDirName )
        throws IOException
    {
        File testPomBasedir = new File( getBasedir(), "target/test/unit/" + testProjectDirName );

        // Using unit test dir
        FileUtils
                 .copyDirectoryStructure(
                                          new File( getBasedir(), "src/test/resources/unit/" + testProjectDirName ),
                                          testPomBasedir );
        List<String> scmFiles = FileUtils.getDirectoryNames( testPomBasedir, "**/.svn", null, true );
        for ( String filename : scmFiles )
        {
            File dir = new File( filename );

            if ( dir.isDirectory() )
            {
                FileUtils.deleteDirectory( dir );
            }
        }
    }

    /**
     * @param file not null
     * @return the content of the given file
     * @throws IOException if any
     */
    private static String readFile( File file )
        throws IOException
    {
        Reader fileReader = null;
        try
        {
            fileReader = ReaderFactory.newReader( file, "UTF-8" );
            return IOUtil.toString( fileReader );
        }
        finally
        {
            IOUtil.close( fileReader );
        }
    }
}
