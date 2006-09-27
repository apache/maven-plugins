package org.apache.maven.plugin.javadoc.stubs;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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
public class DefaultConfigurationMavenProjectStub
    extends MavenProjectStub
{

    private Scm scm;

    private Build build;

    public DefaultConfigurationMavenProjectStub()
    {
        setGroupId( "def.configuraion" );
        setArtifactId( "default-configuration" );
        setVersion( "1.0-SNAPSHOT" );
        setName( "Maven Javadoc Plugin Default configuration Test" );
        setUrl( "http://maven.apache.org" );
        setPackaging( "jar" );

        Scm scm = new Scm();
        scm.setConnection( "scm:svn:http://svn.apache.org/maven/sample/trunk" );
        setScm( scm );

        Build build = new Build();
        build.setFinalName( "default-configuration" );
        build.setDirectory( getBasedir() + "/target/test/unit/default-configuration/target" );
        setBuild( build );

        String basedir = getBasedir().getAbsolutePath();
        List compileSourceRoots = new ArrayList();
        compileSourceRoots.add( basedir + "/src/test/resources/unit/default-configuration/def/configuration" );
        setCompileSourceRoots( compileSourceRoots );

        MavenXpp3Reader pomReader = new MavenXpp3Reader();

        try
        {
            Model model = pomReader.read( new FileReader( new File( getBasedir() +
                "/src/test/resources/unit/default-configuration/default-configuration-plugin-config.xml" ) ) );
            setModel( model );
        }
        catch ( Exception e )
        {

        }
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
