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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.SelectorUtils;

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

    private static final String DEFAULT_EXCLUDES[] = new String[] { "**/*.md5", "**/*.sha1", "**/*.asc" };

    /**
     * The directory from which gpg will load keyrings. If not specified, gpg will use the value configured for its
     * installation, e.g. <code>~/.gnupg</code> or <code>%APPDATA%/gnupg</code>.
     * 
     * @parameter expression="${gpg.homedir}"
     * @since 1.0
     */
    private File homedir;

    /**
     * The passphrase to use when signing.
     * 
     * @parameter expression="${gpg.passphrase}"
     */
    private String passphrase;

    /**
     * The "name" of the key to sign with. Passed to gpg as --local-user.
     * 
     * @parameter expression="${gpg.keyname}"
     */
    private String keyname;

    /**
     * Passes --use-agent or --no-use-agent to gpg. If using an agent, the password is optional as the agent will
     * provide it.
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
     * A list of files to exclude from being signed. Can contain ant-style wildcards and double wildcards. The default
     * excludes are <code>**&#47;*.md5   **&#47;*.sha1    **&#47;*.asc</code>.
     * 
     * @parameter
     * @since 1.0-alpha-4
     */
    private String[] excludes;

    /**
     * The directory where to store signature files.
     * 
     * @parameter default-value="${project.build.directory}/gpg"
     * @since 1.0-alpha-4
     */
    private File outputDirectory;

    /**
     * The maven project.
     * 
     * @parameter default-value="${project}"
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
     * @parameter default-value="${settings}"
     * @required
     * @readonly
     */
    protected Settings settings;

    private GpgSigner signer = new GpgSigner();

    public void execute()
        throws MojoExecutionException
    {

        String pass = passphrase;
        if ( skip )
        {
            // We're skipping the signing stuff
            return;
        }

        if ( excludes == null || excludes.length == 0 )
        {
            excludes = DEFAULT_EXCLUDES;
        }
        String newExcludes[] = new String[excludes.length];
        for ( int i = 0; i < excludes.length; i++ )
        {
            String pattern;
            pattern = excludes[i].trim().replace( '/', File.separatorChar ).replace( '\\', File.separatorChar );
            if ( pattern.endsWith( File.separator ) )
            {
                pattern += "**";
            }
            newExcludes[i] = pattern;
        }
        excludes = newExcludes;

        if ( !useAgent && null == pass )
        {
            if ( !settings.isInteractiveMode() )
            {
                throw new MojoExecutionException( "Cannot obtain passphrase in batch mode" );
            }
            try
            {
                pass = signer.getPassphrase( project );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Exception reading password", e );
            }
        }

        // ----------------------------------------------------------------------------
        // What we need to generateSignatureForArtifact here
        // ----------------------------------------------------------------------------

        signer.setInteractive( settings.isInteractiveMode() );
        signer.setKeyName( keyname );
        signer.setUseAgent( useAgent );
        signer.setOutputDirectory( outputDirectory );
        signer.setBuildDirectory( new File( project.getBuild().getDirectory() ) );
        signer.setBaseDirectory( project.getBasedir() );
        signer.setHomeDirectory( homedir );

        List signingBundles = new ArrayList();

        if ( !"pom".equals( project.getPackaging() ) )
        {
            // ----------------------------------------------------------------------------
            // Project artifact
            // ----------------------------------------------------------------------------

            File projectArtifact = project.getArtifact().getFile();

            File projectArtifactSignature = signer.generateSignatureForArtifact( projectArtifact, pass );

            if ( projectArtifactSignature != null )
            {
                signingBundles.add( new SigningBundle( project.getArtifact().getType(), projectArtifactSignature ) );
            }
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

        File pomSignature = signer.generateSignatureForArtifact( pomToSign, pass );

        if ( pomSignature != null )
        {
            signingBundles.add( new SigningBundle( "pom", pomSignature ) );
        }

        // ----------------------------------------------------------------------------
        // Attached artifacts
        // ----------------------------------------------------------------------------

        for ( Iterator i = project.getAttachedArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            File file = artifact.getFile();

            File signature = signer.generateSignatureForArtifact( file, pass );

            if ( signature != null )
            {
                signingBundles.add( new SigningBundle( artifact.getType(), artifact.getClassifier(), signature ) );
            }
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

            if ( bundle.getClassifier() != null && !"".equals( bundle.getClassifier() ) )
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

    /**
     * Tests whether or not a name matches against at least one exclude pattern.
     * 
     * @param name The name to match. Must not be <code>null</code>.
     * @return <code>true</code> when the name matches against at least one exclude pattern, or <code>false</code>
     *         otherwise.
     */
    protected boolean isExcluded( String name )
    {
        for ( int i = 0; i < excludes.length; i++ )
        {
            if ( SelectorUtils.matchPath( excludes[i], name ) )
            {
                return true;
            }
        }
        return false;
    }

}
