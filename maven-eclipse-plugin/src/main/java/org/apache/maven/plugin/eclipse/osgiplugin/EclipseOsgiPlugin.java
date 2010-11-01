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
package org.apache.maven.plugin.eclipse.osgiplugin;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Abstraction of Eclipse plugins
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public interface EclipseOsgiPlugin
{
    /**
     * Get the plugin Manifest
     * 
     * @return the Manifest or <code>null</code> if it has no manifest
     * @throws IOException
     */
    Manifest getManifest()
        throws IOException;

    /**
     * Whether the manifest is present or not
     * 
     * @return true if the manifest exists, false otherwise
     * @throws IOException
     */
    boolean hasManifest()
        throws IOException;

    /**
     * Get a jar with the plugin contents
     * 
     * @return the jar
     * @throws IOException
     */
    JarFile getJar()
        throws IOException;

    /**
     * Get a jar with the plugin contents
     * 
     * @return the jar file
     * @throws IOException
     */
    File getJarFile()
        throws IOException;

    /**
     * Loads the plugin.properties file from a the plugin, usually needed in order to resolve the artifact name.
     * 
     * @return loaded Properties (or an empty properties if no plugin.properties is found)
     * @throws IOException for exceptions while reading the file
     */
    Properties getPluginProperties()
        throws IOException;

    /**
     * Properties to add to the pom
     * 
     * @return pom properties
     */
    Properties getPomProperties();

    public String getManifestAttribute( String key )
        throws IOException;
}