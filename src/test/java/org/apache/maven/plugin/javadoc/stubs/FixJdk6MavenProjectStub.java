package org.apache.maven.plugin.javadoc.stubs;

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

import org.apache.maven.model.Build;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class FixJdk6MavenProjectStub
    extends MavenProjectStub
{
    public FixJdk6MavenProjectStub()
    {
        readModel( new File( getBasedir(), "pom.xml" ) );

        addCompileSourceRoot( getBasedir().getAbsolutePath() + "/target/classes" );
        addCompileSourceRoot( getBasedir().getAbsolutePath() + "/src/main/java" );

        Build build = new Build();
        build.setDirectory( getBasedir().getAbsolutePath() + "/target" );
        build.setSourceDirectory( getBasedir().getAbsolutePath() + "/src/main/java" );
        build.setOutputDirectory( getBasedir().getAbsolutePath() + "/target/classes" );
        build.setTestSourceDirectory( getBasedir().getAbsolutePath() + "/src/test/java" );
        build.setTestOutputDirectory( getBasedir().getAbsolutePath() + "/target/test-classes" );
        setBuild( build );
    }

    /** {@inheritDoc} */
    public String getArtifactId()
    {
        return getModel().getArtifactId();
    }

    /** {@inheritDoc} */
    public String getGroupId()
    {
        String groupId = getModel().getGroupId();

        if ( ( groupId == null ) && ( getModel().getParent() != null ) )
        {
            groupId = getModel().getParent().getGroupId();
        }

        return groupId;
    }

    /** {@inheritDoc} */
    public String getVersion()
    {
        String version = getModel().getVersion();

        if ( ( version == null ) && ( getModel().getParent() != null ) )
        {
            version = getModel().getParent().getVersion();
        }

        return version;
    }

    /** {@inheritDoc} */
    public String getPackaging()
    {
        return getModel().getPackaging();
    }

    /** {@inheritDoc} */
    public File getBasedir()
    {
        // Using unit test dir
        return new File( super.getBasedir() + "/target/test/unit/fix-jdk6-test/" );
    }

    /** {@inheritDoc} */
    public File getFile()
    {
        return new File( getBasedir(), "pom.xml" );
    }
}
