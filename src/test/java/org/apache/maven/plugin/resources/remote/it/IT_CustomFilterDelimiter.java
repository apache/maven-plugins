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

public class IT_CustomFilterDelimiter
    extends AbstractIT
{
    
    @SuppressWarnings( "unchecked" )
    public void test()
        throws IOException, URISyntaxException, VerificationException
    {
        File dir = TestUtils.getTestDir( "custom-filter-delim" );
        Verifier verifier = new Verifier( dir.getAbsolutePath() );
        
        verifier.getCliOptions().add( "-X" );
        
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        File output = new File( dir, "target/maven-shared-archive-resources/DEPENDENCIES" );
        String content = FileUtils.fileRead( output );
        
        assertTrue( content.indexOf( "Override: custom-filter-delim" ) > -1 );
    }

}
