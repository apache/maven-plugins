package org.apache.maven.plugins.release.exec;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.PrintStream;

/**
 * Consumer that both funnels to System.out/err, and stores in an internal buffer.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class TeeConsumer
    implements StreamConsumer
{
    private PrintStream stream;

    /**
     * @noinspection StringBufferField
     */
    private StringBuffer content = new StringBuffer();

    private static final String LS = System.getProperty( "line.separator" );

    private String indent;

    public TeeConsumer( PrintStream stream )
    {
        this( stream, "    " );
    }

    public TeeConsumer( PrintStream stream, String indent )
    {
        this.stream = stream;

        this.indent = indent;
    }

    public void consumeLine( String line )
    {
        stream.println( indent + line );

        content.append( line );
        content.append( LS );
    }

    public String getContent()
    {
        return content.toString();
    }
}
