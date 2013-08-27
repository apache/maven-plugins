package org.apache.maven.plugins.help;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.maven.BuildFailureException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.DefaultLifecycleExecutor;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Utility methods to play with Help Mojos.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class HelpUtil
{

    private HelpUtil()
    {
        // Utility classes should not have a public or default constructor.
    }

    /**
     * Invoke the following private method <code>
     * DefaultLifecycleExecutor#getMojoDescriptor(String, MavenSession, MavenProject, String, boolean, boolean)</code>
     *
     * @param task           not null
     * @param session        not null
     * @param project        not null
     * @param invokedVia     not null
     * @param canUsePrefix   not null
     * @param isOptionalMojo not null
     * @return MojoDescriptor for the task
     * @throws MojoFailureException   if can not invoke the method.
     * @throws MojoExecutionException if no descriptor was found for <code>task</code>.
     * @see DefaultLifecycleExecutor#getMojoDescriptor(String, MavenSession, MavenProject, String, boolean, boolean)
     */
    protected static MojoDescriptor getMojoDescriptor( String task, MavenSession session, MavenProject project,
                                                       String invokedVia, boolean canUsePrefix, boolean isOptionalMojo )
        throws MojoFailureException, MojoExecutionException
    {
        try
        {
            DefaultLifecycleExecutor lifecycleExecutor =
                (DefaultLifecycleExecutor) session.lookup( LifecycleExecutor.ROLE );

            Method m = lifecycleExecutor.getClass().getDeclaredMethod( "getMojoDescriptor",
                                                                       new Class[]{ String.class, MavenSession.class,
                                                                           MavenProject.class, String.class,
                                                                           Boolean.TYPE, Boolean.TYPE } );
            m.setAccessible( true );
            MojoDescriptor mojoDescriptor =
                (MojoDescriptor) m.invoke( lifecycleExecutor, task, session, project, invokedVia, canUsePrefix,
                                           isOptionalMojo );

            if ( mojoDescriptor == null )
            {
                throw new MojoExecutionException( "No MOJO exists for '" + task + "'." );
            }

            return mojoDescriptor;
        }
        catch ( SecurityException e )
        {
            throw new MojoFailureException( "SecurityException: " + e.getMessage() );
        }
        catch ( IllegalArgumentException e )
        {
            throw new MojoFailureException( "IllegalArgumentException: " + e.getMessage() );
        }
        catch ( ComponentLookupException e )
        {
            throw new MojoFailureException( "ComponentLookupException: " + e.getMessage() );
        }
        catch ( NoSuchMethodException e )
        {
            throw new MojoFailureException( "NoSuchMethodException: " + e.getMessage() );
        }
        catch ( IllegalAccessException e )
        {
            throw new MojoFailureException( "IllegalAccessException: " + e.getMessage() );
        }
        catch ( InvocationTargetException e )
        {
            Throwable cause = e.getCause();

            if ( cause instanceof BuildFailureException )
            {
                throw new MojoFailureException( "BuildFailureException: " + cause.getMessage() );
            }
            else if ( cause instanceof LifecycleExecutionException )
            {
                throw new MojoFailureException( "LifecycleExecutionException: " + cause.getMessage() );
            }
            else if ( cause instanceof PluginNotFoundException )
            {
                throw new MojoFailureException( "PluginNotFoundException: " + cause.getMessage() );
            }

            StringWriter s = new StringWriter();
            PrintWriter writer = new PrintWriter( s );
            e.printStackTrace( writer );

            throw new MojoFailureException( "InvocationTargetException: " + e.getMessage() + "\n" + s.toString() );
        }
    }
}
