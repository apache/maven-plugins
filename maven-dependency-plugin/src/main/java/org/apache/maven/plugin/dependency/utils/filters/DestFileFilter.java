package org.apache.maven.plugin.dependency.utils.filters;

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
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.dependency.fromConfiguration.ArtifactItem;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public class DestFileFilter
    extends AbstractArtifactsFilter
    implements ArtifactItemFilter
{
    private boolean overWriteReleases;

    private boolean overWriteSnapshots;

    private boolean overWriteIfNewer;

    private boolean useSubDirectoryPerArtifact;

    private boolean useSubDirectoryPerType;

    private boolean useSubDirectoryPerScope;

    private boolean useRepositoryLayout;

    private boolean removeVersion;
    
    private boolean removeClassifier;

    private boolean prependGroupId;

    private boolean useBaseVersion;

    private File outputFileDirectory;

    public DestFileFilter( File outputFileDirectory )
    {
        this( false, false, false, false, false, false, false, false, false, false, outputFileDirectory );
    }

    public DestFileFilter( boolean overWriteReleases, boolean overWriteSnapshots, boolean overWriteIfNewer,
                           boolean useSubDirectoryPerArtifact, boolean useSubDirectoryPerType,
                           boolean useSubDirectoryPerScope, boolean useRepositoryLayout, boolean removeVersion,
                           boolean prependGroupId, boolean useBaseVersion, File outputFileDirectory )
    {
        this.overWriteReleases = overWriteReleases;
        this.overWriteSnapshots = overWriteSnapshots;
        this.overWriteIfNewer = overWriteIfNewer;
        this.useSubDirectoryPerArtifact = useSubDirectoryPerArtifact;
        this.useSubDirectoryPerType = useSubDirectoryPerType;
        this.useSubDirectoryPerScope = useSubDirectoryPerScope;
        this.useRepositoryLayout = useRepositoryLayout;
        this.removeVersion = removeVersion;
        this.prependGroupId = prependGroupId;
        this.useBaseVersion = useBaseVersion;
        this.outputFileDirectory = outputFileDirectory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.mojo.dependency.utils.filters.ArtifactsFilter#filter(java.util.Set,
     *      org.apache.maven.plugin.logging.Log)
     */
    public Set filter( Set artifacts )
        throws ArtifactFilterException
    {
        Set<Artifact> result = new HashSet<Artifact>();
        
        for ( Artifact artifact : (Set<Artifact>) artifacts )
        {
            if ( isArtifactIncluded( new ArtifactItem( artifact ) ) )
            {
                result.add( artifact );
            }
        }
        return result;
    }

    /**
     * @return Returns the overWriteReleases.
     */
    public boolean isOverWriteReleases()
    {
        return this.overWriteReleases;
    }

    /**
     * @param overWriteReleases
     *            The overWriteReleases to set.
     */
    public void setOverWriteReleases( boolean overWriteReleases )
    {
        this.overWriteReleases = overWriteReleases;
    }

    /**
     * @return Returns the overWriteSnapshots.
     */
    public boolean isOverWriteSnapshots()
    {
        return this.overWriteSnapshots;
    }

    /**
     * @param overWriteSnapshots
     *            The overWriteSnapshots to set.
     */
    public void setOverWriteSnapshots( boolean overWriteSnapshots )
    {
        this.overWriteSnapshots = overWriteSnapshots;
    }

    /**
     * @return Returns the overWriteIfNewer.
     */
    public boolean isOverWriteIfNewer()
    {
        return this.overWriteIfNewer;
    }

    /**
     * @param overWriteIfNewer
     *            The overWriteIfNewer to set.
     */
    public void setOverWriteIfNewer( boolean overWriteIfNewer )
    {
        this.overWriteIfNewer = overWriteIfNewer;
    }

    /**
     * @return Returns the outputFileDirectory.
     */
    public File getOutputFileDirectory()
    {
        return this.outputFileDirectory;
    }

    /**
     * @param outputFileDirectory
     *            The outputFileDirectory to set.
     */
    public void setOutputFileDirectory( File outputFileDirectory )
    {
        this.outputFileDirectory = outputFileDirectory;
    }

    /**
     * @return Returns the removeVersion.
     */
    public boolean isRemoveVersion()
    {
        return this.removeVersion;
    }

    /**
     * @param removeVersion
     *            The removeVersion to set.
     */
    public void setRemoveVersion( boolean removeVersion )
    {
        this.removeVersion = removeVersion;
    }
    
    /**
     * @return Returns the removeClassifier.
     */
    public boolean isRemoveClassifier()
    {
        return this.removeClassifier;
    }

    /**
     * @param removeClassifier
     *            The removeClassifier to set.
     */
    public void setRemoveClassifier( boolean removeClassifier )
    {
        this.removeClassifier = removeClassifier;
    }

    /**
     * @return Returns the useSubDirectoryPerArtifact.
     */
    public boolean isUseSubDirectoryPerArtifact()
    {
        return this.useSubDirectoryPerArtifact;
    }

    /**
     * @param useSubDirectoryPerArtifact
     *            The useSubDirectoryPerArtifact to set.
     */
    public void setUseSubDirectoryPerArtifact( boolean useSubDirectoryPerArtifact )
    {
        this.useSubDirectoryPerArtifact = useSubDirectoryPerArtifact;
    }

    /**
     * @return Returns the useSubDirectoryPerType.
     */
    public boolean isUseSubDirectoryPerType()
    {
        return this.useSubDirectoryPerType;
    }

    /**
     * @param useSubDirectoryPerType
     *            The useSubDirectoryPerType to set.
     */
    public void setUseSubDirectoryPerType( boolean useSubDirectoryPerType )
    {
        this.useSubDirectoryPerType = useSubDirectoryPerType;
    }

    /**
     * 
     * @return Returns the useRepositoryLayout
     */
    public boolean isUseRepositoryLayout()
    {
        return useRepositoryLayout;
    }

    /**
     * 
     * @param useRepositoryLayout
     *            the useRepositoryLayout to set
     */
    public void setUseRepositoryLayout( boolean useRepositoryLayout )
    {
        this.useRepositoryLayout = useRepositoryLayout;
    }

    public boolean isArtifactIncluded( ArtifactItem item )
    {
        Artifact artifact = item.getArtifact();

        boolean overWrite =
            ( artifact.isSnapshot() && this.overWriteSnapshots )
            || ( !artifact.isSnapshot() && this.overWriteReleases );

        File destFolder = item.getOutputDirectory();
        if ( destFolder == null )
        {
            destFolder =
                DependencyUtil.getFormattedOutputDirectory( useSubDirectoryPerScope, useSubDirectoryPerType,
                                                            useSubDirectoryPerArtifact, useRepositoryLayout,
                                                            removeVersion, this.outputFileDirectory, artifact );
        }

        File destFile;
        if ( StringUtils.isEmpty( item.getDestFileName() ) )
        {
            String formattedFileName =
                    DependencyUtil.getFormattedFileName( artifact, removeVersion, prependGroupId,
                                                         useBaseVersion, removeClassifier );
            destFile = new File( destFolder, formattedFileName );
        }
        else
        {
            destFile = new File( destFolder, item.getDestFileName() );
        }

        return overWrite || !destFile.exists()
            || ( overWriteIfNewer && artifact.getFile().lastModified() > destFile.lastModified() );
    }
}
