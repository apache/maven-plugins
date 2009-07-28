package org.apache.maven.plugin.eclipse.writers;

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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.writers.testutils.TestEclipseWriterConfig;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.tools.easymock.TestFileManager;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jdom.JDOMException;
import junit.framework.TestCase;

public class EclipseProjectWriterTest
    extends TestCase
{
    private TestFileManager fileManager = new TestFileManager( "EclipseProjectWriter.unitTest.", "" );

    public EclipseProjectWriterTest( String name )
    {
        super( name );
    }

    protected void setUp()
        throws Exception
    {

    }

    protected void tearDown()
        throws Exception
    {
        fileManager.cleanUp();
    }

    public void testWrite_preservingLinkedResources()
        throws MojoExecutionException, JDOMException, IOException, XmlPullParserException
    {

        // create the config and the logger
        TestEclipseWriterConfig config = new TestEclipseWriterConfig();
        TestLog log = new TestLog();

        // setup the eclipse project
        File basedir = fileManager.createTempDir();
        config.setProjectBaseDir( basedir );
        config.setEclipseProjectDirectory( basedir );
        config.setEclipseProjectName( "test-project" );
        MavenProject project = new MavenProject();
        config.setProject( project );
        EclipseProjectWriter projectWriter = new EclipseProjectWriter();
        // create the .project file and start writing the contents
        File dotProject = new File( config.getEclipseProjectDirectory(), ".project" );
        Writer w = new OutputStreamWriter( new FileOutputStream( dotProject ), "UTF-8" );
        XMLWriter writer = new PrettyPrintXMLWriter( w );

        writer.startElement( "projectDescription" );

        writer.startElement( "name" );
        writer.writeText( "test-project" );
        writer.endElement();// name

        writer.startElement( "linkedResources" );
        writer.startElement( "link" );

        writer.startElement( "name" );
        writer.writeText( "linkTest" );
        writer.endElement();// name

        writer.startElement( "type" );
        writer.writeText( "2" );
        writer.endElement();// type

        writer.startElement( "location" );
        writer.writeText( basedir + "/dummyName" );
        writer.endElement(); // location

        writer.endElement();// link
        writer.endElement();// linkedResources
        writer.endElement();// projectDescription

        IOUtil.close( w );
        // parse the file we just created in order to keep manually-added linkedResources
        // pre setup
        Reader reader = null;
        reader = new InputStreamReader( new FileInputStream( dotProject ), "UTF-8" );
        Xpp3Dom dom = Xpp3DomBuilder.build( reader );
        Xpp3Dom linkedResourcesElement = dom.getChild( "linkedResources" );
        Xpp3Dom[] existingLinks = linkedResourcesElement.getChildren( "link" );
        String existingName = existingLinks[0].getChild( "name" ).getValue();
        String existingType = existingLinks[0].getChild( "type" ).getValue();
        String existingLocation = existingLinks[0].getChild( "location" ).getValue();
        reader.close();
        // call the projectwriter to write the .project file
        projectWriter.init( log, config );
        projectWriter.write();
        // post check..compare the pre values to make sure the writer preserves the LinkedResources
        reader = new InputStreamReader( new FileInputStream( dotProject ), "UTF-8" );
        Xpp3Dom linkedResourcesElement1 = dom.getChild( "linkedResources" );
        assertNotNull( "No linkedResources present", linkedResourcesElement1 );
        Xpp3Dom[] currentLinks = linkedResourcesElement.getChildren( "link" );
        String currentName = existingLinks[0].getChild( "name" ).getValue();
        String currentType = existingLinks[0].getChild( "type" ).getValue();
        String currentLocation = existingLinks[0].getChild( "location" ).getValue();
        assertEquals( "link name is not equal", existingName, currentName );
        assertEquals( "link type is not equal", existingType, currentType );
        assertEquals( "link location is not equal", existingLocation, currentLocation );

        reader.close();

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
