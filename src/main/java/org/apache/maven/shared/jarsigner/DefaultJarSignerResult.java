package org.apache.maven.shared.jarsigner;

import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Describes the result of a JarSigner invocation.
 *
 * @author tchemit <chemit@codelutin.com>
 * @version $Id$
 * @since 1.0
 */
public class DefaultJarSignerResult
    implements JarSignerResult
{

    /**
     * The exception that prevented to execute the command line, will be <code>null</code> if jarSigner could be
     * successfully started.
     */
    private CommandLineException executionException;

    /**
     * The exit code reported by the Maven invocation.
     */
    private int exitCode = Integer.MIN_VALUE;

    private Commandline commandline;

    /**
     * Creates a new invocation result
     */
    DefaultJarSignerResult()
    {
        // hide constructor
    }

    public int getExitCode()
    {
        return exitCode;
    }

    public Commandline getCommandline()
    {
        return commandline;
    }

    public CommandLineException getExecutionException()
    {
        return executionException;
    }

    /**
     * Sets the exit code reported by the Jarsigner invocation.
     *
     * @param exitCode The exit code reported by the JarSigner invocation.
     */
    void setExitCode( int exitCode )
    {
        this.exitCode = exitCode;
    }

    /**
     * Sets the exception that prevented to execute the command line.
     *
     * @param executionException The exception that prevented to execute the command line, may be <code>null</code>.
     */
    void setExecutionException( CommandLineException executionException )
    {
        this.executionException = executionException;
    }

    void setCommandline( Commandline commandline )
    {
        this.commandline = commandline;
    }
}
