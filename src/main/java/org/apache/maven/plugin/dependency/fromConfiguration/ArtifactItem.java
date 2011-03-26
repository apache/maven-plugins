package org.apache.maven.plugin.dependency.fromConfiguration;

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
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * ArtifactItem represents information specified in the plugin configuration
 * section for each artifact.
 *
 * @since 1.0
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public class ArtifactItem
{
    /**
     * Group Id of Artifact
     *
     * @parameter
     * @required
     */
    private String groupId;

    /**
     * Name of Artifact
     *
     * @parameter
     * @required
     */
    private String artifactId;

    /**
     * Version of Artifact
     *
     * @parameter
     */
    private String version = null;

    /**
     * Type of Artifact (War,Jar,etc)
     *
     * @parameter
     * @required
     */
    private String type = "jar";

    /**
     * Classifier for Artifact (tests,sources,etc)
     *
     * @parameter
     */
    private String classifier;

    /**
     * Location to use for this Artifact. Overrides default location.
     *
     * @parameter
     */
    private File outputDirectory;

    /**
     * Provides ability to change destination file name
     *
     * @parameter
     */
    private String destFileName;

    /**
     * Force Overwrite..this is the one to set in pom
     */
    private String overWrite;

    /**
     * Force Overwrite
     */
    private boolean needsProcessing;

    /**
     * Artifact Item
     */
    private Artifact artifact;

    /**
     * A comma separated list of file patterns to include when unpacking the
     * artifact.
     */
    private String includes;

    /**
     * A comma separated list of file patterns to exclude when unpacking the
     * artifact.
     */
    private String excludes;

    public ArtifactItem()
    {
        // default constructor
    }

    public ArtifactItem( Artifact artifact )
    {
        this.setArtifact( artifact );
        this.setArtifactId( artifact.getArtifactId() );
        this.setClassifier( artifact.getClassifier() );
        this.setGroupId( artifact.getGroupId() );
        this.setType( artifact.getType() );
        this.setVersion( artifact.getVersion() );
    }

    private String filterEmptyString( String in )
    {
        if ( "".equals( in ) )
        {
            return null;
        }
        return in;
    }

    /**
     * @return Returns the artifactId.
     */
    public String getArtifactId()
    {
        return artifactId;
    }

    /**
     * @param artifactId
     *            The artifactId to set.
     */
    public void setArtifactId( String artifact )
    {
        this.artifactId = filterEmptyString( artifact );
    }

    /**
     * @return Returns the groupId.
     */
    public String getGroupId()
    {
        return groupId;
    }

    /**
     * @param groupId
     *            The groupId to set.
     */
    public void setGroupId( String groupId )
    {
        this.groupId = filterEmptyString( groupId );
    }

    /**
     * @return Returns the type.
     */
    public String getType()
    {
        return type;
    }

    /**
     * @param type
     *            The type to set.
     */
    public void setType( String type )
    {
        this.type = filterEmptyString( type );
    }

    /**
     * @return Returns the version.
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * @param version
     *            The version to set.
     */
    public void setVersion( String version )
    {
        this.version = filterEmptyString( version );
    }

    /**
     * @return Classifier.
     */
    public String getClassifier()
    {
        return classifier;
    }

    /**
     * @param classifier
     *            Classifier.
     */
    public void setClassifier( String classifier )
    {
        this.classifier = filterEmptyString( classifier );
    }

    public String toString()
    {
        if ( this.classifier == null )
        {
            return groupId + ":" + artifactId + ":" + StringUtils.defaultString( version, "?" ) + ":" + type;
        }
        else
        {
            return groupId + ":" + artifactId + ":" + classifier + ":" + StringUtils.defaultString( version, "?" )
                + ":" + type;
        }
    }

    /**
     * @return Returns the location.
     */
    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * @param location
     *            The location to set.
     */
    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    /**
     * @return Returns the location.
     */
    public String getDestFileName()
    {
        return destFileName;
    }

    /**
     * @param destFileName
     *            The destFileName to set.
     */
    public void setDestFileName( String destFileName )
    {
        this.destFileName = filterEmptyString( destFileName );
    }

    /**
     * @return Returns the needsProcessing.
     */
    public boolean isNeedsProcessing()
    {
        return this.needsProcessing;
    }

    /**
     * @param needsProcessing
     *            The needsProcessing to set.
     */
    public void setNeedsProcessing( boolean needsProcessing )
    {
        this.needsProcessing = needsProcessing;
    }

    /**
     * @return Returns the overWriteSnapshots.
     */
    public String getOverWrite()
    {
        return this.overWrite;
    }

    /**
     * @param overWriteSnapshots
     *            The overWriteSnapshots to set.
     */
    public void setOverWrite( String overWrite )
    {
        this.overWrite = overWrite;
    }

    /**
     * @return Returns the artifact.
     */
    public Artifact getArtifact()
    {
        return this.artifact;
    }

    /**
     * @param artifact
     *            The artifact to set.
     */
    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }

    /**
     * @return Returns a comma separated list of excluded items
     */
    public String getExcludes ()
    {
        return DependencyUtil.cleanToBeTokenizedString( this.excludes );
    }

    /**
     * @param excludes
     * 			A comma separated list of items to exclude
     * 			i.e.  <code>**\/*.xml, **\/*.properties</code>
     */
    public void setExcludes ( String excludes )
    {
        this.excludes = excludes;
    }

    /**
     * @return Returns a comma separated list of included items
     */
    public String getIncludes()
    {
        return DependencyUtil.cleanToBeTokenizedString( this.includes );
    }

    /**
     * @param includes
     * 			A comma separated list of items to include
     * 			i.e.  <code>**\/*.xml, **\/*.properties</code>
     */
    public void setIncludes ( String includes )
    {
        this.includes = includes;
    }
}
