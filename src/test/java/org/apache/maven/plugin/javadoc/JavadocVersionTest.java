package org.apache.maven.plugin.javadoc;

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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class JavadocVersionTest
{
    /**
     * Parsing is lazy, only triggered when comparing
     * 
     * @throws Exception
     */
    @Test
    public void testParse() throws Exception
    {
        assertTrue( JavadocVersion.parse( "1.4" ).compareTo( JavadocVersion.parse( "1.4.2" ) ) < 0 );
        assertTrue( JavadocVersion.parse( "1.4" ).compareTo( JavadocVersion.parse( "1.5" ) ) < 0 );
        assertTrue( JavadocVersion.parse( "1.8" ).compareTo( JavadocVersion.parse( "9" ) ) < 0 );

        assertTrue( JavadocVersion.parse( "1.4" ).compareTo( JavadocVersion.parse( "1.4" ) ) == 0 );
        assertTrue( JavadocVersion.parse( "1.4.2" ).compareTo( JavadocVersion.parse( "1.4.2" ) ) == 0 );
        assertTrue( JavadocVersion.parse( "9" ).compareTo( JavadocVersion.parse( "9" ) ) == 0 );

        assertTrue( JavadocVersion.parse( "1.4.2" ).compareTo( JavadocVersion.parse( "1.4" ) ) > 0 );
        assertTrue( JavadocVersion.parse( "1.5" ).compareTo( JavadocVersion.parse( "1.4" ) ) > 0 );
        assertTrue( JavadocVersion.parse( "9" ).compareTo( JavadocVersion.parse( "1.8" ) ) > 0 );
    }
    
    @Test
    public void testApiVersion() {
        Pattern p = Pattern.compile( "(1\\.\\d|\\d\\d*)" );
        Matcher m = p.matcher( "9" );
        assertTrue(m.find());
        assertEquals( "9", m.group( 1 ));

        m = p.matcher( "1.4" );
        assertTrue(m.find());
        assertEquals( "1.4", m.group( 1 ));

        m = p.matcher( "1.4.2" );
        assertTrue(m.find());
        assertEquals( "1.4", m.group( 1 ));
    }
}
