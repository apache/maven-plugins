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

import org.apache.maven.plugin.clover.internal.AbstractCloverMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.Project;
import com.cenqua.clover.tasks.HistoryPointTask;

import java.io.File;

/**
 * Save a <a href="http://cenqua.com/clover/doc/tutorial/part2.html">Clover history point</a>.
 *
 * @goal save-history
 *
 * @version $Id$
 */
public class CloverSaveHistoryMojo extends AbstractCloverMojo
{
    /**
     * The location where historical Clover data will be saved.
     *
     * <p>Note: It's recommended to modify the location of this directory so that it points to a more permanent
     * location as the <code>${project.build.directory}</code> directory is erased when the project is cleaned.</p>
     *
     * @parameter default-value="${project.build.directory}/clover/history"
     * @required
     */
    private String historyDir;

    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.clover.internal.AbstractCloverMojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( areCloverDatabasesAvailable() )
        {
            super.execute();

            AbstractCloverMojo.waitForFlush( getWaitForFlush(), getFlushInterval() );

            save();
        }
        else
        {
            getLog().info("No Clover database found, skipping the Clover history point save");
        }
    }

    /**
     * Save a history point for both the main Clover database and the merged Clover database when they exist.
     */
    private void save()
    {
        if ( new File( getCloverDatabase() ).exists() )
        {
            saveDatabase( getCloverDatabase() );
        }
        if ( new File( getCloverMergeDatabase() ).exists() )
        {
            saveDatabase( getCloverMergeDatabase() );
        }
    }

    /**
     * Save a history point for a Clover database.
     *
     * @param database the Clover database to save
     */
    private void saveDatabase(String database)
    {
        Project antProject = AbstractCloverMojo.registerCloverAntTasks();

        getLog().info( "Saving Clover history point for database [" + database + "] in ["
            + this.historyDir + "]" );

        HistoryPointTask cloverHistoryTask = (HistoryPointTask) antProject.createTask( "clover-historypoint" );
        cloverHistoryTask.setInitString( database );
        cloverHistoryTask.setHistoryDir( new File( this.historyDir ) );
        cloverHistoryTask.execute();
    }
}
