package org.apache.maven.plugin.pmd;

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
import java.io.Writer;
import java.util.Locale;

import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.5
 */
public abstract class AbstractPmdReportTest
    extends AbstractMojoTestCase
{
    /**
     * Renderer the sink from the report mojo.
     *
     * @param mojo not null
     * @param outputHtml not null
     * @throws RendererException if any
     * @throws IOException if any
     */
    protected void renderer( AbstractPmdReport mojo, File outputHtml )
        throws RendererException, IOException
    {
        Writer writer = null;
        SiteRenderingContext context = new SiteRenderingContext();
        context.setDecoration( new DecorationModel() );
        context.setTemplateName( "org/apache/maven/doxia/siterenderer/resources/default-site.vm" );
        context.setLocale( Locale.ENGLISH );

        try
        {
            outputHtml.getParentFile().mkdirs();
            writer = WriterFactory.newXmlWriter( outputHtml );

            mojo.getSiteRenderer().generateDocument( writer, (SiteRendererSink) mojo.getSink(), context );

            writer.close();
            writer = null;
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    /**
     * Checks, whether the string <code>contained</code> is contained in
     * the given <code>text</code> ignoring case.
     *
     * @param text the string in which the search is executed
     * @param contained the string, the should be searched
     * @return <code>true</code> if the string is contained, otherwise <code>false</code>.
     */
    public static boolean lowerCaseContains( String text, String contains )
    {
        return text.toLowerCase( Locale.ROOT ).contains( contains.toLowerCase( Locale.ROOT ) );
    }
}
