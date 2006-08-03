package org.apache.maven.plugin.assembly.format;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.MockControl;

public class FileSetFormatterTest
    extends TestCase
{

    private MockManager mockManager;

    private TestFileManager fileManager;

    private Logger logger;

    private AssemblerConfigurationSource configSource;

    private MockControl configSourceControl;

    public void setUp()
    {
        mockManager = new MockManager();

        fileManager = new TestFileManager( "fileSetFormatter-test.", "" );

        logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        configSourceControl = MockControl.createControl( AssemblerConfigurationSource.class );
        mockManager.add( configSourceControl );

        configSource = (AssemblerConfigurationSource) configSourceControl.getMock();
    }

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testShouldReturnOriginalUnalteredDirectoryWhenLineEndingIsNull()
        throws AssemblyFormattingException, IOException
    {
        FileSet fs = new FileSet();

        FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        File dir = fileManager.createTempDir();

        File result = formatter.formatFileSetForAssembly( dir, fs );

        assertEquals( dir, result );
    }

    public void testShouldReturnOriginalUnalteredDirectoryWhenLineEndingIsKeep()
        throws AssemblyFormattingException, IOException
    {
        FileSet fs = new FileSet();
        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_KEEP );

        FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        File dir = fileManager.createTempDir();

        File result = formatter.formatFileSetForAssembly( dir, fs );

        assertEquals( dir, result );
    }

    public void testShouldReturnOriginalUnalteredDirectoryWhenIncludedFileSetIsEmpty()
        throws AssemblyFormattingException, IOException
    {
        File dir = fileManager.createTempDir();

        FileSet fs = new FileSet();

        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_LF );
        fs.setDirectory( dir.getCanonicalPath() );
        fs.addExclude( "**/*" );

        FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        File result = formatter.formatFileSetForAssembly( dir, fs );

        assertEquals( dir, result );
    }

    public void testShouldConvertLineEndingsOnTwoFiles()
        throws AssemblyFormattingException, IOException
    {
        File dir = fileManager.createTempDir();

        String filename1 = "one.txt";
        String filename2 = "two.txt";

        fileManager.createFile( dir, filename1, "Hello\nThis is a test." );
        fileManager.createFile( dir, filename2, "Hello\nThis is also a test." );

        FileSet fs = new FileSet();

        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_CRLF );
        fs.setDirectory( dir.getCanonicalPath() );
        fs.addInclude( "**/*.txt" );

        FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        File result = formatter.formatFileSetForAssembly( dir, fs );

        assertFalse( dir.equals( result ) );

        fileManager.assertFileContents( result, filename1, "Hello\r\nThis is a test.\r\n" );
        fileManager.assertFileContents( result, filename2, "Hello\r\nThis is also a test.\r\n" );
    }

    public void testShouldConvertLineEndingsOnOneFileWithAnotherExplicitlyExcluded()
        throws AssemblyFormattingException, IOException
    {
        File dir = fileManager.createTempDir();

        String filename1 = "one.txt";
        String filename2 = "two.txt";

        fileManager.createFile( dir, filename1, "Hello\nThis is a test." );
        fileManager.createFile( dir, filename2, "Hello\nThis is also a test." );

        FileSet fs = new FileSet();

        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_CRLF );
        fs.setDirectory( dir.getCanonicalPath() );
        fs.addExclude( "**/two.txt" );

        FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        File result = formatter.formatFileSetForAssembly( dir, fs );

        assertFalse( dir.equals( result ) );

        fileManager.assertFileContents( result, filename1, "Hello\r\nThis is a test.\r\n" );
        fileManager.assertFileExistence( result, filename2, false );
    }

    public void testShouldConvertLineEndingsOnOneExplicitlyIncludedFile()
        throws AssemblyFormattingException, IOException
    {
        File dir = fileManager.createTempDir();

        String filename1 = "one.txt";
        String filename2 = "two.txt";

        fileManager.createFile( dir, filename1, "Hello\nThis is a test." );
        fileManager.createFile( dir, filename2, "Hello\nThis is also a test." );

        FileSet fs = new FileSet();

        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_CRLF );
        fs.setDirectory( dir.getCanonicalPath() );
        fs.addInclude( "**/one.txt" );

        FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        File result = formatter.formatFileSetForAssembly( dir, fs );

        assertFalse( dir.equals( result ) );

        fileManager.assertFileContents( result, filename1, "Hello\r\nThis is a test.\r\n" );
        fileManager.assertFileExistence( result, filename2, false );
    }

    public void testShouldConvertLineEndingsOnOneFileAndIgnoreFileWithinDefaultExcludedDir()
        throws AssemblyFormattingException, IOException
    {
        File dir = fileManager.createTempDir();

        String filename1 = "one.txt";
        String filename2 = "CVS/two.txt";

        fileManager.createFile( dir, filename1, "Hello\nThis is a test." );
        fileManager.createFile( dir, filename2, "Hello\nThis is also a test." );

        FileSet fs = new FileSet();

        fs.setLineEnding( AssemblyFileUtils.LINE_ENDING_CRLF );
        fs.setDirectory( dir.getCanonicalPath() );

        FileSetFormatter formatter = new FileSetFormatter( configSource, logger );

        File result = formatter.formatFileSetForAssembly( dir, fs );

        assertFalse( dir.equals( result ) );

        fileManager.assertFileContents( result, filename1, "Hello\r\nThis is a test.\r\n" );
        fileManager.assertFileExistence( result, filename2, false );
    }

}
