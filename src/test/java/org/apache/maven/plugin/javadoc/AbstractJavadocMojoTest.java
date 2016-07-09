package org.apache.maven.plugin.javadoc;

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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import junit.framework.TestCase;

public class AbstractJavadocMojoTest
    extends TestCase
{
    AbstractJavadocMojo mojo;
    
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        mojo = new AbstractJavadocMojo()
        {
            @Override
            public void doExecute()
                throws MojoExecutionException, MojoFailureException
            {
            }
        };
    }
    
    public void testMJAVADOC432_DetectLinksMessages()
    {
        Log log = mock( Log.class );
        when( log.isErrorEnabled() ).thenReturn( true );
        mojo.setLog( log );
        mojo.outputDirectory = new File( "target/test-classes" );

        // first continues after warning, next exits with warning
        assertFalse( mojo.isValidJavadocLink( new File( "pom.xml" ).getPath(), true ) );
        assertFalse( mojo.isValidJavadocLink( "file://%%", true ) );
        assertFalse( mojo.isValidJavadocLink( new File( "pom.xml" ).toURI().toString(), true ) );
        verify( log, times( 4 ) ).warn( anyString() );
        verify( log, never() ).error( anyString() );

        // first continues after error, next exits with error
        assertFalse( mojo.isValidJavadocLink( new File( "pom.xml" ).getPath(), false ) );
        assertFalse( mojo.isValidJavadocLink( "file://%%", false ) );
        assertFalse( mojo.isValidJavadocLink( new File( "pom.xml" ).toURI().toString(), false ) );
        verify( log, times( 4 ) ).error( anyString() );
        verify( log, times( 4 ) ).warn( anyString() ); // no extra warnings
    }
}
