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

import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Site;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
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
import java.util.Arrays;

/**
 * Test release:perform.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class PerformReleaseMojoTest
    extends AbstractMojoTestCase
{
    private File workingDirectory;

    public void testPerform()
        throws Exception
    {
        PerformReleaseMojo mojo = getMojoWithProjectSite( "perform.xml" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        Settings settings = mojo.getSettings();

        Mock mock = new Mock( ReleaseManager.class );
        Constraint[] constraints = new Constraint[]{new IsEqual( releaseDescriptor ), new IsEqual( settings ),
            new IsNull(), new IsEqual( new File( getBasedir(), "target/checkout" ) ),
            new IsEqual( "deploy site-deploy" ), new IsEqual( Boolean.TRUE )};
        mock.expects( new InvokeOnceMatcher() ).method( "perform" ).with( constraints );
        mojo.setReleaseManager( (ReleaseManager) mock.proxy() );

        mojo.execute();

        assertTrue( true );
    }

    public void testPerformWithoutSite()
        throws Exception
    {
        File testFileDirectory = getTestFile( "target/test-classes/mojos/perform/" );
        PerformReleaseMojo mojo =
            (PerformReleaseMojo) lookupMojo( "perform", new File( testFileDirectory, "perform.xml" ) );
        mojo.setBasedir( testFileDirectory );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        Settings settings = mojo.getSettings();

        Mock mock = new Mock( ReleaseManager.class );
        Constraint[] constraints = new Constraint[]{new IsEqual( releaseDescriptor ), new IsEqual( settings ),
            new IsNull(), new IsEqual( new File( getBasedir(), "target/checkout" ) ), new IsEqual( "deploy" ),
            new IsEqual( Boolean.TRUE )};
        mock.expects( new InvokeOnceMatcher() ).method( "perform" ).with( constraints );
        mojo.setReleaseManager( (ReleaseManager) mock.proxy() );

        mojo.execute();

        assertTrue( true );
    }

    private PerformReleaseMojo getMojoWithProjectSite( String fileName )
        throws Exception
    {
        PerformReleaseMojo mojo = (PerformReleaseMojo) lookupMojo( "perform", new File( workingDirectory, fileName ) );
        mojo.setBasedir( workingDirectory );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        DistributionManagement distributionManagement = new DistributionManagement();
        distributionManagement.setSite( new Site() );
        project.setDistributionManagement( distributionManagement );

        return mojo;
    }

    public void testPerformWithExecutionException()
        throws Exception
    {
        PerformReleaseMojo mojo = getMojoWithProjectSite( "perform.xml" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        Settings settings = mojo.getSettings();

        Mock mock = new Mock( ReleaseManager.class );
        Constraint[] constraints = new Constraint[]{new IsEqual( releaseDescriptor ), new IsEqual( settings ),
            new IsNull(), new IsEqual( new File( getBasedir(), "target/checkout" ) ),
            new IsEqual( "deploy site-deploy" ), new IsEqual( Boolean.TRUE )};
        mock.expects( new InvokeOnceMatcher() ).method( "perform" ).with( constraints ).will(
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

    public void testPerformWithExecutionFailure()
        throws Exception
    {
        PerformReleaseMojo mojo = getMojoWithProjectSite( "perform.xml" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        Settings settings = mojo.getSettings();

        Mock mock = new Mock( ReleaseManager.class );
        Constraint[] constraints = new Constraint[]{new IsEqual( releaseDescriptor ), new IsEqual( settings ),
            new IsNull(), new IsEqual( new File( getBasedir(), "target/checkout" ) ),
            new IsEqual( "deploy site-deploy" ), new IsEqual( Boolean.TRUE )};
        mock.expects( new InvokeOnceMatcher() ).method( "perform" ).with( constraints ).will(
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

    public void testPerformWithScm()
        throws Exception
    {
        PerformReleaseMojo mojo = getMojoWithProjectSite( "perform-with-scm.xml" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        Settings settings = mojo.getSettings();
        releaseDescriptor.setScmSourceUrl( "scm-url" );

        Mock mock = new Mock( ReleaseManager.class );
        Constraint[] constraints = new Constraint[]{new IsEqual( releaseDescriptor ), new IsEqual( settings ),
            new IsNull(), new IsEqual( new File( getBasedir(), "target/checkout" ) ),
            new IsEqual( "deploy site-deploy" ), new IsEqual( Boolean.TRUE )};
        mock.expects( new InvokeOnceMatcher() ).method( "perform" ).with( constraints );
        mojo.setReleaseManager( (ReleaseManager) mock.proxy() );

        mojo.execute();

        assertTrue( true );
    }

    public void testPerformWithProfiles()
        throws Exception
    {
        PerformReleaseMojo mojo = getMojoWithProjectSite( "perform.xml" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        Settings settings = mojo.getSettings();
        releaseDescriptor.setAdditionalArguments( "-P prof1,2prof" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        Profile profile1 = new Profile();
        profile1.setId( "prof1" );
        Profile profile2 = new Profile();
        profile2.setId( "2prof" );
        project.setActiveProfiles( Arrays.asList( new Profile[]{profile1, profile2} ) );

        Mock mock = new Mock( ReleaseManager.class );
        Constraint[] constraints = new Constraint[]{new IsEqual( releaseDescriptor ), new IsEqual( settings ),
            new IsNull(), new IsEqual( new File( getBasedir(), "target/checkout" ) ),
            new IsEqual( "deploy site-deploy" ), new IsEqual( Boolean.TRUE )};
        mock.expects( new InvokeOnceMatcher() ).method( "perform" ).with( constraints );
        mojo.setReleaseManager( (ReleaseManager) mock.proxy() );

        mojo.execute();

        assertTrue( true );
    }

    public void testPerformWithProfilesAndArguments()
        throws Exception
    {
        PerformReleaseMojo mojo = getMojoWithProjectSite( "perform-with-args.xml" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        Settings settings = mojo.getSettings();
        releaseDescriptor.setAdditionalArguments( "-Dmaven.test.skip=true -P prof1,2prof" );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        Profile profile1 = new Profile();
        profile1.setId( "prof1" );
        Profile profile2 = new Profile();
        profile2.setId( "2prof" );
        project.setActiveProfiles( Arrays.asList( new Profile[]{profile1, profile2} ) );

        Mock mock = new Mock( ReleaseManager.class );
        Constraint[] constraints = new Constraint[]{new IsEqual( releaseDescriptor ), new IsEqual( settings ),
            new IsNull(), new IsEqual( new File( getBasedir(), "target/checkout" ) ),
            new IsEqual( "deploy site-deploy" ), new IsEqual( Boolean.TRUE )};
        mock.expects( new InvokeOnceMatcher() ).method( "perform" ).with( constraints );
        mojo.setReleaseManager( (ReleaseManager) mock.proxy() );

        mojo.execute();

        assertTrue( true );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
        workingDirectory = getTestFile( "target/test-classes/mojos/perform" );
    }
}
