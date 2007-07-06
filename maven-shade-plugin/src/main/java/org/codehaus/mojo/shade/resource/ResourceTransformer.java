package org.codehaus.mojo.shade.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarOutputStream;

/** @author Jason van Zyl */
public interface ResourceTransformer
{
    boolean canTransformResource( String resource );

    void processResource( InputStream is )
        throws IOException;
    
    boolean hasTransformedResource();

    void modifyOutputStream( JarOutputStream os )
        throws IOException;
}
