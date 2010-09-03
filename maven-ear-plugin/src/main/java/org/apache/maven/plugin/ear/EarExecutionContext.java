package org.apache.maven.plugin.ear;

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

import org.apache.maven.plugin.ear.output.FileNameMapping;
import org.apache.maven.plugin.ear.output.FileNameMappingFactory;
import org.apache.maven.plugin.ear.util.ArtifactRepository;
import org.apache.maven.plugin.ear.util.ArtifactTypeMappingService;
import org.apache.maven.project.MavenProject;

/**
 * Contains various runtime parameters used to customize the generated EAR file.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public class EarExecutionContext
{
    private String defaultLibBundleDir;

    private JbossConfiguration jbossConfiguration;

    private FileNameMapping fileNameMapping;

    private ArtifactRepository artifactRepository;


    public EarExecutionContext( MavenProject project, String mainArtifactId, String defaultLibBundleDir,
                                JbossConfiguration jbossConfiguration, String fileNameMappingName,
                                ArtifactTypeMappingService typeMappingService )
    {
        initialize( project, mainArtifactId, defaultLibBundleDir, jbossConfiguration, fileNameMappingName,
                    typeMappingService );

    }

    public String getDefaultLibBundleDir()
    {
        return defaultLibBundleDir;
    }

    public boolean isJbossConfigured()
    {
        return jbossConfiguration != null;
    }

    public FileNameMapping getFileNameMapping()
    {
        return fileNameMapping;
    }

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
