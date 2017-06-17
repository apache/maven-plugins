package org.apache.maven.plugins.antrun;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.IOUtil;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test class for {@link AntrunXmlPlexusConfigurationWriter}.
 * @author gboue
 */
public class AntrunXmlPlexusConfigurationWriterTest
{

    private static final String TARGET_NAME = "main";

    private TemporaryFolder folder = new TemporaryFolder();

    private AntrunXmlPlexusConfigurationWriter configurationWriter;

    private PlexusConfiguration configuration;

    private File file;

    @Before
    public void setUp()
        throws IOException
    {
        configurationWriter = new AntrunXmlPlexusConfigurationWriter();
        configuration = new XmlPlexusConfiguration( "target" );
        configuration.setAttribute( "name", TARGET_NAME );
        file = folder.newFile();
    }

    /**
     * Tests that the XML file produced with the writer is pretty printed and that basic attributes are kept.
     * 
     * @throws Exception
     */
    @Test
    public void testBasic()
        throws Exception
    {
        configuration.getChild( "echo", true ).setAttribute( "message", "Hello" );
        configurationWriter.write( configuration, file, "", TARGET_NAME );
        assertXmlIsExpected( "/configuration-writer/basic.xml", file );
    }

    /**
     * Tests that serialization is correct even if Ant target is empty (no children, no attributes except name).
     * 
     * @throws Exception
     */
    @Test
    public void testEmptyTarget()
        throws Exception
    {
        configurationWriter.write( configuration, file, "", TARGET_NAME );
        assertXmlIsExpected( "/configuration-writer/empty-target.xml", file );
    }

    /**
     * Tests that setting a custom prefix ends up in the project namespace in the target element with the correct name.
     * 
     * @throws Exception
     */
    @Test
    public void testCustomTaskPrefix()
        throws Exception
    {
        PlexusConfiguration child = configuration.getChild( "mvn:foo", true );
        child.setAttribute( "attr1", "val1" );
        child.setValue( "The first value." );
        child = configuration.getChild( "bar", true );
        child.setAttribute( "attr2", "val2" );
        child.setValue( "The second value." );
        configurationWriter.write( configuration, file, "mvn", TARGET_NAME );
        assertXmlIsExpected( "/configuration-writer/custom-task-prefix.xml", file );
    }

    /**
     * Tests that combine.children and combine.self attributes in the XML configuration elements are ignored during
     * serialization.
     * 
     * @throws Exception
     */
    @Test
    public void testCombineAttributes()
        throws Exception
    {
        configuration.setAttribute( "combine.children", "append" );
        configuration.setAttribute( "description", "foo" );
        configuration.getChild( "child", true ).setAttribute( "combine.self", "override" );
        configurationWriter.write( configuration, file, "", TARGET_NAME );
        assertXmlIsExpected( "/configuration-writer/combine-attributes.xml", file );
    }

    private void assertXmlIsExpected( String expected, File file )
        throws Exception
    {
        Charset charset = Charset.forName( "UTF-8" );
        BufferedReader test = new BufferedReader( new InputStreamReader( new FileInputStream( file ), charset ) );
        BufferedReader control =
            new BufferedReader( new InputStreamReader( getClass().getResourceAsStream( expected ), charset ) );
        try
        {
            XMLAssert.assertXMLEqual( XMLUnit.compareXML( control, test ), true );
        }
        finally
        {
            IOUtil.close( test );
            IOUtil.close( control );
        }
    }

    @Rule
    public TemporaryFolder getFolder()
    {
        return folder;
    }

}
