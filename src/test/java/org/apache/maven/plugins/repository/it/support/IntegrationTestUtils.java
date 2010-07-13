package org.apache.maven.plugins.repository.it.support;

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

import static org.codehaus.plexus.util.IOUtil.close;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class IntegrationTestUtils
{

    private static String cliPluginPrefix;

    private static String pluginVersion;

    private static String pluginGroupId;

    private static String pluginArtifactId;
    
    private static boolean installed = false;
    
    public static void bootstrap()
        throws VerificationException, IOException, URISyntaxException
    {
        if ( !installed )
        {
            File bootstrapDir = getTestDir( "bootstrap" );
            
            Verifier verifier = new Verifier( bootstrapDir.getAbsolutePath() );
            
            verifier.executeGoal( "install" );
            
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
            
            installed = true;
        }
    }

    public static File getTestDir( final String name )
        throws IOException, URISyntaxException
    {
        ClassLoader cloader = Thread.currentThread().getContextClassLoader();
        URL resource = cloader.getResource( name );

        if ( resource == null )
        {
            throw new IOException( "Cannot find test directory: " + name );
        }

        return new File( new URI( resource.toExternalForm() ).normalize().getPath() );
    }

    public static File getBaseDir()
    {
        File result = new File( System.getProperty( "basedir", "." ) );
        try
        {
            return result.getCanonicalFile();
        }
        catch ( IOException e )
        {
            return result.getAbsoluteFile();
        }
    }

    public static String getPluginVersion()
        throws IOException
    {
        if ( pluginVersion == null )
        {
            initPluginInfo();
        }

        return pluginVersion;
    }

    public static String getPluginGroupId()
        throws IOException
    {
        if ( pluginGroupId == null )
        {
            initPluginInfo();
        }

        return pluginGroupId;
    }

    public static String getPluginArtifactId()
        throws IOException
    {
        if ( pluginArtifactId == null )
        {
            initPluginInfo();
        }

        return pluginArtifactId;
    }

    public static String getCliPluginPrefix()
        throws IOException
    {
        if ( cliPluginPrefix == null )
        {
            initPluginInfo();
        }

        return cliPluginPrefix;
    }

    private static void initPluginInfo()
        throws IOException
    {
        URL resource = Thread.currentThread().getContextClassLoader().getResource( "META-INF/maven/plugin.xml" );

        InputStream stream = null;
        try
        {
            stream = resource.openStream();
            Xpp3Dom pluginDom;
            try
            {
                pluginDom = Xpp3DomBuilder.build( new InputStreamReader( stream ) );
            }
            catch ( XmlPullParserException e )
            {
                IOException err = new IOException(
                                                   "Failed to parse plugin descriptor for groupId:artifactId:version prefix. Reason: "
                                                       + e.getMessage() );
                err.initCause( e );

                throw err;
            }

            pluginArtifactId = pluginDom.getChild( "artifactId" ).getValue();
            pluginGroupId = pluginDom.getChild( "groupId" ).getValue();
            pluginVersion = pluginDom.getChild( "version" ).getValue();

            cliPluginPrefix = pluginGroupId + ":" + pluginArtifactId + ":" + pluginVersion + ":";
        }
        finally
        {
            close( stream );
        }
    }
}
