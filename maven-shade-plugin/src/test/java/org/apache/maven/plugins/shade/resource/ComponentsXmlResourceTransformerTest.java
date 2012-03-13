package org.apache.maven.plugins.shade.resource;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.util.Collections;

import junit.framework.TestCase;

import org.codehaus.plexus.util.IOUtil;

/**
 * Test for {@link ComponentsXmlResourceTransformer}.
 * 
 * @author Brett Porter
 * @version $Id$
 */
public class ComponentsXmlResourceTransformerTest
    extends TestCase
{
    private ComponentsXmlResourceTransformer transformer;

    public void setUp()
    {
        this.transformer = new ComponentsXmlResourceTransformer();
    }

    public void testConfigurationMerging() throws IOException
    {
        transformer.processResource( "components-1.xml", getClass().getResourceAsStream( "/components-1.xml" ),
                                     Collections.EMPTY_LIST );
        transformer.processResource( "components-1.xml", getClass().getResourceAsStream( "/components-2.xml" ),
                                     Collections.EMPTY_LIST );
        
        assertEquals( IOUtil.toString( getClass().getResourceAsStream( "/components-expected.xml" ), "UTF-8" ),
                      IOUtil.toString( transformer.getTransformedResource(), "UTF-8" ).replaceAll("\r\n", "\n") );
    }
}