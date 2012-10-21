package org.apache.maven.archiver;

import java.io.File;

import org.codehaus.plexus.archiver.AbstractUnArchiver;
import org.codehaus.plexus.archiver.ArchiverException;

public class LogUnArchiver extends AbstractUnArchiver
{
 
    @Override
    protected void execute()
        throws ArchiverException
    {
        getLogger().info( "LogUnArchiver.execute()" );
    }
    
    @Override
    protected void execute( String path, File outputDirectory )
        throws ArchiverException
    {
        getLogger().info( "LogUnArchiver.execute( String path, File outputDirectory )" );
        getLogger().info( "  path = " + path );
        getLogger().info( "  outputDirectory = " + outputDirectory );
    }
}