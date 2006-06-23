package org.apache.maven.plugin.assembly.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

public final class ProjectUtils
{
    
    private ProjectUtils()
    {
    }

    /**
     * Retrieves all artifact dependencies.
     *
     * @return A HashSet of artifacts
     */
    public static Set getDependencies( MavenProject project )
    {
        Set dependenciesSet = new HashSet();

        if ( project.getArtifact() != null && project.getArtifact().getFile() != null )
        {
            dependenciesSet.add( project.getArtifact() );
        }

        Set projectArtifacts = project.getArtifacts();
        if ( projectArtifacts != null )
        {
            dependenciesSet.addAll( projectArtifacts );
        }

        return dependenciesSet;
    }

}
