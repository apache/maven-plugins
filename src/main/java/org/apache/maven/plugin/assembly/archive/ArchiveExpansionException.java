package org.apache.maven.plugin.assembly.archive;


public class ArchiveExpansionException
    extends Exception
{

    public ArchiveExpansionException( String message, Throwable error )
    {
        super( message, error );
    }

    public ArchiveExpansionException( String message )
    {
        super( message );
    }

}
