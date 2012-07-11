package org.apache.maven.plugins.help;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

/**
 * Base class with some Help Mojo functionalities.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.1
 */
public abstract class AbstractHelpMojo
    extends AbstractMojo
{
    /** The maximum length of a display line. */
    protected static final int LINE_LENGTH = 79;

    /**
     * Optional parameter to write the output of this help in a given file, instead of writing to the console.
     * <br/>
     * <b>Note</b>: Could be a relative path.
     */
    @Parameter( property = "output" )
    protected File output;

    /**
     * Utility method to write a content in a given file.
     *
     * @param output is the wanted output file.
     * @param content contains the content to be written to the file.
     * @throws IOException if any
     * @see #writeFile(File, String)
     */
    protected static void writeFile( File output, StringBuffer content )
        throws IOException
    {
        writeFile( output, content.toString() );
    }

    /**
     * Utility method to write a content in a given file.
     *
     * @param output is the wanted output file.
     * @param content contains the content to be written to the file.
     * @throws IOException if any
     */
    protected static void writeFile( File output, String content )
        throws IOException
    {
        if ( output == null )
        {
            return;
        }

        Writer out = null;
        try
        {
            output.getParentFile().mkdirs();

            out = WriterFactory.newPlatformWriter( output );

            out.write( content );

            out.flush();
        }
        finally
        {
            IOUtil.close( out );
        }
    }
}
