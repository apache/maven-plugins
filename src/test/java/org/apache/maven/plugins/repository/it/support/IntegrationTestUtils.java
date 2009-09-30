package org.apache.maven.plugins.repository.it.support;

import static org.codehaus.plexus.util.IOUtil.close;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class IntegrationTestUtils
{
    
    private static String cliPluginPrefix;
    
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

    public static String getCliPluginPrefix()
        throws IOException
    {
        if ( cliPluginPrefix == null )
        {
            URL resource = Thread.currentThread().getContextClassLoader().getResource( "META-INF/maven/plugin.xml" );

            InputStream stream = null;
            try
            {
                stream = resource.openStream();
                Xpp3Dom pluginDom;
                try
                {
                    pluginDom = Xpp3DomBuilder.build( new InputStreamReader( stream ) );
                }
                catch ( XmlPullParserException e )
                {
                    IOException err = new IOException( "Failed to parse plugin descriptor for groupId:artifactId:version prefix. Reason: " + e.getMessage() );
                    err.initCause( e );
                    
                    throw err;
                }
                

                String artifactId = pluginDom.getChild( "artifactId" ).getValue();
                String groupId = pluginDom.getChild( "groupId" ).getValue();
                String version = pluginDom.getChild( "version" ).getValue();
                
                cliPluginPrefix = groupId + ":" + artifactId + ":" + version + ":";
            }
            finally
            {
                close( stream );
            }
        }
        
        return cliPluginPrefix;
    }
}
