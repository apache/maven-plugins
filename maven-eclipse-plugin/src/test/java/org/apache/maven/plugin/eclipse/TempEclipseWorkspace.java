package org.apache.maven.plugin.eclipse;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.internal.localstore.ILocalStoreConstants;

public class TempEclipseWorkspace
{

    public File workspaceLocation;

    public TempEclipseWorkspace( String testWorkspaceName, boolean usePathToProject )
        throws Exception
    {

        File eclipseLocation = new java.io.File( "target/test-classes/eclipse" ).getCanonicalFile();

        File jdkLocation = new File( eclipseLocation, "dummyJDK" );

        workspaceLocation = new File( eclipseLocation, testWorkspaceName + "/workspace" ).getCanonicalFile();

        File localizedIndicator = new File( workspaceLocation, ".localized" );
        if ( !localizedIndicator.exists() )
        {
            File propertyfile =
                new File( workspaceLocation,
                          ".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.jdt.launching.prefs" );

            preparePropertyFile( jdkLocation, propertyfile );

            String projectLocation;
            if ( usePathToProject )
            {
                projectLocation = "URI//file:" + ( new File( workspaceLocation, "direct-compile" ).getCanonicalPath() );
            }
            else
            {
                projectLocation = "";
            }
            FileOutputStream location =
                new FileOutputStream(
                                      new File( workspaceLocation,
                                                ".metadata/.plugins/org.eclipse.core.resources/.projects/direct-compile/.location" ) );
            DataOutputStream dataOutputStream = new DataOutputStream( location );
            dataOutputStream.write( ILocalStoreConstants.BEGIN_CHUNK );
            dataOutputStream.writeUTF( projectLocation );
            dataOutputStream.write( ILocalStoreConstants.END_CHUNK );
            dataOutputStream.close();
            location.close();
            localizedIndicator.createNewFile();
        }

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

    public File getWorkspaceLocation()
    {
        return workspaceLocation;
    }
}
