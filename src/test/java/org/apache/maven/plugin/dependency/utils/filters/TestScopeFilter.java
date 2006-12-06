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
import java.util.Set;

import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.testUtils.ArtifactStubFactory;
import org.apache.maven.plugin.dependency.utils.SilentLog;
import org.apache.maven.plugin.logging.Log;

/**
 * @author brianf
 * 
 */
public class TestScopeFilter
    extends TestCase
{
    Set artifacts = new HashSet();

    Log log = new SilentLog();

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArtifactStubFactory factory = new ArtifactStubFactory( null, false );
        artifacts = factory.getScopedArtifacts();
    }

    public void testScopeCompile()
        throws MojoExecutionException
    {
        ScopeFilter filter = new ScopeFilter( Artifact.SCOPE_COMPILE, null );
        Set result;

        result = filter.filter( artifacts, log );
        assertEquals( 3, result.size() );

    }

    public void testScopeRuntime()
        throws MojoExecutionException
    {
        ScopeFilter filter = new ScopeFilter( Artifact.SCOPE_RUNTIME, null );
        Set result;
        result = filter.filter( artifacts, log );
        assertEquals( 2, result.size() );

    }

    public void testScopeTest()
        throws MojoExecutionException
    {
        ScopeFilter filter = new ScopeFilter( Artifact.SCOPE_TEST, null );
        Set result = filter.filter( artifacts, log );
        assertEquals( 5, result.size() );
    }

    public void testScopeProvided()
        throws MojoExecutionException
    {

        ScopeFilter filter = new ScopeFilter( Artifact.SCOPE_PROVIDED, null );
        Set result = filter.filter( artifacts, log );
        Iterator iter = result.iterator();
        assertTrue( result.size() > 0 );
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertEquals( Artifact.SCOPE_PROVIDED, artifact.getScope() );
        }
    }

    public void testScopeSystem()
        throws MojoExecutionException
    {

        ScopeFilter filter = new ScopeFilter( Artifact.SCOPE_SYSTEM, null );
        Set result = filter.filter( artifacts, log );
        Iterator iter = result.iterator();
        assertTrue( result.size() > 0 );
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertEquals( Artifact.SCOPE_SYSTEM, artifact.getScope() );
        }
    }

    public void testScopeFilterNull()
        throws MojoExecutionException
    {
        ScopeFilter filter = new ScopeFilter( null, null );
        Set result = filter.filter( artifacts, log );
        assertEquals( 5, result.size() );
    }

    public void testScopeFilterEmpty()
        throws MojoExecutionException
    {
        ScopeFilter filter = new ScopeFilter( "", "" );
        Set result = filter.filter( artifacts, log );
        assertEquals( 5, result.size() );
    }

    public void testExcludeProvided()
        throws MojoExecutionException
    {
        ScopeFilter filter = new ScopeFilter( "", Artifact.SCOPE_PROVIDED );
        Set result = filter.filter( artifacts, log );
        Iterator iter = result.iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertFalse( Artifact.SCOPE_PROVIDED.equalsIgnoreCase( artifact.getScope() ) );
        }
    }

    public void testExcludeSystem()
        throws MojoExecutionException
    {
        ScopeFilter filter = new ScopeFilter( "", Artifact.SCOPE_SYSTEM );
        Set result = filter.filter( artifacts, log );
        Iterator iter = result.iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertFalse( Artifact.SCOPE_SYSTEM.equalsIgnoreCase( artifact.getScope() ) );
        }
    }

    public void testExcludeCompile()
        throws MojoExecutionException
    {
        ScopeFilter filter = new ScopeFilter( "", Artifact.SCOPE_COMPILE );
        Set result = filter.filter( artifacts, log );
        assertEquals( 2, result.size() );
    }

    public void testExcludeTest()
    {
        try
        {
            ScopeFilter filter = new ScopeFilter( "", Artifact.SCOPE_TEST );
            filter.filter( artifacts, log );
            fail( "Expected an Exception" );
        }
        catch ( MojoExecutionException e )
        {

        }
    }

    public void testBadScope()
    {
        ScopeFilter filter = new ScopeFilter( "cOmpile", "" );
        try
        {
            filter.filter( artifacts, log );
            fail( "Expected an Exception" );
        }
        catch ( MojoExecutionException e )
        {

        }
        try
        {
            filter = new ScopeFilter( "", "coMpile" );
            filter.filter( artifacts, log );
            fail( "Expected an Exception" );
        }
        catch ( MojoExecutionException e )
        {

        }
    }

    public void testSettersGetters()
    {
        ScopeFilter filter = new ScopeFilter( "include", "exclude" );
        assertEquals( "include", filter.getIncludeScope() );
        assertEquals( "exclude", filter.getExcludeScope() );

        filter.setExcludeScope( "a" );
        filter.setIncludeScope( "b" );
        assertEquals( "b", filter.getIncludeScope() );
        assertEquals( "a", filter.getExcludeScope() );
    }
}
