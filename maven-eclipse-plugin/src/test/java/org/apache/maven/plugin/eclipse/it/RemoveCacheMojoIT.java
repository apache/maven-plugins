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

package org.apache.maven.plugin.eclipse.it;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:baerrach@apache.org">Barrie Treloar</a>
 * @version $Id$
 */
public class RemoveCacheMojoIT
    extends AbstractEclipsePluginIT
{
    /**
     * Project-46 uses which does not have sources/javadocs for biz.aQute:bndlib:0.0.145. Once the repo is seeded with
     * marker files run remove-cache and verify the marker files no longer exist.
     * 
     * @throws Exception test failures
     */
    public void testRemoveCache()
        throws Exception
    {
        testProject( "project-46" );
        assertNotAvailableMarkerFileExists( "biz.aQute", "bndlib", "0.0.145", null, "sources" );
        assertNotAvailableMarkerFileExists( "biz.aQute", "bndlib", "0.0.145", null, "javadoc" );

        File basedir = getTestFile( "target/test-classes/projects/project-46" );
        File pom = new File( basedir, "pom.xml" );
        String pluginSpec = getPluginCLISpecification();
        List goals = new ArrayList();
        goals.add( pluginSpec + ":remove-cache" );
        executeMaven( pom, new Properties(), goals );

        assertNotAvailableMarkerFileDoesNotExist( "biz.aQute", "bndlib", "0.0.145", null, "sources" );
        assertNotAvailableMarkerFileDoesNotExist( "biz.aQute", "bndlib", "0.0.145", null, "javadoc" );
    }
}
