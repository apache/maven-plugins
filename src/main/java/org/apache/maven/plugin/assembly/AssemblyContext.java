package org.apache.maven.plugin.assembly;

import org.apache.maven.artifact.Artifact;

import java.util.Set;

public interface AssemblyContext
{
    void setResolvedArtifacts( Set<Artifact> artifacts );

    Set<Artifact> getResolvedArtifacts();
}
