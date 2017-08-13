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
import java.util.List;
import java.util.Map;

/**
 * Check that toolchains requirements are met by currently configured toolchains and
 * store the selected toolchains in build context for later retrieval by other plugins.
 *
 * @author mkleint
 */
@Mojo( name = "toolchain", defaultPhase = LifecyclePhase.VALIDATE,
       configurator = "toolchains-requirement-configurator" )
public class ToolchainMojo
    extends AbstractMojo
{

    /**
     */
    @Component
    private ToolchainManagerPrivate toolchainManagerPrivate;

    /**
     * The current build session instance. This is used for toolchain manager API calls.
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    /**
     * Toolchains requirements, specified by one
     * <pre>  &lt;toolchain-type&gt;
     *    &lt;param&gt;expected value&lt;/param&gt;
     *    ...
     *  &lt;/toolchain-type&gt;</pre>
     * element for each required toolchain.
     */
    @Parameter( required = true )
    private ToolchainsRequirement toolchains;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( toolchains == null )
        {
            // should not happen since parameter is required...
            getLog().warn( "No toolchains requirements configured." );
            return;
        }

        List<String> nonMatchedTypes = new ArrayList<String>();

        for ( Map.Entry<String, Map<String, String>> entry : toolchains.getToolchains().entrySet() )
        {
            String type = entry.getKey();

            if ( !selectToolchain( type, entry.getValue() ) )
            {
                nonMatchedTypes.add( type );
            }
        }

        if ( !nonMatchedTypes.isEmpty() )
        {
            // TODO add the default toolchain instance if defined??
            StringBuilder buff = new StringBuilder();
            buff.append( "Cannot find matching toolchain definitions for the following toolchain types:" );

            for ( String type : nonMatchedTypes )
            {
                buff.append( '\n' );
                buff.append( getToolchainRequirementAsString( type, toolchains.getParams( type ) ) );
            }

            getLog().error( buff.toString() );

            throw new MojoFailureException( buff.toString()
                + "\nPlease make sure you define the required toolchains in your ~/.m2/toolchains.xml file." );
        }
    }

    protected String getToolchainRequirementAsString( String type, Map<String, String> params )
    {
        StringBuilder buff = new StringBuilder();

        buff.append( type ).append( " [" );

        if ( params.size() == 0 )
        {
            buff.append( " any" );
        }
        else
        {
            for ( Map.Entry<String, String> param : params.entrySet() )
            {
                buff.append( " " ).append( param.getKey() ).append( "='" ).append( param.getValue() );
                buff.append( "'" );
            }
        }

        buff.append( " ]" );

        return buff.toString();
    }

    protected boolean selectToolchain( String type, Map<String, String> params )
        throws MojoExecutionException
    {
        getLog().info( "Required toolchain: " + getToolchainRequirementAsString( type, params ) );
        int typeFound = 0;

        try
        {
            ToolchainPrivate[] tcs = getToolchains( type );

            for ( ToolchainPrivate tc : tcs )
            {
                if ( !type.equals( tc.getType() ) )
                {
                    // useful because of MNG-5716
                    continue;
                }

                typeFound++;

                if ( tc.matchesRequirements( params ) )
                {
                    getLog().info( "Found matching toolchain for type " + type + ": " + tc );

                    // store matching toolchain to build context
                    toolchainManagerPrivate.storeToolchainToBuildContext( tc, session );

                    return true;
                }
            }
        }
        catch ( MisconfiguredToolchainException ex )
        {
            throw new MojoExecutionException( "Misconfigured toolchains.", ex );
        }

        getLog().error( "No toolchain " + ( ( typeFound == 0 ) ? "found" : ( "matched from " + typeFound + " found" ) )
                            + " for type " + type );

        return false;
    }

    private ToolchainPrivate[] getToolchains( String type )
        throws MojoExecutionException, MisconfiguredToolchainException
    {
        Class<?> managerClass = toolchainManagerPrivate.getClass();

        try
        {
            try
            {
                // try 3.x style API
                Method newMethod =
                    managerClass.getMethod( "getToolchainsForType", new Class[] { String.class, MavenSession.class } );

                return (ToolchainPrivate[]) newMethod.invoke( toolchainManagerPrivate, type, session );
            }
            catch ( NoSuchMethodException e )
            {
                // try 2.x style API
                Method oldMethod = managerClass.getMethod( "getToolchainsForType", new Class[] { String.class } );

                return (ToolchainPrivate[]) oldMethod.invoke( toolchainManagerPrivate, type );
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
