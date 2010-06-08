package org.apache.maven.plugin.pmd.stubs;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import edu.emory.mathcs.backport.java.util.Collections;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
public class InvalidFormatMavenProjectStub
    extends MavenProjectStub
{
    private Build build;

    public InvalidFormatMavenProjectStub()
    {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model = null;

        try
        {
            model = pomReader.read( new FileReader( new File(
                getBasedir() + "/src/test/resources/unit/invalid-format/invalid-format-plugin-config.xml" ) ) );
            setModel( model );
        }
        catch ( Exception e )
        {

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
        build.setFinalName( model.getBuild().getFinalName() );
        build.setDirectory( getBasedir() + "/target/test/unit/invalid-format/target" );
        build.setSourceDirectory( getBasedir() + "/src/test/resources/unit/invalid-format" );
        setBuild( build );

        String basedir = getBasedir().getAbsolutePath();
        List compileSourceRoots = new ArrayList();
        compileSourceRoots.add( basedir + "/src/test/resources/unit/invalid-format/invalid/format" );
        setCompileSourceRoots( compileSourceRoots );

        Artifact artifact = new PmdPluginArtifactStub( getGroupId(), getArtifactId(), getVersion(), getPackaging() );
        artifact.setArtifactHandler( new DefaultArtifactHandlerStub() );
        setArtifact( artifact );

        setFile(new File(getBasedir().getAbsolutePath() + "/pom.xml"));

    }

    /** {@inheritDoc} */
    public void setBuild( Build build )
    {
        this.build = build;
    }

    /** {@inheritDoc} */
    public Build getBuild()
    {
        return build;
    }
    
}
