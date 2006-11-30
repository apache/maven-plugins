package org.apache.maven.plugin.checkstyle;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Generic Report Resource management.
 *
 * @author <a href="mailto:joakim@erdfelt.net">Joakim Erdfelt</a>
 */
public class ReportResource
{
    private String resourcePathBase;

    private File outputDirectory;

    public ReportResource( String resourcePathBase, File outputDirectory )
    {
        this.resourcePathBase = resourcePathBase;
        this.outputDirectory = outputDirectory;
    }

    public void copy( String resourceName ) throws IOException
    {
        File resource = new File( outputDirectory, resourceName );
        if ( ( resource != null ) && ( !resource.exists() ) )
        {
            URL url =
                Thread.currentThread().getContextClassLoader().getResource( resourcePathBase + "/" + resourceName );
            FileUtils.copyURLToFile( url, resource );
        }
    }

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public String getResourcePathBase()
    {
        return resourcePathBase;
    }

    public void setResourcePathBase( String resourcePathBase )
    {
        this.resourcePathBase = resourcePathBase;
    }
}
