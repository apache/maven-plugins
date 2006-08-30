package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddDependencySetsTask;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.IOException;
import java.util.Collections;

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
        
        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mockManager );

        macTask.expectArtifactGetFile();
        macTask.expectArtifactGetType( "jar" );
        macTask.expectGetClassifier( null );
        macTask.expectIsSnapshot( false );
        macTask.expectGetArtifactHandler();
        
        macTask.expectCSGetRepositories( null, null );
        macTask.expectResolveDependencies( Collections.singleton( macTask.artifact ) );

        macTask.expectAddFile( "out/artifact", 8 );

        project.setArtifacts( Collections.singleton( macTask.artifact ) );

        macTask.expectGetProject( project );
        macTask.expectCSGetFinalName( "final-name" );
        
        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );
        
        MavenProject depProject = new MavenProject( new Model() );
        
        macTask.expectBuildFromRepository( depProject );

        mockManager.replayAll();

        createPhase( macTask, logger ).execute( assembly, macTask.archiver, macTask.configSource );

        mockManager.verifyAll();
    }

    public void testExecute_ShouldNotAddDependenciesWhenProjectHasNone()
        throws AssemblyFormattingException, ArchiveCreationException, IOException, InvalidAssemblerConfigurationException
    {
        Assembly assembly = new Assembly();
        
        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( false );
        
        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );
        
        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mockManager );

        macTask.expectGetProject( null );

        mockManager.replayAll();

        createPhase( macTask, logger ).execute( assembly, null, macTask.configSource );

        mockManager.verifyAll();
    }

    private DependencySetAssemblyPhase createPhase( MockAndControlForAddDependencySetsTask macTask, Logger logger )
    {
        MavenProjectBuilder projectBuilder = null;
        DependencyResolver dependencyResolver = null;
        
        if ( macTask != null )
        {
            projectBuilder = macTask.projectBuilder;
            dependencyResolver = macTask.dependencyResolver;
        }
        
        DependencySetAssemblyPhase phase = new DependencySetAssemblyPhase( projectBuilder, dependencyResolver, logger );

        phase.enableLogging( logger );

        return phase;
    }

}
