package org.apache.maven.plugin.resources.remote.it.support;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class TestUtils
{
    public static File getTestDir( final String name )
        throws IOException, URISyntaxException
    {
        ClassLoader cloader = Thread.currentThread().getContextClassLoader();
        URL resource = cloader.getResource( name );

        if ( resource == null )
        {
            throw new IOException( "Cannot find test directory: " + name );
        }

        return new File( new URI( resource.toExternalForm() ).normalize().getPath() );
    }

    public static File getBaseDir()
    {
        File result = new File( System.getProperty( "basedir", "." ) );
        try
        {
            return result.getCanonicalFile();
        }
        catch ( IOException e )
        {
            return result.getAbsoluteFile();
        }
    }
}
