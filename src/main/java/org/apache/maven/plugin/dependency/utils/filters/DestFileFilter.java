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

/**
 * 
 */
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
 *  http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.logging.Log;

/**
 * @author brianf
 * 
 */
public class DestFileFilter
    implements ArtifactsFilter
{

    boolean overWriteReleases;

    boolean overWriteSnapshots;

    boolean overWriteIfNewer;

    boolean useSubDirectoryPerArtifact;

    boolean useSubDirectoryPerType;

    boolean removeVersion;

    File outputFileDirectory;

    public DestFileFilter (File outputFileDirectory)
    {
        this.outputFileDirectory = outputFileDirectory;
        overWriteReleases = false;
        overWriteIfNewer = false;
        overWriteSnapshots = false;
        useSubDirectoryPerArtifact = false;
        useSubDirectoryPerType = false;
        removeVersion = false;
    }
    public DestFileFilter( boolean overWriteReleases, boolean overWriteSnapshots, boolean overWriteIfNewer,
                          boolean useSubDirectoryPerArtifact, boolean useSubDirectoryPerType, boolean removeVersion,
                          File outputFileDirectory )
    {
        this.overWriteReleases = overWriteReleases;
        this.overWriteSnapshots = overWriteSnapshots;
        this.overWriteIfNewer = overWriteIfNewer;
        this.useSubDirectoryPerArtifact = useSubDirectoryPerArtifact;
        this.useSubDirectoryPerType = useSubDirectoryPerType;
        this.removeVersion = removeVersion;
        this.outputFileDirectory = outputFileDirectory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.mojo.dependency.utils.filters.ArtifactsFilter#filter(java.util.Set,
     *      org.apache.maven.plugin.logging.Log)
     */
    public Set filter( Set artifacts, Log log )
        throws MojoExecutionException
    {
        Set result = new HashSet();

        Iterator iter = artifacts.iterator();
        // log.debug("Artifacts:"+ artifacts.size());
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            if ( okToProcess( artifact ) )
            {
                result.add( artifact );
            }
        }
        return result;
    }

    public boolean okToProcess( Artifact artifact )
        throws MojoExecutionException
    {
        boolean overWrite = false;
        boolean result = false;
        if ( ( artifact.isSnapshot() && this.overWriteSnapshots )
            || ( !artifact.isSnapshot() && this.overWriteReleases ) )
        {
            overWrite = true;
        }

        File destFolder = DependencyUtil.getFormattedOutputDirectory( this.useSubDirectoryPerType,
                                                                      this.useSubDirectoryPerArtifact,
                                                                      this.outputFileDirectory, artifact );
        File destFile = new File( destFolder, DependencyUtil.getFormattedFileName( artifact, this.removeVersion ) );

        if ( overWrite
            || ( !destFile.exists() || ( overWriteIfNewer && artifact.getFile().lastModified() < destFile
                .lastModified() ) ) )
        {
            result = true;
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
     * @param outputFileDirectory The outputFileDirectory to set.
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
     * @param removeVersion The removeVersion to set.
     */
    public void setRemoveVersion( boolean removeVersion )
    {
        this.removeVersion = removeVersion;
    }
    /**
     * @return Returns the useSubDirectoryPerArtifact.
     */
    public boolean isUseSubDirectoryPerArtifact()
    {
        return this.useSubDirectoryPerArtifact;
    }
    /**
     * @param useSubDirectoryPerArtifact The useSubDirectoryPerArtifact to set.
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
     * @param useSubDirectoryPerType The useSubDirectoryPerType to set.
     */
    public void setUseSubDirectoryPerType( boolean useSubDirectoryPerType )
    {
        this.useSubDirectoryPerType = useSubDirectoryPerType;
    }
}
