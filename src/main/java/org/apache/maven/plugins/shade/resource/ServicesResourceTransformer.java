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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugins.shade.relocation.Relocator;

import com.google.common.io.LineReader;

/**
 * Resources transformer that relocates classes in META-INF/services and appends entries in META-INF/services resources
 * into a single resource. For example, if there are several META-INF/services/org.apache.maven.project.ProjectBuilder
 * resources spread across many JARs the individual entries will all be concatenated into a single
 * META-INF/services/org.apache.maven.project.ProjectBuilder resource packaged into the resultant JAR produced by the
 * shading process.
 */
public class ServicesResourceTransformer
    implements ResourceTransformer
{

    private static final String SERVICES_PATH = "META-INF/services";

    private Map<String, ServiceStream> serviceEntries = new HashMap<String, ServiceStream>();

    private List<Relocator> relocators;

    public boolean canTransformResource( String resource )
    {
        if ( resource.startsWith( SERVICES_PATH ) )
        {
            return true;
        }

        return false;
    }

    public void processResource( String resource, InputStream is, final List<Relocator> relocators )
        throws IOException
    {
        ServiceStream out = serviceEntries.get( resource );
        if ( out == null )
        {
            out = new ServiceStream();
            serviceEntries.put( resource, out );
        }

        final ServiceStream fout = out;

        final String content = IOUtils.toString( is );
        StringReader reader = new StringReader( content );
        LineReader lineReader = new LineReader( reader );
        String line;
        while ( ( line = lineReader.readLine() ) != null )
        {
            String relContent = line;
            for ( Relocator relocator : relocators )
            {
                relContent = relocator.applyToSourceContent( relContent );
            }
            fout.append( relContent + "\n" );
        }

        if ( this.relocators == null )
        {
            this.relocators = relocators;
        }
    }
    public boolean hasTransformedResource()
    {
        return serviceEntries.size() > 0;
    }

    public void modifyOutputStream( JarOutputStream jos )
        throws IOException
    {
        for ( Map.Entry<String, ServiceStream> entry : serviceEntries.entrySet() )
        {
            String key = entry.getKey();
            ServiceStream data = entry.getValue();

            if ( relocators != null )
            {
                key = key.substring( SERVICES_PATH.length() + 1 );
                for ( Relocator relocator : relocators )
                {
                    if ( relocator.canRelocateClass( key ) )
                    {
                        key = relocator.relocateClass( key );
                        break;
                    }
                }

                key = SERVICES_PATH + '/' + key;
            }

            jos.putNextEntry( new JarEntry( key ) );


            //read the content of service file for candidate classes for relocation
            PrintWriter writer = new PrintWriter( jos );
            InputStreamReader streamReader = new InputStreamReader( data.toInputStream() );
            BufferedReader reader = new BufferedReader( streamReader );
            String className;

            while ( ( className = reader.readLine() ) != null )
            {

                if ( relocators != null )
                {
                    for ( Relocator relocator : relocators )
                    {
                        //if the class can be relocated then relocate it
                        if ( relocator.canRelocateClass( className ) )
                        {
                            className = relocator.applyToSourceContent( className );
                            break;
                        }
                    }
                }

                writer.println( className );
                writer.flush();
            }

            reader.close();
            data.reset();
        }
    }

    static class ServiceStream
        extends ByteArrayOutputStream
    {

        public ServiceStream()
        {
            super( 1024 );
        }

        public void append( String content )
            throws IOException
        {
            if ( count > 0 && buf[count - 1] != '\n' && buf[count - 1] != '\r' )
            {
                write( '\n' );
            }

            byte[] contentBytes = content.getBytes( "UTF-8" );
            this.write( contentBytes );
        }

        public InputStream toInputStream()
        {
            return new ByteArrayInputStream( buf, 0, count );
        }

    }

}
