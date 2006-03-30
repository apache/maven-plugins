package org.apache.maven.plugins.release;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.maven.model.Contributor;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.release.helpers.ScmHelper;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.codehaus.plexus.PlexusTestCase;
import org.jmock.cglib.Mock;
import org.jmock.core.constraint.IsAnything;
import org.jmock.core.matcher.MethodNameMatcher;
import org.jmock.core.stub.ReturnStub;
import org.jmock.core.stub.ThrowStub;

/**
 * Test for PrepareReleaseMojo
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class PrepareReleaseMojoTest
    extends PlexusTestCase
{

    private PrepareReleaseMojo mojo;

    private Mock scmHelperMock;

    private ScmManager scmManager;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        mojo = new PrepareReleaseMojo();
        mojo.basedir = new File( getBasedir(), "target/test" );
        mojo.basedir.mkdirs();

        scmManager = new ScmManagerStub();

        scmHelperMock = new Mock( ScmHelper.class );
        ScmHelper scmHelper = (ScmHelper) scmHelperMock.proxy();
        mojo.setScmHelper( scmHelper );
        mojo.setScmManager( scmManager );
    }

    /**
     * Test for writePom with special characters in the pom
     * @throws Exception
     */
    public void testWritePom()
        throws Exception
    {
        Model model = new Model();
        Contributor contributor = new Contributor();
        /* hack to avoid problems with sources encoding, this string contains accentuated "aeiou" */
        String s = new String( new byte[] { -31, -23, -19, -13, -6 } );
        contributor.setName( s );
        model.addContributor( contributor );
        File file = new File( mojo.basedir, "testWritePom.xml" );

        scmHelperMock.expects( new MethodNameMatcher( new IsAnything() ) );
        scmHelperMock.expects( new MethodNameMatcher( "getScmManager" ) ).will( new ReturnStub( scmManager ) );

        mojo.writePom( file, model, "version" );

        MavenXpp3Reader pomReader = new MavenXpp3Reader();

        Model readModel = pomReader.read( new BufferedReader( new FileReader( file ) ) );
        Contributor readContributor = (Contributor) readModel.getContributors().get( 0 );
        assertEquals( contributor.getName(), readContributor.getName() );

        scmHelperMock.verify();
    }

    /**
     * Test for writePom when the ScmHelper throws a ScmException
     * @throws Exception
     */
    public void testWritePomWithScmException()
        throws Exception
    {
        Model model = new Model();
        File file = new File( mojo.basedir, "testWritePomWithScmException.xml" );

        scmHelperMock.expects( new MethodNameMatcher( new IsAnything() ) );
        scmHelperMock.expects( new MethodNameMatcher( "getScmManager" ) ).will( new ReturnStub( scmManager ) );
        scmHelperMock.expects( new MethodNameMatcher( "requiresEditMode" ) )
            .will( new ThrowStub( new ScmException( "testing" ) ) );

        try
        {
            mojo.writePom( file, model, "version" );
            fail( "MojoExecutionException was not thrown" );
        }
        catch ( MojoExecutionException e )
        {
            // expected
        }

        scmHelperMock.verify();
    }

}
