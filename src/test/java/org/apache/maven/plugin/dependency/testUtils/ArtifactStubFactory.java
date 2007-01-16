package org.apache.maven.plugin.dependency.testUtils;

/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.dependency.fromConfiguration.ArtifactItem;
import org.apache.maven.plugin.dependency.testUtils.stubs.DefaultArtifactHandlerStub;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.dependency.utils.SilentLog;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.util.FileUtils;

public class ArtifactStubFactory
{
    File workingDir;

    boolean createFiles;

    File srcFile;

    boolean createUnpackableFile;

    ArchiverManager archiverManager;

    public ArtifactStubFactory( File workingDir, boolean createFiles )
    {
        this.workingDir = new File( workingDir, "localTestRepo" );
        this.createFiles = createFiles;
    }

    public void setUnpackableFile( ArchiverManager archiverManager )
    {
        this.createUnpackableFile = true;
        this.archiverManager = archiverManager;
    }

    public Artifact createArtifact( String groupId, String artifactId, String version )
        throws IOException
    {
        return createArtifact( groupId, artifactId, version, Artifact.SCOPE_COMPILE, "jar", "" );
    }

    public Artifact createArtifact( String groupId, String artifactId, String version, String scope )
        throws IOException
    {
        return createArtifact( groupId, artifactId, version, scope, "jar", "" );
    }

