package org.apache.maven.plugin.issues;

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

import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.changes.IssueType;

/**
 * An interface for issue management systems.
 * <p/>
 * The plan is to enhance this interface to allow communication with different issue management systems in a consistent
 * way.
 */
public interface IssueManagementSystem
{

    /**
     * Get a mapping of issue types used in this issue management system to the ones used in a changes.xml file.
     *
     * @return The map from keys used in poms and other config files to issue types.
     */
    public abstract Map<String, IssueType> getIssueTypeMap();

    /**
     * Get the name of the issue management system.
     *
     * @return The name of the IMS.
     */
    public abstract String getName();

    /**
     * Configure this issue management system.
     *
     * @param issueTypes The mapping of issue types used in this issue management system to the ones used in a changes.xml file
     * @throws MojoExecutionException If the configuration fails
     */
    public abstract void applyConfiguration( Map<String, String> issueTypes )
        throws MojoExecutionException;

}