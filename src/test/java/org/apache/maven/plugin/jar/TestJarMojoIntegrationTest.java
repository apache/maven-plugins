package org.apache.maven.plugin.jar;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

/**
 * Test for {@link TestJarMojo}
 */
public class TestJarMojoIntegrationTest
    extends AbstractJarPluginTestCase
{

    protected String getGoal()
    {
        return "test-jar";
    }

    /**
     * Tests the normal behavior of test-jar-plugin.
     * 
     * @throws Exception
     */
    public void testMJar_80_01()
        throws Exception
    {
        doTestProject( "mjar-80-01", "tests", new String[] { "test-default-configuration.properties",
            "foo/project003/AppTest.class", "foo/project003/AppIntegrationTest.class", "META-INF/MANIFEST.MF",
            "META-INF/maven/org.apache.maven.plugins/maven-jar-plugin-test-mjar-80-01/pom.properties",
            "META-INF/maven/org.apache.maven.plugins/maven-jar-plugin-test-mjar-80-01/pom.xml" } );
    }

    /**
     * Tests includes.
     * 
     * @throws Exception
     */
    public void testMJar_80_02()
        throws Exception
    {
        doTestProject( "mjar-80-02", "tests", new String[] { "foo/project003/AppIntegrationTest.class",
            "META-INF/MANIFEST.MF",
            "META-INF/maven/org.apache.maven.plugins/maven-jar-plugin-test-mjar-80-02/pom.properties",
            "META-INF/maven/org.apache.maven.plugins/maven-jar-plugin-test-mjar-80-02/pom.xml" } );
    }

    /**
     * Tests excludes.
     * 
     * @throws Exception
     */
    public void testMJar_80_03()
        throws Exception
    {
        doTestProject( "mjar-80-03", "tests", new String[] { "test-default-configuration.properties",
            "foo/project003/AppTest.class", "foo/project003/AppIntegrationTest.class", "META-INF/MANIFEST.MF",
            "META-INF/maven/org.apache.maven.plugins/maven-jar-plugin-test-mjar-80-03/pom.properties",
            "META-INF/maven/org.apache.maven.plugins/maven-jar-plugin-test-mjar-80-03/pom.xml" } );

    }
}
