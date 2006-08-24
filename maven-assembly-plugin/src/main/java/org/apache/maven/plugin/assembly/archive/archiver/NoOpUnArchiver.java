package org.apache.maven.plugin.assembly.archive.archiver;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

public class NoOpUnArchiver
    extends AbstractLogEnabled
    implements UnArchiver
{

    private File destDir;
    private File destFile;
    
    private boolean overwrite = false;
    
    private File sourceFile;

    public void extract()
        throws ArchiverException, IOException
    {
        if ( sourceFile == null || !sourceFile.exists() )
        {
            throw new ArchiverException( "Source file is not available: " + sourceFile );
        }
        
        if ( destFile == null )
        {
            if ( destDir != null )
            {
                String filename = sourceFile.getName();
                
                destFile = new File( destDir, filename );
            }
            else
            {
                throw new ArchiverException( "No destination configured. Please set either destDirectory or destFile property for source file: " + sourceFile );
            }
        }
        
        if ( !overwrite && destFile.exists() )
        {
            getLogger().debug( "Destination file: " + destFile + " already exists. Not overwriting." );
            return;
        }
        
        FileUtils.copyFile( sourceFile, destFile );
    }

    public File getDestDirectory()
    {
        return null;
    }

    public File getDestFile()
    {
        return null;
    }

    public File getSourceFile()
    {
        return null;
    }

    public void setDestDirectory( File destDir )
    {
        this.destDir = destDir;
    }

    public void setDestFile( File destFile )
    {
        this.destFile = destFile;
    }

    public void setOverwrite( boolean overwrite )
    {
        this.overwrite = overwrite;
    }

    public void setSourceFile( File sourceFile )
    {
        this.sourceFile = sourceFile;
    }

}
