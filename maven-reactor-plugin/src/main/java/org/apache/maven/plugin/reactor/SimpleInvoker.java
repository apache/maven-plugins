package org.apache.maven.plugin.reactor;

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

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.invoker.CommandLineConfigurationException;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenCommandLineBuilder;

/** Simplified wrapper for Maven invoker
 * 
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 *
 */
public class SimpleInvoker
{
    /** Runs a "mvn --reactor" build with the specified includes
     * 
     * @param reactorIncludes the list of include patterns for --reactor
     * @param goalList the list of goals (you can also pass other flags in here; they're just command-line args)
     * @param invoker the Maven Invoker (let Maven provide to you as a component)
     * @param printOnly if true, don't actually run anything, just log a message
     * @param log logger
     * @throws InvokerExecutionException if build fails for any reason
     */
    void runReactor( String[] reactorIncludes, List goalList, Invoker invoker, boolean printOnly, Log log )
        throws InvokerExecutionException
    {
        InvocationRequest request = new DefaultInvocationRequest();
        request.activateReactor( reactorIncludes, null/* excludes */);
        request.setGoals( goalList );
        request.setRecursive( false );
        try
        {
            log.info( "Executing: " + new MavenCommandLineBuilder().build( request ) );
        }
        catch ( CommandLineConfigurationException e )
        {
            throw new InvokerExecutionException( "Failed to display command line", e );
        }

        if ( !printOnly )
        {
            try
            {
                InvocationResult result = invoker.execute( request );
                if ( result.getExecutionException() != null )
                    throw result.getExecutionException();
                if ( result.getExitCode() != 0 )
                    throw new InvokerExecutionException( "Exit code was " + result.getExitCode() );
            }
            catch ( Exception e )
            {
                throw new InvokerExecutionException( "Maven build failed: " + e.getLocalizedMessage(), e );
            }
        }
    }
    
    class InvokerExecutionException extends MojoExecutionException {

        private static final long serialVersionUID = 1L;

        public InvokerExecutionException( Object source, String shortMessage, String longMessage )
        {
            super( source, shortMessage, longMessage );
        }

        public InvokerExecutionException( String message, Exception cause )
        {
            super( message, cause );
        }

        public InvokerExecutionException( String message, Throwable cause )
        {
            super( message, cause );
        }

        public InvokerExecutionException( String message )
        {
            super( message );
        }
        
    }
}
