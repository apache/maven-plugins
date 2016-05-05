package org.apache.maven.plugins.war.util;

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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Serializes {@link WebappStructure} back and forth.
 *
 * @author Stephane Nicoll
 * @version $Id$
 */
public class WebappStructureSerializer
{

    private static final XStream XSTREAM;

    static
    {
        XSTREAM = new XStream( new DomDriver() );

        // Register aliases
        XSTREAM.alias( "webapp-structure", WebappStructure.class );
        XSTREAM.alias( "path-set", PathSet.class );
        XSTREAM.alias( "dependency", Dependency.class );

    }

    /**
     * Creates a new instance of the serializer.
     */
    public WebappStructureSerializer()
    {
    }

    /**
     * Reads the {@link WebappStructure} from the specified file.
     *
     * @param file the file containing the webapp structure
     * @return the webapp structure
     * @throws IOException if an error occurred while reading the structure
     */
    public WebappStructure fromXml( File file )
        throws IOException
    {
        Reader reader = null;

        try
        {
            reader = ReaderFactory.newXmlReader( file );
            final WebappStructure webappStructure = (WebappStructure) XSTREAM.fromXML( reader );
            reader.close();
            reader = null;
            return webappStructure;
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    /**
     * Saves the {@link WebappStructure} to the specified file.
     *
     * @param webappStructure the structure to save
     * @param targetFile the file to use to save the structure
     * @throws IOException if an error occurred while saving the webapp structure
     */
    public void toXml( WebappStructure webappStructure, File targetFile )
        throws IOException
    {
        // CHECKSTYLE_OFF: LineLength
        Writer writer = null;
        try
        {
            if ( !targetFile.getParentFile().exists() && !targetFile.getParentFile().mkdirs() )
            {
                throw new IOException( "Could not create parent [" + targetFile.getParentFile().getAbsolutePath() + "]" );
            }

            if ( !targetFile.exists() && !targetFile.createNewFile() )
            {
                throw new IOException( "Could not create file [" + targetFile.getAbsolutePath() + "]" );
            }
            writer = WriterFactory.newXmlWriter( targetFile );
            XSTREAM.toXML( webappStructure, writer );
            writer.close();
            writer = null;
        }
        finally
        {
            IOUtil.close( writer );
        }
        // CHECKSTYLE_ON: LineLength
    }
}
