package org.apache.maven.plugin.gpg;

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

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.commons.lang.SystemUtils;

import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.CommandLineException;

/**
 * Sign project attached artifacts with GnuPG.
 *
 * @version $Rev: 470156 $ $Date$
 * @goal gpg-sign-attached
 * @phase verify
 */
public class GpgSignAttachedMojo
    extends AbstractMojo
{
    /**
     * The passphrase to use when signing.
     *
     * @parameter expression="${passphrase}"
     * @required
     */
    private String passphrase;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Maven ProjectHelper
     *
     * @component
     * @required
     * @readonly
     */
    private MavenProjectHelper projectHelper = null;

    public void execute()
        throws MojoExecutionException
    {
        List artifacts = new ArrayList();

        artifacts.add( project.getArtifact() );

        artifacts.addAll( project.getAttachedArtifacts() );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            File file = artifact.getFile();

            File signature = sign( file );
            
            projectHelper.attachArtifact( project, artifact.getType() + ".asc", "asc", signature );
        }
    }

    private File sign( File file )
        throws MojoExecutionException
    {
        File signature = new File( file + ".asc" );

        if ( signature.exists() )
        {
            signature.delete();
        }

        Commandline cmd = new Commandline();
        
        cmd.setExecutable( "gpg" + ( SystemUtils.IS_OS_WINDOWS ? ".exe" : "" ) );

        cmd.createArgument().setValue( "--passphrase-fd" );
        
        cmd.createArgument().setValue( "0" );

        cmd.createArgument().setValue( "--armor" );

        cmd.createArgument().setValue( "--detach-sign" );

        cmd.createArgument().setFile( file );

        // Prepare the input stream which will be used to pass the passphrase to the executable
        InputStream in = new ByteArrayInputStream( passphrase.getBytes() );

        try
        {
            int exitCode = CommandLineUtils.executeCommandLine( cmd, in, new DefaultConsumer(), new DefaultConsumer() );

            if ( exitCode != 0 )
            {
                throw new MojoExecutionException( "Exit code: " + exitCode );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( "Unable to execute java command", e );
        }

        return signature;
    }
}
