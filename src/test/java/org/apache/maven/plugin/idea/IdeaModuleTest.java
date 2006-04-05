package org.apache.maven.plugin.idea;

import org.dom4j.Document;
import org.dom4j.Element;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/*
 *  Copyright 2005-2006 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Edwin Punzalan
 */
public class IdeaModuleTest
    extends AbstractIdeaTestCase
{
    public void testJarMinConfig()
        throws Exception
    {
        executeMojo( "src/test/module-plugin-configs/min-plugin-config.xml" );
    }

    public void testWarMinConfig()
        throws Exception
    {
        List expectedLibs = new ArrayList();
        expectedLibs.add( "/WEB-INF/lib/maven-model-2.0.1.jar" );
        expectedLibs.add( "/WEB-INF/lib/junit-3.8.1.jar" );

        Document imlDocument = executeMojo( "src/test/module-plugin-configs/min-war-plugin-config.xml" );

        Element root = imlDocument.getRootElement();

        assertEquals( "Test Project type", "J2EE_WEB_MODULE", root.attributeValue( "type" ) );

        Element component = findComponent( root, "WebModuleBuildComponent" );

        Element setting = findElement( component, "setting" );
        assertTrue( "Test exploded url setting", "EXPLODED_URL".equals( setting.attributeValue( "name" ) ) );
        assertTrue( "Test exploded url value",
                    setting.attributeValue( "value" ).startsWith( "file://$MODULE_DIR$/target/" ) );

        component = findComponent( root, "WebModuleProperties" );

        Element deployDescriptor = component.element( "deploymentDescriptor" );
        assertEquals( "Test deployment descriptor version", "2.3", deployDescriptor.attributeValue( "version" ) );
        assertEquals( "Test deployment descriptor name", "web.xml", deployDescriptor.attributeValue( "name" ) );
        assertEquals( "Test deployment descriptor optional", "false", deployDescriptor.attributeValue( "optional" ) );
        assertEquals( "Test deployment descriptor file",
                      "file://$MODULE_DIR$/src/main/webapp/WEB-INF/web.xml",
                      deployDescriptor.attributeValue( "url" ) );

        Element webroots = component.element( "webroots" );
        Element webroot = webroots.element( "root" );
        assertEquals( "Test webroot relative location", "/", webroot.attributeValue( "relative" ) );
        assertEquals( "Test webroot url", "file://$MODULE_DIR$/src/main/webapp", webroot.attributeValue( "url" ) );

        List containerElementList = findElementsByName( component, "containerElement" );
        for ( Iterator containerElements = containerElementList.iterator(); containerElements.hasNext(); )
        {
            Element containerElement = (Element) containerElements.next();

            assertEquals( "Test container element type", "library", containerElement.attributeValue( "type" ) );
            assertEquals( "Test container element level", "module", containerElement.attributeValue( "level" ) );
            assertTrue( "Test library url", containerElement.element( "url" ).getText().startsWith( "jar://" ) );

            Element attribute = findElementByNameAttribute( containerElement, "attribute", "method" );
            assertEquals( "Test library method", "1", attribute.attributeValue( "value" ) );

            attribute = findElementByNameAttribute( containerElement, "attribute", "URI" );
            String attributeValue = attribute.attributeValue( "value" );
            assertTrue( "Test library URI", expectedLibs.contains( attributeValue ) );
            expectedLibs.remove( attributeValue );
        }

        assertTrue( "All libraries are present", expectedLibs.size() == 0 );
    }

    public void testEjbMinConfig()
        throws Exception
    {
        List expectedLibs = new ArrayList();
        expectedLibs.add( "/WEB-INF/lib/maven-model-2.0.1.jar" );
        expectedLibs.add( "/WEB-INF/lib/junit-3.8.1.jar" );

        Document imlDocument = executeMojo( "src/test/module-plugin-configs/min-ejb-plugin-config.xml" );

        Element root = imlDocument.getRootElement();

        assertEquals( "Test Project type", "J2EE_EJB_MODULE", root.attributeValue( "type" ) );

        Element component = findComponent( root, "EjbModuleBuildComponent" );

        Element setting = findElement( component, "setting" );
        assertTrue( "Test exploded url setting", "EXPLODED_URL".equals( setting.attributeValue( "name" ) ) );
        assertTrue( "Test exploded url value",
                    setting.attributeValue( "value" ).startsWith( "file://$MODULE_DIR$/target/" ) );

        component = findComponent( root, "EjbModuleProperties" );

        Element deployDescriptor = component.element( "deploymentDescriptor" );
        assertEquals( "Test deployment descriptor version", "2.x", deployDescriptor.attributeValue( "version" ) );
        assertEquals( "Test deployment descriptor name", "ejb-jar.xml", deployDescriptor.attributeValue( "name" ) );
        assertEquals( "Test deployment descriptor optional", "false", deployDescriptor.attributeValue( "optional" ) );
        assertEquals( "Test deployment descriptor file",
                      "file://$MODULE_DIR$/src/main/resources/META-INF/ejb-jar.xml",
                      deployDescriptor.attributeValue( "url" ) );

        List containerElementList = findElementsByName( component, "containerElement" );
        for ( Iterator containerElements = containerElementList.iterator(); containerElements.hasNext(); )
        {
            Element containerElement = (Element) containerElements.next();

            assertEquals( "Test container element type", "library", containerElement.attributeValue( "type" ) );
            assertEquals( "Test container element level", "module", containerElement.attributeValue( "level" ) );

            Element attribute = findElementByNameAttribute( containerElement, "attribute", "method" );
            assertEquals( "Test library method", "2", attribute.attributeValue( "value" ) );

            attribute = findElementByNameAttribute( containerElement, "attribute", "URI" );
            String attributeValue = attribute.attributeValue( "value" );
            assertTrue( "Test library URI", expectedLibs.contains( attributeValue ) );
            expectedLibs.remove( attributeValue );
        }

        assertTrue( "All libraries are present", expectedLibs.size() == 0 );
    }

    public void testEarMinConfig()
        throws Exception
    {
        Document imlDocument = executeMojo( "src/test/module-plugin-configs/min-ear-plugin-config.xml" );

        Element root = imlDocument.getRootElement();

        assertEquals( "Test Project type", "J2EE_APPLICATION_MODULE", root.attributeValue( "type" ) );

        Element component = findComponent( root, "ApplicationModuleProperties" );

        Element deployDescriptor = component.element( "deploymentDescriptor" );
        assertEquals( "Test deployment descriptor version", "1.3", deployDescriptor.attributeValue( "version" ) );
        assertEquals( "Test deployment descriptor name", "application.xml", deployDescriptor.attributeValue( "name" ) );
        assertEquals( "Test deployment descriptor optional", "false", deployDescriptor.attributeValue( "optional" ) );
        assertEquals( "Test deployment descriptor file",
                      "file://$MODULE_DIR$/target/application.xml",
                      deployDescriptor.attributeValue( "url" ) );
    }

    protected Document executeMojo( String pluginXml )
        throws Exception
    {
        List expectedDeps = new ArrayList();
        expectedDeps.add( "jar://E:/localRepository/org.apache.maven/maven-model/2.0.1/maven-model-2.0.1.jar!/" );
        expectedDeps.add( "jar://E:/localRepository/junit/junit/3.8.1/junit-3.8.1.jar!/" );

        Document imlDocument = super.executeMojo( "module", pluginXml, "iml" );

        Element component = findComponent( imlDocument.getRootElement(), "NewModuleRootManager" );

        Element output = findElement( component, "output" );
        assertEquals( "Module output url created.", "file://$MODULE_DIR$/target/classes", output.attributeValue( "url" ) );

        Element outputTest = findElement( component, "output-test" );
        assertEquals( "Module test-output url created.", "file://$MODULE_DIR$/target/test-classes", outputTest.attributeValue( "url" ) );

        Element content = findElement( component, "content" );
        List elementList = findElementsByName( content, "sourceFolder" );
        for ( Iterator sourceFolders = elementList.iterator(); sourceFolders.hasNext(); )
        {
            Element sourceFolder = (Element) sourceFolders.next();

            String isTestSource = sourceFolder.attributeValue( "isTestSource" ).toLowerCase();
            if ( "false".equals( isTestSource ) )
            {
                assertTrue( "Main source url",
                            sourceFolder.attributeValue( "url" ).startsWith( "file://$MODULE_DIR$/src/main" ) );
            }
            else if ( "true".equals( isTestSource ) )
            {
                assertTrue( "Test source url",
                            sourceFolder.attributeValue( "url" ).startsWith( "file://$MODULE_DIR$/src/test" ) );
            }
            else
            {
                fail( "Unknown sourceFolder 'isTestSource' attribute value: " + isTestSource );
            }
        }

        List orderEntryList = findElementsByName( component, "orderEntry" );

        for ( Iterator orderEntries = orderEntryList.iterator(); orderEntries.hasNext(); )
        {
            Element orderEntry = (Element) orderEntries.next();

            if ( "module-library".equals( orderEntry.attributeValue( "type" ) ) )
            {
                Element library = (Element) orderEntry.elementIterator( "library" ).next();

                Element classes = (Element) library.elementIterator( "CLASSES" ).next();

                Element root = (Element) classes.elementIterator( "root" ).next();

                assertTrue( "Dependency is present", expectedDeps.contains( root.attributeValue( "url" ) ) );

                expectedDeps.remove( root.attributeValue( "url" ) );
            }
        }

        assertTrue( "All dependencies are present", expectedDeps.size() == 0 );

        return imlDocument;
    }
}
