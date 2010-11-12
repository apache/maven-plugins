package org.apache.maven.plugins.site;

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
import java.io.File;
import java.util.HashMap;

import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
@RunWith(JUnit4.class)
public class SiteMojoTest
    extends AbstractMojoTestCase
{
    
    @Before
    public void setup()
        throws Exception
    {
        super.setUp();
    }
    
    /**
     * Test method for 'org.apache.maven.plugins.site.AbstractSiteMojo.getInterpolatedSiteDescriptorContent(Map, MavenProject, String)'
     *
     * @throws Exception
     */
    @SuppressWarnings( "rawtypes" )
    @Test
    public void testGetInterpolatedSiteDescriptorContent()
        throws Exception
    {
        File pluginXmlFile = getTestFile( "src/test/resources/unit/interpolated-site/pom.xml" );
        assertNotNull( pluginXmlFile );
        assertTrue( pluginXmlFile.exists() );

        SiteMojo siteMojo = (SiteMojo) lookupMojo( "site", pluginXmlFile );
        
        assertNotNull( siteMojo );
        
        File descriptorFile = getTestFile( "src/test/resources/unit/interpolated-site/src/site/site.xml" );
        assertNotNull( descriptorFile );
        assertTrue( descriptorFile.exists() );

        String siteDescriptorContent = FileUtils.fileRead( descriptorFile );
        assertNotNull( siteDescriptorContent );
        assertTrue( siteDescriptorContent.indexOf( "${project.name}" ) != -1 );

        SiteTool siteTool = (SiteTool) lookup( SiteTool.ROLE );
        siteDescriptorContent = siteTool.getInterpolatedSiteDescriptorContent( new HashMap(), siteMojo.project,
                                                                               siteDescriptorContent, "UTF-8", "UTF-8" );
        assertNotNull( siteDescriptorContent );
        assertTrue( siteDescriptorContent.indexOf( "${project.name}" ) == -1 );
    }
}
