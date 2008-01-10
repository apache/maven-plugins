package org.apache.maven.plugin.assembly.mojos;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;

/**
 * @version $Id$
 * @goal attach-component-descriptor
 * @phase package
 */
public class AttachComponentDescriptorMojo
    extends AbstractMojo
{

    /**
     * @parameter default-value="src/main/resources/assembly-component.xml"
     * @required
     */
    private File componentDescriptor;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @component role-hint="assembly-component"
     */
    private ArtifactHandler handler;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * @component
     */
    private ArtifactFactory factory;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        Artifact artifact = factory.createProjectArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion() );
        artifact.setFile( project.getFile() );

        getLog().debug( "Replacing main project artifact with POM artifact: " + artifact.getId() );

        project.setArtifact( artifact );

        getLog().info( "Attaching assembly-component descriptor: " + componentDescriptor + " to the main project artifact under type: " + handler.getExtension() + " and classifier: " + handler.getClassifier() );

        projectHelper.attachArtifact( project, handler.getExtension(), handler.getClassifier(), componentDescriptor );
    }

}
