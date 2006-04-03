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

import org.apache.maven.plugin.idea.stubs.TestCounter;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.Iterator;
import java.util.List;

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

        component = findComponent( root, "CompilerConfiguration" );

        Element patterns = findElementByNameAttribute( component, "wildcardResourcePatterns", null );

        findElementByNameAttribute( patterns, "entry", "!?*.java" );
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

    public void testIdeaProjectWithModules()
        throws Exception
    {
        Document iprDocument = executeMojo( "src/test/plugin-configs/plugin-config-modules.xml" );

        Element component = findComponent( iprDocument.getRootElement(), "ProjectModuleManager" );

        Element el = findElementByNameAttribute( component, "modules", null );

        List modules = findElementsByName( el, "module" );

        assertEquals( "Must have 4 modules", 4, modules.size() );

        el = (Element) modules.get( 0 );
        assertEquals( "Test project module",
                      "$PROJECT_DIR$/plugin-test-" + TestCounter.currentCount() + ".iml",
                      el.attributeValue( "filepath" ) );

        el = (Element) modules.get( 1 );
        assertEquals( "Test module 1",
                      "$PROJECT_DIR$/module-1/module-1.iml",
                      el.attributeValue( "filepath" ) );

        el = (Element) modules.get( 2 );
        assertEquals( "Test module 2",
                      "$PROJECT_DIR$/module-2/module-2.iml",
                      el.attributeValue( "filepath" ) );

        el = (Element) modules.get( 3 );
        assertEquals( "Test module 3",
                      "$PROJECT_DIR$/module-3/module-3.iml",
                      el.attributeValue( "filepath" ) );
    }

    private Document executeMojo( String pluginXml )
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), pluginXml );

        mojo = (IdeaProjectMojo) lookupMojo( "project", pluginXmlFile );

        assertNotNull( "Get project mojo instance using " + pluginXmlFile.getAbsolutePath() , mojo );

        mojo.execute();

        int testCounter = TestCounter.currentCount();

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
        return findElementByNameAttribute( module, "component", name );
    }

    private Element findElementByNameAttribute( Element element, String elementName, String nameAttribute )
        throws Exception
    {
        Element e = null;

        for ( Iterator children = element.elementIterator( elementName ); children.hasNext(); )
        {
            Element child = (Element) children.next();
            if ( nameAttribute == null )
            {
                e = child;
            }
            else if ( nameAttribute.equals( child.attributeValue( "name" ) ) )
            {
                e = child;
            }
        }

        if ( e == null)
        {
            if ( nameAttribute == null )
            {
                fail( "Element " + elementName + " not found." );
            }
            else
            {
                fail( "Attribute " + nameAttribute + " not found in elements " + elementName + "." );
            }
        }

        return e;
    }

    private List findElementsByName( Element element, String elementName )
    {
        return element.elements( elementName );
    }
}
