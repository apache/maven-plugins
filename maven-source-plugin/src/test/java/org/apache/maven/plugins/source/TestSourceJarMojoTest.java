package org.apache.maven.plugins.source;

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

/**
 * @author <a href="mailto:oching@exist.com">Maria Odea Ching</a>
 */
public class TestSourceJarMojoTest
    extends AbstractSourcePluginTestCase
{

    protected String getGoal()
    {
        return "test-jar";
    }


    public void testDefaultConfiguration()
        throws Exception
    {
        doTestProjectWithTestSourceArchive( "project-001", new String[]{ "test-default-configuration.properties",
            "foo/project001/AppTest.java", "foo/project001/", "foo/", "META-INF/MANIFEST.MF", "META-INF/" },
                                            "test-sources" );
    }


    public void testExcludes()
        throws Exception
    {
        doTestProjectWithTestSourceArchive( "project-003", new String[]{ "test-default-configuration.properties",
            "foo/project003/AppTest.java", "foo/project003/", "foo/", "META-INF/MANIFEST.MF", "META-INF/" },
                                            "test-sources" );
    }


    public void testNoSources()
        throws Exception
    {
        executeMojo( "project-005", "test-sources" );

        // Now make sure that no archive got created
        final File expectedFile = getTestTargetDir( "project-005" );
        assertFalse("Test source archive should not have been created[" + expectedFile.getAbsolutePath() + "]",
                expectedFile.exists());
    }

    public void testIncludeMavenDescriptorWhenExplicitlyConfigured()
            throws Exception {
        doTestProjectWithSourceArchive("project-010",
                new String[]{
                        "default-configuration.properties", "foo/project010/App.java",
                        "foo/project010/", "foo/", "META-INF/MANIFEST.MF", "META-INF/",
                        "META-INF/maven/", "META-INF/maven/source/",
                        "META-INF/maven/source/maven-source-plugin-test-project-010/",
                        "META-INF/maven/source/maven-source-plugin-test-project-010/pom.xml",
                        "META-INF/maven/source/maven-source-plugin-test-project-010/pom" +
                                ".properties"
                },
                "test-sources");
    }

}
