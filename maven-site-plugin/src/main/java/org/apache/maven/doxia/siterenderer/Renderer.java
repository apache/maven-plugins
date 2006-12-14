package org.apache.maven.doxia.siterenderer;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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

import org.apache.maven.doxia.module.xhtml.decoration.render.RenderingContext;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/**
 * @author <a href="mailto:evenisse@org.codehaus.org>Emmanuel Venisse</a>
 * @version $Id:Renderer.java 348612 2005-11-24 12:54:19 +1100 (Thu, 24 Nov 2005) brett $
 */
public interface Renderer
{
    String ROLE = Renderer.class.getName();

    void render( Collection documents, SiteRenderingContext siteRenderingContext, File outputDirectory )
        throws RendererException, IOException;

    void generateDocument( Writer writer, SiteRendererSink sink, SiteRenderingContext siteRenderingContext )
        throws RendererException;

    SiteRenderingContext createContextForSkin( File skinFile, Map attributes, DecorationModel decoration,
                                               String defaultWindowTitle, Locale locale )
        throws IOException;

    SiteRenderingContext createContextForTemplate( File templateFile, File skinFile, Map attributes,
                                                   DecorationModel decoration, String defaultWindowTitle,
                                                   Locale locale )
        throws MalformedURLException;

    void copyResources( SiteRenderingContext siteRenderingContext, File resourcesDirectory, File outputDirectory )
        throws IOException;

    Map locateDocumentFiles( SiteRenderingContext siteRenderingContext )
        throws IOException, RendererException;

    void renderDocument( Writer writer, RenderingContext renderingContext, SiteRenderingContext context )
        throws RendererException, FileNotFoundException, UnsupportedEncodingException;
}
