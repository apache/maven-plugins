package org.apache.maven.plugin.idea;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.apache.maven.plugin.idea.stubs.SimpleMavenProjectStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.dom4j.io.SAXReader;
import org.dom4j.DocumentException;
import org.dom4j.Document;
import org.dom4j.Element;

import java.io.File;
import java.util.Iterator;

/**
 * @author Edwin Punzalan
 */
public class IdeaProjectTest
    extends AbstractMojoTestCase
{
    private IdeaProjectMojo mojo;

    public void testIdeaProjectTestEnvironment()
        throws Exception
    {
        Document iprDocument = executeMojo( "src/test/plugin-configs/min-plugin-config.xml" );

        testJdkName( iprDocument, null, null );
    }

    public void testIdeaProjectVersion4()
        throws Exception
    {
        Document iprDocument = executeMojo( "src/test/plugin-configs/plugin-config-idea4.xml" );

        Element root = iprDocument.getRootElement();

        Element component = findComponent( root, "ProjectRootManager" );

        String jdkName = component.attributeValue( "project-jdk-name" );

        String javaVersion = System.getProperty( "java.version" );

        assertEquals( "Default jdkName should be from System.Properties",
                      jdkName, "java version &quot;" + javaVersion + "&quot;" );
    }

    public void testIdeaProjectJdk11()
        throws Exception
    {
        Document iprDocument = executeMojo( "src/test/plugin-configs/plugin-config-jdk11.xml" );

        testJdkName( iprDocument, "1.1", "java version 1.1" );
    }

    public void testIdeaProjectJdk15()
        throws Exception
    {
        Document iprDocument = executeMojo( "src/test/plugin-configs/plugin-config-jdk15.xml" );

        testJdkName( iprDocument, "1.5", "java version 1.5" );
    }

    private Document executeMojo( String pluginXml )
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), pluginXml );

        mojo = (IdeaProjectMojo) lookupMojo( "project", pluginXmlFile );

        assertNotNull( "Get project mojo instance using " + pluginXmlFile.getAbsolutePath() , mojo );

        mojo.execute();

        int testCounter = SimpleMavenProjectStub.getUsageCounter();

        File iprFile = new File( "target/test-harness/" + testCounter + "/plugin-test-" + testCounter + ".ipr" );

        assertTrue( "Project file was created", iprFile.exists() );

        return readXmlDocument( iprFile );
    }

    private void testJdkName( Document document, String jdkLevel, String expected )
        throws Exception
    {
        Element root = document.getRootElement();

        Element component = findComponent( root, "ProjectRootManager" );

        String jdkName = component.attributeValue( "project-jdk-name" );

        if ( jdkLevel == null )
        {
            jdkLevel = System.getProperty( "java.specification.version" );
        }

        if ( jdkLevel.startsWith( "1.4" ) )
        {
            assertEquals( "assert-keyword must be true for jdk 1.4",
                          "true", component.attributeValue( "assert-keyword" ) );

            assertEquals( "jdk-15 must be false for jdk 1.4",
                          "false", component.attributeValue( "jdk-15") );
        }
        else if ( jdkLevel.compareTo( "1.5" ) >= 0 )
        {
            assertEquals( "assert-keyword must be true for jdk >= 1.5",
                          "true", component.attributeValue( "assert-keyword" ) );

            assertEquals( "jdk-15 must be true for jdk >= 1.5",
                          "true", component.attributeValue( "jdk-15") );
        }
        else
        {
            assertEquals( "assert-keyword must be true for jdk >= 1.5",
                          "false", component.attributeValue( "assert-keyword" ) );
        }

        if ( expected != null )
        {
            assertEquals( "Expected jdkName test", jdkName, expected );
        }
    }

    private Document readXmlDocument( File xmlFile )
        throws DocumentException
    {
        SAXReader reader = new SAXReader();

        return reader.read( xmlFile );
    }

    private Element findComponent( Element module, String name )
        throws Exception
    {
        return findElement( module, "component", name );
    }

    private Element findElement( Element element, String elementName, String attributeName )
        throws Exception
    {
        for ( Iterator children = element.elementIterator( elementName ); children.hasNext(); )
        {
            Element child = (Element) children.next();
            if ( attributeName.equals( child.attributeValue( "name" ) ) )
            {
                return child;
            }
        }
        throw new Exception( "Expected element not found: " + elementName );
    }
}
