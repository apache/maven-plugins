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

import junit.framework.TestCase;

/**
 * @author Dennis Lundberg
 */
public class IdeaMojoTestCase
    extends TestCase
{
    private IdeaMojo mojo;

    protected void setUp()
        throws Exception
    {
        mojo = new IdeaMojo();
    }

    public void testToRelative()
    {
        String relativePath;

        relativePath = mojo.toRelative( "C:\\dev\\voca\\gateway",
                                        "C:/dev/voca/gateway/parser/gateway-parser.iml" );
        assertEquals( "Test toRelative child, backslash", "parser/gateway-parser.iml", relativePath );

        relativePath = mojo.toRelative( "C:\\dev\\voca\\gateway\\",
                                        "C:/dev/voca/gateway/parser/gateway-parser.iml" );
        assertEquals( "Test toRelative child, trailing backslash", "parser/gateway-parser.iml", relativePath );

        relativePath = mojo.toRelative( "C:/dev/voca/gateway",
                                        "C:/dev/voca/gateway/parser/gateway-parser.iml" );
        assertEquals( "Test toRelative child, slash", "parser/gateway-parser.iml", relativePath );

        relativePath = mojo.toRelative( "C:/dev/voca/gateway/",
                                        "C:/dev/voca/gateway/parser/gateway-parser.iml" );
        assertEquals( "Test toRelative child, trailing slash", "parser/gateway-parser.iml", relativePath );

        // Tests for MIDEA-102
        relativePath = mojo.toRelative( "C:\\foo\\master",
                                        "C:\\foo\\child" );
        assertEquals( "Test toRelative sibling, no trailing backspace", "../child", relativePath );

        relativePath = mojo.toRelative( "C:\\foo\\master\\",
                                        "C:\\foo\\child" );
        assertEquals( "Test toRelative sibling, first trailing backspace", "../child", relativePath );

        relativePath = mojo.toRelative( "C:\\foo\\master",
                                        "C:\\foo\\child\\" );
        assertEquals( "Test toRelative sibling, second trailing backspace", "../child", relativePath );

        relativePath = mojo.toRelative( "C:\\foo\\master\\",
                                        "C:\\foo\\child\\" );
        assertEquals( "Test toRelative sibling, both trailing backspace", "../child", relativePath );

        // Tests for MIDEA-103
        relativePath = mojo.toRelative( "/myproject/myproject",
                                        "/myproject/myproject-module1/myproject-module1.iml" );
        assertEquals( "Test parent matches prefix of child, no trailing slash", "../myproject-module1/myproject-module1.iml", relativePath );

        relativePath = mojo.toRelative( "/myproject/myproject/",
                                        "/myproject/myproject-module1/myproject-module1.iml" );
        assertEquals( "Test parent matches prefix of child, trailing slash", "../myproject-module1/myproject-module1.iml", relativePath );
    }
}