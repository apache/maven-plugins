package org.apache.maven.plugin.eclipse;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

import org.apache.maven.plugin.eclipse.reader.ReadWorkspaceLocations;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.internal.localstore.ILocalStoreConstants;

public class TempEclipseWorkspace
{
    private static TempEclipseWorkspace rad7WithDefault14;

    private static TempEclipseWorkspace eclipseWithDefault15;

    private static TempEclipseWorkspace eclipseWithDefault13;

    private static TempEclipseWorkspace dynamicWorkspace;

    /**
     * @return RAD 7 workspace, JDK 14, includes projects: "direct-compile"
     * @throws Exception
     */
    public static TempEclipseWorkspace getFixtureEclipseWorkspaceWithRad7Default14()
        throws Exception
    {
        if ( rad7WithDefault14 == null )
        {
            rad7WithDefault14 = new TempEclipseWorkspace( "rad7WithDefault14", new String[] { "direct-compile" } );
        }
        return rad7WithDefault14;
    }

    /**
     * @return Eclipse workspace, JDK 1.5, includes projects: "direct-compile".
     * @throws Exception
     */
    public static TempEclipseWorkspace getFixtureEclipseWithDefault15()
        throws Exception
    {
        if ( eclipseWithDefault15 == null )
        {
            eclipseWithDefault15 = new TempEclipseWorkspace( "eclipseWithDefault15", new String[] { "direct-compile" } );
        }
        return eclipseWithDefault15;
    }

    /**
     * @return Eclipse workspace, JDK 1.3, includes projects: "direct-compile"
     * @throws Exception
     */
    public static TempEclipseWorkspace getFixtureEclipseWithDefault13()
        throws Exception
    {
        if ( eclipseWithDefault13 == null )
        {
            eclipseWithDefault13 = new TempEclipseWorkspace( "eclipseWithDefault13", new String[] { "direct-compile" } );
        }
        return eclipseWithDefault13;
    }

    /**
     * @return Eclipse workspace, JDK 1.4, includes projects: "project-A/module-A1", "../project-O"
     * @throws Exception
     */
    public static TempEclipseWorkspace getFixtureEclipseDynamicWorkspace()
        throws Exception
    {
        if ( dynamicWorkspace == null )
        {
            dynamicWorkspace =
                new TempEclipseWorkspace( "dynamicWorkspace", new String[] { "project-A/module-A1", "../project-O" } );
        }
        return dynamicWorkspace;
    }

    public File workspaceLocation;

    public TempEclipseWorkspace( String testWorkspaceName, String[] projectsToLink )
        throws Exception
    {

        File eclipseLocation = new java.io.File( "target/test-classes/eclipse" ).getCanonicalFile();

        File jdkLocation = new File( eclipseLocation, "dummyJDK" );

        workspaceLocation = new File( eclipseLocation, testWorkspaceName + "/workspace" ).getCanonicalFile();

        File propertyfile =
            new File( workspaceLocation,
                      ".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.jdt.launching.prefs" );

        preparePropertyFile( jdkLocation, propertyfile );

        if ( projectsToLink != null && projectsToLink.length != 0 )
        {
            for ( int i = 0; i < projectsToLink.length; i++ )
            {
                String projectToLink = projectsToLink[i];
                writeLocationFile( projectToLink );
            }
        }

    }

    /**
     * Given the relative path from the workspace to the project to link use the basename as the project name and link
     * this project to the fully qualified path anchored at workspaceLocation.
     * 
     * @param projectToLink
     * @throws MalformedURLException
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void writeLocationFile( String projectToLink )
        throws MalformedURLException, FileNotFoundException, IOException
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
        throws IOException, FileNotFoundException
    {
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
