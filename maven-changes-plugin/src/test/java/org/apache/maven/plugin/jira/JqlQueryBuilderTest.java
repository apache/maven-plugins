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
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Test class for {@link JqlQueryBuilder}
 *
 * @author ton.swieb@finalist.com
 * @since 2.8
 */
public class JqlQueryBuilderTest
    extends TestCase
{
    private static final String ENCODING = "UTF-8";

    public void testEmptyQuery()
    {
        String actual = new JqlQueryBuilder( new SystemStreamLog() ).build();
        String expected = "";
        assertEquals( expected, actual );
    }

    public void testSingleParameterValue()
        throws UnsupportedEncodingException
    {
        String expected = URLEncoder.encode( "project = DOXIA", ENCODING );

        String actual = createBuilder()
                .project( "DOXIA" )
                .build();
        assertEquals( expected, actual );
    }

    public void testFixVersion()
        throws UnsupportedEncodingException
    {
        String expected = URLEncoder.encode( "fixVersion = \"1.0\"",
                                             ENCODING );

        String actual = createBuilder()
                .fixVersion( "1.0" )
                .build();
        assertEquals( expected, actual );
    }

    public void testFixVersionCombinedWithOtherParameters()
        throws UnsupportedEncodingException
    {
        String expected = URLEncoder.encode( "project = DOXIA AND fixVersion = \"1.0\"",
                                             ENCODING );

        String actual = createBuilder()
                .project( "DOXIA" )
                .fixVersion( "1.0" )
                .build();
        assertEquals( expected, actual );
    }

    public void testSingleParameterSingleValue()
        throws UnsupportedEncodingException
    {
        String expected = URLEncoder.encode( "priority in (Blocker)",
                                             ENCODING );

        String actual = createBuilder()
                .priorityIds( "Blocker" )
                .build();
        assertEquals( expected, actual );

        actual = createBuilder()
                .priorityIds( "  Blocker   " )
                .build();
        assertEquals( expected, actual );
    }

    public void testSingleParameterMultipleValues()
        throws UnsupportedEncodingException
    {
        String expected = URLEncoder.encode( "priority in (Blocker, Critical, Major)",
                                             ENCODING );

        String actual = createBuilder()
                .priorityIds( "Blocker,Critical,Major" )
                .build();
        assertEquals( expected, actual );

        actual = createBuilder()
                .priorityIds( "  Blocker  ,  Critical,  Major" )
                .build();
        assertEquals( expected, actual );
    }

    public void testMultipleParameterCombinedWithAND()
        throws UnsupportedEncodingException
    {
        String expected = URLEncoder.encode( "priority in (Blocker) AND status in (Resolved)",
                                             ENCODING );

        String actual = createBuilder()
                .priorityIds( "Blocker" )
                .statusIds( "Resolved" )
                .build();
        assertEquals( expected, actual );
    }

    public void testValueWithSpacesAreQuoted()
        throws UnsupportedEncodingException
    {
        String expected = URLEncoder.encode( "status in (\"In Progress\")",
                                             ENCODING );

        String actual = createBuilder()
                .statusIds( "In Progress" )
                .build();
        assertEquals( expected, actual );
    }

    public void testSortSingleRowAscending()
        throws UnsupportedEncodingException
    {
        String expected = URLEncoder.encode( "project = DOXIA ORDER BY key ASC",
                                             ENCODING );

        String actual = createBuilder()
                .project( "DOXIA" )
                .sortColumnNames( "key" )
                .build();
        assertEquals( expected, actual );

        actual = createBuilder()
                .project( "DOXIA" )
                .sortColumnNames( "key ASC" )
                .build();
        assertEquals( expected, actual );

        actual = createBuilder()
                .project( "DOXIA" )
                .sortColumnNames( "     key    ASC    " )
                .build();
        assertEquals( expected, actual );
    }

    public void testSortSingleDescending()
        throws UnsupportedEncodingException
    {
        String expected = URLEncoder.encode( "project = DOXIA ORDER BY key DESC",
                                             ENCODING );

        String actual = createBuilder()
                .project( "DOXIA" )
                .sortColumnNames( "key DESC" )
                .build();
        assertEquals( expected, actual );

        actual = createBuilder()
                .project( "DOXIA" )
                .sortColumnNames( "     key    DESC    " )
                .build();
        assertEquals( expected, actual );
    }

    public void testSortMultipleColumns()
        throws UnsupportedEncodingException
    {
        String expected = URLEncoder.encode( "project = DOXIA ORDER BY key ASC, assignee DESC, reporter ASC",
                                             ENCODING );

        String actual = createBuilder()
                .project( "DOXIA" )
                .sortColumnNames( "key ASC,assignee DESC, reporter ASC" )
                .build();
        assertEquals( expected, actual );
    }

    public void testOrderByIsLastElement()
        throws UnsupportedEncodingException
    {
        String expected = URLEncoder.encode( "project = DOXIA ORDER BY key ASC, assignee DESC, reporter ASC",
                                             ENCODING );

        String actual = createBuilder()
                .sortColumnNames( "key ASC,assignee DESC, reporter ASC" )
                .project( "DOXIA" )
                .build();
        assertEquals( expected, actual );
    }

    private JiraQueryBuilder createBuilder()
    {
        return new JqlQueryBuilder( new SystemStreamLog() );
    }
}
