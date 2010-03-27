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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Represents a reporting plugin and its executions.
 * @since 3.0-beta-1
 */
public class ReportPlugin
{

    private String groupId = "org.apache.maven.plugins";

    private String artifactId;

    private String version;

    private PlexusConfiguration configuration;

    private List<ReportSet> reportSets;

    public String getGroupId()
    {
        return this.groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return this.artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return this.version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public PlexusConfiguration getConfiguration()
    {
        return this.configuration;
    }

    public void setConfiguration( PlexusConfiguration configuration )
    {
        this.configuration = configuration;
    }

    public List<ReportSet> getReportSets()
    {
        if ( this.reportSets == null )
        {
            this.reportSets = new ArrayList<ReportSet>();
        }

        return this.reportSets;
    }

    public void setReportSets( List<ReportSet> reportSets )
    {
        this.reportSets = reportSets;
    }

}
