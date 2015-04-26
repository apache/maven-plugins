package org.apache.maven.plugin.invoker;

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
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TimeZone;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Provides utility methods for artifact metadata processing.
 *
 * @author Benjamin Bentmann
 */
class MetadataUtils
{

    /**
     * Creates local metadata files for the specified artifact. The goal is to simulate the installation of the artifact
     * by a local build, thereby decoupling the forked builds from the inderministic collection of remote repositories
     * that are available to the main build and from which the artifact was originally resolved.
     *
     * @param file The artifact's file in the local test repository, must not be <code>null</code>.
     * @param artifact The artifact to create metadata for, must not be <code>null</code>.
     * @throws IOException If the metadata could not be created.
     */
    public static void createMetadata( File file, Artifact artifact )
        throws IOException
    {
        TimeZone tz = java.util.TimeZone.getTimeZone( "UTC" );
        SimpleDateFormat fmt = new SimpleDateFormat( "yyyyMMddHHmmss" );
        fmt.setTimeZone( tz );
        String timestamp = fmt.format( new Date() );

        if ( artifact.isSnapshot() )
        {
            File metadataFile = new File( file.getParentFile(), "maven-metadata-local.xml" );

            Xpp3Dom metadata = new Xpp3Dom( "metadata" );
            addChild( metadata, "groupId", artifact.getGroupId() );
            addChild( metadata, "artifactId", artifact.getArtifactId() );
            addChild( metadata, "version", artifact.getBaseVersion() );
            Xpp3Dom versioning = new Xpp3Dom( "versioning" );
            versioning.addChild( addChild( new Xpp3Dom( "snapshot" ), "localCopy", "true" ) );
            addChild( versioning, "lastUpdated", timestamp );
            metadata.addChild( versioning );

            writeMetadata( metadataFile, metadata );
        }

        File metadataFile = new File( file.getParentFile().getParentFile(), "maven-metadata-local.xml" );

        Set<String> allVersions = new LinkedHashSet<String>();

        Xpp3Dom metadata = readMetadata( metadataFile );

        if ( metadata != null )
        {
            Xpp3Dom versioning = metadata.getChild( "versioning" );
            if ( versioning != null )
            {
                Xpp3Dom versions = versioning.getChild( "versions" );
                if ( versions != null )
                {

                    Xpp3Dom[] children = versions.getChildren( "version" );
                    for ( Xpp3Dom aChildren : children )
                    {
                        allVersions.add( aChildren.getValue() );
                    }
                }
            }
        }

        allVersions.add( artifact.getBaseVersion() );

        metadata = new Xpp3Dom( "metadata" );
        addChild( metadata, "groupId", artifact.getGroupId() );
        addChild( metadata, "artifactId", artifact.getArtifactId() );
        Xpp3Dom versioning = new Xpp3Dom( "versioning" );
        versioning.addChild( addChildren( new Xpp3Dom( "versions" ), "version", allVersions ) );
        addChild( versioning, "lastUpdated", timestamp );
        metadata.addChild( versioning );

        metadata = Xpp3DomUtils.mergeXpp3Dom( metadata, readMetadata( metadataFile ) );

        writeMetadata( metadataFile, metadata );
    }

    private static Xpp3Dom addChild( Xpp3Dom parent, String childName, String childValue )
    {
        Xpp3Dom child = new Xpp3Dom( childName );
        child.setValue( childValue );
        parent.addChild( child );
        return parent;
    }

    private static Xpp3Dom addChildren( Xpp3Dom parent, String childName, Collection<String> childValues )
    {
        for ( String childValue : childValues )
        {
            addChild( parent, childName, childValue );
        }
        return parent;
    }

    private static Xpp3Dom readMetadata( File metadataFile )
        throws IOException
    {
        if ( !metadataFile.isFile() )
        {
            return null;
        }

        Reader reader = ReaderFactory.newXmlReader( metadataFile );
        try
        {
            try
            {
                return Xpp3DomBuilder.build( reader );
            }
            catch ( XmlPullParserException e )
            {
                throw (IOException) new IOException( e.getMessage() ).initCause( e );
            }
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    private static void writeMetadata( File metadataFile, Xpp3Dom metadata )
        throws IOException
    {
        metadataFile.getParentFile().mkdirs();

        Writer writer = WriterFactory.newXmlWriter( metadataFile );
        try
        {
            Xpp3DomWriter.write( writer, metadata );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

}
