package org.apache.maven.plugin.idea;

/*
 * Copyright 2004-2006 The Apache Software Foundation.
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
import org.codehaus.plexus.PlexusTestCase;
import org.dom4j.Document;
import org.dom4j.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    public void testProvidedDependencies()
        throws Exception
    {
        executeMojo( "src/test/module-plugin-configs/provided-dep-plugin-config.xml" );
    }

    public void testExcludeDirectoryConfig()
        throws Exception
    {
        File projectBasedir = new File( getBasedir(), "target/test-harness/" + ( TestCounter.currentCount() + 1 ) );

        projectBasedir.mkdirs();

        File excluded = new File( projectBasedir, "excluded" );
        excluded.mkdirs();
        File sub = new File( excluded, "sub" );
        sub.mkdirs();
        File sub2 = new File( excluded, "sub2" );
        sub2.mkdirs();
        File subsub1 = new File( sub, "sub1" );
        subsub1.mkdirs();

        Document imlDocument = executeMojo( "src/test/module-plugin-configs/dir-exclusion-plugin-config.xml" );

        Element component = findComponent( imlDocument.getRootElement(), "NewModuleRootManager" );

        boolean excludedDirFound = false;
        Element content = findElement( component, "content" );
        for ( Iterator excludes = content.elementIterator( "excludeFolder" ); excludes.hasNext(); )
        {
            Element exclude = (Element) excludes.next();

            if ( "file://$MODULE_DIR$/excluded".equals( exclude.attributeValue( "url" ) ) )
            {
                excludedDirFound = true;
            }
        }
        assertTrue( "Test excluded dir", excludedDirFound );
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
        assertEquals( "Test deployment descriptor file", "file://$MODULE_DIR$/src/main/webapp/WEB-INF/web.xml",
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

    public void testWarConfigWithProvidedDependency()
        throws Exception
    {
        List expectedLibs = new ArrayList();
        expectedLibs.add( "/WEB-INF/lib/maven-model-2.0.1.jar" );
        expectedLibs.add( "/WEB-INF/lib/jdbc-stdext-2.0.jar" );
        expectedLibs.add( "/WEB-INF/lib/junit-3.8.1.jar" );

        Document imlDocument = executeMojo( "src/test/module-plugin-configs/provided-dep-plugin-config.xml" );

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
        assertEquals( "Test deployment descriptor file", "file://$MODULE_DIR$/src/main/webapp/WEB-INF/web.xml",
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

            Element attribute = findElementByNameAttribute( containerElement, "attribute", "URI" );
            String attributeValue = attribute.attributeValue( "value" );

            attribute = findElementByNameAttribute( containerElement, "attribute", "method" );

            if ( "/WEB-INF/lib/maven-model-2.0.1.jar".equals( attributeValue ) )
            {
                assertEquals( "Test library method for provided dependency", "0", attribute.attributeValue( "value" ) );
            }
            else if ( "/WEB-INF/lib/jdbc-stdext-2.0.jar".equals( attributeValue ) )
            {
                assertEquals( "Test library method for system dependency", "0", attribute.attributeValue( "value" ) );
            }
            else if ( "/WEB-INF/lib/junit-3.8.1.jar".equals( attributeValue ) )
            {
                assertEquals( "Test library method for test dependency", "0", attribute.attributeValue( "value" ) );
            }
            else
            {
                assertEquals( "Test library method", "1", attribute.attributeValue( "value" ) );
            }

            assertTrue( "Test library URI", expectedLibs.contains( attributeValue ) );
            expectedLibs.remove( attributeValue );
        }

        assertTrue( "All libraries are present", expectedLibs.size() == 0 );
    }

    public void testEjbMinConfig()
        throws Exception
    {
        List expectedLibs = new ArrayList();
        expectedLibs.add( "/lib/maven-model-2.0.1.jar" );
        expectedLibs.add( "/lib/junit-3.8.1.jar" );

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
        assertEquals( "Test deployment descriptor file", "file://$MODULE_DIR$/src/main/resources/META-INF/ejb-jar.xml",
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
        assertEquals( "Test deployment descriptor file", "file://$MODULE_DIR$/target/application.xml",
                      deployDescriptor.attributeValue( "url" ) );
    }

    public void testGeneralConfigurations()
        throws Exception
    {
        Document imlDocument = executeMojo( "src/test/module-plugin-configs/general-plugin-config.xml" );

        Element component = findComponent( imlDocument.getRootElement(), "NewModuleRootManager" );

        Element content = findElement( component, "content" );

        List expectedExcludes = new ArrayList();
        expectedExcludes.add( "file://$MODULE_DIR$/src/main/resources/excluded" );
        expectedExcludes.add( "file://$MODULE_DIR$/src/main/resources/excluded-too" );

        List excludeList = content.elements( "excludeFolder" );
        for ( Iterator excludes = excludeList.iterator(); excludes.hasNext(); )
        {
            Element exclude = (Element) excludes.next();

            String excluded = exclude.attributeValue( "url" );

            if ( excluded.equals( "file://$MODULE_DIR$/src/main/resources/excluded/sub" ) )
            {
                fail( "A subdirectory of an already excluded directory must be removed" );
            }

            if ( expectedExcludes.contains( excluded ) )
            {
                expectedExcludes.remove( excluded );
            }
        }
        assertEquals( "Test all excludes", 0, expectedExcludes.size() );

        List orderEntryList = findElementsByName( component, "orderEntry" );
        for ( Iterator orderEntries = orderEntryList.iterator(); orderEntries.hasNext(); )
        {
            Element orderEntry = (Element) orderEntries.next();

            if ( "module-library".equals( orderEntry.attributeValue( "type" ) ) )
            {
                Element library = orderEntry.element( "library" );

                String name = library.attributeValue( "name" );
                assertTrue( "Test usage of fullnames", name.indexOf( ":" ) > 0 );
            }
        }

        File srcFile = new File( PlexusTestCase.getBasedir(),
                                 "target/local-repo/org/apache/maven/maven-model/2.0.1/maven-model-2.0.1-src.jar" );
        assertTrue( "Test maven-model source is downloaded", srcFile.exists() );
        srcFile = new File( PlexusTestCase.getBasedir(), "target/local-repo/junit/junit/3.8.1/junit-3.8.1-src.jar" );
        assertTrue( "Test junit source is downloaded", srcFile.exists() );

        File docFile = new File( PlexusTestCase.getBasedir(),
                                 "target/local-repo/org/apache/maven/maven-model/2.0.1/maven-model-2.0.1-doc.jar" );
        assertTrue( "Test maven-model javadoc is downloaded", docFile.exists() );
    }

    public void testWarConfig()
        throws Exception
    {
        List expectedLibs = new ArrayList();
        expectedLibs.add( "/WEB-INF/lib/maven-model-2.0.1.jar" );
        expectedLibs.add( "/WEB-INF/lib/junit-3.8.1.jar" );

        Document imlDocument = executeMojo( "src/test/module-plugin-configs/war-plugin-config.xml" );

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
        assertEquals( "Test deployment descriptor file", "file://$MODULE_DIR$/src/main/web/WEB-INF/web.xml",
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

    public void testProjectWithModulesConfigurations()
        throws Exception
    {
        Document imlDocument = executeMojo( "src/test/module-plugin-configs/module-plugin-config.xml" );

        Element component = findComponent( imlDocument.getRootElement(), "NewModuleRootManager" );

        boolean moduleFound = false;
        List orderEntryList = component.elements( "orderEntry" );
        for ( Iterator orderEntries = orderEntryList.iterator(); orderEntries.hasNext(); )
        {
            Element orderEntry = (Element) orderEntries.next();
            if ( "module".equals( orderEntry.attributeValue( "type" ) ) )
            {
                String moduleName = orderEntry.attributeValue( "module-name" );
                assertTrue( "Test idea module name", moduleName.startsWith( "plugin-reactor-project-" ) );
                moduleFound = true;
            }
        }
        assertTrue( "Test presence of idea module", moduleFound );
    }

    public void testProjectWithLibrariesConfigurations()
        throws Exception
    {
        Document imlDocument = executeMojo( "src/test/module-plugin-configs/library-plugin-config.xml" );

        Element component = findComponent( imlDocument.getRootElement(), "NewModuleRootManager" );

        boolean libraryFound = false;
        for ( Iterator orderEntries = component.elementIterator( "orderEntry" ); orderEntries.hasNext(); )
        {
            Element orderEntry = (Element) orderEntries.next();
            Element library = orderEntry.element( "library" );
            if ( library != null )
            {
                String name = library.attributeValue( "name" );
                if ( name != null && name.equals( "test-library" ) )
                {
                    libraryFound = true;

                    String url = library.element( "CLASSES" ).element( "root" ).attributeValue( "url" );
                    assertEquals( "Test user provided class path", "file:///user/defined/classes", url );

                    url = library.element( "SOURCES" ).element( "root" ).attributeValue( "url" );
                    assertEquals( "Test user provided source path", "file:///user/defined/sources", url );
                }
            }
        }
        assertTrue( "Test if configured library was found", libraryFound );
    }

    public void testProjectWithLibraryExcludeConfigurations()
        throws Exception
    {
        Document imlDocument = executeMojo( "src/test/module-plugin-configs/library-exclude-plugin-config.xml" );

        Element component = findComponent( imlDocument.getRootElement(), "NewModuleRootManager" );

        for ( Iterator orderEntries = component.elementIterator( "orderEntry" ); orderEntries.hasNext(); )
        {
            Element orderEntry = (Element) orderEntries.next();
            Element library = orderEntry.element( "library" );
            if ( library != null )
            {
                Element classes = library.element( "CLASSES" );
                if ( classes != null )
                {
                    Element root = classes.element( "root" );
                    String url = root.attributeValue( "url" );

                    if ( url.indexOf( "test-library" ) >= 0 )
                    {
                        fail( "test-library must be excluded" );
                    }
                }
            }
        }
    }

    public void testWarProjectWithModulesConfigurations()
        throws Exception
    {
        Document imlDocument = executeMojo( "src/test/module-plugin-configs/war-module-plugin-config.xml" );

        Element component = findComponent( imlDocument.getRootElement(), "NewModuleRootManager" );

        boolean moduleFound = false;
        List orderEntryList = component.elements( "orderEntry" );
        for ( Iterator orderEntries = orderEntryList.iterator(); orderEntries.hasNext(); )
        {
            Element orderEntry = (Element) orderEntries.next();
            if ( "module".equals( orderEntry.attributeValue( "type" ) ) )
            {
                String moduleName = orderEntry.attributeValue( "module-name" );
                assertTrue( "Test idea module name", moduleName.startsWith( "plugin-reactor-project-" ) );
                moduleFound = true;
            }
        }
        assertTrue( "Test presence of idea module", moduleFound );

        component = findComponent( imlDocument.getRootElement(), "WebModuleProperties" );

        boolean webModuleFound = false;
        for ( Iterator elements = component.elementIterator( "containerElement" ); elements.hasNext(); )
        {
            Element containerElement = (Element) elements.next();

            if ( "module".equals( containerElement.attributeValue( "type" ) ) )
            {
                String name = containerElement.attributeValue( "name" );

                assertTrue( "Module must be from reactor", name.indexOf( "plugin-reactor-project-" ) >= 0 );

                assertNull( "Library url for modules must not be present", containerElement.element( "url" ) );

                Element method = findElementByNameAttribute( containerElement, "attribute", "method" );
                assertEquals( "Test Library module method", "5", method.attributeValue( "value" ) );

                Element uri = findElementByNameAttribute( containerElement, "attribute", "URI" );
                assertEquals( "Test Library module method", "/WEB-INF/lib/" + name + "-1.0.jar",
                              uri.attributeValue( "value" ) );

                webModuleFound = true;
            }
        }
        assertTrue( "Test WebModuleProperties for module library", webModuleFound );
    }

    public void testEjbProjectWithModulesConfigurations()
        throws Exception
    {
        Document imlDocument = executeMojo( "src/test/module-plugin-configs/ejb-module-plugin-config.xml" );

        Element component = findComponent( imlDocument.getRootElement(), "NewModuleRootManager" );

        boolean moduleFound = false;
        List orderEntryList = component.elements( "orderEntry" );
        for ( Iterator orderEntries = orderEntryList.iterator(); orderEntries.hasNext(); )
        {
            Element orderEntry = (Element) orderEntries.next();
            if ( "module".equals( orderEntry.attributeValue( "type" ) ) )
            {
                String moduleName = orderEntry.attributeValue( "module-name" );
                assertTrue( "Test idea module name", moduleName.startsWith( "plugin-reactor-project-" ) );
                moduleFound = true;
            }
        }
        assertTrue( "Test presence of idea module", moduleFound );

        component = findComponent( imlDocument.getRootElement(), "EjbModuleProperties" );

        boolean ejbModuleFound = false;
        for ( Iterator elements = component.elementIterator( "containerElement" ); elements.hasNext(); )
        {
            Element containerElement = (Element) elements.next();

            if ( "module".equals( containerElement.attributeValue( "type" ) ) )
            {
                String name = containerElement.attributeValue( "name" );

                assertTrue( "Module must be from reactor", name.indexOf( "plugin-reactor-project-" ) >= 0 );

                assertNull( "Library url for modules must not be present", containerElement.element( "url" ) );

                Element method = findElementByNameAttribute( containerElement, "attribute", "method" );
                assertEquals( "Test Library module method", "6", method.attributeValue( "value" ) );

                Element uri = findElementByNameAttribute( containerElement, "attribute", "URI" );
                assertEquals( "Test Library module method", "/lib/plugin-reactor-project-42.jar", uri.attributeValue( "value" ) );

                ejbModuleFound = true;
            }
        }
        assertTrue( "Test EjbModuleProperties for module library", ejbModuleFound );
    }

    public void testProjectArtifactsWithVersionRange()
        throws Exception
    {
        List expectedDeps = new ArrayList();
        expectedDeps.add( "/junit/junit/4.0/junit-4.0.jar!/" );

        Document imlDocument = super.executeMojo( "module", "src/test/module-plugin-configs/artifact-version-range-plugin-config.xml", "iml" );

        Element component = findComponent( imlDocument.getRootElement(), "NewModuleRootManager" );

        Element output = findElement( component, "output" );
        assertEquals( "Module output url created.", "file://$MODULE_DIR$/target/classes",
                      output.attributeValue( "url" ) );

        Element outputTest = findElement( component, "output-test" );
        assertEquals( "Module test-output url created.", "file://$MODULE_DIR$/target/test-classes",
                      outputTest.attributeValue( "url" ) );

        Element content = findElement( component, "content" );

        List excludeList = content.elements( "excludeFolder" );
        if ( excludeList.size() == 1 )
        {
            Element excl = content.element( "excludeFolder" );
            assertEquals( "Test default exclude folder", "file://$MODULE_DIR$/target", excl.attributeValue( "url" ) );
        }
        else
        {
            boolean targetIsExcluded = false;
            for ( Iterator excludes = excludeList.iterator(); excludes.hasNext(); )
            {
                Element excl = (Element) excludes.next();

                if ( "file://$MODULE_DIR$/target".equals( excl.attributeValue( "url" ) ) )
                {
                    targetIsExcluded = true;
                    break;
                }
            }

            if ( !targetIsExcluded )
            {
                fail( "Default exclude folder 'target' not found" );
            }
        }

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

                String depUrl = root.attributeValue( "url" );

                if ( depUrl.endsWith( "/junit/junit/4.0/junit-4.0.jar!/" ) )
                {
                    expectedDeps.remove( "/junit/junit/4.0/junit-4.0.jar!/" );
                }
            }
        }

        assertTrue( "All dependencies are present", expectedDeps.size() == 0 );
    }

    protected Document executeMojo( String pluginXml )
        throws Exception
    {
        List expectedDeps = new ArrayList();
        expectedDeps.add( "/org/apache/maven/maven-model/2.0.1/maven-model-2.0.1.jar!/" );
        expectedDeps.add( "/junit/junit/3.8.1/junit-3.8.1.jar!/" );

        Document imlDocument = super.executeMojo( "module", pluginXml, "iml" );

        Element component = findComponent( imlDocument.getRootElement(), "NewModuleRootManager" );

        Element output = findElement( component, "output" );
        assertEquals( "Module output url created.", "file://$MODULE_DIR$/target/classes",
                      output.attributeValue( "url" ) );

        Element outputTest = findElement( component, "output-test" );
        assertEquals( "Module test-output url created.", "file://$MODULE_DIR$/target/test-classes",
                      outputTest.attributeValue( "url" ) );

        Element content = findElement( component, "content" );

        List excludeList = content.elements( "excludeFolder" );
        if ( excludeList.size() == 1 )
        {
            Element excl = content.element( "excludeFolder" );
            assertEquals( "Test default exclude folder", "file://$MODULE_DIR$/target", excl.attributeValue( "url" ) );
        }
        else
        {
            boolean targetIsExcluded = false;
            for ( Iterator excludes = excludeList.iterator(); excludes.hasNext(); )
            {
                Element excl = (Element) excludes.next();

                if ( "file://$MODULE_DIR$/target".equals( excl.attributeValue( "url" ) ) )
                {
                    targetIsExcluded = true;
                    break;
                }
            }

            if ( !targetIsExcluded )
            {
                fail( "Default exclude folder 'target' not found" );
            }
        }

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

                String depUrl = root.attributeValue( "url" );

                if ( depUrl.endsWith( "/org/apache/maven/maven-model/2.0.1/maven-model-2.0.1.jar!/" ) )
                {
                    expectedDeps.remove( "/org/apache/maven/maven-model/2.0.1/maven-model-2.0.1.jar!/" );
                }
                else if ( depUrl.endsWith( "/junit/junit/3.8.1/junit-3.8.1.jar!/" ) )
                {
                    expectedDeps.remove( "/junit/junit/3.8.1/junit-3.8.1.jar!/" );
                }
            }
        }

        assertTrue( "All dependencies are present", expectedDeps.size() == 0 );

        return imlDocument;
    }
}
