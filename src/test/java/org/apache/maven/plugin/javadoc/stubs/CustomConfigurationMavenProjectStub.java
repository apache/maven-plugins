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
public class CustomConfigurationMavenProjectStub
    extends MavenProjectStub
{

    private Scm scm;

    private Build build;

    public CustomConfigurationMavenProjectStub()
    {
        readModel( new File( getBasedir(), "custom-configuration-plugin-config.xml" ) );

        setGroupId( "org.apache.maven.plugins.maven-javadoc-plugin.unit" );
        setArtifactId( "custom-configuration" );
        setVersion( "1.0-SNAPSHOT" );
        setName( "Maven Javadoc Plugin Custom configuration Test" );
        setUrl( "http://maven.apache.org" );
        setPackaging( "jar" );
        //setExecutionRoot( true );
        //setDescription( "Sample Maven Project" );

        Scm scm = new Scm();
        scm.setConnection( "scm:svn:http://svn.apache.org/maven/sample/trunk" );
        setScm( scm );

        Build build = new Build();
        build.setFinalName( "default-configuration" );
        build.setDirectory( super.getBasedir() + "/target/test/unit/custom-configuration/target" );
        setBuild( build );

        List<String> compileSourceRoots = new ArrayList<String>();
        String temp =  getBasedir().getAbsolutePath();
        if( !temp.startsWith( "/" ) )
        {
            temp = temp.replace( '/', '\\' );
        }
        compileSourceRoots.add( temp );
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
    public Build getBuild()
    {
        return build;
    }

    /** {@inheritDoc} */
    public void setBuild( Build build )
    {
        this.build = build;
    }

    /** {@inheritDoc} */
    public File getBasedir()
    {
        return new File( super.getBasedir() + "/src/test/resources/unit/custom-configuration/" );
    }
}
