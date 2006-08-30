package org.apache.maven.plugin.assembly.archive.task.testutils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

public class MockAndControlForAddDependencySetsTask
{

    public MockControl artifactCtl;

    public Artifact artifact;

    public File artifactFile;

    public Archiver archiver;

    public MockControl archiverCtl;
    
    public AssemblerConfigurationSource configSource;
    
    public MockControl configSourceCtl;
    
    public ArtifactHandler artifactHandler;
    
    public MockControl artifactHandlerCtl;
    
    public DependencyResolver dependencyResolver;
    
    public MockControl dependencyResolverCtl;
    
    public MavenProjectBuilder projectBuilder;
    
    public MockControl projectBuilderCtl;
    
    public MockAndControlForAddDependencySetsTask( MockManager mockManager )
    {
        artifactCtl = MockControl.createControl( Artifact.class );
        mockManager.add( artifactCtl );

        artifact = ( Artifact ) artifactCtl.getMock();
        
        artifactHandlerCtl = MockControl.createControl( ArtifactHandler.class );
        mockManager.add( artifactHandlerCtl );
        
        artifactHandler = (ArtifactHandler) artifactHandlerCtl.getMock();
        
        archiverCtl = MockControl.createControl( Archiver.class );
        mockManager.add( archiverCtl );

        archiver = (Archiver) archiverCtl.getMock();
        
        configSourceCtl = MockControl.createControl( AssemblerConfigurationSource.class );
        mockManager.add( configSourceCtl );
        
        configSource = (AssemblerConfigurationSource) configSourceCtl.getMock();
        
        dependencyResolverCtl = MockControl.createControl( DependencyResolver.class );
        mockManager.add( dependencyResolverCtl );
        
        dependencyResolver = ( DependencyResolver ) dependencyResolverCtl.getMock();
        
        projectBuilderCtl = MockControl.createControl( MavenProjectBuilder.class );
        mockManager.add( projectBuilderCtl );
        
        projectBuilder = ( MavenProjectBuilder ) projectBuilderCtl.getMock();
    }
    
    public void expectGetArtifactHandler()
    {
        artifact.getArtifactHandler();
        artifactCtl.setReturnValue( artifactHandler, MockControl.ONE_OR_MORE );
    }
    
    public void expectGetClassifier( String classifier )
    {
        artifact.getClassifier();
        artifactCtl.setReturnValue( classifier, MockControl.ONE_OR_MORE );
    }
    
    public void expectGetFinalName( String finalName )
    {
        configSource.getFinalName();
        configSourceCtl.setReturnValue( finalName, MockControl.ONE_OR_MORE );
    }
    
    public void expectArtifactGetFile() throws IOException
    {
        expectArtifactGetFile( true );
    }

    public void expectArtifactGetFile( boolean createTempFile ) throws IOException
    {
        if ( createTempFile )
        {
            artifactFile = File.createTempFile( "add-artifact-task.test.", ".jar" );
        }

        artifact.getFile();
        
        artifactCtl.setReturnValue( artifactFile, MockControl.ZERO_OR_MORE );
    }

