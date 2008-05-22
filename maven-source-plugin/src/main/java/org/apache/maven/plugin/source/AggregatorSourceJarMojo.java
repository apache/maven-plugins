package org.apache.maven.plugin.source;

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

/**
 * Aggregate sources for all modules in an aggregator project.
 *
 * @version $Id$
 * @goal aggregate
 * @phase package
 * @aggregator
 * @execute phase="generate-sources"
 * @since 2.0.3
 */
public class AggregatorSourceJarMojo
    extends SourceJarMojo
{
    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        if ( "pom".equals( project.getPackaging() ) )
        {
            packageSources( reactorProjects );
        }
    }
}
