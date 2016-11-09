package org.apache.maven.plugin.assembly.stubs;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Reporting;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author Edwin Punzalan
 */
public class ReactorMavenProjectStub
    extends MavenProjectStub
{
    public static List reactorProjects = new ArrayList();

    private MavenProject parent;

    private Model model;

    private File basedir;

    public ReactorMavenProjectStub()
    {
        this( "jar" );
    }

    public ReactorMavenProjectStub( String packaging )
    {
        super();

        reactorProjects.add( this );

        setGroupId( "assembly" );
        setArtifactId( "reactor-project-" + reactorProjects.size() );
        setVersion( "1.0" );
        setPackaging( packaging );

        setArtifact( new ArtifactStub( getGroupId(), getArtifactId(),
                                       getVersion(), getPackaging(), Artifact.SCOPE_COMPILE ) );
    }

    public String getId()
    {
        return getGroupId() + ":" + getArtifactId() + ":" + getVersion() + ":" + getPackaging();
    }

    public Set getArtifacts()
    {
        Set artifacts = new HashSet();

        artifacts.add( new ArtifactStub( "assembly", "reactor-dependency", "1.0", "jar", Artifact.SCOPE_COMPILE ) );

        return artifacts;
    }

    public File getBasedir()
    {
        File basedir =  super.getBasedir();

        if ( parent != null )
        {
            basedir = parent.getBasedir();
        }

        return new File( basedir, getArtifactId() );
    }

    public Reporting getReporting()
    {
        return model.getReporting();
    }

    public Model getModel()
    {
        if ( model == null )
        {
            model = new Model();

            model.setGroupId( getGroupId() );

            model.setArtifactId( getArtifactId() );

            model.setVersion( getVersion() );

            model.setPackaging( getPackaging() );

            model.setProperties( new Properties() );

            Build build = new Build();
            build.setFinalName( getArtifactId() + "-" + getVersion() + "." + getPackaging() );

            if ( parent != null )
            {
                build.setDirectory( parent.getBasedir().getAbsolutePath() + "/" + getArtifactId() + "/target" );
                FileUtils.mkdir( build.getDirectory() );

                build.setOutputDirectory( parent.getBasedir().getAbsolutePath() + "/" +
                                          getArtifactId() + "/target/classes" );
                FileUtils.mkdir( build.getOutputDirectory() );

                build.setTestOutputDirectory( parent.getBasedir().getAbsolutePath() + "/" +
                                          getArtifactId() + "/target/test-classes" );
                FileUtils.mkdir( build.getTestOutputDirectory() );

                Reporting reporting = new Reporting();
                reporting.setOutputDirectory( parent.getBasedir().getAbsolutePath() + "/" +
                                          getArtifactId() + "/target/site" );
                FileUtils.mkdir( reporting.getOutputDirectory() );

                model.setReporting( reporting );
            }

            model.setBuild( build );
        }

        return model;
    }

    public Build getBuild()
    {
        return getModel().getBuild();
    }

    public void setParent( MavenProject parent )
    {
        this.parent = parent;
    }

    public MavenProject getParent()
    {
        return parent;
    }
}
