package org.apache.maven.plugin.resources.remote;

import java.net.URLClassLoader;
import java.net.URL;

/**
 * @author Jason van Zyl
 */
public class RemoteResourcesClassLoader
    extends URLClassLoader
{
    public RemoteResourcesClassLoader()
    {
        super( new URL[]{} );
    }

    public void addURL( URL url )
    {
        super.addURL( url );
    }
}
