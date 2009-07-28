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
package org.apache.maven.plugin.ide;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.codehaus.plexus.util.Os;

/**
 * Test for {@link IdeUtils}
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class IdeUtilsTest
    extends TestCase
{

    public void testGetProjectNameStringIdeDependency()
    {
        IdeDependency dependency = new IdeDependency();
        dependency.setGroupId( "g" );
        dependency.setArtifactId( "a" );
        dependency.setVersion( "v" );

        String name = IdeUtils.getProjectName( IdeUtils.PROJECT_NAME_DEFAULT_TEMPLATE, dependency );
        assertEquals( dependency.getArtifactId(), name );

        name = IdeUtils.getProjectName( IdeUtils.PROJECT_NAME_WITH_GROUP_AND_VERSION_TEMPLATE, dependency );
        assertEquals( dependency.getGroupId() + "." + dependency.getArtifactId() + "-" + dependency.getVersion(), name );

        name = IdeUtils.getProjectName( IdeUtils.PROJECT_NAME_WITH_GROUP_TEMPLATE, dependency );
        assertEquals( dependency.getGroupId() + "." + dependency.getArtifactId(), name );

        name = IdeUtils.getProjectName( IdeUtils.PROJECT_NAME_WITH_VERSION_TEMPLATE, dependency );
        assertEquals( dependency.getArtifactId() + "-" + dependency.getVersion(), name );
    }

    /**
     * When the file to add is on a different drive and an absolute path expect that the returned value is the same as
     * the file to add (but with /s)
     * 
     * @throws Exception
     */
    public void testToRelativeAndFixSeparator_WhereOnDifferentDrivesAndAbsolutePaths()
        throws Exception
    {
        if ( !Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            return;
        }
        File basedir = new File( "C:\\TEMP\\EclipsePlugin.unitTest.1165557188766\\" );
        File fileToAdd = new File( "D:\\ide\\workspace\\maven\\maven-eclipse-plugin\\target\\main-output" );
        try
        {
            fileToAdd.getCanonicalPath();
        }
        catch ( IOException e )
        {
            // skip the test if the fileToAdd can't be canonicalized.
            // Likely it is because D refers to a CD drive that is not ready.
            return;
        }

        String actual = IdeUtils.toRelativeAndFixSeparator( basedir, fileToAdd, false );
        String expected = "D:/ide/workspace/maven/maven-eclipse-plugin/target/main-output";

        assertEquals( expected, actual );
    }

    /**
     * When the file to add is a relative file then expect the result to be relative to the basedir (not whatever the
     * current processes basedir is set to)
     * 
     * @throws Exception
     */
    public void testToRelativeAndFixSeparator_WhereOnDifferentDrivesAndFileToAddRelative()
        throws Exception
    {
        if ( !Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            return;
        }

        File basedir = new File( "C:\\TEMP\\EclipsePlugin.unitTest.1165557188766\\" );
        File fileToAdd = new File( "target/main-output" );

        String actual = IdeUtils.toRelativeAndFixSeparator( basedir, fileToAdd, false );
        String expected = "target/main-output";

        assertEquals( expected, actual );
    }

    /**
     * See MECLIPSE-261.
     * <p>
     * When the base dir is a windows root directory the assumption that the full path to fileToAdd is basedir + "/" +
     * fileToAdd is incorrect.
     * <p>
     * As the canonical form of a windows root dir ends in a slash, whereas the canonical form of any other file does
     * not.
     * 
     * @throws Exception
     */
    public void testToRelativeAndFixSeparator_MECLIPSE_261()
        throws Exception
    {
        if ( !Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            return;
        }

        File basedir = new File( new File( "" ).getAbsolutePath().substring( 0, 3 ) );
        File fileToAdd = new File( "target/main-output" );

        String actual = IdeUtils.toRelativeAndFixSeparator( basedir, fileToAdd, false );
        String expected = "target/main-output";

        assertEquals( expected, actual );
    }

}
