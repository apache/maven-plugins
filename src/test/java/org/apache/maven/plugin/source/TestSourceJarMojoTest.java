package org.apache.maven.plugin.source;

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
 * @author <a href="mailto:oching@exist.com">Maria Odea Ching</a>
 */
public class TestSourceJarMojoTest
    extends AbstractSourcePluginTestCase
{

    public void testProject002()
        throws Exception
    {
        doTestProjectWithTestSourceArchive( "project-002", new String[]{"test-default-configuration.properties",
            "foo/project002/AppTest.java", "foo/project002/", "foo/", "META-INF/MANIFEST.MF", "META-INF/"

        } );
    }

    public void testProject004()
        throws Exception
    {
        doTestProjectWithTestSourceArchive( "project-004", new String[]{"test-default-configuration.properties",
            "foo/project004/AppTest.java", "foo/project004/", "foo/", "META-INF/MANIFEST.MF", "META-INF/"

        } );
    }

    public void testProject006()
        throws Exception
    {
        final File baseDir = executeMojo( "project-006", new Properties() );
        // Now make sure that no archive got created
        final File expectedFile = getTestSourceArchive( baseDir, "project-006" );
        assertFalse( "Test source archive should not have been created[" + expectedFile.getAbsolutePath() + "]",
                     expectedFile.exists() );

    }

}
