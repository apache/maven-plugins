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
        String versionString = null;
        if ( !removeVersion )
        {
            versionString = "-" + artifact.getVersion();
        }
        else
        {
            versionString = "";
        }

        if ( artifact.getFile() != null )
        {
            destFileName = artifact.getFile().getName();
        }
        else //if offline
        {
            String classifierString = "";

            if ( StringUtils.isNotEmpty( artifact.getClassifier() ) )
            {
                classifierString = "-" + artifact.getClassifier();
            }

            destFileName = artifact.getArtifactId() + versionString + classifierString + "." + artifact.getType();
        }
        return destFileName;
    }

    /**
     * Formats the outputDirectory based on type.
     * 
     * @param useSubdirsPerType
     *            if a new sub directory should be used for each type.
     * @param useSubdirsPerArtifact
     *            if a new sub directory should be used for each artifact.
     * @param outputDirectory
     *            base outputDirectory.
     * @param artifact
     *            information about the artifact.
     * 
     * @return a formatted File object to use for output.
     */
    public static File getFormattedOutputDirectory( boolean useSubdirsPerType, boolean useSubdirPerArtifact,
                                                   File outputDirectory, Artifact artifact )
    {
        File result = null;

        // get id but convert the chars so it's safe as a folder name.
        String artifactId = artifact.getId().replace( ':', '-' );
        if ( !useSubdirsPerType )
        {
            if ( useSubdirPerArtifact )
            {

                result = new File( outputDirectory.getAbsolutePath() + File.separatorChar + artifactId
                    + File.separatorChar );
            }
            else
            {
                result = outputDirectory;
            }
        }
        else
        {
            if ( useSubdirPerArtifact )
            {
                result = new File( outputDirectory.getAbsolutePath() + File.separatorChar + artifact.getType() + "s"
                    + File.separatorChar + artifactId + File.separatorChar );
            }
            else
            {
                result = new File( outputDirectory.getAbsolutePath() + File.separatorChar + artifact.getType() + "s"
                    + File.separatorChar );
            }
        }

        return result;

    }
}
