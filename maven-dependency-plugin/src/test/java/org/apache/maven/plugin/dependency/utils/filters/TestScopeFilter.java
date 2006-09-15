/*
 *  Copyright 2005-2006 Brian Fox (brianefox@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 */
package org.apache.maven.plugin.dependency.utils.filters;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.dependency.utils.ArtifactStubFactory;
import org.apache.maven.plugin.dependency.utils.SilentLog;

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

        ArtifactStubFactory factory = new ArtifactStubFactory (null,false);
        artifacts = factory.getScopedArtifacts();
    }

    public void testScopeFilter()
    {
        ScopeFilter filter = new ScopeFilter( Artifact.SCOPE_COMPILE, null );
        Set result;
        try
        {
            result = filter.filter( artifacts, log );
            assertEquals( 3, result.size() );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( true );
        }
    }

    public void testScopeFilter2()
    {
        ScopeFilter filter = new ScopeFilter( Artifact.SCOPE_RUNTIME, null );
        Set result;
        try
        {
            result = filter.filter( artifacts, log );
            assertEquals( 2, result.size() );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( false );
        }
    }

    public void testScopeFilter3()
    {
        try
        {
            ScopeFilter filter = new ScopeFilter( Artifact.SCOPE_TEST, null );
            Set result = filter.filter( artifacts, log );
            assertEquals( 5, result.size() );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( false );
        }
    }

    public void testScopeFilterNull()
    {
        try
        {
            ScopeFilter filter = new ScopeFilter( null, null );
            Set result = filter.filter( artifacts, log );
            assertEquals( 5, result.size() );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( false );
        }

    }

    public void testScopeFilterEmpty()
    {
        try
        {
            ScopeFilter filter = new ScopeFilter( "", "" );
            Set result = filter.filter( artifacts, log );
            assertEquals( 5, result.size() );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( false );
        }
    }

    public void testExcludeProvided()
    {
        try
        {
            ScopeFilter filter = new ScopeFilter( "", Artifact.SCOPE_PROVIDED );
            Set result = filter.filter( artifacts, log );
            assertEquals( 4, result.size() );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( false );
        }
    }

    public void testExcludeCompile()
    {
        try
        {
            ScopeFilter filter = new ScopeFilter( "", Artifact.SCOPE_COMPILE );
            Set result = filter.filter( artifacts, log );
            assertEquals( 2, result.size() );
        }
        catch ( MojoExecutionException e )
        {
            assertTrue( false );
        }
    }

    public void testExcludeTest()
    {
        try
        {
            ScopeFilter filter = new ScopeFilter( "", Artifact.SCOPE_TEST );
            Set result = filter.filter( artifacts, log );
            assertTrue(false);
        }
        catch ( MojoExecutionException e )
        {
            //I expect an exception here.
            assertTrue( true );
        }
    }
}
