/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.maven.plugin.eclipse.reader;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;

import org.apache.maven.plugin.eclipse.WorkspaceConfiguration;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.internal.localstore.SafeChunkyInputStream;

/**
 * Scan the eclipse workspace and create a array with {@link IdeDependency} for all found artefacts.
 * 
 * @author Richard van Nieuwenhoven
 * @version $Id$
 */
public class ReadWorkspaceLocations
{

    private static final String BINARY_LOCATION_FILE = ".location";

    private static final String METADATA_PLUGINS_ORG_ECLIPSE_CORE_RESOURCES_PROJECTS =
        ".metadata/.plugins/org.eclipse.core.resources/.projects";

    private static final String[] PARENT_VERSION = new String[] { "parent", "version" };

    private static final String[] PARENT_GROUP_ID = new String[] { "parent", "groupId" };

    private static final String[] PACKAGING = new String[] { "packaging" };

    private static final String[] VERSION = new String[] { "version" };

    private static final String[] GROUP_ID = new String[] { "groupId" };

    private static final String[] ARTEFACT_ID = new String[] { "artifactId" };

    private static final String METADATA_PLUGINS_ORG_ECLIPSE_CORE_RUNTIME_LAUNCHING_PREFS =
        ".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.jdt.launching.prefs";

    private static final String METADATA_PLUGINS_ORG_ECLIPSE_CORE_RUNTIME_PREFS_VM_KEY =
        "org.eclipse.jdt.launching.PREF_VM_XML";

    private static final String METADATA_PLUGINS_ORG_ECLIPSE_CORE_RUNTIME_SERVER_PREFS =
        ".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.wst.server.core.prefs";

    private static final String METADATA_PLUGINS_ORG_ECLIPSE_CORE_RUNTIME_PREFS_RUNTIMES_KEY = "runtimes";

    private static final String CLASSPATHENTRY_DEFAULT = "org.eclipse.jdt.launching.JRE_CONTAINER";

    private static final String CLASSPATHENTRY_STANDARD =
        CLASSPATHENTRY_DEFAULT + "/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/";

    private static final String CLASSPATHENTRY_FORMAT = ReadWorkspaceLocations.CLASSPATHENTRY_DEFAULT + "/{0}/{1}";

    public void init( Log log, WorkspaceConfiguration workspaceConfiguration, MavenProject project,
                      String wtpDefaultServer )
    {
        detectDefaultJREContaigner( workspaceConfiguration, project, log );
        readWorkspace( workspaceConfiguration, log );
        detectWTPDefaultServer( log, workspaceConfiguration, wtpDefaultServer );
    }

    private void detectWTPDefaultServer( Log log, WorkspaceConfiguration workspaceConfiguration, String wtpDefaultServer )
    {
        HashMap servers = readDefinedServers( workspaceConfiguration, log );
        if ( wtpDefaultServer != null )
        {
            Set ids = servers.keySet();
            // first we try the exact match
            Iterator idIterator = ids.iterator();
            while ( workspaceConfiguration.getDefaultDeployServerId() == null && idIterator.hasNext() )
            {
                String id = (String) idIterator.next();
                String name = (String) servers.get( id );
                if ( wtpDefaultServer.equals( id ) || wtpDefaultServer.equals( name ) )
                {
                    workspaceConfiguration.setDefaultDeployServerId( id );
                    workspaceConfiguration.setDefaultDeployServerName( name );
                }
            }
            if ( workspaceConfiguration.getDefaultDeployServerId() == null )
            {
                log.info( "no exact wtp server match." );
                // now we will try the substring match
                idIterator = ids.iterator();
                while ( workspaceConfiguration.getDefaultDeployServerId() == null && idIterator.hasNext() )
                {
                    String id = (String) idIterator.next();
                    String name = (String) servers.get( id );
                    if ( id.indexOf( wtpDefaultServer ) >= 0 || name.indexOf( wtpDefaultServer ) >= 0 )
                    {
                        workspaceConfiguration.setDefaultDeployServerId( id );
                        workspaceConfiguration.setDefaultDeployServerName( name );
                    }
                }
            }
        }
        if ( workspaceConfiguration.getDefaultDeployServerId() == null && servers.size() > 0 )
        {
            // now take the default server
            log.info( "no substring wtp server match." );
            workspaceConfiguration.setDefaultDeployServerId( (String) servers.get( "" ) );
            workspaceConfiguration.setDefaultDeployServerName( (String) servers.get( workspaceConfiguration.getDefaultDeployServerId() ) );
        }
        log.info( "Using as WTP server : " + workspaceConfiguration.getDefaultDeployServerName() );
    }

