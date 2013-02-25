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

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Searches for integration test Maven projects, and executes each, collecting a log in the project directory, and
 * outputting the results to the command line.
 *
 * @since 1.0
 *
 * @author <a href="mailto:kenney@apache.org">Kenney Westerhof</a>
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 * @version $Id$
 */
@Mojo( name = "run", defaultPhase = LifecyclePhase.INTEGRATION_TEST,
       requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true )
public class InvokerMojo
    extends AbstractInvokerMojo
{

    /**
     * A flag controlling whether failures of the sub builds should fail the main build, too. If set to
     * <code>true</code>, the main build will proceed even if one or more sub builds failed.
     *
     * @since 1.3
     */
    @Parameter( property = "maven.test.failure.ignore", defaultValue = "false" )
    private boolean ignoreFailures;

    /**
     * Set this to <code>true</code> to cause a failure if there are no projects to invoke.
     *
     * @since 1.9
     */
    @Parameter( property = "invoker.failIfNoProjects" )
    private Boolean failIfNoProjects;
    
    void processResults( InvokerSession invokerSession )
        throws MojoFailureException
    {
        if ( !suppressSummaries )
        {
            invokerSession.logSummary( getLog(), ignoreFailures );
        }

        invokerSession.handleFailures( getLog(), ignoreFailures );
    }
    
    @Override
    protected void doFailIfNoProjects()
        throws MojoFailureException
    {
        if( Boolean.TRUE.equals( failIfNoProjects ) )
        {
            throw new MojoFailureException( "No projects to invoke!" );
        }
    }

}
