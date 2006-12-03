/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.maven.plugin.clover.internal;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.clover.internal.AbstractCloverMojo;
import org.jmock.MockObjectTestCase;
import org.jmock.Mock;
import org.codehaus.plexus.resource.ResourceManager;

import java.io.File;

/**
 * Unit tests for {@link org.apache.maven.plugin.clover.internal.AbstractCloverMojo}.
 * 
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @version $Id$
 */
public class CloverMojoTest extends MockObjectTestCase
{
    public class TestableAbstractCloverMojo extends AbstractCloverMojo
    {
        public void execute() throws MojoExecutionException
        {
            // Voluntarily left blank
        }
    }

    public void testRegisterLicenseFile() throws MojoExecutionException
    {
        TestableAbstractCloverMojo mojo = new TestableAbstractCloverMojo();

        Mock mockResourceManager = mock( ResourceManager.class );
        mojo.setResourceManager( (ResourceManager) mockResourceManager.proxy() );

        // Ensure that the system property is not already set
        System.setProperty( "clover.license.path", "" );

        mojo.setLicenseLocation( "build-tools/clover.license" );
        mockResourceManager.expects( once() ).method( "getResourceAsFile" )
            .with( eq( "build-tools/clover.license" ) )
            .will( returnValue( new File( "targetFile" ) ) );

        mojo.registerLicenseFile();

        assertEquals( "targetFile", System.getProperty( "clover.license.path" ) );
    }
}
