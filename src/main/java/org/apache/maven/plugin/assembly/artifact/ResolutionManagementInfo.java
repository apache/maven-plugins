package org.apache.maven.plugin.assembly.artifact;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.ScopeArtifactFilter;

/**
 * Helper class used to accumulate scopes and modules (with binaries included) 
 * that are used in an assembly, for the purposes of creating an aggregated
 * managed-version map with dependency version conflicts resolved.
 * 
 * @author jdcasey
 */
class ResolutionManagementInfo
{
    private boolean resolutionRequired;
    
    private ScopeArtifactFilter scopeFilter = new ScopeArtifactFilter();
    
    private boolean resolvedTransitively;
    
    private Set enabledProjects = new LinkedHashSet();
    
    ResolutionManagementInfo( MavenProject mainProject )
    {
        enabledProjects.add( mainProject );
    }

    boolean isResolutionRequired()
    {
        return resolutionRequired;
    }

    void setResolutionRequired( boolean resolutionRequired )
    {
        this.resolutionRequired = resolutionRequired;
    }

    boolean isResolvedTransitively()
    {
        return resolvedTransitively;
    }

    void setResolvedTransitively( boolean resolvedTransitively )
    {
        this.resolvedTransitively = resolvedTransitively;
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
    
    void enableProjectResolution( MavenProject project )
    {
        if ( !enabledProjects.contains( project ) )
        {
            enabledProjects.add( project );
        }
    }

    Set getEnabledProjects()
    {
        return enabledProjects;
    }
}
