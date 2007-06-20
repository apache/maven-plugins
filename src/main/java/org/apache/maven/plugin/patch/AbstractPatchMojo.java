package org.apache.maven.plugin.patch;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.cli.StreamConsumer;

public abstract class AbstractPatchMojo extends AbstractMojo
{

    public static final List DEFAULT_IGNORED_PATCHES;

    public static final List DEFAULT_IGNORED_PATCH_PATTERNS;

    static
    {
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
    
    protected List getPatches()
    {
        return patches;
    }

    protected boolean useDefaultIgnores()
    {
        return useDefaultIgnores;
    }

    public final void execute() throws MojoExecutionException, MojoFailureException
    {
        doExecute();
    }

    public StreamConsumer newDebugStreamConsumer()
    {
        return new StreamConsumer()
        {
            public void consumeLine( String line )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( line );
                }
            }
        };
    }

    public StreamConsumer newInfoStreamConsumer()
    {
        return new StreamConsumer()
        {
            public void consumeLine( String line )
            {
                if ( getLog().isInfoEnabled() )
                {
                    getLog().info( line );
                }
            }
        };
    }

    protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;

}
