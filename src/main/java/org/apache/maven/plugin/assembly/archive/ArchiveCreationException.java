package org.apache.maven.plugin.assembly.archive;


public class ArchiveCreationException
    extends Exception
{

    public ArchiveCreationException( String message, Throwable error )
    {
        super( message, error );
    }

    public ArchiveCreationException( String message )
    {
        super( message );
    }

}
