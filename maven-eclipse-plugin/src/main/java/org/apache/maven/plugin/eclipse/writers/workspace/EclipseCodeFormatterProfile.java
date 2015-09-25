package org.apache.maven.plugin.eclipse.writers.workspace;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Messages;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * an Eclipse code style file
 * 
 * @author dtran
 */

public class EclipseCodeFormatterProfile
{
    private static final String ELT_PROFILE = "profile";

    /**
     * String presentation of the formatter with EOLs are escaped so that it can be embedded in a property value
     */
    private String content;

    private String profileName;

    public EclipseCodeFormatterProfile init( URL url, String profileName )
        throws MojoExecutionException
    {

        this.profileName = profileName;

        if ( this.profileName == null )
        {
            loadDefaultProfileName( url );
        }

        this.convertFormatterToString( url );

        return this;
    }

    private void loadDefaultProfileName( URL url )
        throws MojoExecutionException
    {
        Reader reader = null;
        try
        {
            reader = new InputStreamReader( url.openStream() );
            Xpp3Dom dom = Xpp3DomBuilder.build( reader );

            Xpp3Dom[] existingProfiles = dom.getChildren( ELT_PROFILE );
            if ( existingProfiles.length != 0 )
            {
                Xpp3Dom firstProfile = existingProfiles[0];
                this.profileName = firstProfile.getAttribute( "name" );
            }
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.cantparseexisting", url.toString() ) );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.cantparseexisting", url.toString() ) );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    private void convertFormatterToString( URL url )
        throws MojoExecutionException
    {
        InputStream is = null;

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        try
        {
            is = url.openStream();

            IOUtil.copy( is, os );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.cantreadfile", url.toString() ), e );
        }
        finally
        {
            IOUtil.close( is );
        }

        content = os.toString();

    }

    public String getContent()
    {
        return this.content;
    }

    public String getProfileName()
    {
        return this.profileName;
    }

}
