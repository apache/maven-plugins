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

import java.util.regex.PatternSyntaxException;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class JavadocUtilTest
    extends TestCase
{
    /**
     * Method to test the javadoc parsing.
     *
     * @throws Exception if any
     */
    public void testParseJavadocVersion()
        throws Exception
    {
        String version = null;
        try
        {
            JavadocUtil.parseJavadocVersion( version );
            assertTrue( "Not catch null", false );
        }
        catch ( IllegalArgumentException e )
        {
            assertTrue( true );
        }

        // Sun JDK 1.4
        version = "java full version \"1.4.2_12-b03\"";
        assertEquals( JavadocUtil.parseJavadocVersion( version ), 1.42f, 0 );

        // Sun JDK 1.5
        version = "java full version \"1.5.0_07-164\"";
        assertEquals( JavadocUtil.parseJavadocVersion( version ), 1.5f, 0 );

        // IBM JDK 1.4
        version = "java full version \"J2RE 1.4.2 IBM Windows 32 build cn1420-20040626\"";
        assertEquals( JavadocUtil.parseJavadocVersion( version ), 1.42f, 0 );

        // IBM JDK 1.5
        version = "javadoc version complète de \"J2RE 1.5.0 IBM Windows 32 build pwi32pdev-20070426a\"";
        assertEquals( JavadocUtil.parseJavadocVersion( version ), 1.5f, 0 );

        // IBM JDK 1.5
        version = "J2RE 1.5.0 IBM Windows 32 build pwi32devifx-20070323 (ifix 117674: SR4 + 116644 + 114941 + 116110 + 114881)";
        assertEquals( JavadocUtil.parseJavadocVersion( version ), 1.5f, 0 );

        // FreeBSD
        version = "java full version \"diablo-1.5.0-b01\"";
        assertEquals( JavadocUtil.parseJavadocVersion( version ), 1.5f, 0 );

        // BEA
        version = "java full version \"1.5.0_11-b03\"";
        assertEquals( JavadocUtil.parseJavadocVersion( version ), 1.5f, 0 );

        // Other tests
        version = "java full version \"1.5.0_07-164\"" + System.getProperty( "line.separator" );
        assertEquals( JavadocUtil.parseJavadocVersion( version ), 1.5f, 0 );

        version = "java full version \"1.99.123-b01\"";
        assertEquals( JavadocUtil.parseJavadocVersion( version ), 1.99123f, 0 );

        version = "java full version \"1.5.0.07-164\"";
        assertEquals( JavadocUtil.parseJavadocVersion( version ), 1.5f, 0 );

        version = "java full version \"1.4\"";
        assertEquals( JavadocUtil.parseJavadocVersion( version ), 1.4f, 0 );

        version = "java full version \"1.A.B_07-164\"";
        try
        {
            JavadocUtil.parseJavadocVersion( version );
            assertTrue( "Not catch wrong pattern", false );
        }
        catch ( PatternSyntaxException e )
        {
            assertTrue( true );
        }
    }
}
