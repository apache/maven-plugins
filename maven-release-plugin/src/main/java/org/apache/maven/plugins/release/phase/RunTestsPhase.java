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
import org.apache.maven.plugins.release.exec.MavenExecutor;
import org.apache.maven.plugins.release.exec.MavenExecutorException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * Run the integration tests for the project to verify that it builds before committing.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RunTestsPhase
    extends AbstractLogEnabled
    implements ReleasePhase
{
    /**
     * Component to assist in executing Maven.
     */
    private MavenExecutor mavenExecutor;

    public void execute( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        try
        {
            mavenExecutor.executeGoals( releaseConfiguration.getWorkingDirectory(), "clean integration-test",
                                        releaseConfiguration.isInteractive() );
        }
        catch ( MavenExecutorException e )
        {
            throw new ReleaseExecutionException( e.getMessage(), e );
        }
    }

    public void simulate( ReleaseConfiguration releaseConfiguration )
        throws ReleaseExecutionException
    {
        getLogger().info(
            "Executing tests - since this is simulation mode it is testing the original project, not the rewritten ones" );

        execute( releaseConfiguration );
    }

    public void setMavenExecutor( MavenExecutor mavenExecutor )
    {
        this.mavenExecutor = mavenExecutor;
    }
}
