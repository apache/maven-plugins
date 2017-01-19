package org.apache.maven.plugins.checkstyle;

import org.apache.maven.plugins.checkstyle.RuleUtil;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;

public class RuleUtilTest
    extends TestCase
{
    private static final String CHECKSTYLE_PACKAGE = "com.puppycrawl.tools.checkstyle.checks";

    public void testGetName()
    {
        assertEquals( "FinalParameters", RuleUtil.getName( CHECKSTYLE_PACKAGE + ".FinalParameters" ) );
        assertEquals( "FinalParameters", RuleUtil.getName( CHECKSTYLE_PACKAGE + ".FinalParametersCheck" ) );
        assertNull( RuleUtil.getName( (String) null ) );
    }

    public void testGetCategory()
    {
        assertEquals( "misc", RuleUtil.getCategory( CHECKSTYLE_PACKAGE + ".FinalParametersCheck" ) );
        assertEquals( "test", RuleUtil.getCategory( CHECKSTYLE_PACKAGE + ".test.FinalParametersCheck" ) );
        assertEquals( "extension", RuleUtil.getCategory( "test.FinalParametersCheck" ) );
        assertNull( RuleUtil.getCategory( (String) null ) );
    }

    public void testMatcher()
    {
        String[] specs = ( "misc, test, extension, Header, " + CHECKSTYLE_PACKAGE + ".test2" ).split( "," );
        String[] eventSrcNames =
            new String[] { CHECKSTYLE_PACKAGE + ".FinalParametersCheck",
                CHECKSTYLE_PACKAGE + ".test.FinalParametersCheck", "test.FinalParametersCheck",
                CHECKSTYLE_PACKAGE + ".whitespace.HeaderCheck", CHECKSTYLE_PACKAGE + ".test2.FinalParametersCheck" };

        RuleUtil.Matcher[] matchers = RuleUtil.parseMatchers( specs );

        for ( int i = 0; i < matchers.length; i++ )
        {
            String spec = specs[i];
            RuleUtil.Matcher matcher = matchers[i];
            for ( int j = 0; j < matchers.length; j++ )
            {
                String eventSrcName = eventSrcNames[j];
                assertEquals( spec + " should" + ( ( i == j ) ? " " : " not " ) + "match " + eventSrcName, i == j,
                              matcher.match( eventSrcName ) );
            }
        }
    }
}
