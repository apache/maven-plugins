package org.apache.maven.plugin.toolchain;

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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.ToolchainManagerPrivate;
import org.apache.maven.toolchain.ToolchainPrivate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author mkleint
 */
@Mojo( name = "toolchain", defaultPhase = LifecyclePhase.VALIDATE )
public class ToolchainMojo
    extends AbstractMojo
{

    /**
     */
    @Component
    private ToolchainManagerPrivate toolchainManager;

    /**
     * The current build session instance. This is used for
     * toolchain manager API calls.
     */
    @Component
    private MavenSession session;

    /**
     */
    @Parameter( required = true )
    private Toolchains toolchains;

    public ToolchainMojo()
    {
    }

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( toolchains != null )
        {
            Iterator en = toolchains.getToolchainsTypes().iterator();
            List nonMatchedTypes = new ArrayList();
            while ( en.hasNext() )
            {
                try
                {
                    String type = (String) en.next();
                    getLog().info( "Type:" + type );
                    Map params = toolchains.getParams( type );
                    ToolchainPrivate[] tcs = getToolchains( type );
                    boolean matched = false;
                    for ( int i = 0; i < tcs.length; i++ )
                    {
                        if ( tcs[i].matchesRequirements( params ) )
                        {
                            getLog().info( "Toolchain (" + type + ") matched:" + tcs[i] );
                            toolchainManager.storeToolchainToBuildContext( tcs[i], session );
                            matched = true;
                            break;
                        }
                    }
                    if ( !matched )
                    {
                        nonMatchedTypes.add( type );
                    }
                }
                catch ( MisconfiguredToolchainException ex )
                {
                    throw new MojoExecutionException( "Misconfigured toolchains.", ex );
                }
            }
            if ( !nonMatchedTypes.isEmpty() )
            {
                //TODO add the default toolchain instance if defined??
                StringBuffer buff = new StringBuffer();
                buff.append( "Cannot find matching toolchain definitions for the following toolchain types:" );
                Iterator it = nonMatchedTypes.iterator();
                while ( it.hasNext() )
                {
                    String type = (String) it.next();
                    buff.append( '\n' );
                    buff.append( type );
                    Map params = toolchains.getParams( type );
                    if ( params.size() > 0 )
                    {
                        Iterator it2 = params.keySet().iterator();
                        buff.append( " [" );
                        while ( it2.hasNext() )
                        {
                            String string = (String) it2.next();
                            buff.append( " " + string + "='" + params.get( string ) + "' " );
                        }
                        buff.append( ']' );
                    }
                }
                getLog().error( buff.toString() );
                throw new MojoFailureException( buff.toString()
                    + "\nPlease make sure you define the required toolchains in your ~/.m2/toolchains.xml file." );
            }
        }
        else
        {
            //can that happen?
        }
    }

    private ToolchainPrivate[] getToolchains( String type )
        throws MojoExecutionException, MisconfiguredToolchainException
    {
        Class managerClass = toolchainManager.getClass();

        try
        {
            try
            {
                // try 3.x style API
                Method newMethod =
                    managerClass.getMethod( "getToolchainsForType", new Class[] { String.class, MavenSession.class } );

                return (ToolchainPrivate[]) newMethod.invoke( toolchainManager, new Object[] { type, session } );
            }
            catch ( NoSuchMethodException e )
            {
                // try 2.x style API
                Method oldMethod = managerClass.getMethod( "getToolchainsForType", new Class[] { String.class } );

                return (ToolchainPrivate[]) oldMethod.invoke( toolchainManager, new Object[] { type } );
            }
        }
        catch ( NoSuchMethodException e )
        {
            throw new MojoExecutionException( "Incompatible toolchain API", e );
        }
        catch ( IllegalAccessException e )
        {
            throw new MojoExecutionException( "Incompatible toolchain API", e );
        }
        catch ( InvocationTargetException e )
        {
            Throwable cause = e.getCause();

            if ( cause instanceof RuntimeException )
            {
                throw (RuntimeException) cause;
            }
            if ( cause instanceof MisconfiguredToolchainException )
            {
                throw (MisconfiguredToolchainException) cause;
            }

            throw new MojoExecutionException( "Incompatible toolchain API", e );
        }
    }

}
