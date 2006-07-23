package org.apache.maven.plugin.changes;

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
 * An action in a changes.xml file.
 *
 * @version $Id$
 */
public class Action
{
    private String action;

    private String dev;

    private String dueTo;

    private String dueToEmail;

    private String issue;

    private String type;

    public Action()
    {
    }

    public void setAction( String action )
    {
        this.action = action;
    }

    public String getAction()
    {
        return action;
    }

    public void setDev( String dev )
    {
        this.dev = dev;
    }

    public String getDev()
    {
        return dev;
    }

    public void setDueTo( String dueTo )
    {
        this.dueTo = dueTo;
    }

    public String getDueTo()
    {
        return dueTo;
    }

    public void setDueToEmail( String dueToEmail )
    {
        this.dueToEmail = dueToEmail;
    }

    public String getDueToEmail()
    {
        return dueToEmail;
    }

    public void setIssue( String issue )
    {
        this.issue = issue;
    }

    public String getIssue()
    {
        return issue;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    public String getType()
    {
        return type;
    }
}
