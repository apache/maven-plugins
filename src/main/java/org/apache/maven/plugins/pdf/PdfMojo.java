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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.maven.doxia.docrenderer.DocRenderer;
import org.apache.maven.doxia.docrenderer.DocRendererException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;


/**
 * @author ltheussl
 * @version $Id$
 *
 * @goal pdf
 */
public class PdfMojo
    extends AbstractPdfMojo
{
    /**
     * Directory containing source for apt, fml and xdoc docs.
     *
     * @parameter expression="${basedir}/src/site"
     * @required
     */
    protected File siteDirectory;

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

                File outputDir = getOutputDirectory( locale, defaultLocale );

                // Generate static site
                File siteDirectoryFile = siteDirectory;

                if ( !locale.getLanguage().equals( defaultLocale.getLanguage() ) )
                {
                    siteDirectoryFile = new File( siteDirectory, locale.getLanguage() );
                }

                docRenderer.render( siteDirectoryFile, outputDir, docDescriptor );
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

}
