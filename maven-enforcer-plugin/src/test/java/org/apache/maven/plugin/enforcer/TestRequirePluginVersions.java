package org.apache.maven.plugin.enforcer;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * 
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public class TestRequirePluginVersions
    extends AbstractMojoTestCase
{
    public void testCheckIfModelMatches ()
    {

        RequirePluginVersions rule = new RequirePluginVersions();

        Model model = new Model();
        model.setArtifactId( "" );
        model.setGroupId( "" );
        model.setVersion( "" );

        // should generate internal NPE on the parent, but
        // will still
        // compare the raw values
        assertTrue( rule.checkIfModelMatches( "", "", "", model ) );
        assertFalse( rule.checkIfModelMatches( "", "", "1.0", model ) );

        // now setup a parent
        Parent parent = new Parent();
        parent.setArtifactId( "foo" );
        parent.setGroupId( "foo-group" );
        parent.setVersion( "1.0" );
        model.setParent( parent );

        // should NOT pickup the parent artifact
        assertFalse( rule.checkIfModelMatches( "foo-group", "foo", "1.0", model ) );

        // check that the version and group are inherited
        // from the parent.
        assertTrue( rule.checkIfModelMatches( "foo-group", "", "1.0", model ) );

        // check handling of nulls
        assertFalse( rule.checkIfModelMatches( "foo-group", null, "1.0", model ) );
    }

    public void testHasVersionSpecified ()
    {
        Plugin source = new Plugin();
        source.setArtifactId( "foo" );
        source.setGroupId( "group" );

        // setup the plugins. I'm setting up the foo group
        // with a few bogus entries and then a real one.
        // this is to test that the list is exhaustively
        // searched for versions before giving up.
        // banLatest/Release will fail if it is found
        // anywhere in the list
        List plugins = new ArrayList();
        plugins.add( EnforcerTestUtils.newPlugin( "group", "a-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo", null ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo", "" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "b-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "foo", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "c-artifact", "LATEST" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "c-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "d-artifact", "RELEASE" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "d-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "e-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "e-artifact", "RELEASE" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "f-artifact", "1.0" ) );
        plugins.add( EnforcerTestUtils.newPlugin( "group", "f-artifact", "LATEST" ) );

        RequirePluginVersions rule = new RequirePluginVersions();
        rule.setBanLatest( false );
        rule.setBanRelease( false );

        assertTrue( rule.hasVersionSpecified( source, plugins ) );

        // check that LATEST is allowed
        source.setArtifactId( "c-artifact" );
        assertTrue( rule.hasVersionSpecified( source, plugins ) );

        // check that LATEST is banned
        rule.setBanLatest( true );
        assertFalse( rule.hasVersionSpecified( source, plugins ) );

        // check that LATEST is exhausively checked
        source.setArtifactId( "f-artifact" );
        assertFalse( rule.hasVersionSpecified( source, plugins ) );

        // check that RELEASE is allowed
        source.setArtifactId( "d-artifact" );
        assertTrue( rule.hasVersionSpecified( source, plugins ) );

        // check that RELEASE is banned
        rule.setBanRelease( true );
        assertFalse( rule.hasVersionSpecified( source, plugins ) );

        // check that RELEASE is exhausively checked
        source.setArtifactId( "e-artifact" );
        assertFalse( rule.hasVersionSpecified( source, plugins ) );
    }

    public void testGetModelsRecursivelyBottom ()
        throws ArtifactResolutionException, ArtifactNotFoundException, IOException, XmlPullParserException
    {
        RequirePluginVersions rule = new RequirePluginVersions();
        String path = "target/test-classes/requirePluginVersions/getPomRecursively/b/c";

        StringUtils.replace( path, "/", File.separator );

        File pom = new File( getBasedir() + File.separator + path, "pom.xml" );

        List models = rule.getModelsRecursively( "group", "c", "1.0", pom );

        // there should be 3
        assertEquals( 3, models.size() );

        // now make sure they are all there
        Model m = new Model();
        m.setGroupId( "group" );
        m.setVersion( "1.0" );
        m.setArtifactId( "c" );

        models.contains( m );

        m.setArtifactId( "b" );
        models.contains( m );

        m.setArtifactId( "a" );
        models.contains( m );
    }

    public void testGetModelsRecursivelyTop ()
        throws ArtifactResolutionException, ArtifactNotFoundException, IOException, XmlPullParserException
    {
        RequirePluginVersions rule = new RequirePluginVersions();
        String path = "target/test-classes/requirePluginVersions/getPomRecursively";

        StringUtils.replace( path, "/", File.separator );

        File pom = new File( getBasedir() + File.separator + path, "pom.xml" );

        List models = rule.getModelsRecursively( "group", "a", "1.0", pom );

        // there should be 1
        assertEquals( 1, models.size() );

        // now make sure they are all there
        Model m = new Model();
        m.setGroupId( "group" );
        m.setVersion( "1.0" );
        m.setArtifactId( "a" );

        models.contains( m );
    }

    public void testGetAllPlugins ()
        throws ArtifactResolutionException, ArtifactNotFoundException, IOException, XmlPullParserException
    {
        RequirePluginVersions rule = new RequirePluginVersions();
        String path = "target/test-classes/requirePluginVersions/getPomRecursively/b/c";

        StringUtils.replace( path, "/", File.separator );

        File projectDir = new File( getBasedir(), path );

        MockProject project = new MockProject();
        project.setArtifactId( "c" );
        project.setGroupId( "group" );
        project.setVersion( "1.0" );
        project.setBaseDir( projectDir );

        List plugins = rule.getAllPluginEntries( project );

        // there should be 3
        assertEquals( 3, plugins.size() );
    }
}
