package org.apache.maven.plugin.resources.stub;

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

import java.io.File;

import org.apache.maven.model.Resource;

public class MavenProjectResourcesStub
    extends MavenProjectBuildStub
{
    
    private File baseDir;

    public MavenProjectResourcesStub( String id )
        throws Exception
    {
        super( id );
        setupResources();
        setupTestResources();
    }

    public void addInclude( String pattern )
    {
        ( (Resource) build.getResources().get( 0 ) ).addInclude( pattern );
    }

    public void addExclude( String pattern )
    {
        ( (Resource) build.getResources().get( 0 ) ).addExclude( pattern );
    }

    public void addTestInclude( String pattern )
    {
        ( (Resource) build.getTestResources().get( 0 ) ).addInclude( pattern );
    }

    public void addTestExclude( String pattern )
    {
        ( (Resource) build.getTestResources().get( 0 ) ).addExclude( pattern );
    }

    public void setTargetPath( String path )
    {
        ( (Resource) build.getResources().get( 0 ) ).setTargetPath( path );
    }

    public void setTestTargetPath( String path )
    {
        ( (Resource) build.getTestResources().get( 0 ) ).setTargetPath( path );
    }

    public void setDirectory( String dir )
    {
        ( (Resource) build.getResources().get( 0 ) ).setDirectory( dir );
    }

    public void setTestDirectory( String dir )
    {
        ( (Resource) build.getTestResources().get( 0 ) ).setDirectory( dir );
    }

    public void setResourceFiltering( int nIndex, boolean filter )
    {
        if ( build.getResources().size() > nIndex )
        {
            ( (Resource) build.getResources().get( nIndex ) ).setFiltering( filter );
        }
    }

    private void setupResources()
    {
        Resource resource = new Resource();

        // see MavenProjectBasicStub for details
        // of getBasedir

        // setup default resources
        resource.setDirectory( getBasedir().getPath() + "/src/main/resources" );
        resource.setFiltering( false );
        resource.setTargetPath( null );
        build.addResource( resource );
    }

    private void setupTestResources()
    {
        Resource resource = new Resource();

        // see MavenProjectBasicStub for details
        // of getBasedir

        // setup default test resources
        resource.setDirectory( getBasedir().getPath() + "/src/test/resources" );
        resource.setFiltering( false );
        resource.setTargetPath( null );
        build.addTestResource( resource );
    }

    public File getBaseDir()
    {
        return baseDir == null ? super.getBasedir() : baseDir;
    }

    public void setBaseDir( File baseDir )
    {
        this.baseDir = baseDir;
    }
}
