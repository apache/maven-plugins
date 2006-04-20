package org.apache.maven.plugin.checkstyle.stubs;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.model.Build;
import org.apache.maven.model.Organization;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.codehaus.plexus.PlexusTestCase;

import java.util.List;
import java.util.Collections;
import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class MinMavenProjectStub
    extends org.apache.maven.plugin.testing.stubs.MavenProjectStub
{
    public List getCompileClasspathElements()
        throws DependencyResolutionRequiredException
    {
        return Collections.singletonList( PlexusTestCase.getBasedir() + "/target/classes" );
    }

    public File getBasedir()
    {
        return new File( PlexusTestCase.getBasedir() );
    }

    public List getReportPlugins()
    {
        ReportPlugin jxrPlugin = new ReportPlugin();

        jxrPlugin.setArtifactId( "maven-jxr-plugin" );

        return Collections.singletonList( jxrPlugin );
    }

    public Organization getOrganization()
    {
        Organization organization = new Organization();

        organization.setName( "maven-plugin-tests" );

        return organization;
    }

    public String getInceptionYear()
    {
        return "2006";
    }

    public Build getBuild()
    {
        Build build = new Build();

        build.setDirectory( PlexusTestCase.getBasedir() + "/target/test-harness/checkstyle/min" );

        return build;
    }
}
