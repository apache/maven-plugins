package org.apache.maven.plugin.changelog;

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
import org.apache.maven.scm.ChangeFile;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Edwin Punzalan
 */
public class FileActivityComparatorTest
    extends TestCase
{
    private FileActivityComparator comparator;

    protected void setUp()
        throws Exception
    {
        comparator = new FileActivityComparator();
    }

    public void testCompareByNumberOfCommits()
    {
        List list1 = new ArrayList();
        list1.add( new ChangeFile( "anything" ) );

        List list2 = new ArrayList();

        assertTrue( "Test compare by commits, less than", comparator.compare( list1, list2 ) < 0 );

        list1 = new ArrayList();
        list1.add( new ChangeFile( "anything" ) );

        list2 = new ArrayList();
        list2.add( new ChangeFile( "one thing" ) );
        list2.add( new ChangeFile( "something") );

        assertTrue( "Test compare by commits, greater than", comparator.compare( list1, list2 ) > 0 );
    }

    public void testCompareByRevision()
    {
        List list1 = new ArrayList();
        list1.add( new ChangeFile( "changefile-1", "123" ) );
        list1.add( new ChangeFile( "changefile-1", "234" ) );

        List list2 = new ArrayList();
        list2.add( new ChangeFile( "changefile-2", "246" ) );
        list2.add( new ChangeFile( "changefile-2", "468" ) );

        assertTrue( "Test compare by revision, less than", comparator.compare( list1, list2 ) < 0 );

        list1 = new ArrayList();
        list1.add( new ChangeFile( "changefile-1", "246" ) );
        list1.add( new ChangeFile( "changefile-1", "468" ) );

        list2 = new ArrayList();
        list2.add( new ChangeFile( "changefile-2", "123" ) );
        list2.add( new ChangeFile( "changefile-2", "234" ) );

        assertTrue( "Test compare by revision, greater than", comparator.compare( list1, list2 ) > 0 );
    }

    public void testCompareByName()
    {
        List list1 = new ArrayList();
        list1.add( new ChangeFile( "changefile-1", "123" ) );
        list1.add( new ChangeFile( "changefile-1", "468" ) );

        List list2 = new ArrayList();
        list2.add( new ChangeFile( "changefile-2", "246" ) );
        list2.add( new ChangeFile( "changefile-2", "468" ) );

        assertTrue( "Test compare by name, less than", comparator.compare( list1, list2 ) < 0 );

        list1 = new ArrayList();
        list1.add( new ChangeFile( "changefile-1", "246" ) );
        list1.add( new ChangeFile( "changefile-1", "468" ) );

        list2 = new ArrayList();
        list2.add( new ChangeFile( "changefile-2", "123" ) );
        list2.add( new ChangeFile( "changefile-2", "234" ) );

        assertTrue( "Test compare by name, greater than", comparator.compare( list1, list2 ) > 0 );
    }
}
