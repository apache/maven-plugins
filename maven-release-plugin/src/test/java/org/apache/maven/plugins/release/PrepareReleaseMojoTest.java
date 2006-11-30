package org.apache.maven.plugins.release;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.jmock.Mock;
import org.jmock.core.Constraint;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.constraint.IsNull;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.stub.ThrowStub;

import java.io.File;

/**
 * Test release:prepare.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class PrepareReleaseMojoTest
    extends AbstractMojoTestCase
{
    public void testPrepare()
        throws Exception
    {
        File testFile = getTestFile( "target/test-classes/mojos/prepare/prepare.xml" );
        PrepareReleaseMojo mojo = (PrepareReleaseMojo) lookupMojo( "prepare", testFile );
        mojo.setBasedir( testFile.getParentFile() );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setWorkingDirectory( testFile.getParentFile().getAbsolutePath() );
        Mock mock = new Mock( ReleaseManager.class );
        Constraint[] constraints = new Constraint[]{new IsEqual( releaseDescriptor ), new IsEqual( mojo.getSettings() ),
            new IsNull(), new IsEqual( Boolean.TRUE ), new IsEqual( Boolean.FALSE )};
        mock.expects( new InvokeOnceMatcher() ).method( "prepare" ).with( constraints );
        mojo.setReleaseManager( (ReleaseManager) mock.proxy() );

        mojo.execute();

        assertTrue( true );
    }

    public void testPrepareWithExecutionException()
        throws Exception
    {
        File testFile = getTestFile( "target/test-classes/mojos/prepare/prepare.xml" );
        PrepareReleaseMojo mojo = (PrepareReleaseMojo) lookupMojo( "prepare", testFile );
        mojo.setBasedir( testFile.getParentFile() );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setWorkingDirectory( testFile.getParentFile().getAbsolutePath() );
        Mock mock = new Mock( ReleaseManager.class );
        Constraint[] constraints = new Constraint[]{new IsEqual( releaseDescriptor ), new IsEqual( mojo.getSettings() ),
            new IsNull(), new IsEqual( Boolean.TRUE ), new IsEqual( Boolean.FALSE )};
        mock.expects( new InvokeOnceMatcher() ).method( "prepare" ).with( constraints ).will(
            new ThrowStub( new ReleaseExecutionException( "..." ) ) );
        mojo.setReleaseManager( (ReleaseManager) mock.proxy() );

        try
        {
            mojo.execute();

            fail( "Should have thrown an exception" );
        }
        catch ( MojoExecutionException e )
        {
            assertEquals( "Check cause", ReleaseExecutionException.class, e.getCause().getClass() );
        }
    }

    public void testPrepareWithExecutionFailure()
        throws Exception
    {
        File testFile = getTestFile( "target/test-classes/mojos/prepare/prepare.xml" );
        PrepareReleaseMojo mojo = (PrepareReleaseMojo) lookupMojo( "prepare", testFile );
        mojo.setBasedir( testFile.getParentFile() );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setWorkingDirectory( testFile.getParentFile().getAbsolutePath() );
        Mock mock = new Mock( ReleaseManager.class );
        Constraint[] constraints = new Constraint[]{new IsEqual( releaseDescriptor ), new IsEqual( mojo.getSettings() ),
            new IsNull(), new IsEqual( Boolean.TRUE ), new IsEqual( Boolean.FALSE )};
        mock.expects( new InvokeOnceMatcher() ).method( "prepare" ).with( constraints ).will(
            new ThrowStub( new ReleaseFailureException( "..." ) ) );
        mojo.setReleaseManager( (ReleaseManager) mock.proxy() );

        try
        {
            mojo.execute();

            fail( "Should have thrown an exception" );
        }
        catch ( MojoFailureException e )
        {
            assertNull( "Check no cause", e.getCause() );
        }
    }

/*
    public void testPerformWithScm()
        throws Exception
    {
        PerformReleaseMojo mojo = (PerformReleaseMojo) lookupMojo( "perform", getTestFile(
            "target/test-classes/mojos/perform/perform-with-scm.xml" ) );

        ReleaseDescriptor releaseConfiguration = new ReleaseDescriptor();
        releaseConfiguration.setSettings( mojo.getSettings() );
        releaseConfiguration.setUrl( "scm-url" );

        Mock mock = new Mock( ReleaseManager.class );
        Constraint[] constraints = new Constraint[]{new IsEqual( releaseConfiguration ),
            new IsEqual( new File( getBasedir(), "target/checkout" ) ), new IsEqual( "deploy site-deploy" ),
            new IsEqual( Boolean.TRUE )};
        mock.expects( new InvokeOnceMatcher() ).method( "perform" ).with( constraints );
        mojo.setReleaseManager( (ReleaseManager) mock.proxy() );

        mojo.execute();

        assertTrue( true );
    }

    public void testPerformWithProfiles()
        throws Exception
    {
        PerformReleaseMojo mojo = (PerformReleaseMojo) lookupMojo( "perform", getTestFile(
            "target/test-classes/mojos/perform/perform.xml" ) );

        ReleaseDescriptor releaseConfiguration = new ReleaseDescriptor();
        releaseConfiguration.setSettings( mojo.getSettings() );
        releaseConfiguration.setAdditionalArguments( "-P prof1,2prof" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        Profile profile1 = new Profile();
        profile1.setId( "prof1" );
        Profile profile2 = new Profile();
        profile2.setId( "2prof" );
        project.setActiveProfiles( Arrays.asList( new Profile[]{profile1, profile2} ) );

        Mock mock = new Mock( ReleaseManager.class );
        Constraint[] constraints = new Constraint[]{new IsEqual( releaseConfiguration ),
            new IsEqual( new File( getBasedir(), "target/checkout" ) ), new IsEqual( "deploy site-deploy" ),
            new IsEqual( Boolean.TRUE )};
        mock.expects( new InvokeOnceMatcher() ).method( "perform" ).with( constraints );
        mojo.setReleaseManager( (ReleaseManager) mock.proxy() );

        mojo.execute();

        assertTrue( true );
    }

    public void testPerformWithProfilesAndArguments()
        throws Exception
    {
        PerformReleaseMojo mojo = (PerformReleaseMojo) lookupMojo( "perform", getTestFile(
            "target/test-classes/mojos/perform/perform-with-args.xml" ) );

        ReleaseDescriptor releaseConfiguration = new ReleaseDescriptor();
        releaseConfiguration.setSettings( mojo.getSettings() );
        releaseConfiguration.setAdditionalArguments( "-Dmaven.test.skip=true -P prof1,2prof" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        Profile profile1 = new Profile();
        profile1.setId( "prof1" );
        Profile profile2 = new Profile();
        profile2.setId( "2prof" );
        project.setActiveProfiles( Arrays.asList( new Profile[]{profile1, profile2} ) );

        Mock mock = new Mock( ReleaseManager.class );
        Constraint[] constraints = new Constraint[]{new IsEqual( releaseConfiguration ),
            new IsEqual( new File( getBasedir(), "target/checkout" ) ), new IsEqual( "deploy site-deploy" ),
            new IsEqual( Boolean.TRUE )};
        mock.expects( new InvokeOnceMatcher() ).method( "perform" ).with( constraints );
        mojo.setReleaseManager( (ReleaseManager) mock.proxy() );

        mojo.execute();

        assertTrue( true );
    }
*/
}
