package org.apache.maven.plugin.invoker;

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

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Provides utility methods for POM processing.
 *
 * @author Benjamin Bentmann
 */
class PomUtils
{

    /**
     * Loads the (raw) model from the specified POM file.
     *
     * @param pomFile The path to the POM file to load, must not be <code>null</code>.
     * @return The raw model, never <code>null</code>.
     * @throws MojoExecutionException If the POM file could not be loaded.
     */
    public static Model loadPom( File pomFile )
        throws MojoExecutionException
    {
        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( pomFile );
            return new MavenXpp3Reader().read( reader, false );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Failed to parse POM: " + pomFile, e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to read POM: " + pomFile, e );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

}
