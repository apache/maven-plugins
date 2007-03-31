package org.apache.maven.plugin.idea;

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

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.apache.maven.plugin.idea.stubs.TestCounter;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.Mojo;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @author Edwin Punzalan
 */
public abstract class AbstractIdeaTestCase
    extends AbstractMojoTestCase
{
    protected Mojo mojo;

    protected Document executeMojo( String goal, String pluginXml, String targetExtension )
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), pluginXml );

        mojo = lookupMojo( goal, pluginXmlFile );

        assertNotNull( "Get mojo instance using " + pluginXmlFile.getAbsolutePath() , mojo );

        mojo.execute();

        int testCounter = TestCounter.currentCount();

        File outputFile = new File( PlexusTestCase.getBasedir(), "target/test-harness/" + testCounter +
                                 "/plugin-test-" + testCounter + "." + targetExtension );

        assertTrue( "Target file was created", outputFile.exists() );

        return readXmlDocument( outputFile );
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

    protected Element findElement( Element component, String name )
    {
        Element element = component.element( name );

        if ( element == null )
        {
            fail( "Element " + name + " not found." );
        }
        
        return element;
    }
}
