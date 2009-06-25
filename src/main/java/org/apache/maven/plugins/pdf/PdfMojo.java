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
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.docrenderer.DocumentRenderer;
import org.apache.maven.doxia.docrenderer.DocumentRendererException;
import org.apache.maven.doxia.docrenderer.pdf.PdfRenderer;
import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.doxia.document.io.xpp3.DocumentXpp3Writer;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.io.xpp3.DecorationXpp3Reader;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.doxia.tools.SiteToolException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Generates a PDF document for a project.
 *
 * @author ltheussl
 * @version $Id$
 * @goal pdf
 */
public class PdfMojo
    extends AbstractMojo
{
    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * FO Document Renderer.
     *
     * @component role-hint="fo"
     */
    private PdfRenderer foRenderer;

    /**
     * Internationalization.
     *
     * @component
     */
    private I18N i18n;

    /**
     * IText Document Renderer.
     *
     * @component role-hint="itext"
     */
    private PdfRenderer itextRenderer;

    /**
     * A comma separated list of locales supported by Maven.
     * The first valid token will be the default Locale for this instance of the Java Virtual Machine.
     *
     * @parameter expression="${locales}"
     */
    private String locales;

    /**
     * Site renderer.
     *
     * @component
     */
    private Renderer siteRenderer;

    /**
     * SiteTool.
     *
     * @component
     */
    private SiteTool siteTool;

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
     * @parameter default-value="${basedir}/src/site"
     * @required
     */
    private File siteDirectory;

    /**
     * Output directory where PDF files should be created.
     *
     * @parameter default-value="${project.build.directory}/pdf"
     * @required
     */
    private File outputDirectory;

    /**
     * Working directory for working files like temp files/resources.
     *
     * @parameter default-value="${project.build.directory}/pdf"
     * @required
     */
    private File workingDirectory;

    /**
     * File that contains the DocumentModel of the PDF to generate.
     *
     * @parameter default-value="src/site/pdf.xml"
     */
    private File docDescriptor;

    /**
     * Identifies the framework to use for pdf generation: either "fo" (default) or "itext".
     *
     * @parameter expression="${implementation}" default-value="fo"
     * @required
     */
    private String implementation;

    /**
     * The local repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     */
    private ArtifactRepository localRepository;

    /**
     * If <code>true</false>, aggregate all source documents in one pdf, otherwise generate one pdf for each
     * source document.
     *
     * @parameter default-value="true"
     */
    private boolean aggregate;

    /**
     * Document Renderer.
     */
    private DocumentRenderer docRenderer;

    /**
     * Default locale
     */
    private Locale defaultLocale;

    /**
     * Default decoration model
     */
    private DecorationModel defaultDecorationModel;

    /**
     * @parameter expression="${plugin.version}"
     */
    private String pluginVersion;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @throws MojoExecutionException if an exception ocurred during mojo execution.
     * @throws MojoFailureException if the mojo could not be executed.
     */
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
            final List localesList = siteTool.getAvailableLocales( locales );

            // Default is first in the list
            this.defaultLocale = (Locale) localesList.get( 0 );
            Locale.setDefault( defaultLocale );

            for ( final Iterator iterator = localesList.iterator(); iterator.hasNext(); )
            {
                final Locale locale = (Locale) iterator.next();

                final File workingDir = getWorkingDirectory( locale );

                File siteDirectoryFile = siteDirectory;
                if ( !locale.getLanguage().equals( defaultLocale.getLanguage() ) )
                {
                    siteDirectoryFile = new File( siteDirectory, locale.getLanguage() );
                }

                // Copy extra-resources
                copyResources( locale );

                try
                {
                    if ( aggregate )
                    {
                        docRenderer.render( siteDirectoryFile, workingDir, getDocumentModel( locale ) );
                    }
                    else
                    {
                        docRenderer.render( siteDirectoryFile, workingDir, null );
                    }
                }
                catch ( DocumentRendererException e )
                {
                    throw new MojoExecutionException( "Error during document generation", e );
                }
            }

            if ( !outputDirectory.getCanonicalPath().equals( workingDirectory.getCanonicalPath() ) )
            {
                final List pdfs = FileUtils.getFiles( workingDirectory, "**/*.pdf", null );

                for ( final Iterator it = pdfs.iterator(); it.hasNext(); )
                {
                    final File pdf = (File) it.next();

                    FileUtils.copyFile( pdf, new File( outputDirectory, pdf.getName() ) );
                    pdf.delete();
                }
            }
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
     * @param locale not null
     * @return DocumentModel.
     * @throws MojoExecutionException if any
     * @see #readAndFilterDocumentDescriptor(MavenProject, File, Log)
     */
    private DocumentModel getDocumentModel( Locale locale )
        throws MojoExecutionException
    {
        if ( docDescriptor.exists() )
        {
            DocumentModel doc = getDocumentModelFromDescriptor( locale );
            // TODO: descriptor model should get merged into default model, see MODELLO-63

            return doc;
        }

        DocumentModel model = new DocumentModelBuilder( project, getDefaultDecorationModel() ).getDocumentModel();

        model.getMeta().setGenerator( getDefaultGenerator() );
        model.getMeta().setLanguage( locale.getLanguage() );
        model.getCover().setCoverType( i18n.getString( "pdf-plugin", defaultLocale, "toc.type" ) );
        model.getToc().setName( i18n.getString( "pdf-plugin", defaultLocale, "toc.title" ) );

        debugLogGeneratedModel( model );

        return model;
    }

    /**
     * Read a DocumentModel from a file.
     *
     * @param locale used to set the language.
     * @return the DocumentModel read from the configured document descriptor.
     * @throws org.apache.maven.plugin.MojoExecutionException if the model could not be read.
     */
    private DocumentModel getDocumentModelFromDescriptor( Locale locale )
            throws MojoExecutionException
    {
        DocumentModel model = null;

        try
        {
            model = new DocumentDescriptorReader( project, getLog() ).readAndFilterDocumentDescriptor( docDescriptor );
        }
        catch ( XmlPullParserException ex )
        {
            throw new MojoExecutionException( "Error reading DocumentDescriptor!", ex );
        }
        catch ( IOException io )
        {
            throw new MojoExecutionException( "Error opening DocumentDescriptor!", io );
        }

        if ( StringUtils.isEmpty( model.getMeta().getLanguage() ) )
        {
            model.getMeta().setLanguage( locale.getLanguage() );
        }

        if ( StringUtils.isEmpty( model.getMeta().getGenerator() ) )
        {
            model.getMeta().setGenerator( getDefaultGenerator() );
        }

        return model;
    }

    /**
     * Return the working directory for a given Locale and the current default Locale.
     *
     * @param locale a Locale.
     * @return File.
     */
    private File getWorkingDirectory( Locale locale )
    {
        if ( locale.getLanguage().equals( defaultLocale.getLanguage() ) )
        {
            return workingDirectory;
        }

        return new File( workingDirectory, locale.getLanguage() );
    }

    /**
     * @return the default locale from <code>siteTool</code>.
     */
    private Locale getDefaultLocale()
    {
        if ( this.defaultLocale == null )
        {
            final List localesList = siteTool.getAvailableLocales( locales );
            this.defaultLocale = (Locale) localesList.get( 0 );
        }

        return this.defaultLocale;
    }

    /**
     * @return the DecorationModel instance from <code>site.xml</code>
     * @throws MojoExecutionException if any
     */
    private DecorationModel getDefaultDecorationModel()
        throws MojoExecutionException
    {
        if ( this.defaultDecorationModel == null )
        {
            final Locale locale = getDefaultLocale();

            final File basedir = project.getBasedir();
            final String relativePath = siteTool.getRelativePath(
                    siteDirectory.getAbsolutePath(), basedir.getAbsolutePath() );

            final File descriptorFile = siteTool.getSiteDescriptorFromBasedir( relativePath, basedir, locale );
            DecorationModel decoration = null;

            if ( descriptorFile.exists() )
            {
                XmlStreamReader reader = null;
                try
                {
                    reader = ReaderFactory.newXmlReader( descriptorFile );
                    String siteDescriptorContent = IOUtil.toString( reader );

                    siteDescriptorContent =
                        siteTool.getInterpolatedSiteDescriptorContent( new HashMap(), project, siteDescriptorContent,
                                                                       reader.getEncoding(), reader.getEncoding() );

                    decoration = new DecorationXpp3Reader().read( new StringReader( siteDescriptorContent ) );
                }
                catch ( XmlPullParserException e )
                {
                    throw new MojoExecutionException( "Error parsing site descriptor", e );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Error reading site descriptor", e );
                }
                catch ( SiteToolException e )
                {
                    throw new MojoExecutionException( "Error when interpoling site descriptor", e );
                }
                finally
                {
                    IOUtil.close( reader );
                }
            }

            this.defaultDecorationModel = decoration;
        }

        return this.defaultDecorationModel;
    }

    /**
     * Parse the decoration model to find the skin artifact and copy its resources to the output dir.
     *
     * @param locale not null
     * @throws MojoExecutionException if any
     * @see #getDefaultDecorationModel()
     */
    private void copyResources( Locale locale )
        throws MojoExecutionException
    {
        final DecorationModel decorationModel = getDefaultDecorationModel();

        if ( decorationModel == null )
        {
            return;
        }

        File skinFile;

        try
        {
            skinFile =
                siteTool.getSkinArtifactFromRepository( localRepository, project.getRemoteArtifactRepositories(),
                                                        decorationModel ).getFile();
        }
        catch ( SiteToolException e )
        {
            throw new MojoExecutionException( "SiteToolException: " + e.getMessage(), e );
        }

        if ( skinFile == null )
        {
            return;
        }

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Copy resources from skin artifact: '" + skinFile + "'..." );
        }

        try
        {
            final SiteRenderingContext context =
                siteRenderer.createContextForSkin( skinFile, new HashMap(), decorationModel, project.getName(),
                                                   locale );
            context.addSiteDirectory( new File( siteDirectory, locale.getLanguage() ) );

            for ( final Iterator i = context.getSiteDirectories().iterator(); i.hasNext(); )
            {
                final File siteDirectoryFile = (File) i.next();

                siteRenderer.copyResources( context, new File( siteDirectoryFile, "resources" ), workingDirectory );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "IOException: " + e.getMessage(), e );
        }
    }

    /**
     * Construct a default producer.
     *
     * @return A String in the form <code>Maven PDF Plugin v. 1.1.1, 'fo' implementation</code>.
     */
    private String getDefaultGenerator()
    {
        return "Maven PDF Plugin v. " + pluginVersion + ", '" + implementation + "' implementation.";
    }

    /**
     * Write the auto-generated model to disc.
     *
     * @param docModel the model to write.
     */
    private void debugLogGeneratedModel( final DocumentModel docModel )
    {
        if ( getLog().isDebugEnabled() && project != null )
        {
            final File outputDir = new File( project.getBuild().getDirectory(), "pdf" );

            if ( !outputDir.exists() )
            {
                outputDir.mkdirs();
            }

            final File doc = FileUtils.createTempFile( "pdf", ".xml", outputDir );
            final DocumentXpp3Writer xpp3 = new DocumentXpp3Writer();

            Writer w = null;

            try
            {
                w = WriterFactory.newXmlWriter( doc );
                xpp3.write( w, docModel );
                getLog().debug( "Generated a default document model: " + doc.getAbsolutePath() );
            }
            catch ( IOException io )
            {
                getLog().debug( "Failed to write document model: " + doc.getAbsolutePath(), io );
            }
            finally
            {
                IOUtil.close( w );
            }
        }
    }
}
