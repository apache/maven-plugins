package org.apache.maven.report.projectinfo.dependencies;

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

/**
 * Wrap DependenciesReport Mojo parameters.
 *
 * @version $Id$
 * @since 2.1
 */
public class DependenciesReportConfiguration
{
    private boolean dependencyDetailsEnabled;

    private boolean dependencyLocationsEnabled;

    /**
     * @param detailsEnabled
     * @param locationEnabled
     */
    public DependenciesReportConfiguration( boolean detailsEnabled, boolean locationEnabled )
    {
        this.dependencyDetailsEnabled = detailsEnabled;
        this.dependencyLocationsEnabled = locationEnabled;
    }

    /**
     * @return value of Mojo dependencyDetailsEnabled parameter.
     */
    public boolean getDependencyDetailsEnabled()
    {
        return dependencyDetailsEnabled;
    }

    /**
     * @return value of Mojo dependencyLocationsEnabled parameter.
     */
    public boolean getDependencyLocationsEnabled()
    {
        return dependencyLocationsEnabled;
    }
}
