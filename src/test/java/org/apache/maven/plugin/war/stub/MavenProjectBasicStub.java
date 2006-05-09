package org.apache.maven.plugin.war.stub;

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

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.model.Organization;

/**
 * Stub
 */
public class MavenProjectBasicStub
    extends MavenProjectStub
{
    protected String testRootDir;

    protected Properties properties;

    protected SimpleWarArtifactStub artifact;

    public MavenProjectBasicStub()
        throws Exception
    {
        properties = new Properties();
    }

    public Set getArtifacts()
    {
        return new HashSet();
    }

    public String getName()
    {
        return "Test Project ";
    }

    public File getBasedir()
    {
        // create an isolated environment
        // see setupTestEnvironment for details
        return new File( testRootDir );
    }

    public void setArtifact( SimpleWarArtifactStub _artifact )
    {
        artifact = _artifact;
    }

    public Artifact getArtifact()
    {
        return artifact;
    }

    public String getGroupId()
    {
        return "org.apache.maven.plugin.test";
    }

    public String getArtifactId()
    {
        return "maven-war-plugin-test";
    }

    public String getPackaging()
    {
        return "jar";
    }

    public String getVersion()
    {
        return "0.0-Test";
    }

    public void addProperty( String key, String value )
    {
        properties.put( key, value );
    }

    public Properties getProperties()
    {
        return properties;
    }

    public String getDescription()
    {
        return "Test Description";
    }

    public Organization getOrganization()
    {
        return new Organization()
        {
            public String getName()
            {
                return "Test Name";
            }
        };
    }

}
