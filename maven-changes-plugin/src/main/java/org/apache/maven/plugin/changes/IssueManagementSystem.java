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
package org.apache.maven.plugin.changes;

import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;

public interface IssueManagementSystem
{

    /**
     * @return the map from keys used in poms and other config files to issue types.
     */
    public abstract Map<String, IssueType> getIssueTypeMap();

    /**
     * @return the name of the IMS.
     */
    public abstract String getName();

    public abstract void applyConfiguration( Map<String, String> issueTypes )
        throws MojoExecutionException;

}