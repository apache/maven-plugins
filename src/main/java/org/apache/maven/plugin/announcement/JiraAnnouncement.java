package org.apache.maven.plugin.announcement;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.jira.JiraIssue;

/**
 * A JIRA announcement.
 *  
 * @version $Id$
 */
public class JiraAnnouncement
    extends JiraIssue
{
    private String type;

    private String comment;

    private String title;

    private String fixVersion;

    private String reporter;

    private List comments;

    public void addComment( String comment )
    {
        if ( comments == null )
        {
            comments = new ArrayList();
        }
        comments.add( comment );
    }

    public String getFixVersion()
    {
        return fixVersion;
    }

    public void setFixVersion( String fixVersion )
    {
        this.fixVersion = fixVersion;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment( String comment )
    {
        this.comment = comment;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle( String title )
    {
        this.title = title;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public String getType()
    {
        return this.type;
    }

    public void setReporter( String reporter )
    {
        this.reporter = reporter;
    }

    public String getReporter()
    {
        return this.reporter;
    }

    public List getComments()
    {
        return comments;
    }
}
