package org.apache.maven.report.projectinfo;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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

import junit.framework.TestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;

import java.util.Set;
import java.util.Map;
import java.util.List;

/**
 * @author Edwin Punzalan
 */
public class ReportResolutionListenerTest
    extends TestCase
{
    private ReportResolutionListener listener;

    public void testSimpleDependencyTree()
    {
        Artifact projectArtifact = createArtifact( "test-project", "project-artifact", "1.0" );

        listener.startProcessChildren( projectArtifact );

        Artifact depArtifact01 = createArtifact( "test-dep", "dependency-one", "1.0" );
        listener.includeArtifact( depArtifact01 );

        Artifact depArtifact02 = createArtifact( "test-dep", "dependency-two", "1.0" );
        listener.includeArtifact( depArtifact02 );

        Artifact depArtifact03 = createArtifact( "test-dep", "dependency-three", "1.0" );
        listener.includeArtifact( depArtifact03 );

        listener.endProcessChildren( projectArtifact );

        Set artifacts = listener.getArtifacts();
        assertEquals( "Test total artifacts", 3, artifacts.size() );
        assertTrue( "Test dependency one", artifacts.contains( depArtifact01 ) );
        assertTrue( "Test dependency two", artifacts.contains( depArtifact02 ) );
        assertTrue( "Test dependency three", artifacts.contains( depArtifact03 ) );

        Map depMap = listener.getDepMap();
        assertEquals( "Test total artifacts in dependency map", 1, depMap.size() );
        assertTrue( "Test dependency map key", depMap.containsKey( projectArtifact.getId() ) );
        List dependencies = (List) depMap.get( projectArtifact.getId() );
        assertEquals( "Test dependency total", 3, dependencies.size() );
        assertTrue( "Test dependency content 1", dependencies.contains( depArtifact01 ) );
        assertTrue( "Test dependency content 2", dependencies.contains( depArtifact02 ) );
        assertTrue( "Test dependency content 3", dependencies.contains( depArtifact03 ) );

        Map depTree = listener.getDepTree();
        assertEquals( "Test total artifacts in dependency map", 1, depTree.size() );
        assertTrue( "Test dependency map key", depTree.containsKey( projectArtifact.getId() ) );
        dependencies = (List) depTree.get( projectArtifact.getId() );
        assertEquals( "Test dependency total", 3, dependencies.size() );
        assertTrue( "Test dependency content 1", dependencies.contains( depArtifact01 ) );
        assertTrue( "Test dependency content 2", dependencies.contains( depArtifact02 ) );
        assertTrue( "Test dependency content 3", dependencies.contains( depArtifact03 ) );

        Map directDeps = listener.getDirectDependencies();
        assertEquals( "Test total direct dependencies", 3, directDeps.size() );
        assertTrue( "Test dependency content 1", directDeps.containsValue( depArtifact01 ) );
        assertTrue( "Test dependency content 2", directDeps.containsValue( depArtifact02 ) );
        assertTrue( "Test dependency content 3", directDeps.containsValue( depArtifact03 ) );

        Map omittedDeps = listener.getOmittedArtifacts();
        assertEquals( "Test total omitted dependencies", 0, omittedDeps.size() );

        Map transDeps = listener.getTransitiveDependencies();
        assertEquals( "Test total transitive dependencies", 0, transDeps.size() );
    }

    public void testSimpleDepTreeWithTransitiveDeps()
    {
        Artifact projectArtifact = createArtifact( "test-project", "project-artifact", "1.0" );

        listener.startProcessChildren( projectArtifact );

        Artifact depArtifact1 = createArtifact( "test-dep", "dependency-one", "1.0" );
        listener.includeArtifact( depArtifact1 );

            listener.startProcessChildren( depArtifact1 );

            Artifact depArtifact01 = createArtifact( "test-dep", "dependency-zero-one", "1.0" );
            listener.includeArtifact( depArtifact01 );

            Artifact depArtifact02 = createArtifact( "test-dep", "dependency-zero-two", "1.0" );
            listener.includeArtifact( depArtifact02 );

            listener.endProcessChildren( depArtifact1 );


        Artifact depArtifact2 = createArtifact( "test-dep", "dependency-two", "1.0" );
        listener.includeArtifact( depArtifact2 );

        Artifact depArtifact3 = createArtifact( "test-dep", "dependency-three", "1.0" );
        listener.includeArtifact( depArtifact3 );

        listener.endProcessChildren( projectArtifact );

        Set artifacts = listener.getArtifacts();
        assertEquals( "Test total artifacts", 5, artifacts.size() );
        assertTrue( "Test dependency one", artifacts.contains( depArtifact1 ) );
        assertTrue( "Test dependency two", artifacts.contains( depArtifact2 ) );
        assertTrue( "Test dependency three", artifacts.contains( depArtifact3 ) );
        assertTrue( "Test dependency four", artifacts.contains( depArtifact01 ) );
        assertTrue( "Test dependency five", artifacts.contains( depArtifact02 ) );

        Map depMap = listener.getDepMap();
        assertEquals( "Test total artifacts in dependency map", 2, depMap.size() );
        assertTrue( "Test dependency map key", depMap.containsKey( projectArtifact.getId() ) );
        List dependencies = (List) depMap.get( projectArtifact.getId() );
        assertEquals( "Test dependency total", 3, dependencies.size() );
        assertTrue( "Test dependency content 1", dependencies.contains( depArtifact1 ) );
        assertTrue( "Test dependency content 2", dependencies.contains( depArtifact2 ) );
        assertTrue( "Test dependency content 3", dependencies.contains( depArtifact3 ) );
        assertTrue( "Test dependency map key", depMap.containsKey( projectArtifact.getId() ) );
        dependencies = (List) depMap.get( depArtifact1.getId() );
        assertEquals( "Test dependency total", 2, dependencies.size() );
        assertTrue( "Test dependency content 1", dependencies.contains( depArtifact01 ) );
        assertTrue( "Test dependency content 2", dependencies.contains( depArtifact02 ) );

        Map depTree = listener.getDepTree();
        assertEquals( "Test total artifacts in dependency map", 2, depTree.size() );
        assertTrue( "Test dependency map key", depTree.containsKey( projectArtifact.getId() ) );
        dependencies = (List) depTree.get( projectArtifact.getId() );
        assertEquals( "Test dependency total", 3, dependencies.size() );
        assertTrue( "Test dependency content 1", dependencies.contains( depArtifact1 ) );
        assertTrue( "Test dependency content 2", dependencies.contains( depArtifact2 ) );
        assertTrue( "Test dependency content 3", dependencies.contains( depArtifact3 ) );
        assertTrue( "Test dependency map key", depTree.containsKey( depArtifact1.getId() ) );
        dependencies = (List) depTree.get( depArtifact1.getId() );
        assertEquals( "Test dependency total", 2, dependencies.size() );
        assertTrue( "Test dependency content 1", dependencies.contains( depArtifact01 ) );
        assertTrue( "Test dependency content 2", dependencies.contains( depArtifact02 ) );

        Map directDeps = listener.getDirectDependencies();
        assertEquals( "Test total direct dependencies", 3, directDeps.size() );
        assertTrue( "Test dependency content 1", directDeps.containsValue( depArtifact1 ) );
        assertTrue( "Test dependency content 2", directDeps.containsValue( depArtifact2 ) );
        assertTrue( "Test dependency content 3", directDeps.containsValue( depArtifact3 ) );

        Map omittedDeps = listener.getOmittedArtifacts();
        assertEquals( "Test total omitted dependencies", 0, omittedDeps.size() );

        Map transDeps = listener.getTransitiveDependencies();
        assertEquals( "Test total transitive dependencies", 2, transDeps.size() );
        assertTrue( "Test transitive dependency 1", transDeps.containsValue( depArtifact01 ) );
        assertTrue( "Test transitive dependency 1", transDeps.containsValue( depArtifact02 ) );
    }

    public void testComplexDependencyTree()
    {
        Artifact projectArtifact = createArtifact( "test-project", "project-artifact", "1.0" );

        listener.startProcessChildren( projectArtifact );

        Artifact depArtifact1 = createArtifact( "test-dep", "dependency-one", "1.0", Artifact.SCOPE_COMPILE );
        listener.includeArtifact( depArtifact1 );

            listener.startProcessChildren( depArtifact1 );

            Artifact depArtifact11 = createArtifact( "test-dep", "dependency-zero-one", "1.0" );
            listener.includeArtifact( depArtifact11 );

            Artifact depArtifact12 = createArtifact( "test-dep", "dependency-zero-two", "1.0" );
            listener.includeArtifact( depArtifact12 );

                listener.startProcessChildren( depArtifact12 );

                Artifact depArtifact121 = createArtifact( "test-dep", "dep-zero-two-1", "1.0" );
                listener.includeArtifact( depArtifact121 );

                listener.endProcessChildren( depArtifact12 );

            listener.endProcessChildren( depArtifact1 );


        Artifact depArtifact2 = createArtifact( "test-dep", "dependency-two", "1.0", Artifact.SCOPE_TEST );
        listener.includeArtifact( depArtifact2 );

            listener.startProcessChildren( depArtifact2 );

            Artifact depArtifact21 = createArtifact( "test-dep", "dep-zero-two-1", "1.0" );
            listener.includeArtifact( depArtifact21 );
            listener.omitForNearer( depArtifact121, depArtifact21 );
            listener.updateScope( depArtifact121, Artifact.SCOPE_TEST );

            listener.endProcessChildren( depArtifact2 );

        Artifact depArtifact3 = createArtifact( "test-dep", "dependency-three", "1.0", Artifact.SCOPE_COMPILE );
        listener.includeArtifact( depArtifact3 );

        listener.endProcessChildren( projectArtifact );

        Set artifacts = listener.getArtifacts();
        assertEquals( "Test total artifacts", 6, artifacts.size() );
        assertTrue( "Test dependency one", artifacts.contains( depArtifact1 ) );
        assertTrue( "Test dependency two", artifacts.contains( depArtifact2 ) );
        assertTrue( "Test dependency three", artifacts.contains( depArtifact3 ) );
        assertTrue( "Test dependency four", artifacts.contains( depArtifact11 ) );
        assertTrue( "Test dependency five", artifacts.contains( depArtifact12 ) );
        assertTrue( "Test dependency six", artifacts.contains( depArtifact21 ) );

        Map depMap = listener.getDepMap();
        assertEquals( "Test total artifacts in dependency map", 4, depMap.size() );
        assertTrue( "Test dependency map key", depMap.containsKey( projectArtifact.getId() ) );
        List dependencies = (List) depMap.get( projectArtifact.getId() );
        assertEquals( "Test dependency total", 3, dependencies.size() );
        assertTrue( "Test dependency content 1", dependencies.contains( depArtifact1 ) );
        assertTrue( "Test dependency content 2", dependencies.contains( depArtifact2 ) );
        assertTrue( "Test dependency content 3", dependencies.contains( depArtifact3 ) );
        assertTrue( "Test dependency map key", depMap.containsKey( depArtifact1.getId() ) );
        dependencies = (List) depMap.get( depArtifact1.getId() );
        assertEquals( "Test dependency total", 2, dependencies.size() );
        assertTrue( "Test dependency content 1", dependencies.contains( depArtifact11 ) );
        assertTrue( "Test dependency content 2", dependencies.contains( depArtifact12 ) );
        assertTrue( "Test dependency map key", depMap.containsKey( depArtifact12.getId() ) );
        dependencies = (List) depMap.get( depArtifact12.getId() );
        assertEquals( "Test dependency total", 1, dependencies.size() );
        assertTrue( "Test dependency content 1", dependencies.contains( depArtifact121 ) );
        assertTrue( "Test dependency map key", depMap.containsKey( depArtifact2.getId() ) );
        dependencies = (List) depMap.get( depArtifact2.getId() );
        assertEquals( "Test dependency total", 1, dependencies.size() );
        assertTrue( "Test dependency content 1", dependencies.contains( depArtifact21 ) );

        Map depTree = listener.getDepTree();
        assertEquals( "Test total artifacts in dependency map", 4, depTree.size() );
        assertTrue( "Test dependency map key", depTree.containsKey( projectArtifact.getId() ) );
        dependencies = (List) depTree.get( projectArtifact.getId() );
        assertEquals( "Test dependency total", 3, dependencies.size() );
        assertTrue( "Test dependency content 1", dependencies.contains( depArtifact1 ) );
        assertTrue( "Test dependency content 2", dependencies.contains( depArtifact2 ) );
        assertTrue( "Test dependency content 3", dependencies.contains( depArtifact3 ) );
        assertTrue( "Test dependency map key", depTree.containsKey( depArtifact1.getId() ) );
        dependencies = (List) depTree.get( depArtifact1.getId() );
        assertEquals( "Test dependency total", 2, dependencies.size() );
        assertTrue( "Test dependency content 1", dependencies.contains( depArtifact11 ) );
        assertTrue( "Test dependency content 2", dependencies.contains( depArtifact12 ) );
        assertTrue( "Test dependency map key", depTree.containsKey( depArtifact12.getId() ) );
        dependencies = (List) depTree.get( depArtifact12.getId() );
        assertEquals( "Test dependency total", 0, dependencies.size() );
        assertTrue( "Test dependency map key", depTree.containsKey( depArtifact2.getId() ) );
        dependencies = (List) depTree.get( depArtifact2.getId() );
        assertEquals( "Test dependency total", 1, dependencies.size() );
        assertTrue( "Test dependency content 1", dependencies.contains( depArtifact21 ) );

        Map directDeps = listener.getDirectDependencies();
        assertEquals( "Test total direct dependencies", 3, directDeps.size() );
        assertTrue( "Test dependency content 1", directDeps.containsValue( depArtifact1 ) );
        assertTrue( "Test dependency content 2", directDeps.containsValue( depArtifact2 ) );
        assertTrue( "Test dependency content 3", directDeps.containsValue( depArtifact3 ) );

        Map omittedDeps = listener.getOmittedArtifacts();
        assertEquals( "Test total omitted dependencies", 1, omittedDeps.size() );

        Map transDeps = listener.getTransitiveDependencies();
        assertEquals( "Test total transitive dependencies", 3, transDeps.size() );
        assertTrue( "Test transitive dependency 1", transDeps.containsValue( depArtifact11 ) );
        assertTrue( "Test transitive dependency 2", transDeps.containsValue( depArtifact12 ) );
        assertTrue( "Test transitive dependency 3", transDeps.containsValue( depArtifact21 ) );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version )
    {
        return createArtifact( groupId, artifactId, version, null );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version, String scope )
    {
        VersionRange versionRange = VersionRange.createFromVersion( version );

        Artifact artifact = new DefaultArtifact( groupId, artifactId, versionRange, scope,
                                                 "jar", null, new DefaultArtifactHandler(), false );

        return artifact;
    }

    protected void setUp()
        throws Exception
    {
        listener = new ReportResolutionListener();
    }

    protected void tearDown()
        throws Exception
    {
        listener = null;
    }
}
