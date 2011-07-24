package org.apache.maven.plugins.site.webapp;

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

import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.plugins.site.ReportDocumentRenderer;

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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Render a page as requested.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class DoxiaFilter
    implements Filter
{
    public static final String SITE_RENDERER_KEY = "siteRenderer";

    public static final String I18N_DOXIA_CONTEXTS_KEY = "i18nDoxiaContexts";

    public static final String LOCALES_LIST_KEY = "localesList";

    private Renderer siteRenderer;

    private Map<String, DoxiaBean> i18nDoxiaContexts;

    private List<Locale> localesList;

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init( FilterConfig filterConfig )
        throws ServletException
    {
        ServletContext servletContext = filterConfig.getServletContext();

        siteRenderer = (Renderer) servletContext.getAttribute( SITE_RENDERER_KEY );

        i18nDoxiaContexts = (Map<String, DoxiaBean>) servletContext.getAttribute( I18N_DOXIA_CONTEXTS_KEY );

        localesList = (List<Locale>) servletContext.getAttribute( LOCALES_LIST_KEY );
    }

    /**
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter( ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain )
        throws IOException, ServletException
    {
        HttpServletRequest req = (HttpServletRequest) servletRequest;

        // ----------------------------------------------------------------------
        // Handle the servlet path
        // ----------------------------------------------------------------------
        String path = req.getServletPath();
        // welcome file
        if ( path.endsWith( "/" ) )
        {
            path += "index.html";
        }

        // Remove the /
        path = path.substring( 1 );

        // Handle locale request
        SiteRenderingContext context;
        Map<String, DocumentRenderer> documents;
        File generatedSiteDirectory;

        String localeWanted = null;
        for ( Locale locale : localesList )
        {
            if ( path.startsWith( locale.getLanguage() + "/" ) )
            {
                localeWanted = locale.toString();
                path = path.substring( locale.getLanguage().length() + 1 );
            }
        }

        if ( localeWanted == null )
        {
            DoxiaBean defaultDoxiaBean = i18nDoxiaContexts.get( "default" );
            if ( defaultDoxiaBean == null )
            {
                throw new ServletException( "No doxia bean found for the default locale" );
            }
            context = defaultDoxiaBean.getContext();
            documents = defaultDoxiaBean.getDocuments();
            generatedSiteDirectory = defaultDoxiaBean.getGeneratedSiteDirectory();
        }
        else
        {
            DoxiaBean i18nDoxiaBean = i18nDoxiaContexts.get( localeWanted );
            if ( i18nDoxiaBean == null )
            {
                throw new ServletException( "No doxia bean found for the locale " + localeWanted );
            }
            context = i18nDoxiaBean.getContext();
            documents = i18nDoxiaBean.getDocuments();
            generatedSiteDirectory = i18nDoxiaBean.getGeneratedSiteDirectory();
        }

        // ----------------------------------------------------------------------
        // Handle report and documents
        // ----------------------------------------------------------------------
        if ( documents.containsKey( path ) )
        {
            try
            {
                DocumentRenderer renderer = (DocumentRenderer) documents.get( path );
                renderer.renderDocument( servletResponse.getWriter(), siteRenderer, context );

                if ( renderer instanceof ReportDocumentRenderer )
                {
                    ReportDocumentRenderer reportDocumentRenderer = (ReportDocumentRenderer) renderer;
                    if ( reportDocumentRenderer.isExternalReport() )
                    {
                        try
                        {
                            filterChain.doFilter( servletRequest, servletResponse );
                        }
                        catch ( Exception e )
                        {
                            throw new ServletException( e );
                        }
                    }
                }
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
                Map<String, DocumentRenderer> locateDocuments = siteRenderer.locateDocumentFiles( context );

                if ( locateDocuments.containsKey( path ) )
                {
                    DocumentRenderer renderer = (DocumentRenderer) locateDocuments.get( path );
                    renderer.renderDocument( servletResponse.getWriter(), siteRenderer, context );
                }
            }
            catch ( RendererException e )
            {
                throw new ServletException( e );
            }

            List<File> originalSiteDirectories = new ArrayList<File>( context.getSiteDirectories() );
            for ( File dir : originalSiteDirectories )
            {
                context.addSiteDirectory( dir );
            }
        }
        else
        {
            filterChain.doFilter( servletRequest, servletResponse );
        }
    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy()
    {
    }
}
