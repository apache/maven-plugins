package org.apache.maven.plugin.assembly;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.codehaus.plexus.util.FileUtils;

public class TestUtils
{
    
    private static Set toDelete = new HashSet();
    
    public static void cleanUp() throws IOException
    {
        
        for ( Iterator it = toDelete.iterator(); it.hasNext(); )
        {
            File file = (File) it.next();
            
            if ( file.exists() )
            {
                if ( file.isDirectory() )
                {
                    FileUtils.deleteDirectory( file );
                }
                else
                {
                    file.delete();
                }
            }
        }
    }
    
    public static void markForDeletion( File file )
    {
        toDelete.add( file );
    }

    public static File createTempBasedir() throws InterruptedException
    {
        Thread.sleep( 100 );
        
        File basedir = new File( System.getProperty( "java.io.tmpdir" ), "basedir." + System.currentTimeMillis() );
        
        toDelete.add( basedir );
        
        return basedir;
    }

    public static File findFileForClasspathResource( String resourceName )
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        
        URL resource = cl.getResource( resourceName );
        
        if ( resource != null )
        {
            return new File( resource.getPath() );
        }

        return null;
    }

}
