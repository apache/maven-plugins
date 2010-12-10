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
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
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
    /** {@inheritDoc} */
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    /** {@inheritDoc} */
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
        setVariableValueToObject( mojo, "remoteRepositories", getRemoteRepositories() );
        setVariableValueToObject( mojo, "about", mavenProject.getUrl() );

        mojo.execute();

        File doapFile = new File( getBasedir(), "target/test/unit/doap-configuration/doap-configuration.rdf" );
        assertTrue( "Doap File was not generated!", doapFile.exists() );

        String readed = readFile( doapFile );

        // Validate

        // Pure DOAP
        assertTrue( readed.indexOf( "<rdf:RDF xml:lang=\"en\" xmlns=\"http://usefulinc.com/ns/doap#\" "
            + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
            + "xmlns:foaf=\"http://xmlns.com/foaf/0.1/\">" ) != -1 );
        if ( StringUtils.isNotEmpty( mavenProject.getUrl() ) )
        {
            assertTrue( readed.indexOf( "<Project rdf:about=\"" + mavenProject.getUrl() + "\">" ) != -1 );
            assertTrue( readed.indexOf( "<homepage rdf:resource=\"" + mavenProject.getUrl() + "\"/>" ) != -1 );
        }
        assertTrue( readed.indexOf( "<name>" + mavenProject.getName() + "</name>" ) != -1 );
        assertTrue( readed.indexOf( "<programming-language>java</programming-language>" ) != -1 );

        // ASF ext
        assertFalse( readed.indexOf( "<asfext:pmc rdf:resource=\"" + mavenProject.getUrl() + "\"/>" ) != -1 );
        assertFalse( readed.indexOf( "<asfext:name>" + mavenProject.getName() + "</name>" ) != -1 );

        // Developers and Organizations
        assertTrue( readed.indexOf( "<maintainer>" ) != -1 );
        assertTrue( readed.indexOf( "<foaf:Person rdf:nodeID=\"b1\">" ) != -1 );
        assertTrue( readed.indexOf( "<foaf:name>Jane Doe</foaf:name>" ) != -1 );
        assertTrue( readed.indexOf( "<foaf:Organization>" ) != -1 );
        assertTrue( readed.indexOf( "<foaf:homepage rdf:resource=\"http://www.example.org\"/>" ) != -1 );
        assertTrue( readed.indexOf( "<foaf:member rdf:nodeID=\"b1\"/>" ) != -1 );
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
        setVariableValueToObject( mojo, "remoteRepositories", getRemoteRepositories() );
        setVariableValueToObject( mojo, "about", mavenProject.getUrl() );

        mojo.execute();

        File doapFile = new File( getBasedir(), "target/test/unit/asf-doap-configuration/asf-doap-configuration.rdf" );
        assertTrue( "Doap File was not generated!", doapFile.exists() );

        String readed = readFile( doapFile );

        // Validate

        // Pure DOAP
        assertTrue( readed.indexOf( "<rdf:RDF xml:lang=\"en\" xmlns=\"http://usefulinc.com/ns/doap#\" "
            + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
            + "xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" " + "xmlns:asfext=\"http://projects.apache.org/ns/asfext#\">" ) != -1 );
        if ( StringUtils.isNotEmpty( mavenProject.getUrl() ) )
        {
            assertTrue( readed.indexOf( "<Project rdf:about=\"" + mavenProject.getUrl() + "\">" ) != -1 );
            assertTrue( readed.indexOf( "<homepage rdf:resource=\"" + mavenProject.getUrl() + "\"/>" ) != -1 );
        }
        assertTrue( readed.indexOf( "<name>Apache " + mavenProject.getName() + "</name>" ) != -1 );
        assertTrue( readed.indexOf( "<programming-language>java</programming-language>" ) != -1 );

        // ASF ext
        assertTrue( readed.indexOf( "<asfext:pmc rdf:resource=\"" + mavenProject.getUrl() + "\"/>" ) != -1 );
        assertTrue( readed.indexOf( "<asfext:name>Apache " + mavenProject.getName() + "</asfext:name>" ) != -1 );
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

    /**
     * @return remote repo
     */
    private static List getRemoteRepositories()
    {
        ArtifactRepository repository =
            new DefaultArtifactRepository( "central", "http://repo1.maven.org/maven2", new DefaultRepositoryLayout() );
        return Collections.singletonList( repository );
    }
}
