package org.apache.maven.plugins.invoker;

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

import static org.junit.Assert.assertEquals;
import java.util.Properties;

import org.apache.maven.plugins.invoker.InvokerProperties;
import org.apache.maven.plugins.invoker.Selector;
import org.junit.Test;

public class SelectorTest
{
    @Test
    public void testGlobalMatch()
    {
        Selector selector = new Selector( "3.2.5", "1.7" );

        Properties props = new Properties();
        props.setProperty( "invoker.maven.version", "3.0+" );
        InvokerProperties invokerProperties = new InvokerProperties( props );
        assertEquals( 0, selector.getSelection( invokerProperties ) );
    }

    @Test
    public void testSelectorMatch()
    {
        Selector selector = new Selector( "3.2.5", "1.7" );

        Properties props = new Properties();
        props.setProperty( "selector.1.maven.version", "3.0+" );
        InvokerProperties invokerProperties = new InvokerProperties( props );
        assertEquals( 0, selector.getSelection( invokerProperties ) );

        props.setProperty( "selector.1.maven.version", "3.3.1+" );
        assertEquals( Selector.SELECTOR_MULTI, selector.getSelection( invokerProperties ) );
    }

    @Test
    public void testSelectorWithGlobalMatch()
    {
        Selector selector = new Selector( "3.2.5", "1.7" );

        Properties props = new Properties();
        // invoker.maven.version is used by all selectors
        props.setProperty( "invoker.maven.version", "3.0+" );
        props.setProperty( "selector.1.java.version", "1.4+" );
        props.setProperty( "selector.2.os.family", "myos" );
        InvokerProperties invokerProperties = new InvokerProperties( props );
        assertEquals( 0, selector.getSelection( invokerProperties ) );

        props.setProperty( "invoker.maven.version", "3.3.1+" );
        assertEquals( Selector.SELECTOR_MULTI, selector.getSelection( invokerProperties ) );

        props.setProperty( "invoker.maven.version", "3.0+" );
        props.setProperty( "selector.1.maven.version", "3.3.1+" );
        assertEquals( Selector.SELECTOR_MULTI, selector.getSelection( invokerProperties ) );

        props.setProperty( "selector.2.os.family", "!myos" );
        assertEquals( 0, selector.getSelection( invokerProperties ) );
    }

}
