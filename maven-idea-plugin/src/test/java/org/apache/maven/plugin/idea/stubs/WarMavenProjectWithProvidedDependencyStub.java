package org.apache.maven.plugin.idea.stubs;

import org.apache.maven.model.Dependency;
import org.apache.maven.artifact.Artifact;

import java.util.List;
import java.util.ArrayList;

/*
 *
 *  Copyright 2005-2006 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

/**
 * @author Edwin Punzalan
 */
public class WarMavenProjectWithProvidedDependencyStub
    extends WarMavenProjectStub
{
    public List getTestArtifacts()
    {
        List testArtifacts = new ArrayList();

        Artifact artifact = createArtifact( "org.apache.maven", "maven-model", "2.0.1" );
        artifact.setScope( Artifact.SCOPE_PROVIDED );
        testArtifacts.add( artifact );

        Artifact artifact2 = createArtifact("javax.sql", "jdbc-stdext", "2.0");
        artifact2.setScope( Artifact.SCOPE_SYSTEM );
        testArtifacts.add( artifact2 );

        Artifact artifact3 = createArtifact("junit", "junit", "3.8.1");
        artifact3.setScope( Artifact.SCOPE_TEST );
        testArtifacts.add( artifact3 );

        return testArtifacts;
    }

    public List getDependencies()
    {
        List dependencies = new ArrayList();

        Dependency dep = new Dependency();
        dep.setGroupId( "org.apache.maven" );
        dep.setArtifactId( "maven-model" );
        dep.setVersion( "2.0.1" );
        dep.setScope( Artifact.SCOPE_PROVIDED );
        dependencies.add( dep );

        dep = new Dependency();
        dep.setGroupId( "javax.sql" );
        dep.setArtifactId( "jdbc-stdext" );
        dep.setVersion( "2.0" );
        dep.setScope( Artifact.SCOPE_SYSTEM );
        dep.setSystemPath( "${java.home}/lib/rt.jar" );
        dependencies.add( dep );

        dep = new Dependency();
        dep.setGroupId( "junit" );
        dep.setArtifactId( "junit" );
        dep.setVersion( "3.8.1" );
        dep.setScope( Artifact.SCOPE_TEST );
        dependencies.add( dep );

        return dependencies;
    }
}
