package org.apache.maven.plugins.assembly.archive.task;

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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Creates project building requests, regardless of aether type.
 *
 * @author Kristian Rosenvold
 */
public class ProjectBuildingRequestCreator
{

    static final Class AETHER = tryLoadClass( "org.sonatype.aether.RepositorySystemSession" );

    static final Class ECLIPSE = tryLoadClass( "org.eclipse.aether.RepositorySystemSession" );

    public static ProjectBuildingRequest create( MavenSession session )
    {
        ProjectBuildingRequest projectBuildingRequest1 = new DefaultProjectBuildingRequest();

        Object repositorySession = invoke( session, "getRepositorySession" );
        Method setRepositorySession =
            getMethod( projectBuildingRequest1.getClass(), "setRepositorySession", ECLIPSE != null ? ECLIPSE : AETHER );
        invoke( setRepositorySession, projectBuildingRequest1, repositorySession );
        injectSession( projectBuildingRequest1, session );
        return projectBuildingRequest1;
    }

    private static void injectSession( ProjectBuildingRequest request, MavenSession session )
    {
        request.setSystemProperties( session.getSystemProperties() );
        if ( request.getUserProperties().isEmpty() )
        {
            request.setUserProperties( session.getUserProperties() );
        }
    }

    private static Class tryLoadClass( String name )
    {
        try
        {
            return Class.forName( name );
        }
        catch ( ClassNotFoundException e )
        {
            return null;
        }
    }

    public static Object invoke( Object object, String method )
    {
        try
        {
            return object.getClass().getMethod( method ).invoke( object );
        }
        catch ( IllegalAccessException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
        catch ( InvocationTargetException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    public static Method getMethod( Class<?> objectClazz, String method, Class<?>... params )
    {
        try
        {
            return objectClazz.getMethod( method, params );
        }
        catch ( NoSuchMethodException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    public static Object invoke( Method method, Object object, Object... args )
    {
        try
        {
            return method.invoke( object, args );
        }
        catch ( IllegalAccessException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
        catch ( InvocationTargetException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
    }
}
