package org.apache.maven.plugin.assembly.archive.archiver;

import org.apache.maven.plugin.assembly.testutils.TestFileManager;
import org.apache.maven.plugin.assembly.testutils.TestUtils;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

public class NoOpUnArchiverTest
    extends TestCase
{

    private TestFileManager fileManager = new TestFileManager( "no-op-unarchiver.test.", "" );

    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testShouldFailWhenDestDirAndDestFileAreMissing()
        throws IOException
    {
        UnArchiver unarch = new NoOpUnArchiver();

        File testFile = fileManager.createTempFile();

        unarch.setSourceFile( testFile );

        try
        {
            unarch.extract();

            fail( "Should fail when no destination is set." );
        }
        catch ( ArchiverException e )
        {
            // expected
        }
    }

    public void testShouldFailWhenSourceIsMissing()
        throws IOException
    {
        UnArchiver unarch = new NoOpUnArchiver();

        File testFile = fileManager.createTempFile();

        unarch.setDestFile( testFile );

        try
        {
            unarch.extract();

            fail( "Should fail when no source is set." );
        }
        catch ( ArchiverException e )
        {
            // expected
        }
    }

    public void testShouldCopyFromSourceFileToDestDirUsingSourceFilename()
        throws IOException, ArchiverException
    {
        UnArchiver unarch = new NoOpUnArchiver();

        File destDir = fileManager.createTempDir();

        String testContent = "This is a test.";

        File sourceFile = fileManager.createFile( "test.txt", testContent );

        unarch.setDestDirectory( destDir );
        unarch.setSourceFile( sourceFile );

        unarch.extract();

        String content = TestUtils.readFile( new File( destDir, "test.txt" ) );

        assertEquals( testContent, content );
    }

    public void testShouldCopyFromSourceFileToDestFile()
        throws IOException, ArchiverException
    {
        UnArchiver unarch = new NoOpUnArchiver();

        File destFile = fileManager.createTempFile();

        destFile.delete();

        String testContent = "This is a test.";

        File sourceFile = fileManager.createFile( "test.txt", testContent );

        unarch.setDestFile( destFile );
        unarch.setSourceFile( sourceFile );

        unarch.extract();

        String content = TestUtils.readFile( destFile );

        assertEquals( testContent, content );
    }

    public void testShouldNotCopyWhenDestFileExistsAndOverwriteIsFalse()
        throws IOException, ArchiverException
    {
        NoOpUnArchiver unarch = new NoOpUnArchiver();

        unarch.enableLogging( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        File destFile = fileManager.createTempFile();

        String testContent = "This is a test.";

        File sourceFile = fileManager.createFile( "test.txt", testContent );

        unarch.setDestFile( destFile );
        unarch.setSourceFile( sourceFile );
        unarch.setOverwrite( false );

        unarch.extract();

        String content = TestUtils.readFile( destFile ).trim();

        assertEquals( "", content );
    }

    public void testShouldCopyWhenDestFileExistsAndOverwriteIsTrue()
        throws IOException, ArchiverException
    {
        UnArchiver unarch = new NoOpUnArchiver();

        File destFile = fileManager.createTempFile();

        String testContent = "This is a test.";

        File sourceFile = fileManager.createFile( "test.txt", testContent );

        unarch.setDestFile( destFile );
        unarch.setSourceFile( sourceFile );
        unarch.setOverwrite( true );

        unarch.extract();

        String content = TestUtils.readFile( destFile );

        assertEquals( testContent, content );
    }

}
