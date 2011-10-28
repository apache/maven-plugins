package org.apache.maven.shared.jarsigner;

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Default implementation of component {@link JarSigner}.
 *
 * @author tchemit <chemit@codelutin.com>
 * @version $Id$
 * @plexus.component role="org.apache.maven.shared.jarsigner.JarSigner" role-hint="default"
 * @since 1.0
 */
public class DefaultJarSigner
    extends AbstractLogEnabled
    implements JarSigner
{

    /**
     * The location of the jarSigner executable file.
     */
    protected String jarSignerFile;

    public JarSignerResult execute( JarSignerRequest request )
        throws JarSignerException
    {

        if ( jarSignerFile == null )
        {

            // find the jar singer to use
            try
            {
                jarSignerFile = findJarSignerExecutable();
            }
            catch ( IOException e )
            {
                throw new JarSignerException( "Error finding jar signer executable. Reason: " + e.getMessage(), e );
            }
        }

        // creates the command line
        Commandline cli = createCommandLine( request );

        // execute it
        return executeCommandLine( cli, request );
    }

    protected Commandline createCommandLine( JarSignerRequest request )
        throws JarSignerException
    {
        JarSignerCommandLineBuilder cliBuilder = new JarSignerCommandLineBuilder();
        cliBuilder.setLogger( getLogger() );
        cliBuilder.setJarSignerFile( jarSignerFile );
        Commandline cli;
        try
        {
            cli = cliBuilder.build( request );
        }
        catch ( CommandLineConfigurationException e )
        {
            throw new JarSignerException( "Error configuring command-line. Reason: " + e.getMessage(), e );
        }
        return cli;
    }

    protected JarSignerResult executeCommandLine( Commandline cli, JarSignerRequest request )
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Executing: " + cli );
        }

        final boolean verbose = request.isVerbose();

        InputStream systemIn = new InputStream()
        {

            public int read()
            {
                return -1;
            }

        };
        StreamConsumer systemOut = new StreamConsumer()
        {

            public void consumeLine( final String line )
            {
                if ( verbose )
                {
                    getLogger().info( line );
                }
                else
                {
                    getLogger().debug( line );
                }
            }

        };

        StreamConsumer systemErr = new StreamConsumer()
        {

            public void consumeLine( final String line )
            {
                getLogger().warn( line );
            }

        };

        DefaultJarSignerResult result = new DefaultJarSignerResult();
        result.setCommandline( cli );

        try
        {
            int resultCode = CommandLineUtils.executeCommandLine( cli, systemIn, systemOut, systemErr );

            result.setExitCode( resultCode );
        }
        catch ( CommandLineException e )
        {
            result.setExecutionException( e );
        }

        return result;
    }

    protected String findJarSignerExecutable()
        throws IOException
    {
        String command = "jarsigner" + ( Os.isFamily( Os.FAMILY_WINDOWS ) ? ".exe" : "" );

        String executable =
            findExecutable( command, System.getProperty( "java.home" ), new String[]{ "../bin", "bin", "../sh" } );

        if ( executable == null )
        {
            try
            {
                Properties env = CommandLineUtils.getSystemEnvVars();

                String[] variables = { "JDK_HOME", "JAVA_HOME" };

                for ( int i = 0; i < variables.length && executable == null; i++ )
                {
                    executable =
                        findExecutable( command, env.getProperty( variables[i] ), new String[]{ "bin", "sh" } );
                }
            }
            catch ( IOException e )
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().warn( "Failed to retrieve environment variables, cannot search for " + command, e );
                }
                else
                {
                    getLogger().warn( "Failed to retrieve environment variables, cannot search for " + command );
                }
            }
        }

        if ( executable == null )
        {
            executable = command;
        }

        return executable;
    }

    /**
     * Finds the specified command in any of the given sub directories of the specified JDK/JRE home directory.
     *
     * @param command The command to find, must not be <code>null</code>.
     * @param homeDir The home directory to search in, may be <code>null</code>.
     * @param subDirs The sub directories of the home directory to search in, must not be <code>null</code>.
     * @return The (absolute) path to the command if found, <code>null</code> otherwise.
     */
    protected String findExecutable( String command, String homeDir, String[] subDirs )
    {
        if ( StringUtils.isNotEmpty( homeDir ) )
        {
            for ( int i = 0; i < subDirs.length; i++ )
            {
                File file = new File( new File( homeDir, subDirs[i] ), command );

                if ( file.isFile() )
                {
                    return file.getAbsolutePath();
                }
            }
        }

        return null;
    }
}
