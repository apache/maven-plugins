package org.apache.maven.plugin.assembly.testutils;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;

import java.io.File;
import java.util.List;

public class ConfigSourceStub
    implements AssemblerConfigurationSource
{

    private String archiverConfig;

    private MavenProject project;

    public File getArchiveBaseDirectory()
    {
        return null;
    }

    public File getBasedir()
    {
        return null;
    }

    public String getClassifier()
    {
        return null;
    }

    public String getDescriptor()
    {
        return null;
    }

    public String getDescriptorId()
    {
        return null;
    }

    public String[] getDescriptorReferences()
    {
        return null;
    }

    public File getDescriptorSourceDirectory()
    {
        return null;
    }

    public String[] getDescriptors()
    {
        return null;
    }

    public List<String> getFilters()
    {
        return null;
    }

    public String getFinalName()
    {
        return null;
    }

    public MavenArchiveConfiguration getJarArchiveConfiguration()
    {
        return null;
    }

    public ArtifactRepository getLocalRepository()
    {
        return null;
    }

    public MavenSession getMavenSession()
    {
        return null;
    }

    public File getOutputDirectory()
    {
        return null;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public List<MavenProject> getReactorProjects()
    {
        return null;
    }

    public List<ArtifactRepository> getRemoteRepositories()
    {
        return null;
    }

    public File getSiteDirectory()
    {
        return null;
    }

    public String getTarLongFileMode()
    {
        return null;
    }

    public File getTemporaryRootDirectory()
    {
        return null;
    }

    public File getWorkingDirectory()
    {
        return null;
    }

    public boolean isAssemblyIdAppended()
    {
        return false;
    }

    public boolean isDryRun()
    {
        return false;
    }

    public boolean isIgnoreDirFormatExtensions()
    {
        return false;
    }

    public boolean isIgnoreMissingDescriptor()
    {
        return false;
    }

    public boolean isSiteIncluded()
    {
        return false;
    }

    public void setArchiverConfig( final String archiverConfig )
    {
        this.archiverConfig = archiverConfig;
    }

    public String getArchiverConfig()
    {
        return archiverConfig;
    }

    public MavenFileFilter getMavenFileFilter()
    {
        return null;
    }

    public void setProject( final MavenProject mavenProject )
    {
        project = mavenProject;
    }

}
