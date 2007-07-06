package org.codehaus.mojo.shade.resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;

// 1. We want to process all resources that are common in all the JARs that we are processing.
// 2. At the end of processing we want to hand back the transformation of the resources.

// In my particular case I want to grab all the plexus components.xml files and aggregate them
// and then stick them in one place in the aggregated JAR.

public class ComponentsXmlResourceTransformer
    implements ResourceTransformer
{
    private Map components = new LinkedHashMap();

    public static final String COMPONENTS_XML_PATH = "META-INF/plexus/components.xml";

    public boolean canTransformResource( String resource )
    {
        return COMPONENTS_XML_PATH.equals( resource );
    }

    public void processResource( InputStream is )
        throws IOException
    {
        // We can't just read the stream because the plexus dom builder closes the stream

        File f = File.createTempFile( "maven-shade-plugin", "tmp" );

        f.deleteOnExit();
        
        String n = f.getAbsolutePath();

        OutputStream os = new FileOutputStream( f );

        IOUtil.copy( is, os );

        os.close();

        //

        Reader reader;

        Xpp3Dom newDom;

        try
        {
            reader = new FileReader( n );

            newDom = Xpp3DomBuilder.build( reader );
        }
        catch ( Exception e )
        {
            throw new IOException( "Error parsing components.xml in " + is );
        }

        Xpp3Dom[] children = newDom.getChild( "components" ).getChildren( "component" );

        for ( int i = 0; i < children.length; i++ )
        {
            Xpp3Dom component = children[i];

            String role = component.getChild( "role" ).getValue();

            Xpp3Dom child = component.getChild( "role-hint" );

            String roleHint = child != null ? child.getValue() : "";

            components.put( role + roleHint, component );
        }
    }

    public void modifyOutputStream( JarOutputStream jos )
        throws IOException
    {
        Reader reader = new FileReader( getTransformedResource() );

        jos.putNextEntry( new JarEntry( COMPONENTS_XML_PATH ) );

        IOUtil.copy( reader, jos );

        reader.close();
    }

    public boolean hasTransformedResource()
    {
        return components.size() > 0;
    }

    public File getTransformedResource()
        throws IOException
    {
        File f = File.createTempFile( "shade-maven-plugin-plx", "tmp" );

        f.deleteOnExit();

        FileWriter fileWriter = new FileWriter( f );
        try
        {
            Xpp3Dom dom = new Xpp3Dom( "component-set" );

            Xpp3Dom componentDom = new Xpp3Dom( "components" );

            dom.addChild( componentDom );

            for ( Iterator i = components.values().iterator(); i.hasNext(); )
            {
                Xpp3Dom component = (Xpp3Dom) i.next();
                componentDom.addChild( component );
            }

            Xpp3DomWriter.write( fileWriter, dom );
        }
        finally
        {
            IOUtil.close( fileWriter );
        }

        return f;
    }


}
