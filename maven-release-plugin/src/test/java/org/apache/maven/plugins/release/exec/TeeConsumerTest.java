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

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Test the consumer that tees output both to a stream and into an internal buffer for later.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class TeeConsumerTest
    extends TestCase
{
    private TeeConsumer consumer;

    private ByteArrayOutputStream out;

    private static final String LS = System.getProperty( "line.separator" );

    protected void setUp()
        throws Exception
    {
        super.setUp();

        out = new ByteArrayOutputStream();
        consumer = new TeeConsumer( new PrintStream( out ), "xxx " );
    }

    public void testConsumeLine()
    {
        consumer.consumeLine( "line" );

        assertEquals( "Check output", "xxx line" + LS, out.toString() );

        assertEquals( "Check content", "line" + LS, consumer.getContent() );

        assertEquals( "Check toString", "line" + LS, consumer.toString() );
    }
}
