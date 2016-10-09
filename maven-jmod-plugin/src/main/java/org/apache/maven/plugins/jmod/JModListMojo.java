package org.apache.maven.plugins.jmod;

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

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * <pre>
 * jmod list...
 * </pre>
 * 
 * @author Karl Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
@Mojo( name = "list", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE )
public class JModListMojo
    extends AbstractJModMojo
{
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        
        String jModExecutable;
        try
        {
            jModExecutable = getJModExecutable();
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Unable to find jmod command: " + e.getMessage(), e );
        }

//        // Synopsis
//        // Usage: jmod ...
//        Commandline cmd = createJModCommandLine();
//        cmd.setExecutable( jModExecutable );
//
//        executeCommand( cmd, outputDirectory );
        
    }

}
