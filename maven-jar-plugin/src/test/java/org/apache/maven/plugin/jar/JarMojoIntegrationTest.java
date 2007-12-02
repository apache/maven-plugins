package org.apache.maven.plugin.jar;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.Properties;

/**
 * Test for {@link JarMojo}
 */
public class JarMojoIntegrationTest
    extends AbstractJarPluginTestCase
{

    protected String getGoal()
    {
        return "jar";
    }
    /**
     * Tests the normal behavior of jar-plugin.
     * @throws Exception
     */
    public void testMJar_30_01()
        throws Exception
    {
        doTestProject( "mjar-30-01", null, new String[] { "default-configuration.properties",
            "foo/project001/App.class", "META-INF/MANIFEST.MF",
            "META-INF/maven/org.apache.maven.plugins/maven-jar-plugin-test-mjar-30-01/pom.properties",
            "META-INF/maven/org.apache.maven.plugins/maven-jar-plugin-test-mjar-30-01/pom.xml" } );
    }
    /**
     * Tests a includes.
     * @throws Exception
     */
    public void testMJar_30_02()
        throws Exception
    {
        doTestProject( "mjar-30-02", null, new String[] { "service/TestInterface.class", "META-INF/MANIFEST.MF",
            "META-INF/maven/org.apache.maven.plugins/maven-jar-plugin-test-mjar-30-02/pom.properties",
            "META-INF/maven/org.apache.maven.plugins/maven-jar-plugin-test-mjar-30-02/pom.xml" } );
    }
    /**
     * Tests excludes.
     * @throws Exception
     */
    public void testMJar_30_03()
        throws Exception
    {
        doTestProject( "mjar-30-03", null, new String[] { "default-configuration.properties", "META-INF/MANIFEST.MF",
            "META-INF/maven/org.apache.maven.plugins/maven-jar-plugin-test-mjar-30-03/pom.properties",
            "META-INF/maven/org.apache.maven.plugins/maven-jar-plugin-test-mjar-30-03/pom.xml" } );

    }

    public void testMultipleJars()
        throws Exception
    {
        String projectName = "project-004";
        File baseDir = executeMojo( projectName, new Properties() );

        assertJarArchive( baseDir, projectName, null );
        assertArchiveContent( baseDir, projectName, null, new String[] { "service/TestInterface.class",
            "service/impl/TestImplementation.class", "TestCompile1.class", "notIncluded.xml", "META-INF/MANIFEST.MF",
            "META-INF/maven/org.apache.maven.plugins/maven-jar-plugin-test-project-004/pom.properties",
            "META-INF/maven/org.apache.maven.plugins/maven-jar-plugin-test-project-004/pom.xml" } );

        assertJarArchive( baseDir, projectName, "service" );
        assertArchiveContent( baseDir, projectName, "service", new String[] { "service/TestInterface.class",
            "META-INF/MANIFEST.MF",
            "META-INF/maven/org.apache.maven.plugins/maven-jar-plugin-test-project-004/pom.properties",
            "META-INF/maven/org.apache.maven.plugins/maven-jar-plugin-test-project-004/pom.xml" } );
    }
}
