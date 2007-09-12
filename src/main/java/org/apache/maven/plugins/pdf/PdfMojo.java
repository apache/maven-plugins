package org.apache.maven.plugins.pdf;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.maven.doxia.docrenderer.DocRenderer;
import org.apache.maven.doxia.docrenderer.DocRendererException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.StringUtils;


/**
 * @author ltheussl
 * @version $Id$
 *
 * @goal pdf
 */
public class PdfMojo
    extends AbstractMojo
{
    /**
     * The locale by default for all default bundles
     */
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    /**
     * Directory containing source for apt, fml and xdoc docs.
     *
     * @parameter expression="${basedir}/src/site"
     * @required
     */
    protected File siteDirectory;

    /**
     * A comma separated list of locales supported by Maven. The first valid token will be the default Locale
     * for this instance of the Java Virtual Machine.
     *
     * @parameter expression="${locales}"
     */
    private String locales;

    /**
     * Internationalization.
     *
     * @component
     */
    protected I18N i18n;

    /**
     * Directory containing the generated project sites and report distributions.
     *
     * @parameter alias="workingDirectory" expression="${project.build.directory}/pdf"
     * @required
     */
    protected File outputDirectory;

    /**
     * File that contains the DocumentModel of the PDF to generate.
     *
     * @parameter expression="src/site/pdf.xml"
     * @required
     */
    protected File docDescriptor;

    /**
     * Document Renderer.
     *
     * @component
     */
    private DocRenderer docRenderer;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            List localesList = initLocalesList();

            // Default is first in the list
            Locale defaultLocale = (Locale) localesList.get( 0 );
            Locale.setDefault( defaultLocale );

            for ( Iterator iterator = localesList.iterator(); iterator.hasNext(); )
            {
                Locale locale = (Locale) iterator.next();

                File outputDirectory = getOutputDirectory( locale, defaultLocale );

                // Generate static site
                File siteDirectoryFile = siteDirectory;

                if ( !locale.getLanguage().equals( defaultLocale.getLanguage() ) )
                {
                    siteDirectoryFile = new File( siteDirectory, locale.getLanguage() );
                }

                docRenderer.render( siteDirectoryFile, outputDirectory, docDescriptor );
            }
        }
        catch ( DocRendererException e )
        {
            throw new MojoExecutionException( "Error during document generation", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error during document generation", e );
        }
    }

    // TODO: can be re-used
    private File getOutputDirectory( Locale locale, Locale defaultLocale )
    {
        File file;

        if ( locale.getLanguage().equals( defaultLocale.getLanguage() ) )
        {
            file = outputDirectory;
        }
        else
        {
            file = new File( outputDirectory, locale.getLanguage() );
        }

        return file;
    }

    /**
     * Init the <code>localesList</code> variable.
     * <p>If <code>locales</code> variable is available, the first valid token will be the <code>defaultLocale</code>
     * for this instance of the Java Virtual Machine.</p>
     *
     * @return a list of <code>Locale</code>
     * @todo that's not specific to pdf, move it somewhere to make it re-usable.
     */
    protected List initLocalesList()
    {
        List localesList = new ArrayList();
        if ( locales != null )
        {
            String[] localesArray = StringUtils.split( locales, "," );

            for ( int i = 0; i < localesArray.length; i++ )
            {
                Locale locale = codeToLocale( localesArray[i] );

                if ( locale != null )
                {
                    if ( !Arrays.asList( Locale.getAvailableLocales() ).contains( locale ) )
                    {
                        StringBuffer sb = new StringBuffer();

                        sb.append( "The locale '" ).append( locale );
                        sb.append( "' is not available in this Java Virtual Machine (" );
                        sb.append( System.getProperty( "java.version" ) ).append(  "from"  );
                        sb.append( System.getProperty( "java.vendor" ) ).append( ") - IGNORING" );

                        getLog().warn( sb.toString() );

                        continue;
                    }

                    // Default bundles are in English
                    if ( !locale.getLanguage().equals( DEFAULT_LOCALE.getLanguage() ) )
                    {
                        if ( !i18n.getBundle( "site-plugin", locale ).getLocale().getLanguage().equals( locale
                            .getLanguage() ) )
                        {
                            StringBuffer sb = new StringBuffer();

                            sb.append( "The locale '" ).append( locale ).append( "' (" );
                            sb.append( locale.getDisplayName( Locale.ENGLISH ) );
                            sb.append( ") is not supported IGNORING. " );

                            getLog().warn( sb.toString() );

                            continue;
                        }
                    }

                    localesList.add( locale );
                }
            }
        }

        if ( localesList.isEmpty() )
        {
            localesList = Collections.singletonList( DEFAULT_LOCALE );
        }

        return localesList;
    }

    /**
     * Converts a locale code like "en", "en_US" or "en_US_win" to a <code>java.util.Locale</code>
     * object.
     * <p>If localeCode = <code>default</code>, return the current value of the default locale for this instance
     * of the Java Virtual Machine.</p>
     *
     * @param localeCode the locale code string.
     * @return a java.util.Locale object instancied or null if errors occurred
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/Locale.html">java.util.Locale#getDefault()</a>
     * @todo that's not specific to pdf, move it somewhere to make it re-usable.
     */
    private Locale codeToLocale( String localeCode )
    {
        if ( localeCode == null )
        {
            return null;
        }

        if ( "default".equalsIgnoreCase( localeCode ) )
        {
            return Locale.getDefault();
        }

        String language = "";
        String country = "";
        String variant = "";

        StringTokenizer tokenizer = new StringTokenizer( localeCode, "_" );
        if ( tokenizer.countTokens() > 3 )
        {
            getLog().warn( "Invalid java.util.Locale format for '" + localeCode + "' entry - IGNORING" );
            return null;
        }

        if ( tokenizer.hasMoreTokens() )
        {
            language = tokenizer.nextToken();
            if ( tokenizer.hasMoreTokens() )
            {
                country = tokenizer.nextToken();
                if ( tokenizer.hasMoreTokens() )
                {
                    variant = tokenizer.nextToken();
                }
            }
        }

        return new Locale( language, country, variant );
    }
}
