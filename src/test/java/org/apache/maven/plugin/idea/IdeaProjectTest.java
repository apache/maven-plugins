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
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.List;

/**
 * @author Edwin Punzalan
 */
public class IdeaProjectTest
    extends AbstractIdeaTestCase
{
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
}
