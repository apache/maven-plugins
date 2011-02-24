package org.apache.maven.plugin.resources.remote.it.support;

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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class BootstrapInstaller
{
    
    private static boolean installed = false;
    
    public static void install()
        throws IOException, URISyntaxException, VerificationException
    {
        if ( !installed )
        {
            File bootstrapDir = TestUtils.getTestDir( "bootstrap" );
            
            Verifier verifier = new Verifier( bootstrapDir.getAbsolutePath() );
            
            verifier.executeGoal( "deploy" );
            
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
            
            installed = true;
        }
    }

}
