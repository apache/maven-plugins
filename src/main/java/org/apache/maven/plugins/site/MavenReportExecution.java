package org.apache.maven.plugins.site;

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
