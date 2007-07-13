package org.apache.maven.plugin.clover;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.clover.internal.AbstractCloverMojo;

/**
 * Instrument all sources using Clover and forks a custom lifecycle to execute project's tests on the instrumented
 * code so that a Clover database is created.
 *
 * <p>Note: We're forking a lifecycle because we don't want the Clover instrumentation to affect the main lifecycle
 * build. This will prevent instrumented sources to be put in production by error. Thus running
 * <code>mvn install</code> on a project where this <code>instrument</code> goal has been specified will run the
 * build twice: once for building the project as usual and another time for instrumenting the sources with Clover
 * and generating the Clover database.</p>
 *
 * @goal instrument
 * @execute phase="install" lifecycle="clover"
 *
 * @version $Id$
 */
public class CloverInstrumentMojo extends AbstractCloverMojo
{
    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.clover.internal.AbstractCloverMojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
    }
}
