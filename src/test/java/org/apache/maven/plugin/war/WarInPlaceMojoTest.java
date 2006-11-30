package org.apache.maven.plugin.war;

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

import org.apache.maven.plugin.war.stub.MavenProjectBasicStub;
import org.apache.maven.plugin.war.stub.ResourceStub;

import java.io.File;
import java.util.LinkedList;

public class WarInPlaceMojoTest
    extends AbstractWarMojoTest
{
    protected static final String pomFilePath =
        getBasedir() + "/target/test-classes/unit/warexplodedinplacemojo/plugin-config.xml";

    protected File getTestDirectory()
        throws Exception
    {
        return new File( getBasedir(), "target/test-classes/unit/warexplodedinplacemojo/test-dir" );
    }

    private WarInPlaceMojo mojo;

    public void setUp()
        throws Exception
    {
        super.setUp();

        mojo = (WarInPlaceMojo) lookupMojo( "inplace", pomFilePath );
        assertNotNull( mojo );
    }

    public void testEnvironment()
        throws Exception
    {
        // see setUp
    }

    /**
     * @throws Exception
     */
    public void testSimpleExplodedInplaceWar()
        throws Exception
    {
        // setup test data
        String testId = "SimpleExplodedInplaceWar";
        MavenProjectBasicStub project = new MavenProjectBasicStub();
        File webAppSource = createWebAppSource( testId );
        File classesDir = createClassesDir( testId, true );
        File webAppResource = new File( getTestDirectory(), "resources" );
        File sampleResource = new File( webAppResource, "pix/panis_na.jpg" );
        ResourceStub[] resources = new ResourceStub[]{new ResourceStub()};

        createFile( sampleResource );

        // configure mojo
        resources[0].setDirectory( webAppResource.getAbsolutePath() );
        this.configureMojo( mojo, new LinkedList(), classesDir, webAppSource, null, project );
        setVariableValueToObject( mojo, "webResources", resources );
        mojo.execute();

        // validate operation
        File expectedWebSourceFile = new File( webAppSource, "pansit.jsp" );
        File expectedWebSource2File = new File( webAppSource, "org/web/app/last-exile.jsp" );
        File expectedWebResourceFile = new File( webAppSource, "pix/panis_na.jpg" );
        File expectedWEBINFDir = new File( webAppSource, "WEB-INF" );
        File expectedMETAINFDir = new File( webAppSource, "META-INF" );

        assertTrue( "source files not found: " + expectedWebSourceFile.toString(), expectedWebSourceFile.exists() );
        assertTrue( "source files not found: " + expectedWebSource2File.toString(), expectedWebSource2File.exists() );
        assertTrue( "resources doesn't exist: " + expectedWebResourceFile, expectedWebResourceFile.exists() );
        assertTrue( "WEB-INF not found", expectedWEBINFDir.exists() );
        assertTrue( "META-INF not found", expectedMETAINFDir.exists() );
    }
}
