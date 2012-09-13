package org.apache.maven.plugin.patch;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Apply one or more patches to project sources.
 */
@Mojo( name = "apply", defaultPhase = LifecyclePhase.PROCESS_SOURCES )
public class ApplyMojo
    extends AbstractMojo
{

    public static final List PATCH_FAILURE_WATCH_PHRASES;

    public static final List DEFAULT_IGNORED_PATCHES;

    public static final List DEFAULT_IGNORED_PATCH_PATTERNS;

    static
    {
        List watches = new ArrayList();

        watches.add( "fail" );
        watches.add( "skip" );
        watches.add( "reject" );

        PATCH_FAILURE_WATCH_PHRASES = watches;

        List ignored = new ArrayList();

        ignored.add( ".svn" );
        ignored.add( "CVS" );

        DEFAULT_IGNORED_PATCHES = ignored;

        List ignoredPatterns = new ArrayList();

        ignoredPatterns.add( ".svn/**" );
        ignoredPatterns.add( "CVS/**" );

        DEFAULT_IGNORED_PATCH_PATTERNS = ignoredPatterns;
    }

    /**
     * Whether to exclude default ignored patch items, such as <code>.svn</code> or <code>CVS</code> directories.
     */
    @Parameter( defaultValue = "true" )
    private boolean useDefaultIgnores;

    /**
     * The list of patch file names, supplying the order in which patches should be applied. The path names in this list
     * must be relative to the base directory specified by the parameter <code>patchDirectory</code>. This parameter
     * is mutually exclusive with the <code>patchfile</code> parameter.
     */
    @Parameter
    protected List patches;

    /**
     * Whether to skip this goal's execution.
     */
    @Parameter( alias = "patch.apply.skip", defaultValue = "false" )
    private boolean skipApplication;

    /**
     * Flag to enable/disable optimization file from being written. This file tracks the patches that were applied the
     * last time this goal actually executed. It is required for cases where project-sources optimizations are enabled,
     * since project-sources will not be re-unpacked if they are at least as fresh as the source archive. If we avoid
     * re-unpacking project sources, we need to make sure we don't reapply patches.<br/> <strong>Note:</strong> If the
     * list of patches changes and this flag is enabled, a "<code>mvn clean</code>" must be executed before the next
     * build, to remove the tracking file.
     */
    @Parameter( defaultValue = "true" )
    private boolean optimizations;

    /**
     * This is the tracking file used to maintain a list of the patches applied to the unpacked project sources which
     * are currently in the target directory. If this file is present, and project-source unpacking is optimized
     * (meaning it won't re-unpack unless the project-sources archive is newer), this goal will not execute and no
     * patches will be applied in the current build.
     */
    @Parameter( defaultValue = "${project.build.directory}/optimization-files/patches-applied.txt" )
    private File patchTrackingFile;

    /**
     * The target directory for applying patches. Files in this directory will be modified.
     */
    @Parameter( alias = "patchTargetDir", defaultValue = "${project.build.sourceDirectory}" )
    private File targetDirectory;

    /**
     * Flag being <code>true</code> if the desired behavior is to fail the build on the first failed patch detected.
     */
    @Parameter( defaultValue = "true" )
    private boolean failFast;

    /**
     * Setting natural order processing to <code>true</code> will cause all patches in a directory to be processed in
     * a natural order alleviating the need to declare patches directly in the project file.
     */
    @Parameter( defaultValue = "false" )
    private boolean naturalOrderProcessing;

    /**
     * When the <code>strictPatching</code> flag is set, this parameter is useful to mark certain contents of the
     * patch-source directory that should be ignored without causing the build to fail.
     */
    @Parameter
    private List ignoredPatches;

    /**
     * Flag that, when set to <code>true</code>, will make sure that all patches included in the <code>patches</code>
     * list must be present and describe the full contents of the patch directory. If <code>strictPatching</code> is
     * set to <code>true</code>, and the <code>patches</code> list has a value that does not correspond to a file
     * in the patch directory, the build will fail. If <code>strictPatching</code> is set to <code>true</code>, and
     * the patch directory contains files not listed in the <code>patches</code> parameter, the build will fail. If
     * set to <code>false</code>, only the patches listed in the <code>patches</code> parameter that have
     * corresponding files will be applied; the rest will be ignored.
     */
    @Parameter( defaultValue = "false" )
    private boolean strictPatching;

    /**
     * The number of directories to be stripped from patch file paths, before applying, starting from the leftmost, or
     * root-est.
     */
    @Parameter( defaultValue = "0" )
    private int strip;

    /**
     * Whether to ignore whitespaces when applying the patches.
     */
    @Parameter( defaultValue = "true" )
    private boolean ignoreWhitespace;

    /**
     * Whether to treat these patches as having reversed source and dest in the patch syntax.
     */
    @Parameter( defaultValue = "false" )
    private boolean reverse;

    /**
     * Whether to make backups of the original files before modding them.
     */
    @Parameter( defaultValue = "false" )
    private boolean backups;

    /**
     * List of phrases to watch for in the command output from the patch tool. If one is found, it will cause the build
     * to fail. All phrases should be lower-case <em>only</em>. By default, the phrases <code>fail</code>,
     * <code>skip</code> and <code>reject</code> are used.
     */
    @Parameter
    private List failurePhrases = PATCH_FAILURE_WATCH_PHRASES;

    /**
     * The original file which will be modified by the patch. By default, the patch tool will automatically derive the
     * original file from the header of the patch file.
     */
    @Parameter
    private File originalFile;

    /**
     * The output file which is the original file, plus modifications from the patch. By default, the file(s) will be
     * patched inplace.
     */
    @Parameter
    private File destFile;

    /**
     * The single patch file to apply. This parameter is mutually exclusive with the <code>patches</code> parameter.
     */
    @Parameter
    private File patchFile;

    /**
     * The base directory for the file names specified by the parameter <code>patches</code>.
     */
    @Parameter( defaultValue = "src/main/patches" )
    private File patchDirectory;

    /**
     * When set to <code>true</code>, the empty files resulting from the patching process are removed. Empty ancestor
     * directories are removed as well.
     *
     * @since 1.1
     */
    @Parameter( defaultValue = "false" )
    private boolean removeEmptyFiles;

    /**
     * Apply the patches. Give preference to patchFile over patchSourceDir/patches, and preference to originalFile over
     * workDir.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        boolean patchDirEnabled = ( ( patches != null ) && !patches.isEmpty() ) || naturalOrderProcessing;
        boolean patchFileEnabled = patchFile != null;

        // if patches is null or empty, and naturalOrderProcessing is not true then disable patching
        if ( !patchFileEnabled && !patchDirEnabled )
        {
            getLog().info( "Patching is disabled for this project." );
            return;
        }

        if ( skipApplication )
        {
            getLog().info( "Skipping patch file application (per configuration)." );
            return;
        }

        patchTrackingFile.getParentFile().mkdirs();

        Map patchesToApply = null;

        try
        {
            if ( patchFileEnabled )
            {
                patchesToApply = Collections.singletonMap( patchFile.getName(), createPatchCommand( patchFile ) );
            }
            else
            {
                if ( !patchDirectory.isDirectory() )
                {
                    throw new FileNotFoundException( "The base directory for patch files does not exist: "
                        + patchDirectory );
                }

                List foundPatchFiles = FileUtils.getFileNames( patchDirectory, "*", null, false );

                patchesToApply = findPatchesToApply( foundPatchFiles, patchDirectory );

                checkStrictPatchCompliance( foundPatchFiles );
            }

            String output = applyPatches( patchesToApply );

            checkForWatchPhrases( output );

            writeTrackingFile( patchesToApply );
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Unable to obtain list of patch files", ioe );
        }
    }

    private Map findPatchesToApply( List foundPatchFiles, File patchSourceDir )
        throws MojoFailureException
    {
        Map patchesApplied = new LinkedHashMap();

        if ( naturalOrderProcessing )
        {
            patches = new ArrayList( foundPatchFiles );
            Collections.sort( patches );
        }

        String alreadyAppliedPatches = "";

        try
        {
            if ( optimizations && patchTrackingFile.exists() )
            {
                alreadyAppliedPatches = FileUtils.fileRead( patchTrackingFile );
            }
        }
        catch ( IOException ioe )
        {
            throw new MojoFailureException( "unable to read patch tracking file: " + ioe.getMessage() );
        }

        for ( Iterator it = patches.iterator(); it.hasNext(); )
        {
            String patch = (String) it.next();

            if ( alreadyAppliedPatches.indexOf( patch ) == -1 )
            {
                File patchFile = new File( patchSourceDir, patch );

                getLog().debug( "Looking for patch: " + patch + " in: " + patchFile );

                if ( !patchFile.exists() )
                {
                    if ( strictPatching )
                    {
                        throw new MojoFailureException( this, "Patch operation cannot proceed.",
                                                        "Cannot find specified patch: \'" + patch
                                                            + "\' in patch-source directory: \'" + patchSourceDir
                                                            + "\'.\n\nEither fix this error, "
                                                            + "or relax strictPatching." );
                    }
                    else
                    {
                        getLog().info(
                                       "Skipping patch: " + patch + " listed in the parameter \"patches\"; "
                                           + "it is missing." );
                    }
                }
                else
                {
                    foundPatchFiles.remove( patch );

                    patchesApplied.put( patch, createPatchCommand( patchFile ) );
                }
            }
        }

        return patchesApplied;
    }

    private void checkStrictPatchCompliance( List foundPatchFiles )
        throws MojoExecutionException
    {
        if ( strictPatching )
        {
            List ignored = new ArrayList();

            if ( ignoredPatches != null )
            {
                ignored.addAll( ignoredPatches );
            }

            if ( useDefaultIgnores )
            {
                ignored.addAll( DEFAULT_IGNORED_PATCHES );
            }

            List limbo = new ArrayList( foundPatchFiles );

            for ( Iterator it = ignored.iterator(); it.hasNext(); )
            {
                String ignoredFile = (String) it.next();

                limbo.remove( ignoredFile );
            }

            if ( !limbo.isEmpty() )
            {
                StringBuilder extraFileBuffer = new StringBuilder();

                extraFileBuffer.append( "Found " + limbo.size() + " unlisted patch files:" );

                for ( Iterator it = foundPatchFiles.iterator(); it.hasNext(); )
                {
                    String patch = (String) it.next();

                    extraFileBuffer.append( "\n  \'" ).append( patch ).append( '\'' );
                }

                extraFileBuffer.append( "\n\nEither remove these files, "
                    + "add them to the patches configuration list, " + "or relax strictPatching." );

                throw new MojoExecutionException( extraFileBuffer.toString() );
            }
        }
    }

    private String applyPatches( Map patchesApplied )
        throws MojoExecutionException
    {
        final StringWriter outputWriter = new StringWriter();

        StreamConsumer consumer = new StreamConsumer()
        {
            public void consumeLine( String line )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( line );
                }

                outputWriter.write( line + "\n" );
            }
        };

        // used if failFast is false
        List failedPatches = new ArrayList();

        for ( Iterator it = patchesApplied.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Entry) it.next();
            String patchName = (String) entry.getKey();
            Commandline cli = (Commandline) entry.getValue();

            try
            {
                getLog().info( "Applying patch: " + patchName );

                int result = executeCommandLine( cli, consumer, consumer );

                if ( result != 0 )
                {
                    if ( failFast )
                    {
                        throw new MojoExecutionException( "Patch command failed with exit code " + result + " for "
                            + patchName + ". Please see console and debug output for more information." );
                    }
                    else
                    {
                        failedPatches.add( patchName );
                    }
                }
            }
            catch ( CommandLineException e )
            {
                throw new MojoExecutionException( "Failed to apply patch: " + patchName
                    + ". See debug output for more information.", e );
            }
        }

        if ( !failedPatches.isEmpty() )
        {
            getLog().error( "Failed applying one or more patches:" );
            for ( Iterator it = failedPatches.iterator(); it.hasNext(); )
            {
                getLog().error( "* " + it.next() );
            }
            throw new MojoExecutionException( "Patch command failed for one or more patches."
                + " Please see console and debug output for more information." );
        }

        return outputWriter.toString();
    }

    private int executeCommandLine( Commandline cli, StreamConsumer out, StreamConsumer err )
        throws CommandLineException
    {
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Executing: " + cli );
        }

        int result = CommandLineUtils.executeCommandLine( cli, out, err );

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Exit code: " + result );
        }

        return result;
    }

    private void writeTrackingFile( Map patchesApplied )
        throws MojoExecutionException
    {
        FileWriter writer = null;
        try
        {
            boolean appending = patchTrackingFile.exists();

            writer = new FileWriter( patchTrackingFile, appending );

            for ( Iterator it = patchesApplied.keySet().iterator(); it.hasNext(); )
            {
                if ( appending )
                {
                    writer.write( System.getProperty( "line.separator" ) );
                }

                String patch = (String) it.next();
                writer.write( patch );

                if ( it.hasNext() )
                {
                    writer.write( System.getProperty( "line.separator" ) );
                }
            }

            writer.flush();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to write patch-tracking file: " + patchTrackingFile, e );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    private void checkForWatchPhrases( String output )
        throws MojoExecutionException
    {
        for ( Iterator it = failurePhrases.iterator(); it.hasNext(); )
        {
            String phrase = (String) it.next();

            if ( output.indexOf( phrase ) > -1 )
            {
                throw new MojoExecutionException( "Failed to apply patches (detected watch-phrase: \'" + phrase
                    + "\' in output). " + "If this is in error, configure the patchFailureWatchPhrases parameter." );
            }
        }
    }

    /**
     * Add a new Patch task to the Ant calling mechanism. Give preference to originalFile/destFile, then workDir, and
     * finally ${basedir}.
     */
    private Commandline createPatchCommand( File patchFile )
    {
        Commandline cli = new Commandline();

        cli.setExecutable( "patch" );

        cli.setWorkingDirectory( targetDirectory.getAbsolutePath() );

        cli.createArg().setValue( "-p" + strip );

        if ( ignoreWhitespace )
        {
            cli.createArg().setValue( "-l" );
        }

        if ( reverse )
        {
            cli.createArg().setValue( "-R" );
        }

        if ( backups )
        {
            cli.createArg().setValue( "-b" );
        }

        if ( removeEmptyFiles )
        {
            cli.createArg().setValue( "-E" );
        }

        cli.createArg().setValue( "-i" );
        cli.createArg().setFile( patchFile );

        if ( destFile != null )
        {
            cli.createArg().setValue( "-o" );
            cli.createArg().setFile( destFile );
        }

        if ( originalFile != null )
        {
            cli.createArg().setFile( originalFile );
        }

        return cli;
    }

}
