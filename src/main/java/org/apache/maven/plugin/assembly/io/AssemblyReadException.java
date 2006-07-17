package org.apache.maven.plugin.assembly.io;


public class AssemblyReadException
    extends Exception
{

    public AssemblyReadException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public AssemblyReadException( String message )
    {
        super( message );
    }

}
