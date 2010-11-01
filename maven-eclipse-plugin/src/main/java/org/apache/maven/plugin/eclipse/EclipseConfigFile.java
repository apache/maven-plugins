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
package org.apache.maven.plugin.eclipse;

import java.net.URL;

/**
 * Represents a generic configuration file, with a name and a content.
 * 
 * @author Fabrizio Giustina
 * @version $Id$
 */
public class EclipseConfigFile
{
    /**
     * The name of the file.
     */
    private String name;

    /**
     * The file content.
     */
    private String content;

    /**
     * The file location
     * 
     * @since 2.5
     */
    private String location;

    /**
     * The file URL
     * 
     * @since 2.5
     */
    private URL url;

    /**
     * Getter for <code>content</code>.
     * 
     * @return Returns the content.
     */
    public String getContent()
    {
        return content;
    }

    /**
     * Setter for <code>content</code>.
     * 
     * @param content The content to set.
     */
    public void setContent( String content )
    {
        this.content = content;
    }

    /**
     * Getter for <code>name</code>.
     * 
     * @return Returns the name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Setter for <code>name</code>.
     * 
     * @param name The name to set.
     */
    public void setName( String name )
    {
        this.name = name;
    }

    /**
     * Getter for <code>location</code>.
     * 
     * @return Returns the location.
     */
    public String getLocation()
    {
        return location;
    }

    /**
     * Setter for <code>location</code>.
     * 
     * @param location The location to set.
     */
    public void setLocation( String location )
    {
        this.location = location;
    }

    /**
     * Getter for <code>url</code>.
     * 
     * @return Returns the url.
     */
    public URL getURL()
    {
        return url;
    }

    /**
     * Setter for <code>url</code>.
     * 
     * @param location The url to set.
     */
    public void setURL( URL url )
    {
        this.url = url;
    }
}
