package org.apache.maven.plugin.assembly;


public class InvalidAssemblerConfigurationException
    extends Exception
{

    public InvalidAssemblerConfigurationException( String message, Throwable error )
    {
        super( message, error );
    }

    public InvalidAssemblerConfigurationException( String message )
    {
        super( message );
    }

}
