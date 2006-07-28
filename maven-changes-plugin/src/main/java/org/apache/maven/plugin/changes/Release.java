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

import java.util.ArrayList;
import java.util.List;

/**
 * A release in a changes.xml file.
 *
 * @version $Id$
 */
public class Release
{
    private List action;

    private String dateRelease;

    private String description;

    private String version;

    public Release()
    {
    }

    public void setAction( List action )
    {
        this.action = action;
    }

    public List getAction()
    {
        return action;
    }

    public void addAction( Action act )
    {
        if ( action == null )
        {
            action = new ArrayList();
        }
        action.add( act );
    }

    public void setDateRelease( String dateRelease )
    {
        this.dateRelease = dateRelease;
    }

    public String getDateRelease()
    {
        return dateRelease;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getVersion()
    {
        return version;
    }
}
