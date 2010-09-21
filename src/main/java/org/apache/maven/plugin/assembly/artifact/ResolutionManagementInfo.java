package org.apache.maven.plugin.assembly.artifact;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.ScopeArtifactFilter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Helper class used to accumulate scopes and modules (with binaries included) that are used in an assembly, for the
 * purposes of creating an aggregated managed-version map with dependency version conflicts resolved.
 * 
 * @author jdcasey
 */
class ResolutionManagementInfo
{
    private boolean resolutionRequired;

    private final ScopeArtifactFilter scopeFilter = new ScopeArtifactFilter();

    private boolean resolvedTransitively;

    private final Set<MavenProject> enabledProjects = new LinkedHashSet<MavenProject>();

    private final Set<Artifact> artifacts = new LinkedHashSet<Artifact>();

    ResolutionManagementInfo( final MavenProject currentProject )
    {
        enabledProjects.add( currentProject );
    }

    boolean isResolutionRequired()
    {
        return resolutionRequired;
    }

    void setResolutionRequired( final boolean resolutionRequired )
    {
        this.resolutionRequired = resolutionRequired;
    }

    boolean isResolvedTransitively()
    {
        return resolvedTransitively;
    }

    void setResolvedTransitively( final boolean resolvedTransitively )
    {
        this.resolvedTransitively = this.resolvedTransitively || resolvedTransitively;
    }

    ScopeArtifactFilter getScopeFilter()
    {
        return scopeFilter;
    }

    void enableCompileScope()
    {
        scopeFilter.setIncludeCompileScope( true );
        scopeFilter.setIncludeProvidedScope( true );
        scopeFilter.setIncludeSystemScope( true );
    }

    void enableProvidedScope()
    {
        scopeFilter.setIncludeProvidedScope( true );
    }

    void enableRuntimeScope()
    {
        scopeFilter.setIncludeRuntimeScope( true );
        scopeFilter.setIncludeCompileScope( true );
    }

    void enableTestScope()
    {
        scopeFilter.setIncludeTestScope( true );
        scopeFilter.setIncludeCompileScope( true );
        scopeFilter.setIncludeProvidedScope( true );
        scopeFilter.setIncludeSystemScope( true );
        scopeFilter.setIncludeRuntimeScope( true );
    }

    void enableSystemScope()
    {
        scopeFilter.setIncludeSystemScope( true );
    }

    void enableProjectResolution( final MavenProject project )
    {
        if ( !enabledProjects.contains( project ) )
        {
            enabledProjects.add( project );
        }
    }

    Set<MavenProject> getEnabledProjects()
    {
        return enabledProjects;
    }

    Set<Artifact> getArtifacts()
    {
        return artifacts;
    }

    void addArtifacts( final Set<Artifact> a )
    {
        artifacts.addAll( a );
    }

    void addArtifact( final Artifact a )
    {
        artifacts.add( a );
    }
}
