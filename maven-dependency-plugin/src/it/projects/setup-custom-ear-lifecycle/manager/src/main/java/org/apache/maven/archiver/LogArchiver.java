package org.apache.maven.archiver;

import java.io.IOException;

import org.codehaus.plexus.archiver.AbstractArchiver;
import org.codehaus.plexus.archiver.ArchiverException;

public class LogArchiver extends AbstractArchiver 
{
    private String archiveType;
 
    @Override
    protected void execute()
        throws ArchiverException, IOException
    {
        getLogger().info( "LogArchiver.execute()" );
    }
    
    @Override
    protected void close()
        throws IOException
    {
        getLogger().info( "LogArchiver.close()" );
    }
    
    @Override
    protected String getArchiveType()
    {
        return archiveType;
    }
}