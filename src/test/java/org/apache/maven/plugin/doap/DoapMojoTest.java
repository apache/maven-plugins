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
        if ( StringUtils.isNotEmpty( mavenProject.getUrl() ) )
        {
            assertTrue( readed.contains( "<Project rdf:about=\"" + mavenProject.getUrl() + "\">" ) );
            assertTrue( readed.contains( "<homepage rdf:resource=\"" + mavenProject.getUrl() + "\"/>" ) );
        }
        assertTrue( readed.contains( "<name>" + mavenProject.getName() + "</name>" ) );
        assertTrue( readed.contains( "<programming-language>java</programming-language>" ) );

        // ASF ext
        assertFalse( readed.contains( "<asfext:pmc rdf:resource=\"" + mavenProject.getUrl() + "\"/>" ) );
        assertFalse( readed.contains( "<asfext:name>" + mavenProject.getName() + "</name>" ) );

        // Developers and Organizations
        assertTrue( readed.contains( "<maintainer>" ) );
        assertTrue( readed.contains( "<foaf:Person rdf:nodeID=\"b1\">" ) );
        assertTrue( readed.contains( "<foaf:name>Jane Doe</foaf:name>" ) );
        assertTrue( readed.contains( "<foaf:Organization>" ) );
        assertTrue( readed.contains( "<foaf:homepage rdf:resource=\"http://www.example.org\"/>" ) );
        assertTrue( readed.contains( "<foaf:member rdf:nodeID=\"b1\"/>" ) );
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
        assertTrue( readed.contains( "<name>Plexus Common Utilities</name>" ) );
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

        // ASF ext
        assertTrue( readed.contains( "<asfext:pmc rdf:resource=\"" + mavenProject.getUrl() + "\"/>" ) );
        assertTrue( readed.contains( "<asfext:name>Apache " + mavenProject.getName() + "</asfext:name>" ) );
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
