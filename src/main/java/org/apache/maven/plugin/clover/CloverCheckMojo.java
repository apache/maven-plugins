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
import org.apache.maven.plugin.clover.internal.AbstractCloverMojo;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Verify test percentage coverage from an existing Clover database and fail the build if it is below the defined
 * threshold.
 *
 * @goal check
 * @phase verify
 *
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @version $Id$
 */
public class CloverCheckMojo extends AbstractCloverMojo
{
    /**
     * @parameter default-value="70%"
     * @required
     */
    private String targetPercentage;

    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.clover.internal.AbstractCloverMojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( !isInCloverForkedLifecycle() )
        {
            if ( areCloverDatabasesAvailable() )
            {
                super.execute();

                AbstractCloverMojo.waitForFlush( getWaitForFlush(), getFlushInterval() );

                check();
            }
            else
            {
                getLog().info("No Clover database found, skipping test coverage verification");
            }
        }
    }

    private void check() throws MojoExecutionException
    {
        Project antProject = registerCloverAntTasks();

        getLog().info( "Checking for coverage of " + targetPercentage);

        CloverPassTask cloverPassTask = (CloverPassTask) antProject.createTask( "clover-check" );
        cloverPassTask.setInitString( getCloverDatabase() );
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

    private boolean isInCloverForkedLifecycle()
    {
        // We know we're in the forked lifecycle if the output directory is set to target/clover...
        // TODO: Not perfect, need to find a better way. This is a hack!
        return getProject().getBuild().getDirectory().endsWith( "clover" );
    }
}
