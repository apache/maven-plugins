package org.apache.maven.plugins.release.exec;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
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
 */

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.apache.maven.plugins.release.ReleaseResult;

import java.io.File;

/**
 * Fork Maven to executed a series of goals.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ForkedMavenExecutor
    extends AbstractLogEnabled
    implements MavenExecutor
{
    /**
     * Command line factory.
     */
    private CommandLineFactory commandLineFactory;

    /**
     * @noinspection UseOfSystemOutOrSystemErr
     */
    public void executeGoals( File workingDirectory, String goals, boolean interactive,
                                             String additionalArguments, String pomFileName, ReleaseResult relResult )
        throws MavenExecutorException
    {
        Commandline cl = commandLineFactory.createCommandLine( "mvn" );

        cl.setWorkingDirectory( workingDirectory.getAbsolutePath() );

        cl.addEnvironment( "MAVEN_TERMINATE_CMD", "on" );

        if ( pomFileName != null )
        {
            cl.createArgument().setLine( "-f " + pomFileName );
        }

        if ( goals != null )
        {
            // accept both space and comma, so the old way still work
            String [] tokens = StringUtils.split( goals, ", " );

            for ( int i = 0; i < tokens.length; ++i )
            {
                cl.createArgument().setValue( tokens[i] );
            }
        }

        cl.createArgument().setValue( "--no-plugin-updates" );

        if ( !interactive )
        {
            cl.createArgument().setValue( "--batch-mode" );
        }

        if ( !StringUtils.isEmpty( additionalArguments ) )
        {
            cl.createArgument().setLine( additionalArguments );
        }

        StreamConsumer stdOut = new TeeConsumer( System.out );

        StreamConsumer stdErr = new TeeConsumer( System.err );

        try
        {
            relResult.appendInfo( "Executing: " + cl.toString() );
            getLogger().info( "Executing: " + cl.toString() );

            int result = CommandLineUtils.executeCommandLine( cl, stdOut, stdErr );

            if ( result != 0 )
            {
                throw new MavenExecutorException( "Maven execution failed, exit code: \'" + result + "\'", result,
                                                  stdOut.toString(), stdErr.toString() );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MavenExecutorException( "Can't run goal " + goals, stdOut.toString(), stdErr.toString(), e );
        }
        finally
        {
            relResult.appendOutput( stdOut.toString() );
        }
    }

    public void executeGoals( File workingDirectory, String goals, boolean interactive,
                                             String arguments, ReleaseResult result )
        throws MavenExecutorException
    {
        executeGoals( workingDirectory, goals, interactive, arguments, null, result );
    }

    public void setCommandLineFactory( CommandLineFactory commandLineFactory )
    {
        this.commandLineFactory = commandLineFactory;
    }
}
