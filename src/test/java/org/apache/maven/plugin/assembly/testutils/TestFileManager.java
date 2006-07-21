package org.apache.maven.plugin.assembly.testutils;

import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TestFileManager
{

    public static final String TEMP_DIR_PATH = System.getProperty( "java.io.tempdir" );

    private List filesToDelete = new ArrayList();

    private final String baseFilename;

    public TestFileManager( String baseFilename )
    {
        this.baseFilename = baseFilename;
    }

    public void markForDeletion( File toDelete )
    {
        filesToDelete.add( toDelete );
    }

    public synchronized File createTempDir()
    {
        try
        {
            Thread.sleep( 20 );
        }
        catch ( InterruptedException e )
        {
        }

        File dir = new File( TEMP_DIR_PATH, baseFilename + System.currentTimeMillis() );

        dir.mkdirs();
        markForDeletion( dir );

        return dir;
    }

    public synchronized File createTempFile()
        throws IOException
    {
        File tempFile = File.createTempFile( baseFilename, "" );
        tempFile.deleteOnExit();

        return tempFile;
    }

    public void cleanUp()
        throws IOException
    {
        for ( Iterator it = filesToDelete.iterator(); it.hasNext(); )
        {
            File file = (File) it.next();

            if ( file.exists() )
            {
                if ( file.isDirectory() )
                {
                    FileUtils.deleteDirectory( file );
                }
                else
                {
                    file.delete();
                }
            }

            it.remove();
        }
    }

}
