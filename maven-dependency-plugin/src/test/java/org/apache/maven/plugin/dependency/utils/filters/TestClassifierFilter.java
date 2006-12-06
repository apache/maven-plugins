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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.dependency.testUtils.ArtifactStubFactory;
import org.apache.maven.plugin.dependency.utils.SilentLog;
import org.apache.maven.plugin.logging.Log;

/**
 * @author brianf
 * 
 */
public class TestClassifierFilter
    extends TestCase
{
    Set artifacts = new HashSet();

    Log log = new SilentLog();

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArtifactStubFactory factory = new ArtifactStubFactory( null, false );
        artifacts = factory.getClassifiedArtifacts();
    }

    public void testClassifierParsing()
    {
        ClassifierFilter filter = new ClassifierFilter( "one,two", "three,four," );
        List includes = filter.getIncludes();
        List excludes = filter.getExcludes();

        assertEquals( 2, includes.size() );
        assertEquals( 2, excludes.size() );
        assertEquals( "one", includes.get( 0 ).toString() );
        assertEquals( "two", includes.get( 1 ).toString() );
        assertEquals( "three", excludes.get( 0 ).toString() );
        assertEquals( "four", excludes.get( 1 ).toString() );
    }

    public void testFiltering()
    {
        ClassifierFilter filter = new ClassifierFilter( "one,two", "one,three," );
        Set result = filter.filter( artifacts, log );
        assertEquals( 2, result.size() );

        Iterator iter = result.iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertTrue( artifact.getClassifier().equals( "one" ) || artifact.getClassifier().equals( "two" ) );
        }
    }

    public void testFiltering2()
    {
        ClassifierFilter filter = new ClassifierFilter( null, "one,three," );
        Set result = filter.filter( artifacts, log );
        assertEquals( 2, result.size() );

        Iterator iter = result.iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertTrue( artifact.getClassifier().equals( "two" ) || artifact.getClassifier().equals( "four" ) );
        }
    }

    public void testFiltering3()
    {
        ClassifierFilter filter = new ClassifierFilter( null, null );
        Set result = filter.filter( artifacts, log );
        assertEquals( 4, result.size() );
    }
}
