package org.apache.maven.plugin.war.util;

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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Serializes {@link WebappStructure} back and forth.
 *
 * @author Stephane Nicoll
 */
public class WebappStructureSerializer
{

    private final XStream xStream;

    /**
     * Creates a new instance of the serializer.
     */
    public WebappStructureSerializer()
    {
        this.xStream = new XStream();

        // Register aliases
        xStream.alias( "webapp-structure", WebappStructure.class );
        xStream.alias( "path-set", PathSet.class );
    }


    /**
     * Reads the {@link WebappStructure} from the specified file.
     *
     * @param file the file containing the webapp structure
     * @return the webapp structure
     * @throws IOException if an error occured while reading the structure
     */
    public WebappStructure fromXml( File file )
        throws IOException
    {
        FileReader reader = null;

        try
        {
            reader = new FileReader( file );
            return (WebappStructure) xStream.fromXML( reader );
        }
        finally
        {
            if ( reader != null )
            {
                reader.close();
            }
        }
    }

    /**
     * Saves the {@link WebappStructure} to the specified file.
     *
     * @param webappStructure the structure to save
     * @param targetFile      the file to use to save the structure
     * @throws IOException if an error occured while saving the webapp structure
     */
    public void toXml( WebappStructure webappStructure, File targetFile )
        throws IOException
    {
        FileWriter writer = null;
        try
        {
            if ( !targetFile.getParentFile().exists() && !targetFile.getParentFile().mkdirs() )
            {
                throw new IOException(
                    "Could not create parent[" + targetFile.getParentFile().getAbsolutePath() + "]" );
            }

            if ( !targetFile.exists() && !targetFile.createNewFile() )
            {
                throw new IOException( "Could not create file[" + targetFile.getAbsolutePath() + "]" );
            }
            writer = new FileWriter( targetFile );
            xStream.toXML( webappStructure, writer );
        }
        finally
        {
            if ( writer != null )
            {
                writer.close();
            }
        }
    }
}
