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

import junit.framework.TestCase;

import java.io.File;

/**
 * Tests <code>AntBuildWriter</code>.
 *
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class AntBuildWriterTest
    extends TestCase
{

    public void testGetProjectRepoDirectory()
    {
        String basedir = new File( System.getProperty( "java.io.tmpdir" ) ).getPath();

        // non-project rooted repo URLs
        assertEquals( null, AntBuildWriter.getProjectRepoDirectory( "http://maven.apache.org/", basedir ) );
        assertEquals( null, AntBuildWriter.getProjectRepoDirectory( "file:///just-some-test-directory", basedir ) );

        // RFC-compliant URLs
        assertEquals( "", AntBuildWriter.getProjectRepoDirectory( new File( basedir ).toURI().toString(), basedir ) );
        assertEquals( "dir", AntBuildWriter.getProjectRepoDirectory( new File( basedir, "dir" ).toURI().toString(),
                                                                     basedir ) );
        assertEquals( "dir/subdir",
                      AntBuildWriter.getProjectRepoDirectory( new File( basedir, "dir/subdir" ).toURI().toString(),
                                                              basedir ) );

        // not so strict URLs
        assertEquals( "", AntBuildWriter.getProjectRepoDirectory( "file://" + basedir, basedir ) );
        assertEquals( "dir", AntBuildWriter.getProjectRepoDirectory( "file://" + basedir + "/dir", basedir ) );
        assertEquals( "dir/subdir",
                      AntBuildWriter.getProjectRepoDirectory( "file://" + basedir + "/dir/subdir", basedir ) );

        // URLs with encoded characters
        assertEquals( "some dir",
                      AntBuildWriter.getProjectRepoDirectory( new File( basedir, "some dir" ).toURI().toString(),
                                                              basedir ) );
    }

}
