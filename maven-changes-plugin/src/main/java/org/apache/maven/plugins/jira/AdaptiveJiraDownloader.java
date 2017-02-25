package org.apache.maven.plugins.jira;

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
import org.apache.maven.plugins.issues.Issue;

import java.util.List;

/**
 * Jira downloader that uses REST or RSS, depending. This code is not very attractive. However, JIRA has supported REST
 * for a very long time, and so the fallback is only relevant for people with very old copies of JIRA.
 */
public class AdaptiveJiraDownloader
    extends AbstractJiraDownloader
{
    private AbstractJiraDownloader effectiveDownloader;

    private boolean forceClassic;

    public void doExecute()
        throws Exception
    {
        effectiveDownloader = new RestJiraDownloader();
        copySettings( effectiveDownloader );
        try
        {
            effectiveDownloader.doExecute();
        }
        catch ( RestJiraDownloader.NoRest nre )
        {
            getLog().info( "Falling back to RSS for issue download: " + nre.getMessage() );
            effectiveDownloader = new ClassicJiraDownloader();
            copySettings( effectiveDownloader );
            effectiveDownloader.doExecute();
        }
    }

    private void copySettings( AbstractJiraDownloader target )
    {
        target.setLog( getLog() );
        target.setMavenProject( project );
        target.setOutput( output );
        target.setNbEntries( nbEntriesMax );
        target.setComponent( component );
        target.setFixVersionIds( fixVersionIds );
        target.setStatusIds( statusIds );
        target.setResolutionIds( resolutionIds );
        target.setPriorityIds( priorityIds );
        target.setSortColumnNames( sortColumnNames );
        target.setFilter( filter );
        target.setJiraDatePattern( jiraDatePattern );
        target.setJiraUser( jiraUser );
        target.setJiraPassword( jiraPassword );
        target.setTypeIds( typeIds );
        target.setWebUser( webUser );
        target.setWebPassword( webPassword );
        target.setSettings( settings );
        target.setUseJql( useJql );
        target.setOnlyCurrentVersion( onlyCurrentVersion );
        target.setVersionPrefix( versionPrefix );
        target.setConnectionTimeout( connectionTimeout );
        target.setReceiveTimout( receiveTimout );
    }

    public List<Issue> getIssueList()
        throws MojoExecutionException
    {
        return effectiveDownloader.getIssueList();
    }

    public boolean isForceClassic()
    {
        return forceClassic;
    }

    public void setForceClassic( boolean forceClassic )
    {
        this.forceClassic = forceClassic;
    }
}
