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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.invoker.model.io.xpp3.BuildJobXpp3Reader;
import org.apache.maven.plugin.invoker.model.BuildJob;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * Checks the results of maven-invoker-plugin based integration tests and fails the build if any tests failed.
 *
 * @goal verify
 * @phase verify
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @since 1.4
 */
public class VerifyMojo extends AbstractMojo
{
    /**
     * Flag used to suppress certain invocations. This is useful in tailoring the build using profiles.
     *
     * @parameter expression="${invoker.skip}" default-value="false"
     * @since 1.1
     */
    private boolean skipInvocation;

    /**
     * Base directory where all build reports are written to.
     *
     * @parameter expression="${invoker.reportsDirectory}" default-value="${project.build.directory}/invoker-reports"
     * @since 1.4
     */
    private File reportsDirectory;

    /**
     * A flag controlling whether failures of the sub builds should fail the main build, too. If set to
     * <code>true</code>, the main build will proceed even if one or more sub builds failed.
     *
     * @parameter expression="${maven.test.failure.ignore}" default-value="false"
     * @since 1.3
     */
    private boolean ignoreFailures;

    /**
     * Flag used to suppress the summary output notifying of successes and failures. If set to <code>true</code>, the
     * only indication of the build's success or failure will be the effect it has on the main build (if it fails, the
     * main build should fail as well).
     *
     * @parameter default-value="false"
     */
    private boolean suppressSummaries;

    /**
     * Invokes Maven on the configured test projects.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException If the goal encountered severe errors.
     * @throws org.apache.maven.plugin.MojoFailureException If any of the Maven builds failed.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skipInvocation )
        {
            getLog().info( "Skipping invocation per configuration."
                + " If this is incorrect, ensure the skipInvocation parameter is not set to true." );
            return;
        }
        File[] reportFiles = getReportFiles();
        if ( reportFiles.length <= 0 )
        {
            getLog().info( "No invoker report files found, nothing to check." );
            return;
        }

        List buildJobs = new ArrayList( reportFiles.length );
        for ( int i = 0, size = reportFiles.length; i < size; i++ )
        {
            File reportFile = reportFiles[i];
            try
            {
                BuildJobXpp3Reader reader = new BuildJobXpp3Reader();
                buildJobs.add( reader.read( ReaderFactory.newXmlReader( reportFile ) ) );
            }
            catch ( XmlPullParserException e )
            {
                throw new MojoExecutionException( "Failed to parse report file: " + reportFile, e );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to read report file: " + reportFile, e );
            }
        }

        List failures = new ArrayList();
        List successes = new ArrayList();

        for ( Iterator iterator = buildJobs.iterator(); iterator.hasNext();)
        {
            BuildJob buildJob = (BuildJob) iterator.next();
            if ( BuildJob.Result.SUCCESS.equals( buildJob.getResult() ) )
            {
                successes.add( buildJob );
            }
            else if ( !BuildJob.Result.SKIPPED.equals( buildJob.getResult() ) )
            {
                failures.add( buildJob );
            }
        }

        if ( !suppressSummaries )
        {
            getLog().info( "---------------------------------------" );
            getLog().info( "Execution Summary:" );
            getLog().info( "  Builds Passing: " + ( buildJobs.size() - failures.size() ) );
            getLog().info( "  Builds Failing: " + failures.size() );
            getLog().info( "---------------------------------------" );

            if ( !failures.isEmpty() )
            {
                String heading = "The following builds failed:";
                if ( ignoreFailures )
                {
                    getLog().warn( heading );
                }
                else
                {
                    getLog().error( heading );
                }

                for ( final Iterator it = failures.iterator(); it.hasNext(); )
                {
                    BuildJob buildJob = (BuildJob) it.next();
                    String item = "*  " + buildJob.getProject();
                    if ( ignoreFailures )
                    {
                        getLog().warn( item );
                    }
                    else
                    {
                        getLog().error( item );
                    }
                }

                getLog().info( "---------------------------------------" );
            }
        }

        if ( !failures.isEmpty() )
        {
            String message = failures.size() + " build" + ( failures.size() == 1 ? "" : "s" ) + " failed.";

            if ( ignoreFailures )
            {
                getLog().warn( "Ignoring that " + message );
            }
            else
            {
                throw new MojoFailureException( this, message, message );
            }
        }

    }

    /**
     * Gets the paths to the available invoker reports to generate the site output from.
     *
     * @return The paths to the invoker reports, can be empty but never <code>null</code>.
     */
    private File[] getReportFiles()
    {
        File[] reportFiles = ( reportsDirectory != null ) ? reportsDirectory.listFiles() : null;
        if ( reportFiles == null )
        {
            reportFiles = new File[0];
        }
        return reportFiles;
    }

}
