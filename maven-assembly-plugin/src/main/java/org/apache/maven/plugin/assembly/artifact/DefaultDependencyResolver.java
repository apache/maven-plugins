package org.apache.maven.plugin.assembly.artifact;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.assembly.utils.FilterUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.maven.shared.artifact.filter.ScopeArtifactFilter;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @plexus.component role="org.apache.maven.plugin.assembly.artifact.DependencyResolver" role-hint="default"
 * 
 * @author jdcasey
 */
public class DefaultDependencyResolver
    extends AbstractLogEnabled implements DependencyResolver
{

    /**
     * @plexus.requirement
     */
    private ArtifactResolver resolver;

    /**
     * @plexus.requirement
     */
    private ArtifactMetadataSource metadataSource;

    /**
     * @plexus.requirement
     */
    private ArtifactFactory factory;

    public DefaultDependencyResolver()
    {
        // for plexus init
    }

    public DefaultDependencyResolver( ArtifactResolver resolver, ArtifactMetadataSource metadataSource,
                               ArtifactFactory factory, Logger logger )
    {
        this.resolver = resolver;
        this.metadataSource = metadataSource;
        this.factory = factory;

        enableLogging( logger );
    }

    /* (non-Javadoc)
     * @see org.apache.maven.plugin.assembly.artifact.DependencyResolver#resolveDependencies(org.apache.maven.project.MavenProject, java.lang.String, org.apache.maven.artifact.repository.ArtifactRepository, java.util.List)
     */
    public Set resolveDependencies( MavenProject project, String scope, ArtifactRepository localRepository,
                                    List remoteRepositories )
        throws InvalidDependencyVersionException, ArtifactResolutionException, ArtifactNotFoundException
    {
        List repos = aggregateRemoteArtifactRepositories( remoteRepositories, project );

        ArtifactFilter filter = new ScopeArtifactFilter( scope );

        // TODO: such a call in MavenMetadataSource too - packaging not really the intention of type
        Artifact artifact =
            factory.createBuildArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion(),
                                         project.getPackaging() );

        Set dependencyArtifacts =
            MavenMetadataSource.createArtifacts( factory, project.getDependencies(), null, filter, project );

        ArtifactResolutionResult result =
            resolver.resolveTransitively( dependencyArtifacts, artifact, localRepository, repos, metadataSource, filter );
        
        getLogger().debug( "While resolving dependencies of " + project.getId() + ":" );
        
        FilterUtils.reportFilteringStatistics( Collections.singleton( filter ), getLogger() );

        return result.getArtifacts();
    }

    private List aggregateRemoteArtifactRepositories( List remoteRepositories, MavenProject project )
    {
        List repoLists = new ArrayList();
        
        repoLists.add( remoteRepositories );
        repoLists.add( project.getRemoteArtifactRepositories() );
        
        List remoteRepos = new ArrayList();
        Set encounteredUrls = new HashSet();

        for ( Iterator listIterator = repoLists.iterator(); listIterator.hasNext(); )
        {
            List repositoryList = ( List ) listIterator.next();
            
            if ( repositoryList != null && !repositoryList.isEmpty() )
            {
                for ( Iterator it = repositoryList.iterator(); it.hasNext(); )
                {
                    ArtifactRepository repo = ( ArtifactRepository ) it.next();
                    
                    if ( !encounteredUrls.contains( repo.getUrl() ) )
                    {
                        remoteRepos.add( repo );
                        encounteredUrls.add( repo.getUrl() );
                    }
                }
            }
        }

        return remoteRepos;
    }

}
