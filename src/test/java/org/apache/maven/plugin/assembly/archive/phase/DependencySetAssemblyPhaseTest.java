package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddArtifactTask;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.MockControl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

public class DependencySetAssemblyPhaseTest
    extends TestCase
{

    private MockManager mockManager = new MockManager();

    public void testExecute_ShouldAddOneDependencyFromProject()
        throws AssemblyFormattingException, ArchiveCreationException, IOException, InvalidAssemblerConfigurationException
    {
        String outputLocation = "/out";

        MavenProject project = new MavenProject( new Model() );

        DependencySet ds = new DependencySet();
        ds.setOutputDirectory( outputLocation );
        ds.setOutputFileNameMapping( "artifact" );
        ds.setUnpack( false );
        ds.setScope( Artifact.SCOPE_COMPILE );
        ds.setFileMode( Integer.toString( 8, 8 ) );
        
        Assembly assembly = new Assembly();
        
        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( false );
        assembly.addDependencySet( ds );
        
        MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mockManager );

        macTask.expectArtifactGetFile();
        macTask.expectArtifactGetScope( Artifact.SCOPE_COMPILE );
        macTask.expectGetClassifier( null );
        macTask.expectIsSnapshot( false );
        macTask.expectGetArtifactHandler();

        macTask.expectAddFile( "out/artifact", 8 );

        project.setArtifacts( Collections.singleton( macTask.artifact ) );

        MockAndControlForConfigSource macCS = new MockAndControlForConfigSource();

        macCS.enableGetProject( project );
        macCS.enableGetFinalName( "final-name" );
        macCS.enableGetRepositories( null, Collections.EMPTY_LIST );
        
        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );
        
        MockAndControlForProjectBuilder macPB = new MockAndControlForProjectBuilder();
        
        MavenProject depProject = new MavenProject( new Model() );
        macPB.expectBuildFromRepository( depProject );

        mockManager.replayAll();

        createPhase( macPB.projectBuilder, logger ).execute( assembly, macTask.archiver, macCS.configSource );

        mockManager.verifyAll();
    }

    public void testExecute_ShouldNotAddDependenciesWhenProjectHasNone()
        throws AssemblyFormattingException, ArchiveCreationException, IOException, InvalidAssemblerConfigurationException
    {
        Assembly assembly = new Assembly();
        
        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( false );
        
        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );
        
        MockAndControlForProjectBuilder macPB = new MockAndControlForProjectBuilder();
        
        MockAndControlForConfigSource macCS = new MockAndControlForConfigSource();
        macCS.enableGetProject( null );

        mockManager.replayAll();

        createPhase( macPB.projectBuilder, logger ).execute( assembly, null, macCS.configSource );

        mockManager.verifyAll();
    }

    private DependencySetAssemblyPhase createPhase( MavenProjectBuilder projectBuilder, Logger logger )
    {
        DependencySetAssemblyPhase phase = new DependencySetAssemblyPhase( projectBuilder );

        phase.enableLogging( logger );

        return phase;
    }

    private final class MockAndControlForConfigSource
    {
        AssemblerConfigurationSource configSource;

        MockControl control;

        MockAndControlForConfigSource()
        {
            control = MockControl.createControl( AssemblerConfigurationSource.class );
            mockManager.add( control );

            configSource = ( AssemblerConfigurationSource ) control.getMock();
        }

        public void enableGetRepositories( ArtifactRepository localRepo, List remoteRepos )
        {
            configSource.getLocalRepository();
            control.setReturnValue( localRepo, MockControl.ONE_OR_MORE );
            
            configSource.getRemoteRepositories();
            control.setReturnValue( remoteRepos, MockControl.ONE_OR_MORE );
        }
        
        public void enableGetFinalName( String finalName )
        {
            configSource.getFinalName();
            control.setReturnValue( finalName, MockControl.ONE_OR_MORE );
        }

        void enableGetProject( MavenProject project )
        {
            configSource.getProject();
            control.setReturnValue( project, MockControl.ONE_OR_MORE );
        }
    }

    private final class MockAndControlForProjectBuilder
    {
        MavenProjectBuilder projectBuilder;
        
        MockControl control;
        
        public MockAndControlForProjectBuilder()
        {
            control = MockControl.createControl( MavenProjectBuilder.class );
            mockManager.add( control );
            
            projectBuilder = ( MavenProjectBuilder ) control.getMock();
        }

        public void expectBuildFromRepository( MavenProject project )
        {
            try
            {
                projectBuilder.buildFromRepository( null, null, null );
                control.setMatcher( MockControl.ALWAYS_MATCHER );
                control.setReturnValue( project, MockControl.ONE_OR_MORE );
            }
            catch ( ProjectBuildingException e )
            {
                Assert.fail( "should never happen" );
            }
        }
    }

}
