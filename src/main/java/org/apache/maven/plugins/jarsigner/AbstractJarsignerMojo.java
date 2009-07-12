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

import java.io.File;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

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
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/jarsigner.html#Options">options</a>.
     *
     * @parameter expression="${jarsigner.verbose}" default-value="false"
     */
    private boolean verbose;

    /**
     * Archive to sign. If set, neither the project artifact nor any attachments are processed.
     *
     * @parameter expression="${jarsigner.archive}"
     * @optional
     */
    private File archive;

    /**
     * List of arguments to append to the jarsigner command line.
     *
     * @parameter expression="${jarsigner.arguments}"
     * @optional
     */
    private String[] arguments;

    /**
     * Set to {@code true} to disable the plugin.
     *
     * @parameter expression="${jarsigner.skip}" default-value="false"
     */
    private boolean skip;

    /**
     * Controls processing of project attachments.
     *
     * @parameter expression="${jarsigner.attachments}" default-value="true"
     */
    private boolean attachments;

    /**
     * The Maven project.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    public final void execute()
        throws MojoExecutionException
    {
        if ( !this.skip )
        {
            if ( this.archive != null )
            {
                this.processArchive( this.archive );
            }
            else
            {
                this.processArtifact( this.project.getArtifact() );

                for ( Iterator it = this.project.getAttachedArtifacts().iterator(); it.hasNext(); )
                {
                    final Artifact artifact = (Artifact) it.next();

                    if ( this.attachments )
                    {
                        this.processArtifact( artifact );
                    }
                    else if ( this.isJavaLanguageCapable( artifact ) )
                    {
                        this.getLog().info( this.getMessage( "ignoringAttachment", new Object[]
                            {
                                artifact.toString()
                            } ) );

                    }
                }
            }
        }
        else
        {
            this.getLog().info( this.getMessage( "disabled", null ) );
        }
    }

    /**
     * Gets the {@code Commandline} to execute for a given Java archive taking a command line prepared for executing
     * jarsigner.
     *
     * @param archive The Java archive to get a {@code Commandline} to execute for.
     * @param commandLine A {@code Commandline} prepared for executing jarsigner without any arguments.
     *
     * @return A {@code Commandline} for executing jarsigner with {@code archive}.
     *
     * @throws NullPointerException if {@code archive} or {@code commandLine} is {@code null}.
     */
    protected abstract Commandline getCommandline( final File archive, final Commandline commandLine );

    /**
     * Gets a string representation of a {@code Commandline}.
     * <p>This method creates the string representation by calling {@code commandLine.toString()} by default.</p>
     *
     * @param commandLine The {@code Commandline} to get a string representation of.
     *
     * @return The string representation of {@code commandLine}.
     *
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
     * Checks Java language capability of an artifact.
     *
     * @param artifact The artifact to check.
     *
     * @return {@code true} if {@code artifact} is Java language capable; {@code false} if not.
     */
    private boolean isJavaLanguageCapable( final Artifact artifact )
    {
        return artifact != null && artifact.getFile() != null && artifact.getArtifactHandler() != null
            && "java".equals( artifact.getArtifactHandler().getLanguage() );
    }

    /**
     * Processes a given artifact.
     *
     * @param artifact The artifact to process.
     *
     * @throws NullPointerException if {@code artifact} is {@code null}.
     * @throws MojoExecutionException if processing {@code artifact} fails.
     */
    private void processArtifact( final Artifact artifact )
        throws MojoExecutionException
    {
        if ( artifact == null )
        {
            throw new NullPointerException( "artifact" );
        }

        if ( this.isJavaLanguageCapable( artifact ) )
        {
            if ( this.verbose )
            {
                this.getLog().info( this.getMessage( "processing", new Object[]
                    {
                        artifact.toString()
                    } ) );

            }
            else if ( this.getLog().isDebugEnabled() )
            {
                this.getLog().debug( this.getMessage( "processing", new Object[]
                    {
                        artifact.toString()
                    } ) );

            }

            this.processArchive( artifact.getFile() );
        }
        else
        {
            if ( this.verbose )
            {
                this.getLog().info( this.getMessage( "unsupported", new Object[]
                    {
                        artifact.toString()
                    } ) );

            }
            else if ( this.getLog().isDebugEnabled() )
            {
                this.getLog().debug( this.getMessage( "unsupported", new Object[]
                    {
                        artifact.toString()
                    } ) );

            }
        }
    }

    /**
     * Processes a given archive.
     *
     * @param archive The archive to process.
     *
     * @throws NullPointerException if {@code archive} is {@code null}.
     * @throws MojoExecutionException if processing {@code archive} fails.
     */
    private void processArchive( final File archive )
        throws MojoExecutionException
    {
        if ( archive == null )
        {
            throw new NullPointerException( "archive" );
        }

        Commandline commandLine = new Commandline();
        commandLine.setExecutable( "jarsigner" + ( Os.isFamily( Os.FAMILY_WINDOWS ) ? ".exe" : "" ) );
        commandLine.setWorkingDirectory( this.project.getBasedir() );

        if ( this.verbose )
        {
            commandLine.createArg().setValue( "-verbose" );
        }

        if ( this.arguments != null )
        {
            commandLine.addArguments( this.arguments );
        }

        commandLine = this.getCommandline( archive, commandLine );

        try
        {
            if ( this.getLog().isDebugEnabled() )
            {
                this.getLog().debug( this.getMessage( "command", new Object[]
                    {
                        this.getCommandlineInfo( commandLine )
                    } ) );

            }

            final int result = CommandLineUtils.executeCommandLine( commandLine,
                new InputStream()
            {

                public int read()
                {
                    return -1;
                }

            }, new StreamConsumer()
            {

                public void consumeLine( final String line )
                {
                    if ( verbose )
                    {
                        getLog().info( line );
                    }
                }

            }, new StreamConsumer()
            {

                public void consumeLine( final String line )
                {
                    getLog().warn( line );
                }

            } );

            if ( result != 0 )
            {
                throw new MojoExecutionException( this.getMessage( "failure", new Object[]
                    {
                        this.getCommandlineInfo( commandLine ), new Integer( result )
                    } ) );

            }
        }
        catch ( CommandLineException e )
        {
            throw new MojoExecutionException( this.getMessage( "commandLineException", new Object[]
                {
                    this.getCommandlineInfo( commandLine )
                } ), e );

        }
    }

    /**
     * Gets a message for a given key from the resource bundle backing the implementation.
     *
     * @param key The key of the message to return.
     * @param args Arguments to format the message with or {@code null}.
     *
     * @return The message with key {@code key} from the resource bundle backing the implementation.
     *
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws MissingResourceException if there is no message available matching {@code key} or accessing the resource
     * bundle fails.
     */
    private String getMessage( final String key, final Object[] args )
    {
        if ( key == null )
        {
            throw new NullPointerException( "key" );
        }

        return new MessageFormat( ResourceBundle.getBundle(
            "org/apache/maven/plugins/jarsigner/AbstractJarsignerMojo" ).getString( key ) ).format( args );

    }

}
