package org.apache.maven.plugin.idea;

import org.dom4j.Document;
import org.dom4j.Element;

/*
 *
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
 *
 */

/**
 * @author Edwin Punzalan
 */
public class IdeaWorkspaceTest
    extends AbstractIdeaTestCase
{
    public void testMinConfig()
        throws Exception
    {
        executeMojo( "src/test/workspace-plugin-configs/min-plugin-config.xml" );
    }

    public void testScmConnectionConfig()
        throws Exception
    {
        Document iwsDocument = executeMojo( "src/test/workspace-plugin-configs/connection-plugin-config.xml" );

        Element component = findComponent( iwsDocument.getRootElement(), "VcsManagerConfiguration" );

        Element element = findElementByNameAttribute( component, "option", "ACTIVE_VCS_NAME" );

        assertEquals( "Test scm type from scm connection", "type", element.attributeValue( "value" ) );
    }

    public void testScmConnectionWithPipeConfig()
        throws Exception
    {
        Document iwsDocument = executeMojo( "src/test/workspace-plugin-configs/connection-with-pipe-plugin-config.xml" );

        Element component = findComponent( iwsDocument.getRootElement(), "VcsManagerConfiguration" );

        Element element = findElementByNameAttribute( component, "option", "ACTIVE_VCS_NAME" );

        assertEquals( "Test scm type from scm connection", "type", element.attributeValue( "value" ) );
    }

    public void testScmDevConnectionConfig()
        throws Exception
    {
        Document iwsDocument = executeMojo( "src/test/workspace-plugin-configs/devconnection-plugin-config.xml" );

        Element component = findComponent( iwsDocument.getRootElement(), "VcsManagerConfiguration" );

        Element element = findElementByNameAttribute( component, "option", "ACTIVE_VCS_NAME" );

        assertEquals( "Test scm type from scm connection", "type", element.attributeValue( "value" ) );
    }

    private Document executeMojo( String pluginXml )
        throws Exception
    {
        return super.executeMojo( "workspace", pluginXml, "iws" );
    }
}