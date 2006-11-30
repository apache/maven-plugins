package org.apache.maven.plugin.dependency.testUtils;
/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.handler.manager.DefaultArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.SilentLog;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.codehaus.plexus.util.ReflectionUtils;

public class DependencyTestUtils
{

    /**
     * Deletes a directory and its contents.
     * 
     * @param dir
     *            The base directory of the included and excluded files.
     * @throws IOException
     * @throws MojoExecutionException
     *             When a directory failed to get deleted.
     */
    static public void removeDirectory( File dir )
        throws IOException
    {
        if ( dir != null )
        {
            FileSetManager fileSetManager = new FileSetManager( new SilentLog(), false );

            FileSet fs = new FileSet();
            fs.setDirectory( dir.getPath() );
            fs.addInclude( "**/**" );
            fileSetManager.delete( fs );

        }
    }

    static public ArtifactFactory getArtifactFactory()
        throws IllegalAccessException
    {
        ArtifactFactory artifactFactory;
        ArtifactHandlerManager manager = new DefaultArtifactHandlerManager();
        setVariableValueToObject( manager, "artifactHandlers", new HashMap() );

        artifactFactory = new DefaultArtifactFactory();
        setVariableValueToObject( artifactFactory, "artifactHandlerManager", manager );

        return artifactFactory;
    }

    /**
     * convience method to set values to variables in objects that don't have
     * setters
     * 
     * @param object
     * @param variable
     * @param value
     * @throws IllegalAccessException
     */
    public static void setVariableValueToObject( Object object, String variable, Object value )
        throws IllegalAccessException
    {
        Field field = ReflectionUtils.getFieldByNameIncludingSuperclasses( variable, object.getClass() );

        field.setAccessible( true );

        field.set( object, value );
    }

}
