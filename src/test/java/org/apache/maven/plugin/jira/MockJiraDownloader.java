package org.apache.maven.plugin.jira;

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

import java.io.StringReader;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.issues.Issue;
import org.xml.sax.InputSource;

/**
 * Allow test cases in the jira mojo without actually talking to jira.
 *
 */
public class MockJiraDownloader
    extends AbstractJiraDownloader
{
    @Override
    public void doExecute()
        throws Exception
    {
        // do nothing
    }

    private String jiraXml;

    @Override
    public List<Issue> getIssueList()
        throws MojoExecutionException
    {
        JiraXML jira = new JiraXML( log, jiraDatePattern );
        InputSource inputSource = new InputSource( new StringReader( jiraXml ) );
        jira.parse( inputSource );
        log.info( "The JIRA version is '" + jira.getJiraVersion() + "'" );
        return jira.getIssueList();
    }

    public void setJiraXml( String jiraXml )
    {
        this.jiraXml = jiraXml;
    }

    public String getJiraXml()
    {
        return jiraXml;
    }

}
