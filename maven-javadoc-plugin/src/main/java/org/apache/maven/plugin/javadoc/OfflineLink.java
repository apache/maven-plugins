package org.apache.maven.plugin.javadoc;

/*
 * Copyright 2004-2006 The Apache Software Foundation.
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

import java.io.File;

/**
 * This class represents an offline link parameter specfied in the plugin configuration.
 */
public class OfflineLink
{
    private String url;

    private File location;

    /**
     * Method to get the url of the link
     *
     * @return a String that contains the url
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * Method to set the url of the link
     *
     * @param url the value to be set
     */
    public void setUrl( String url )
    {
        this.url = url;
    }

    /**
     * Method to get the location of the link
     *
     * @return a String that contains the url
     */
    public File getLocation()
    {
        return location;
    }

    /**
     * Method to set the location of the link
     *
     * @param location the value to be set
     */
    public void setLocation( File location )
    {
        this.location = location;
    }
}
