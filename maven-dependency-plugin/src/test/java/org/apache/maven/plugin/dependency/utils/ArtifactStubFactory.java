package org.apache.maven.plugin.dependency.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;

public class ArtifactStubFactory
{
    File workingDir;

    boolean createFiles;

    public ArtifactStubFactory( File workingDir, boolean createFiles )
    {
        this.workingDir = new File( workingDir, "localTestRepo" );
        this.createFiles = createFiles;
    }

    public Artifact createArtifact( String groupId, String artifactId, String version )
    {
        return createArtifact( groupId, artifactId, version, Artifact.SCOPE_COMPILE, "jar", "" );
    }

    public Artifact createArtifact( String groupId, String artifactId, String version, String scope )
    {
        return createArtifact( groupId, artifactId, version, scope, "jar", "" );
    }

    public Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type,
                                   String classifier )
    {
        VersionRange vr = VersionRange.createFromVersion( version );
        return createArtifact( groupId, artifactId, vr, scope, type, classifier, false );
    }

    public Artifact createArtifact( String groupId, String artifactId, VersionRange versionRange, String scope,
                                   String type, String classifier, boolean optional )
    {
        ArtifactHandler ah = new DefaultArtifactHandler();
        Artifact artifact = new DefaultArtifact( groupId, artifactId, versionRange, scope, type, classifier, ah,
                                                 optional );
        if ( createFiles )
        {
            setArtifactFile( artifact );
        }
        return artifact;
    }

    public void setArtifactFile( Artifact artifact )
    {
        String fileName = DependencyUtil.getFormattedFileName( artifact, false );

        File theFile = new File( this.workingDir, fileName );
        theFile.getParentFile().mkdirs();
        try
        {
            theFile.createNewFile();
        }
        catch ( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        artifact.setFile( theFile );
    }

    public Artifact getReleaseArtifact()
    {
        return createArtifact( "groupId", "release", "1.0" );
    }

    public Artifact getSnapshotArtifact()
    {
        return createArtifact( "groupId", "snapshot", "2.0-SNAPSHOT" );
    }

    public Set getReleaseAndSnapshotArtifacts()
    {
        Set set = new HashSet();
        set.add( getReleaseArtifact() );
        set.add( getSnapshotArtifact() );
        return set;
    }

    public Set getScopedArtifacts()
    {
        Set set = new HashSet();
        set.add( createArtifact( "g", "compile", "1.0", Artifact.SCOPE_COMPILE ) );
        set.add( createArtifact( "g", "provided", "1.0", Artifact.SCOPE_PROVIDED ) );
        set.add( createArtifact( "g", "test", "1.0", Artifact.SCOPE_TEST ) );
        set.add( createArtifact( "g", "runtime", "1.0", Artifact.SCOPE_RUNTIME ) );
        set.add( createArtifact( "g", "system", "1.0", Artifact.SCOPE_SYSTEM ) );
        return set;
    }

    public Set getTypedArtifacts()
    {
        Set set = new HashSet();
        set.add( createArtifact( "g", "a", "1.0", Artifact.SCOPE_COMPILE, "war", null ) );
        set.add( createArtifact( "g", "b", "1.0", Artifact.SCOPE_COMPILE, "jar", null ) );
        set.add( createArtifact( "g", "c", "1.0", Artifact.SCOPE_COMPILE, "sources", null ) );
        set.add( createArtifact( "g", "d", "1.0", Artifact.SCOPE_COMPILE, "zip", null ) );
        return set;
    }

    public Set getMixedArtifacts()
    {
        Set set = new HashSet();
        set.addAll( getTypedArtifacts() );
        set.addAll( getScopedArtifacts() );
        set.addAll( getReleaseAndSnapshotArtifacts() );
        return set;
    }

    /**
     * @return Returns the createFiles.
     */
    public boolean isCreateFiles()
    {
        return this.createFiles;
    }

    /**
     * @param createFiles
     *            The createFiles to set.
     */
    public void setCreateFiles( boolean createFiles )
    {
        this.createFiles = createFiles;
    }

    /**
     * @return Returns the workingDir.
     */
    public File getWorkingDir()
    {
        return this.workingDir;
    }

    /**
     * @param workingDir
     *            The workingDir to set.
     */
    public void setWorkingDir( File workingDir )
    {
        this.workingDir = workingDir;
    }
}
