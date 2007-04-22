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
public class WarOverlaysTest
    extends AbstractWarMojoTest
{

    private static File pomFile = new File( getBasedir(), "target/test-classes/unit/waroverlays/default.xml" );

    private WarExplodedMojo mojo;


    public void setUp()
        throws Exception
    {
        super.setUp();
        mojo = (WarExplodedMojo) lookupMojo( "exploded", pomFile );
    }

    protected File getTestDirectory()
    {
        return new File( getBasedir(), "target/test-classes/unit/waroverlays" );
    }

    public void testEnvironment()
        throws Exception
    {
        // see setup
    }

    public void testNoOverlay()
        throws Exception
    {
        // setup test data
        final String testId = "no-overlay";
        final File xmlSource = createXMLConfigDir( testId, new String[]{"web.xml"} );

        final File webAppDirectory = setUpMojo( testId, null );
        try
        {
            mojo.setWebXml( new File( xmlSource, "web.xml" ) );
            mojo.execute();

            // Validate content of the webapp
            assertDefaultContent( webAppDirectory );
            assertWebXml( webAppDirectory );
        }
        finally
        {
            cleanWebAppDirectory( webAppDirectory );
        }
    }

    public void testDefaultOverlay()
        throws Exception
    {
        // setup test data
        final String testId = "default-overlay";

        // Add an overlay
        final ArtifactStub overlay = buildWarOverlayStub( "overlay-one" );

        final File webAppDirectory = setUpMojo( testId, new ArtifactStub[]{overlay} );
        final List assertedFiles = new ArrayList();
        try
        {
            mojo.execute();
            assertedFiles.addAll( assertDefaultContent( webAppDirectory ) );
            assertedFiles.addAll( assertWebXml( webAppDirectory ) );
            assertedFiles.addAll( assertCustomContent( webAppDirectory, new String[]{"index.jsp", "login.jsp"},
                                                       "overlay file not found" ) );

            // index and login come from overlay1
            assertOverlayedFile( webAppDirectory, "overlay-one", "index.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-one", "login.jsp" );

            // Ok now check that there is no more files/directories
            final FileFilter filter = new FileFilterImpl( webAppDirectory, new String[]{"META-INF/MANIFEST.MF"} );
            assertWebAppContent( webAppDirectory, assertedFiles, filter );
        }
        finally
        {
            cleanWebAppDirectory( webAppDirectory );
        }
    }

    public void testDefaultOverlays()
        throws Exception
    {
        // setup test data
        final String testId = "default-overlays";

        // Add an overlay
        final ArtifactStub overlay = buildWarOverlayStub( "overlay-one" );
        final ArtifactStub overlay2 = buildWarOverlayStub( "overlay-two" );

        final File webAppDirectory = setUpMojo( testId, new ArtifactStub[]{overlay, overlay2} );
        final List assertedFiles = new ArrayList();
        try
        {
            mojo.execute();
            assertedFiles.addAll( assertDefaultContent( webAppDirectory ) );
            assertedFiles.addAll( assertWebXml( webAppDirectory ) );
            assertedFiles.addAll( assertCustomContent( webAppDirectory,
                                                       new String[]{"index.jsp", "login.jsp", "admin.jsp"},
                                                       "overlay file not found" ));

            // index and login come from overlay1
            assertOverlayedFile( webAppDirectory, "overlay-one", "index.jsp" );
            assertOverlayedFile( webAppDirectory, "overlay-one", "login.jsp" );

            //admin comes from overlay2
            // index and login comes from overlay1
            assertOverlayedFile( webAppDirectory, "overlay-two", "admin.jsp" );

            // Ok now check that there is no more files/directories
            final FileFilter filter = new FileFilterImpl( webAppDirectory, new String[]{"META-INF/MANIFEST.MF"} );
            assertWebAppContent( webAppDirectory, assertedFiles, filter );
        }
        finally
        {
            cleanWebAppDirectory( webAppDirectory );
        }
    }

    // Helpers


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
     * Asserts that the content of an overlayed file is correct.
     * <p/>
     * Note that the <tt>filePath</tt> is relative to both the webapp
     * directory and the overlayed directory, defined by the <tt>overlayId</tt>.
     *
     * @param webAppDirectory the webapp directory
     * @param overlayId       the id of the overlay
     * @param filePath        the relative path
     * @throws IOException if an error occured while reading the files
     */
    protected void assertOverlayedFile( File webAppDirectory, String overlayId, String filePath )
        throws IOException
    {
        final File webAppFile = new File( webAppDirectory, filePath );
        final File overlayFile = getOverlayFile( overlayId, filePath );
        assertEquals( "Wrong content for overlayed file " + filePath, FileUtils.fileRead( overlayFile ),
                      FileUtils.fileRead( webAppFile ) );

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
        final MavenProjectArtifactsStub project = new MavenProjectArtifactsStub();
        final File webAppDirectory = new File( getTestDirectory(), testId );
        final File webAppSource = createWebAppSource( testId );
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
     * Cleans up the webapp directory.
     *
     * @param webAppDirectory the directory to remove
     * @throws IOException if an error occured while removing the directory
     */
    protected void cleanWebAppDirectory( File webAppDirectory )
        throws IOException
    {
        FileUtils.deleteDirectory( webAppDirectory );
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