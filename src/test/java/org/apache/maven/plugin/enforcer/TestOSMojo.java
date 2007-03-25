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

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

/**
 * Exhaustively check the OS mojo.
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class TestOSMojo
    extends TestCase
{
    Set set = new HashSet();

    /*
     * public void testOS() throws MojoExecutionException { Os os = new Os();
     * 
     * OsMojo mojo = new OsMojo(); mojo.displayOSInfo();
     * 
     * Iterator iter = mojo.getValidFamilies().iterator(); String validFamily =
     * null; String invalidFamily = null; while (iter.hasNext() && (validFamily ==
     * null || invalidFamily == null)) { String fam = (String) iter.next(); if
     * (Os.isFamily( fam )) { validFamily = fam; } else { invalidFamily = fam; } }
     * 
     * mojo.getLog().info( "Testing Mojo Using Valid Family: "+validFamily+"
     * Invalid Family: "+invalidFamily);
     * 
     * mojo.setFamily( validFamily ); assertTrue( mojo.isAllowed() );
     * 
     * mojo.setFamily( invalidFamily); assertFalse( mojo.isAllowed() );
     * 
     * mojo.setFamily( "!"+invalidFamily); assertTrue( mojo.isAllowed() );
     * 
     * //test !invalidFamily doesn't throw an exception mojo.execute();
     * 
     * mojo.setFamily( "junk" ); try { mojo.execute(); fail( "Expected
     * MojoExecution Exception because of invalid family type" ); } catch (
     * MojoExecutionException e ) { mojo.getLog().info( "Caught Expected
     * Exception:" + e.getLocalizedMessage() ); }
     * 
     * mojo.setFamily( null );
     * 
     * //test empty config try { mojo.execute(); fail( "Expected MojoExecution
     * Exception because of no params" ); } catch ( MojoExecutionException e ) {
     * mojo.getLog().info( "Caught Expected Exception:" +
     * e.getLocalizedMessage() ); }
     * 
     * mojo.setArch( OsMojo.OS_ARCH ); assertTrue( mojo.isAllowed() );
     * 
     * mojo.setArch( "somecrazyarch" ); assertFalse(mojo.isAllowed());
     * 
     * mojo.setArch( "!somecrazyarch" ); assertTrue(mojo.isAllowed());
     * 
     * mojo.setArch( null );
     * 
     * mojo.setName( OsMojo.OS_NAME ); assertTrue( mojo.isAllowed() );
     * 
     * mojo.setName( "somecrazyname" ); assertFalse(mojo.isAllowed());
     * 
     * mojo.setName( "!somecrazyname" ); assertTrue(mojo.isAllowed());
     * 
     * mojo.setName( null );
     * 
     * mojo.setVersion( OsMojo.OS_VERSION ); assertTrue( mojo.isAllowed() );
     * 
     * mojo.setVersion( "somecrazyversion" ); assertFalse(mojo.isAllowed());
     * 
     * mojo.setVersion( "!somecrazyversion" ); assertTrue(mojo.isAllowed()); }
     * 
     * public void testValidFamily() { OsMojo mojo = new OsMojo();
     * assertTrue(mojo.isValidFamily( null )); assertTrue(mojo.isValidFamily( ""
     * )); assertTrue(mojo.isValidFamily( "windows" ));
     * assertTrue(mojo.isValidFamily( "unix" )); assertTrue(mojo.isValidFamily(
     * "!unix" )); assertFalse(mojo.isValidFamily( "somethingelse" )); }
     */
    public void test()
    {

    }

}
