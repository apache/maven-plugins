package org.apache.maven.plugin.assembly.archive.task.testutils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.easymock.MockControl;

import java.io.File;
import java.util.List;

public class MockAndControlForArtifact
{

    public Artifact artifact;

    public MockControl artifactCtl;

    public ArtifactHandler artifactHandler;

    public MockControl artifactHandlerCtl;

    private final MockManager mockManager;

    private final String classifier;

    public MockAndControlForArtifact( MockManager mockManager )
    {
        this( mockManager, null );
    }

    public MockAndControlForArtifact( MockManager mockManager, String classifier )
    {
        this.mockManager = mockManager;
        this.classifier = classifier;

        artifactCtl = MockControl.createControl( Artifact.class );
        mockManager.add( artifactCtl );

        artifact = ( Artifact ) artifactCtl.getMock();

        enableDefaultExpectations();
    }

    private void enableDefaultExpectations()
    {
        artifact.getClassifier();
        artifactCtl.setReturnValue( classifier, MockControl.ZERO_OR_MORE );
    }

    public void expectGetId( String id )
    {
        artifact.getId();
        artifactCtl.setReturnValue( id, MockControl.ONE_OR_MORE );
    }

    public void expectGetDependencyTrail( List dependencyTrail )
    {
        artifact.getDependencyTrail();
        artifactCtl.setReturnValue( dependencyTrail, MockControl.ONE_OR_MORE );
    }

    public void expectGetDependencyConflictId( String conflictId )
    {
        artifact.getDependencyConflictId();
        artifactCtl.setReturnValue( conflictId, MockControl.ONE_OR_MORE );
    }

    public void expectGetArtifactId( String artifactId )
    {
        artifact.getArtifactId();
        artifactCtl.setReturnValue( artifactId, MockControl.ONE_OR_MORE );
    }

    public void expectGetGroupId( String groupId )
    {
        artifact.getGroupId();
        artifactCtl.setReturnValue( groupId, MockControl.ONE_OR_MORE );
    }

    public void expectGetScope( String scope )
    {
        artifact.getScope();
        artifactCtl.setReturnValue( scope, MockControl.ONE_OR_MORE );
    }

    public void expectIsSnapshot( boolean isSnapshot )
    {
        artifact.isSnapshot();
        artifactCtl.setReturnValue( isSnapshot, MockControl.ONE_OR_MORE );
    }

    public void expectGetClassifier( String classifier )
    {
        artifact.getClassifier();
        artifactCtl.setReturnValue( classifier, MockControl.ONE_OR_MORE );
    }

    public void expectGetArtifactHandler()
    {
        artifactHandlerCtl = MockControl.createControl( ArtifactHandler.class );
        mockManager.add( artifactHandlerCtl );

        artifactHandler = ( ArtifactHandler ) artifactHandlerCtl.getMock();

        artifact.getArtifactHandler();
        artifactCtl.setReturnValue( artifactHandler, MockControl.ONE_OR_MORE );
    }

    public void expectGetDependencyConflictId( String groupId, String artifactId, String packaging )
    {
        artifact.getDependencyConflictId();
        artifactCtl.setReturnValue( groupId + ":" + artifactId + ":" + packaging, MockControl.ONE_OR_MORE );
    }

    public void expectGetType( String type )
    {
        artifact.getType();
        artifactCtl.setReturnValue( type, MockControl.ONE_OR_MORE );
    }

    public void expectGetFile( File artifactFile )
    {
        artifact.getFile();
        artifactCtl.setReturnValue( artifactFile, MockControl.ONE_OR_MORE );
    }
}
