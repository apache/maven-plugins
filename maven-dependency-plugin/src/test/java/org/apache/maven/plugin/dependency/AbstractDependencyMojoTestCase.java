package org.apache.maven.plugin.dependency;

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
import java.io.IOException;

import org.apache.maven.plugin.dependency.testUtils.DependencyArtifactStubFactory;
import org.apache.maven.plugin.dependency.testUtils.DependencyTestUtils;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public abstract class AbstractDependencyMojoTestCase
    extends AbstractMojoTestCase
{

    protected File testDir;

    protected DependencyArtifactStubFactory stubFactory;

    public AbstractDependencyMojoTestCase()
    {
        super();
    }

    protected void setUp( String testDirStr, boolean createFiles )
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp();
        testDir = new File( getBasedir(), "target" + File.separatorChar + "unit-tests" + File.separatorChar
            + testDirStr + File.separatorChar );
        DependencyTestUtils.removeDirectory( testDir );
        assertFalse( testDir.exists() );

        stubFactory = new DependencyArtifactStubFactory( this.testDir, createFiles );

    }

    protected void tearDown()
    {
        if ( testDir != null )
        {
            try
            {
                DependencyTestUtils.removeDirectory( testDir );
            }
            catch ( IOException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                fail( "Trying to remove directory:" + testDir + "\r\n" + e.toString() );
            }
            assertFalse( testDir.exists() );
            
            testDir = null;
        }
        
        stubFactory = null;
    }

}
