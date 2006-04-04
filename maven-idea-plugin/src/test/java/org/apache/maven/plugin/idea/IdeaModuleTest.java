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
    public void testMinConfig()
        throws Exception
    {
        List expectedDeps = new ArrayList();
        expectedDeps.add( "jar://E:/localRepository/org.apache.maven/maven-model/2.0.1/maven-model-2.0.1.jar!/" );
        expectedDeps.add( "jar://E:/localRepository/junit/junit/3.8.1/junit-3.8.1.jar!/" );

        Document imlDocument = executeMojo( "src/test/module-plugin-configs/min-plugin-config.xml" );

        Element component = findComponent( imlDocument.getRootElement(), "NewModuleRootManager" );

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
    }

    protected Document executeMojo( String pluginXml )
        throws Exception
    {
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

        return imlDocument;
    }
}