    public Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type,
                                   String classifier )
        throws IOException
    {
        VersionRange vr = VersionRange.createFromVersion( version );
        return createArtifact( groupId, artifactId, vr, scope, type, classifier, false );
    }

    public Artifact createArtifact( String groupId, String artifactId, VersionRange versionRange, String scope,
                                   String type, String classifier, boolean optional )
        throws IOException
    {
        ArtifactHandler ah = new DefaultArtifactHandlerStub(type,classifier);
        
        Artifact artifact = new DefaultArtifact( groupId, artifactId, versionRange, scope, type, classifier, ah,
                                                 optional );
        if ( createFiles )
        {
            setArtifactFile( artifact );
        }
        return artifact;
    }

    /*
     * Creates a file that can be copied or unpacked based on the passed in
     * artifact
     */
    public void setArtifactFile( Artifact artifact )
        throws IOException
    {
        String fileName = DependencyUtil.getFormattedFileName( artifact, false );

        File theFile = new File( this.workingDir, fileName );
        theFile.getParentFile().mkdirs();

        if ( srcFile == null )
        {
            theFile.createNewFile();
        }
        else if ( createUnpackableFile )
        {
            try
            {
                createUnpackableFile( artifact, theFile );
            }
            catch ( NoSuchArchiverException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch ( ArchiverException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch ( IOException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else
        {
            FileUtils.copyFile( srcFile, theFile );
        }

        artifact.setFile( theFile );
    }

    static public String getUnpackableFileName( Artifact artifact )
    {
        return "" + artifact.getGroupId() + "-" + artifact.getArtifactId() + "-" + artifact.getVersion() + "-"
            + artifact.getClassifier() + "-" + artifact.getType() + ".txt";
    }

    public void createUnpackableFile( Artifact artifact, File destFile )
        throws NoSuchArchiverException, ArchiverException, IOException
    {
        Archiver archiver = archiverManager.getArchiver( destFile );

        archiver.setDestFile( destFile );
        archiver.addFile( srcFile, getUnpackableFileName( artifact ) );

        try
        {
            DependencyTestUtils.setVariableValueToObject( archiver, "logger", new SilentLog() );
        }
        catch ( IllegalAccessException e )
        {
            System.out.println( "Unable to override logger with silent log." );
            e.printStackTrace();
        }
        if ( archiver instanceof WarArchiver )
        {
            WarArchiver war = (WarArchiver) archiver;
            // the use of this is counter-intuitive:
            // http://jira.codehaus.org/browse/PLX-286
            war.setIgnoreWebxml( false );
        }
        archiver.createArchive();
    }

    public Artifact getReleaseArtifact()
        throws IOException
    {
        return createArtifact( "testGroupId", "release", "1.0" );
    }

    public Artifact getSnapshotArtifact()
        throws IOException
    {
        return createArtifact( "testGroupId", "snapshot", "2.0-SNAPSHOT" );
    }

    public Set getReleaseAndSnapshotArtifacts()
        throws IOException
    {
        Set set = new HashSet();
        set.add( getReleaseArtifact() );
        set.add( getSnapshotArtifact() );
        return set;
    }

    public Set getScopedArtifacts()
        throws IOException
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
        throws IOException
    {
        Set set = new HashSet();
        set.add( createArtifact( "g", "a", "1.0", Artifact.SCOPE_COMPILE, "war", null ) );
        set.add( createArtifact( "g", "b", "1.0", Artifact.SCOPE_COMPILE, "jar", null ) );
        set.add( createArtifact( "g", "c", "1.0", Artifact.SCOPE_COMPILE, "sources", null ) );
        set.add( createArtifact( "g", "d", "1.0", Artifact.SCOPE_COMPILE, "zip", null ) );
        return set;
    }

    public Set getClassifiedArtifacts()
        throws IOException
    {
        Set set = new HashSet();
        set.add( createArtifact( "g", "a", "1.0", Artifact.SCOPE_COMPILE, "jar", "one" ) );
        set.add( createArtifact( "g", "b", "1.0", Artifact.SCOPE_COMPILE, "jar", "two" ) );
        set.add( createArtifact( "g", "c", "1.0", Artifact.SCOPE_COMPILE, "jar", "three" ) );
        set.add( createArtifact( "g", "d", "1.0", Artifact.SCOPE_COMPILE, "jar", "four" ) );
        return set;
    }

    public Set getTypedArchiveArtifacts()
        throws IOException
    {
        Set set = new HashSet();
        set.add( createArtifact( "g", "a", "1.0", Artifact.SCOPE_COMPILE, "war", null ) );
        set.add( createArtifact( "g", "b", "1.0", Artifact.SCOPE_COMPILE, "jar", null ) );
        set.add( createArtifact( "g", "d", "1.0", Artifact.SCOPE_COMPILE, "zip", null ) );
        return set;
    }

    public Set getArtifactArtifacts()
        throws IOException
    {
        Set set = new HashSet();
        set.add( createArtifact( "g", "one", "1.0", Artifact.SCOPE_COMPILE, "jar", "a" ) );
        set.add( createArtifact( "g", "two", "1.0", Artifact.SCOPE_COMPILE, "jar", "a" ) );
        set.add( createArtifact( "g", "three", "1.0", Artifact.SCOPE_COMPILE, "jar", "a" ) );
        set.add( createArtifact( "g", "four", "1.0", Artifact.SCOPE_COMPILE, "jar", "a" ) );
        return set;
    }

    public Set getGroupIdArtifacts()
        throws IOException
    {
        Set set = new HashSet();
        set.add( createArtifact( "one", "group-one", "1.0", Artifact.SCOPE_COMPILE, "jar", "a" ) );
        set.add( createArtifact( "two", "group-two", "1.0", Artifact.SCOPE_COMPILE, "jar", "a" ) );
        set.add( createArtifact( "three", "group-three", "1.0", Artifact.SCOPE_COMPILE, "jar", "a" ) );
        set.add( createArtifact( "four", "group-four", "1.0", Artifact.SCOPE_COMPILE, "jar", "a" ) );
        return set;
    }

    public Set getMixedArtifacts()
        throws IOException
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

    /**
     * @return Returns the srcFile.
     */
    public File getSrcFile()
    {
        return this.srcFile;
    }

    /**
     * @param srcFile
     *            The srcFile to set.
     */
    public void setSrcFile( File srcFile )
    {
        this.srcFile = srcFile;
    }

    public ArtifactItem getArtifactItem( Artifact artifact )
    {
        ArtifactItem item = new ArtifactItem( artifact );
        return item;
    }

    public ArrayList getArtifactItems( Collection artifacts )
    {
        ArrayList list = new ArrayList();
        Iterator iter = artifacts.iterator();
        while ( iter.hasNext() )
        {
            list.add( getArtifactItem( (Artifact) iter.next() ) );
        }
        return list;
    }

}
