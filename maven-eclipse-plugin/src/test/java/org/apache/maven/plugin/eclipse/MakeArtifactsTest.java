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

import junit.framework.TestCase;

import org.apache.maven.model.Dependency;

/**
 * @author Fabrizio Giustina
 * @version $Id$
 */
public class MakeArtifactsTest
    extends TestCase
{

    /**
     * Mojo under test.
     */
    private MakeArtifactsMojo mojo;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        super.setUp();
        mojo = new MakeArtifactsMojo();
    }

    /**
     * Tests the parsing of the "Require-Bundle" entry from a manifest.
     */
    public void testParseDependencies()
    {
        Dependency[] deps = mojo.parseDependencies( "org.eclipse.ui;bundle-version=\"[3.2.0,4.0.0)\","
            + "org.eclipse.ui.console;resolution:=\"optional\";bundle-version=\"[3.1.100,4.0.0)\",org.eclipse.help;"
            + "bundle-version=\"[3.2.0,4.0.0)\",org.eclipse.core.expressions;bundle-version=\"[3.2.0,4.0.0)\"" );

        assertEquals( 4, deps.length );
        assertEquals( "org.eclipse.ui", deps[0].getArtifactId() );
        assertEquals( "[3.2.0,4.0.0)", deps[0].getVersion() );
        assertEquals( "org.eclipse.ui.console", deps[1].getArtifactId() );
        assertEquals( "[3.1.100,4.0.0)", deps[1].getVersion() );
        assertEquals( "org.eclipse.help", deps[2].getArtifactId() );
        assertEquals( "[3.2.0,4.0.0)", deps[2].getVersion() );
        assertEquals( "org.eclipse.core.expressions", deps[3].getArtifactId() );
        assertEquals( "[3.2.0,4.0.0)", deps[3].getVersion() );
    }

    /**
     * Tests the parsing of the "Require-Bundle" entry from a manifest.
     */
    public void testParseDependenciesWithQualifier()
    {
        Dependency[] deps = mojo
            .parseDependencies( "org.eclipse.ui;bundle-version=\"[3.2.0.0,4.0.0.0)\","
                + "org.eclipse.ui.console;resolution:=\"optional\";bundle-version=\"[3.1.100.0,4.0.0.0)\",org.eclipse.help;"
                + "bundle-version=\"[3.2.0.1,4.0.0.2)\",org.eclipse.core.expressions;bundle-version=\"[3.2.0.0,4.0.0.0)\"" );

        assertEquals( 4, deps.length );
        assertEquals( "org.eclipse.ui", deps[0].getArtifactId() );
        assertEquals( "[3.2.0-0,4.0.0-0)", deps[0].getVersion() );
        assertEquals( "org.eclipse.ui.console", deps[1].getArtifactId() );
        assertEquals( "[3.1.100-0,4.0.0-0)", deps[1].getVersion() );
        assertEquals( "org.eclipse.help", deps[2].getArtifactId() );
        assertEquals( "[3.2.0-1,4.0.0-2)", deps[2].getVersion() );
        assertEquals( "org.eclipse.core.expressions", deps[3].getArtifactId() );
        assertEquals( "[3.2.0-0,4.0.0-0)", deps[3].getVersion() );
    }

    /**
     * Test the generation of a groupId from a bundle symbolic name.
     */
    public void testCreateGroupId()
    {
        assertEquals( "test", mojo.createGroupId( "test" ) );
        assertEquals( "org.eclipse", mojo.createGroupId( "org.eclipse" ) );
        assertEquals( "org.eclipse.jdt", mojo.createGroupId( "org.eclipse.jdt" ) );
        assertEquals( "org.eclipse.jdt", mojo.createGroupId( "org.eclipse.jdt.apt" ) );
        assertEquals( "org.eclipse.jdt", mojo.createGroupId( "org.eclipse.jdt.apt.core" ) );
    }

    public void testOsgiVersionToMavenVersion()
    {
        assertEquals( "1.2.3", mojo.osgiVersionToMavenVersion( "1.2.3", null, false ) );
        assertEquals( "1.2.3", mojo.osgiVersionToMavenVersion( "1.2.3", "20060101", false ) );
        assertEquals( "1.2.3", mojo.osgiVersionToMavenVersion( "1.2.3", null, true ) );
        assertEquals( "1.2.3", mojo.osgiVersionToMavenVersion( "1.2.3", "20060101", true ) );

        assertEquals( "1.2.3", mojo.osgiVersionToMavenVersion( "1.2.3.1", null, true ) );
        assertEquals( "1.2.3", mojo.osgiVersionToMavenVersion( "1.2.3.1", "20060101", true ) );
        assertEquals( "1.2.3-20060101", mojo.osgiVersionToMavenVersion( "1.2.3.1", "20060101", false ) );
        assertEquals( "1.2.3-1", mojo.osgiVersionToMavenVersion( "1.2.3.1", null, false ) );
    }
}
