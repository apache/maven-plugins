package org.apache.maven.plugin.changes;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.plugins.changes.model.Release;

/**
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.4
 */
public class ReleaseUtilsTestCase extends TestCase
{
    public void testMergeReleases()
        throws Exception
    {
        Log log = new SilentLog();
        ReleaseUtils releaseUtils = new ReleaseUtils( log );

        List firstReleases = new ArrayList();
        List secondReleases = new ArrayList();
        List mergedReleases;

        mergedReleases = releaseUtils.mergeReleases( firstReleases, secondReleases );
        assertEquals( "Both empty", 0, mergedReleases.size() );

        Release release = new Release();
        release.setVersion( "1.0" );
        firstReleases.add( release );

        mergedReleases = releaseUtils.mergeReleases( firstReleases, secondReleases );
        assertEquals( "One release in first", 1, mergedReleases.size() );

        release = new Release();
        release.setVersion( "1.1" );
        secondReleases.add( release );

        mergedReleases = releaseUtils.mergeReleases( firstReleases, secondReleases );
        assertEquals( "One release each", 2, mergedReleases.size() );

        release = new Release();
        release.setVersion( "1.1" );
        firstReleases.add( release );

        mergedReleases = releaseUtils.mergeReleases( firstReleases, secondReleases );
        assertEquals( "Two releases in first, one release in second with one version being the same",
                      2, mergedReleases.size() );

        release = new Release();
        release.setVersion( "1.2" );
        secondReleases.add( release );

        mergedReleases = releaseUtils.mergeReleases( firstReleases, secondReleases );
        assertEquals( "Two releases each with one version being the same", 3, mergedReleases.size() );
    }
}
