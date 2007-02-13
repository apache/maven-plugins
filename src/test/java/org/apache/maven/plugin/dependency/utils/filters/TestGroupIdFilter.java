package org.apache.maven.plugin.dependency.utils.filters;

/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

/**
 * 
 */

import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.dependency.testUtils.AbstractArtifactFeatureFilterTestCase;
import org.apache.maven.plugin.dependency.testUtils.ArtifactStubFactory;
import org.apache.maven.plugin.dependency.utils.SilentLog;

/**
 * @author clove TestCases for GroupIdFilter
 * @see org.apache.maven.plugin.dependency.testUtils.AbstractArtifactFeatureFilterTestCase
 * 
 */
public class TestGroupIdFilter
    extends AbstractArtifactFeatureFilterTestCase
{

    protected void setUp()
        throws Exception
    {
        super.setUp();
        filterClass = GroupIdFilter.class;
        ArtifactStubFactory factory = new ArtifactStubFactory( null, false );
        artifacts = factory.getGroupIdArtifacts();

    }

    public void testParsing()
        throws Exception
    {
        parsing();
    }

    public void testFiltering()
        throws Exception
    {
        Set result = filtering();
        Iterator iter = result.iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertTrue( artifact.getGroupId().equals( "one" ) || artifact.getGroupId().equals( "two" ) );
        }
    }

    public void testFiltering2()
        throws Exception
    {
        Set result = filtering2();
        Iterator iter = result.iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertTrue( artifact.getGroupId().equals( "two" ) || artifact.getGroupId().equals( "four" ) );
        }
    }

    public void testFiltering3()
        throws Exception
    {
        filtering3();
    }

    public void testFiltering4()
        throws Exception
    {
        SilentLog log = new SilentLog();
        // include o* from groupIds one,two should leave one
        Set result = filtering();
        assertTrue( result.size() == 2 );
        GroupIdFilter filter = new GroupIdFilter( "o", null );
        result = filter.filter( result, log );
        Iterator iter = result.iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertTrue( artifact.getGroupId().equals( "one" ) );

        }

        // exclude on* from groupIds one,two should leave two
        result = filtering();
        assertTrue( result.size() == 2 );
        filter = new GroupIdFilter( null, "on" );
        result = filter.filter( result, log );
        iter = result.iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertTrue( artifact.getGroupId().equals( "two" ) );

        }
    }
}
