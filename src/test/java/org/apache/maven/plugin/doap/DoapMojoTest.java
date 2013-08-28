package org.apache.maven.plugin.doap;

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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.maven.plugin.doap.options.DoapArtifact;
import org.apache.maven.plugin.doap.options.DoapOptions;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * Test {@link DoapMojo} class.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class DoapMojoTest
    extends AbstractMojoTestCase
{
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    /**
     * Verify the generation of a pure DOAP file.
     *
     * @throws Exception if any
     */
    public void testGeneratedDoap()
        throws Exception
    {
        File pluginXmlFile =
            new File( getBasedir(), "src/test/resources/unit/doap-configuration/doap-configuration-plugin-config.xml" );
        DoapMojo mojo = (DoapMojo) lookupMojo( "generate", pluginXmlFile );
        assertNotNull( "Mojo found.", mojo );

        MavenProject mavenProject = (MavenProject) getVariableValueFromObject( mojo, "project" );
        assertNotNull( mavenProject );

        // Set some Mojo parameters
        setVariableValueToObject( mojo, "remoteRepositories", mavenProject.getRemoteArtifactRepositories() );
        setVariableValueToObject( mojo, "about", mavenProject.getUrl() );

        mojo.execute();

        File doapFile = new File( getBasedir(), "target/test/unit/doap-configuration/doap-configuration.rdf" );
        assertTrue( "Doap File was not generated!", doapFile.exists() );

        String readed = readFile( doapFile );

        // Validate

        // Pure DOAP
        assertTrue( readed.contains( "<rdf:RDF xml:lang=\"en\" xmlns=\"http://usefulinc.com/ns/doap#\" "
            + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
            + "xmlns:foaf=\"http://xmlns.com/foaf/0.1/\">" ) );
        assertTrue( readed.contains( "<Project rdf:about=\"" + mavenProject.getUrl() + "\">" ) );
        assertTrue( readed.contains( "<description xml:lang=\"en\">Test the DOAP plugin</description>" ) );
        assertTrue( readed.contains( "<shortdesc xml:lang=\"en\">Test the DOAP plugin</shortdesc>" ) );
        assertTrue( readed.contains( "<homepage rdf:resource=\"" + mavenProject.getUrl() + "\"/>" ) );
        assertTrue( readed.contains( "<category>library</category>" ) );
        assertTrue( readed.contains( "<created>2008-01-01</created>" ) );
        assertTrue( readed.contains( "<name>" + mavenProject.getName() + "</name>" ) );
        assertTrue( readed.contains( "<download-page rdf:resource=\"http://foo.org/download.html\"/>" ) );
        assertTrue( readed.contains( "<programming-language>Java</programming-language>" ) );
        assertTrue( readed.contains( "<bug-database rdf:resource=\"http://jira.codehaus.org/browse/MDOAPTEST\"/>" ) );
        assertTrue( readed.contains( "<license rdf:resource=\"http://www.apache.org/licenses/LICENSE-2.0.txt\"/>" ) );
        assertTrue( readed.contains( "<SVNRepository>" ) );
        assertTrue( readed.contains( "<location rdf:resource=\"http://svn.foo.org/repos/asf/maven/plugins/trunk/maven-doap-plugin/src/test/resources/unit/doap-configuration\"/>" ) );
        assertTrue( readed.contains( "<browse rdf:resource=\"http://svn.foo.org/viewvc/maven/plugins/trunk/maven-doap-plugin/src/test/resources/unit/doap-configuration\"/>" ) );
        assertTrue( readed.contains( "<location rdf:resource=\"https://svn.foo.org/repos/asf/maven/plugins/trunk/maven-doap-plugin/src/test/resources/unit/doap-configuration\"/>" ) );

        // conf
        assertTrue( readed.contains( "<audience>developers</audience>" ) );
        assertTrue( readed.contains( "<blog rdf:resource=\"http://myblog.foo.org\"/>" ) );
        assertTrue( readed.contains( "<implements>JSR-foo</implements>" ) );
        assertTrue( readed.contains( "<language>en</language>" ) );
        assertTrue( readed.contains( "<language>fr</language>" ) );
        assertTrue( readed.contains( "<old-homepage rdf:resource=\"http://old.foo.org\"/>" ) );
        assertTrue( readed.contains( "<os>windows</os>" ) );
        assertTrue( readed.contains( "<os>linux</os>" ) );
        assertTrue( readed.contains( "<os>mac</os>" ) );
        assertTrue( readed.contains( "<platform>java</platform>" ) );
        assertTrue( readed.contains( "<platform>firefox</platform>" ) );
        assertTrue( readed.contains( "<screenshots rdf:resource=\"" + mavenProject.getUrl() +"/screenshots.html\"/>" ) );
        assertTrue( readed.contains( "<service-endpoint rdf:resource=\"http://webservice.foo.org\"/>" ) );
        assertTrue( readed.contains( "<wiki rdf:resource=\"http://wiki.foo.org\"/>" ) );

        // ASF ext
        assertFalse( readed.contains( "<asfext:pmc rdf:resource=\"" + mavenProject.getUrl() + "\"/>" ) );
        assertFalse( readed.contains( "<asfext:name>" + mavenProject.getName() + "</name>" ) );

        // Developers and Organizations
        assertTrue( readed.contains( "<maintainer>" ) );
        assertTrue( readed.contains( "<foaf:Person rdf:nodeID=\"b" ) );
        assertTrue( readed.contains( "<foaf:name>Jane Doe</foaf:name>" ) );
        assertTrue( readed.contains( "<foaf:Organization>" ) );
        assertTrue( readed.contains( "<foaf:homepage rdf:resource=\"http://www.example.org\"/>" ) );
        assertTrue( readed.contains( "<foaf:member rdf:nodeID=\"b" ) );
    }

    /**
     * @throws Exception if any
     */
    public void testLangParameter()
        throws Exception
    {
        File pluginXmlFile =
            new File( getBasedir(), "src/test/resources/unit/doap-configuration/doap-configuration-plugin-config.xml" );
        DoapMojo mojo = (DoapMojo) lookupMojo( "generate", pluginXmlFile );
        assertNotNull( "Mojo found.", mojo );

        MavenProject mavenProject = (MavenProject) getVariableValueFromObject( mojo, "project" );
        assertNotNull( mavenProject );

        // check invalid lang
        setVariableValueToObject( mojo, "remoteRepositories", mavenProject.getRemoteArtifactRepositories() );
        setVariableValueToObject( mojo, "lang", "foo" );
        try
        {
            mojo.execute();
            assertTrue( "No lang checked", false );
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }
    }

    /**
     * @throws Exception if any
     */
    public void testAboutParameter()
        throws Exception
    {
        File pluginXmlFile =
            new File( getBasedir(), "src/test/resources/unit/doap-configuration/doap-configuration-plugin-config.xml" );
        DoapMojo mojo = (DoapMojo) lookupMojo( "generate", pluginXmlFile );
        assertNotNull( "Mojo found.", mojo );

        MavenProject mavenProject = (MavenProject) getVariableValueFromObject( mojo, "project" );
        assertNotNull( mavenProject );

        // check invalid lang
        setVariableValueToObject( mojo, "remoteRepositories", mavenProject.getRemoteArtifactRepositories() );
        setVariableValueToObject( mojo, "about", "foo" );
        try
        {
            mojo.execute();
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }
    }

    /**
     * Verify the generation of a DOAP file from an artifact.
     *
     * @throws Exception if any
     */
    public void testGeneratedDoapArtifact()
        throws Exception
    {
        File pluginXmlFile =
            new File( getBasedir(), "src/test/resources/unit/doap-configuration/doap-configuration-plugin-config.xml" );
        DoapMojo mojo = (DoapMojo) lookupMojo( "generate", pluginXmlFile );
        assertNotNull( "Mojo found.", mojo );

        MavenProject mavenProject = (MavenProject) getVariableValueFromObject( mojo, "project" );
        assertNotNull( mavenProject );

        // Set some Mojo parameters
        setVariableValueToObject( mojo, "remoteRepositories", mavenProject.getRemoteArtifactRepositories() );
        setVariableValueToObject( mojo, "about", mavenProject.getUrl() );
        DoapOptions doapOptions = (DoapOptions)getVariableValueFromObject(  mojo, "doapOptions" );
        doapOptions.setDescription( "Common Utilities" );
        doapOptions.setShortdesc( "Common Utilities" );
        doapOptions.setDownloadPage( "http://plexus.codehaus.org/download-binaries.html" );
        setVariableValueToObject( mojo, "doapOptions", doapOptions );

        DoapArtifact artifact = new DoapArtifact();
        artifact.setGroupId( "org.codehaus.plexus" );
        artifact.setArtifactId( "plexus-utils" );
        artifact.setVersion( "1.5.5" );
        setVariableValueToObject( mojo, "artifact", artifact );
        setVariableValueToObject( mojo, "outputDirectory", "target/test/unit/doap-configuration/" );

        mojo.execute();

        File doapFile = new File( getBasedir(), "target/test/unit/doap-configuration/doap_plexus-utils.rdf" );
        assertTrue( "Doap File was not generated!", doapFile.exists() );

        String readed = readFile( doapFile );

        // Validate

        // Pure DOAP
        assertTrue( readed.contains( "<rdf:RDF xml:lang=\"en\" xmlns=\"http://usefulinc.com/ns/doap#\" "
            + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
            + "xmlns:foaf=\"http://xmlns.com/foaf/0.1/\">" ) );
        assertTrue( readed.contains( "<Project rdf:about=\"http://plexus.codehaus.org/plexus-utils\">" ) );
        assertTrue( readed.contains( "<name>Plexus Common Utilities</name>" ) );
        assertTrue( readed.contains( "<description xml:lang=\"en\">Common Utilities</description>" ) );
        assertTrue( readed.contains( "<shortdesc xml:lang=\"en\">Common Utilities</shortdesc>" ) );
        assertTrue( readed.contains( "<created>2001-01-01</created>" ) );
        assertTrue( readed.contains( "<download-page rdf:resource=\"http://plexus.codehaus.org/download-binaries.html\"/>" ) );
        assertTrue( readed.contains( "<programming-language>Java</programming-language>" ) );
        assertTrue( readed.contains( "<bug-database rdf:resource=\"http://jira.codehaus.org/browse/PLXUTILS\"/>" ) );
        assertTrue( readed.contains( "<license rdf:resource=\"http://www.apache.org/licenses/LICENSE-2.0.txt\"/>" ) );
        assertTrue( readed.contains( "<SVNRepository>" ) );
        assertTrue( readed.contains( "<location rdf:resource=\"http://svn.codehaus.org/plexus/plexus-utils/tags/plexus-utils-1.5.5\"/>" ) );
        assertTrue( readed.contains( "<browse rdf:resource=\"http://fisheye.codehaus.org/browse/plexus/plexus-utils/tags/plexus-utils-1.5.5\"/>" ) );

        // conf
        assertTrue( readed.contains( "<audience>developers</audience>" ) );
        assertTrue( readed.contains( "<blog rdf:resource=\"http://myblog.foo.org\"/>" ) );
        assertTrue( readed.contains( "<implements>JSR-foo</implements>" ) );
        assertTrue( readed.contains( "<language>en</language>" ) );
        assertTrue( readed.contains( "<language>fr</language>" ) );
        assertTrue( readed.contains( "<old-homepage rdf:resource=\"http://old.foo.org\"/>" ) );
        assertTrue( readed.contains( "<os>windows</os>" ) );
        assertTrue( readed.contains( "<os>linux</os>" ) );
        assertTrue( readed.contains( "<os>mac</os>" ) );
        assertTrue( readed.contains( "<platform>java</platform>" ) );
        assertTrue( readed.contains( "<screenshots rdf:resource=\"http://plexus.codehaus.org/plexus-utils/screenshots.html\"/>" ) );
        assertTrue( readed.contains( "<service-endpoint rdf:resource=\"http://webservice.foo.org\"/>" ) );
        assertTrue( readed.contains( "<wiki rdf:resource=\"http://wiki.foo.org\"/>" ) );
    }

    /**
     * Verify the generation of a DOAP file from a minimalist artifact.
     *
     * @throws Exception if any
     */
    public void testGeneratedDoapArtifactMinimalist()
        throws Exception
    {
        File pluginXmlFile =
            new File( getBasedir(), "src/test/resources/unit/doap-configuration/doap-configuration-plugin-config.xml" );
        DoapMojo mojo = (DoapMojo) lookupMojo( "generate", pluginXmlFile );
        assertNotNull( "Mojo found.", mojo );

        MavenProject mavenProject = (MavenProject) getVariableValueFromObject( mojo, "project" );
        assertNotNull( mavenProject );

        // Set some Mojo parameters
        setVariableValueToObject( mojo, "remoteRepositories", mavenProject.getRemoteArtifactRepositories() );
        setVariableValueToObject( mojo, "about", "foo" );
        DoapOptions doapOptions = new DoapOptions();
        doapOptions.setName( "XStream" );
        doapOptions.setDescription( "XStream is a simple library to serialize objects to XML and back again." );
        doapOptions.setShortdesc( "XML Serializer" );
        doapOptions.setHomepage( "http://xstream.codehaus.org/" );
        doapOptions.setDownloadPage( "http://xstream.codehaus.org/download.html" );
        doapOptions.setBugDatabase( "http://jira.codehaus.org/browse/XSTR" );
        doapOptions.setLicense( "http://xstream.codehaus.org/license.html" );
        doapOptions.setScmDeveloper( "http://svn.codehaus.org/xstream/trunk/xstream" );
        doapOptions.setMailingList( "http://xstream.codehaus.org/list-user.html" );
        doapOptions.setCreated( "2000-01-01");
        setVariableValueToObject( mojo, "doapOptions", doapOptions );

        DoapArtifact artifact = new DoapArtifact();
        artifact.setGroupId( "xstream" );
        artifact.setArtifactId( "xstream" );
        artifact.setVersion( "1.1" );
        setVariableValueToObject( mojo, "artifact", artifact );
        setVariableValueToObject( mojo, "outputDirectory", "target/test/unit/doap-configuration/" );

        mojo.execute();

        File doapFile = new File( getBasedir(), "target/test/unit/doap-configuration/doap_xstream.rdf" );
        assertTrue( "Doap File was not generated!", doapFile.exists() );

        String readed = readFile( doapFile );

        // Validate

        // Pure DOAP
        assertTrue( readed.contains( "<rdf:RDF xml:lang=\"en\" xmlns=\"http://usefulinc.com/ns/doap#\" "
            + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
            + "xmlns:foaf=\"http://xmlns.com/foaf/0.1/\">" ) );
        assertTrue( readed.contains( "<Project>" ) );
        assertTrue( readed.contains( "<name>XStream</name>" ) );
        assertTrue( readed.contains( "<description xml:lang=\"en\">XStream is a simple library to serialize objects to XML and back again.</description>" ) );
        assertTrue( readed.contains( "<shortdesc xml:lang=\"en\">XML Serializer</shortdesc>" ) );
        assertTrue( readed.contains( "<created>2000-01-01</created>" ) );
        assertTrue( readed.contains( "<download-page rdf:resource=\"http://xstream.codehaus.org/download.html\"/>" ) );
        assertTrue( readed.contains( "<programming-language>Java</programming-language>" ) );
        assertTrue( readed.contains( "<bug-database rdf:resource=\"http://jira.codehaus.org/browse/XSTR\"/>" ) );
        assertTrue( readed.contains( "<license rdf:resource=\"http://xstream.codehaus.org/license.html\"/>" ) );
        assertTrue( readed.contains( "<Repository>" ) );
        assertTrue( readed.contains( "<location rdf:resource=\"http://svn.codehaus.org/xstream/trunk/xstream\"/>" ) );
        assertTrue( readed.contains( "<mailing-list rdf:resource=\"http://xstream.codehaus.org/list-user.html\"/>" ) );

        // conf
        assertFalse( readed.contains( "<audience>" ) );
        assertFalse( readed.contains( "<blog rdf:resource=" ) );
        assertFalse( readed.contains( "<implements>" ) );
        assertFalse( readed.contains( "<language>" ) );
        assertFalse( readed.contains( "<old-homepage rdf:resource=" ) );
        assertFalse( readed.contains( "<os>" ) );
        assertFalse( readed.contains( "<platform>" ) );
        assertFalse( readed.contains( "<screenshots rdf:resource=" ) );
        assertFalse( readed.contains( "<service-endpoint rdf:resource=" ) );
        assertFalse( readed.contains( "<wiki rdf:resource=" ) );
    }

    /**
     * Verify the generation of a DOAP file with ASF extension.
     *
     * @throws Exception if any
     */
    public void testGeneratedDoapForASF()
        throws Exception
    {
        File pluginXmlFile =
            new File( getBasedir(),
                      "src/test/resources/unit/asf-doap-configuration/asf-doap-configuration-plugin-config.xml" );
        DoapMojo mojo = (DoapMojo) lookupMojo( "generate", pluginXmlFile );
        assertNotNull( "Mojo found.", mojo );

        MavenProject mavenProject = (MavenProject) getVariableValueFromObject( mojo, "project" );
        assertNotNull( mavenProject );

        // Set some Mojo parameters
        setVariableValueToObject( mojo, "remoteRepositories", mavenProject.getRemoteArtifactRepositories() );
        setVariableValueToObject( mojo, "about", mavenProject.getUrl() );

        mojo.execute();

        File doapFile = new File( getBasedir(), "target/test/unit/asf-doap-configuration/asf-doap-configuration.rdf" );
        assertTrue( "Doap File was not generated!", doapFile.exists() );

        String readed = readFile( doapFile );

        // Validate

        // ASF DOAP
        assertTrue( readed.contains( "<rdf:RDF xml:lang=\"en\" xmlns=\"http://usefulinc.com/ns/doap#\" "
            + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
            + "xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" " + "xmlns:asfext=\"http://projects.apache.org/ns/asfext#\">" ) );
        if ( StringUtils.isNotEmpty( mavenProject.getUrl() ) )
        {
            assertTrue( readed.contains( "<Project rdf:about=\"" + mavenProject.getUrl() + "\">" ) );
            assertTrue( readed.contains( "<homepage rdf:resource=\"" + mavenProject.getUrl() + "\"/>" ) );
        }
        assertTrue( readed.contains( "<name>Apache " + mavenProject.getName() + "</name>" ) );
        assertTrue( readed.contains( "<programming-language>Java</programming-language>" ) );
        assertTrue( readed.contains( "<category rdf:resource=\"http://projects.apache.org/category/library\"/>" ) );

        // ASF ext
        assertTrue( readed.contains( "<asfext:pmc rdf:resource=\"" + mavenProject.getUrl() + "\"/>" ) );
        assertTrue( readed.contains( "<asfext:name>Apache " + mavenProject.getName() + "</asfext:name>" ) );
        assertTrue( readed.contains( "<asfext:charter>" ) );
        assertTrue( readed.contains( "<asfext:chair>" ) );
    }

    /**
     * Verify the generation of a DOAP file with extra extension.
     *
     * @throws Exception if any
     */
    public void testGeneratedExtraDoap()
        throws Exception
    {
        File pluginXmlFile =
            new File( getBasedir(),
                      "src/test/resources/unit/doap-configuration/doap-extra-configuration-plugin-config.xml" );
        DoapMojo mojo = (DoapMojo) lookupMojo( "generate", pluginXmlFile );
        assertNotNull( "Mojo found.", mojo );

        MavenProject mavenProject = (MavenProject) getVariableValueFromObject( mojo, "project" );
        assertNotNull( mavenProject );

        // Set some Mojo parameters
        setVariableValueToObject( mojo, "remoteRepositories", mavenProject.getRemoteArtifactRepositories() );
        setVariableValueToObject( mojo, "about", mavenProject.getUrl() );

        mojo.execute();

        File doapFile = new File( getBasedir(), "target/test/unit/doap-configuration/doap-extra-configuration.rdf" );
        assertTrue( "Doap File was not generated!", doapFile.exists() );

        String readed = readFile( doapFile );

        assertTrue( readed.contains( "<ciManagement rdf:resource=\"http://ci.foo.org\"/>" ) );
        assertTrue( readed.contains( "<asfext:status>active</asfext:status>" ) );
        assertTrue( readed.contains( "<labs:status>active</labs:status>" ) );
    }

    /**
     * @param file
     * @return
     * @throws IOException if any
     */
    private String readFile( File file )
        throws IOException
    {
        String result = null;

        FileReader reader = null;
        try
        {
            // platform encoding
            reader = new FileReader( file );

            result = IOUtil.toString( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }

        return result;
    }
}
