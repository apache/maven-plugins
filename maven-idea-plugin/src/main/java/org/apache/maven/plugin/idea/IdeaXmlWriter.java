package org.apache.maven.plugin.idea;

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

import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Custom implementation of <a href="http://dom4j.org/apidocs/org/dom4j/io/XMLWriter.html">XMLWriter</a> for use with
 * the IDEA plugin.
 */
public class IdeaXmlWriter
    extends XMLWriter
{
    /**
     * Default constructor.
     *
     * @param file output file to be written
     */
    public IdeaXmlWriter( File file )
        throws IOException
    {
        super( new OutputStreamWriter( new FileOutputStream( file ), "UTF-8" ),
               OutputFormat.createPrettyPrint() );
    }

    protected String escapeAttributeEntities( String text )
    {
        String answer = super.escapeAttributeEntities( text );
        answer = answer.replaceAll( "\n", "&#10;" );
        answer = answer.replaceAll( "\n\r", "&#10;" );
        return answer;
    }
}