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
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class Jdk6TestMavenProjectStub
    extends MavenProjectStub
{
    private Scm scm;

    private Build build;

    public Jdk6TestMavenProjectStub()
    {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model = null;

        try
        {
            model = pomReader.read( new FileReader( new File( getBasedir()
                + "/src/test/resources/unit/jdk6-test/jdk6-test-plugin-config.xml" ) ) );
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

        Scm scm = new Scm();
        scm.setConnection( "scm:svn:http://svn.apache.org/maven/sample/trunk" );
        setScm( scm );

        Build build = new Build();
        build.setFinalName( model.getArtifactId() );
        build.setDirectory( getBasedir() + "/target/test/unit/jdk6-test/target" );
        setBuild( build );

        String basedir = getBasedir().getAbsolutePath();
        List compileSourceRoots = new ArrayList();
        compileSourceRoots.add( basedir + "/src/test/resources/unit/jdk6-test/" );
        setCompileSourceRoots( compileSourceRoots );
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.MavenProjectStub#getScm()
     */
    public Scm getScm()
    {
        return scm;
    }

    /**
     * @see org.apache.maven.plugin.testing.stubs.MavenProjectStub#setScm(org.apache.maven.model.Scm)
     */
    public void setScm( Scm scm )
    {
        this.scm = scm;
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
}
