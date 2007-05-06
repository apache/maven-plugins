/*
 * Copyright 2001-2007 The Apache Software Foundation.
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
package org.apache.maven.plugin.clover;

import com.cenqua.clover.tasks.CloverLogTask;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.clover.internal.AbstractCloverMojo;
import org.apache.tools.ant.Project;

import java.io.File;

/**
 * Provides information on the current Clover database.
 *
 * @goal log
 *
 * @version $Id$
 */
public class CloverLogMojo extends AbstractCloverMojo
{
    public void execute()
        throws MojoExecutionException
    {
        if ( areCloverDatabasesAvailable() )
        {
            super.execute();

            AbstractCloverMojo.waitForFlush( getWaitForFlush(), getFlushInterval() );

            log();
        }
        else
        {
            getLog().info("No Clover database found, skipping Clover database logging");
        }
    }

    /**
     * Log information for both the main Clover database and the merged Clover database when they exist.
     */
    private void log()
    {
        if ( new File( getCloverDatabase() ).exists() )
        {
            logDatabase( getCloverDatabase() );
        }
        if ( new File( getCloverMergeDatabase() ).exists() )
        {
            logDatabase( getCloverMergeDatabase() );
        }
    }

    /**
     * Log information from a Clover database.
     *
     * @param database the Clover database to log
     */
    private void logDatabase(String database)
    {
        Project antProject = AbstractCloverMojo.registerCloverAntTasks();

        CloverLogTask cloverLogTask = (CloverLogTask) antProject.createTask( "clover-log" );
        cloverLogTask.setInitString( database );
        cloverLogTask.setOutputProperty( "cloverlogproperty" );
        cloverLogTask.execute();

        getLog().info( antProject.getProperty( "cloverlogproperty" ) );
    }
}
