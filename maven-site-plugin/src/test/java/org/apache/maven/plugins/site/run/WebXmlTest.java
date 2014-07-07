package org.apache.maven.plugins.site.run;

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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WebXmlTest
{

    @Test
    public void testFilters()
        throws Exception
    {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = docBuilder.parse( SiteRunMojo.class.getResourceAsStream( "/run/web.xml" ) );
        XPath xPath = XPathFactory.newInstance().newXPath();

        NodeList filterClasses =
            (NodeList) xPath.compile( "/web-app/filter/filter-class" ).evaluate( doc, XPathConstants.NODESET );

        assertTrue( "Expected at least one filter", filterClasses.getLength() > 0 );
        for ( int index = 0; index < filterClasses.getLength(); index++ )
        {
            Node filterClass = filterClasses.item( index ).getFirstChild();
            try
            {
                Class.forName( filterClass.getNodeValue() );
            }
            catch ( ClassNotFoundException cnfe )
            {
                fail( "/web-app/filter[" + index + "]/filter-class refers to " + filterClass.getNodeValue()
                    + ", which doesn't exist" );
            }
        }
    }
}