    /**
     * Take the compiler executable and try to find a JRE that contains that compiler.
     * 
     * @param rawExecutable the executable with the complete path.
     * @param jreMap the map with defined JRE's.
     * @param logger the logger to log the error's
     * @return the found container or null if non found.
     */
    private String getContaignerFromExecutable( String rawExecutable, Map jreMap, Log logger )
    {
        String foundContaigner = null;
        if ( rawExecutable != null )
        {
            String executable;
            try
            {
                executable = new File( rawExecutable ).getCanonicalPath();
                logger.debug( "detected executable: " + executable );
            }
            catch ( Exception e )
            {
                return null;
            }
            File executableFile = new File( executable );
            while ( executableFile != null )
            {
                foundContaigner = (String) jreMap.get( executableFile.getPath() );
                if ( foundContaigner != null )
                {
                    logger.debug( "detected classpathContaigner from executable: " + foundContaigner );
                    return foundContaigner;

                }
                executableFile = executableFile.getParentFile();
            }
        }
        return null;
    }

    /**
     * Search the default JREContainer from eclipse for the current MavenProject
     * 
     * @param workspaceLocation the location of the workspace.
     * @param project the maven project the get the configuration
     * @param logger the logger for errors
     */
    private void detectDefaultJREContaigner( WorkspaceConfiguration workspaceConfiguration, MavenProject project,
                                             Log logger )
    {
        String defaultJREContainer = ReadWorkspaceLocations.CLASSPATHENTRY_DEFAULT;
        if ( workspaceConfiguration.getWorkspaceDirectory() != null )
        {
            Map jreMap = readAvailableJREs( workspaceConfiguration.getWorkspaceDirectory(), logger );
            if ( jreMap != null )
            {
                String foundContaigner =
                    getContaignerFromExecutable( System.getProperty( "maven.compiler.executable" ), jreMap, logger );
                if ( foundContaigner == null )
                {
                    foundContaigner =
                        getContaignerFromExecutable( IdeUtils.getCompilerPluginSetting( project, "executable" ),
                                                     jreMap, logger );
                }
                if ( foundContaigner == null )
                {
                    String sourceVersion = IdeUtils.getCompilerSourceVersion( project );
                    foundContaigner = (String) jreMap.get( sourceVersion );
                    if ( foundContaigner != null )
                    {
                        logger.debug( "detected classpathContaigner from sourceVersion(" + sourceVersion + "): " +
                            foundContaigner );
                    }
                }
                if ( foundContaigner == null )
                {
                    foundContaigner = getContaignerFromExecutable( System.getProperty( "java.home" ), jreMap, logger );
                }
                if ( foundContaigner != null )
                {
                    defaultJREContainer = foundContaigner;
                }

            }
        }
        workspaceConfiguration.setDefaultClasspathContainer( defaultJREContainer );
    }

