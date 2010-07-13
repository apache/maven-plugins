package org.apache.maven.plugins.repository.it;

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

import static org.apache.maven.plugins.repository.it.support.IntegrationTestUtils.getTestDir;
import static org.apache.maven.plugins.repository.testutil.Assertions.assertZipContents;
import static junit.framework.Assert.fail;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.plugins.repository.it.support.IntegrationTestUtils;
import org.apache.maven.plugins.repository.testutil.Assertions;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BundleCreateIT
{
    
    @SuppressWarnings( "unchecked" )
    @Test
    public void createWithSCMInfoProvided()
        throws IOException, URISyntaxException, VerificationException
    {
        File dir = getTestDir( "bundle-create" );
        
        Verifier verifier = new Verifier( dir.getAbsolutePath() );
        
        verifier.getCliOptions().add( "--settings ../settings.xml" );
        
        String prefix = IntegrationTestUtils.getCliPluginPrefix();
        
        verifier.executeGoal( prefix + "bundle-create" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        File bundleSource = new File( dir, "target/test-1.0-bundle.jar" );
        
        Set<String> requiredEntries = new HashSet<String>();
        requiredEntries.add( "pom.xml" );
        requiredEntries.add( "test-1.0.jar" );
        requiredEntries.add( "test-1.0-sources.jar" );
        
        assertZipContents( requiredEntries, Assertions.EMPTY_ENTRY_NAMES, bundleSource );
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void createWithSCMInfoMissing()
        throws IOException, URISyntaxException, VerificationException
    {
        File dir = getTestDir( "bundle-create-no-scm" );
        
        Verifier verifier = new Verifier( dir.getAbsolutePath() );
        
        List<String> cliOptions = verifier.getCliOptions();
        cliOptions.add( "--settings ../settings.xml" );
        
        String prefix = IntegrationTestUtils.getCliPluginPrefix();
        
        try
        {
            verifier.executeGoal( prefix + "bundle-create" );
            verifier.verifyErrorFreeLog();
            
            fail( "No SCM Section provided, build should fail." );
        }
        catch( VerificationException e )
        {
            // expected, since POM doesn't supply a SCM section.
        }
        finally
        {
            verifier.resetStreams();
        }
    }

}
