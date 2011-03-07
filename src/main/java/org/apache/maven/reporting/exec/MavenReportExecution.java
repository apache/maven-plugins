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

import org.apache.maven.model.Plugin;
import org.apache.maven.reporting.MavenReport;

/**
 * <p>
 *   Since maven 3 reporting plugin {@link MavenReport} are not anymore injected by maven core
 *   This class will store all necessary information for {@link MavenReport} execution :
 *   <ul>
 *     <li>a build {@link MavenReport}</li>
 *     <li>The associated {@link ClassLoader} for the Report Mojo execution</li>
 *     <li>The {@link Plugin} associated to the {@link MavenReport}</li>
 *   </ul> 
 * </p>
 * <p>
 *   With this it's possible to execute the {@link MavenReport} generate with settings
 *   the current {@link Thread} classLoader first with {@link #classLoader}
 * </p>
 * <p>
 *   This beans will be build by {@link MavenReportExecutor}.
 * </p>
 * 
 * @author Olivier Lamy
 * @since 3.0-beta-1
 */
public class MavenReportExecution
{
    private MavenReport mavenReport;

    private ClassLoader classLoader;

    private Plugin plugin;

    public MavenReportExecution( Plugin plugin, MavenReport mavenReport, ClassLoader classLoader )
    {
        this.setPlugin( plugin );
        this.mavenReport = mavenReport;
        this.classLoader = classLoader;
    }

    public MavenReport getMavenReport()
    {
        return mavenReport;
    }

    public void setMavenReport( MavenReport mavenReport )
    {
        this.mavenReport = mavenReport;
    }

    public ClassLoader getClassLoader()
    {
        return classLoader;
    }

    public void setClassLoader( ClassLoader classLoader )
    {
        this.classLoader = classLoader;
    }

    public void setPlugin( Plugin plugin )
    {
        this.plugin = plugin;
    }

    public Plugin getPlugin()
    {
        return plugin;
    }
}