    /**
     * Get the project location for a project in the eclipse metadata.
     * 
     * @param workspaceLocation the location of the workspace
     * @param project the project subdirectory in the metadata
     * @return the full path to the project.
     */
    private String getProjectLocation( File workspaceLocation, File project )
    {
        String projectLocation = null;
        File location = new File( project, ReadWorkspaceLocations.BINARY_LOCATION_FILE );
        if ( location.exists() )
        {
            try
            {
                SafeChunkyInputStream fileInputStream = new SafeChunkyInputStream( location );
                DataInputStream dataInputStream = new DataInputStream( fileInputStream );
                String file = dataInputStream.readUTF().trim();

                if ( file.length() > 0 )
                {
                    file = file.substring( file.indexOf( ':' ) + 1 );
                    while ( !Character.isLetterOrDigit( file.charAt( 0 ) ) )
                    {
                        file = file.substring( 1 );
                    }
                    if ( file.indexOf( ':' ) < 0 )
                    {
                        file = File.separator + file;
                    }
                    projectLocation = file;
                }

            }
            catch ( FileNotFoundException e )
            {
                projectLocation = "unknown";
            }
            catch ( IOException e )
            {
                projectLocation = "unknown";
            }
        }
        if ( projectLocation == null )
        {
            File projectBase = new File( workspaceLocation, project.getName() );
            if ( projectBase.isDirectory() )
            {
                projectLocation = projectBase.getAbsolutePath();
            }
        }
        return projectLocation;
    }

    /**
     * get a value from a dom element.
     * 
     * @param element the element to get a value from
     * @param elementNames the sub elements to get
     * @param defaultValue teh default value if the value was null or empty
     * @return the value of the dome element.
     */
    private String getValue( Xpp3Dom element, String[] elementNames, String defaultValue )
    {
        String value = null;
        Xpp3Dom dom = element;
        for ( int index = 0; dom != null && index < elementNames.length; index++ )
        {
            dom = dom.getChild( elementNames[index] );
        }
        if ( dom != null )
        {
            value = dom.getValue();
        }
        if ( value == null || value.trim().length() == 0 )
        {
            return defaultValue;
        }
        else
        {
            return value;
        }
    }

    /**
     * Read the artefact information from the pom in the project location and the eclipse project name from the .project
     * file.
     * 
     * @param projectLocation the location of the project
     * @param logger the logger to report errors and debug info.
     * @return an {@link IdeDependency} or null.
     * @throws FileNotFoundException
     * @throws XmlPullParserException
     * @throws IOException
     */
    private IdeDependency readArtefact( String projectLocation, Log logger )
        throws FileNotFoundException, XmlPullParserException, IOException
    {
        File projectFile = new File( new File( projectLocation ), ".project" );
        String eclipseProjectName = new File( projectLocation ).getName();
        if ( projectFile.exists() )
        {
            Xpp3Dom project = Xpp3DomBuilder.build( new FileReader( projectFile ) );
            eclipseProjectName = getValue( project, new String[] { "name" }, eclipseProjectName );
        }
        File pomFile = new File( new File( projectLocation ), "pom.xml" );
        if ( pomFile.exists() )
        {
            Xpp3Dom pom = Xpp3DomBuilder.build( new FileReader( pomFile ) );

            String artifact = getValue( pom, ReadWorkspaceLocations.ARTEFACT_ID, null );
            String group =
                getValue( pom, ReadWorkspaceLocations.GROUP_ID, getValue( pom, ReadWorkspaceLocations.PARENT_GROUP_ID,
                                                                          null ) );
            String version =
                getValue( pom, ReadWorkspaceLocations.VERSION, getValue( pom, ReadWorkspaceLocations.PARENT_VERSION,
                                                                         null ) );
            String packaging = getValue( pom, ReadWorkspaceLocations.PACKAGING, "jar" );

            logger.debug( "found workspace artefact " + group + ":" + artifact + ":" + version + " " + packaging +
                " (" + eclipseProjectName + ")" + " -> " + projectLocation );
            return new IdeDependency( group, artifact, version, packaging, true, false, false, false, false, null,
                                      packaging, false, null, 0, eclipseProjectName );
        }
        else
        {
            logger.debug( "ignored workspace project NO pom available " + projectLocation );
            return null;
        }
    }

