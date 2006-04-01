/*
 * Copyright 2001-2006 The Apache Software Foundation.
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
import org.apache.tools.ant.Project;

/**
 * Provides information on the current Clover database.
 *
 * @goal log
 *
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @version $Id$
 *
 */
public class CloverLogMojo
    extends AbstractCloverMojo
{
    public void execute()
        throws MojoExecutionException
    {
        super.execute();

        Project antProject = registerCloverAntTasks();

        CloverLogTask cloverLogTask = (CloverLogTask) antProject.createTask( "clover-log" );
        cloverLogTask.setInitString( getCloverDatabase() );
        cloverLogTask.setOutputProperty( "cloverlogproperty" );
        cloverLogTask.execute();

        getLog().info( antProject.getProperty( "cloverlogproperty" ) );
    }
}
