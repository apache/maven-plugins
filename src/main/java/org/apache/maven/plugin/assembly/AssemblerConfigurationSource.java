package org.apache.maven.plugin.assembly;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;


public interface AssemblerConfigurationSource
{

    String getDescriptor();

    String getDescriptorId();

    String[] getDescriptors();

    String[] getDescriptorReferences();

    File getDescriptorSourceDirectory();

    File getBasedir();

    MavenProject getProject();

    boolean isSiteIncluded();

    File getSiteDirectory();

    String getFinalName();

    boolean isAssemblyIdAppended();

    String getClassifier();

    String getTarLongFileMode();

    File getOutputDirectory();

    File getWorkingDirectory();

    MavenArchiveConfiguration getJarArchiveConfiguration();

    ArtifactRepository getLocalRepository();

    File getTemporaryRootDirectory();

    File getArchiveBaseDirectory();

    List getFilters();

    List getReactorProjects();

    List getRemoteRepositories();

    boolean isDryRun();

}
