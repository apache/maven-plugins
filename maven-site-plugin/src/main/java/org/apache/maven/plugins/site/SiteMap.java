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

import java.util.List;
import java.util.Locale;

import org.apache.maven.doxia.module.xdoc.XdocSinkFactory;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.Menu;
import org.apache.maven.doxia.site.decoration.MenuItem;

import org.codehaus.plexus.i18n.I18N;

/**
 * Generate a sitemap.
 *
 * @author ltheussl
 * @version $Id$
 * @since 2.1
 */
public class SiteMap
 {

    private String encoding;
    private I18N i18n;

    /**
     * Constructor sets default values.
     *
     * @param encoding the default encoding to use when writing the output file.
     * @param i18n the default I18N for translations.
     */
    public SiteMap( String encoding, I18N i18n )
    {
        this.encoding = encoding;
        this.i18n = i18n;
    }

    /**
     * Get the value of i18n.
     *
     * @return the value of i18n.
     */
    public I18N getI18n()
    {
        return i18n;
    }

    /**
     * Set the value of i18n.
     *
     * @param i18n new value of i18n.
     */
    public void setI18n( I18N i18n )
    {
        this.i18n = i18n;
    }

    /**
     * Get the encoding to use when writing the output file.
     *
     * @return the value of encoding.
     */
    public String getEncoding()
    {
        return encoding;
    }

    /**
     * Set the encoding to use when writing the output file.
     *
     * @param enc new value of encoding.
     */
    public void setEncoding( String enc )
    {
        this.encoding = enc;
    }

    /**
     * Generates a sitemap.xml in targetDir/xdoc/.
     * This is a valid xdoc document that can be processed by a Doxia parser.
     * The file lists all the menus and menu items of the DecorationModel in expanded form.
     *
     * @param model the DecorationModel to extract the menus from.
     * @param targetDir the target output directory. The file will be created in targetDir/xdoc/.
     * @param locale the Locale for the result.
     *
     * @throws IOException if the file cannot be ceated.
     */
    public void generate( DecorationModel model, File targetDir, Locale locale )
            throws IOException
    {
        File outputDir = new File( targetDir, "xdoc" );
        Sink sink = new XdocSinkFactory().createSink( outputDir, "sitemap.xml", encoding );

        try
        {
            extract( model, sink, locale );
        }
        finally
        {
            sink.close();
        }
    }

    private void extract( DecorationModel decoration, Sink sink, Locale locale )
    {
        sink.head();
        sink.title();
        sink.text( i18n.getString( "site-plugin", locale, "site.sitemap.title" ) );
        sink.title_();
        sink.head_();
        sink.body();

        sink.section1();
        sink.sectionTitle1();
        sink.text( i18n.getString( "site-plugin", locale, "site.sitemap.section.title" ) );
        sink.sectionTitle1_();

        sink.paragraph();
        sink.text( i18n.getString( "site-plugin", locale, "site.sitemap.description" ) );
        sink.paragraph_();

        for ( Menu menu : decoration.getMenus() )
        {
            sink.section3();
            sink.sectionTitle3();
            sink.text( menu.getName() );
            sink.sectionTitle3_();
            sink.horizontalRule();

            extractItems( menu.getItems(), sink );

            sink.section3_();
        }

        sink.section1_();
        sink.body_();
    }

    private static void extractItems( List<MenuItem> items, Sink sink )
    {
        if ( items == null || items.isEmpty() )
        {
            return;
        }

        sink.list();

        for ( MenuItem item : items )
        {
            sink.listItem();
            sink.link( relativePath( item.getHref() ) );
            sink.text( item.getName() );
            sink.link_();
            extractItems( item.getItems(), sink );
            sink.listItem_();
        }

        sink.list_();
    }

    // sitemap.html gets generated into top-level so we only have to check leading slashes
    private static String relativePath( String href )
    {
        return href.startsWith( "/" ) ? "." + href : href;
    }
}
