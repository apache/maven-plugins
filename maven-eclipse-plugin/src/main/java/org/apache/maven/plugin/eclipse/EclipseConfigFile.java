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

/**
 * Represents a generic configuration file, with a name and a content.
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
     * Getter for <code>content</code>.
     * @return Returns the content.
     */
    public String getContent()
    {
        return this.content;
    }

    /**
     * Setter for <code>content</code>.
     * @param content The content to set.
     */
    public void setContent( String content )
    {
        this.content = content;
    }

    /**
     * Getter for <code>name</code>.
     * @return Returns the name.
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Setter for <code>name</code>.
     * @param name The name to set.
     */
    public void setName( String name )
    {
        this.name = name;
    }
}
