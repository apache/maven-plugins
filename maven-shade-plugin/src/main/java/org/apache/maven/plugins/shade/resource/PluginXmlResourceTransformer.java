package org.apache.maven.plugins.shade.resource;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;

/**
 * A resource processor that aggregates Maven <code>plugin.xml</code> files.
 * 
 * @author Robert Scholte
 * @since 3.0
 */
public class PluginXmlResourceTransformer
    implements ResourceTransformer
{
    private List<Xpp3Dom> mojos = new LinkedList<Xpp3Dom>();

    public static final String PLUGIN_XML_PATH = "META-INF/maven/plugin.xml";

    public boolean canTransformResource( String resource )
    {
        return PLUGIN_XML_PATH.equals( resource );
    }

    public void processResource( String resource, InputStream is, List<Relocator> relocators )
        throws IOException
    {
        Xpp3Dom newDom;

        try
        {
            BufferedInputStream bis = new BufferedInputStream( is )
            {
                public void close()
                    throws IOException
                {
                    // leave ZIP open
                }
            };

            Reader reader = ReaderFactory.newXmlReader( bis );

            newDom = Xpp3DomBuilder.build( reader );
        }
        catch ( Exception e )
        {
            throw (IOException) new IOException( "Error parsing plugin.xml in " + is ).initCause( e );
        }

        // Only try to merge in mojos if there are some elements in the plugin
        if ( newDom.getChild( "mojos" ) == null )
        {
            return;
        }

        for ( Xpp3Dom mojo : newDom.getChild( "mojos" ).getChildren( "mojo" ) )
        {

            String impl = getValue( mojo, "implementation" );
            impl = getRelocatedClass( impl, relocators );
            setValue( mojo, "implementation", impl );

            Xpp3Dom parameters = mojo.getChild( "parameters" );
            if ( parameters != null )
            {
                for ( Xpp3Dom parameter : parameters.getChildren() )
                {
                    String type = getValue( parameter, "type" );
                    type = getRelocatedClass( type, relocators );
                    setValue( parameter, "type", type );
                }
            }

            Xpp3Dom configuration = mojo.getChild( "configuration" );
            if ( configuration != null )
            {
                for ( Xpp3Dom configurationEntry : configuration.getChildren() )
                {
                    String implementation = getAttribute( configurationEntry, "implementation" );
                    implementation = getRelocatedClass( implementation, relocators );
                    setAttribute( configurationEntry, "implementation", implementation );
                }
            }

            Xpp3Dom requirements = mojo.getChild( "requirements" );
            if ( requirements != null && requirements.getChildCount() > 0 )
            {
                for ( Xpp3Dom requirement : requirements.getChildren() )
                {
                    String requiredRole = getValue( requirement, "role" );
                    requiredRole = getRelocatedClass( requiredRole, relocators );
                    setValue( requirement, "role", requiredRole );
                }
            }
            mojos.add( mojo );
        }
    }

    public void modifyOutputStream( JarOutputStream jos )
        throws IOException
    {
        byte[] data = getTransformedResource();

        jos.putNextEntry( new JarEntry( PLUGIN_XML_PATH ) );

        IOUtil.copy( data, jos );

        mojos.clear();
    }

    public boolean hasTransformedResource()
    {
        return !mojos.isEmpty();
    }

    byte[] getTransformedResource()
        throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream( 1024 * 4 );

        Writer writer = WriterFactory.newXmlWriter( baos );
        try
        {
            Xpp3Dom dom = new Xpp3Dom( "plugin" );

            Xpp3Dom componentDom = new Xpp3Dom( "mojos" );

            dom.addChild( componentDom );

            for ( Xpp3Dom mojo : mojos )
            {
                componentDom.addChild( mojo );
            }

            Xpp3DomWriter.write( writer, dom );

            writer.close();
            writer = null;
        }
        finally
        {
            IOUtil.close( writer );
        }

        return baos.toByteArray();
    }

    private String getRelocatedClass( String className, List<Relocator> relocators )
    {
        if ( className != null && className.length() > 0 && relocators != null )
        {
            for ( Relocator relocator : relocators )
            {
                if ( relocator.canRelocateClass( className ) )
                {
                    return relocator.relocateClass( className );
                }
            }
        }

        return className;
    }

    private static String getValue( Xpp3Dom dom, String element )
    {
        Xpp3Dom child = dom.getChild( element );

        return ( child != null && child.getValue() != null ) ? child.getValue() : "";
    }

    private static void setValue( Xpp3Dom dom, String element, String value )
    {
        Xpp3Dom child = dom.getChild( element );

        if ( child == null || value == null || value.length() <= 0 )
        {
            return;
        }

        child.setValue( value );
    }

    private static String getAttribute( Xpp3Dom dom, String attribute )
    {
        return ( dom.getAttribute( attribute ) != null ) ? dom.getAttribute( attribute ) : "";
    }

    private static void setAttribute( Xpp3Dom dom, String attribute, String value )
    {
        String attr = dom.getAttribute( attribute );

        if ( attr == null || value == null || value.length() <= 0 )
        {
            return;
        }

        dom.setAttribute( attribute, value );
    }

}
