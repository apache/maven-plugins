package org.apache.maven.plugin.announcement;

import java.io.File;
import java.io.FileReader;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @version $Id$
 */
public class AnnouncementMojoTest
    extends AbstractMojoTestCase
{

    public void testAnnounceGeneration()
        throws Exception
    {
        File pom = new File( getBasedir(), "/src/test/unit/plugin-config.xml" );
        AnnouncementMojo mojo = (AnnouncementMojo) lookupMojo( "announcement-generate", pom );

        setVariableValueToObject( mojo, "xmlPath", new File( getBasedir(), "/src/test/unit/announce-changes.xml" ) );

        File outputDirectory = new File( getBasedir(), "target/test" );

        if ( outputDirectory.exists() )
        {
            FileUtils.deleteDirectory( outputDirectory );
            outputDirectory.mkdirs();
        }
        else
        {
            outputDirectory.mkdirs();
        }
        setVariableValueToObject( mojo, "outputDirectory", outputDirectory );
        setVariableValueToObject( mojo, "version", "1.1" );
        setVariableValueToObject( mojo, "template", "announcement.vm" );
        setVariableValueToObject( mojo, "templateDirectory",
                                  "/src/main/resources/org/apache/maven/plugin/announcement/" );
        setVariableValueToObject( mojo, "basedir", getBasedir() );
        setVariableValueToObject( mojo, "introduction", "Nice library" );
        mojo.execute();

        FileReader fileReader = new FileReader( new File( outputDirectory, "announcement.vm" ) );
        String result = IOUtil.toString( fileReader );

        assertContains( "Nice library", result );

        assertContains( "Changes in this version include:", result );

        assertContains( "New features:", result );

        assertContains( "o Added additional documentation on how to configure the plugin.", result );

        assertContains( "Fixed Bugs:", result );

        assertContains( "o Enable retrieving component-specific issues.  Issue: MCHANGES-88.", result );

        assertContains( "Changes:", result );

        assertContains( "o Handle different issue systems.", result );

        assertContains( "o Updated dependencies.", result );

        assertContains( "Removed:", result );

        assertContains( "o The element type \" link \" must be terminated by the matching end-tag.", result );

        assertContains( "Deleted the erroneous code.", result );
    }

    protected void assertContains( String content, String announce )
    {
        assertTrue( announce.indexOf( content ) > 0 );
    }
}
