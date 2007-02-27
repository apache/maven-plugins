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
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.FileUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Sign project artifact, the POM, and attached artifacts with GnuPG for deployment.
 *
 * @author Jason van Zyl
 * @author Jason Dillon
 * @author Daniel Kulp
 * @goal sign
 * @phase verify
 */
public class GpgSignAttachedMojo
    extends AbstractMojo
{
    public static final String SIGNATURE_EXTENSION = ".asc";

    /**
     * The passphrase to use when signing.
     *
     * @parameter expression="${gpg.passphrase}"
     */
    private String passphrase;

    /**
     * The "name" of the key to sign with.  Passed to gpg as --local-user.
     * 
     * @parameter expression="${gpg.keyname}"
     */
    private String keyname;


    /**
     * Passes --use-agent or --no-use-agent to gpg.   If using an agent,
     * the password is optional as the agent will provide it.
     * 
     * @parameter expression="${gpg.useagent}" default-value="false"
     * @required
     */
    private boolean useAgent;
    
    /**
     * Skip doing the gpg signing
     * 
     * @parameter expression="${gpg.skip}" default-value="false"
     * @required
     */
    private boolean skip;


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
     * Maven ArtifactHandlerManager
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactHandlerManager artifactHandlerManager;
    
    /**
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    protected Settings settings;


    public void execute()
        throws MojoExecutionException
    {

        if ( skip ) 
        {
            //We're skipping the signing stuff
            return;
        }
        
        if ( !useAgent && null == passphrase )
        {
            if ( !settings.isInteractiveMode() )
            {
                throw new MojoExecutionException("Cannot obtain passphrase in batch mode");
            }
            try 
            {
                getPassphrase();
            }
            catch (IOException e) 
            {
                throw new MojoExecutionException("Exception reading password", e);
            }
        }
        
        
        // ----------------------------------------------------------------------------
        // What we need to generateSignatureForArtifact here
        // ----------------------------------------------------------------------------

        List signingBundles = new ArrayList();

        if ( !"pom".equals( project.getPackaging() ) )
        {
            // ----------------------------------------------------------------------------
            // Project artifact
            // ----------------------------------------------------------------------------

            File projectArtifact = project.getArtifact().getFile();

            File projectArtifactSignature = generateSignatureForArtifact( projectArtifact );

            signingBundles.add( new SigningBundle( project.getArtifact().getType(), projectArtifactSignature ) );
        }

        // ----------------------------------------------------------------------------
        // POM
        // ----------------------------------------------------------------------------

        File pomToSign = new File( project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".pom" );

        try
        {
            FileUtils.copyFile( project.getFile(), pomToSign );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error copying POM for signing.", e );
        }

        File pomSignature = generateSignatureForArtifact( pomToSign );

        signingBundles.add( new SigningBundle( "pom", pomSignature ) );

        // ----------------------------------------------------------------------------
        // Attached artifacts
        // ----------------------------------------------------------------------------

        for ( Iterator i = project.getAttachedArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            File file = artifact.getFile();

            File signature = generateSignatureForArtifact( file );

            signingBundles.add( new SigningBundle( artifact.getType(), artifact.getClassifier(), signature ) );
        }

        // ----------------------------------------------------------------------------
        // Attach all the signatures
        // ----------------------------------------------------------------------------

        ArtifactHandler handler = new DefaultArtifactHandler( "asc" );

        Map map = new HashMap();

        map.put( "asc", handler );

        artifactHandlerManager.addHandlers( map );

        for ( Iterator i = signingBundles.iterator(); i.hasNext(); )
        {
            SigningBundle bundle = (SigningBundle) i.next();

            ArtifactHandler ah = artifactHandlerManager.getArtifactHandler( bundle.getArtifactType() );

            if ( bundle.getClassifier() != null )
            {

                projectHelper.attachArtifact( project, "asc", bundle.getClassifier() + "." + ah.getExtension(),
                                              bundle.getSignature() );
            }
            else
            {
                projectHelper.attachArtifact( project, ah.getExtension() + ".asc", null, bundle.getSignature() );
            }
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

        if ( useAgent )
        {
            cmd.createArgument().setValue( "--use-agent" );
        }
        else
        {
            cmd.createArgument().setValue( "--no-use-agent" );
        }

        InputStream in = null;
        if ( null != passphrase) 
        {
            cmd.createArgument().setValue( "--passphrase-fd" );

            cmd.createArgument().setValue( "0" );

            // Prepare the input stream which will be used to pass the passphrase to the executable
            in = new ByteArrayInputStream( passphrase.getBytes() );
        }

        if ( null != keyname)
        {
            cmd.createArgument().setValue( "--local-user" );

            cmd.createArgument().setValue( keyname );
        }

        cmd.createArgument().setValue( "--armor" );

        cmd.createArgument().setValue( "--detach-sign" );

        cmd.createArgument().setFile( file );


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
            throw new MojoExecutionException( "Unable to execute gpg command", e );
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
    
    protected void getPassphrase() throws IOException
    {
        //TODO: with JDK 1.6, we could call System.console().readPassword("GPG Passphrase: ", null);
        
        System.out.print("GPG Passphrase: ");
        MaskingThread thread = new MaskingThread();
        thread.start();

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        passphrase = in.readLine();

        // stop masking
        thread.stopMasking();
    }
    
    
    // based on ideas from http://java.sun.com/developer/technicalArticles/Security/pwordmask/
    class MaskingThread extends Thread
    {
        private volatile boolean stop;

       /**
        * Begin masking until asked to stop.
        */
        public void run()
        {
            //this needs to be high priority to make sure the characters don't
            //really get to the screen.
            
            int priority = Thread.currentThread().getPriority();
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

            try {
                stop = false;
                while(!stop)
                { 
                    //print a backspace + * to overwrite anything they type
                    System.out.print("\010*");
                    try
                    {
                        //attempt masking at this rate
                        Thread.sleep(1);
                    }
                    catch (InterruptedException iex)
                    {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            finally
            {
                // restore the original priority
                Thread.currentThread().setPriority(priority);
            }
        }

        /**
         * Instruct the thread to stop masking.
         */
        public void stopMasking() {
            this.stop = true;
        }
    }    
}
