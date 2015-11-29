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
public class SourceJarMojoTest
    extends AbstractSourcePluginTestCase
{
    protected String getGoal()
    {
        return "jar";
    }

    private String[] addMavenDescriptor( String project, String[] listOfElements )
    {
        final String METAINF = "META-INF/";
        final String MAVENSOURCE = "maven/source/maven-source-plugin-test-";
        String[] result = new String[listOfElements.length + 5];
        System.arraycopy( listOfElements, 0, result, 0, listOfElements.length );
        result[listOfElements.length] = METAINF + "maven/";
        result[listOfElements.length + 1] = METAINF + "maven/source/";
        result[listOfElements.length + 2] = METAINF + MAVENSOURCE + project + "/";
        result[listOfElements.length + 3] = METAINF + MAVENSOURCE + project + "/pom.properties";
        result[listOfElements.length + 4] = METAINF + MAVENSOURCE + project + "/pom.xml";
        return result;
    }

    public void testDefaultConfiguration()
        throws Exception
    {
        doTestProjectWithSourceArchive( "project-001",
                                        addMavenDescriptor( "project-001",
                                                            new String[] { "default-configuration.properties",
                                                                "foo/project001/App.java", "foo/project001/", "foo/",
                                                                "META-INF/MANIFEST.MF", "META-INF/" } ),
                                        "sources" );
    }

    public void testExcludes()
        throws Exception
    {
        doTestProjectWithSourceArchive( "project-003",
                                        addMavenDescriptor( "project-003",
                                                            new String[] { "default-configuration.properties",
                                                                "foo/project003/App.java", "foo/project003/", "foo/",
                                                                "META-INF/MANIFEST.MF", "META-INF/" } ),
                                        "sources" );
    }

    public void testNoSources()
        throws Exception
    {
        executeMojo( "project-005", "sources" );
        // Now make sure that no archive got created
        final File expectedFile = getTestTargetDir( "project-005" );
        assertFalse( "Source archive should not have been created[" + expectedFile.getAbsolutePath() + "]",
                     expectedFile.exists() );
    }

    public void testIncludes()
        throws Exception
    {
        doTestProjectWithSourceArchive( "project-007", addMavenDescriptor( "project-007", new String[] {
            "templates/configuration-template.properties", "foo/project007/App.java", "templates/", "foo/project007/",
            "foo/", "META-INF/MANIFEST.MF", "META-INF/" } ), "sources" );
    }

    public void testIncludePom()
        throws Exception
    {
        doTestProjectWithSourceArchive( "project-009",
                                        addMavenDescriptor( "project-009",
                                                            new String[] { "default-configuration.properties",
                                                                "pom.xml", "foo/project009/App.java", "foo/project009/",
                                                                "foo/", "META-INF/MANIFEST.MF", "META-INF/" } ),
                                        "sources" );
    }

    public void testIncludeMavenDescriptorWhenExplicitlyConfigured()
        throws Exception
    {
        doTestProjectWithSourceArchive( "project-010",
                                        addMavenDescriptor( "project-010",
                                                            new String[] { "default-configuration.properties",
                                                                "foo/project010/App.java", "foo/project010/", "foo/",
                                                                "META-INF/MANIFEST.MF", "META-INF/" } ),
                                        "sources" );
    }
}
