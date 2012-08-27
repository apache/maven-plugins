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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.maven.model.Build;
import org.codehaus.plexus.util.FileUtils;

public class MavenProjectBuildStub
    extends MavenProjectBasicStub
{
    protected Build build;

    protected String srcDirectory;

    protected String targetDirectory;

    protected String buildDirectory;

    protected String outputDirectory;

    protected String testOutputDirectory;

    protected String resourcesDirectory;

    protected String testResourcesDirectory;

    protected String targetResourceDirectory;

    protected String targetTestResourcesDirectory;

    protected ArrayList<String> fileList;

    protected ArrayList<String> directoryList;

    protected HashMap<String, String> dataMap;

    public MavenProjectBuildStub( String key )
        throws Exception
    {
        super( key );

        build = new Build();
        fileList = new ArrayList<String>();
        directoryList = new ArrayList<String>();
        dataMap = new HashMap<String, String>();
        setupBuild();
    }

    public void addDirectory( String name )
    {
        if ( isValidPath( name ) )
        {
            directoryList.add( name );
        }
    }

    public void setOutputDirectory( String dir )
    {
        outputDirectory = buildDirectory + "/" + dir;
        build.setOutputDirectory( outputDirectory );
    }

    public void addFile( String name )
    {
        if ( isValidPath( name ) )
        {
            fileList.add( name );
        }
    }

    public void addFile( String name, String data )
    {
        File fileName = new File( name );

        addFile( name );
        dataMap.put( fileName.getName(), data );
    }

    public String getOutputDirectory()
    {
        return outputDirectory;
    }

    public String getTestOutputDirectory()
    {
        return testOutputDirectory;
    }

    public String getResourcesDirectory()
    {
        return resourcesDirectory;
    }

    public String getTestResourcesDirectory()
    {
        return testResourcesDirectory;
    }

    public Build getBuild()
    {
        return build;
    }

    /**
     * returns true if the path is relative
     * and false if absolute
     * also returns false if it is relative to
     * the parent
     *
     * @param path
     * @return
     */
    private boolean isValidPath( String path )
    {
        boolean bRetVal = true;

        if ( path.startsWith( "c:" ) || path.startsWith( ".." ) || path.startsWith( "/" ) || path.startsWith( "\\" ) )
        {
            bRetVal = false;
        }

        return bRetVal;
    }

    private void setupBuild()
    {
        // check getBasedir method for the exact path
        // we need to recreate the dir structure in
        // an isolated environment
        srcDirectory = testRootDir + "/src";
        buildDirectory = testRootDir + "/target";
        outputDirectory = buildDirectory + "/classes";
        testOutputDirectory = buildDirectory + "/test-classes";
        resourcesDirectory = srcDirectory + "/main/resources/";
        testResourcesDirectory = srcDirectory + "/test/resources/";

        build.setDirectory( buildDirectory );
        build.setOutputDirectory( outputDirectory );
        build.setTestOutputDirectory( testOutputDirectory );
    }

    public void cleanBuildEnvironment()
        throws Exception
    {
        if ( FileUtils.fileExists( resourcesDirectory ) )
        {
            FileUtils.deleteDirectory( resourcesDirectory );
        }

        if ( FileUtils.fileExists( testResourcesDirectory ) )
        {
            FileUtils.deleteDirectory( testResourcesDirectory );
        }

        if ( FileUtils.fileExists( outputDirectory ) )
        {
            FileUtils.deleteDirectory( outputDirectory );
        }

        if ( FileUtils.fileExists( testOutputDirectory ) )
        {
            FileUtils.deleteDirectory( testOutputDirectory );
        }
    }

    public void setupBuildEnvironment()
        throws Exception
    {
        // populate dummy resources and dummy test resources

        // setup src dir
        if ( !FileUtils.fileExists( resourcesDirectory ) )
        {
            FileUtils.mkdir( resourcesDirectory );
        }

        if ( !FileUtils.fileExists( testResourcesDirectory ) )
        {
            FileUtils.mkdir( testResourcesDirectory );
        }

        createDirectories( resourcesDirectory, testResourcesDirectory );
        createFiles( resourcesDirectory, testResourcesDirectory );

        // setup target dir
        if ( !FileUtils.fileExists( outputDirectory ) )
        {
            FileUtils.mkdir( outputDirectory );
        }

        if ( !FileUtils.fileExists( testOutputDirectory ) )
        {
            FileUtils.mkdir( testOutputDirectory );
        }
    }

    private void createDirectories( String parent, String testparent )
    {
        File currentDirectory;

        for ( String directory : directoryList )
        {
            currentDirectory = new File( parent, "/" + directory );

            if ( !currentDirectory.exists() )
            {
                currentDirectory.mkdirs();
            }

            // duplicate dir structure in test resources
            currentDirectory = new File( testparent, "/" + directory );

            if ( !currentDirectory.exists() )
            {
                currentDirectory.mkdirs();
            }
        }
    }

    private void createFiles( String parent, String testparent )
    {
        File currentFile;

        for ( String file : fileList )
        {
            currentFile = new File( parent, file );

            // create the necessary parent directories
            // before we create the files
            if ( !currentFile.getParentFile().exists() )
            {
                currentFile.getParentFile().mkdirs();
            }

            if ( !currentFile.exists() )
            {
                try
                {
                    currentFile.createNewFile();
                    populateFile( currentFile );
                }
                catch ( IOException io )
                {
                    //TODO: handle exception
                }
            }

            // duplicate file in test resources
            currentFile = new File( testparent, file );

            if ( !currentFile.getParentFile().exists() )
            {
                currentFile.getParentFile().mkdirs();
            }

            if ( !currentFile.exists() )
            {
                try
                {
                    currentFile.createNewFile();
                    populateFile( currentFile );
                }
                catch ( IOException io )
                {
                    //TODO: handle exception
                }
            }
        }
    }

    private void populateFile( File file )
    {
        FileOutputStream outputStream;
        String data = (String) dataMap.get( file.getName() );

        if ( ( data != null ) && file.exists() )
        {
            try
            {
                outputStream = new FileOutputStream( file );
                outputStream.write( data.getBytes() );
                outputStream.flush();
                outputStream.close();
            }
            catch ( IOException ex )
            {
                // TODO: handle exception here
            }
        }
    }
}
