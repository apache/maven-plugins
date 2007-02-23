package org.apache.maven.plugin.dependency.utils;

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

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.StringUtils;

/**
 * Utility class with static helper methods
 * 
 * @author brianf
 * 
 */
public final class DependencyUtil
{

    /**
     * Builds the file name. If removeVersion is set, then the file name must be
     * reconstructed from the artifactId, Classifier (if used) and Type.
     * Otherwise, this method returns the artifact file name.
     * 
     * @param artifact
     *            File to be formatted.
     * @param removeVersion
     *            Specifies if the version should be removed from the file name.
     * @return Formatted file name in the format
     *         artifactId-[version]-[classifier].[type]
     */
    public static String getFormattedFileName( Artifact artifact, boolean removeVersion )
    {
        String destFileName = null;

        // if there is a file and we aren't stripping the version, just get the
        // name directly
        if ( artifact.getFile() != null && !removeVersion )
        {
            destFileName = artifact.getFile().getName();
        }
        else
        // if offline
        {
            String versionString = null;
            if ( !removeVersion )
            {
                versionString = "-" + artifact.getVersion();
            }
            else
            {
                versionString = "";
            }

            String classifierString = "";

            if ( StringUtils.isNotEmpty( artifact.getClassifier() ) )
            {
                classifierString = "-" + artifact.getClassifier();
            }

            destFileName = artifact.getArtifactId() + versionString + classifierString + "."
                + artifact.getArtifactHandler().getExtension();
        }
        return destFileName;
    }

    /**
     * Formats the outputDirectory based on type.
     * 
     * @param useSubdirsPerType
     *            if a new sub directory should be used for each type.
     * @param useSubdirPerArtifact
     *            if a new sub directory should be used for each artifact.
     * @param useRepositoryLayout
     *            if dependendies must be moved into a Maven repository layout, if
     *            set, other settings will be ignored.
     * @param removeVersion
     *            if the version must not be mentioned in the filename
     * @param outputDirectory
     *            base outputDirectory.
     * @param artifact
     *            information about the artifact.
     * 
     * @return a formatted File object to use for output.
     */
    public static File getFormattedOutputDirectory( boolean useSubdirsPerType, boolean useSubdirPerArtifact,
                                                   boolean useRepositoryLayout, boolean removeVersion,
                                                   File outputDirectory, Artifact artifact )
    {
        StringBuffer sb = new StringBuffer( 128 );
        if ( useRepositoryLayout )
        {
            // group id
            sb.append( artifact.getGroupId().replace( '.', File.separatorChar ) ).append( File.separatorChar );
            // artifact id
            sb.append( artifact.getArtifactId() ).append( File.separatorChar );
            // version
            sb.append( artifact.getVersion() ).append( File.separatorChar );
        }
        else
        {
            if ( useSubdirsPerType )
            {
                sb.append( artifact.getType() ).append( "s" ).append( File.separatorChar );
            }
            if ( useSubdirPerArtifact )
            {
                String artifactString = getDependencyId( artifact, removeVersion );
                sb.append( artifactString ).append( File.separatorChar );
            }
        }
        return new File( outputDirectory, sb.toString() );
    }

    private static String getDependencyId( Artifact artifact, boolean removeVersion )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( artifact.getArtifactId() );

        if ( StringUtils.isNotEmpty( artifact.getClassifier() ) )
        {
            sb.append( "-" );
            sb.append( artifact.getClassifier() );
        }

        if ( !removeVersion )
        {
            sb.append( "-" );
            sb.append( artifact.getVersion() );
            sb.append( "-" );
            sb.append( artifact.getType() );
        }
        else
        {
            // if the classifier and type are the same (sources), then don't
            // repeat.
            // avoids names like foo-sources-sources
            if ( !StringUtils.equals( artifact.getClassifier(), artifact.getType() ) )
            {
                sb.append( "-" );
                sb.append( artifact.getType() );
            }
        }
        return sb.toString();
    }
}