    public void expectAddArchivedFileSet( String outputLocation, String[] includes, String[] excludes )
    {
        try
        {
            archiver.addArchivedFileSet( artifactFile, outputLocation, includes, excludes );
            
            if ( includes != null || excludes != null )
            {
                archiverCtl.setMatcher( MockControl.ARRAY_MATCHER );
            }
            
            archiverCtl.setVoidCallable( MockControl.ONE_OR_MORE );
        }
        catch ( ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }
    
    public void expectModeChange( int originalDirMode, int originalFileMode, int dirMode, int fileMode,
                                            int numberOfChanges )
    {
        archiver.getDefaultDirectoryMode();
        archiverCtl.setReturnValue( originalDirMode );

        archiver.getDefaultFileMode();
        archiverCtl.setReturnValue( originalFileMode );

        // one of the changes will occur below, when we restore the original mode.
        if ( numberOfChanges > 1 )
        {
            for( int i = 1; i< numberOfChanges; i++ )
            {
                archiver.setDefaultDirectoryMode( dirMode );
                archiver.setDefaultFileMode( fileMode );
            }
        }

        archiver.setDefaultDirectoryMode( originalDirMode );
        archiver.setDefaultFileMode( originalFileMode );
    }

    public void expectAddFile( String outputLocation )
    {
        try
        {
            archiver.addFile( artifactFile, outputLocation );
            archiverCtl.setMatcher( MockControl.ALWAYS_MATCHER );
            archiverCtl.setVoidCallable( MockControl.ONE_OR_MORE );
        }
        catch ( ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }
    
    public void expectAddFile( String outputLocation, int fileMode )
    {
        try
        {
            archiver.addFile( artifactFile, outputLocation, fileMode );
            archiverCtl.setVoidCallable( MockControl.ONE_OR_MORE );
        }
        catch ( ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }
    
    public void expectAddFile( File file, String outputLocation, int fileMode )
    {
        try
        {
            archiver.addFile( file, outputLocation, fileMode );
            archiverCtl.setVoidCallable( MockControl.ONE_OR_MORE );
        }
        catch ( ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }
    
    public void expectArtifactGetScope( String scope )
    {
        artifact.getScope();
        artifactCtl.setReturnValue( scope, MockControl.ONE_OR_MORE );
    }

    public void expectGetProject( MavenProject project )
    {
        configSource.getProject();
        configSourceCtl.setReturnValue( project, MockControl.ONE_OR_MORE );
    }

    public void expectGetReactorProjects( List projects )
    {
        configSource.getReactorProjects();
        configSourceCtl.setReturnValue( projects, MockControl.ONE_OR_MORE );
    }

    public void expectArtifactGetDependencyConflictId( String dependencyConflictId )
    {
        artifact.getDependencyConflictId();
        artifactCtl.setReturnValue( dependencyConflictId, MockControl.ONE_OR_MORE );
    }

    public void expectIsSnapshot( boolean isSnapshot )
    {
        artifact.isSnapshot();
        artifactCtl.setReturnValue( isSnapshot, MockControl.ONE_OR_MORE );
    }

    public void expectArtifactGetType( String type )
    {
        artifact.getType();
        artifactCtl.setReturnValue( type, MockControl.ONE_OR_MORE );
    }

    public void expectArtifactGetArtifactId( String artifactId )
    {
        artifact.getArtifactId();
        artifactCtl.setReturnValue( artifactId, MockControl.ONE_OR_MORE );
    }

    public void expectCSGetFinalName( String finalName )
    {
        configSource.getFinalName();
        configSourceCtl.setReturnValue( finalName, MockControl.ONE_OR_MORE );
    }

    public void expectCSGetRepositories( ArtifactRepository localRepo, List remoteRepos )
    {
        configSource.getLocalRepository();
        configSourceCtl.setReturnValue( localRepo, MockControl.ONE_OR_MORE );

        configSource.getRemoteRepositories();
        configSourceCtl.setReturnValue( remoteRepos, MockControl.ONE_OR_MORE );
    }

    public void expectBuildFromRepository( MavenProject project )
    {
        try
        {
            projectBuilder.buildFromRepository( null, null, null );
            projectBuilderCtl.setMatcher( MockControl.ALWAYS_MATCHER );
            projectBuilderCtl.setReturnValue( project, MockControl.ONE_OR_MORE );
        }
        catch ( ProjectBuildingException e )
        {
            Assert.fail( "should never happen" );
        }
    }

    public void expectResolveDependencies( Set resolvedArtifacts )
    {
        try
        {
            dependencyResolver.resolveDependencies( null, null, null, null );
        }
        catch ( ArtifactResolutionException e )
        {
            Assert.fail( "should never happen!" );
        }
        catch ( ArtifactNotFoundException e )
        {
            Assert.fail( "should never happen!" );
        }
        catch ( InvalidDependencyVersionException e )
        {
            Assert.fail( "should never happen!" );
        }
        
        dependencyResolverCtl.setMatcher( MockControl.ALWAYS_MATCHER );
        dependencyResolverCtl.setReturnValue( resolvedArtifacts, MockControl.ONE_OR_MORE );
    }

}
