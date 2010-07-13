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

import static org.apache.maven.plugins.repository.it.support.IntegrationTestUtils.bootstrap;
import static org.apache.maven.plugins.repository.it.support.IntegrationTestUtils.getCliPluginPrefix;
import static org.apache.maven.plugins.repository.it.support.IntegrationTestUtils.getTestDir;
import static org.apache.maven.plugins.repository.testutil.Assertions.assertZipContents;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.plugins.repository.testutil.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BundlePackIT
{
    
    @BeforeClass
    public static void doBootstrap()
        throws VerificationException, IOException, URISyntaxException
    {
        bootstrap();
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void packWithSCMInfoProvided()
        throws IOException, URISyntaxException, VerificationException
    {
        File dir = getTestDir( "bundle-pack" );
        
        String prefix = getCliPluginPrefix();
        String artifactId = "bundle-pack-target";
        String groupId = "org.apache.maven.its.repository";
        String version = "1.0";

        File bundleSource = new File( dir, artifactId + "-" + version + "-bundle.jar" );

        if ( bundleSource.exists() )
        {
            bundleSource.delete();
        }
        
        Verifier verifier = new Verifier( dir.getAbsolutePath() );
        
        verifier.setAutoclean( false );

        List<String> cliOptions = verifier.getCliOptions();
        cliOptions.add( "-DgroupId=" + groupId );
        cliOptions.add( "-DartifactId=" + artifactId );
        cliOptions.add( "-Dversion=" + version );

        verifier.executeGoal( prefix + "bundle-pack" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Set<String> requiredEntries = new HashSet<String>();
        requiredEntries.add( "pom.xml" );
        requiredEntries.add( artifactId + "-" + version + ".jar" );
        
        if ( !verifier.isMavenDebug() )
        {
            bundleSource.deleteOnExit();
        }

        assertZipContents( requiredEntries, Assertions.EMPTY_ENTRY_NAMES, bundleSource );
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void packWithSCMInfoMissing()
        throws IOException, URISyntaxException, VerificationException
    {
        File dir = getTestDir( "bundle-pack" );
        
        String prefix = getCliPluginPrefix();
        String artifactId = "bundle-pack-target-no-scm";
        String groupId = "org.apache.maven.its.repository";
        String version = "1.0";

        File bundleSource = new File( dir, artifactId + "-" + version + "-bundle.jar" );

        if ( bundleSource.exists() )
        {
            bundleSource.delete();
        }
        
        Verifier verifier = new Verifier( dir.getAbsolutePath() );
        
        verifier.setAutoclean( false );

        List<String> cliOptions = verifier.getCliOptions();
        cliOptions.add( "-DgroupId=" + groupId );
        cliOptions.add( "-DartifactId=" + artifactId );
        cliOptions.add( "-Dversion=" + version );
        cliOptions.add( "-DscmUrl=http://foo/" );
        cliOptions.add( "-DscmConnection=scm:svn:http://foo/" );

        verifier.executeGoal( prefix + "bundle-pack" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Set<String> requiredEntries = new HashSet<String>();
        requiredEntries.add( "pom.xml" );
        requiredEntries.add( artifactId + "-" + version + ".jar" );
        
        if ( !verifier.isMavenDebug() )
        {
            bundleSource.deleteOnExit();
        }

        assertZipContents( requiredEntries, Assertions.EMPTY_ENTRY_NAMES, bundleSource );
    }

}
