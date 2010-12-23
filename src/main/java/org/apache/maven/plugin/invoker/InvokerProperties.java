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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.shared.invoker.InvocationRequest;
import org.codehaus.plexus.util.StringUtils;

/**
 * Provides a convenient facade around the <code>invoker.properties</code>.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
class InvokerProperties
{

    /**
     * The invoker properties being wrapped, never <code>null</code>.
     */
    private final Properties properties;

    /**
     * The constant for the invoker property.
     */
    private static final String PROJECT = "invoker.project";

    /**
     * The constant for the invoker property.
     */
    private static final String GOALS = "invoker.goals";

    /**
     * The constant for the invoker property.
     */
    private static final String PROFILES = "invoker.profiles";

    /**
     * The constant for the invoker property.
     */
    private static final String MAVEN_OPTS = "invoker.mavenOpts";

    /**
     * The constant for the invoker property.
     */
    private static final String FAILURE_BEHAVIOR = "invoker.failureBehavior";

    /**
     * The constant for the invoker property.
     */
    private static final String NON_RECURSIVE = "invoker.nonRecursive";

    /**
     * The constant for the invoker property.
     */
    private static final String OFFLINE = "invoker.offline";

    /**
     * The constant for the invoker property.
     */
    private static final String SYSTEM_PROPERTIES_FILE = "invoker.systemPropertiesFile";

    /**
     * Creates a new facade for the specified invoker properties. The properties will not be copied, so any changes to
     * them will be reflected by the facade.
     * 
     * @param properties The invoker properties to wrap, may be <code>null</code> if none.
     */
    public InvokerProperties( Properties properties )
    {
        this.properties = ( properties != null ) ? properties : new Properties();
    }

    /**
     * Gets the invoker properties being wrapped.
     * 
     * @return The invoker properties being wrapped, never <code>null</code>.
     */
    public Properties getProperties()
    {
        return this.properties;
    }

    /**
     * Gets the name of the corresponding build job.
     * 
     * @return The name of the build job or an empty string if not set.
     */
    public String getJobName()
    {
        return this.properties.getProperty( "invoker.name", "" );
    }

    /**
     * Gets the description of the corresponding build job.
     * 
     * @return The description of the build job or an empty string if not set.
     */
    public String getJobDescription()
    {
        return this.properties.getProperty( "invoker.description", "" );
    }

    /**
     * Gets the specification of JRE versions on which this build job should be run.
     * 
     * @return The specification of JRE versions or an empty string if not set.
     */
    public String getJreVersion()
    {
        return this.properties.getProperty( "invoker.java.version", "" );
    }

    /**
     * Gets the specification of Maven versions on which this build job should be run.
     *
     * @return The specification of Maven versions on which this build job should be run.
     * @since 1.5
     */
    public String getMavenVersion()
    {
        return this.properties.getProperty( "invoker.maven.version", "" );
    }

    /**
     * Gets the specification of OS families on which this build job should be run.
     * 
     * @return The specification of OS families or an empty string if not set.
     */
    public String getOsFamily()
    {
        return this.properties.getProperty( "invoker.os.family", "" );
    }

    /**
     * Determines whether these invoker properties contain a build definition for the specified invocation index.
     * 
     * @param index The one-based index of the invocation to check for, must not be negative.
     * @return <code>true</code> if the invocation with the specified index is defined, <code>false</code> otherwise.
     */
    public boolean isInvocationDefined( int index )
    {
        String[] keys =
            { PROJECT, GOALS, PROFILES, MAVEN_OPTS, FAILURE_BEHAVIOR, NON_RECURSIVE, OFFLINE, SYSTEM_PROPERTIES_FILE };
        for ( int i = 0; i < keys.length; i++ )
        {
            if ( properties.getProperty( keys[i] + '.' + index ) != null )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Configures the specified invocation request from these invoker properties. Settings not present in the invoker
     * properties will be left unchanged in the invocation request.
     * 
     * @param request The invocation request to configure, must not be <code>null</code>.
     * @param index The one-based index of the invocation to configure, must not be negative.
     */
    public void configureInvocation( InvocationRequest request, int index )
    {
        String project = get( PROJECT, index );
        if ( project != null )
        {
            File file = new File( request.getBaseDirectory(), project );
            if ( file.isFile() )
            {
                request.setBaseDirectory( file.getParentFile() );
                request.setPomFile( file );
            }
            else
            {
                request.setBaseDirectory( file );
                request.setPomFile( null );
            }
        }

        String goals = get( GOALS, index );
        if ( goals != null )
        {
            request.setGoals( new ArrayList<String>( Arrays.asList( StringUtils.split( goals, ", \t\n\r\f" ) ) ) );
        }

        String profiles = get( PROFILES, index );
        if ( profiles != null )
        {
            request.setProfiles( new ArrayList<String>( Arrays.asList( StringUtils.split( profiles, ", \t\n\r\f" ) ) ) );
        }

        String mvnOpts = get( MAVEN_OPTS, index );
        if ( mvnOpts != null )
        {
            request.setMavenOpts( mvnOpts );
        }

        String failureBehavior = get( FAILURE_BEHAVIOR, index );
        if ( failureBehavior != null )
        {
            request.setFailureBehavior( failureBehavior );
        }

        String nonRecursive = get( NON_RECURSIVE, index );
        if ( nonRecursive != null )
        {
            request.setRecursive( !Boolean.valueOf( nonRecursive ).booleanValue() );
        }

        String offline = get( OFFLINE, index );
        if ( offline != null )
        {
            request.setOffline( Boolean.valueOf( offline ).booleanValue() );
        }
    }

    /**
     * Checks whether the specified exit code matches the one expected for the given invocation.
     * 
     * @param exitCode The exit code of the Maven invocation to check.
     * @param index The index of the invocation for which to check the exit code, must not be negative.
     * @return <code>true</code> if the exit code is zero and a success was expected or if the exit code is non-zero and
     *         a failue was expected, <code>false</code> otherwise.
     */
    public boolean isExpectedResult( int exitCode, int index )
    {
        boolean nonZeroExit = "failure".equalsIgnoreCase( get( "invoker.buildResult", index ) );
        return ( exitCode != 0 ) == nonZeroExit;
    }

    /**
     * Gets the path to the properties file used to set the system properties for the specified execution.
     * 
     * @param index The index of the invocation for which to check the exit code, must not be negative.
     * @return The path to the properties file or <code>null</code> if not set.
     */
    public String getSystemPropertiesFile( int index )
    {
        return get( SYSTEM_PROPERTIES_FILE, index );
    }

    /**
     * Gets a value from the invoker properties. The invoker properties are intended to describe the invocation settings
     * for multiple builds of the same project. For this reason, the properties are indexed. First, a property named
     * <code>key.index</code> will be queried. If this property does not exist, the value of the property named
     * <code>key</code> will finally be returned.
     * 
     * @param key The (base) key for the invoker property to lookup, must not be <code>null</code>.
     * @param index The index of the invocation for which to retrieve the value, must not be negative.
     * @return The value for the requested invoker property or <code>null</code> if not defined.
     */
    String get( String key, int index )
    {
        if ( index < 0 )
        {
            throw new IllegalArgumentException( "invalid invocation index: " + index );
        }

        String value = properties.getProperty( key + '.' + index );
        if ( value == null )
        {
            value = properties.getProperty( key );
        }
        return value;
    }

}
