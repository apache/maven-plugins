package org.apache.maven.plugins.release.versions;

public class VersionParseException
    extends Exception
{
    public VersionParseException()
    {
        super();
    }

    public VersionParseException( String message )
    {
        super( message );
    }

    public VersionParseException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public VersionParseException( Throwable cause )
    {
        super( cause );
    }
}
