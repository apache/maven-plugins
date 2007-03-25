package org.apache.maven.plugin.enforcer;

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

/**
 * Exhaustively check the enforcer mojo.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class TestEnforceMojo
    extends TestCase
{/*
     * public void testFixJDKVersion() { // test that we only take the first 3
     * versions for comparision assertEquals( "1.5.0",
     * EnforceMojo.fixJDKVersion( "1.5.0-11" ) ); assertEquals( "1.5.1",
     * EnforceMojo.fixJDKVersion( "1.5.1" ) ); assertEquals( "1.5.2",
     * EnforceMojo.fixJDKVersion( "1.5.2-b11" ) ); assertEquals( "1.5.3",
     * EnforceMojo.fixJDKVersion( "1.5.3_11" ) ); assertEquals( "1.5.4",
     * EnforceMojo.fixJDKVersion( "1.5.4.5_11" ) ); assertEquals( "1.5.5",
     * EnforceMojo.fixJDKVersion( "1.5.5.6_11.2" ) );
     *  // test for non-standard versions assertEquals( "1.5.0",
     * EnforceMojo.fixJDKVersion( "1-5-0-11" ) ); assertEquals( "1.5.0",
     * EnforceMojo.fixJDKVersion( "1-_5-_0-_11" ) ); assertEquals( "1.5.0",
     * EnforceMojo.fixJDKVersion( "1_5_0_11" ) );
     *  }
     * 
     * public void testContainsVersion() throws
     * InvalidVersionSpecificationException { ArtifactVersion version = new
     * DefaultArtifactVersion( "2.0.5" ); // test ranges assertTrue(
     * EnforceMojo.containsVersion( VersionRange.createFromVersionSpec(
     * "[2.0.5,)" ), version ) ); assertTrue( EnforceMojo.containsVersion(
     * VersionRange.createFromVersionSpec( "[2.0.4,)" ), version ) );
     * assertTrue( EnforceMojo.containsVersion(
     * VersionRange.createFromVersionSpec( "[2.0.4,2.0.5]" ), version ) );
     * assertTrue( EnforceMojo.containsVersion(
     * VersionRange.createFromVersionSpec( "[2.0.4,2.0.6]" ), version ) );
     * assertTrue( EnforceMojo.containsVersion(
     * VersionRange.createFromVersionSpec( "[2.0.4,2.0.6)" ), version ) );
     * assertTrue( EnforceMojo.containsVersion(
     * VersionRange.createFromVersionSpec( "[2.0,)" ), version ) ); assertTrue(
     * EnforceMojo.containsVersion( VersionRange.createFromVersionSpec(
     * "[2.0.0,)" ), version ) ); // not matching versions assertFalse(
     * EnforceMojo.containsVersion( VersionRange.createFromVersionSpec(
     * "[2.0.4,2.0.5)" ), version ) ); assertFalse( EnforceMojo.containsVersion(
     * VersionRange.createFromVersionSpec( "[2.0.6,)" ), version ) );
     * assertFalse( EnforceMojo.containsVersion(
     * VersionRange.createFromVersionSpec( "(2.0.5,)" ), version ) );
     *  // test singular versions -> 2.0.5 == [2.0.5,) or x >= 2.0.5 assertTrue(
     * EnforceMojo.containsVersion( VersionRange.createFromVersionSpec( "2.0" ),
     * version ) ); assertTrue( EnforceMojo.containsVersion(
     * VersionRange.createFromVersionSpec( "2.0.4" ), version ) ); assertTrue(
     * EnforceMojo.containsVersion( VersionRange.createFromVersionSpec( "2.0.5" ),
     * version ) );
     * 
     * assertFalse( EnforceMojo.containsVersion(
     * VersionRange.createFromVersionSpec( "2.0.6" ), version ) ); }
     * 
     * public void testEnforceVersion() throws MojoExecutionException,
     * MojoFailureException { EnforceMojo mojo = new EnforceMojo();
     * ArtifactVersion version = new DefaultArtifactVersion( "2.0.5" );
     * mojo.setFail( false );
     *  // test ranges assertTrue( mojo.enforceVersion( "test", "[2.0.5,)",
     * version ) ); assertTrue( mojo.enforceVersion( "test", "[2.0.4,)", version ) );
     * assertTrue( mojo.enforceVersion( "test", "[2.0.4,2.0.5]", version ) );
     * assertTrue( mojo.enforceVersion( "test", "[2.0.4,2.0.6]", version ) );
     * assertTrue( mojo.enforceVersion( "test", "[2.0.4,2.0.6)", version ) );
     * assertTrue( mojo.enforceVersion( "test", "[2.0,)", version ) );
     * assertTrue( mojo.enforceVersion( "test", "[2.0.0,)", version ) ); // not
     * matching versions assertFalse( mojo.enforceVersion( "test",
     * "[2.0.4,2.0.5)", version ) ); assertFalse( mojo.enforceVersion( "test",
     * "[2.0.6,)", version ) ); assertFalse( mojo.enforceVersion( "test",
     * "(2.0.5,)", version ) );
     *  // test singular versions -> 2.0.5 == [2.0.5,) or x >= 2.0.5 assertTrue(
     * mojo.enforceVersion( "test", "2.0", version ) ); assertTrue(
     * mojo.enforceVersion( "test", "2.0.4", version ) ); assertTrue(
     * mojo.enforceVersion( "test", "2.0.5", version ) );
     * 
     * assertFalse( mojo.enforceVersion( "test", "2.0.6", version ) );
     *  // test fail on error mojo.setFail( true );
     * 
     * try { assertTrue( mojo.enforceVersion( "test", "2.0.5", version ) ); }
     * catch ( Exception e ) { fail( "No Exception expected. Caught:" +
     * e.getLocalizedMessage() );
     *  }
     * 
     * try { mojo.enforceVersion( "test", "2.0.6", version ); fail( "Expected to
     * receive MojoExecutionException" ); } catch ( Exception e ) { if ( e
     * instanceof MojoExecutionException ) { mojo.getLog().info( "Caught
     * Expected Exception: " + e.getLocalizedMessage() ); } else { fail(
     * "Received wrong exception. Expected MojoExecutionException. Received:" +
     * e.toString() ); } }
     *  // make sure to handle the invalid range specification try {
     * mojo.enforceVersion( "test", "[(2.0.6", version ); fail( "Expected to
     * receive MojoExecutionException" ); } catch ( Exception e ) { if ( e
     * instanceof MojoExecutionException ) { mojo.getLog().info( "Caught
     * Expected Exception: " + e.getLocalizedMessage() ); } else { fail(
     * "Received wrong exception. Expected MojoExecutionException. Received:" +
     * e.toString() ); } } }
     * 
     * public void testEnforceMojo() { EnforceMojo mojo = new EnforceMojo();
     * mojo.setFail( true ); mojo.setRti( new MockRuntimeInformation() );
     * 
     * try { mojo.execute(); fail( "Expected to receive exception because no
     * required version ranges are set." ); } catch ( MojoExecutionException e ) {
     * mojo.getLog().info( "Caught Expected Exception: " +
     * e.getLocalizedMessage() ); }
     * 
     * try { mojo.setMavenVersion( "2.0.6" ); mojo.execute(); fail( "Expected to
     * receive exception because 2.0.5 does not contain 2.0.6" ); } catch (
     * MojoExecutionException e ) { mojo.getLog().info( "Caught Expected
     * Exception: " + e.getLocalizedMessage() ); }
     * 
     * try { mojo.setMavenVersion( null ); mojo.setJdkVersion( "(" +
     * EnforceMojo.fixJDKVersion( SystemUtils.JAVA_VERSION_TRIMMED ) + ",]" );
     * mojo.execute(); fail( "Expected to receive exception because I have set
     * the lowerbounds noninclusive." ); } catch ( MojoExecutionException e ) {
     * mojo.getLog().info( "Caught Expected Exception: " +
     * e.getLocalizedMessage() ); }
     * 
     * try { mojo.setMavenVersion( null ); mojo.setJdkVersion(
     * SystemUtils.JAVA_VERSION_TRIMMED ); mojo.execute(); } catch (
     * MojoExecutionException e ) { fail( "Did not expect an exception.
     * Received:" + e.getLocalizedMessage() ); }
     * 
     * try { mojo.setFail( false ); mojo.setMavenVersion( null );
     * mojo.setJdkVersion( "(" + EnforceMojo.fixJDKVersion(
     * SystemUtils.JAVA_VERSION_TRIMMED ) + ",]" ); mojo.execute(); } catch (
     * MojoExecutionException e ) { fail( "Did not expect an exception.
     * Received:" + e.getLocalizedMessage() ); }
     *  }
     */
    public void test()
    {

    }
}
