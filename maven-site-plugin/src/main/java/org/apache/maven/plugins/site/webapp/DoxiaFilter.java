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

import org.apache.maven.doxia.siterenderer.Renderer;
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

/**
 * Render a page as requested.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class DoxiaFilter
    implements Filter
{
    private File siteDirectory;

    private Renderer siteRenderer;

    private SiteRenderingContext context;

    public void init( FilterConfig filterConfig )
        throws ServletException
    {
        ServletContext servletContext = filterConfig.getServletContext();
        siteDirectory = (File) servletContext.getAttribute( "siteDirectory" );
        siteRenderer = (Renderer) servletContext.getAttribute( "siteRenderer" );
        context = (SiteRenderingContext) servletContext.getAttribute( "context" );
    }

    public void doFilter( ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain )
        throws IOException, ServletException
    {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        String path = req.getServletPath();

        if ( "/index.html".equals( path ) )
        {
/*
            // TODO
            context.setLocale( req.getLocale() );

            SiteRendererSink sink = null;
            try
            {
                sink = siteRenderer.createSink( siteDirectory, "index.html" );
            }
            catch ( RendererException e )
            {
                // TODO
            }

            Locale locale = context.getLocale();
            String title = i18n.getString( "site-plugin", locale, "report.index.title" ).trim() + " " + project.getName();
            String title = "title";

            sink.head();
            sink.title();
            sink.text( title );
            sink.title_();
            sink.head_();
            sink.body();

            sink.section1();
            sink.sectionTitle1();
            sink.text( title );
            sink.sectionTitle1_();

            sink.paragraph();

            if ( project.getDescription() != null )
            {
                // TODO How to handle i18n?
                sink.text( project.getDescription() );
            }
            else
            {
                sink.text( i18n.getString( "site-plugin", locale, "report.index.nodescription" ) );
            }

            sink.paragraph_();

            sink.body_();

            sink.flush();

            sink.close();

            try
            {
                siteRenderer.generateDocument( servletResponse.getWriter(), sink, context );
            }
            catch ( RendererException e )
            {
                throw new ServletException( e );
            }
*/
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
