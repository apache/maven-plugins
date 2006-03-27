package org.apache.maven.plugin.checkstyle;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
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
    private Log log;

    private String resourcePathBase;

    private File outputDirectory;

    public ReportResource( String resourcePathBase, File outputDirectory )
    {
        this.resourcePathBase = resourcePathBase;
        this.outputDirectory = outputDirectory;
    }

    public void copy( String resourceName )
        throws IOException
    {
        URL url = Thread.currentThread().getContextClassLoader().getResource( resourcePathBase + "/" + resourceName );
        FileUtils.copyURLToFile( url, new File( outputDirectory, resourceName ) );
    }

    public Log getLog()
    {
        if ( this.log == null )
        {
            this.log = new SystemStreamLog();
        }
        return log;
    }

    public void setLog( Log log )
    {
        this.log = log;
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
