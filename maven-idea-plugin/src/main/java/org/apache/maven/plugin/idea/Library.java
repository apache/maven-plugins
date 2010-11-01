package org.apache.maven.plugin.idea;

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

/**
 * @author Patrick Lightbody (plightbo at gmail dot com)
 */
public class Library
{
    private String name;

    private String sources;

    private String classes;

    private boolean exclude;

    private String javadocs;

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

    public String getJavadocs()
    {
        return javadocs;
    }

    public void setJavadocs( String javadocs )
    {
        this.javadocs = javadocs;
    }

    public String[] getSplitJavadocs()
    {
        if ( javadocs == null )
        {
            return new String[0];
        }

        return javadocs.split( "[,\\s]+" );
    }

    public String toString()
    {
        return name + " : " + getSplitSources() + "; " + getSplitClasses() + "; " + exclude;
    }
}
