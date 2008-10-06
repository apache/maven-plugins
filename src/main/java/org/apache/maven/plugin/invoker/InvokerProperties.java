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
     * Determines whether these invoker properties contain a build definition for the specified invocation index.
     * 
     * @param index The one-based index of the invocation to check for, must not be negative.
     * @return <code>true</code> if the invocation with the specified index is defined, <code>false</code> otherwise.
     */
    public boolean isInvocationDefined( int index )
    {
        return properties.getProperty( "invoker.goals." + index ) != null;
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
        String goals = get( "invoker.goals", index );
        if ( goals != null )
        {
            request.setGoals( new ArrayList( Arrays.asList( StringUtils.split( goals, ", \t\n\r\f" ) ) ) );
        }

        String profiles = get( "invoker.profiles", index );
        if ( profiles != null )
        {
            request.setProfiles( new ArrayList( Arrays.asList( StringUtils.split( profiles, ", \t\n\r\f" ) ) ) );
        }

        String mvnOpts = get( "invoker.mavenOpts", index );
        if ( mvnOpts != null )
        {
            request.setMavenOpts( mvnOpts );
        }

        String failureBehavior = get( "invoker.failureBehavior", index );
        if ( failureBehavior != null )
        {
            request.setFailureBehavior( failureBehavior );
        }

        String nonRecursive = get( "invoker.nonRecursive", index );
        if ( nonRecursive != null )
        {
            request.setRecursive( !Boolean.valueOf( nonRecursive ).booleanValue() );
        }

        String offline = get( "invoker.offline", index );
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
