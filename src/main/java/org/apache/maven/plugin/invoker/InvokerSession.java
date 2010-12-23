package org.apache.maven.plugin.invoker;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.invoker.model.BuildJob;
import org.apache.maven.plugin.logging.Log;

/**
 * Tracks a set of build jobs and their results.
 * 
 * @author Benjamin Bentmann
 */
class InvokerSession
{

    private List<BuildJob> buildJobs;

    private List<BuildJob> failedJobs;

    private List<BuildJob> errorJobs;

    private List<BuildJob> successfulJobs;

    private List<BuildJob> skippedJobs;

    /**
     * Creates a new empty session.
     */
    public InvokerSession()
    {
        buildJobs = new ArrayList<BuildJob>();
    }

    /**
     * Creates a session that initially contains the specified build jobs.
     * 
     * @param buildJobs The build jobs to set, must not be <code>null</code>.
     */
    public InvokerSession( BuildJob[] buildJobs )
    {
        this.buildJobs = new ArrayList<BuildJob>( Arrays.asList( buildJobs ) );
    }

    /**
     * Adds the specified build job to this session.
     * 
     * @param buildJob The build job to add, must not be <code>null</code>.
     */
    public void addJob( BuildJob buildJob )
    {
        buildJobs.add( buildJob );

        resetStats();
    }

    /**
     * Sets the build jobs of this session.
     * 
     * @param buildJobs The build jobs to set, must not be <code>null</code>.
     */
    public void setJobs( List<? extends BuildJob> buildJobs )
    {
        this.buildJobs = new ArrayList<BuildJob>( buildJobs );

        resetStats();
    }

    /**
     * Gets the build jobs in this session.
     * 
     * @return The build jobs in this session, can be empty but never <code>null</code>.
     */
    public List<BuildJob> getJobs()
    {
        return buildJobs;
    }

    /**
     * Gets the successful build jobs in this session.
     * 
     * @return The successful build jobs in this session, can be empty but never <code>null</code>.
     */
    public List<BuildJob> getSuccessfulJobs()
    {
        updateStats();

        return successfulJobs;
    }

    /**
     * Gets the failed build jobs in this session.
     * 
     * @return The failed build jobs in this session, can be empty but never <code>null</code>.
     */
    public List<BuildJob> getFailedJobs()
    {
        updateStats();

        return failedJobs;
    }

    /**
     * Gets the build jobs which had errors for this session.
     *
     * @return The build jobs in error for this session, can be empty but never <code>null</code>.
     */
    public List<BuildJob> getErrorJobs()
    {
        updateStats();

        return errorJobs;
    } 

    /**
     * Gets the skipped build jobs in this session.
     * 
     * @return The skipped build jobs in this session, can be empty but never <code>null</code>.
     */
    public List<BuildJob> getSkippedJobs()
    {
        updateStats();

        return skippedJobs;
    }

    private void resetStats()
    {
        successfulJobs = null;
        failedJobs = null;
        skippedJobs = null;
        errorJobs = null;
    }

    private void updateStats()
    {
        if ( successfulJobs != null && skippedJobs != null && failedJobs != null && errorJobs != null )
        {
            return;
        }

        successfulJobs = new ArrayList<BuildJob>();
        failedJobs = new ArrayList<BuildJob>();
        skippedJobs = new ArrayList<BuildJob>();
        errorJobs = new ArrayList<BuildJob>();

        for ( BuildJob buildJob : buildJobs )
        {
            if ( BuildJob.Result.SUCCESS.equals( buildJob.getResult() ) )
            {
                successfulJobs.add( buildJob );
            }
            else if ( BuildJob.Result.SKIPPED.equals( buildJob.getResult() ) )
            {
                skippedJobs.add( buildJob );
            }
            else if ( BuildJob.Result.ERROR.equals( buildJob.getResult() ) )
            {
                errorJobs.add( buildJob );
            }
            else if ( buildJob.getResult() != null )
            {
                failedJobs.add( buildJob );
            }
        }
    }

    /**
     * Prints a summary of this session to the specified logger.
     * 
     * @param logger The mojo logger to output messages to, must not be <code>null</code>.
     * @param ignoreFailures A flag whether failures should be ignored or whether a build failure should be signaled.
     */
    public void logSummary( Log logger, boolean ignoreFailures )
    {
        updateStats();

        String separator = "-------------------------------------------------";

        logger.info( separator );
        logger.info( "Build Summary:" );
        logger.info( "  Passed: " + successfulJobs.size() + ", Failed: " + failedJobs.size() + ", Errors: "
            + errorJobs.size() + ", Skipped: " + skippedJobs.size() );
        logger.info( separator );

        if ( !failedJobs.isEmpty() )
        {
            String heading = "The following builds failed:";
            if ( ignoreFailures )
            {
                logger.warn( heading );
            }
            else
            {
                logger.error( heading );
            }

            for ( BuildJob buildJob : failedJobs )
            {
                String item = "*  " + buildJob.getProject();
                if ( ignoreFailures )
                {
                    logger.warn( item );
                }
                else
                {
                    logger.error( item );
                }
            }

            logger.info( separator );
        }
    }

    /**
     * Handles the build failures in this session.
     * 
     * @param logger The mojo logger to output messages to, must not be <code>null</code>.
     * @param ignoreFailures A flag whether failures should be ignored or whether a build failure should be signaled.
     * @throws MojoFailureException If failures are present and not ignored.
     */
    public void handleFailures( Log logger, boolean ignoreFailures )
        throws MojoFailureException
    {
        updateStats();

        if ( !failedJobs.isEmpty() )
        {
            String message = failedJobs.size() + " build" + ( failedJobs.size() == 1 ? "" : "s" ) + " failed.";

            if ( ignoreFailures )
            {
                logger.warn( "Ignoring that " + message );
            }
            else
            {
                throw new MojoFailureException( message + " See console output above for details." );
            }
        }

        if ( !errorJobs.isEmpty() )
        {
            String message = errorJobs.size() + " build" + ( errorJobs.size() == 1 ? "" : "s" ) + " in error.";

            if ( ignoreFailures )
            {
                logger.warn( "Ignoring that " + message );
            }
            else
            {
                throw new MojoFailureException( message + " See console output above for details." );
            }
        }
    }

}
