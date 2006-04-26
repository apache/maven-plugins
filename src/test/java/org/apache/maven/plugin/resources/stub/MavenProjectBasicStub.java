package org.apache.maven.plugin.resources.stub;

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
import java.util.Properties;

import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

public class MavenProjectBasicStub
    extends MavenProjectStub
{
    protected String identifier;

    protected String testRootDir;

    protected Properties properties;

    protected String description;

    public MavenProjectBasicStub( String id )
        throws Exception
    {
        properties = new Properties();
        identifier = id;
        testRootDir = PlexusTestCase.getBasedir() + "/target/test-classes/unit/test-dir/" + identifier;

        if ( !FileUtils.fileExists( testRootDir ) )
        {
            FileUtils.mkdir( testRootDir );
        }
    }

    public String getName()
    {
        return "Test Project " + identifier;
    }

    public void setDescription( String desc )
    {
        description = desc;
    }

    public String getDescription()
    {
        if ( description == null )
        {
            return "this is a test project";
        }
        else
        {
            return description;
        }
    }

    public File getBasedir()
    {
        // create an isolated environment
        // see setupTestEnvironment for details
        return new File( testRootDir );
    }

    public String getGroupId()
    {
        return "org.apache.maven.plugin.test";
    }

    public String getArtifactId()
    {
        return "maven-resource-plugin-test#" + identifier;
    }

    public String getPackaging()
    {
        return "org.apache.maven.plugin.test";
    }

    public String getVersion()
    {
        return identifier;
    }

    public void addProperty( String key, String value )
    {
        properties.put( key, value );
    }

    public Properties getProperties()
    {
        return properties;
    }
}
