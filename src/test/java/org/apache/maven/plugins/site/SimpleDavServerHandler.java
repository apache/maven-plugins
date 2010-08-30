package org.apache.maven.plugins.site;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * @author <a href="mailto:olamy@apache.org">olamy</a>
 * @since 3.0-beta-2
 * @version $Id$
 */
public class SimpleDavServerHandler
{
    
    private Logger log = LoggerFactory.getLogger( getClass() );
    
    private Server server;
    
    private File siteTargetPath;
    
    List<HttpRequest> httpRequests = new ArrayList<HttpRequest>();
    
    public SimpleDavServerHandler(final File targetPath )
        throws Exception
    {
        this.siteTargetPath = targetPath;
        Handler repoHandler = new AbstractHandler()
        {
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
                throws IOException, ServletException
            {
                String targetPath = request.getPathInfo();
                
                HttpRequest rq = new HttpRequest();
                rq.method = request.getMethod();
                rq.path = targetPath;

                Enumeration headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements())
                {
                    String name = (String) headerNames.nextElement();
                    rq.headers.put( name, request.getHeader( name ) );
                }
                
                httpRequests.add( rq );
                
              
                if ( request.getMethod().equalsIgnoreCase( "PUT" ) )
                {
                    File targetFile = new File( siteTargetPath, targetPath );
                    log.info( "writing file " + targetFile.getPath() );
                    FileUtils.writeByteArrayToFile( targetFile, IOUtils.toByteArray( request.getInputStream() ) );
                }
                
                //PrintWriter writer = response.getWriter();

                response.setStatus( HttpServletResponse.SC_OK );

                ( (Request) request ).setHandled( true );
            }
        };
        server = new Server( 0 );
        server.setHandler( repoHandler );
        server.start();

    }

    public SimpleDavServerHandler( Servlet servlet )
        throws Exception
    {
        siteTargetPath = null;
        server = new Server( 0 );
        Context context = new Context( server, "/", 0 );

        context.addServlet( new ServletHolder( servlet ), "/" );
        
        server.start();
    }   
    
    public int getPort()
    {
        return server.getConnectors()[0].getLocalPort();
    }

    public void stop()
        throws Exception
    {
        server.stop();
    }
    
    
    static class HttpRequest
    {
        Map<String, String> headers = new HashMap<String,String>();
        
        String method;
        
        String path;
        
        HttpRequest()
        {
            // nop
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder( method ).append( " path " ).append( path )
                .append( SystemUtils.LINE_SEPARATOR );
            for ( Entry<String, String> entry : headers.entrySet() )
            {
                sb.append( entry.getKey() ).append( " : " ).append( entry.getValue() )
                    .append( SystemUtils.LINE_SEPARATOR );
            }
            return sb.toString();
        }
    }
}
