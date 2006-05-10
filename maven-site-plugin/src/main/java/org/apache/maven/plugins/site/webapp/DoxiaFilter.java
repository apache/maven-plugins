package org.apache.maven.plugins.site.webapp;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Render a page as requested.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class DoxiaFilter
    implements Filter
{
    private Renderer siteRenderer;

    private SiteRenderingContext context;

    private Map documents;

    private File generatedSiteDirectory;

    private List originalSiteDirectories;

    public void init( FilterConfig filterConfig )
        throws ServletException
    {
        ServletContext servletContext = filterConfig.getServletContext();
        siteRenderer = (Renderer) servletContext.getAttribute( "siteRenderer" );
        context = (SiteRenderingContext) servletContext.getAttribute( "context" );
        documents = (Map) servletContext.getAttribute( "documents" );
        generatedSiteDirectory = (File) servletContext.getAttribute( "generatedSiteDirectory" );
        originalSiteDirectories = new ArrayList( context.getSiteDirectories() );
    }

    public void doFilter( ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain )
        throws IOException, ServletException
    {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        String path = req.getServletPath().substring( 1 );

        if ( documents.containsKey( path ) )
        {
            // TODO: documents are not right for the locale
            context.setLocale( req.getLocale() );

            try
            {
                DocumentRenderer renderer = (DocumentRenderer) documents.get( path );
                renderer.renderDocument( servletResponse.getWriter(), siteRenderer, context );
            }
            catch ( RendererException e )
            {
                throw new ServletException( e );
            }
        }
        else if ( generatedSiteDirectory != null && generatedSiteDirectory.exists() )
        {
            context.getSiteDirectories().clear();
            context.addSiteDirectory( generatedSiteDirectory );
            try
            {
                Map documents = siteRenderer.locateDocumentFiles( context );

                if ( documents.containsKey( path ) )
                {
                    // TODO: documents are not right for the locale
                    context.setLocale( req.getLocale() );

                    DocumentRenderer renderer = (DocumentRenderer) documents.get( path );
                    renderer.renderDocument( servletResponse.getWriter(), siteRenderer, context );
                }
            }
            catch ( RendererException e )
            {
                throw new ServletException( e );
            }
            for ( Iterator i = originalSiteDirectories.iterator(); i.hasNext(); )
            {
                File dir = (File) i.next();
                context.addSiteDirectory( dir );
            }
        }
        else
        {
            filterChain.doFilter( servletRequest, servletResponse );
        }
    }

    public void destroy()
    {
    }
}
