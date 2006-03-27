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

import com.cenqua.clover.cfg.Percentage;
import com.cenqua.clover.tasks.CloverPassTask;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Verify test percentage coverage and fail the build if it is below the defined threshold.
 *
 * Note: We're forking a lifecycle because we don't want the Clover instrumentation to affect the main lifecycle build.
 * This will prevent instrumented sources to be put in production by error. Thus running <code>mvn install</code> on
 * a project where this <code>check</code> goal has been specified will run the build twice: once for building the
 * project as usual and another time for instrumenting the sources with Clover and verifying the test coverage value.
 *
 * @goal check
 * @phase verify
 * @execute phase="test" lifecycle="clover"
 *
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @version $Id$
 */
public class CloverCheckMojo extends AbstractCloverMojo
{
    /**
     * @parameter expression="${project.build.directory}/clover/clover.db"
     * @required
     */
    private String cloverDatabase;

    /**
     * @parameter default-value="70%"
     * @required
     */
    private String targetPercentage;

    public void execute()
        throws MojoExecutionException
    {
        AbstractCloverMojo.waitForFlush( this.waitForFlush, this.flushInterval );

        Project antProject = registerCloverAntTasks();

        getLog().info( "Checking for coverage of " + targetPercentage);

        CloverPassTask cloverPassTask = (CloverPassTask) antProject.createTask( "clover-check" );
        cloverPassTask.setInitString( this.cloverDatabase );
        cloverPassTask.setHaltOnFailure( true );
        cloverPassTask.setTarget( new Percentage( this.targetPercentage ) );
        cloverPassTask.setFailureProperty( "clovercheckproperty" );
        try
        {
            cloverPassTask.execute();
        }
        catch ( BuildException e )
        {
            getLog().error( antProject.getProperty( "clovercheckproperty" ) );
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

}
