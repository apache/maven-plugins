package org.apache.maven.plugin.assembly;

import org.apache.maven.artifact.Artifact;

import java.util.Set;

public class DefaultAssemblyContext
    implements AssemblyContext
{

    private Set<Artifact> artifacts;

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.maven.plugin.assembly.AssemblyContext#setResolvedArtifacts(java.util.Set)
     */
    public void setResolvedArtifacts( final Set<Artifact> artifacts )
    {
        this.artifacts = artifacts;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.maven.plugin.assembly.AssemblyContext#getResolvedArtifacts()
     */
    public Set<Artifact> getResolvedArtifacts()
    {
        return artifacts;
    }

}
