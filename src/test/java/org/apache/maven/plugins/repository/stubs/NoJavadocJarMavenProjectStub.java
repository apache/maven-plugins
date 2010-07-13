/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.repository.stubs;

import java.io.File;

import org.apache.maven.model.Build;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

/**
 * @author <a href="mailto:oching@exist.com">Maria Odea Ching</a>
 */
public class NoJavadocJarMavenProjectStub
    extends MavenProjectStub
{

    private Scm scm;

    private Build build;

    public NoJavadocJarMavenProjectStub()
    {
        setGroupId( "no.javadocjar" );
        setArtifactId( "no-javadocjar" );
        setVersion( "1.0-SNAPSHOT" );
        setName( "Maven Project No Javadoc Jar" );
        setUrl( "http://maven.apache.org" );
        setPackaging( "jar" );
        setDescription( "Sample Maven Project that has no javadoc jar file." );

        Scm scm = new Scm();
        scm.setUrl( "http://svn.apache.org/maven/sample/trunk" );
        scm.setConnection( "scm:svn:http://svn.apache.org/maven/sample/trunk" );
        setScm( scm );

        Build build = new Build();
        build.setFinalName( "no-javadocjar" );
        build.setDirectory( getBasedir() + "/target/test/unit/no-javadocjar/target" );
        setBuild( build );

    }

    public File getFile()
    {
        return new File( getBasedir(), "src/test/resources/unit/no-javadocjar/pom.xml" );
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