    private HashMap readDefinedServers( WorkspaceConfiguration workspaceConfiguration, Log logger )
    {
        HashMap detectedRuntimes = new HashMap();
        if ( workspaceConfiguration.getWorkspaceDirectory() != null )
        {
            Xpp3Dom runtimesElement = null;
            try
            {
                File prefs =
                    new File( workspaceConfiguration.getWorkspaceDirectory(),
                              ReadWorkspaceLocations.METADATA_PLUGINS_ORG_ECLIPSE_CORE_RUNTIME_SERVER_PREFS );
                Properties properties = new Properties();
                properties.load( new FileInputStream( prefs ) );
                runtimesElement =
                    Xpp3DomBuilder.build( new StringReader(
                                                            properties.getProperty( ReadWorkspaceLocations.METADATA_PLUGINS_ORG_ECLIPSE_CORE_RUNTIME_PREFS_RUNTIMES_KEY ) ) );
            }
            catch ( Exception e )
            {
                logger.error( "Could not read workspace wtp server runtimes preferences : " + e.getMessage() );
            }

            if ( runtimesElement != null )
            {
                Xpp3Dom[] runtimeArray = runtimesElement.getChildren( "runtime" );
                for ( int index = 0; runtimeArray != null && index < runtimeArray.length; index++ )
                {
                    String id = runtimeArray[index].getAttribute( "id" );
                    String name = runtimeArray[index].getAttribute( "name" );
                    if ( detectedRuntimes.isEmpty() )
                    {
                        logger.debug( "Using WTP runtime with id: \"" + id + "\" as default runtime" );
                        detectedRuntimes.put( "", id );
                    }
                    detectedRuntimes.put( id, name );
                    logger.debug( "Detected WTP runtime with id: \"" + id + "\" and name: \"" + name + "\"" );
                }
            }
        }
        return detectedRuntimes;
    }

