package org.apache.maven.plugin.eclipse;

import java.io.File;
import java.net.URL;

import org.apache.maven.artifact.repository.ArtifactRepository;

public class WorkspaceConfiguration
{
    private File workspaceDirectory;

    private URL codeStylesURL;

    private String activeCodeStyleProfileName;

    private ArtifactRepository localRepository;

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

}
