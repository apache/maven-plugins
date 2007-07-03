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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
//import org.codehaus.plexus.util.cli.shell.BourneShell;

import java.io.File;
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
 *
 * @goal apply
 * @phase process-sources
 */
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
     * Whether to exclude default ignored patch items, such as .svn or CVS directories.
     *
     * @parameter default-value="true"
     */
    private boolean useDefaultIgnores;

    /**
     * The list of patch file names (without directory information), supplying the order in which patches should be
     * applied. (relative to 'patchSourceDir')
     *
     * @parameter
     */
    protected List patches;

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
     * true if the desired behavior is to fail the build on the first failed patch detected
     *
     * @parameter default-value="true"
     */
    private boolean failFast;
    
    /**
     * setting natural order processing to true will all patches in a directory to be processed in an natural order
     * alleviating the need to declare patches directly in the project file.
     *
     * @parameter default-value="false"
     */
    private boolean naturalOrderProcessing;

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
    private List failurePhrases = PATCH_FAILURE_WATCH_PHRASES;

    /**
     * The original file which will be modified by the patch. Mutually exclusive with workDir.
     *
     * @parameter
     */
    private File originalFile;

    /**
     * The file which is the original file, plus modifications from the patch. Mutually exclusive with workDir.
     *
     * @parameter
     */
    private File destFile;

    /**
     * The single patch file to apply. Mutually exclusive with 'patches'.
     *
     * @parameter
     */
    private File patchFile;

    /**
     * @parameter default-value="src/main/patches"
     * @required
     */
    private File patchDirectory;

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
            getLog().info( "Skipping patchfile application (per configuration)." );
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
                List foundPatchFiles = FileUtils.getFileNames( patchDirectory, "*", null, false );

                patchesToApply = findPatchesToApply(foundPatchFiles, patchDirectory );

                checkStrictPatchCompliance(foundPatchFiles);
            }

			String output = applyPatches(patchesToApply);

			checkForWatchPhrases(output);

			writeTrackingFile(patchesToApply);
		}
        catch (IOException ioe)
		{
			throw new MojoExecutionException( "unable to obtain list of patch files", ioe);
		}
    }

    private Map findPatchesToApply( List foundPatchFiles, File patchSourceDir )
        throws MojoFailureException
    {
        Map patchesApplied = new LinkedHashMap();

        if ( naturalOrderProcessing )
        {
        	patches = new ArrayList( foundPatchFiles ) ;
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
        catch (IOException ioe)
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

        // used if failFast is false
        String failedPatches = null;
        
        for ( Iterator it = patchesApplied.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Entry) it.next();
            String patchName = (String) entry.getKey();
            Commandline cli = (Commandline) entry.getValue();

            try
            {
                getLog().info( "Applying patch: " + patchName );
                int result = executeCommandLine( cli, consumer, consumer );

                getLog().info( "patch command returned: " + result );
                
                if ( result != 0 )
                {
                    if ( failFast )
                    {
                        throw new MojoExecutionException( "Patch command failed (exit value != 0) for " + patchName + ". Please see debug output for more information." );
                    }
                    else
                    {
                        if ( failedPatches == null )
                        {
                            failedPatches = new String();
                        }
                        failedPatches = failedPatches + patchName + "\n";
                    }
                }
            }
            catch ( CommandLineException e )
            {
                throw new MojoExecutionException( "Failed to apply patch: " + patchName
                                + ". See debug output for more information.", e );
            }
        }
        
        if ( failedPatches != null )
        {
           getLog().info( "Failed applying one or more patches:" );
           getLog().info( failedPatches );
           throw new MojoExecutionException( "Patch command failed for one or more patches. Please see console and debug output for more information." );
        }

        return outputWriter.toString();
    }

    private int executeCommandLine( Commandline cli, StreamConsumer out, StreamConsumer err )
        throws CommandLineException
    {
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Executing:\n" + cli + "\n" );
        }

        getLog().info( Commandline.toString( cli.getShellCommandline() ) );
        
        return CommandLineUtils.executeCommandLine( cli, out, err );
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
        //cli.setShell( new BourneShell() );

        cli.setExecutable( "patch" );

        cli.setWorkingDirectory( targetDirectory.getAbsolutePath() );

        if ( originalFile != null )
        {
            cli.createArgument().setLine( originalFile.getAbsolutePath() );
            //cli.createArg().setLine( originalFile.getAbsolutePath() );

            if ( destFile != null )
            {
                cli.createArgument().setLine( "-o " + destFile.getAbsolutePath() );
                //cli.createArg().setLine( "-o " + destFile.getAbsolutePath() );
            }

            cli.createArgument().setValue( patchFile.getAbsolutePath() );
            //cli.createArg().setLine( patchFile.getAbsolutePath() );
        }

        cli.createArgument().setLine( "-p" + strip  );
        //cli.createArg().setLine( "-p" + strip );

        if ( ignoreWhitespace )
        {
            cli.createArgument().setValue( "-l" );
            //cli.createArg().setLine( "-l" );
        }

        if ( reverse )
        {
            cli.createArgument().setValue( "-R" );
            //cli.createArg().setLine( "-R" );
        }

        if ( backups )
        {
            cli.createArgument().setValue( "-b" );
            //cli.createArg().setLine( "-b" );
        }

        cli.createArgument().setLine( "-i " +  patchFile.getAbsolutePath() );
        //cli.createArg().setLine( " < " + patchFile.getAbsolutePath() );

        return cli;
    }

}
