package org.codehaus.mojo.shade.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarOutputStream;

/**
 * Prevents duplicate copies of the license
 *
 */
public class ApacheLicenseResourceTransformer
    implements ResourceTransformer
{
    Set entries = new HashSet();
    
    public boolean canTransformResource( String resource )
    {
        String s = resource.toLowerCase();

        if ( s.startsWith( "meta-inf/license.txt" ) || s.equals( "meta-inf/license" ))
        {
            return true;
        }

        return false;
    }

    public void processResource( InputStream is )
        throws IOException
    {
       
    }

    public boolean hasTransformedResource()
    {
        return false;
    }

    public void modifyOutputStream( JarOutputStream os )
        throws IOException
    {
    }
}
