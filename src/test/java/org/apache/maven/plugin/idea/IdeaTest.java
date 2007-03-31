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

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.idea.stubs.TestCounter;
import org.dom4j.io.SAXReader;
import org.dom4j.Document;
import org.dom4j.Element;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class IdeaTest
    extends AbstractMojoTestCase
{
    public void testIdea()
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/idea-plugin-configs/min-plugin-config.xml" );

        Mojo mojo = lookupMojo( "idea", pluginXmlFile );

        mojo.execute();

        File basedir = new File( getBasedir(),  "target/test-harness/" + TestCounter.currentCount() );

        String artifactId = "plugin-test-" + TestCounter.currentCount();

        File iprFile = new File( basedir, artifactId + ".ipr" );
        assertTrue( "Test creation of project files", iprFile.exists() );

        File imlFile = new File( basedir, artifactId + ".iml" );
        assertTrue( "Test creation of project files", imlFile.exists() );

        File iwsFile = new File( basedir, artifactId + ".iws" );
        assertTrue( "Test creation of project files", iwsFile.exists() );
    }

    public void testIdeaWithMacro()
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/idea-plugin-configs/macro-plugin-config.xml" );

        Mojo mojo = lookupMojo( "idea", pluginXmlFile );

        mojo.execute();

        int testCounter = TestCounter.currentCount();

        File basedir = new File( getBasedir(), "target/test-harness/" + TestCounter.currentCount() );

        String artifactId = "plugin-test-" + testCounter;

        File iprFile = new File( basedir, artifactId + ".ipr" );
        assertTrue( "Test creation of project files", iprFile.exists() );

        File imlFile = new File( basedir, artifactId + ".iml" );
        assertTrue( "Test creation of project files", imlFile.exists() );

        File iwsFile = new File( basedir, artifactId + ".iws" );
        assertTrue( "Test creation of project files", iwsFile.exists() );

        File outputFile = new File( getBasedir(), "target/test-harness/" + testCounter + "/plugin-test-" + testCounter + ".ipr" );

        SAXReader reader = new SAXReader();

        Document iprDocument = reader.read( outputFile );

        Element macros = iprDocument.getRootElement().element( "UsedPathMacros" );

        assertEquals( "Test creation of macros", 1, macros.elements( "macro" ).size() );

        Element macro = macros.element( "macro" );

        assertEquals( "Test macro name", "USER_HOME", macro.attributeValue( "name" ) );
    }
}
