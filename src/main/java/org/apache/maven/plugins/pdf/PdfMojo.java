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
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.maven.doxia.docrenderer.DocumentRenderer;
import org.apache.maven.doxia.docrenderer.DocumentRendererException;
import org.apache.maven.doxia.document.DocumentMeta;
import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.doxia.document.io.xpp3.DocumentXpp3Writer;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;


/**
 * Generates a PDF document for a project.
 *
 * @author ltheussl
 * @version $Id$
 *
 * @goal pdf
 */
public class PdfMojo
    extends AbstractPdfMojo
{
    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The Maven Project Object.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Directory containing source for apt, fml and xdoc docs.
     *
     * @parameter expression="${basedir}/src/site"
     * @required
     */
    private File siteDirectory;

    /**
     * Directory containing the generated project sites and report distributions.
     *
     * @parameter alias="workingDirectory" expression="${project.build.directory}/pdf"
     * @required
     */
    private File outputDirectory;

    /**
     * File that contains the DocumentModel of the PDF to generate.
     *
     * @parameter expression="src/site/pdf.xml"
     */
    private File docDescriptor;

    /**
     * Identifies the framework to use for pdf generation: either "fo" (default) or "itext".
     *
     * @parameter expression="fo"
     * @required
     */
    private String implementation = "fo";

    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * FO Document Renderer.
     *
     * @component role-hint="fo"
     */
    private DocumentRenderer foRenderer;

    /**
     * IText Document Renderer.
     *
     * @component role-hint="itext"
     */
    private DocumentRenderer itextRenderer;

    /**
     * Document Renderer.
     */
    private DocumentRenderer docRenderer;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( "fo".equals( implementation ) )
        {
            this.docRenderer = foRenderer;
        }
        else if ( "itext".equals( implementation ) )
        {
            this.docRenderer = itextRenderer;
        }
        else
        {
            throw new MojoFailureException( "Not a valid implementation: " + implementation );
        }

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

                File siteDirectoryFile = siteDirectory;

                if ( !locale.getLanguage().equals( defaultLocale.getLanguage() ) )
                {
                    siteDirectoryFile = new File( siteDirectory, locale.getLanguage() );
                }

                docRenderer.render( siteDirectoryFile, outputDir, getDocumentModel() );
            }
        }
        catch ( DocumentRendererException e )
        {
            throw new MojoExecutionException( "Error during document generation", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error during document generation", e );
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Constructs a DocumentModel for the current project. The model is either read from
     * a descriptor file, if it exists, or constructed from information in the pom and site.xml.
     *
     * @return DocumentModel.
     * @throws DocumentRendererException if any.
     * @throws IOException if any.
     */
    private DocumentModel getDocumentModel()
        throws DocumentRendererException, IOException
    {
        if ( docDescriptor.exists() )
        {
            return docRenderer.readDocumentModel( docDescriptor );
        }

        return generateDefaultDocDescriptor();
    }

    /**
     * Return the output directory for a given Locale and the current default Locale.
     *
     * @param locale a Locale.
     * @param defaultLocale the current default Locale.
     * @return File.
     * @todo can be re-used
     */
    private File getOutputDirectory( Locale locale, Locale defaultLocale )
    {
        if ( locale.getLanguage().equals( defaultLocale.getLanguage() ) )
        {
            return outputDirectory;
        }

        return new File( outputDirectory, locale.getLanguage() );
    }

    /**
     * @return Generate a default document descriptor from the Maven project
     * @throws IOException if any
     */
    private DocumentModel generateDefaultDocDescriptor()
        throws IOException
    {
        File outputDir = new File( project.getBuild().getDirectory(), "pdf" );
        if ( outputDir.exists() && outputDir.isFile() )
        {
            throw new IOException( "The file '" + outputDir + "' should be a dir." );
        }
        if ( !outputDir.exists() )
        {
            outputDir.mkdirs();
        }

        DocumentMeta meta = new DocumentMeta(); // TODO Improve metadata
        meta.setAuthor( ( project.getOrganization() != null
            && StringUtils.isNotEmpty( project.getOrganization().getName() ) ? project.getOrganization().getName()
                                                                            : System.getProperty( "user.name" ) ) );
        meta.setTitle( ( StringUtils.isEmpty( project.getName() ) ? project.getGroupId() + ":"
            + project.getArtifactId() : project.getName() ) );

        DocumentModel docModel = new DocumentModel();
        docModel.setModelEncoding( ( StringUtils.isEmpty( project.getModel().getModelEncoding() ) ? "UTF-8" : project
            .getModel().getModelEncoding() ) );
        docModel.setOutputName( project.getArtifactId() + "-doc" );
        docModel.setMeta( meta );

        if ( getLog().isDebugEnabled() )
        {
            File doc = FileUtils.createTempFile( "pdf", ".xml", outputDir );

            getLog().debug( "Generated a default document model: " + doc.getAbsolutePath() );

            DocumentXpp3Writer xpp3 = new DocumentXpp3Writer();
            Writer w = null;
            try
            {
                w = WriterFactory.newPlatformWriter( doc );
                xpp3.write( w, docModel );
            }
            finally
            {
                IOUtil.close( w );
            }
        }

        return docModel;
    }
}
