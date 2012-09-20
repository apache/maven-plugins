package org.apache.maven.plugin.resources;

import java.io.File;
import java.util.Collections;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.resources.stub.MavenProjectResourcesStub;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.FileUtils;

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

/**
 * @author Olivier Lamy
 * @version $Id$
 */
public class CopyResourcesMojoTest
    extends AbstractMojoTestCase
{

    protected final static String defaultPomFilePath = "/target/test-classes/unit/resources-test/plugin-config.xml";

    File outputDirectory = new File( getBasedir(), "/target/copyResourcesTests" );

    protected void setUp()
        throws Exception
    {
        super.setUp();
        if ( !outputDirectory.exists() )
        {
            outputDirectory.mkdirs();
        }
        else
        {
            FileUtils.cleanDirectory( outputDirectory );
        }
    }

    public void testCopyWithoutFiltering()
        throws Exception
    {
        File testPom = new File( getBasedir(), defaultPomFilePath );
        ResourcesMojo mojo = (ResourcesMojo) lookupMojo( "resources", testPom );

        mojo.setOutputDirectory( outputDirectory );

        Resource resource = new Resource();
        resource.setDirectory( getBasedir() + "/src/test/unit-files/copy-resources-test/no-filter" );
        resource.setFiltering( false );

        mojo.setResources( Collections.singletonList( resource ) );

        MavenProjectResourcesStub project = new MavenProjectResourcesStub( "CopyResourcesMojoTest" );
        File targetFile =  new File( getBasedir(), "/target/copyResourcesTests" );
        project.setBaseDir( targetFile );
        setVariableValueToObject( mojo, "project", project );
        mojo.execute();

        assertTrue( new File( targetFile, "config.properties" ).exists() );
    }

}
