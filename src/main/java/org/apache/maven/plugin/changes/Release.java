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
import java.util.Iterator;

/**
 * A release in a changes.xml file.
 *
 * @version $Id$
 */
public class Release
{
    public static final String ADD_ACTION = "add";

    public static final String FIX_ACTION = "fix";

    public static final String UPDATE_ACTION = "update";

    public static final String REMOVE_ACTION = "remove";


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
        if (action == null)
        {
            action = new ArrayList();
        }
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

    /**
     * Returns the actions for the specified type.
     *
     * @param actionType the action type
     * @return the actions with the specified type
     */
    public List getActions( final String actionType )
    {
        final List result = new ArrayList();
        if ( getAction() == null )
        {
            return new ArrayList();
        }
        final Iterator it = getAction().iterator();
        while ( it.hasNext() )
        {
            Action action = (Action) it.next();
            if ( actionType.equals( action.getType() ) )
            {
                result.add( action );
            }
        }

        return result;
    }
}
