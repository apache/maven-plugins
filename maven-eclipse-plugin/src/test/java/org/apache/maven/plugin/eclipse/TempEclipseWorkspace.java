package org.apache.maven.plugin.eclipse;

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


import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.eclipse.reader.ReadWorkspaceLocations;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.internal.localstore.ILocalStoreConstants;

public class TempEclipseWorkspace
{
    private static int workspaceNumber = 0;
    
    /**
     * @return RAD 7 workspace, JDK 14, includes projects: "direct-compile"
     * @throws Exception
     */
    public static TempEclipseWorkspace getFixtureEclipseWorkspaceWithRad7Default14()
        throws Exception
    {
        return new TempEclipseWorkspace( "rad7WithDefault14", new String[] { "direct-compile" } );
    }

    /**
     * @return Eclipse workspace, JDK 1.5, includes projects: "direct-compile".
     * @throws Exception
     */
    public static TempEclipseWorkspace getFixtureEclipseWithDefault15()
        throws Exception
    {
        return new TempEclipseWorkspace( "eclipseWithDefault15", new String[] { "direct-compile" } );
    }

    /**
     * @return Eclipse workspace, JDK 1.3, includes projects: "direct-compile"
     * @throws Exception
     */
    public static TempEclipseWorkspace getFixtureEclipseWithDefault13()
        throws Exception
    {
        return new TempEclipseWorkspace( "eclipseWithDefault13", new String[] { "direct-compile" } );
    }

    /**
     * @return Eclipse workspace, JDK 1.4, includes projects: "project-A/module-A1", "../project-O"
     * @throws Exception
     */
    public static TempEclipseWorkspace getFixtureEclipseDynamicWorkspace()
        throws Exception
    {
        return new TempEclipseWorkspace( "dynamicWorkspace", new String[] { "project-A/module-A1", "../project-O" } );
    }

    public File workspaceLocation;

    public TempEclipseWorkspace( String testWorkspaceName, String[] projectsToLink )
        throws Exception
    {

        File tempWorkspace = new File( "target/tmp-workspace" + workspaceNumber++ );
        FileUtils.deleteDirectory( tempWorkspace );
        FileUtils.copyDirectoryToDirectory( new File( "src/test/resources/eclipse" ), tempWorkspace );

        File eclipseLocation = new File( tempWorkspace, "eclipse" ).getCanonicalFile();

        File jdkLocation = new File( eclipseLocation, "dummyJDK" );

        workspaceLocation = new File( eclipseLocation, testWorkspaceName + "/workspace" ).getCanonicalFile();

        File propertyfile =
            new File( workspaceLocation,
                      ".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.jdt.launching.prefs" );

        preparePropertyFile( jdkLocation, propertyfile );

        if ( projectsToLink != null && projectsToLink.length != 0 )
        {
            for (String projectToLink : projectsToLink) {
                writeLocationFile(projectToLink);
            }
        }

    }

    /**
     * Given the relative path from the workspace to the project to link use the basename as the project name and link
     * this project to the fully qualified path anchored at workspaceLocation.
     * 
     * @param projectToLink The project to link
     * @throws MalformedURLException
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void writeLocationFile( String projectToLink )
        throws IOException
    {
        File projectToLinkAsRelativeFile = new File( projectToLink );

        File projectWorkspaceDirectory =
            new File( workspaceLocation, projectToLinkAsRelativeFile.getPath() ).getCanonicalFile();
        String uriToProjectWorkspaceDirectory = "URI//" + projectWorkspaceDirectory.toURI().toURL().toString();

        File metaDataPlugins =
            new File( workspaceLocation, ReadWorkspaceLocations.METADATA_PLUGINS_ORG_ECLIPSE_CORE_RESOURCES_PROJECTS );
        File projectMetaDataDirectory = new File( metaDataPlugins, projectToLinkAsRelativeFile.getName() );
        File locationFile = new File( projectMetaDataDirectory, ReadWorkspaceLocations.BINARY_LOCATION_FILE );

        DataOutputStream dataOutputStream = new DataOutputStream( new FileOutputStream( locationFile ) );

        dataOutputStream.write( ILocalStoreConstants.BEGIN_CHUNK );
        dataOutputStream.writeUTF( uriToProjectWorkspaceDirectory );
        dataOutputStream.write( ILocalStoreConstants.END_CHUNK );
        IOUtil.close( dataOutputStream );
    }

    private static void preparePropertyFile( File jdkLocation, File propertyfile )
        throws IOException {
        Properties properties = new Properties();
        properties.load( new FileInputStream( propertyfile ) );
        properties.setProperty(
                                "org.eclipse.jdt.launching.PREF_VM_XML",
                                properties.getProperty( "org.eclipse.jdt.launching.PREF_VM_XML" ).replaceAll(
                                                                                                              "__replace_with_test_dir__",
                                                                                                              jdkLocation.getCanonicalPath().replace(
                                                                                                                                                      '\\',
                                                                                                                                                      '/' ) ) );
        properties.store( new FileOutputStream( propertyfile ), "" );
    }

}
