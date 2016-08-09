package org.apache.maven.plugins.ear;

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

import org.apache.maven.plugins.ear.output.FileNameMapping;
import org.apache.maven.plugins.ear.output.FileNameMappingFactory;
import org.apache.maven.plugins.ear.util.ArtifactRepository;
import org.apache.maven.plugins.ear.util.ArtifactTypeMappingService;
import org.apache.maven.project.MavenProject;

/**
 * Contains various runtime parameters used to customize the generated EAR file.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id: EarExecutionContext.java 1755538 2016-08-08 20:32:12Z rfscholte $
 */
public class EarExecutionContext
{
    private String defaultLibBundleDir;

    private JbossConfiguration jbossConfiguration;

    private FileNameMapping fileNameMapping;

    private ArtifactRepository artifactRepository;

    /**
     * @param project {@link MavenProject}
     * @param mainArtifactId The artifactId.
     * @param defaultLibBundleDir The defaultLibBundleDir.
     * @param jbossConfiguration {@link JbossConfiguration}
     * @param fileNameMappingName file name mapping.
     * @param typeMappingService {@link ArtifactTypeMappingService}
     */
    public EarExecutionContext( MavenProject project, String mainArtifactId, String defaultLibBundleDir,
                                JbossConfiguration jbossConfiguration, String fileNameMappingName,
                                ArtifactTypeMappingService typeMappingService )
    {
        initialize( project, mainArtifactId, defaultLibBundleDir, jbossConfiguration, fileNameMappingName,
                    typeMappingService );

    }

    /**
     * @return {@link #defaultLibBundleDir}
     */
    public String getDefaultLibBundleDir()
    {
        return defaultLibBundleDir;
    }

    /**
     * @return {@link #jbossConfiguration}
     */
    public boolean isJbossConfigured()
    {
        return jbossConfiguration != null;
    }

    /**
     * @return {@link #fileNameMapping}
     */
    public FileNameMapping getFileNameMapping()
    {
        return fileNameMapping;
    }

    /**
     * @return {@link #artifactRepository}
     */
    public ArtifactRepository getArtifactRepository()
    {
        return artifactRepository;
    }

    private void initialize( MavenProject project, String mainArtifactId, String defaultLibBundleDir,
                             JbossConfiguration jbossConfiguration, String fileNameMappingName,
                             ArtifactTypeMappingService typeMappingService )
    {
        this.artifactRepository = new ArtifactRepository( project.getArtifacts(), mainArtifactId, typeMappingService );
        this.defaultLibBundleDir = defaultLibBundleDir;
        this.jbossConfiguration = jbossConfiguration;
        if ( fileNameMappingName == null || fileNameMappingName.trim().length() == 0 )
        {
            this.fileNameMapping = FileNameMappingFactory.getDefaultFileNameMapping();
        }
        else
        {
            this.fileNameMapping = FileNameMappingFactory.getFileNameMapping( fileNameMappingName );
        }
    }
}
