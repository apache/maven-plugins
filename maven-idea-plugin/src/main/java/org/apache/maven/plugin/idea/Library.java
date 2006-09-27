package org.apache.maven.plugin.idea;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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
 * @author Patrick Lightbody (plightbo at gmail dot com)
 */
public class Library
{
    private String name;

    private String sources;

    private String classes;

    private boolean exclude;

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getSources()
    {
        return sources;
    }

    public void setSources( String sources )
    {
        this.sources = sources;
    }

    public String[] getSplitSources()
    {
        if ( sources == null )
        {
            return new String[0];
        }

        return sources.split( "[,\\s]+" );
    }

    public String[] getSplitClasses()
    {
        if ( classes == null )
        {
            return new String[0];
        }

        return classes.split( "[,\\s]+" );
    }

    public boolean isExclude()
    {
        return exclude;
    }

    public void setExclude( boolean exclude )
    {
        this.exclude = exclude;
    }

    public String getClasses()
    {
        return classes;
    }

    public void setClasses( String classes )
    {
        this.classes = classes;
    }

    public String toString()
    {
        return name + " : " + getSplitSources() + "; " + getSplitClasses() + "; " + exclude;
    }
}
