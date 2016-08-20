package org.apache.maven.plugins.war.packaging;

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
import org.apache.maven.plugin.MojoFailureException;

/**
 * Defines tasks that should be performed after the packaging.
 *
 * @author Stephane Nicoll
 * @version $Id$
 */
public interface WarPostPackagingTask
{

    /**
     * Executes the post packaging task.
     * 
     * The packaging context hold all information regarding the webapp that has been packaged.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if an error occurred
     * @throws MojoFailureException if a failure occurred
     */
    void performPostPackaging( WarPackagingContext context )
        throws MojoExecutionException, MojoFailureException;

}
