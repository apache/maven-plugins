package org.apache.maven.plugin.ant;

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
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;

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
     * Method to test Default Ant generation
     *
     * @throws Exception
     */
    public void testDefaultProject()
        throws Exception
    {
        invokeAntMojo( "ant-test" );
    }

    /**
     * Method to test Project with no dependencies
     *
     * @throws Exception
     */
    public void testProjectWithNoDep()
        throws Exception
    {
        invokeAntMojo( "ant-nodep-test" );
    }

    /**
     * Method to test Project with no dependencies
     *
     * @throws Exception
     */
    public void testProjectWithJavadoc()
        throws Exception
    {
        invokeAntMojo( "ant-javadoc-test" );
    }

    /**
     * Invoke Ant mojo.
     * <br/>
     * The Maven test project should be in a directory called <code>testProject</code> in  "src/test/resources/unit/" directory.
     * The Maven test project should be called <code>"testProject"-plugin-config.xml</code> and should produced
     * <code>ant-plugin-test.jar</code> as artefact.
     *
     * @param testProject
     * @throws Exception
     */
    private void invokeAntMojo( String testProject )
        throws Exception
    {
        File testPom = new File( getBasedir(), "src/test/resources/unit/" + testProject + "/pom.xml" );
        AntMojo mojo = (AntMojo) lookupMojo( "ant", testPom );
        mojo.execute();

        MavenProject currentProject = (MavenProject) getVariableValueFromObject( mojo, "project" );

        File antBasedir = new File( getBasedir(), "target/test/unit/" + testProject + "/" );
        File antBuild = new File( antBasedir, AntBuildWriter.DEFAULT_BUILD_FILENAME );
        assertTrue( antBuild.exists() );
        if ( !currentProject.getPackaging().toLowerCase().equals( "pom" ) )
        {
            File antProperties = new File( antBasedir, AntBuildWriter.DEFAULT_MAVEN_PROPERTIES_FILENAME );
            assertTrue( antProperties.exists() );
        }

        AntWrapper.invoke( antBuild );

        if ( !currentProject.getPackaging().toLowerCase().equals( "pom" ) )
        {
            assertTrue( new File( antBasedir, "target" ).exists() );
            assertTrue( new File( antBasedir, "target/classes" ).exists() );
            assertTrue( new File( antBasedir, "target/" + currentProject.getBuild().getFinalName() + ".jar" ).exists() );

            Properties properties = new Properties();
            properties
                .load( new FileInputStream( new File( antBasedir, AntBuildWriter.DEFAULT_MAVEN_PROPERTIES_FILENAME ) ) );
            String repo = properties.getProperty( "maven.repo.local" );
            assertTrue( repo.equals( new File( getBasedir(), "target/local-repo" ).getAbsolutePath() ) );
        }
    }
}
