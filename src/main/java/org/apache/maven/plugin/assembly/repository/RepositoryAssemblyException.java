package org.apache.maven.plugin.assembly.repository;

/**
 * @author Jason van Zyl
 */
public class RepositoryAssemblyException
    extends Exception
{
    public RepositoryAssemblyException( String string )
    {
        super( string );
    }

    public RepositoryAssemblyException( String string,
                                        Throwable throwable )
    {
        super( string, throwable );
    }

    public RepositoryAssemblyException( Throwable throwable )
    {
        super( throwable );
    }
}
