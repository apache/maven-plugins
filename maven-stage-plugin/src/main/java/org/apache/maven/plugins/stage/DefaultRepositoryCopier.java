package org.apache.maven.plugins.stage;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.providers.ssh.jsch.ScpWagon;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author Jason van Zyl
 * @plexus.component
 */
public class DefaultRepositoryCopier
    implements RepositoryCopier
{
    private MetadataXpp3Reader reader = new MetadataXpp3Reader();

    private MetadataXpp3Writer writer = new MetadataXpp3Writer();

    /** @plexus.requirement */
    private WagonManager wagonManager;

    /**
     * @deprecated use {@link #copy(String, Repository, String)} so the server configuration applies 
     */
    public void copy( String sourceRepositoryUrl, String targetRepositoryUrl, String version )
        throws WagonException, IOException
    {
        Repository targetRepository = new Repository( "target", targetRepositoryUrl );
        copy( sourceRepositoryUrl, targetRepository, version );
    }

    public void copy( String sourceRepositoryUrl, Repository targetRepository, String version )
        throws WagonException, IOException
    {
        String groupId = "staging-plugin";

        String fileName = groupId + "-" + version + ".zip";

        String tempdir = System.getProperty( "java.io.tmpdir" );

        // Create the renameScript script

        String renameScriptName = groupId + "-" + version + "-rename.sh";

        File renameScript = new File( tempdir, renameScriptName );

        // Work directory

        File basedir = new File( tempdir, groupId + "-" + version );

        FileUtils.deleteDirectory( basedir );

        basedir.mkdirs();

        Repository sourceRepository = new Repository( "source", sourceRepositoryUrl );

        String protocol = sourceRepository.getProtocol();

        Wagon sourceWagon = wagonManager.getWagon( protocol );

        sourceWagon.connect( sourceRepository );

        List files = new ArrayList();

        scan( sourceWagon, "", files );

        for ( Iterator i = files.iterator(); i.hasNext(); )
        {
            String s = (String) i.next();

            if ( s.indexOf( ".svn" ) >= 0 )
            {
                continue;
            }

            File f = new File( basedir, s );

            FileUtils.mkdir( f.getParentFile().getAbsolutePath() );

            sourceWagon.get( s, f );
        }

        // ----------------------------------------------------------------------------
        // Now all the files are present locally and now we are going to grab the
        // metadata files from the targetRepositoryUrl stage and pull those down locally
        // so that we can merge the metadata.
        // ----------------------------------------------------------------------------

        // TODO BUG for some reason it gets the wagon without authentication info
        Wagon targetWagon = wagonManager.getWagon( targetRepository );

        targetWagon.connect( targetRepository );

        PrintWriter rw = new PrintWriter( new FileWriter( renameScript ) );

        File archive = new File( tempdir, fileName );

        for ( Iterator i = files.iterator(); i.hasNext(); )
        {
            String s = (String) i.next();

            if ( s.startsWith( "/" ) )
            {
                s = s.substring( 1 );
            }

            if ( s.endsWith( MAVEN_METADATA ) )
            {
                File emf = new File( basedir, s + IN_PROCESS_MARKER );

                try
                {
                    targetWagon.get( s, emf );
                }
                catch ( ResourceDoesNotExistException e )
                {
                    // We don't have an equivalent on the targetRepositoryUrl side because we have something
                    // new on the sourceRepositoryUrl side so just skip the metadata merging.

                    continue;
                }

                try
                {
                    mergeMetadata( emf );
                }
                catch ( XmlPullParserException e )
                {
                    throw new IOException( "Metadata file is corrupt " + s + " Reason: " + e.getMessage() );
                }
            }
        }

        Set moveCommands = new TreeSet();

        // ----------------------------------------------------------------------------
        // Create the Zip file that we will deploy to the targetRepositoryUrl stage
        // ----------------------------------------------------------------------------

        OutputStream os = new FileOutputStream( archive );

        ZipOutputStream zos = new ZipOutputStream( os );

        scanDirectory( basedir, basedir, zos, version, moveCommands );

        // ----------------------------------------------------------------------------
        // Create the renameScript script. This is as atomic as we can
        // ----------------------------------------------------------------------------

        for ( Iterator i = moveCommands.iterator(); i.hasNext(); )
        {
            String s = (String) i.next();

            rw.println( s );
        }

        IOUtil.close( rw );

        ZipEntry e = new ZipEntry( renameScript.getName() );

        zos.putNextEntry( e );

        InputStream is = new FileInputStream( renameScript );

        IOUtil.copy( is, zos );

        IOUtil.close( is );

        IOUtil.close( zos );

        sourceWagon.disconnect();

        // Push the Zip to the target system

        targetWagon.put( archive, fileName );

        String targetRepoBaseDirectory = targetRepository.getBasedir();

        // We use the super quiet option here as all the noise seems to kill/stall the connection

        String command = "unzip -o -qq -d " + targetRepoBaseDirectory + " " + targetRepoBaseDirectory + "/" + fileName;

        ( (ScpWagon) targetWagon ).executeCommand( command );

        command = "rm -f " + targetRepoBaseDirectory + "/" + fileName;

        ( (ScpWagon) targetWagon ).executeCommand( command );

        command = "cd " + targetRepoBaseDirectory + "; sh " + renameScriptName;

        ( (ScpWagon) targetWagon ).executeCommand( command );

        command = "rm -f " + targetRepoBaseDirectory + "/" + renameScriptName;

        ( (ScpWagon) targetWagon ).executeCommand( command );

        targetWagon.disconnect();
    }

    private void scanDirectory( File basedir, File dir, ZipOutputStream zos, String version, Set moveCommands )
        throws IOException
    {
        if ( dir == null )
        {
            return;
        }

        File[] files = dir.listFiles();

        for ( int i = 0; i < files.length; i++ )
        {
            File f = files[i];

            if ( f.isDirectory() )
            {
                if ( f.getName().equals( ".svn" ) )
                {
                    continue;
                }

                if ( f.getName().endsWith( version ) )
                {
                    String s = f.getAbsolutePath().substring( basedir.getAbsolutePath().length() + 1 );

                    moveCommands.add( "mv " + s + IN_PROCESS_MARKER + " " + s );                   
                }

                scanDirectory( basedir, f, zos, version, moveCommands );
            }
            else
            {
                InputStream is = new FileInputStream( f );

                String s = f.getAbsolutePath().substring( basedir.getAbsolutePath().length() + 1 );                

                // We are marking any version directories with the in-process flag so that
                // anything being unpacked on the target side will not be recogized by Maven
                // and so users cannot download partially uploaded files.

                String vtag = "/" + version;

                s = StringUtils.replace( s, vtag + "/", vtag + IN_PROCESS_MARKER + "/" );

                ZipEntry e = new ZipEntry( s );

                zos.putNextEntry( e );

                IOUtil.copy( is, zos );

                IOUtil.close( is );

                int idx = s.indexOf( IN_PROCESS_MARKER );

                if ( idx > 0 )
                {
                    String d = s.substring( 0, idx );

                    moveCommands.add( "mv " + d + IN_PROCESS_MARKER + " " + d );
                }
            }
        }
    }

    private void mergeMetadata( File existingMetadata )
        throws IOException, XmlPullParserException
    {
        // Existing Metadata in target stage

        Reader existingMetadataReader = new FileReader( existingMetadata );

        Metadata existing = reader.read( existingMetadataReader );

        // Staged Metadata

        File stagedMetadataFile = new File( existingMetadata.getParentFile(), MAVEN_METADATA );

        Reader stagedMetadataReader = new FileReader( stagedMetadataFile );

        Metadata staged = reader.read( stagedMetadataReader );

        // Merge

        existing.merge( staged );

        Writer writer = new FileWriter( existingMetadata );

        this.writer.write( writer, existing );

        IOUtil.close( writer );

        IOUtil.close( stagedMetadataReader );

        IOUtil.close( existingMetadataReader );

        // Mark all metadata as in-process and regenerate the checksums as they will be different
        // after the merger

        try
        {
            File newMd5 = new File( existingMetadata.getParentFile(), MAVEN_METADATA + ".md5" + IN_PROCESS_MARKER );

            FileUtils.fileWrite( newMd5.getAbsolutePath(), checksum( existingMetadata, MD5 ) );

            File oldMd5 = new File( existingMetadata.getParentFile(), MAVEN_METADATA + ".md5" );

            oldMd5.delete();

            File newSha1 = new File( existingMetadata.getParentFile(), MAVEN_METADATA + ".sha1" + IN_PROCESS_MARKER );

            FileUtils.fileWrite( newSha1.getAbsolutePath(), checksum( existingMetadata, SHA1 ) );

            File oldSha1 = new File( existingMetadata.getParentFile(), MAVEN_METADATA + ".sha1" );

            oldSha1.delete();
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new RuntimeException( e );
        }

        // We have the new merged copy so we're good

        stagedMetadataFile.delete();
    }

    private String checksum( File file,
                             String type )
        throws IOException, NoSuchAlgorithmException
    {
        MessageDigest md5 = MessageDigest.getInstance( type );

        InputStream is = new FileInputStream( file );

        byte[] buf = new byte[8192];

        int i;

        while ( ( i = is.read( buf ) ) > 0 )
        {
            md5.update( buf, 0, i );
        }

        IOUtil.close( is );

        return encode( md5.digest() );
    }

    protected String encode( byte[] binaryData )
    {
        if ( binaryData.length != 16 && binaryData.length != 20 )
        {
            int bitLength = binaryData.length * 8;
            throw new IllegalArgumentException( "Unrecognised length for binary data: " + bitLength + " bits" );
        }

        String retValue = "";

        for ( int i = 0; i < binaryData.length; i++ )
        {
            String t = Integer.toHexString( binaryData[i] & 0xff );

            if ( t.length() == 1 )
            {
                retValue += ( "0" + t );
            }
            else
            {
                retValue += t;
            }
        }

        return retValue.trim();
    }

    private void scan( Wagon wagon,
                       String basePath,
                       List collected )
    {
        try
        {
            List files = wagon.getFileList( basePath );

            if ( files.isEmpty() )
            {
                collected.add( basePath );
            }
            else
            {
                basePath = basePath + "/";
                for ( Iterator iterator = files.iterator(); iterator.hasNext(); )
                {
                    String file = (String) iterator.next();
                    scan( wagon, basePath + file, collected );
                }
            }
        }
        catch ( TransferFailedException e )
        {
            throw new RuntimeException( e );
        }
        catch ( ResourceDoesNotExistException e )
        {
            // is thrown when calling getFileList on a file
            collected.add( basePath );
        }
        catch ( AuthorizationException e )
        {
            throw new RuntimeException( e );
        }

    }

    protected List scanForArtifactPaths( ArtifactRepository repository )
    {
        List collected;
        try
        {
            Wagon wagon = wagonManager.getWagon( repository.getProtocol() );
            Repository artifactRepository = new Repository( repository.getId(), repository.getUrl() );
            wagon.connect( artifactRepository );
            collected = new ArrayList();
            scan( wagon, "/", collected );
            wagon.disconnect();

            return collected;

        }
        catch ( UnsupportedProtocolException e )
        {
            throw new RuntimeException( e );
        }
        catch ( ConnectionException e )
        {
            throw new RuntimeException( e );
        }
        catch ( AuthenticationException e )
        {
            throw new RuntimeException( e );
        }
    }
}
