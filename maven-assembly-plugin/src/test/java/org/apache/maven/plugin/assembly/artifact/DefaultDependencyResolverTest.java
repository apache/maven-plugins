package org.apache.maven.plugin.assembly.artifact;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.model.ModuleBinaries;
import org.apache.maven.plugin.assembly.model.ModuleSet;
import org.apache.maven.plugin.assembly.model.Repository;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.ArgumentsMatcher;
import org.easymock.MockControl;

public class DefaultDependencyResolverTest
    extends PlexusTestCase
{
    
    private ArtifactFactory factory;
    
    private ArtifactRepositoryFactory repoFactory;
    
    private ArtifactRepositoryLayout layout;
    
    private ArtifactCollector collector;
    
    public void setUp()
        throws Exception
    {
        super.setUp();
        
        factory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        repoFactory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );
        layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );
        
        collector = (ArtifactCollector) lookup( ArtifactCollector.class.getName() );
    }
    
    public void test_getDependencySetResolutionRequirements()
    {
        List depSets = new ArrayList();
        
        DependencySet ds1 = new DependencySet();
        ds1.setScope( Artifact.SCOPE_COMPILE );
        ds1.setUseTransitiveDependencies( false );
        
        depSets.add( ds1 );
        
        DependencySet ds2 = new DependencySet();
        ds2.setScope( Artifact.SCOPE_SYSTEM );
        ds2.setUseTransitiveDependencies( false );
        
        depSets.add( ds2 );
        
        ResolutionManagementInfo info = new ResolutionManagementInfo( new MavenProject( new Model() ) );
        
        new DefaultDependencyResolver().getDependencySetResolutionRequirements( depSets, info );
        
        assertTrue( info.isResolutionRequired() );
        assertFalse( info.isResolvedTransitively() );
        
        assertTrue( info.getScopeFilter().isIncludeCompileScope() );
        assertTrue( info.getScopeFilter().isIncludeSystemScope() );
        
        assertTrue( info.getScopeFilter().isIncludeProvidedScope() );
        
        assertFalse( info.getScopeFilter().isIncludeRuntimeScope() );
        assertFalse( info.getScopeFilter().isIncludeTestScope() );
    }
    
    public void test_getModuleSetResolutionRequirements()
        throws ArchiveCreationException
    {
        MockManager mm = new MockManager();
        
        MockControl csControl = MockControl.createControl( AssemblerConfigurationSource.class );
        mm.add( csControl );
        
        AssemblerConfigurationSource cs = (AssemblerConfigurationSource) csControl.getMock();
        
        File rootDir = new File( "root" );
        MavenProject project = createMavenProject( "main-group", "main-artifact", "1", rootDir );
        
        File module1Dir = new File( rootDir, "module-1" );
        MavenProject module1 = createMavenProject( "main-group", "module-1", "1", module1Dir );
        MavenProject module1a = createMavenProject( "group1", "module-1a", "1", new File( module1Dir, "module-1a" ) );
        MavenProject module1b = createMavenProject( "group1.b", "module-1b", "1", new File( module1Dir, "module-1b" ) );
        
        module1.getModel().addModule( module1a.getArtifactId() );
        module1.getModel().addModule( module1b.getArtifactId() );
        
        File module2Dir = new File( rootDir, "module-2" );
        MavenProject module2 = createMavenProject( "main-group", "module-2", "1", module2Dir );
        MavenProject module2a = createMavenProject( "main-group", "module-2a", "1", new File( module2Dir, "module-2a" ) );
        
        module2.getModel().addModule( module2a.getArtifactId() );
        
        project.getModel().addModule( module1.getArtifactId() );
        project.getModel().addModule( module2.getArtifactId() );
        
        List allProjects = new ArrayList();
        allProjects.add( project );
        allProjects.add( module1 );
        allProjects.add( module1a );
        allProjects.add( module1b );
        allProjects.add( module2 );
        allProjects.add( module2a );
        
        cs.getReactorProjects();
        csControl.setReturnValue( allProjects, MockControl.ZERO_OR_MORE );
        
        cs.getProject();
        csControl.setReturnValue( project, MockControl.ZERO_OR_MORE );
        
        ResolutionManagementInfo info = new ResolutionManagementInfo( project );
        
        List moduleSets = new ArrayList();
        
        {
            ModuleSet ms = new ModuleSet();
            ms.addInclude( "*module1*" );
            ms.setIncludeSubModules( false );
            
            ModuleBinaries mb = new ModuleBinaries();
            
            DependencySet ds = new DependencySet();
            ds.setScope( Artifact.SCOPE_COMPILE );
            
            mb.addDependencySet( ds );
            ms.setBinaries( mb );
            moduleSets.add( ms );
        }
        
        {
            ModuleSet ms = new ModuleSet();
            ms.addInclude( "main-group:*" );
            ms.setIncludeSubModules( true );
            
            ModuleBinaries mb = new ModuleBinaries();
            
            DependencySet ds = new DependencySet();
            ds.setScope( Artifact.SCOPE_TEST );
            
            mb.addDependencySet( ds );
            ms.setBinaries( mb );
            moduleSets.add( ms );
        }
        
        mm.replayAll();
        
        DefaultDependencyResolver resolver = new DefaultDependencyResolver();
        resolver.enableLogging( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        
        resolver.getModuleSetResolutionRequirements( moduleSets, info, cs );
        
        assertTrue( info.isResolutionRequired() );
        
        Set enabledProjects = info.getEnabledProjects();
        assertTrue( enabledProjects.contains( project ) );
        
        assertTrue( enabledProjects.contains( module1 ) );
        
        // these should be excluded since sub-modules are not traversable
        assertFalse( enabledProjects.contains( module1a ) );
        assertFalse( enabledProjects.contains( module1b ) );
        
        assertTrue( enabledProjects.contains( module2 ) );
        assertTrue( enabledProjects.contains( module2a ) );
        
        // these are the two we directly set above.
        assertTrue( info.getScopeFilter().isIncludeTestScope() );
        assertTrue( info.getScopeFilter().isIncludeCompileScope() );
        
        // this combination should be implied by the two direct scopes set above.
        assertTrue( info.getScopeFilter().isIncludeRuntimeScope() );
        assertTrue( info.getScopeFilter().isIncludeProvidedScope() );
        assertTrue( info.getScopeFilter().isIncludeSystemScope() );
        
        mm.verifyAll();
    }

    public void test_getRepositoryResolutionRequirements()
    {
        List repositories = new ArrayList();
        
        {
            Repository r = new Repository();
            r.setScope( Artifact.SCOPE_COMPILE );
            repositories.add( r );
        }
        
        {
            Repository r = new Repository();
            r.setScope( Artifact.SCOPE_SYSTEM );
            repositories.add( r );
        }
        
        ResolutionManagementInfo info = new ResolutionManagementInfo( new MavenProject( new Model() ) );
        
        new DefaultDependencyResolver().getRepositoryResolutionRequirements( repositories, info );
        
        assertTrue( info.isResolutionRequired() );
        
        assertTrue( info.getScopeFilter().isIncludeCompileScope() );
        assertTrue( info.getScopeFilter().isIncludeSystemScope() );
        
        assertTrue( info.getScopeFilter().isIncludeProvidedScope() );
        
        assertFalse( info.getScopeFilter().isIncludeRuntimeScope() );
        assertFalse( info.getScopeFilter().isIncludeTestScope() );
    }
    
    public void test_getManagedVersionMap()
        throws InvalidVersionSpecificationException
    {
        MavenProject project = createMavenProject( "group", "artifact", "1", new File( "base" ) );
        
        DependencyManagement dm = new DependencyManagement();
        project.getModel().setDependencyManagement( dm );
        
        Dependency d1 = new Dependency();
        d1.setGroupId( "group" );
        d1.setArtifactId( "dep1" );
        d1.setVersion( "2" );
        d1.setScope( Artifact.SCOPE_PROVIDED );
        
        dm.addDependency( d1 );
        
        Dependency d2 = new Dependency();
        d2.setGroupId( "group" );
        d2.setArtifactId( "dep2" );
        d2.setVersion( "2.1" );
        
        dm.addDependency( d2 );
        
        DefaultDependencyResolver resolver = new DefaultDependencyResolver();
        resolver.setLogger( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        resolver.setArtifactFactory( factory );
        
        Map managedVersionMap = resolver.getManagedVersionMap( project );
        
        Artifact a1 = (Artifact) managedVersionMap.get( d1.getManagementKey() );
        assertNotNull( a1 );
        assertEquals( d1.getVersion(), a1.getVersion() );
        assertEquals( d1.getScope(), a1.getScope() );
        
        Artifact a2 = (Artifact) managedVersionMap.get( d2.getManagementKey() );
        assertNotNull( a2 );
        assertEquals( d2.getVersion(), a2.getVersion() );
        assertNull( a2.getScope() );
    }
    
    public void test_aggregateRemoteArtifactRepositories()
    {
        List externalRepos = new ArrayList();
        
        ArtifactRepository er1 = repoFactory.createArtifactRepository( "test.1", "http://test.com/path", layout, null, null );
        externalRepos.add( er1 );
        
        ArtifactRepository er2 = repoFactory.createArtifactRepository( "test.2", "http://test2.com/path", layout, null, null );
        externalRepos.add( er2 );
        
        List projectRepos = new ArrayList();
        
        ArtifactRepository pr1 = repoFactory.createArtifactRepository( "project.1", "http://test.com/project", layout, null, null );
        projectRepos.add( pr1 );
        
        ArtifactRepository pr2 = repoFactory.createArtifactRepository( "project.2", "http://test2.com/path", layout, null, null );
        projectRepos.add( pr2 );
        
        MavenProject project = createMavenProject( "group", "artifact", "1", new File( "base" ) );
        project.setRemoteArtifactRepositories( projectRepos );
        
        List aggregated = new DefaultDependencyResolver().aggregateRemoteArtifactRepositories( externalRepos, project );
        
        assertRepositoryWithId( er1.getId(), aggregated, true );
        assertRepositoryWithId( er2.getId(), aggregated, true );
        assertRepositoryWithId( pr1.getId(), aggregated, true );
        assertRepositoryWithId( pr2.getId(), aggregated, false );
    }
    
    public void test_manageArtifact()
    {
        Artifact managed = factory.createArtifact( "group", "artifact", "1", Artifact.SCOPE_PROVIDED, "jar" );
        
        Artifact target =
            factory.createArtifact( managed.getGroupId(), managed.getArtifactId(), "2", Artifact.SCOPE_COMPILE,
                                    managed.getType() );

        Artifact target2 =
            factory.createArtifact( "other-group", managed.getArtifactId(), "2", Artifact.SCOPE_COMPILE,
                                    managed.getType() );
        
        Map managedVersions = Collections.singletonMap( managed.getDependencyConflictId(), managed );

        DefaultDependencyResolver resolver =
            new DefaultDependencyResolver().setLogger( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        
        resolver.manageArtifact( target, managedVersions );
        resolver.manageArtifact( target2, managedVersions );
        
        assertEquals( managed.getVersion(), target.getVersion() );
        assertEquals( managed.getScope(), target.getScope() );
        
        assertEquals( "2", target2.getVersion() );
        assertEquals( Artifact.SCOPE_COMPILE, target2.getScope() );
    }
    
    public void test_buildManagedVersionMap_NonTransitiveResolution()
        throws ArtifactResolutionException, ArchiveCreationException, InvalidVersionSpecificationException,
        InvalidDependencyVersionException
    {
        Assembly assembly = new Assembly();
        
        DependencySet ds = new DependencySet();
        ds.setScope( Artifact.SCOPE_PROVIDED );
        ds.setUseTransitiveDependencies( false );
        
        assembly.addDependencySet( ds );
        
        ModuleSet ms = new ModuleSet();
        ModuleBinaries mb = new ModuleBinaries();
        ms.setBinaries( mb );
        
        DependencySet mds = new DependencySet();
        mds.setScope( Artifact.SCOPE_PROVIDED );
        mds.setUseTransitiveDependencies( false );
        
        mb.addDependencySet( mds );
        
        assembly.addModuleSet( ms );
        
        MavenProject project = createMavenProject( "group", "artifact", "1", new File( "base" ) );
        
        Dependency d1 = new Dependency();
        d1.setGroupId( "group.dep" );
        d1.setArtifactId( "dep1" );
        d1.setVersion( "1" );
        d1.setScope( Artifact.SCOPE_COMPILE );
        
        project.getModel().addDependency( d1 );
        
        Dependency d2 = new Dependency();
        d2.setGroupId( "group.dep" );
        d2.setArtifactId( "dep2" );
        d2.setVersion( "1" );
        d2.setScope( Artifact.SCOPE_PROVIDED );
        
        project.getModel().addDependency( d2 );
        
        Dependency d3 = new Dependency();
        d3.setGroupId( "group.dep" );
        d3.setArtifactId( "dep3" );
        d3.setVersion( "1" );
        d3.setScope( Artifact.SCOPE_PROVIDED );
        
        project.getModel().addDependency( d3 );
        
        MavenProject module = createMavenProject( "group", "module", "1", new File( "base/module" ) );
        
        project.getModel().addModule( module.getArtifactId() );
        
        Dependency md = new Dependency();
        md.setGroupId( "group.dep" );
        md.setArtifactId( "dep3" );
        md.setVersion( "2" );
        md.setScope( Artifact.SCOPE_PROVIDED );
        
        module.getModel().addDependency( md );
        
        List allProjects = new ArrayList();
        allProjects.add( project );
        allProjects.add( module );
        
        MockManager mm = new MockManager();
        
        MockControl csControl = MockControl.createControl( AssemblerConfigurationSource.class );
        mm.add( csControl );
        
        AssemblerConfigurationSource cs = (AssemblerConfigurationSource) csControl.getMock();
        
        cs.getProject();
        csControl.setReturnValue( project, MockControl.ZERO_OR_MORE );
        
        cs.getReactorProjects();
        csControl.setReturnValue( allProjects, MockControl.ZERO_OR_MORE );
        
        cs.getRemoteRepositories();
        csControl.setReturnValue( Collections.EMPTY_LIST, MockControl.ZERO_OR_MORE );
        
        mm.replayAll();
        
        DefaultDependencyResolver resolver = new DefaultDependencyResolver();
        resolver.setArtifactFactory( factory );
        resolver.setLogger( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        
        Map managedVersionMap = resolver.buildManagedVersionMap( assembly, cs );
        
        {
            Dependency d = d1;
            Artifact a = (Artifact) managedVersionMap.get( d.getManagementKey() );
            assertNull( a );
        }
        
        {
            Dependency d = d2;
            Artifact a = (Artifact) managedVersionMap.get( d.getManagementKey() );
            assertNotNull( a );
            assertEquals( d.getVersion(), a.getVersion() );
            assertEquals( d.getScope(), a.getScope() );
        }
        
        {
            Dependency d = d3;
            Artifact a = (Artifact) managedVersionMap.get( d.getManagementKey() );
            assertNotNull( a );
            assertEquals( d.getVersion(), a.getVersion() );
            assertEquals( d.getScope(), a.getScope() );
        }
        
        mm.verifyAll();
    }
    
    public void test_buildManagedVersionMap_TransitiveResolution()
        throws ArtifactResolutionException, ArchiveCreationException, InvalidVersionSpecificationException,
        InvalidDependencyVersionException
    {
        Assembly assembly = new Assembly();
        
        DependencySet ds = new DependencySet();
        ds.setScope( Artifact.SCOPE_COMPILE );
        ds.setUseTransitiveDependencies( true );
        
        assembly.addDependencySet( ds );
        
        MavenProject project = createMavenProject( "group", "artifact", "1", new File( "base" ) );
        
        Dependency d1 = new Dependency();
        d1.setGroupId( "group.dep" );
        d1.setArtifactId( "dep1" );
        d1.setVersion( "1" );
        d1.setScope( Artifact.SCOPE_COMPILE );
        
        project.getModel().addDependency( d1 );
        
        Dependency d2 = new Dependency();
        d2.setGroupId( "group.dep" );
        d2.setArtifactId( "dep2" );
        d2.setVersion( "1" );
        d2.setScope( Artifact.SCOPE_COMPILE );
        final Artifact a2 = factory.createArtifact( d2.getGroupId(), d2.getArtifactId(), d2.getVersion(), d2.getScope(), "jar" );
        
        project.getModel().addDependency( d2 );
        
        Dependency d3 = new Dependency();
        d3.setGroupId( "group.dep" );
        d3.setArtifactId( "dep3" );
        d3.setVersion( "1" );
        d3.setScope( Artifact.SCOPE_COMPILE );
        
        project.getModel().addDependency( d3 );
        
        final Artifact a2a = factory.createArtifact( d3.getGroupId(), d3.getArtifactId(), "2", Artifact.SCOPE_RUNTIME, "jar" );
        
        MockManager mm = new MockManager();
        
        MockControl msControl = MockControl.createControl( ArtifactMetadataSource.class );
        mm.add( msControl );
        
        ArtifactMetadataSource ms = (ArtifactMetadataSource) msControl.getMock();
        
        try
        {
            ms.retrieve( null, null, null );
        }
        catch ( ArtifactMetadataRetrievalException e )
        {
        }
        
        msControl.setDefaultReturnValue( new ResolutionGroup( null, Collections.EMPTY_SET, Collections.EMPTY_LIST ) );
        msControl.setMatcher( new ArgumentsMatcher()
        {
            public boolean matches( Object[] expected, Object[] actual )
            {
                Artifact a = (Artifact) actual[0];

                return a2.getArtifactId().equals( a.getArtifactId() );
            }

            public String toString( Object[] args )
            {
                return "with artifact: " + args[0] ;
            }
            
        } );
        msControl.setReturnValue( new ResolutionGroup( a2, Collections.singleton( a2a ), Collections.EMPTY_LIST ) );
        
        
        MockControl csControl = MockControl.createControl( AssemblerConfigurationSource.class );
        mm.add( csControl );
        
        AssemblerConfigurationSource cs = (AssemblerConfigurationSource) csControl.getMock();
        
        cs.getProject();
        csControl.setReturnValue( project, MockControl.ZERO_OR_MORE );
        
        String tmpDir = System.getProperty( "java.io.tmpdir" );
        ArtifactRepository lr = repoFactory.createArtifactRepository( "local", "file://" + tmpDir, layout, null, null );
        
        cs.getLocalRepository();
        csControl.setReturnValue( lr, MockControl.ZERO_OR_MORE );
        
        cs.getRemoteRepositories();
        csControl.setReturnValue( Collections.EMPTY_LIST, MockControl.ZERO_OR_MORE );
        
        mm.replayAll();
        
        DefaultDependencyResolver resolver = new DefaultDependencyResolver();
        resolver.setArtifactMetadataSource( ms );
        resolver.setArtifactCollector( collector );
        resolver.setArtifactFactory( factory );
        resolver.setLogger( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        
        Map managedVersionMap = resolver.buildManagedVersionMap( assembly, cs );
        
        {
            Dependency d = d1;
            Artifact a = (Artifact) managedVersionMap.get( d.getManagementKey() );
            assertNotNull( a );
            assertEquals( d.getVersion(), a.getVersion() );
            assertEquals( d.getScope(), a.getScope() );
        }
        
        {
            Dependency d = d2;
            Artifact a = (Artifact) managedVersionMap.get( d.getManagementKey() );
            assertNotNull( a );
            assertEquals( d.getVersion(), a.getVersion() );
            assertEquals( d.getScope(), a.getScope() );
        }
        
        {
            Dependency d = d3;
            Artifact a = (Artifact) managedVersionMap.get( d.getManagementKey() );
            assertNotNull( a );
            assertEquals( d.getVersion(), a.getVersion() );
            assertEquals( d.getScope(), a.getScope() );
        }
        
        mm.verifyAll();
    }
    
    private void assertRepositoryWithId( String repoId, List repos, boolean shouldExist )
    {
        if ( ( repos == null || repos.isEmpty() ) )
        {
            if ( shouldExist )
            {
                fail( "Repository with id: " + repoId + " should be present, but repository list is null or empty." );
            }
        }
        else
        {
            boolean found = false;
            for ( Iterator it = repos.iterator(); it.hasNext(); )
            {
                ArtifactRepository repo = (ArtifactRepository) it.next();
                if ( repoId.equals( repo.getId() ) )
                {
                    found = true;
                    break;
                }
            }
            
            if ( shouldExist )
            {
                assertTrue( "Repository with id: " + repoId + " should be present in repository list.", found );
            }
            else
            {
                assertFalse( "Repository with id: " + repoId + " should NOT be present in repository list.", found );
            }
        }
    }

    private MavenProject createMavenProject( String groupId, String artifactId, String version, File basedir )
    {
        Model model = new Model();
        
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        model.setPackaging( "pom" );
        
        MavenProject project = new MavenProject( model );
        
        Artifact pomArtifact = factory.createProjectArtifact( groupId, artifactId, version );
        project.setArtifact( pomArtifact );
        
        project.setFile( new File( basedir, "pom.xml" ) );
        
        return project;
    }

}
