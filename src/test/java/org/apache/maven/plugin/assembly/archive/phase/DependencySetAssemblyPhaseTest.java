package org.apache.maven.plugin.assembly.archive.phase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.task.testutils.ArtifactMock;
import org.apache.maven.plugin.assembly.archive.task.testutils.MockAndControlForAddDependencySetsTask;
import org.apache.maven.plugin.assembly.artifact.DependencyResolver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
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

        MavenProject project = newMavenProject( "group", "project", "0" );

        ArtifactMock projectArtifactMock = new ArtifactMock( mockManager, "group", "project", "0", "jar", false );

        project.setArtifact( projectArtifactMock.getArtifact() );

        DependencySet ds = new DependencySet();
        ds.setUseProjectArtifact( false );
        ds.setOutputDirectory( outputLocation );
        ds.setOutputFileNameMapping( "${artifact.artifactId}" );
        ds.setUnpack( false );
        ds.setScope( Artifact.SCOPE_COMPILE );
        ds.setFileMode( Integer.toString( 10, 8 ) );

        Assembly assembly = new Assembly();

        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( false );
        assembly.addDependencySet( ds );

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mockManager, project );

        ArtifactMock artifactMock = new ArtifactMock( mockManager, "group", "dep", "1", "jar", false );
        File artifactFile = artifactMock.setNewFile();

        System.out.println( "On test setup, hashcode for dependency artifact: " + artifactMock.getArtifact().hashCode() );

        macTask.expectCSGetRepositories( null, null );
        macTask.expectResolveDependencies( Collections.singleton( artifactMock.getArtifact() ) );

        macTask.expectAddFile( artifactFile, "out/dep", 10 );

        project.setArtifacts( Collections.singleton( artifactMock.getArtifact() ) );

        macTask.expectCSGetFinalName( "final-name" );

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        MavenProject depProject = newMavenProject( "group", "dep", "1" );

        macTask.expectBuildFromRepository( depProject );

        mockManager.replayAll();

        createPhase( macTask, logger ).execute( assembly, macTask.archiver, macTask.configSource );

        mockManager.verifyAll();
    }

    private MavenProject newMavenProject( String groupId, String artifactId, String version )
    {
        Model model = new Model();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );

        return new MavenProject( model );
    }

    public void testExecute_ShouldNotAddDependenciesWhenProjectHasNone()
        throws AssemblyFormattingException, ArchiveCreationException, IOException, InvalidAssemblerConfigurationException
    {
        Assembly assembly = new Assembly();

        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( false );

        Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        MockAndControlForAddDependencySetsTask macTask = new MockAndControlForAddDependencySetsTask( mockManager, null );

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
