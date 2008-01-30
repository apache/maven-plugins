package org.apache.maven.plugin.eclipse;

import java.io.File;
import java.net.URL;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.ide.IdeDependency;

public class WorkspaceConfiguration
{
    private File workspaceDirectory;

    private URL codeStylesURL;

    private String activeCodeStyleProfileName;

    private ArtifactRepository localRepository;

    private String defaultClasspathContainer;

    private IdeDependency[] workspaceArtefacts;

    private String defaultDeployServerId;

    private String defaultDeployServerName;

    public File getWorkspaceDirectory()
    {
        return this.workspaceDirectory;
    }

    public void setWorkspaceDirectory( File dir )
    {
        this.workspaceDirectory = dir;
    }

    public URL getCodeStylesURL()
    {
        return this.codeStylesURL;
    }

    public void setCodeStylesURL( URL url )
    {
        this.codeStylesURL = url;
    }

    public String getActiveStyleProfileName()
    {
        return this.activeCodeStyleProfileName;
    }

    public void setActiveStyleProfileName( String name )
    {
        this.activeCodeStyleProfileName = name;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    public String getDefaultClasspathContainer()
    {
        return defaultClasspathContainer;
    }

    public void setDefaultClasspathContainer( String defaultClasspathContainer )
    {
        this.defaultClasspathContainer = defaultClasspathContainer;
    }

    public IdeDependency[] getWorkspaceArtefacts()
    {
        return workspaceArtefacts;
    }

    public void setWorkspaceArtefacts( IdeDependency[] workspaceArtefacts )
    {
        this.workspaceArtefacts = workspaceArtefacts;
    }

    public String getDefaultDeployServerId()
    {
        return defaultDeployServerId;
    }

    public void setDefaultDeployServerId( String defaultDeployServerId )
    {
        this.defaultDeployServerId = defaultDeployServerId;
    }

    public String getDefaultDeployServerName()
    {
        return defaultDeployServerName;
    }

    public void setDefaultDeployServerName( String defaultDeployServerName )
    {
        this.defaultDeployServerName = defaultDeployServerName;
    }

    /**
     * @return the defined websphere server version and null if the target is no websphere.
     */
    public String getWebsphereVersion()
    {
        if ( getDefaultDeployServerId() != null && getDefaultDeployServerId().startsWith( "was." ) )
        {
            if ( getDefaultDeployServerId().indexOf( "v61" ) >= 0 )
            {
                return "6.1";
            }
            if ( getDefaultDeployServerId().indexOf( "v6" ) >= 0 )
            {
                return "6.0";
            }
            if ( getDefaultDeployServerId().indexOf( "v51" ) >= 0 )
            {
                return "5.1";
            }
            if ( getDefaultDeployServerId().indexOf( "v5" ) >= 0 )
            {
                return "5.0";
            }
        }
        return null;
    }

}
