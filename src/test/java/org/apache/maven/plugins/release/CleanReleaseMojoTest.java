package org.apache.maven.plugins.release;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugins.release.config.ReleaseConfiguration;
import org.jmock.Mock;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.matcher.InvokeOnceMatcher;

/**
 * Test release:clean.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CleanReleaseMojoTest
    extends AbstractMojoTestCase
{
    protected CleanReleaseMojo mojo;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        mojo = (CleanReleaseMojo) lookupMojo( "clean", getTestFile( "target/test-classes/mojos/clean/clean.xml" ) );
    }

    public void testClean()
        throws MojoFailureException, MojoExecutionException
    {
        ReleaseConfiguration releaseConfiguration = new ReleaseConfiguration();
        releaseConfiguration.setReactorProjects( mojo.getReactorProjects() );

        Mock mock = new Mock( ReleaseManager.class );
        mock.expects( new InvokeOnceMatcher() ).method( "clean" ).with( new IsEqual( releaseConfiguration ) );
        mojo.setReleaseManager( (ReleaseManager) mock.proxy() );

        mojo.execute();

        assertTrue( true );
    }
}
