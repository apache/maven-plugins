package org.apache.maven.plugin.jira;

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

import junit.framework.TestCase;

/**
 * Test class for {@link UrlBuilder}
 *
 * @author ton.swieb@finalist.com
 * @since 2.8
 */
public class UrlBuilderTest
    extends TestCase
{
    public void testUrlWithoutParameters()
    {
        String expected = "http://www.jira.com/context";
        String actual = new UrlBuilder( "http://www.jira.com", "context" )
                .build();
        assertEquals( expected, actual );
    }

    public void testUrlWithSingleParameter()
    {
        String expected = "http://www.jira.com/context?key1=value1";
        String actual = new UrlBuilder( "http://www.jira.com", "context" )
                .addParameter( "key1", "value1" )
                .build();
        assertEquals( expected, actual );
    }

    public void testUrlWithMultipleParameters()
    {
        String expected = "http://www.jira.com/context?key1=value1&key2=value2";
        String actual = new UrlBuilder( "http://www.jira.com", "context" )
                .addParameter( "key1", "value1" )
                .addParameter( "key2", "value2" )
                .build();
        assertEquals( expected, actual );
    }

    public void testUrlWithIntParameter()
    {
        String expected = "http://www.jira.com/context?key1=1";
        String actual = new UrlBuilder( "http://www.jira.com", "context" )
                .addParameter( "key1", 1 )
                .build();
        assertEquals( expected, actual );
    }
}
