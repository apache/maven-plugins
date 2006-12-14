package org.apache.maven.doxia.siterenderer;

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

import org.apache.maven.doxia.module.xhtml.decoration.render.RenderingContext;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * Renders a page with Doxia.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class DoxiaDocumentRenderer
    implements DocumentRenderer
{
    private RenderingContext renderingContext;

    public DoxiaDocumentRenderer( RenderingContext renderingContext )
    {
        this.renderingContext = renderingContext;
    }

    public void renderDocument( Writer writer, Renderer renderer, SiteRenderingContext siteRenderingContext )
        throws RendererException, FileNotFoundException, UnsupportedEncodingException
    {
        renderer.renderDocument( writer, renderingContext, siteRenderingContext );
    }

    public String getOutputName()
    {
        return renderingContext.getOutputName();
    }

    public RenderingContext getRenderingContext()
    {
        return renderingContext;
    }

    public boolean isOverwrite()
    {
        return false;
    }

}
