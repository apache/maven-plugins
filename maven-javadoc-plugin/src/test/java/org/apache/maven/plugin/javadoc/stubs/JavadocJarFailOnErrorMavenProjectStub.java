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
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id$
 */
public class JavadocJarFailOnErrorMavenProjectStub
    extends MavenProjectStub
{
    private Scm scm;

    public JavadocJarFailOnErrorMavenProjectStub()
    {
        readModel( new File( getBasedir(), "javadocjar-failonerror-plugin-config.xml" ) );

        setGroupId( getModel().getGroupId() );
        setArtifactId( getModel().getArtifactId() );
        setVersion( getModel().getVersion() );
        setName( getModel().getName() );
        setUrl( getModel().getUrl() );
        setPackaging( getModel().getPackaging() );

        Scm scm = new Scm();
        scm.setConnection( "scm:svn:http://svn.apache.org/maven/sample/trunk" );
        setScm( scm );

        JavadocPluginArtifactStub artifact =
            new JavadocPluginArtifactStub( getGroupId(), getArtifactId(), getVersion(), getPackaging() );
        artifact.setArtifactHandler( new DefaultArtifactHandlerStub() );
        artifact.setType( "jar" );
        artifact.setBaseVersion( "1.0-SNAPSHOT" );
        setArtifact( artifact );

        Build build = new Build();
        build.setFinalName( "javadocjar-failonerror" );
        build.setDirectory( super.getBasedir() + "/target/test/unit/javadocjar-failonerror/target" );
        setBuild( build );

        List<String> compileSourceRoots = new ArrayList<String>();
        compileSourceRoots.add( getBasedir() + "/javadocjar/def" );
        setCompileSourceRoots( compileSourceRoots );
    }

    /** {@inheritDoc} */
    public Scm getScm()
    {
        return scm;
    }

    /** {@inheritDoc} */
    public void setScm( Scm scm )
    {
        this.scm = scm;
    }

    /** {@inheritDoc} */
    public File getBasedir()
    {
        return new File( super.getBasedir() + "/src/test/resources/unit/javadocjar-failonerror" );
    }
}
