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
package org.apache.maven.plugin.eclipse;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * Test class for EclipseSourceDir
 * 
 * @author <a href="mailto:baerrach@apache.org">Barrie Treloar</a>
 * @version $Id$
 */
public class EclipseSourceDirTest
    extends TestCase
{
    private EclipseSourceDir testFixture_src_main_java()
    {
        List includes = new ArrayList();
        includes.add( EclipsePlugin.JAVA_FILE_PATTERN );
        return new EclipseSourceDir( "/src/main/java", null, false, false, includes, null, false );
    }

    private EclipseSourceDir testFixture_src_main_resources()
    {
        List excludes = new ArrayList();
        excludes.add( EclipsePlugin.JAVA_FILE_PATTERN );
        return new EclipseSourceDir( "/src/main/resources", "target/classes", true, false, null, excludes, false );
    }

    private EclipseSourceDir testFixture_src_test_java()
    {
        List includes = new ArrayList();
        includes.add( EclipsePlugin.JAVA_FILE_PATTERN );
        return new EclipseSourceDir( "/src/test/java", "target/test-classes", false, true, includes, null, false );
    }

    private EclipseSourceDir testFixture_src_test_resources()
    {
        List excludes = new ArrayList();
        excludes.add( EclipsePlugin.JAVA_FILE_PATTERN );
        return new EclipseSourceDir( "/src/test/resources", "target/test-classes", true, true, null, excludes, false );
    }

    /**
     * A merge with a source directory and a resource directory results in:
     * <ul>
     * <li>source directory
     * <li>includes is empty
     * <li>excludes is empty
     * </ul>
     * 
     * @throws Exception
     */
    public void testMerge_src_main_java_with_src_main_resources()
        throws Exception
    {
        EclipseSourceDir src_main_java = testFixture_src_main_java();
        EclipseSourceDir src_main_resources = testFixture_src_main_resources();

        src_main_java.merge( src_main_resources );
        assertEquals( "source /src/main/java: output=null, include=[], exclude=[], test=false, filtering=false",
                      src_main_java.toString() );
    }

    public void testMerge_two_resource_directories()
        throws Exception
    {
        EclipseSourceDir resource1 = testFixture_src_main_resources();
        EclipseSourceDir resource2 = testFixture_src_main_resources();

        resource1.getInclude().add( "**/*.txt" );
        resource1.getExclude().add( "**/*.svn" );

        resource2.getInclude().add( "**/*.xml" );
        resource2.getExclude().add( "**/*.cvs" );

        resource1.merge( resource2 );

        assertEquals(
                      "resource /src/main/resources: output=target/classes, include=[**/*.txt|**/*.xml], exclude=[**/*.java|**/*.svn|**/*.cvs], test=false, filtering=false",
                      resource1.toString() );
    }

    public void testMerge_two_resource_directories_with_duplicates()
        throws Exception
    {
        EclipseSourceDir resource1 = testFixture_src_main_resources();
        EclipseSourceDir resource2 = testFixture_src_main_resources();

        resource1.getInclude().add( "**/*.dup" );
        resource1.getInclude().add( "**/*.txt" );
        resource1.getExclude().add( "**/*.svn" );
        resource1.getExclude().add( "**/*~" );

        resource2.getInclude().add( "**/*.xml" );
        resource2.getInclude().add( "**/*.dup" );
        resource2.getExclude().add( "**/*.cvs" );
        resource2.getExclude().add( "**/*~" );

        resource1.merge( resource2 );

        assertEquals(
                      "resource /src/main/resources: output=target/classes, include=[**/*.dup|**/*.txt|**/*.xml], exclude=[**/*.java|**/*.svn|**/*~|**/*.cvs], test=false, filtering=false",
                      resource1.toString() );
    }

    public void testToString_src_main_java()
    {
        EclipseSourceDir objectUnderTest = testFixture_src_main_java();

        assertEquals(
                      "source /src/main/java: output=null, include=[**/*.java], exclude=[], test=false, filtering=false",
                      objectUnderTest.toString() );
    }

    public void testToString_src_main_resources()
    {
        EclipseSourceDir objectUnderTest = testFixture_src_main_resources();

        assertEquals(
                      "resource /src/main/resources: output=target/classes, include=[], exclude=[**/*.java], test=false, filtering=false",
                      objectUnderTest.toString() );
    }

    public void testToString_src_test_java()
    {
        EclipseSourceDir objectUnderTest = testFixture_src_test_java();

        assertEquals(
                      "source /src/test/java: output=target/test-classes, include=[**/*.java], exclude=[], test=true, filtering=false",
                      objectUnderTest.toString() );
    }

    public void testToString_src_test_resources()
    {
        EclipseSourceDir objectUnderTest = testFixture_src_test_resources();

        assertEquals(
                      "resource /src/test/resources: output=target/test-classes, include=[], exclude=[**/*.java], test=true, filtering=false",
                      objectUnderTest.toString() );
    }

    public void testMergWhenFilteringIsNotIdentical() throws Exception
    {
        EclipseSourceDir resource1 = testFixture_src_main_resources();
        EclipseSourceDir resource2 = testFixture_src_main_resources();

        resource1.getInclude().add("**/*.dup");
        resource1.getInclude().add("**/*.txt");
        resource1.getExclude().add("**/*.svn");
        resource1.getExclude().add("**/*~");

        resource2.getInclude().add("**/*.xml");
        resource2.getInclude().add("**/*.dup");
        resource2.getExclude().add("**/*.cvs");
        resource2.getExclude().add("**/*~");
        resource2.setFiltering(true);

        boolean result = resource1.merge(resource2);

        assertFalse("Resource dirs should not have been merged successfully", result);
       
        assertEquals(
            "resource /src/main/resources: output=target/classes, include=[**/*.dup|**/*.txt|**/*.xml], exclude=[**/*.java|**/*.svn|**/*~|**/*.cvs], test=false, filtering=false",
            resource1.toString());
    }

    public void testMergWhenFilteringIsNotIdenticalOverlapping() throws Exception
    {
        EclipseSourceDir resource1 = testFixture_src_main_resources();
        EclipseSourceDir resource2 = testFixture_src_main_resources();

        resource2.getInclude().add("**/*.properties");
        resource2.setFiltering(true);

        boolean result = resource1.merge(resource2);

        assertFalse("Resource dirs should not have been merged successfully", result);

        assertEquals(
            "resource /src/main/resources: output=target/classes, include=[], exclude=[**/*.java], test=false, filtering=false",
            resource1.toString());
    }

}
