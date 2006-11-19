package org.apache.maven.plugin.ejb.stub;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.maven.model.Build;
import org.codehaus.plexus.util.FileUtils;

/**
 * Stub
 */
public class MavenProjectBuildStub
    extends MavenProjectBasicStub
{
    public static final int RESOURCES_FILE = 1;

    public static final int ROOT_FILE = 2;

    public static final int OUTPUT_FILE = 3;

    public static final int SOURCE_FILE = 4;

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

    protected ArrayList targetClassesList;

    protected ArrayList sourceFileList;

    protected ArrayList resourcesFileList;

    protected ArrayList rootFileList;

    protected ArrayList directoryList;

    protected HashMap dataMap;

    public MavenProjectBuildStub( String key )
        throws Exception
    {
        super( key );

        build = new Build();
        resourcesFileList = new ArrayList();
        sourceFileList = new ArrayList();
        rootFileList = new ArrayList();
        directoryList = new ArrayList();
        targetClassesList = new ArrayList();
        dataMap = new HashMap();
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

    public void addFile( String name, int type )
    {
        if ( isValidPath( name ) )
        {
            List list = getList( type );

            list.add( name );
        }
    }

    public void addFile( String name, String data, int type )
    {
        File fileName = new File( name );

        addFile( name, type );
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
        setupRootFiles();

        // setup target dir        
        if ( !FileUtils.fileExists( outputDirectory ) )
        {
            FileUtils.mkdir( outputDirectory );
        }

        if ( !FileUtils.fileExists( testOutputDirectory ) )
        {
            FileUtils.mkdir( testOutputDirectory );
        }

        setupTargetFiles();
    }

    private void createDirectories( String parent, String testparent )
    {
        File currentDirectory;

        for ( int nIndex = 0; nIndex < directoryList.size(); nIndex++ )
        {
            currentDirectory = new File( parent, "/" + (String) directoryList.get( nIndex ) );

            if ( !currentDirectory.exists() )
            {
                currentDirectory.mkdirs();
            }

            // duplicate dir structure in test resources
            currentDirectory = new File( testparent, "/" + (String) directoryList.get( nIndex ) );

            if ( !currentDirectory.exists() )
            {
                currentDirectory.mkdirs();
            }
        }
    }

    private List getList( int type )
    {
        ArrayList retVal = null;

        switch ( type )
        {
            case SOURCE_FILE :
                retVal = sourceFileList;
                break;
            case OUTPUT_FILE :
                retVal = targetClassesList;
                break;
            case RESOURCES_FILE :
                retVal = resourcesFileList;
                break;
            case ROOT_FILE :
                retVal = rootFileList;
                break;
        }

        return retVal;
    }

    private void createFiles( String parent, int type )
    {
        File currentFile;
        ArrayList list = (ArrayList) getList( type );

        // guard
        if ( list == null )
        {
            return;
        }

        for ( int nIndex = 0; nIndex < list.size(); nIndex++ )
        {
            currentFile = new File( parent, (String) list.get( nIndex ) );

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
                    populateFile( currentFile, RESOURCES_FILE );
                }
                catch ( IOException io )
                {
                    //TODO: handle exception
                }
            }
        }
    }

    private void setupRootFiles()
    {
        createFiles( testRootDir, ROOT_FILE );
    }

    private void setupTargetFiles()
    {
        createFiles( getOutputDirectory(), OUTPUT_FILE );
    }

    private void setupSourceFiles()
    {
        createFiles( srcDirectory, SOURCE_FILE );
    }

    private void createFiles( String parent, String testparent )
    {
        createFiles( parent, RESOURCES_FILE );
        createFiles( testparent, RESOURCES_FILE );
    }

    private void populateFile( File file, int type )
    {
        FileOutputStream outputStream;
        String data = data = (String) dataMap.get( file.getName() );

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
