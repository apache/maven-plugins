package org.apache.maven.report.projectinfo;

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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
import org.apache.maven.report.projectinfo.stubs.SettingsStub;
import org.apache.maven.settings.Settings;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * @author <a href="mailto:vincent.siveton@crim.ca">Vincent Siveton</a>
 * @version $Id$
 */
public class ProjectInfoReportUtilsTest
    extends AbstractMojoTestCase
{
    private static final int MAX_IDLE_TIME = 30000;

    private int port = -1;

    private Settings settingsStub;

    private Server jettyServer;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        final List<org.apache.maven.settings.Server> servers = new ArrayList<org.apache.maven.settings.Server>();
        org.apache.maven.settings.Server server = new org.apache.maven.settings.Server();
        server.setId( "localhost" );
        server.setUsername( "admin" );
        server.setPassword( "admin" );
        servers.add( server );
        settingsStub = new SettingsStub()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public org.apache.maven.settings.Server getServer( String serverId )
            {
                for ( org.apache.maven.settings.Server server : getServers() )
                {
                    if ( server.getId().equals( serverId ) )
                    {
                        return server;
                    }
                }
                return null;
            }

            @Override
            public List<org.apache.maven.settings.Server> getServers()
            {
                return servers;
            }
        };

    }

    private MavenProject getMavenProjectStub( boolean https )
    {
        final DistributionManagement distributionManagement = new DistributionManagement();
        DeploymentRepository repository = new DeploymentRepository();
        repository.setId( "localhost" );
        repository.setUrl( ( https ? "https" : "http" ) + "://localhost:" + port );
        distributionManagement.setRepository( repository );
        distributionManagement.setSnapshotRepository( repository );
        return new MavenProjectStub()
        {
            @Override
            public DistributionManagement getDistributionManagement()
            {
                return distributionManagement;
            }
        };
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    public void testGetInputStreamURL()
        throws Exception
    {
        // file
        URL url = new File( getBasedir(), "/target/classes/project-info-report.properties" ).toURI().toURL();

        String content = ProjectInfoReportUtils.getContent( url, getMavenProjectStub( false ), settingsStub, null );
        Assert.assertNotNull( content );
        Assert.assertTrue( content.contains( "Licensed to the Apache Software Foundation" ) );

        // http + no auth
        startJetty( false, false );

        url = new URL( "http://localhost:" + port + "/project-info-report.properties" );

        content = ProjectInfoReportUtils.getContent( url, getMavenProjectStub( false ), settingsStub, null );
        Assert.assertNotNull( content );
        Assert.assertTrue( content.contains( "Licensed to the Apache Software Foundation" ) );

        stopJetty();

        // http + auth
        startJetty( false, true );

        url = new URL( "http://localhost:" + port + "/project-info-report.properties" );

        content = ProjectInfoReportUtils.getContent( url, getMavenProjectStub( false ), settingsStub, null );
        Assert.assertNotNull( content );
        Assert.assertTrue( content.contains( "Licensed to the Apache Software Foundation" ) );

        stopJetty();

        // https + no auth
        startJetty( true, false );

        url = new URL( "https://localhost:" + port + "/project-info-report.properties" );

        content = ProjectInfoReportUtils.getContent( url, getMavenProjectStub( true ), settingsStub, null );
        Assert.assertNotNull( content );
        Assert.assertTrue( content.contains( "Licensed to the Apache Software Foundation" ) );

        stopJetty();

        // https + auth
        startJetty( true, true );

        url = new URL( "https://localhost:" + port + "/project-info-report.properties" );

        content = ProjectInfoReportUtils.getContent( url, getMavenProjectStub( true ), settingsStub, null );
        Assert.assertNotNull( content );
        Assert.assertTrue( content.contains( "Licensed to the Apache Software Foundation" ) );

        stopJetty();

        // TODO need to test with a proxy
    }

    private void startJetty( boolean isSSL, boolean withAuth )
        throws Exception
    {
        jettyServer = new Server();
        jettyServer.setStopAtShutdown( true );

        Connector connector = ( isSSL ? getSSLConnector() : getDefaultConnector() );
        jettyServer.setConnectors( new Connector[] { connector } );

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath( "/" );
        webapp.setResourceBase( getBasedir() + "/target/classes/" );

        webapp.setServer( jettyServer );

        if ( withAuth )
        {
            Constraint constraint = new Constraint();
            constraint.setName( Constraint.__BASIC_AUTH );
            constraint.setRoles( new String[] { "user", "admin" } );
            constraint.setAuthenticate( true );

            ConstraintMapping cm = new ConstraintMapping();
            cm.setConstraint( constraint );
            cm.setPathSpec( "/*" );

            SecurityHandler sh = new SecurityHandler();
            sh.setUserRealm( new HashUserRealm( "MyRealm", getBasedir() + "/src/test/resources/realm.properties" ) );
            sh.setConstraintMappings( new ConstraintMapping[] { cm } );

            webapp.addHandler( sh );
        }

        DefaultHandler defaultHandler = new DefaultHandler();
        defaultHandler.setServer( jettyServer );

        Handler[] handlers = new Handler[2];
        handlers[0] = webapp;
        handlers[1] = defaultHandler;
        jettyServer.setHandlers( handlers );

        jettyServer.start();

        port = connector.getLocalPort();
    }

    private void stopJetty()
        throws Exception
    {
        if ( jettyServer != null )
        {
            jettyServer.stop();

            jettyServer = null;

            port = -1;
        }
    }

    private Connector getDefaultConnector()
    {
        Connector connector = new SelectChannelConnector();
        connector.setMaxIdleTime( MAX_IDLE_TIME );
        return connector;
    }

    private Connector getSSLConnector()
    {
        SslSocketConnector connector = new SslSocketConnector();
        connector.setKeystore( getBasedir() + "/target/jetty.jks" );
        connector.setPassword( "apache" );
        connector.setKeyPassword( "apache" );
        connector.setTruststore( getBasedir() + "/target/jetty.jks" );
        connector.setTrustPassword( "apache" );
        return connector;
    }
}
