package org.apache.maven.plugin.patch;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.shell.BourneShell;

/**
 * Apply a set of patches to project sources.
 *
 * @goal apply-directory
 * @phase generate-sources
 */
public class ApplyPatchDirectoryMojo
    extends AbstractPatchMojo
{

    public static final List PATCH_FAILURE_WATCH_PHRASES;

    static
    {
        List watches = new ArrayList();

        watches.add( "fail" );
        watches.add( "skip" );
        watches.add( "reject" );

        PATCH_FAILURE_WATCH_PHRASES = watches;
    }

    /**
     * Whether to skip this mojo's execution.
     *
     * @parameter default-value="false" alias="patch.apply.skip"
     */
    private boolean skipApplication;

    /**
     * Flag to enable/disable optimization file from being written. This file tracks the patches that were applied the
     * last time this mojo actually executed. It is required for cases where project-sources optimizations are enabled,
     * since project-sources will not be re-unpacked if they are at least as fresh as the source archive. If we avoid
     * re-unpacking project sources, we need to make sure we don't reapply patches. This flag is true by default. <br/>
     * <b>NOTE:</b> If the list of patches changes and this flag is enabled, a `mvn clean` must be executed before the
     * next build, to remove the tracking file.
     *
     * @parameter default-value="true"
     */
    private boolean optimizations;

    /**
     * This is the tracking file used to maintain a list of the patches applied to the unpacked project sources which
     * are currently in the target directory. If this file is present, and project-source unpacking is optimized
     * (meaning it won't re-unpack unless the project-sources archive is newer), this mojo will not execute and no
     * patches will be applied in the current build.
     *
     * @parameter default-value="${project.build.directory}/optimization-files/patches-applied.txt"
     */
    private File patchTrackingFile;

    /**
     * The target directory for applying patches. Files in this directory will be modified.
     *
     * @parameter alias="patchTargetDir" default-value="${project.build.sourceDirectory}"
     * @required
     */
    private File targetDirectory;

    /**
     * When the strictPatching flag is set, this parameter is useful to mark certain contents of the patch-source
     * directory that should be ignored without causing the build to fail.
     *
     * @parameter
     */
    private List ignoredPatches;

    /**
     * Flag that, when set to true, will make sure that all patches included in the 'patches' list must be present and
     * describe the full contents of the patch directory. If strictPatching is set to true, and the patches list has a
     * value that does not correspond to a file in the patch directory, the build will fail. If strictPatching is set to
     * true, and the patch directory contains files not listed in the patches parameter, the build will fail. If set to
     * false, only the patches listed in the patches parameter that have corresponding files will be applied; the rest
     * will be ignored. Default value for this parameter is false.
     *
     * @parameter default-value="false"
     * @required
     */
    private boolean strictPatching;

    /**
     * The number of directories to be stripped from patch file paths, before applying, starting from the leftmost, or
     * root-est.
     *
     * @parameter
     */
    private int strip = 0;

    /**
     * Whether to ignore whitespaces when applying the patches.
     *
     * @parameter
     */
    private boolean ignoreWhitespace = true;

    /**
     * Whether to treat these patches as having reversed source and dest in the patch syntax.
     *
     * @parameter
     */
    private boolean reverse = false;

    /**
     * Whether to make backups of the original files before modding them.
     *
     * @parameter
     */
    private boolean backups = false;

    /**
     * List of phrases to watch for in patch-command output. If one is found, it will cause the build to fail. All
     * phrases should be lower-case ONLY.
     *
     * @parameter
     */
    private List patchFailureWatchPhrases = PATCH_FAILURE_WATCH_PHRASES;

    /**
     * @parameter default-value="src/main/patches"
     * @required
     */
    private File patchDirectory;

    /**
     * Apply the patches. Give preference to patchFile over patchSourceDir/patches, and preference to originalFile over
     * workDir.
     */
    public void doExecute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skipApplication )
        {
            getLog().info( "Skipping patchfile application (per configuration)." );
            return;
        }

        patchTrackingFile.getParentFile().mkdirs();
        if ( optimizations && patchTrackingFile.exists() )
        {
            getLog().info( "Skipping patchfile application (patches already applied in previous build)." );
            return;
        }

        List foundPatchFiles = new ArrayList( Arrays.asList( patchDirectory.list() ) );

        Map patchesApplied = findPatchesToApply( foundPatchFiles, patchDirectory );

        checkStrictPatchCompliance( foundPatchFiles );

        String output = applyPatches( patchesApplied );

        checkForWatchPhrases( output );

        writeTrackingFile( patchesApplied );
    }

    private Map findPatchesToApply( List foundPatchFiles, File patchSourceDir )
        throws MojoFailureException
    {
        List patches = getPatches();

        Map patchesApplied = new LinkedHashMap( patches.size() );

        for ( Iterator it = patches.iterator(); it.hasNext(); )
        {
            String patch = (String) it.next();

            File patchFile = new File( patchSourceDir, patch );

            getLog().debug( "Looking for patch: " + patch + " in: " + patchFile );

            if ( !patchFile.exists() )
            {
                if ( strictPatching )
                {
                    throw new MojoFailureException(
                                                    this,
                                                    "Patch operation cannot proceed.",
                                                    "Cannot find specified patch: \'"
                                                                    + patch
                                                                    + "\' in patch-source directory: \'"
                                                                    + patchSourceDir
                                                                    + "\'.\n\nEither fix this error, or relax strictPatching." );
                }
                else
                {
                    getLog().info( "Skipping patch: " + patch + " listed in the patches parameter; it is missing." );
                }
            }
            else
            {
                foundPatchFiles.remove( patch );

                patchesApplied.put( patch, createPatchCommand( patchFile ) );
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

            if ( useDefaultIgnores() )
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
                StringBuffer extraFileBuffer = new StringBuffer();

                extraFileBuffer.append( "Found " + limbo.size() + " unlisted patch files:" );

                for ( Iterator it = foundPatchFiles.iterator(); it.hasNext(); )
                {
                    String patch = (String) it.next();

                    extraFileBuffer.append( "\n  \'" ).append( patch ).append( '\'' );
                }

                extraFileBuffer.append( "\n\nEither remove these files, add them to the patches configuration list, or relax strictPatching." );

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

        for ( Iterator it = patchesApplied.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Entry) it.next();
            String patchName = (String) entry.getKey();
            Commandline cli = (Commandline) entry.getValue();

            try
            {
                int result = CommandLineUtils.executeCommandLine( cli, consumer, consumer );

                if ( result != 0 )
                {
                    throw new MojoExecutionException( "Patch command failed (exit value != 0). Please see debug output for more information." );
                }
            }
            catch ( CommandLineException e )
            {
                throw new MojoExecutionException( "Failed to apply patch: " + patchName
                                + ". See debug output for more information.", e );
            }
        }

        return outputWriter.toString();
    }

    private void writeTrackingFile( Map patchesApplied )
        throws MojoExecutionException
    {
        FileWriter writer = null;
        try
        {
            writer = new FileWriter( patchTrackingFile );

            for ( Iterator it = patchesApplied.keySet().iterator(); it.hasNext(); )
            {
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
        for ( Iterator it = patchFailureWatchPhrases.iterator(); it.hasNext(); )
        {
            String phrase = (String) it.next();

            if ( output.indexOf( phrase ) > -1 )
            {
                throw new MojoExecutionException( "Failed to apply patches (detected watch-phrase: \'" + phrase
                                + "\' in output). "
                                + "If this is in error, configure the patchFailureWatchPhrases parameter." );
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
        cli.setShell( new BourneShell() );

        cli.setExecutable( "patch" );

        cli.setWorkingDirectory( targetDirectory.getAbsolutePath() );

        cli.createArg().setLine( "-p" + strip );

        if ( ignoreWhitespace )
        {
            cli.createArg().setLine( "-l" );
        }

        if ( reverse )
        {
            cli.createArg().setLine( "-R" );
        }

        if ( backups )
        {
            cli.createArg().setLine( "-b" );
        }

        cli.createArg().setLine( " < " + patchFile.getAbsolutePath() );

        return cli;
    }

}
