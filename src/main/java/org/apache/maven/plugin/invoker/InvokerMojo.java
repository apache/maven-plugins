package org.apache.maven.plugin.invoker;

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

import java.util.Iterator;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.invoker.model.BuildJob;

/**
 * Searches for integration test Maven projects, and executes each, collecting a log in the project directory, and
 * outputting the results to the command line.
 *
 * @goal run
 * @phase integration-test
 * @requiresDependencyResolution test
 * @since 1.0
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 * @version $Id$
 */
public class InvokerMojo
    extends AbstractInvokerMojo
{

    /**
     * A flag controlling whether failures of the sub builds should fail the main build, too. If set to
     * <code>true</code>, the main build will proceed even if one or more sub builds failed.
     * 
     * @parameter expression="${maven.test.failure.ignore}" default-value="false"
     * @since 1.3
     */
    private boolean ignoreFailures;

    /**
     * Processes the results of invoking the build jobs.
     * 
     * @param buildJobs The set of build jobs which were invoked
     * @param failures The failed build jobs.
     * @throws MojoExecutionException If the mojo had an execution exception as a result of invoking the build jobs.
     * @throws MojoFailureException If the mojo had failed as a result of invoking the build jobs.
     * @since 1.4
     */
    protected void processResults( BuildJob[] buildJobs, List failures )
        throws MojoExecutionException, MojoFailureException
    {

        if ( !suppressSummaries )
        {
            getLog().info( "---------------------------------------" );
            getLog().info( "Execution Summary:" );
            getLog().info( "  Builds Passing: " + ( buildJobs.length - failures.size() ) );
            getLog().info( "  Builds Failing: " + failures.size() );
            getLog().info( "---------------------------------------" );

            if ( !failures.isEmpty() )
            {
                String heading = "The following builds failed:";
                if ( ignoreFailures )
                {
                    getLog().warn( heading );
                }
                else
                {
                    getLog().error( heading );
                }

                for ( final Iterator it = failures.iterator(); it.hasNext(); )
                {
                    BuildJob buildJob = (BuildJob) it.next();
                    String item = "*  " + buildJob.getProject();
                    if ( ignoreFailures )
                    {
                        getLog().warn( item );
                    }
                    else
                    {
                        getLog().error( item );
                    }
                }

                getLog().info( "---------------------------------------" );
            }
        }

        if ( !failures.isEmpty() )
        {
            String message = failures.size() + " build" + ( failures.size() == 1 ? "" : "s" ) + " failed.";

            if ( ignoreFailures )
            {
                getLog().warn( "Ignoring that " + message );
            }
            else
            {
                throw new MojoFailureException( this, message, message );
            }
        }
    }

}
