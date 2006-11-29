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

import org.apache.maven.artifact.Artifact;
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
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class JavadocJarInvalidDestdirMavenProjectStub
    extends MavenProjectStub
{
    private Scm scm;

    private Build build;

    public JavadocJarInvalidDestdirMavenProjectStub()
    {

        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model = null;

        try
        {
            model = pomReader.read( new FileReader( new File( getBasedir() +
                "/src/test/resources/unit/javadocjar-invalid-destdir/javadocjar-invalid-destdir-plugin-config.xml" ) ) );
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

        Artifact artifact =
            new JavadocPluginArtifactStub( getGroupId(), getArtifactId(), getVersion(), getPackaging() );
        artifact.setArtifactHandler( new DefaultArtifactHandlerStub() );
        setArtifact( artifact );

        Build build = new Build();
        build.setFinalName( "javadocjar-invalid-destdir" );
        build.setDirectory( getBasedir() + "/target/test/unit/javadocjar-invalid-destdir/target" );
        setBuild( build );

        String basedir = getBasedir().getAbsolutePath();
        List compileSourceRoots = new ArrayList();
        compileSourceRoots.add(
            basedir + "/src/test/resources/unit/javadocjar-invalid-destdir/javadocjar/invalid/destdir" );
        setCompileSourceRoots( compileSourceRoots );
    }

    public Scm getScm()
    {
        return scm;
    }

    public void setScm( Scm scm )
    {
        this.scm = scm;
    }

    public Build getBuild()
    {
        return build;
    }

    public void setBuild( Build build )
    {
        this.build = build;
    }

}
