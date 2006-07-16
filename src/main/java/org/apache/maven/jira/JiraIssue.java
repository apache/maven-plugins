package org.apache.maven.jira;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * A JIRA issue.
 *
 * @version $Id$
 */
public class JiraIssue
{
    private String key;
    private String link;
    private String summary;
    private String status;
    private String resolution;
    private String assignee;

    public JiraIssue(  )
    {
    }

    public String getKey(  )
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    public String getLink(  )
    {
        return link;
    }

    public void setLink( String link )
    {
        this.link = link;
    }

    public String getSummary(  )
    {
        return summary;
    }

    public void setSummary( String summary )
    {
        this.summary = summary;
    }

    public String getStatus(  )
    {
        return status;
    }

    public void setStatus( String status )
    {
        this.status = status;
    }

    public String getResolution(  )
    {
        return resolution;
    }

    public void setResolution( String resolution )
    {
        this.resolution = resolution;
    }

    public String getAssignee(  )
    {
        return assignee;
    }

    public void setAssignee( String assignee )
    {
        this.assignee = assignee;
    }
}
