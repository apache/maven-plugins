package org.apache.maven.plugins.release.phase;

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

import org.apache.maven.plugins.release.ReleaseExecutionException;
import org.apache.maven.plugins.release.config.ReleaseConfiguration;

/**
 * Remove release POMs.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RemoveReleasePomsPhase
    extends AbstractReleasePhase
{
    public void execute( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        // TODO [!]: implement
        getLogger().info( "Removing release POMs..." );

/*
        File currentReleasePomFile = null;

        try
        {
            String canonicalBasedir = trimPathForScmCalculation( basedir );

            for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
            {
                MavenProject project = (MavenProject) it.next();

                currentReleasePomFile = new File( project.getFile().getParentFile(), RELEASE_POM );

                String releasePomPath = trimPathForScmCalculation( currentReleasePomFile );

                releasePomPath = releasePomPath.substring( canonicalBasedir.length() + 1 );

                ScmHelper scm = getScm( basedir.getAbsolutePath() );
                if ( !dryRun )
                {
                    scm.remove( "Removing for next development iteration.", releasePomPath );
                }
                else
                {
                    getLog().info( "[TESTMODE] Removing for next development iteration. " + releasePomPath );
                }

                pomFiles.remove( currentReleasePomFile );

                currentReleasePomFile.delete();
            }
        }
        catch ( ScmException e )
        {
            throw new MojoExecutionException( "Cannot remove " + currentReleasePomFile + " from development HEAD.",
                                                                      e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot remove " + currentReleasePomFile + " from development HEAD.",
                                              e );
        }

*/
    }

    public void simulate( ReleaseConfiguration releaseConfiguration )
    {
        // TODO [!]: implement

    }
}
