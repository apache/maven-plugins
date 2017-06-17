package org.apache.maven.plugins.antrun;

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

import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;
import org.codehaus.plexus.util.xml.pull.MXSerializer;
import org.codehaus.plexus.util.xml.pull.XmlSerializer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Write the Ant target Plexus configuration to an XML file.
 */
class AntrunXmlPlexusConfigurationWriter
{

    private static final Set<String> EXCLUDED_ATTRIBUTES =
        new HashSet<String>( Arrays.asList( Xpp3DomUtils.CHILDREN_COMBINATION_MODE_ATTRIBUTE,
                                            Xpp3DomUtils.SELF_COMBINATION_MODE_ATTRIBUTE ) );

    /**
     * @param configuration {@link PlexusConfiguration}
     * @param file File to write the Plexus configuration to.
     * @param customTaskPrefix Prefix to use for the custom Ant tasks. Empty if no prefix should be used.
     * @param antTargetName Name of the default Ant target.
     * @throws IOException In case of problems.
     */
    public void write( PlexusConfiguration configuration, File file, String customTaskPrefix, String antTargetName )
        throws IOException
    {
        MXSerializer serializer = new MXSerializer();
        serializer.setProperty( "http://xmlpull.org/v1/doc/properties.html#serializer-line-separator",
                                System.getProperty( "line.separator" ) );
        serializer.setProperty( "http://xmlpull.org/v1/doc/properties.html#serializer-indentation", "  " );
        BufferedOutputStream bos = new BufferedOutputStream( new FileOutputStream( file ) );
        try
        {
            serializer.setOutput( bos, AntRunMojo.UTF_8 );
            serializer.startDocument( AntRunMojo.UTF_8, null );
            if ( !customTaskPrefix.isEmpty() )
            {
                serializer.setPrefix( customTaskPrefix, AntRunMojo.TASK_URI );
            }
            serializer.startTag( null, "project" );
            serializer.attribute( null, "name", "maven-antrun-" );
            serializer.attribute( null, "default", antTargetName );
            write( configuration, serializer );
            serializer.endTag( null, "project" );
            serializer.endDocument();
        }
        finally
        {
            IOUtil.close( bos );
        }
    }

    private void write( PlexusConfiguration c, XmlSerializer serializer )
        throws IOException
    {
        serializer.startTag( null, c.getName() );
        writeAttributes( c, serializer );

        int count = c.getChildCount();
        if ( count == 0 )
        {
            String value = c.getValue();
            if ( value != null )
            {
                serializer.text( value );
            }
        }
        else
        {
            for ( int i = 0; i < count; i++ )
            {
                PlexusConfiguration child = c.getChild( i );
                write( child, serializer );
            }
        }

        serializer.endTag( null, c.getName() );
    }

    private void writeAttributes( PlexusConfiguration c, XmlSerializer serializer )
        throws IOException
    {
        String[] names = c.getAttributeNames();

        for ( String name : names )
        {
            if ( !EXCLUDED_ATTRIBUTES.contains( name ) )
            {
                serializer.attribute( null, name, c.getAttribute( name ) );
            }
        }
    }

}

