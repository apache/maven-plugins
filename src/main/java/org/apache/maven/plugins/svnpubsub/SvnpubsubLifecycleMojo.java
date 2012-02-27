package org.apache.maven.plugins.svnpubsub;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * 
 * @goal svnpubsub
 * @execute lifecycle="svnpubsub" phase="svnpubsub-publish"
 * @aggregate
 */
public class SvnpubsubLifecycleMojo
    extends AbstractSvnpubsubMojo
{

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // nothing to do. This only purpose here is to set loose the lifecycle.
    }

}
