package org.apache.maven.plugin.assembly.archive.phase.wrappers;

import org.apache.maven.plugins.assembly.model.GroupVersionAlignment;
import org.apache.maven.plugins.assembly.model.Repository;
import org.apache.maven.shared.repository.model.RepositoryInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RepoInfoWrapper
    implements RepositoryInfo
{

    private final Repository repo;
    private List convertedAlignments;

    public RepoInfoWrapper( Repository repo )
    {
        this.repo = repo;
    }
    
    public List getExcludes()
    {
        return repo.getExcludes();
    }

    public List getGroupVersionAlignments()
    {
        List alignments = repo.getGroupVersionAlignments();
        
        if ( convertedAlignments == null || alignments.size() != convertedAlignments.size() )
        {
            List l = new ArrayList( alignments.size() );
            
            for ( Iterator it = alignments.iterator(); it.hasNext(); )
            {
                GroupVersionAlignment alignment = (GroupVersionAlignment) it.next();
                
                l.add( new GroupVersionAlignmentWrapper( alignment ) );
            }
            
            convertedAlignments = l;
        }
        
        return convertedAlignments;
    }

    public List getIncludes()
    {
        return repo.getIncludes();
    }

    public String getScope()
    {
        return repo.getScope();
    }

    public boolean isIncludeMetadata()
    {
        return repo.isIncludeMetadata();
    }
    
}
