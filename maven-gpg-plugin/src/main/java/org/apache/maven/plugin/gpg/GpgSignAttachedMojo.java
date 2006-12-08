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

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Sign project artifact, the POM, and attached artifacts with GnuPG for deployment.
 *
 * @goal sign
 * @execute phase="verify"
 * @author Jason van Zyl
 * @author Jason Dillon
 */
public class GpgSignAttachedMojo
    extends AbstractMojo
{
    public static final String SIGNATURE_EXTENSION = ".asc";

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
    private MavenProjectHelper projectHelper;

    /**
     * Maven ProjectHelper
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactHandlerManager artifactHandlerManager;


    public void execute()
        throws MojoExecutionException
    {
        // ----------------------------------------------------------------------------
        // What we need to generateSignatureForArtifact here
        // ----------------------------------------------------------------------------

        List signingBundles = new ArrayList();

        if ( !"pom".equals( project.getPackaging() ) )
        {
            // ----------------------------------------------------------------------------
            // Project artifact
            // ----------------------------------------------------------------------------                

            File projectArtifact = getProjectFile( project.getBuild().getDirectory(), project.getBuild().getFinalName() );

            File projectArtifactSignature = generateSignatureForArtifact( projectArtifact );

            signingBundles.add( new SigningBundle( project.getArtifact().getType(), projectArtifactSignature ) );
        }

        // ----------------------------------------------------------------------------
        // POM
        // ----------------------------------------------------------------------------

        File pomSignature = generateSignatureForArtifact( project.getFile() );

        signingBundles.add( new SigningBundle( "pom", pomSignature ) );

        // ----------------------------------------------------------------------------
        // Attached artifacts
        // ----------------------------------------------------------------------------

        for ( Iterator i = project.getAttachedArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            File file = artifact.getFile();

            File signature = generateSignatureForArtifact( file );

            signingBundles.add( new SigningBundle( artifact.getType(), signature ) );
        }

        // ----------------------------------------------------------------------------
        // Attach all the signatures
        // ----------------------------------------------------------------------------

        for ( Iterator i = signingBundles.iterator(); i.hasNext(); )
        {
            SigningBundle bundle = (SigningBundle) i.next();

            // Get the correct artifact handler to we can create the right extension.
            ArtifactHandler ah = artifactHandlerManager.getArtifactHandler( bundle.getArtifactType() );

            // We don't want a classifier, we just want to add the extension ".asc" 
            projectHelper.attachArtifact( project, ah.getExtension() + SIGNATURE_EXTENSION, "", bundle.getSignature() );
        }
    }

    private File generateSignatureForArtifact( File file )
        throws MojoExecutionException
    {
        File signature = new File( file + SIGNATURE_EXTENSION );

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

    //TODO: This must be made to work generally or the packaging plugins must
    // set the project artifact as part of what they do. We should not have to
    // guess or synthesize what project artifact is here. It should have happened
    // already. We'll settle for JAR files right now.
    protected File getProjectFile( String basedir,
                                   String finalName )
    {
        return new File( basedir, finalName + ".jar" );
    }
}
