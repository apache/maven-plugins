package org.apache.maven.plugin.checkstyle.stubs;

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
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Build;
import org.apache.maven.model.Organization;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @version $Id$
 */
public class MultiMavenProjectStub
    extends org.apache.maven.plugin.testing.stubs.MavenProjectStub
{

    public MultiMavenProjectStub()
    {
        setExecutionRoot( true );
    }

    @Override
    public MavenProject getExecutionProject()
    {
        return this;
    }

    /** {@inheritDoc} */
    public List<String> getCompileClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return Collections.EMPTY_LIST;
    }

    /** {@inheritDoc} */
    public List<String> getTestClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return Collections.EMPTY_LIST;
    }

    /** {@inheritDoc} */
    public File getBasedir()
    {
        return new File( PlexusTestCase.getBasedir() );
    }

    /** {@inheritDoc} */
    public List<ReportPlugin> getReportPlugins()
    {
        ReportPlugin jxrPlugin = new ReportPlugin();

        jxrPlugin.setArtifactId( "maven-jxr-plugin" );

        return Collections.singletonList( jxrPlugin );
    }

    /** {@inheritDoc} */
    public Organization getOrganization()
    {
        Organization organization = new Organization();

        organization.setName( "maven-plugin-tests" );

        return organization;
    }

    /** {@inheritDoc} */
    public String getInceptionYear()
    {
        return "2006";
    }

    /** {@inheritDoc} */
    public Build getBuild()
    {
        Build build = new Build();

        build.setDirectory( PlexusTestCase.getBasedir() + "/target/test-harness/checkstyle/multi" );

        return build;
    }

    /** {@inheritDoc} */
    public File getFile()
    {
        File file = new File( getBasedir(), "pom.xml" );

        return file;
    }
}
