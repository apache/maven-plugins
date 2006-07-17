package org.apache.maven.plugin.assembly.utils;

import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

    public static Set getProjectModules( MavenProject project, List reactorProjects ) throws IOException
    {
        Set moduleCandidates = new HashSet( reactorProjects );
        
        Set modules = new HashSet();
        
        // we temporarily add the master project to the modules set, since this set is pulling double duty as a set of
        // potential module parents in the tree rooted at the master project...this allows us to use the same looping
        // algorithm below to discover both direct modules of the master project AND modules of those direct modules.
        modules.add( project );

        int changed = -1;
        
        while( changed != 0 )
        {
            changed = 0;
            
            for ( Iterator candidateIterator = moduleCandidates.iterator(); candidateIterator.hasNext(); )
            {
                MavenProject moduleCandidate = (MavenProject) candidateIterator.next();
                
                for ( Iterator parentIterator = modules.iterator(); parentIterator.hasNext(); )
                {
                    MavenProject potentialParent = (MavenProject) parentIterator.next();
                    
                    // if this parent has an entry for the module candidate in the path adjustments map, it's a direct
                    // module of that parent.
                    if ( potentialParent.getModulePathAdjustment( moduleCandidate ) != null )
                    {
                        // add the candidate to the list of modules (and potential parents)
                        modules.add( moduleCandidate );
                        
                        // remove the candidate from the candidate pool, because it's been verified.
                        candidateIterator.remove();
                        
                        // increment the change counter, to show that we verified a new module on this pass.
                        changed++;
                    }
                }
            }
        }
        
        // remove the master project from the modules set, now that we're done using it as a set of potential module
        // parents...
        modules.remove( project );

        return modules;
    }

}
