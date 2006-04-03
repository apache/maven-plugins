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

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.apache.maven.plugin.idea.stubs.TestCounter;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @author Edwin Punzalan
 */
public abstract class AbstractIdeaTestCase
    extends AbstractMojoTestCase
{
    protected IdeaProjectMojo mojo;

    protected Document executeMojo( String pluginXml )
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

    protected Document readXmlDocument( File xmlFile )
        throws DocumentException
    {
        SAXReader reader = new SAXReader();

        return reader.read( xmlFile );
    }

    protected Element findComponent( Element module, String name )
        throws Exception
    {
        return findElementByNameAttribute( module, "component", name );
    }

    protected Element findElementByNameAttribute( Element element, String elementName, String nameAttribute )
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

    protected List findElementsByName( Element element, String elementName )
    {
        return element.elements( elementName );
    }
}
