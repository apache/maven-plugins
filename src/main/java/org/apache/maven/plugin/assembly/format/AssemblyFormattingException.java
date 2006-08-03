package org.apache.maven.plugin.assembly.format;


public class AssemblyFormattingException
    extends Exception
{

    public AssemblyFormattingException( String message, Throwable error )
    {
        super( message, error );
    }

    public AssemblyFormattingException( String message )
    {
        super( message );
    }

}
