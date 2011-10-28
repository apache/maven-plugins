package org.apache.maven.plugins.jarsigner;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.jarsigner.JarSigner;
import org.apache.maven.shared.jarsigner.JarSignerException;
import org.apache.maven.shared.jarsigner.JarSignerRequest;
import org.apache.maven.shared.jarsigner.JarSignerResult;
import org.apache.maven.shared.jarsigner.JarSignerUtil;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Maven Jarsigner Plugin base class.
 *
 * @author <a href="cs@schulte.it">Christian Schulte</a>
 * @version $Id$
 */
public abstract class AbstractJarsignerMojo
    extends AbstractMojo
{

    /**
     * See <a href="http://java.sun.com/javase/6/docs/technotes/tools/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${jarsigner.verbose}" default-value="false"
     */
    private boolean verbose;

    /**
     * The maximum memory available to the JAR signer, e.g. <code>256M</code>. See <a
     * href="http://java.sun.com/javase/6/docs/technotes/tools/windows/java.html#Xms">-Xmx</a> for more details.
     *
     * @parameter expression="${jarsigner.maxMemory}"
     */
    private String maxMemory;

    /**
     * Archive to process. If set, neither the project artifact nor any attachments or archive sets are processed.
     *
     * @parameter expression="${jarsigner.archive}"
     */
    private File archive;

    /**
     * The base directory to scan for JAR files using Ant-like inclusion/exclusion patterns.
     *
     * @parameter expression="${jarsigner.archiveDirectory}"
     * @since 1.1
     */
    private File archiveDirectory;

    /**
     * The Ant-like inclusion patterns used to select JAR files to process. The patterns must be relative to the
     * directory given by the parameter {@link #archiveDirectory}. By default, the pattern
     * <code>&#42;&#42;/&#42;.?ar</code> is used.
     *
     * @parameter
     * @since 1.1
     */
    private String[] includes = { "**/*.?ar" };

    /**
     * The Ant-like exclusion patterns used to exclude JAR files from processing. The patterns must be relative to the
     * directory given by the parameter {@link #archiveDirectory}.
     *
     * @parameter
     * @since 1.1
     */
    private String[] excludes = { };

    /**
     * List of additional arguments to append to the jarsigner command line.
     *
     * @parameter expression="${jarsigner.arguments}"
     */
    private String[] arguments;

    /**
     * Set to {@code true} to disable the plugin.
     *
     * @parameter expression="${jarsigner.skip}" default-value="false"
     */
    private boolean skip;

    /**
     * Controls processing of the main artifact produced by the project.
     *
     * @parameter expression="${jarsigner.processMainArtifact}" default-value="true"
     * @since 1.1
     */
    private boolean processMainArtifact;

    /**
     * Controls processing of project attachments. If enabled, attached artifacts that are no JAR/ZIP files will be
     * automatically excluded from processing.
     *
     * @parameter expression="${jarsigner.processAttachedArtifacts}" default-value="true"
     * @since 1.1
     */
    private boolean processAttachedArtifacts;

    /**
     * Controls processing of project attachments.
     *
     * @parameter expression="${jarsigner.attachments}"
     * @deprecated As of version 1.1 in favor of the new parameter <code>processAttachedArtifacts</code>.
     */
    private Boolean attachments;

    /**
     * A set of artifact classifiers describing the project attachments that should be processed. This parameter is only
     * relevant if {@link #processAttachedArtifacts} is <code>true</code>. If empty, all attachments are included.
     *
     * @parameter
     * @since 1.2
     */
    private String[] includeClassifiers;

    /**
     * A set of artifact classifiers describing the project attachments that should not be processed. This parameter is
     * only relevant if {@link #processAttachedArtifacts} is <code>true</code>. If empty, no attachments are excluded.
     *
     * @parameter
     * @since 1.2
     */
    private String[] excludeClassifiers;

    /**
     * The Maven project.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Location of the working directory.
     *
     * @parameter default-value="${project.basedir}"
     * @since 1.3
     */
    private File workingDirectory;

    /**
     * @component
     */
    private JarSigner jarSigner;

    public final void execute()
        throws MojoExecutionException
    {
        if ( !this.skip )
        {
            int processed = 0;

            if ( this.archive != null )
            {
                processArchive( this.archive );
                processed++;
            }
            else
            {
                if ( processMainArtifact )
                {
                    processed += processArtifact( this.project.getArtifact() ) ? 1 : 0;
                }

                if ( processAttachedArtifacts && !Boolean.FALSE.equals( attachments ) )
                {
                    Collection includes = new HashSet();
                    if ( includeClassifiers != null )
                    {
                        includes.addAll( Arrays.asList( includeClassifiers ) );
                    }

                    Collection excludes = new HashSet();
                    if ( excludeClassifiers != null )
                    {
                        excludes.addAll( Arrays.asList( excludeClassifiers ) );
                    }

                    for ( Iterator it = this.project.getAttachedArtifacts().iterator(); it.hasNext(); )
                    {
                        final Artifact artifact = (Artifact) it.next();

                        if ( !includes.isEmpty() && !includes.contains( artifact.getClassifier() ) )
                        {
                            continue;
                        }

                        if ( excludes.contains( artifact.getClassifier() ) )
                        {
                            continue;
                        }

                        processed += processArtifact( artifact ) ? 1 : 0;
                    }
                }
                else
                {
                    if ( verbose )
                    {
                        getLog().info( getMessage( "ignoringAttachments" ) );
                    }
                    else
                    {
                        getLog().debug( getMessage( "ignoringAttachments" ) );
                    }
                }

                if ( archiveDirectory != null )
                {
                    String includeList = ( includes != null ) ? StringUtils.join( includes, "," ) : null;
                    String excludeList = ( excludes != null ) ? StringUtils.join( excludes, "," ) : null;

                    List jarFiles;
                    try
                    {
                        jarFiles = FileUtils.getFiles( archiveDirectory, includeList, excludeList );
                    }
                    catch ( IOException e )
                    {
                        throw new MojoExecutionException(
                            "Failed to scan archive directory for JARs: " + e.getMessage(), e );
                    }

                    for ( Iterator it = jarFiles.iterator(); it.hasNext(); )
                    {
                        File jarFile = (File) it.next();

                        processArchive( jarFile );
                        processed++;
                    }
                }
            }

            getLog().info( getMessage( "processed", new Integer( processed ) ) );
        }
        else
        {
            getLog().info( getMessage( "disabled", null ) );
        }
    }

    /**
     * Creates the jar signer request to be executed.
     *
     * @param archive the archive file to treat by jarsigner
     * @return the request
     * @since 1.3
     */
    protected abstract JarSignerRequest createRequest( File archive );

    /**
     * Gets a string representation of a {@code Commandline}.
     * <p>This method creates the string representation by calling {@code commandLine.toString()} by default.</p>
     *
     * @param commandLine The {@code Commandline} to get a string representation of.
     * @return The string representation of {@code commandLine}.
     * @throws NullPointerException if {@code commandLine} is {@code null}.
     */
    protected String getCommandlineInfo( final Commandline commandLine )
    {
        if ( commandLine == null )
        {
            throw new NullPointerException( "commandLine" );
        }

        return commandLine.toString();
    }

    /**
     * Checks whether the specified artifact is a ZIP file.
     *
     * @param artifact The artifact to check, may be <code>null</code>.
     * @return <code>true</code> if the artifact looks like a ZIP file, <code>false</code> otherwise.
     */
    private boolean isZipFile( final Artifact artifact )
    {
        return artifact != null && artifact.getFile() != null && JarSignerUtil.isZipFile( artifact.getFile() );
    }

    /**
     * Processes a given artifact.
     *
     * @param artifact The artifact to process.
     * @return <code>true</code> if the artifact is a JAR and was processed, <code>false</code> otherwise.
     * @throws NullPointerException   if {@code artifact} is {@code null}.
     * @throws MojoExecutionException if processing {@code artifact} fails.
     */
    private boolean processArtifact( final Artifact artifact )
        throws MojoExecutionException
    {
        if ( artifact == null )
        {
            throw new NullPointerException( "artifact" );
        }

        boolean processed = false;

        if ( isZipFile( artifact ) )
        {
            processArchive( artifact.getFile() );

            processed = true;
        }
        else
        {
            if ( this.verbose )
            {
                getLog().info( getMessage( "unsupported", artifact ) );
            }
            else if ( getLog().isDebugEnabled() )
            {
                getLog().debug( getMessage( "unsupported", artifact ) );
            }
        }

        return processed;
    }

    /**
     * Pre-processes a given archive.
     *
     * @param archive The archive to process, must not be <code>null</code>.
     * @throws MojoExecutionException If pre-processing failed.
     */
    protected void preProcessArchive( final File archive )
        throws MojoExecutionException
    {
        // default does nothing
    }

    /**
     * Processes a given archive.
     *
     * @param archive The archive to process.
     * @throws NullPointerException   if {@code archive} is {@code null}.
     * @throws MojoExecutionException if processing {@code archive} fails.
     */
    private void processArchive( final File archive )
        throws MojoExecutionException
    {
        if ( archive == null )
        {
            throw new NullPointerException( "archive" );
        }

        preProcessArchive( archive );

        if ( this.verbose )
        {
            getLog().info( getMessage( "processing", archive ) );
        }
        else if ( getLog().isDebugEnabled() )
        {
            getLog().debug( getMessage( "processing", archive ) );
        }

        JarSignerRequest request = createRequest( archive );
        request.setArchive( archive );
        request.setWorkingDirectory( workingDirectory );
        request.setMaxMemory( maxMemory );
        request.setArguments( arguments );

        try
        {
            JarSignerResult result = jarSigner.execute( request );

            Commandline commandLine = result.getCommandline();

            int resultCode = result.getExitCode();

            if ( resultCode != 0 )
            {
                throw new MojoExecutionException(
                    getMessage( "failure", getCommandlineInfo( commandLine ), new Integer( resultCode ) ) );
            }

        }
        catch ( JarSignerException e )
        {
            throw new MojoExecutionException( getMessage( "commandLineException", e.getMessage() ), e );
        }
    }

    /**
     * Gets a message for a given key from the resource bundle backing the implementation.
     *
     * @param key  The key of the message to return.
     * @param args Arguments to format the message with or {@code null}.
     * @return The message with key {@code key} from the resource bundle backing the implementation.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws java.util.MissingResourceException
     *                              if there is no message available matching {@code key} or accessing
     *                              the resource bundle fails.
     */
    private String getMessage( final String key, final Object[] args )
    {
        if ( key == null )
        {
            throw new NullPointerException( "key" );
        }

        return new MessageFormat( ResourceBundle.getBundle( "jarsigner" ).getString( key ) ).format( args );
    }

    private String getMessage( final String key )
    {
        return getMessage( key, null );
    }

    private String getMessage( final String key, final Object arg )
    {
        return getMessage( key, new Object[]{ arg } );
    }

    private String getMessage( final String key, final Object arg1, final Object arg2 )
    {
        return getMessage( key, new Object[]{ arg1, arg2 } );
    }

}
