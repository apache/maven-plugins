package org.apache.maven.doxia.siterenderer.sink;

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

import org.apache.maven.doxia.module.xhtml.XhtmlSink;
import org.apache.maven.doxia.module.xhtml.decoration.render.RenderingContext;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:evenisse@org.codehaus.org>Emmanuel Venisse</a>
 * @version $Id:SiteRendererSink.java 348612 2005-11-24 12:54:19 +1100 (Thu, 24 Nov 2005) brett $
 */
public class SiteRendererSink
    extends XhtmlSink
    implements org.codehaus.doxia.sink.Sink
{
    private String date = "";

    private String title = "";

    private List authors = new ArrayList();

    private final Writer writer;

    public SiteRendererSink( RenderingContext renderingContext )
    {
        this( new StringWriter(), renderingContext );
    }

    private SiteRendererSink( StringWriter writer, RenderingContext renderingContext )
    {
        super( writer, renderingContext, null );

        this.writer = writer;
    }

    /**
     * @see org.apache.maven.doxia.module.xhtml.XhtmlSink#title_()
     */
    public void title_()
    {
        if ( getBuffer().length() > 0 )
        {
            title = getBuffer().toString();
        }

        resetBuffer();
    }

    /**
     * @see org.apache.maven.doxia.module.xhtml.XhtmlSink#title()
     */
    public void title()
    {
    }

    public String getTitle()
    {
        return title;
    }

    /**
     * @see org.apache.maven.doxia.module.xhtml.XhtmlSink#author_()
     */
    public void author_()
    {
        if ( getBuffer().length() > 0 )
        {
            authors.add( getBuffer().toString() );
        }

        resetBuffer();
    }

    public List getAuthors()
    {
        return authors;
    }

    /**
     * @see org.apache.maven.doxia.module.xhtml.XhtmlSink#date_()
     */
    public void date_()
    {
        if ( getBuffer().length() > 0 )
        {
            date = getBuffer().toString();
        }

        resetBuffer();
    }

    public String getDate()
    {
        return date;
    }

    /**
     * @see org.apache.maven.doxia.module.xhtml.XhtmlSink#body_()
     */
    public void body_()
    {
    }

    /**
     * @see org.apache.maven.doxia.module.xhtml.XhtmlSink#body()
     */
    public void body()
    {
    }

    public String getBody()
    {
        return writer.toString();
    }

    /**
     * @see org.apache.maven.doxia.module.xhtml.XhtmlSink#head_()
     */
    public void head_()
    {
        setHeadFlag( false );
    }

    /**
     * @see org.apache.maven.doxia.module.xhtml.XhtmlSink#head()
     */
    public void head()
    {
        setHeadFlag( true );
    }
}
