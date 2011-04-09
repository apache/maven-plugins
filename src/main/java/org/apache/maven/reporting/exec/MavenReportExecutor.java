package org.apache.maven.reporting.exec;

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
import org.apache.maven.reporting.MavenReport;

/**
 * This component builds some {@link MavenReportExecution} from {@link MavenReportExecutorRequest}.
 * If a {@link MavenReport} need to fork a lifecycle, this fork is executed here. 
 * The component asks the core to get some informations on the plugin configuration in order to correctly setup {@link MavenReport}.
 *
 * @author Olivier Lamy
 * @since 3.0-beta-1
 */
public interface MavenReportExecutor
{
    /**
     * @param mavenReportExecutorRequest
     * @return
     * @throws MojoExecutionException
     */
    List<MavenReportExecution> buildMavenReports( MavenReportExecutorRequest mavenReportExecutorRequest )
        throws MojoExecutionException;
}