    /**
     * Read the JRE definition configured in the workspace. They will be put in a HashMap with as key there path and as
     * value the JRE constant. a second key is included with the JRE version as a key.
     * 
     * @param workspaceLocation the workspace location
     * @param logger the logger to error messages
     * @return the map with found jre's
     */
    private HashMap readAvailableJREs( File workspaceLocation, Log logger )
    {
        HashMap jreMap = new HashMap();
        jreMap.put( "1.2", CLASSPATHENTRY_STANDARD + "J2SE-1.2" );
        jreMap.put( "1.3", CLASSPATHENTRY_STANDARD + "J2SE-1.3" );
        jreMap.put( "1.4", CLASSPATHENTRY_STANDARD + "J2SE-1.4" );
        jreMap.put( "1.5", CLASSPATHENTRY_STANDARD + "J2SE-1.5" );
        jreMap.put( "5", jreMap.get( "1.5" ) );
        jreMap.put( "1.6", CLASSPATHENTRY_STANDARD + "JavaSE-1.6" );
        jreMap.put( "6", jreMap.get( "1.6" ) );
        Xpp3Dom vms;
        try
        {
            File prefs =
                new File( workspaceLocation,
                          ReadWorkspaceLocations.METADATA_PLUGINS_ORG_ECLIPSE_CORE_RUNTIME_LAUNCHING_PREFS );
            Properties properties = new Properties();
            properties.load( new FileInputStream( prefs ) );
            vms =
                Xpp3DomBuilder.build( new StringReader(
                                                        properties.getProperty( ReadWorkspaceLocations.METADATA_PLUGINS_ORG_ECLIPSE_CORE_RUNTIME_PREFS_VM_KEY ) ) );
        }
        catch ( Exception e )
        {
            logger.error( "Could not read workspace JRE preferences", e );
            return jreMap;
        }
        String defaultJRE = vms.getAttribute( "defaultVM" ).trim();
        Xpp3Dom[] vmTypes = vms.getChildren( "vmType" );
        for ( int vmTypeIndex = 0; vmTypeIndex < vmTypes.length; vmTypeIndex++ )
        {
            String typeId = vmTypes[vmTypeIndex].getAttribute( "id" );
            Xpp3Dom[] vm = vmTypes[vmTypeIndex].getChildren( "vm" );
            for ( int vmIndex = 0; vmIndex < vm.length; vmIndex++ )
            {
                try
                {
                    String path = vm[vmIndex].getAttribute( "path" );
                    String name = vm[vmIndex].getAttribute( "name" );
                    String vmId = vm[vmIndex].getAttribute( "id" ).trim();
                    String classpathEntry =
                        MessageFormat.format( ReadWorkspaceLocations.CLASSPATHENTRY_FORMAT,
                                              new Object[] { typeId, name } );
                    String jrePath = new File( path ).getCanonicalPath();
                    JarFile rtJar = new JarFile( new File( new File( jrePath ), "jre/lib/rt.jar" ) );
                    String version = rtJar.getManifest().getMainAttributes().getValue( "Specification-Version" );
                    if ( defaultJRE.endsWith( "," + vmId ) )
                    {
                        jreMap.put( jrePath, ReadWorkspaceLocations.CLASSPATHENTRY_DEFAULT );
                        jreMap.put( version, ReadWorkspaceLocations.CLASSPATHENTRY_DEFAULT );
                        logger.debug( "Default Classpath Contaigner version: " + version + "  location: " + jrePath );
                    }
                    else if ( !jreMap.containsKey( jrePath ) )
                    {
                        if ( !jreMap.containsKey( version ) )
                        {
                            jreMap.put( version, classpathEntry );
                        }
                        jreMap.put( jrePath, classpathEntry );
                        logger.debug( "Additional Classpath Contaigner version: " + version + " " + classpathEntry +
                            " location: " + jrePath );
                    }
                    else
                    {
                        logger.debug( "Ignored (duplicated) additional Classpath Contaigner version: " + version + " " +
                            classpathEntry + " location: " + jrePath );
                    }
                }
                catch ( IOException e )
                {
                    logger.warn( "Could not interpret entry: " + vm.toString() );
                }
            }
        }
        return jreMap;
    }

    /**
     * Scan the eclipse workspace and create a array with {@link IdeDependency} for all found artifacts.
     * 
     * @param workspaceLocation the location of the eclipse workspace.
     * @param logger the logger to report errors and debug info.
     */
    private void readWorkspace( WorkspaceConfiguration workspaceConfiguration, Log logger )
    {
        ArrayList dependencys = new ArrayList();
        if ( workspaceConfiguration.getWorkspaceDirectory() != null )
        {
            File workspace =
                new File( workspaceConfiguration.getWorkspaceDirectory(),
                          ReadWorkspaceLocations.METADATA_PLUGINS_ORG_ECLIPSE_CORE_RESOURCES_PROJECTS );

            File[] directories = workspace.listFiles();
            for ( int index = 0; directories != null && index < directories.length; index++ )
            {
                File project = directories[index];
                if ( project.isDirectory() )
                {
                    try
                    {
                        String projectLocation =
                            getProjectLocation( workspaceConfiguration.getWorkspaceDirectory(), project );
                        if ( projectLocation != null )
                        {
                            IdeDependency ideDependency = readArtefact( projectLocation, logger );
                            if ( ideDependency != null )
                            {
                                dependencys.add( ideDependency );
                            }
                        }
                    }
                    catch ( Exception e )
                    {
                        logger.warn( "could not read workspace project:" + project );
                    }
                }
            }
        }
        workspaceConfiguration.setWorkspaceArtefacts( (IdeDependency[]) dependencys.toArray( new IdeDependency[dependencys.size()] ) );
    }
}
