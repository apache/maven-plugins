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

import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.Artifact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Edwin Punzalan
 */
public class ReactorMavenProjectStub
    extends MavenProjectStub
{
    public static List reactorProjects = new ArrayList();

    private MavenProject parent;

    public Set getArtifacts()
    {
        return Collections.EMPTY_SET;
    }

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

    public void setParent( MavenProject parent )
    {
        this.parent = parent;
    }

    public MavenProject getParent()
    {
        return parent;
    }
}
