package org.apache.maven.plugin.assembly.archive.task.testutils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.assembly.testutils.MockManager;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

public class MockAndControlForAddArtifactTask
{

    public MockControl artifactCtl;

    public Artifact artifact;

    public File artifactFile;

    public Archiver archiver;

    public MockControl archiverCtl;
    
    public MockAndControlForAddArtifactTask( MockManager mockManager )
        throws IOException
    {
        artifactCtl = MockControl.createControl( Artifact.class );
        mockManager.add( artifactCtl );

        artifact = ( Artifact ) artifactCtl.getMock();
        
        archiverCtl = MockControl.createControl( Archiver.class );
        mockManager.add( archiverCtl );

        archiver = (Archiver) archiverCtl.getMock();
    }

    public void expectArtifactGetFile() throws IOException
    {
        artifactFile = File.createTempFile( "add-artifact-task.test.", ".jar" );

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
    
    public void expectArtifactGetArtifactHandler( ArtifactHandler artifactHandler )
    {
        artifact.getArtifactHandler();
        artifactCtl.setReturnValue( artifactHandler, MockControl.ONE_OR_MORE );
    }

    public void expectArtifactGetClassifier( String classifier )
    {
        artifact.getClassifier();
        artifactCtl.setReturnValue( classifier, MockControl.ONE_OR_MORE );
    }

    public void expectArtifactGetScope( String scope )
    {
        artifact.getScope();
        artifactCtl.setReturnValue( scope, MockControl.ONE_OR_MORE );
    }

}
