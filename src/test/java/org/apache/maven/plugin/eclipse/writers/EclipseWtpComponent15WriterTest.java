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
package org.apache.maven.plugin.eclipse.writers;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.writers.testutils.TestEclipseWriterConfig;
import org.apache.maven.plugin.eclipse.writers.wtp.EclipseWtpComponent15Writer;
import org.apache.maven.plugin.eclipse.writers.wtp.EclipseWtpComponentWriter;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.tools.easymock.TestFileManager;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * Component writer test for WTP 1.5.
 * 
 * @author Steffen Grunwald
 */
public class EclipseWtpComponent15WriterTest
    extends TestCase
{

    private TestFileManager fileManager = new TestFileManager( "EclipseWtpComponent15Writer.unitTest.", "" );

    protected void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    /**
     * Tests the creation of the ejb module references in the org.eclipse.wst.common.component file for:
     * <ul>
     * <li>component file of EAR
     * <li>WTP 1.5
     * <li>dep is referenced project
     * </ul>
     * The archivename is expected to be jar - independent from the packaging (ejb).
     * 
     * @throws MojoExecutionException Exception
     * @throws IOException Exception
     * @throws JDOMException Exception
     */
    public void testWriteEjbComponentMECLIPSE455()
        throws MojoExecutionException, IOException, JDOMException
    {

        TestEclipseWriterConfig config = new TestEclipseWriterConfig();

        config.setWtpVersion( 1.5f );
        config.setEclipseProjectName( "test-project" );

        File basedir = fileManager.createTempDir();
        File pom = new File( basedir, "pom.xml" );
        pom.createNewFile();

        MavenProject project = new MavenProject();
        project.setFile( pom );

        config.setProject( project );
        config.setProjectBaseDir( basedir );

        config.setEclipseProjectDirectory( basedir );
        config.setPackaging( "ear" );

        // add an ejb3 and ejb packaged dependency
        config.setDeps( new IdeDependency[] { createDep( "ejb" ), createDep( "jar" ) } );

        EclipseWtpComponentWriter lWriter = new EclipseWtpComponent15Writer();

        Log log = new TestLog();

        lWriter.init( log, config );

        lWriter.write();

        // now check extension of archivenames to be jar
        SAXBuilder builder = new SAXBuilder( false );

        Document doc = builder.build( new File( basedir, ".settings/org.eclipse.wst.common.component" ) );

        XPath archiveNames = XPath.newInstance( "//dependent-module/@archiveName" );

        assertEquals( "Must be 2 modules", 2, archiveNames.selectNodes( doc ).size() );
        for ( Iterator it = archiveNames.selectNodes( doc ).iterator(); it.hasNext(); )
        {
            Attribute attribute = (Attribute) it.next();

            String archiveName = attribute.getValue();
            String extension = archiveName.substring( archiveName.lastIndexOf( "." ) + 1 ).toLowerCase();

            assertEquals( "Must be of type jar", "jar", extension );
        }

    }

    private IdeDependency createDep( String packagingType )
    {
        IdeDependency dependency = new IdeDependency();
        dependency.setGroupId( "g" );
        dependency.setArtifactId( packagingType + "Artifact" );
        dependency.setVersion( "v" );
        dependency.setReferencedProject( true );
        dependency.setAddedToClasspath( true );
        dependency.setEclipseProjectName( packagingType + "Project" );
        dependency.setType( packagingType );
        return dependency;
    }

    private static final class TestLog
        extends SystemStreamLog
    {
        public boolean isDebugEnabled()
        {
            return true;
        }
    }

}
