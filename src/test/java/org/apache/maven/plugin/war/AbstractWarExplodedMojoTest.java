package org.apache.maven.plugin.war;

import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugin.war.stub.MavenProjectArtifactsStub;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractWarExplodedMojoTest
    extends AbstractWarMojoTest
{

    protected WarExplodedMojo mojo;

    public void setUp()
        throws Exception
    {
        super.setUp();
        mojo = (WarExplodedMojo) lookupMojo( "exploded", getPomFile() );
    }


    /**
     * Returns the pom configuration to use.
     *
     * @return the pom configuration
     */
    protected abstract File getPomFile();

    /**
     * Returns the test directory to use.
     *
     * @return the test directory
     */
    protected abstract File getTestDirectory();

    /**
     * Configures the exploded mojo for the specified test.
     * <p/>
     * If the <tt>sourceFiles</tt> parameter is <tt>null</tt>, sample
     * JSPs are created by default.
     *
     * @param testId        the id of the test
     * @param artifactStubs the dependencies (may be null)
     * @param sourceFiles the source files to create (may be null)
     * @return the webapp directory
     * @throws Exception if an error occurs while configuring the mojo
     */
    protected File setUpMojo( final String testId, ArtifactStub[] artifactStubs, String[] sourceFiles )
        throws Exception
    {
        final MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        final File webAppDirectory = new File( getTestDirectory(), testId );

        // Create the webapp sources
        File webAppSource;
        if ( sourceFiles == null )
        {
            webAppSource = createWebAppSource( testId );
        }
        else
        {
            webAppSource = createWebAppSource( testId, false );
            for ( int i = 0; i < sourceFiles.length; i++ )
            {
                String sourceFile = sourceFiles[i];
                File sample = new File( webAppSource, sourceFile );
                createFile( sample );

            }

        }

        final File classesDir = createClassesDir( testId, true );
        final File workDirectory = new File( getTestDirectory(), "/war/work-" + testId );
        createDir( workDirectory );

        if ( artifactStubs != null )
        {
            for ( int i = 0; i < artifactStubs.length; i++ )
            {
                ArtifactStub artifactStub = artifactStubs[i];
                project.addArtifact( artifactStub );
            }
        }

        configureMojo( mojo, new LinkedList(), classesDir, webAppSource, webAppDirectory, project );
        setVariableValueToObject( mojo, "workDirectory", workDirectory );

        return webAppDirectory;
    }


    /**
     * Configures the exploded mojo for the specified test.
     *
     * @param testId        the id of the test
     * @param artifactStubs the dependencies (may be null)
     * @return the webapp directory
     * @throws Exception if an error occurs while configuring the mojo
     */
    protected File setUpMojo( final String testId, ArtifactStub[] artifactStubs )
        throws Exception
    {
       return setUpMojo( testId, artifactStubs, null);
    }

    /**
     * Cleans up the webapp directory.
     *
     * @param webAppDirectory the directory to remove
     * @throws java.io.IOException if an error occured while removing the directory
     */
    protected void cleanWebAppDirectory( File webAppDirectory )
        throws IOException
    {
        FileUtils.deleteDirectory( webAppDirectory );
    }

    /**
     * Asserts the default content of the war based on the specified
     * webapp directory.
     *
     * @param webAppDirectory the webapp directory
     * @return a list of File objects that have been asserted
     */
    protected List assertDefaultContent( File webAppDirectory )
    {
        // Validate content of the webapp
        File expectedWebSourceFile = new File( webAppDirectory, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppDirectory, "org/web/app/last-exile.jsp" );

        assertTrue( "source file not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source file not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );

        final List content = new ArrayList();
        content.add( expectedWebSourceFile );
        content.add( expectedWebSource2File );

        return content;
    }


    /**
     * Asserts the web.xml file of the war based on the specified
     * webapp directory.
     *
     * @param webAppDirectory the webapp directory
     * @return a list with the web.xml File object
     */
    protected List assertWebXml( File webAppDirectory )
    {
        File expectedWEBXMLFile = new File( webAppDirectory, "WEB-INF/web.xml" );
        assertTrue( "web xml not found: " + expectedWEBXMLFile.toString(), expectedWEBXMLFile.exists() );

        final List content = new ArrayList();
        content.add( expectedWEBXMLFile );

        return content;
    }

    /**
     * Asserts custom content of the war based on the specified webapp
     * directory.
     *
     * @param webAppDirectory the webapp directory
     * @param filePaths       an array of file paths relative to the webapp directory
     * @param customMessage   a custom message if an assertion fails
     * @return a list of File objects that have been inspected
     */
    protected List assertCustomContent( File webAppDirectory, String[] filePaths, String customMessage )
    {
        final List content = new ArrayList();
        for ( int i = 0; i < filePaths.length; i++ )
        {
            String filePath = filePaths[i];
            final File expectedFile = new File( webAppDirectory, filePath );
            if ( customMessage != null )
            {
                assertTrue( customMessage + " - " + expectedFile.toString(), expectedFile.exists() );
            }
            else
            {
                assertTrue( "source file not found: " + expectedFile.toString(), expectedFile.exists() );
            }
            content.add( expectedFile );
        }
        return content;
    }

    /**
     * Asserts that the webapp contains only the specified files.
     *
     * @param webAppDirectory the webapp directory
     * @param expectedFiles   the expected files
     * @param filter          an optional filter to ignore some resources
     */
    protected void assertWebAppContent( File webAppDirectory, List expectedFiles, FileFilter filter )
    {
        final List webAppContent = new ArrayList();
        if ( filter != null )
        {
            buildFilesList( webAppDirectory, filter, webAppContent );
        }
        else
        {
            buildFilesList( webAppDirectory, new FileFilterImpl( webAppDirectory, null ), webAppContent );
        }

        // Now we have the files, sort them.
        Collections.sort( expectedFiles );
        Collections.sort( webAppContent );
        assertEquals( "Invalid webapp content, expected " + expectedFiles.size() + "file(s) " + expectedFiles +
            " but got " + webAppContent.size() + " file(s) " + webAppContent, expectedFiles, webAppContent );
    }

    /**
     * Builds the list of files and directories from the specified dir.
     * <p/>
     * Note that the filter is not used the usual way. If the filter does
     * not accept the current file, it's not added but yet the subdirectories
     * are added if any.
     *
     * @param dir     the base directory
     * @param filter  the filter
     * @param content the current content, updated recursivly
     */
    private void buildFilesList( final File dir, FileFilter filter, final List content )
    {
        final File[] files = dir.listFiles();

        for ( int i = 0; i < files.length; i++ )
        {
            File file = files[i];

            // Add the file if the filter is ok with it
            if ( filter.accept( file ) )
            {
                content.add( file );
            }

            // Even if the file is not accepted and is a directory, add it
            if ( file.isDirectory() )
            {
                buildFilesList( file, filter, content );
            }

        }
    }

    class FileFilterImpl
        implements FileFilter
    {

        private final List rejectedFilePaths;

        private final int webAppDirIndex;


        public FileFilterImpl( File webAppDirectory, String[] rejectedFilePaths )
        {
            if ( rejectedFilePaths != null )
            {
                this.rejectedFilePaths = Arrays.asList( rejectedFilePaths );
            }
            else
            {
                this.rejectedFilePaths = new ArrayList();
            }
            this.webAppDirIndex = webAppDirectory.getAbsolutePath().length() + 1;
        }

        public boolean accept( File file )
        {
            String effectiveRelativePath = buildRelativePath( file );
            return !( rejectedFilePaths.contains( effectiveRelativePath ) || file.isDirectory() );
        }

        private String buildRelativePath( File f )
        {
            return f.getAbsolutePath().substring( webAppDirIndex );
        }
    }

}
