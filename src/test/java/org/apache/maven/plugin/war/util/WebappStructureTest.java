package org.apache.maven.plugin.war.util;

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

import junit.framework.TestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stephane Nicoll
 */
public class WebappStructureTest
    extends TestCase
{


    public void testDependencyAnalysisNoChange()
    {
        final List dependencies = new ArrayList();
        dependencies.add( createDependency( "groupTest", "artifactTest", "1.0" ) );
        final WebappStructure cache = new WebappStructure( dependencies );

        final WebappStructure webappStructure = new WebappStructure( dependencies, cache );

        webappStructure.analyseDependencies( new WebappStructure.DependenciesAnalysisCallback()
        {

            int count = 0;

            public void unchangedDependency( Dependency dependency )
            {
                if ( count == 0 )
                {
                    count++;
                }
                else
                {
                    fail( "Should have called unchanged dependency only once" );
                }
            }

            public void newDependency( Dependency dependency )
            {
                fail( "Should have failed to trigger this callback" );
            }

            public void removedDependency( Dependency dependency )
            {
                fail( "Should have failed to trigger this callback" );
            }

            public void updatedVersion( Dependency dependency, String previousVersion )
            {
                fail( "Should have failed to trigger this callback" );
            }

            public void updatedScope( Dependency dependency, String previousScope )
            {
                fail( "Should have failed to trigger this callback" );
            }

            public void updatedOptionalFlag( Dependency dependency, boolean previousOptional )
            {
                fail( "Should have failed to trigger this callback" );
            }

            public void updatedUnknown( Dependency dependency, Dependency previousDep )
            {
                fail( "Should have failed to trigger this callback" );
            }
        } );

    }


    public void testDependencyAnalysisWithNewDependency()
    {
        final List dependencies = new ArrayList();
        dependencies.add( createDependency( "groupTest", "artifactTest", "1.0" ) );
        final WebappStructure cache = new WebappStructure( dependencies );
        final List newDependencies = new ArrayList( dependencies );
        final Dependency newDependency = createDependency( "groupTest", "nexArtifact", "2.0" );
        newDependencies.add( newDependency );

        final WebappStructure webappStructure = new WebappStructure( newDependencies, cache );

        webappStructure.analyseDependencies( new WebappStructure.DependenciesAnalysisCallback()
        {

            int count = 0;

            public void unchangedDependency( Dependency dependency )
            {
                if ( count == 0 )
                {
                    count++;
                }
                else
                {
                    fail( "Should have called unchanged dependency only once" );
                }
            }

            public void newDependency( Dependency dependency )
            {
                if ( !newDependency.equals( dependency ) )
                {
                    fail( "Called new dependency with an unexpected dependency " + dependency );
                }
            }

            public void removedDependency( Dependency dependency )
            {
                fail( "Should have failed to trigger this callback" );
            }

            public void updatedVersion( Dependency dependency, String previousVersion )
            {
                fail( "Should have failed to trigger this callback" );
            }

            public void updatedScope( Dependency dependency, String previousScope )
            {
                fail( "Should have failed to trigger this callback" );
            }

            public void updatedOptionalFlag( Dependency dependency, boolean previousOptional )
            {
                fail( "Should have failed to trigger this callback" );
            }

            public void updatedUnknown( Dependency dependency, Dependency previousDep )
            {
                fail( "Should have failed to trigger this callback" );
            }
        } );

    }

    public void testDependencyAnalysisWithRemovedDependency()
    {
        final List dependencies = new ArrayList();
        dependencies.add( createDependency( "groupTest", "artifactTest", "1.0" ) );
        final Dependency removedDependency = createDependency( "groupTest", "removedDep", "5.2" );
        dependencies.add( removedDependency );
        final WebappStructure cache = new WebappStructure( dependencies );

        final List newDependencies = new ArrayList( dependencies );
        newDependencies.remove( removedDependency );
        final WebappStructure webappStructure = new WebappStructure( newDependencies, cache );

        webappStructure.analyseDependencies( new WebappStructure.DependenciesAnalysisCallback()
        {

            int count = 0;

            public void unchangedDependency( Dependency dependency )
            {
                if ( count == 0 )
                {
                    count++;
                }
                else
                {
                    fail( "Should have called unchanged dependency only once" );
                }
            }

            public void newDependency( Dependency dependency )
            {
                fail( "Should have failed to trigger this callback" );
            }

            public void removedDependency( Dependency dependency )
            {
                if ( !removedDependency.equals( dependency ) )
                {
                    fail( "Called removed dependency with an unexpected dependency " + dependency );
                }
            }

            public void updatedVersion( Dependency dependency, String previousVersion )
            {
                fail( "Should have failed to trigger this callback" );
            }

            public void updatedScope( Dependency dependency, String previousScope )
            {
                fail( "Should have failed to trigger this callback" );
            }

            public void updatedOptionalFlag( Dependency dependency, boolean previousOptional )
            {
                fail( "Should have failed to trigger this callback" );
            }

            public void updatedUnknown( Dependency dependency, Dependency previousDep )
            {
                fail( "Should have failed to trigger this callback" );
            }
        } );

    }

    public void testUnknownFileNotAvailable()
    {
        final WebappStructure structure = new WebappStructure( new ArrayList() );
        assertFalse( structure.isRegistered( "/foo/bar.txt" ) );
    }

    public void testRegisterSamePathTwice()
    {
        final WebappStructure structure = new WebappStructure( new ArrayList() );
        structure.registerFile( "overlay1", "WEB-INF/web.xml" );
        assertFalse( structure.registerFile( "currentBuild", "WEB-INF/web.xml" ) );

    }

    public void testRegisterForced()
    {
        final String path = "WEB-INF/web.xml";
        final WebappStructure structure = new WebappStructure( new ArrayList() );
        assertFalse("New file should return false",
                    structure.registerFileForced( "overlay1", path ));
        assertEquals( "overlay1", structure.getOwner( path ) );         
    }

    public void testRegisterSamePathTwiceForced()
    {
        final String path = "WEB-INF/web.xml";
        final WebappStructure structure = new WebappStructure( new ArrayList() );
        structure.registerFile( "overlay1", path );
        assertEquals( "overlay1", structure.getOwner( path ) );
        assertTrue("owner replacement should have returned true",
                   structure.registerFileForced( "currentBuild", path ));
        assertEquals("currentBuild", structure.getOwner( path ));
    }


    protected Dependency createDependency( String groupId, String artifactId, String version, String type, String scope,
                                           String classifier )
    {
        final Dependency dep = new Dependency();
        dep.setGroupId( groupId );
        dep.setArtifactId( artifactId );
        dep.setVersion( version );
        if ( type == null )
        {
            dep.setType( "jar" );
        }
        else
        {
            dep.setType( type );
        }
        if ( scope != null )
        {
            dep.setScope( scope );
        }
        else
        {
            dep.setScope( Artifact.SCOPE_COMPILE );
        }
        if ( classifier != null )
        {
            dep.setClassifier( classifier );
        }
        return dep;
    }

    protected Dependency createDependency( String groupId, String artifactId, String version, String type,
                                           String scope )
    {
        return createDependency( groupId, artifactId, version, type, scope, null );
    }

    protected Dependency createDependency( String groupId, String artifactId, String version, String type )
    {
        return createDependency( groupId, artifactId, version, type, null );
    }

    protected Dependency createDependency( String groupId, String artifactId, String version )
    {
        return createDependency( groupId, artifactId, version, null );
    }
}
