package org.apache.maven.plugin.assembly.artifact;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;

import java.util.List;
import java.util.Set;

public interface DependencyResolver
{

    public abstract Set resolveDependencies( MavenProject project, String scope, ArtifactRepository localRepository,
                                             List remoteRepositories )
        throws InvalidDependencyVersionException, ArtifactResolutionException, ArtifactNotFoundException;

}