package org.apache.maven.plugin.war.stub;

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

import org.apache.maven.model.Organization;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Stub
 */
public class MavenProjectBasicStub
    extends MavenProject
{
    protected String testRootDir;

    protected Properties properties;

    public MavenProjectBasicStub()
        throws Exception
    {
        super( new ModelStub() );
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
        //return new File( testRootDir );
        return null;
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
