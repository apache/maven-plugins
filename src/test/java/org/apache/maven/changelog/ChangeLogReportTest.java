package org.apache.maven.changelog;

/*
 *  Copyright 2001-2006 The Apache Software Foundation.
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
 *
 */

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.changelog.stubs.ScmManagerStub;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class ChangeLogReportTest
    extends AbstractMojoTestCase
{
    private ScmManager scmManager;

    public void testMinConfig()
        throws Exception
    {
        executeMojo( "src/test/plugin-configs/min-plugin-config.xml" );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        scmManager = new ScmManagerStub();
    }

    private void executeMojo( String pluginXml )
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), pluginXml );

        Mojo mojo = lookupMojo( "changelog", pluginXmlFile );

        assertNotNull( "Mojo found.", mojo );

        this.setVariableValueToObject( mojo, "manager", scmManager );

        mojo.execute();

        File outputXML = (File) getVariableValueFromObject( mojo, "outputXML" );

        String encoding = (String) getVariableValueFromObject( mojo, "outputEncoding" );

        assertTrue( "Test if changelog.xml is created", outputXML.exists() );

        String changelogXml = FileUtils.fileRead( outputXML );

        assertTrue( "Test for xml header", changelogXml.startsWith( "<?xml version=\"1.0\" encoding=\"" +
                    encoding + "\"?>\n<changelog>" ) );

        assertTrue( "Test for xml footer", changelogXml.endsWith( "\n</changelog>" ) );
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();
    }
}
