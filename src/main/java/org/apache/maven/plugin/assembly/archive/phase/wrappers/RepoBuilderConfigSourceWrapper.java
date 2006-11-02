package org.apache.maven.plugin.assembly.archive.phase.wrappers;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.repository.RepositoryBuilderConfigSource;

public class RepoBuilderConfigSourceWrapper
    implements RepositoryBuilderConfigSource
{

    private final AssemblerConfigurationSource configSource;

    public RepoBuilderConfigSourceWrapper( AssemblerConfigurationSource configSource )
    {
        this.configSource = configSource;
    }

    public ArtifactRepository getLocalRepository()
    {
        return configSource.getLocalRepository();
    }

    public MavenProject getProject()
    {
        return configSource.getProject();
    }
    
}
