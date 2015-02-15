package org.apache.maven.plugin.ear.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.ear.AbstractEarTestBase;
import org.apache.maven.plugin.ear.EarModule;
import org.apache.maven.plugin.ear.EjbModule;
import org.junit.Test;

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

/**
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public class EarMavenArchiverTest
    extends AbstractEarTestBase
{

    @Test
    public void testSimpleEjbModule()
    {
        final List<EarModule> modules = new ArrayList<EarModule>();
        final EarModule module = new EjbModule( createArtifact( "foo", "ejb" ) );
        setUri( module, "foo-1.0.jar" );
        modules.add( module );

        final EarMavenArchiver archiver = new EarMavenArchiver( modules );
        assertEquals( "foo-1.0.jar", archiver.generateClassPathEntry( "" ) );

    }

    @Test
    public void testSimpleJarModuleWithCustomBundleDir()
    {
        final List<EarModule> modules = new ArrayList<EarModule>();
        final EarModule module = new EjbModule( createArtifact( "foo", "jar" ) );
        setUri( module, "libs/foo-1.0.jar" );
        modules.add( module );

        final EarMavenArchiver archiver = new EarMavenArchiver( modules );
        assertEquals( "libs/foo-1.0.jar", archiver.generateClassPathEntry( "" ) );

    }

    @Test
    public void testTwoModules()
    {
        final List<EarModule> modules = new ArrayList<EarModule>();
        final EarModule module = new EjbModule( createArtifact( "foo", "ejb" ) );
        setUri( module, "foo-1.0.jar" );
        modules.add( module );

        final EarModule module2 = new EjbModule( createArtifact( "bar", "war" ) );
        setUri( module2, "bar-2.0.1.war" );
        modules.add( module2 );

        final EarMavenArchiver archiver = new EarMavenArchiver( modules );
        assertEquals( "foo-1.0.jar bar-2.0.1.war", archiver.generateClassPathEntry( "" ) );

    }

}