package org.apache.maven.plugin.resources.remote.it;

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

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.plugin.resources.remote.it.support.TestUtils;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author Benjamin Bentmann
 */
public class IT_GetDependencyProjects
    extends AbstractIT
{

    public void test()
        throws IOException, URISyntaxException, VerificationException
    {
        File dir = TestUtils.getTestDir( "get-dependency-projects" );

        Verifier verifier;

        verifier = TestUtils.newVerifier( dir );
        verifier.executeGoal( "deploy" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = TestUtils.newVerifier( new File( dir, "project" ) );

        verifier.deleteArtifacts( "org.apache.maven.plugin.rresource.it.gdp" );

        try
        {
            verifier.executeGoal( "generate-resources" );
        }
        catch ( VerificationException e)
        {
            verifier.resetStreams();

            // We will get an exception from harness in case
            // of execution failure (return code non zero).
            // This is the case if we have missing artifacts
            // as in this test case.
            // This means we can't test the created file which will never
            // contain the appropriate data we wan't to check for. 
            // So the only reliable way is to check the log output 
            // from maven which will print out message according to
            // the missing artifacts.
            File output = new File( verifier.getBasedir(), "log.txt" );
            String content = FileUtils.fileRead( output );
            
            assertTrue(content.contains( "mvn install:install-file -DgroupId=org.apache.maven.plugin.rresource.it.gdp -DartifactId=release -Dversion=1.0 -Dpackaging=jar" ));
            assertTrue (content.contains( "mvn install:install-file -DgroupId=org.apache.maven.plugin.rresource.it.gdp -DartifactId=snapshot -Dversion=1.0-SNAPSHOT -Dpackaging=jar" ));
        }

        
    }

}
