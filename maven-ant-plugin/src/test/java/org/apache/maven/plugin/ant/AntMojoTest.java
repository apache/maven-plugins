package org.apache.maven.plugin.ant;

/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

/**
 * Class to test Ant plugin
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class AntMojoTest
    extends AbstractMojoTestCase
{
    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp();
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown()
        throws Exception
    {
        // nop
    }

    /**
     * Method to test Ant generation
     *
     * @throws Exception
     */
    public void testWriter()
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/ant-test/ant-test-plugin-config.xml" );
        AntMojo mojo = (AntMojo) lookupMojo( "ant", testPom );
        mojo.execute();

        File antBasedir = new File( getBasedir(), "target/test/unit/ant-test/" );
        File antBuild = new File( antBasedir, AntBuildWriter.DEFAULT_BUILD_FILENAME );
        assertTrue( antBuild.exists() );
        File antProperties = new File( antBasedir, AntBuildWriter.DEFAULT_PROPERTIES_FILENAME );
        assertTrue( antProperties.exists() );

        AntWrapper.invoke( antBuild );

        assertTrue( new File( antBasedir, "target" ).exists() );
        assertTrue( new File( antBasedir, "target/classes" ).exists() );
        assertTrue( new File( antBasedir, "target/ant-plugin-test.jar" ).exists() );

        Properties properties = new Properties();
        properties.load( new FileInputStream( new File( getBasedir(), "target/test/unit/ant-test/build.properties" ) ) );
        String repo = properties.getProperty( "maven.repo.local" );
        assertTrue( repo.equals( new File( getBasedir(), "target/local-repo" ).getAbsolutePath() ) );
    }
}
