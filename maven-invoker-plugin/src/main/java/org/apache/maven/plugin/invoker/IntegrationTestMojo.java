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

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.invoker.model.BuildJob;

/**
 * Searches for integration test Maven projects, and executes each, collecting a log in the project directory, will
 * never fail the build, designed to be used in conjunction with the verify mojo.
 *
 * @goal integration-test
 * @phase integration-test
 * @requiresDependencyResolution test
 * @since 1.4
 *
 * @author <a href="mailto:stephenconnolly at codehaus">Stephen Connolly</a>
 * @version $Id$
 */
public class IntegrationTestMojo
    extends AbstractInvokerMojo
{

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
        // do nothing
    }

}
