package org.apache.maven.plugins.site;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugins.site.SimpleDavServerHandler.HttpRequest;
import org.mortbay.jetty.security.B64Code;
import org.mortbay.proxy.AsyncProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @since 
 * @version $Id$
 */
public class AuthAsyncProxyServlet
    extends AsyncProxyServlet
{
    private Map<String, String> authentications;

    private long sleepTime = 0;

    private Logger log = LoggerFactory.getLogger( getClass() );

    List<HttpRequest> httpRequests = new ArrayList<HttpRequest>();

    private File siteTargetPath;

    /**
     * Constructor for non authentication servlet.
     */
    public AuthAsyncProxyServlet( File siteTargetPath )
    {
        super();
        this.siteTargetPath = siteTargetPath;
    }

    /**
     * Constructor for authentication servlet.
     * 
     * @param authentications a map of user/password
     */
    public AuthAsyncProxyServlet( Map<String, String> authentications, File siteTargetPath )
    {
        this( siteTargetPath );

        this.authentications = authentications;
    }

    /**
     * Constructor for authentication servlet.
     * 
     * @param authentications a map of user/password
     * @param sleepTime a positive time to sleep the service thread (for timeout)
     */
    public AuthAsyncProxyServlet( Map<String, String> authentications, long sleepTime, File siteTargetPath )
    {
        this( siteTargetPath );

        this.authentications = authentications;
        this.sleepTime = sleepTime;
    }

    /** {@inheritDoc} */
    public void service( ServletRequest req, ServletResponse res )
        throws ServletException, IOException
    {
        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        log.info( "handle " );

        if ( this.authentications != null && !this.authentications.isEmpty() )
        {
            String proxyAuthorization = request.getHeader( "Proxy-Authorization" );
            if ( proxyAuthorization != null && proxyAuthorization.startsWith( "Basic " ) )
            {
                String proxyAuth = proxyAuthorization.substring( 6 );
                String authorization = B64Code.decode( proxyAuth );
                String[] authTokens = authorization.split( ":" );
                String user = authTokens[0];
                String password = authTokens[1];

                if ( this.authentications.get( user ) == null )
                {
                    throw new IllegalArgumentException( user + " not found in the map!" );
                }

                if ( sleepTime > 0 )
                {
                    try
                    {
                        Thread.sleep( sleepTime );
                    }
                    catch ( InterruptedException e )
                    {
                        // nop
                    }
                }
                String authPass = this.authentications.get( user ).toString();
                if ( password.equals( authPass ) )
                {
                    String targetPath = request.getServletPath();

                    HttpRequest rq = new HttpRequest();
                    rq.method = request.getMethod();
                    rq.path = targetPath;

                    @SuppressWarnings( "rawtypes" )
                    Enumeration headerNames = request.getHeaderNames();
                    while ( headerNames.hasMoreElements() )
                    {
                        String name = (String) headerNames.nextElement();
                        rq.headers.put( name, request.getHeader( name ) );
                    }

                    httpRequests.add( rq );

                    if ( request.getMethod().equalsIgnoreCase( "PUT" ) && targetPath != null )
                    {
                        File targetFile = new File( siteTargetPath, targetPath );
                        log.info( "writing file " + targetFile.getPath() );
                        FileUtils.writeByteArrayToFile( targetFile, IOUtils.toByteArray( request.getInputStream() ) );
                    }

                    //PrintWriter writer = response.getWriter();

                    response.setStatus( HttpServletResponse.SC_OK );
                    return;
                }
            }

            // Proxy-Authenticate Basic realm="CCProxy Authorization"
            response.addHeader( "Proxy-Authenticate", "Basic realm=\"Jetty Proxy Authorization\"" );
            response.setStatus( HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED );
            return;
        }

        super.service( req, res );
    }
}
