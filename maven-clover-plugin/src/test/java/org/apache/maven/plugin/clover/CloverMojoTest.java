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
package org.apache.maven.plugin.clover;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.clover.internal.Locator;
import org.jmock.MockObjectTestCase;
import org.jmock.Mock;

import java.io.File;

/**
 * Unit tests for {@link org.apache.maven.plugin.clover.AbstractCloverMojo}.
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

        Mock mockLocator = mock( Locator.class );
        mojo.setLocator( (Locator) mockLocator.proxy() );

        // Ensure that the system property is not already set
        System.clearProperty( "clover.license.path" );

        mojo.setLicenseLocation( "build-tools/clover.license" );
        mockLocator.expects( once() ).method( "resolveLocation" )
            .with( eq( "build-tools/clover.license" ), eq( "clover.license" ) )
            .will( returnValue( new File( "targetFile" ) ) );

        mojo.registerLicenseFile();

        assertEquals( "targetFile", System.getProperty( "clover.license.path" ) );
    }
}
