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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.javadoc.ProxyServer.AuthAsyncProxyServlet;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManager;

/**
 * Test {@link org.apache.maven.plugin.javadoc.JavadocReport} class.
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class JavadocReportTest
    extends AbstractMojoTestCase
{
    private static final char LINE_SEPARATOR = ' ';

    /** flag to copy repo only one time */
    private static boolean TEST_REPO_CREATED = false;

    private File unit;
    
    private File localRepo;

    /** {@inheritDoc} */
    protected void setUp()
        throws Exception
    {
        super.setUp();

        unit = new File( getBasedir(), "src/test/resources/unit" );

        localRepo = new File( getBasedir(), "target/local-repo/" );
        
        createTestRepo();
    }
    

    private JavadocReport lookupMojo( File testPom )
        throws Exception
    {
        JavadocReport mojo = (JavadocReport) lookupMojo( "javadoc", testPom );

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setPlugin( new Plugin() );
        
        setVariableValueToObject( mojo, "plugin", pluginDescriptor );
        return mojo;
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

        localRepo.mkdirs();

        // ----------------------------------------------------------------------
        // UMLGraph
        // ----------------------------------------------------------------------

        File sourceDir = new File( unit, "doclet-test/artifact-doclet" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // ----------------------------------------------------------------------
        // UMLGraph-bis
        // ----------------------------------------------------------------------

        sourceDir = new File( unit, "doclet-path-test/artifact-doclet" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // ----------------------------------------------------------------------
        // commons-attributes-compiler
        // http://www.tullmann.org/pat/taglets/
        // ----------------------------------------------------------------------

        sourceDir = new File( unit, "taglet-test/artifact-taglet" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // ----------------------------------------------------------------------
        // stylesheetfile-test
        // ----------------------------------------------------------------------

        sourceDir = new File( unit, "stylesheetfile-test/artifact-stylesheetfile" );
        assertTrue( sourceDir.exists() );
        FileUtils.copyDirectoryStructure( sourceDir, localRepo );

        // ----------------------------------------------------------------------
        // helpfile-test
        // ----------------------------------------------------------------------

        sourceDir = new File( unit, "helpfile-test/artifact-helpfile" );
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
     * Convenience method that reads the contents of the specified file object into a string with a
     * <code>space</code> as line separator.
     *
     * @see #LINE_SEPARATOR
     * @param file the file to be read
     * @return a String object that contains the contents of the file
     * @throws IOException if any
     */
    private static String readFile( File file )
        throws IOException
    {
        String strTmp;
        StringBuilder str = new StringBuilder( (int) file.length() );
        BufferedReader in = new BufferedReader( new FileReader( file ) );

        try
        {
            while ( ( strTmp = in.readLine() ) != null )
            {
                str.append( LINE_SEPARATOR );
                str.append( strTmp );
            }
        }
        finally
        {
            in.close();
        }

        return str.toString();
    }

    /**
     * Test when default configuration is provided for the plugin
     *
     * @throws Exception if any
     */
    public void testDefaultConfiguration()
        throws Exception
    {
        File testPom = new File( unit, "default-configuration/default-configuration-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        mojo.execute();

        // package level generated javadoc files
        File apidocs = new File( getBasedir(), "target/test/unit/default-configuration/target/site/apidocs" );

        File generatedFile = new File( apidocs, "def/configuration/App.html" );
        assertTrue( generatedFile.exists() );
        assertTrue( FileUtils.fileRead( generatedFile, "UTF-8" ).contains( "/docs/api/java/lang/Object.html" ) );

        assertTrue( new File( apidocs, "def/configuration/AppSample.html" ).exists() );
        assertTrue( new File( apidocs, "def/configuration/package-frame.html" ).exists() );
        assertTrue( new File( apidocs, "def/configuration/package-summary.html" ).exists() );
        assertTrue( new File( apidocs, "def/configuration/package-tree.html" ).exists() );
        assertTrue( new File( apidocs, "def/configuration/package-use.html" ).exists() );

        // class level generated javadoc files
        assertTrue( new File( apidocs, "def/configuration/class-use/App.html" ).exists() );
        assertTrue( new File( apidocs, "def/configuration/class-use/AppSample.html" ).exists() );

        // project level generated javadoc files
        assertTrue( new File( apidocs, "allclasses-frame.html" ).exists() );
        assertTrue( new File( apidocs, "allclasses-noframe.html" ).exists() );
        assertTrue( new File( apidocs, "constant-values.html" ).exists() );
        assertTrue( new File( apidocs, "deprecated-list.html" ).exists() );
        assertTrue( new File( apidocs, "help-doc.html" ).exists() );
        assertTrue( new File( apidocs, "index-all.html" ).exists() );
        assertTrue( new File( apidocs, "index.html" ).exists() );
        assertTrue( new File( apidocs, "overview-tree.html" ).exists() );
        assertTrue( new File( apidocs, "package-list" ).exists() );
        assertTrue( new File( apidocs, "stylesheet.css" ).exists() );
    }

    /**
     * Method for testing the subpackages and excludePackageNames parameter
     *
     * @throws Exception if any
     */
    public void testSubpackages()
        throws Exception
    {
        File testPom = new File( unit, "subpackages-test/subpackages-test-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        mojo.execute();

        File apidocs = new File( getBasedir(), "target/test/unit/subpackages-test/target/site/apidocs" );

        // check the excluded packages
        assertFalse( new File( apidocs, "subpackages/test/excluded" ).exists() );
        assertFalse( new File( apidocs, "subpackages/test/included/exclude" ).exists() );

        // check if the classes in the specified subpackages were included
        assertTrue( new File( apidocs, "subpackages/test/App.html" ).exists() );
        assertTrue( new File( apidocs, "subpackages/test/AppSample.html" ).exists() );
        assertTrue( new File( apidocs, "subpackages/test/included/IncludedApp.html" ).exists() );
        assertTrue( new File( apidocs, "subpackages/test/included/IncludedAppSample.html" ).exists() );
    }

    public void testIncludesExcludes()
            throws Exception
    {
        File testPom = new File( unit, "file-include-exclude-test/file-include-exclude-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        mojo.execute();

        File apidocs = new File( getBasedir(), "target/test/unit/file-include-exclude-test/target/site/apidocs" );

        // check if the classes in the specified subpackages were included
        assertTrue( new File( apidocs, "subpackages/test/App.html" ).exists() );
        assertTrue( new File( apidocs, "subpackages/test/AppSample.html" ).exists() );
        assertTrue( new File( apidocs, "subpackages/test/included/IncludedApp.html" ).exists() );
        assertTrue( new File( apidocs, "subpackages/test/included/IncludedAppSample.html" ).exists() );
        assertFalse( new File( apidocs, "subpackages/test/PariahApp.html" ).exists() );
    }

    /**
     * Test the recursion and exclusion of the doc-files subdirectories.
     *
     * @throws Exception if any
     */
    public void testDocfiles()
        throws Exception
    {
        File testPom = new File( unit, "docfiles-test/docfiles-test-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        mojo.execute();

        File apidocs = new File( getBasedir(), "target/test/unit/docfiles-test/target/site/apidocs/" );

        // check if the doc-files subdirectories were copied
        assertTrue( new File( apidocs, "doc-files" ).exists() );
        assertTrue( new File( apidocs, "doc-files/included-dir1/sample-included1.gif" ).exists() );
        assertTrue( new File( apidocs, "doc-files/included-dir2/sample-included2.gif" ).exists() );
        assertFalse( new File( apidocs, "doc-files/excluded-dir1" ).exists() );
        assertFalse( new File( apidocs, "doc-files/excluded-dir2" ).exists() );

        testPom = new File( unit, "docfiles-with-java-test/docfiles-with-java-test-plugin-config.xml" );
        mojo = lookupMojo( testPom );
        mojo.execute();
    }

    /**
     * Test javadoc plugin using custom configuration. noindex, notree and nodeprecated parameters
     * were set to true.
     *
     * @throws Exception if any
     */
    public void testCustomConfiguration()
        throws Exception
    {
        File testPom = new File( unit, "custom-configuration/custom-configuration-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        mojo.execute();

        File apidocs = new File( getBasedir(), "target/test/unit/custom-configuration/target/site/apidocs" );

        // check if there is a tree page generated (notree == true)
        assertFalse( new File( apidocs, "overview-tree.html" ).exists() );
        assertFalse( new File( apidocs, "custom/configuration/package-tree.html" ).exists() );

        // check if the main index page was generated (noindex == true)
        assertFalse( new File( apidocs, "index-all.html" ).exists() );

        // check if the deprecated list and the deprecated api were generated (nodeprecated == true)
        // @todo Fix: the class-use of the deprecated api is still created eventhough the deprecated api of that class
        // is no longer generated
        assertFalse( new File( apidocs, "deprecated-list.html" ).exists() );
        assertFalse( new File( apidocs, "custom/configuration/App.html" ).exists() );

        // read the contents of the html files based on some of the parameter values
        // author == false
        String str = readFile( new File( apidocs, "custom/configuration/AppSample.html" ) );
        assertFalse( str.toLowerCase().contains( "author" ) );

        // bottom
        assertTrue( str.toUpperCase().contains( "SAMPLE BOTTOM CONTENT" ) );

        // offlineLinks
        assertTrue( str.toLowerCase().contains( "href=\"http://java.sun.com/j2se/1.4.2/docs/api/java/lang/string.html" ) );

        // header
        assertTrue( str.toUpperCase().contains( "MAVEN JAVADOC PLUGIN TEST" ) );

        // footer
        assertTrue( str.toUpperCase().contains( "MAVEN JAVADOC PLUGIN TEST FOOTER" ) );

        // nohelp == true
        assertFalse( str.toUpperCase().contains( "/HELP-DOC.HTML" ) );

        // check the wildcard (*) package exclusions -- excludePackageNames parameter
        assertTrue( new File( apidocs, "custom/configuration/exclude1/Exclude1App.html" ).exists() );
        assertFalse( new File( apidocs, "custom/configuration/exclude1/subexclude/SubexcludeApp.html" ).exists() );
        assertFalse( new File( apidocs, "custom/configuration/exclude2/Exclude2App.html" ).exists() );

        File options = new File( apidocs, "options" );
        assertTrue( options.isFile() );
        String contentOptions = null;
        Reader reader = null;
        try
        {
            reader = ReaderFactory.newPlatformReader( options );
            contentOptions = IOUtil.toString( reader );
            reader.close();
            reader = null;
        }
        finally
        {
            IOUtil.close( reader );
        }

        assertTrue( contentOptions != null );
        assertTrue( contentOptions.contains( "-link" ) );
        assertTrue( contentOptions.contains( "http://java.sun.com/j2se/" ) );
    }

    /**
     * Method to test the doclet artifact configuration
     *
     * @throws Exception if any
     */
    public void testDoclets()
        throws Exception
    {
        // ----------------------------------------------------------------------
        // doclet-test: check if the file generated by UmlGraph exists and if
        // doclet path contains the UmlGraph artifact
        // ----------------------------------------------------------------------

        File testPom = new File( unit, "doclet-test/doclet-test-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        
        MavenSession session = spy( newMavenSession( mojo.project ) );
        ProjectBuildingRequest buildingRequest = mock( ProjectBuildingRequest.class );
        when( buildingRequest.getRemoteRepositories() ).thenReturn( mojo.project.getRemoteArtifactRepositories() );
        when( session.getProjectBuildingRequest() ).thenReturn( buildingRequest );
        MavenRepositorySystemSession repositorySession = new MavenRepositorySystemSession();
        repositorySession.setLocalRepositoryManager( new SimpleLocalRepositoryManager( localRepo ) );
        when( buildingRequest.getRepositorySession() ).thenReturn( repositorySession );
        when( session.getRepositorySession() ).thenReturn( repositorySession );
        LegacySupport legacySupport = lookup( LegacySupport.class );
        legacySupport.setSession( session );
        
        setVariableValueToObject( mojo, "session", session );
        
        mojo.execute();

        File generatedFile = new File( getBasedir(), "target/test/unit/doclet-test/target/site/apidocs/graph.dot" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        File optionsFile = new File( mojo.getOutputDirectory(), "options" );
        assertTrue( optionsFile.exists() );
        String options = readFile( optionsFile );
        assertTrue( options.contains( "/target/local-repo/umlgraph/UMLGraph/2.1/UMLGraph-2.1.jar" ) );

        // ----------------------------------------------------------------------
        // doclet-path: check if the file generated by UmlGraph exists and if
        // doclet path contains the twice UmlGraph artifacts
        // ----------------------------------------------------------------------

        testPom = new File( unit, "doclet-path-test/doclet-path-test-plugin-config.xml" );
        mojo = lookupMojo( testPom );
        setVariableValueToObject( mojo, "session", session );
        mojo.execute();

        generatedFile = new File( getBasedir(), "target/test/unit/doclet-test/target/site/apidocs/graph.dot" );
        assertTrue( FileUtils.fileExists( generatedFile.getAbsolutePath() ) );

        optionsFile = new File( mojo.getOutputDirectory(), "options" );
        assertTrue( optionsFile.exists() );
        options = readFile( optionsFile );
        assertTrue( options.contains( "/target/local-repo/umlgraph/UMLGraph/2.1/UMLGraph-2.1.jar" ) );
        assertTrue( options.contains( "/target/local-repo/umlgraph/UMLGraph-bis/2.1/UMLGraph-bis-2.1.jar" ) );
    }

    

    /**
     * Method to test when the path to the project sources has an apostrophe (')
     *
     * @throws Exception if any
     */
    public void testQuotedPath()
        throws Exception
    {
        File testPom = new File( unit, "quotedpath'test/quotedpath-test-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        mojo.execute();

        File apidocs = new File( getBasedir(), "target/test/unit/quotedpath'test/target/site/apidocs" );

        // package level generated javadoc files
        assertTrue( new File( apidocs, "quotedpath/test/App.html" ).exists() );
        assertTrue( new File( apidocs, "quotedpath/test/AppSample.html" ).exists() );

        // project level generated javadoc files
        assertTrue( new File( apidocs, "index-all.html" ).exists() );
        assertTrue( new File( apidocs, "index.html" ).exists() );
        assertTrue( new File( apidocs, "overview-tree.html" ).exists() );
        assertTrue( new File( apidocs, "package-list" ).exists() );
        assertTrue( new File( apidocs, "stylesheet.css" ).exists() );
    }

    /**
     * @throws Exception if any
     */
    public void testExceptions()
        throws Exception
    {
        try
        {
            File testPom = new File( unit, "default-configuration/exception-test-plugin-config.xml" );
            JavadocReport mojo = lookupMojo( testPom );
            mojo.execute();

            fail( "Must throw exception." );
        }
        catch ( Exception e )
        {
            assertTrue( true );

            try
            {
                FileUtils.deleteDirectory( new File( getBasedir(), "exception" ) );
            }
            catch ( IOException ie )
            {
                // nop
            }
        }
    }

    /**
     * Method to test the taglet artifact configuration
     *
     * @throws Exception if any
     */
    public void testTaglets()
        throws Exception
    {
        // ----------------------------------------------------------------------
        // taglet-test: check if a taglet is used
        // ----------------------------------------------------------------------

        File testPom = new File( unit, "taglet-test/taglet-test-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        
        MavenSession session = spy( newMavenSession( mojo.project ) );
        ProjectBuildingRequest buildingRequest = mock( ProjectBuildingRequest.class );
        when( buildingRequest.getRemoteRepositories() ).thenReturn( mojo.project.getRemoteArtifactRepositories() );
        when( session.getProjectBuildingRequest() ).thenReturn( buildingRequest );
        MavenRepositorySystemSession repositorySession = new MavenRepositorySystemSession();
        repositorySession.setLocalRepositoryManager( new SimpleLocalRepositoryManager( localRepo ) );
        when( buildingRequest.getRepositorySession() ).thenReturn( repositorySession );
        when( session.getRepositorySession() ).thenReturn( repositorySession );
        LegacySupport legacySupport = lookup( LegacySupport.class );
        legacySupport.setSession( session );
        
        setVariableValueToObject( mojo, "session", session );
        
        mojo.execute();

        File apidocs = new File( getBasedir(), "target/test/unit/taglet-test/target/site/apidocs" );

        assertTrue( new File( apidocs, "index.html" ).exists() );

        File appFile = new File( apidocs, "taglet/test/App.html" );
        assertTrue( appFile.exists() );
        String appString = readFile( appFile );
        assertTrue( appString.contains( "<b>To Do:</b>" ) );
    }

    /**
     * Method to test the jdk5 javadoc
     *
     * @throws Exception if any
     */
    public void testJdk5()
        throws Exception
    {
        File testPom = new File( unit, "jdk5-test/jdk5-test-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        mojo.execute();

        File apidocs = new File( getBasedir(), "target/test/unit/jdk5-test/target/site/apidocs" );

        File index = new File( apidocs, "index.html" );
        assertTrue( FileUtils.fileExists( index.getAbsolutePath() ) );

        File overviewSummary = new File( apidocs, "overview-summary.html" );
        assertTrue( overviewSummary.exists() );
        String content = readFile( overviewSummary );
        assertTrue( content.contains( "<b>Test the package-info</b>" ) );

        File packageSummary = new File( apidocs, "jdk5/test/package-summary.html" );
        assertTrue( packageSummary.exists() );
        content = readFile( packageSummary );
        assertTrue( content.contains( "<b>Test the package-info</b>" ) );
    }

    /**
     * Test to find the javadoc executable when <code>java.home</code> is not in the JDK_HOME. In this case, try to
     * use the <code>JAVA_HOME</code> environment variable.
     *
     * @throws Exception if any
     */
    public void testToFindJavadoc()
        throws Exception
    {
        String oldJreHome = System.getProperty( "java.home" );
        System.setProperty( "java.home", "foo/bar" );

        File testPom = new File( unit, "javaHome-test/javaHome-test-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        mojo.execute();

        System.setProperty( "java.home", oldJreHome );
    }

    /**
     * Test the javadoc resources.
     *
     * @throws Exception if any
     */
    public void testJavadocResources()
        throws Exception
    {
        File testPom = new File( unit, "resources-test/resources-test-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        mojo.execute();

        File apidocs = new File( getBasedir(), "target/test/unit/resources-test/target/site/apidocs/" );

        File app = new File( apidocs, "resources/test/App.html" );
        assertTrue( app.exists() );
        String content = readFile( app );
        assertTrue( content.contains( "<img src=\"doc-files/maven-feather.png\" alt=\"Maven\">" ) );
        assertTrue( new File( apidocs, "resources/test/doc-files/maven-feather.png" ).exists() );

        File app2 = new File( apidocs, "resources/test2/App2.html" );
        assertTrue( app2.exists() );
        content = readFile( app2 );
        assertTrue( content.contains( "<img src=\"doc-files/maven-feather.png\" alt=\"Maven\">" ) );
        assertFalse( new File( apidocs, "resources/test2/doc-files/maven-feather.png" ).exists() );

        // with excludes
        testPom = new File( unit, "resources-with-excludes-test/resources-with-excludes-test-plugin-config.xml" );
        mojo = lookupMojo( testPom );
        mojo.execute();

        apidocs = new File( getBasedir(), "target/test/unit/resources-with-excludes-test/target/site/apidocs" );

        app = new File( apidocs, "resources/test/App.html" );
        assertTrue( app.exists() );
        content = readFile( app );
        assertTrue( content.contains( "<img src=\"doc-files/maven-feather.png\" alt=\"Maven\">" ) );

        float javadocVersion = (Float) getVariableValueFromObject( mojo, "fJavadocVersion" );
        if( Float.compare( 1.8f, javadocVersion ) == 0 )
        {
            // https://bugs.openjdk.java.net/browse/JDK-8032205
            assertTrue( "This bug appeared in JDK8 and was planned to be fixed in JDK9, see JDK-8032205", new File( apidocs, "resources/test/doc-files/maven-feather.png" ).exists() );
        }
        else
        {
            assertFalse( new File( apidocs, "resources/test/doc-files/maven-feather.png" ).exists() );
        }

        app2 = new File( apidocs, "resources/test2/App2.html" );
        assertTrue( app2.exists() );
        content = readFile( app2 );
        assertTrue( content.contains( "<img src=\"doc-files/maven-feather.png\" alt=\"Maven\">" ) );
        assertTrue( new File( apidocs, "resources/test2/doc-files/maven-feather.png" ).exists() );
    }

    /**
     * Test the javadoc for a POM project.
     *
     * @throws Exception if any
     */
    public void testPom()
        throws Exception
    {
        File testPom = new File( unit, "pom-test/pom-test-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        mojo.execute();

        assertFalse( new File( getBasedir(), "target/test/unit/pom-test/target/site" ).exists() );
    }

    /**
     * Test the javadoc with tag.
     *
     * @throws Exception if any
     */
    public void testTag()
        throws Exception
    {
        File testPom = new File( unit, "tag-test/tag-test-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        mojo.execute();

        File app = new File( getBasedir(), "target/test/unit/tag-test/target/site/apidocs/tag/test/App.html" );
        assertTrue( FileUtils.fileExists( app.getAbsolutePath() ) );
        String readed = readFile( app );
        assertTrue( readed.contains( ">To do something:</" ) );
        assertTrue( readed.contains( ">Generator Class:</" ) );
        assertTrue( readed.contains( ">Version:</" ) );
        assertTrue( readed.toLowerCase().contains( "</dt>" + LINE_SEPARATOR + "  <dd>1.0</dd>" )
            || readed.toLowerCase().contains( "</dt>" + LINE_SEPARATOR + "<dd>1.0</dd>" /* JDK 8 */) );
    }

    /**
     * Test newline in the header/footer parameter
     *
     * @throws Exception if any
     */
    public void testHeaderFooter()
        throws Exception
    {
        File testPom = new File( unit, "header-footer-test/header-footer-test-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        try
        {
            mojo.execute();
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( "Doesnt handle correctly newline for header or footer parameter", false );
        }

        assertTrue( true );
    }

    /**
     * Test newline in various string parameters
     *
     * @throws Exception if any
     */
    public void testNewline()
        throws Exception
    {
        File testPom = new File( unit, "newline-test/newline-test-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        try
        {
            mojo.execute();
        }
        catch ( MojoExecutionException e )
        {
            fail( "Doesn't handle correctly newline for string parameters. See options and packages files." );
        }

        assertTrue( true );
    }

    /**
     * Method to test the jdk6 javadoc
     *
     * @throws Exception if any
     */
    public void testJdk6()
        throws Exception
    {
        File testPom = new File( unit, "jdk6-test/jdk6-test-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        mojo.execute();

        File apidocs = new File( getBasedir(), "target/test/unit/jdk6-test/target/site/apidocs" );

        assertTrue( new File( apidocs, "index.html" ).exists() );

        File overviewSummary = new File( apidocs, "overview-summary.html" );
        assertTrue( overviewSummary.exists() );
        String content = readFile( overviewSummary );
        assertTrue( content.contains( "Top - Copyright &#169; All rights reserved." ) );
        assertTrue( content.contains( "Header - Copyright &#169; All rights reserved." ) );
        assertTrue( content.contains( "Footer - Copyright &#169; All rights reserved." ) );

        File packageSummary = new File( apidocs, "jdk6/test/package-summary.html" );
        assertTrue( packageSummary.exists() );
        content = readFile( packageSummary );
        assertTrue( content.contains( "Top - Copyright &#169; All rights reserved." ) );
        assertTrue( content.contains( "Header - Copyright &#169; All rights reserved." ) );
        assertTrue( content.contains( "Footer - Copyright &#169; All rights reserved." ) );
    }

    /**
     * Method to test proxy support in the javadoc
     *
     * @throws Exception if any
     */
    public void testProxy()
        throws Exception
    {
        Settings settings = new Settings();
        Proxy proxy = new Proxy();

        // dummy proxy
        proxy.setActive( true );
        proxy.setHost( "127.0.0.1" );
        proxy.setPort( 80 );
        proxy.setProtocol( "http" );
        proxy.setUsername( "toto" );
        proxy.setPassword( "toto" );
        proxy.setNonProxyHosts( "www.google.com|*.somewhere.com" );
        settings.addProxy( proxy );

        File testPom = new File( getBasedir(), "src/test/resources/unit/proxy-test/proxy-test-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        
        MavenSession session = spy( newMavenSession( mojo.project ) );
        ProjectBuildingRequest buildingRequest = mock( ProjectBuildingRequest.class );
        when( buildingRequest.getRemoteRepositories() ).thenReturn( mojo.project.getRemoteArtifactRepositories() );
        when( session.getProjectBuildingRequest() ).thenReturn( buildingRequest );
        MavenRepositorySystemSession repositorySession = new MavenRepositorySystemSession();
        repositorySession.setLocalRepositoryManager( new SimpleLocalRepositoryManager( localRepo ) );
        when( buildingRequest.getRepositorySession() ).thenReturn( repositorySession );
        when( session.getRepositorySession() ).thenReturn( repositorySession );
        LegacySupport legacySupport = lookup( LegacySupport.class );
        legacySupport.setSession( session );
        
        setVariableValueToObject( mojo, "settings", settings );
        setVariableValueToObject( mojo, "session", session );
        mojo.execute();

        File commandLine = new File( getBasedir(), "target/test/unit/proxy-test/target/site/apidocs/javadoc." + ( SystemUtils.IS_OS_WINDOWS ? "bat" : "sh" ) );
        assertTrue( FileUtils.fileExists( commandLine.getAbsolutePath() ) );
        String readed = readFile( commandLine );
        assertTrue( readed.contains( "-J-Dhttp.proxySet=true" ) );
        assertTrue( readed.contains( "-J-Dhttp.proxyHost=127.0.0.1" ) );
        assertTrue( readed.contains( "-J-Dhttp.proxyPort=80" ) );
        assertTrue( readed.contains( "-J-Dhttp.proxyUser=\\\"toto\\\"" ) );
        assertTrue( readed.contains( "-J-Dhttp.proxyPassword=\\\"toto\\\"" ) );
        assertTrue( readed.contains( "-J-Dhttp.nonProxyHosts=\\\"www.google.com|*.somewhere.com\\\"" ) );

        File options = new File( getBasedir(), "target/test/unit/proxy-test/target/site/apidocs/options" );
        assertTrue( FileUtils.fileExists( options.getAbsolutePath() ) );
        String optionsContent = readFile( options );
        // NO -link expected
        assertFalse( optionsContent.contains( "-link" ) );

        // real proxy
        ProxyServer proxyServer = null;
        AuthAsyncProxyServlet proxyServlet;
        try
        {
            proxyServlet = new AuthAsyncProxyServlet();
            proxyServer = new ProxyServer( proxyServlet );
            proxyServer.start();

            settings = new Settings();
            proxy = new Proxy();
            proxy.setActive( true );
            proxy.setHost( proxyServer.getHostName() );
            proxy.setPort( proxyServer.getPort() );
            proxy.setProtocol( "http" );
            settings.addProxy( proxy );

            mojo = lookupMojo( testPom );
            setVariableValueToObject( mojo, "settings", settings );
            setVariableValueToObject( mojo, "session", session );
            mojo.execute();
            readed = readFile( commandLine );
            assertTrue( readed.contains( "-J-Dhttp.proxySet=true" ) );
            assertTrue( readed.contains( "-J-Dhttp.proxyHost=" + proxyServer.getHostName() ) );
            assertTrue( readed.contains( "-J-Dhttp.proxyPort=" + proxyServer.getPort() ) );

            optionsContent = readFile( options );
            // -link expected
// TODO: This got disabled for now!
// This test fails since the last commit but I actually think it only ever worked by accident.
// It did rely on a commons-logging-1.0.4.pom which got resolved by a test which did run previously.
// But after updating to commons-logging.1.1.1 there is no pre-resolved artifact available in
// target/local-repo anymore, thus the javadoc link info cannot get built and the test fails
// I'll for now just disable this line of code, because the test as far as I can see _never_
// did go upstream. The remoteRepository list used is always empty!.
//
//            assertTrue( optionsContent.contains( "-link 'http://commons.apache.org/logging/apidocs'" ) );
        }
        finally
        {
            if ( proxyServer != null )
            {
                proxyServer.stop();
            }
        }

        // auth proxy
        Map<String, String> authentications = new HashMap<String, String>();
        authentications.put( "foo", "bar" );
        try
        {
            proxyServlet = new AuthAsyncProxyServlet( authentications );
            proxyServer = new ProxyServer( proxyServlet );
            proxyServer.start();

            settings = new Settings();
            proxy = new Proxy();
            proxy.setActive( true );
            proxy.setHost( proxyServer.getHostName() );
            proxy.setPort( proxyServer.getPort() );
            proxy.setProtocol( "http" );
            proxy.setUsername( "foo" );
            proxy.setPassword( "bar" );
            settings.addProxy( proxy );

            mojo = lookupMojo( testPom );
            setVariableValueToObject( mojo, "settings", settings );
            setVariableValueToObject( mojo, "session", session );
            mojo.execute();
            readed = readFile( commandLine );
            assertTrue( readed.contains( "-J-Dhttp.proxySet=true" ) );
            assertTrue( readed.contains( "-J-Dhttp.proxyHost=" + proxyServer.getHostName() ) );
            assertTrue( readed.contains( "-J-Dhttp.proxyPort=" + proxyServer.getPort() ) );
            assertTrue( readed.contains( "-J-Dhttp.proxyUser=\\\"foo\\\"" ) );
            assertTrue( readed.contains( "-J-Dhttp.proxyPassword=\\\"bar\\\"" ) );

            optionsContent = readFile( options );
            // -link expected
// see comment above (line 829)
//             assertTrue( optionsContent.contains( "-link 'http://commons.apache.org/logging/apidocs'" ) );
        }
        finally
        {
            if ( proxyServer != null )
            {
                proxyServer.stop();
            }
        }
    }

    /**
     * Method to test error or conflict in Javadoc options and in standard doclet options.
     *
     * @throws Exception if any
     */
    public void testValidateOptions()
        throws Exception
    {
        // encoding
        File testPom = new File( unit, "validate-options-test/wrong-encoding-test-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );
        try
        {
            mojo.execute();
            assertTrue( "No wrong encoding catch", false );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( "No wrong encoding catch", e.getMessage().contains( "Unsupported option <encoding/>" ) );
        }
        testPom = new File( unit, "validate-options-test/wrong-docencoding-test-plugin-config.xml" );
        mojo = lookupMojo( testPom );
        try
        {
            mojo.execute();
            assertTrue( "No wrong docencoding catch", false );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( "No wrong docencoding catch", e.getMessage().contains( "Unsupported option <docencoding/>" ) );
        }
        testPom = new File( unit, "validate-options-test/wrong-charset-test-plugin-config.xml" );
        mojo = lookupMojo( testPom );
        try
        {
            mojo.execute();
            assertTrue( "No wrong charset catch", false );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( "No wrong charset catch", e.getMessage().contains( "Unsupported option <charset/>" ) );
        }

        // locale
        testPom = new File( unit, "validate-options-test/wrong-locale-test-plugin-config.xml" );
        mojo = lookupMojo( testPom );
        try
        {
            mojo.execute();
            assertTrue( "No wrong locale catch", false );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( "No wrong locale catch", e.getMessage().contains( "Unsupported option <locale/>" ) );
        }
        testPom = new File( unit, "validate-options-test/wrong-locale-with-variant-test-plugin-config.xml" );
        mojo = lookupMojo( testPom );
        mojo.execute();
        assertTrue( "No wrong locale catch", true );

        // conflict options
        testPom = new File( unit, "validate-options-test/conflict-options-test-plugin-config.xml" );
        mojo = lookupMojo( testPom );
        try
        {
            mojo.execute();
            assertTrue( "No conflict catch", false );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( "No conflict catch", e.getMessage().contains( "Option <nohelp/> conflicts with <helpfile/>" ) );
        }
    }

    /**
     * Method to test the <code>&lt;tagletArtifacts/&gt;</code> parameter.
     *
     * @throws Exception if any
     */
    public void testTagletArtifacts()
        throws Exception
    {
        File testPom = new File( unit, "tagletArtifacts-test/tagletArtifacts-test-plugin-config.xml" );
        JavadocReport mojo = lookupMojo( testPom );

        MavenSession session = spy( newMavenSession( mojo.project ) );
        ProjectBuildingRequest buildingRequest = mock( ProjectBuildingRequest.class );
        when( buildingRequest.getRemoteRepositories() ).thenReturn( mojo.project.getRemoteArtifactRepositories() );
        when( session.getProjectBuildingRequest() ).thenReturn( buildingRequest );
        MavenRepositorySystemSession repositorySession = new MavenRepositorySystemSession();
        repositorySession.setLocalRepositoryManager( new SimpleLocalRepositoryManager( localRepo ) );
        when( buildingRequest.getRepositorySession() ).thenReturn( repositorySession );
        when( session.getRepositorySession() ).thenReturn( repositorySession );
        LegacySupport legacySupport = lookup( LegacySupport.class );
        legacySupport.setSession( session );
        setVariableValueToObject( mojo, "session", session );

        mojo.execute();

        File optionsFile = new File( mojo.getOutputDirectory(), "options" );
        assertTrue( optionsFile.exists() );
        String options = readFile( optionsFile );
        // count -taglet
        assertEquals( 20, StringUtils.countMatches( options, LINE_SEPARATOR + "-taglet" + LINE_SEPARATOR ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoAggregatorTypeTaglet" ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoComponentFieldTaglet" ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoConfiguratorTypeTaglet" ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoExecuteTypeTaglet" ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoExecutionStrategyTypeTaglet" ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoGoalTypeTaglet" ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoInheritByDefaultTypeTaglet" ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoInstantiationStrategyTypeTaglet" ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoParameterFieldTaglet" ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoPhaseTypeTaglet" ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoReadOnlyFieldTaglet" ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoRequiredFieldTaglet" ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoRequiresDependencyResolutionTypeTaglet" ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoRequiresDirectInvocationTypeTaglet" ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoRequiresOnLineTypeTaglet" ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoRequiresProjectTypeTaglet" ) );
        assertTrue( options.contains( "org.apache.maven.tools.plugin.javadoc.MojoRequiresReportsTypeTaglet" ) );
        assertTrue( options.contains( "org.codehaus.plexus.javadoc.PlexusConfigurationTaglet" ) );
        assertTrue( options.contains( "org.codehaus.plexus.javadoc.PlexusRequirementTaglet" ) );
        assertTrue( options.contains( "org.codehaus.plexus.javadoc.PlexusComponentTaglet" ) );
    }

    /**
     * Method to test the <code>&lt;stylesheetfile/&gt;</code> parameter.
     *
     * @throws Exception if any
     */
    public void testStylesheetfile()
        throws Exception
    {
        File testPom = new File( unit, "stylesheetfile-test/pom.xml" );

        JavadocReport mojo = lookupMojo( testPom );
        assertNotNull( mojo );

        MavenSession session = spy( newMavenSession( mojo.project ) );
        ProjectBuildingRequest buildingRequest = mock( ProjectBuildingRequest.class );
        when( buildingRequest.getRemoteRepositories() ).thenReturn( mojo.project.getRemoteArtifactRepositories() );
        when( session.getProjectBuildingRequest() ).thenReturn( buildingRequest );
        MavenRepositorySystemSession repositorySession = new MavenRepositorySystemSession();
        repositorySession.setLocalRepositoryManager( new SimpleLocalRepositoryManager( localRepo ) );
        when( buildingRequest.getRepositorySession() ).thenReturn( repositorySession );
        when( session.getRepositorySession() ).thenReturn( repositorySession );
        LegacySupport legacySupport = lookup( LegacySupport.class );
        legacySupport.setSession( session );
        setVariableValueToObject( mojo, "session", session );

        File apidocs = new File( getBasedir(), "target/test/unit/stylesheetfile-test/target/site/apidocs" );

        File stylesheetfile = new File( apidocs, "stylesheet.css" );
        File options = new File( apidocs, "options" );

        // stylesheet == maven OR java
        setVariableValueToObject( mojo, "stylesheet", "javamaven" );

        try
        {
            mojo.execute();
            assertTrue( false );
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }

        // stylesheet == java
        setVariableValueToObject( mojo, "stylesheet", "java" );
        mojo.execute();

        String content = readFile( stylesheetfile );
        assertTrue( content.contains( "/* Javadoc style sheet */" ) );

        String optionsContent = readFile( options );
        assertFalse( optionsContent.contains( "-stylesheetfile" ) );

        // stylesheet == maven
        setVariableValueToObject( mojo, "stylesheet", "maven" );
        mojo.execute();

        content = readFile( stylesheetfile );
        assertTrue( content.contains( "/* Javadoc style sheet */" )
            && content.contains( "Licensed to the Apache Software Foundation (ASF) under one" ) );

        optionsContent = readFile( options );
        assertTrue( optionsContent.contains( "-stylesheetfile" ) );
        assertTrue( optionsContent.contains( "'" + stylesheetfile.getAbsolutePath().replaceAll( "\\\\", "/" ) + "'" ) );

        // stylesheetfile defined as a project resource
        setVariableValueToObject( mojo, "stylesheet", null );
        setVariableValueToObject( mojo, "stylesheetfile", "com/mycompany/app/javadoc/css/stylesheet.css" );
        mojo.execute();

        content = readFile( stylesheetfile );
        assertTrue( content.contains( "/* Custom Javadoc style sheet in project */" ) );

        optionsContent = readFile( options );
        assertTrue( optionsContent.contains( "-stylesheetfile" ) );
        File stylesheetResource =
            new File( unit, "stylesheetfile-test/src/main/resources/com/mycompany/app/javadoc/css/stylesheet.css" );
        assertTrue( optionsContent.contains( "'" + stylesheetResource.getAbsolutePath().replaceAll( "\\\\", "/" )
            + "'" ) );

        // stylesheetfile defined in a javadoc plugin dependency
        setVariableValueToObject( mojo, "stylesheetfile", "com/mycompany/app/javadoc/css2/stylesheet.css" );
        mojo.execute();

        content = readFile( stylesheetfile );
        assertTrue( content.contains( "/* Custom Javadoc style sheet in artefact */" ) );

        optionsContent = readFile( options );
        assertTrue( optionsContent.contains( "-stylesheetfile" ) );
        assertTrue( optionsContent.contains( "'" + stylesheetfile.getAbsolutePath().replaceAll( "\\\\", "/" ) + "'" ) );

        // stylesheetfile defined as file
        File css =
            new File( unit, "stylesheetfile-test/src/main/resources/com/mycompany/app/javadoc/css3/stylesheet.css" );
        setVariableValueToObject( mojo, "stylesheetfile", css.getAbsolutePath() );
        mojo.execute();

        content = readFile( stylesheetfile );
        assertTrue( content.contains( "/* Custom Javadoc style sheet as file */" ) );

        optionsContent = readFile( options );
        assertTrue( optionsContent.contains( "-stylesheetfile" ) );
        stylesheetResource =
            new File( unit, "stylesheetfile-test/src/main/resources/com/mycompany/app/javadoc/css3/stylesheet.css" );
        assertTrue( optionsContent.contains( "'" + stylesheetResource.getAbsolutePath().replaceAll( "\\\\", "/" ) + "'" ) );
    }

    /**
     * Method to test the <code>&lt;helpfile/&gt;</code> parameter.
     *
     * @throws Exception if any
     */
    public void testHelpfile()
        throws Exception
    {
        File testPom = new File( unit, "helpfile-test/pom.xml" );
        
        JavadocReport mojo = lookupMojo( testPom );
        assertNotNull( mojo );

        MavenSession session = spy( newMavenSession( mojo.project ) );
        ProjectBuildingRequest buildingRequest = mock( ProjectBuildingRequest.class );
        when( buildingRequest.getRemoteRepositories() ).thenReturn( mojo.project.getRemoteArtifactRepositories() );
        when( session.getProjectBuildingRequest() ).thenReturn( buildingRequest );
        MavenRepositorySystemSession repositorySession = new MavenRepositorySystemSession();
        repositorySession.setLocalRepositoryManager( new SimpleLocalRepositoryManager( localRepo ) );
        when( buildingRequest.getRepositorySession() ).thenReturn( repositorySession );
        when( session.getRepositorySession() ).thenReturn( repositorySession );
        LegacySupport legacySupport = lookup( LegacySupport.class );
        legacySupport.setSession( session );
        setVariableValueToObject( mojo, "session", session );

        File apidocs = new File( getBasedir(), "target/test/unit/helpfile-test/target/site/apidocs" );

        File helpfile = new File( apidocs, "help-doc.html" );
        File options = new File( apidocs, "options" );

        // helpfile by default
        mojo.execute();

        String content = readFile( helpfile );
        assertTrue( content.contains( "<!-- Generated by javadoc" ) );

        String optionsContent = readFile( options );
        assertFalse( optionsContent.contains( "-helpfile" ) );

        // helpfile defined in a javadoc plugin dependency
        setVariableValueToObject( mojo, "helpfile", "com/mycompany/app/javadoc/helpfile/help-doc.html" );
        
        setVariableValueToObject( mojo, "session", session );
        
        mojo.execute();

        content = readFile( helpfile );
        assertTrue( content.contains( "<!--  Help file from artefact -->" ) );

        optionsContent = readFile( options );
        assertTrue( optionsContent.contains( "-helpfile" ) );
        File help = new File( apidocs, "help-doc.html" );
        assertTrue( optionsContent.contains( "'" + help.getAbsolutePath().replaceAll( "\\\\", "/" ) + "'" ) );

        // helpfile defined as a project resource
        setVariableValueToObject( mojo, "helpfile", "com/mycompany/app/javadoc/helpfile2/help-doc.html" );
        mojo.execute();

        content = readFile( helpfile );
        assertTrue( content.contains( "<!--  Help file from file -->" ) );

        optionsContent = readFile( options );
        assertTrue( optionsContent.contains( "-helpfile" ) );
        help = new File( unit, "helpfile-test/src/main/resources/com/mycompany/app/javadoc/helpfile2/help-doc.html" );
        assertTrue( optionsContent.contains( "'" + help.getAbsolutePath().replaceAll( "\\\\", "/" ) + "'" ) );

        // helpfile defined as file
        help = new File( unit, "helpfile-test/src/main/resources/com/mycompany/app/javadoc/helpfile2/help-doc.html" );
        setVariableValueToObject( mojo, "helpfile", help.getAbsolutePath() );
        mojo.execute();

        content = readFile( helpfile );
        assertTrue( content.contains( "<!--  Help file from file -->" ) );

        optionsContent = readFile( options );
        assertTrue( optionsContent.contains( "-helpfile" ) );
        assertTrue( optionsContent.contains( "'" + help.getAbsolutePath().replaceAll( "\\\\", "/" ) + "'" ) );
    }
}
