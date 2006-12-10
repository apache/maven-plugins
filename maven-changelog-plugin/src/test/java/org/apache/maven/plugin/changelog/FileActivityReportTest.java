package org.apache.maven.plugin.changelog;

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
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.plugin.changelog.stubs.ScmManagerStub;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class FileActivityReportTest
    extends AbstractMojoTestCase
{
    private ScmManager scmManager;

    public void testNoSource()
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(),
                                               "src/test/plugin-configs/file-activity/no-source-plugin-config.xml" );

        Mojo mojo = lookupMojo( "file-activity", pluginXmlFile );

        assertNotNull( "Mojo found.", mojo );

        this.setVariableValueToObject( mojo, "manager", scmManager );

        mojo.execute();

        File outputDir = (File) getVariableValueFromObject( mojo, "outputDirectory" );

        File outputHtml = new File( outputDir, "file-activity.html" );

        assertTrue( "Test html generated", outputHtml.exists() );
    }

    public void testMinConfig()
        throws Exception
    {
        File outputXML = new File( getBasedir(), "src/test/changelog-xml/min-changelog.xml" );

        // force reuse of existing changelog cache
        outputXML.setLastModified( System.currentTimeMillis() );

        executeMojo( "min-plugin-config.xml" );
    }

    private void executeMojo( String pluginXml )
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/plugin-configs/file-activity/" + pluginXml );

        Mojo mojo = lookupMojo( "file-activity", pluginXmlFile );

        assertNotNull( "Mojo found.", mojo );

        this.setVariableValueToObject( mojo, "manager", scmManager );

        mojo.execute();

        File outputXML = (File) getVariableValueFromObject( mojo, "outputXML" );

        String encoding = (String) getVariableValueFromObject( mojo, "outputEncoding" );

        assertTrue( "Test if changelog.xml is created", outputXML.exists() );

        String changelogXml = FileUtils.fileRead( outputXML );

        assertTrue( "Test for xml header", changelogXml.startsWith( "<?xml version=\"1.0\" encoding=\"" +
                    encoding + "\"?>" ) );

        assertTrue( "Test for xml footer", changelogXml.endsWith( "</changelog>" ) );

        File outputDir = (File) getVariableValueFromObject( mojo, "outputDirectory" );

        File outputHtml = new File( outputDir, "file-activity.html" );

        assertTrue( "Test html generated", outputHtml.exists() );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        scmManager = new ScmManagerStub();
    }
}
