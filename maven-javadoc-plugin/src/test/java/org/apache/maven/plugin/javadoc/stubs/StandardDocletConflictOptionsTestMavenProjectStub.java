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

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class StandardDocletConflictOptionsTestMavenProjectStub extends MavenProjectStub
{
    private Build build;

    public StandardDocletConflictOptionsTestMavenProjectStub()
    {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model = null;

        try
        {
            model =
                pomReader.read( new FileReader( new File( getBasedir(), "conflict-options-test-plugin-config.xml" ) ) );
            setModel( model );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }

        setGroupId( model.getGroupId() );
        setArtifactId( model.getArtifactId() );
        setVersion( model.getVersion() );
        setName( model.getName() );
        setUrl( model.getUrl() );
        setPackaging( model.getPackaging() );

        Build build = new Build();
        build.setFinalName( model.getArtifactId() );
        build.setSourceDirectory( getBasedir() + "/src/main/java" );
        build.setDirectory( super.getBasedir() + "/target/test/unit/validate-options-test/target" );
        setBuild( build );

        List compileSourceRoots = new ArrayList();
        compileSourceRoots.add( getBasedir() + "/src/main/java" );
        setCompileSourceRoots( compileSourceRoots );
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.MavenProjectStub#getBuild()
     */
    public Build getBuild()
    {
        return build;
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.MavenProjectStub#setBuild(org.apache.maven.model.Build)
     */
    public void setBuild( Build build )
    {
        this.build = build;
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.MavenProjectStub#getBasedir()
     */
    public File getBasedir()
    {
        return new File( super.getBasedir() + "/src/test/resources/unit/validate-options-test" );
    }
}
