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
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.doxia.Doxia;
import org.apache.maven.doxia.docrenderer.AbstractDocumentRenderer;
import org.apache.maven.doxia.docrenderer.DocumentRenderer;
import org.apache.maven.doxia.docrenderer.DocumentRendererContext;
import org.apache.maven.doxia.docrenderer.DocumentRendererException;
import org.apache.maven.doxia.docrenderer.pdf.PdfRenderer;
import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.doxia.document.DocumentTOCItem;
import org.apache.maven.doxia.document.io.xpp3.DocumentXpp3Writer;
import org.apache.maven.doxia.markup.HtmlMarkup;
import org.apache.maven.doxia.module.xdoc.XdocSink;
import org.apache.maven.doxia.parser.ParseException;
import org.apache.maven.doxia.parser.manager.ParserNotFoundException;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkAdapter;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.io.xpp3.DecorationXpp3Reader;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.doxia.tools.SiteToolException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.settings.Settings;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.PathTool;
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

    /**
     * The Plugin manager instance used to resolve Plugin descriptors.
     *
     * @component role="org.apache.maven.plugin.PluginManager"
     * @since 1.1
     */
    private PluginManager pluginManager;

    /**
     * Doxia.
     *
     * @component
     * @since 1.1
     */
    private Doxia doxia;

    /**
     * Factory for creating artifact objects.
     *
     * @component
     * @since 1.1
     */
    private ArtifactFactory artifactFactory;

    /**
     * Project builder.
     *
     * @component
     * @since 1.1
     */
    private MavenProjectBuilder mavenProjectBuilder;

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The Maven Project Object.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The Maven Settings.
     *
     * @parameter default-value="${settings}"
     * @required
     * @readonly
     * @since 1.1
     */
    private Settings settings;

    /**
     * The current build session instance.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     * @since 1.1
     */
    private MavenSession session;

    /**
     * Directory containing source for apt, fml and xdoc docs.
     *
     * @parameter default-value="${basedir}/src/site"
     * @required
     */
    private File siteDirectory;

    /**
     * Directory containing generating sources for apt, fml and xdoc docs.
     *
     * @parameter default-value="${project.build.directory}/generated-site"
     * @required
     * @since 1.1
     */
    private File generatedSiteDirectory;

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
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * The remote repositories where artifacts are located.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @since 1.1
     */
    private List remoteRepositories;

    /**
     * If <code>true</false>, aggregate all source documents in one pdf, otherwise generate one pdf for each
     * source document.
     *
     * @parameter default-value="true"
     */
    private boolean aggregate;

    /**
     * The current version of this plugin.
     *
     * @parameter default-value="${plugin.version}"
     * @readonly
     */
    private String pluginVersion;

    /**
     * If <code>true</false>, generate all Maven reports defined in <code>${project.reporting}</code> and append
     * them as a new entry in the Table Of Contents.
     * <b>Note</b>: Including the report generation could fail the PDF generation.
     *
     * @parameter default-value="true"
     * @since 1.1
     */
    private boolean includeReports;

    // ----------------------------------------------------------------------
    // Instance fields
    // ----------------------------------------------------------------------

    /**
     * The current document Renderer.
     * @see #implementation
     */
    private DocumentRenderer docRenderer;

    /**
     * The default locale.
     */
    private Locale defaultLocale;

    /**
     * The available locales list.
     */
    private List localesList;

    /**
     * The default decoration model.
     */
    private DecorationModel defaultDecorationModel;

    /**
     * The temp Site dir to have all site and generated-site files.
     *
     * @since 1.1
     */
    private File siteDirectoryTmp;

    /**
     * The temp Generated Site dir to have generated reports by this plugin.
     *
     * @since 1.1
     */
    private File generatedSiteDirectoryTmp;

    /**
     * The generated MavenReport list.
     *
     * @since 1.1
     */
    private List generatedMavenReports;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( "fo".equalsIgnoreCase( implementation ) )
        {
            this.docRenderer = foRenderer;
        }
        else if ( "itext".equalsIgnoreCase( implementation ) )
        {
            this.docRenderer = itextRenderer;
        }
        else
        {
            throw new MojoFailureException( "Not a valid implementation: '" + implementation
                + "'. Should be 'fo' or 'itext'." );
        }

        try
        {
            generatedPdf();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error during document generation: " + e.getMessage(), e );
        }

        try
        {
            if ( !outputDirectory.getCanonicalPath().equals( workingDirectory.getCanonicalPath() ) )
            {
                String outputName = getDocumentModel( getDefaultLocale() ).getOutputName();
                final String extension = FileUtils.getExtension( outputName );
                if ( StringUtils.isNotEmpty( extension ) )
                {
                    outputName = outputName.substring( 0, outputName.indexOf( extension ) - 1 );
                }
                final List pdfs = FileUtils.getFiles( workingDirectory, "**/" + outputName + ".pdf", null );

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
            throw new MojoExecutionException( "Error copying generated PDF: " + e.getMessage(), e );
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Generate the PDF.
     *
     * @throws MojoExecutionException if any
     * @throws IOException if any
     * @since 1.1
     */
    private void generatedPdf()
        throws MojoExecutionException, IOException
    {
        Locale.setDefault( getDefaultLocale() );

        for ( final Iterator iterator = getAvailableLocales().iterator(); iterator.hasNext(); )
        {
            final Locale locale = (Locale) iterator.next();

            final File workingDir = getWorkingDirectory( locale );

            File siteDirectoryFile = getSiteDirectoryTmp();
            if ( !locale.getLanguage().equals( getDefaultLocale().getLanguage() ) )
            {
                siteDirectoryFile = new File( getSiteDirectoryTmp(), locale.getLanguage() );
            }

            // Copy extra-resources
            copyResources( locale );

            // generate reports
            generateMavenReports( locale );

            DocumentRendererContext context = new DocumentRendererContext();
            context.put( "project", project );
            context.put( "settings", settings );
            context.put( "PathTool", new PathTool() );
            context.put( "FileUtils", new FileUtils() );
            context.put( "StringUtils", new StringUtils() );
            context.put( "i18n", i18n );

            try
            {
                // TODO use interface see DOXIASITETOOLS-30
                if ( aggregate )
                {
                    ( (AbstractDocumentRenderer) docRenderer ).render( siteDirectoryFile, workingDir,
                                                                       getDocumentModel( locale ), context );
                }
                else
                {
                    ( (AbstractDocumentRenderer) docRenderer ).render( siteDirectoryFile, workingDir, null,
                                                                       context );
                }
            }
            catch ( DocumentRendererException e )
            {
                if ( getLog().isDebugEnabled() )
                {
                    throw new MojoExecutionException( "Error during document generation: " + e.getMessage(), e );
                }

                throw new MojoExecutionException( "Error during document generation: " + e.getMessage() );
            }
        }
    }

    /**
     * @return the default tmpSiteDirectory.
     * @throws IOException if any
     * @since 1.1
     */
    private File getSiteDirectoryTmp()
        throws IOException
    {
        if ( this.siteDirectoryTmp == null )
        {
            final File tmpSiteDir = new File( workingDirectory, "site.tmp" );
            prepareTempSiteDirectory( tmpSiteDir );

            this.siteDirectoryTmp = tmpSiteDir;
        }

        return this.siteDirectoryTmp;
    }

    /**
     * @return the default tmpGeneratedSiteDirectory when report will be created.
     * @since 1.1
     */
    private File getGeneratedSiteDirectoryTmp()
    {
        if ( this.generatedSiteDirectoryTmp == null )
        {
            this.generatedSiteDirectoryTmp = new File( workingDirectory, "generated-site.tmp" );
        }

        return this.generatedSiteDirectoryTmp;
    }

    /**
     * Copy all site and generated-site files in the tmpSiteDirectory.
     * <br/>
     * <b>Note</b>: ignore copying of <code>generated-site</code> files if they already exist in the
     * <code>site</code> dir.
     *
     * @param tmpSiteDir not null
     * @throws IOException if any
     * @since 1.1
     */
    private void prepareTempSiteDirectory( final File tmpSiteDir )
        throws IOException
    {
        // safety
        tmpSiteDir.mkdirs();

        // copy site
        FileUtils.copyDirectoryStructure( siteDirectory, tmpSiteDir );
        // Remove SCM files
        List files =
            FileUtils.getFileAndDirectoryNames( tmpSiteDir, FileUtils.getDefaultExcludesAsString(), null, true,
                                                true, true, true );
        for ( final Iterator it = files.iterator(); it.hasNext(); )
        {
            final File file = new File( it.next().toString() );

            if ( file.isDirectory() )
            {
                FileUtils.deleteDirectory( file );
            }
            else
            {
                file.delete();
            }
        }

        copySiteDir( generatedSiteDirectory, tmpSiteDir );
    }

    /**
     * Copy the from site dir to the to dir.
     *
     * @param from not null
     * @param to not null
     * @throws IOException if any
     * @since 1.1
     */
    private void copySiteDir( final File from, final File to )
        throws IOException
    {
        if ( from == null || !from.exists() )
        {
            return;
        }

        // copy generated-site
        for ( final Iterator iterator = getAvailableLocales().iterator(); iterator.hasNext(); )
        {
            final Locale locale = (Locale) iterator.next();

            String excludes = getDefaultExcludesWithLocales( getAvailableLocales(), getDefaultLocale() );
            List siteFiles = FileUtils.getFileNames( siteDirectory, "**/*", excludes, false );
            if ( !locale.getLanguage().equals( getDefaultLocale().getLanguage() ) )
            {
                siteFiles =
                    FileUtils.getFileNames( new File( siteDirectory, locale.getLanguage() ), "**/*", excludes,
                                            false );
            }

            List generatedSiteFiles = FileUtils.getFileNames( from, "**/*", excludes, false );
            if ( !locale.getLanguage().equals( getDefaultLocale().getLanguage() ) )
            {
                generatedSiteFiles =
                    FileUtils.getFileNames( new File( from, locale.getLanguage() ), "**/*", excludes, false );
            }

            for ( final Iterator it = generatedSiteFiles.iterator(); it.hasNext(); )
            {
                final String generatedSiteFile = it.next().toString();

                if ( siteFiles.contains( generatedSiteFile ) )
                {
                    getLog().warn(
                                   "Generated-site already contains a file in site: " + generatedSiteFile
                                       + ". Ignoring copying it!" );
                    continue;
                }

                if ( !locale.getLanguage().equals( getDefaultLocale().getLanguage() ) )
                {
                    File in = new File( new File( from, locale.getLanguage() ), generatedSiteFile );
                    File out = new File( new File( to, locale.getLanguage() ), generatedSiteFile );
                    out.getParentFile().mkdirs();
                    FileUtils.copyFile( in, out );
                }
                else
                {
                    File in = new File( from, generatedSiteFile );
                    File out = new File( to, generatedSiteFile );
                    out.getParentFile().mkdirs();
                    FileUtils.copyFile( in, out );
                }
            }
        }
    }

    /**
     * Constructs a DocumentModel for the current project. The model is either read from
     * a descriptor file, if it exists, or constructed from information in the pom and site.xml.
     *
     * @param locale not null
     * @return DocumentModel.
     * @throws MojoExecutionException if any
     * @see #appendGeneratedReports(DocumentModel, Locale)
     */
    private DocumentModel getDocumentModel( Locale locale )
        throws MojoExecutionException
    {
        if ( docDescriptor.exists() )
        {
            DocumentModel doc = getDocumentModelFromDescriptor( locale );
            // TODO: descriptor model should get merged into default model, see MODELLO-63

            appendGeneratedReports( doc, locale );

            return doc;
        }

        DocumentModel model = new DocumentModelBuilder( project, getDefaultDecorationModel() ).getDocumentModel();

        model.getMeta().setGenerator( getDefaultGenerator() );
        model.getMeta().setLanguage( locale.getLanguage() );
        model.getCover().setCoverType( i18n.getString( "pdf-plugin", getDefaultLocale(), "toc.type" ) );
        model.getToc().setName( i18n.getString( "pdf-plugin", getDefaultLocale(), "toc.title" ) );

        appendGeneratedReports( model, locale );

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
            model =
                new DocumentDescriptorReader( project, getLog() ).readAndFilterDocumentDescriptor( docDescriptor );
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
        if ( locale.getLanguage().equals( getDefaultLocale().getLanguage() ) )
        {
            return workingDirectory;
        }

        return new File( workingDirectory, locale.getLanguage() );
    }

    /**
     * @return the default locale from <code>siteTool</code>.
     * @see #getAvailableLocales()
     */
    private Locale getDefaultLocale()
    {
        if ( this.defaultLocale == null )
        {
            this.defaultLocale = (Locale) getAvailableLocales().get( 0 );
        }

        return this.defaultLocale;
    }

    /**
     * @return the available locales from <code>siteTool</code>.
     * @see SiteTool#getAvailableLocales(String)
     */
    private List getAvailableLocales()
    {
        if ( this.localesList == null )
        {
            this.localesList = siteTool.getAvailableLocales( locales );
        }

        return this.localesList;
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
            final String relativePath =
                siteTool.getRelativePath( siteDirectory.getAbsolutePath(), basedir.getAbsolutePath() );

            final File descriptorFile = siteTool.getSiteDescriptorFromBasedir( relativePath, basedir, locale );
            DecorationModel decoration = null;

            if ( descriptorFile.exists() )
            {
                XmlStreamReader reader = null;
                try
                {
                    reader = ReaderFactory.newXmlReader( descriptorFile );
                    String enc = reader.getEncoding();

                    String siteDescriptorContent = IOUtil.toString( reader );
                    siteDescriptorContent =
                        siteTool.getInterpolatedSiteDescriptorContent( new HashMap(), project,
                                                                       siteDescriptorContent, enc, enc );

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

    /**
     * Generate all Maven reports defined in <code>${project.reporting}</code> part only if <code>generateReports</code> is
     * enabled.
     *
     * @param locale not null
     * @throws MojoExecutionException if any
     * @throws IOException if any
     * @since 1.1
     */
    private void generateMavenReports( Locale locale )
        throws MojoExecutionException, IOException
    {
        if ( !includeReports )
        {
            getLog().info( "Skipped report generation." );
            return;
        }

        if ( project.getReporting() == null )
        {
            getLog().info( "No report was specified." );
            return;
        }

        for ( final Iterator it = project.getReporting().getPlugins().iterator(); it.hasNext(); )
        {
            final ReportPlugin reportPlugin = (ReportPlugin) it.next();

            final PluginDescriptor pluginDescriptor = getPluginDescriptor( reportPlugin );

            List goals = new ArrayList();
            for ( final Iterator it2 = reportPlugin.getReportSets().iterator(); it2.hasNext(); )
            {
                final ReportSet reportSet = (ReportSet) it2.next();

                for ( final Iterator it3 = reportSet.getReports().iterator(); it3.hasNext(); )
                {
                    goals.add( it3.next().toString() );
                }
            }

            List mojoDescriptors = pluginDescriptor.getMojos();
            for ( final Iterator it2 = mojoDescriptors.iterator(); it2.hasNext(); )
            {
                final MojoDescriptor mojoDescriptor = (MojoDescriptor) it2.next();

                if ( goals.isEmpty() || ( !goals.isEmpty() && goals.contains( mojoDescriptor.getGoal() ) ) )
                {
                    MavenReport report = getMavenReport( mojoDescriptor );

                    generateMavenReport( mojoDescriptor, report, locale );
                }
            }
        }

        // generate project-info report
        if ( !getGeneratedMavenReports().isEmpty() )
        {
            File outDir = new File( getGeneratedSiteDirectoryTmp(), "xdoc" );
            if ( !locale.getLanguage().equals( defaultLocale.getLanguage() ) )
            {
                outDir = new File( new File( getGeneratedSiteDirectoryTmp(), locale.getLanguage() ), "xdoc" );
            }
            outDir.mkdirs();

            File piReport = new File( outDir, "project-info.xml" );

            StringWriter sw = new StringWriter();

            PdfSink sink = new PdfSink( sw );
            ProjectInfoRenderer r = new ProjectInfoRenderer( sink, getGeneratedMavenReports(), i18n, locale );
            r.render();

            writeGeneratedReport( sw.toString(), piReport );
        }

        // copy generated site
        copySiteDir( getGeneratedSiteDirectoryTmp(), getSiteDirectoryTmp() );
        copySiteDir( generatedSiteDirectory, getSiteDirectoryTmp() );
    }

    /**
     * @param reportPlugin not null
     * @return the PluginDescriptor instance for the given reportPlugin.
     * @throws MojoExecutionException if any
     * @since 1.1
     */
    private PluginDescriptor getPluginDescriptor( ReportPlugin reportPlugin )
        throws MojoExecutionException
    {
        try
        {
            return pluginManager.verifyReportPlugin( reportPlugin, project, session );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "ArtifactResolutionException: " + e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "ArtifactNotFoundException: " + e.getMessage(), e );
        }
        catch ( PluginNotFoundException e )
        {
            throw new MojoExecutionException( "PluginNotFoundException: " + e.getMessage(), e );
        }
        catch ( PluginVersionResolutionException e )
        {
            throw new MojoExecutionException( "PluginVersionResolutionException: " + e.getMessage(), e );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new MojoExecutionException( "InvalidVersionSpecificationException: " + e.getMessage(), e );
        }
        catch ( InvalidPluginException e )
        {
            throw new MojoExecutionException( "InvalidPluginException: " + e.getMessage(), e );
        }
        catch ( PluginManagerException e )
        {
            throw new MojoExecutionException( "PluginManagerException: " + e.getMessage(), e );
        }
        catch ( PluginVersionNotFoundException e )
        {
            throw new MojoExecutionException( "PluginVersionNotFoundException: " + e.getMessage(), e );
        }
    }

    /**
     * @param mojoDescriptor not null
     * @return the MavenReport instance for the given mojoDescriptor.
     * @throws MojoExecutionException if any
     * @since 1.1
     */
    private MavenReport getMavenReport( MojoDescriptor mojoDescriptor )
        throws MojoExecutionException
    {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(
                                                          mojoDescriptor.getPluginDescriptor().getClassRealm()
                                                                        .getClassLoader() );

            MojoExecution mojoExecution = new MojoExecution( mojoDescriptor );

            return pluginManager.getReport( project, mojoExecution, session );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "ArtifactNotFoundException: " + e.getMessage(), e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "ArtifactResolutionException: " + e.getMessage(), e );
        }
        catch ( PluginConfigurationException e )
        {
            throw new MojoExecutionException( "PluginConfigurationException: " + e.getMessage(), e );
        }
        catch ( PluginManagerException e )
        {
            throw new MojoExecutionException( "PluginManagerException: " + e.getMessage(), e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldClassLoader );
        }
    }

    /**
     * Generate the given Maven report only if it is not an external report and the report could be generated.
     *
     * @param mojoDescriptor not null, to catch linkage error
     * @param report could be null
     * @param locale not null
     * @throws IOException if any
     * @throws MojoExecutionException if any
     * @see #isValidGeneratedReport(MojoDescriptor, File, String)
     * @since 1.1
     */
    private void generateMavenReport( MojoDescriptor mojoDescriptor, MavenReport report, Locale locale )
        throws IOException, MojoExecutionException
    {
        if ( report == null )
        {
            return;
        }

        String localReportName = report.getName( locale );
        if ( !report.canGenerateReport() )
        {
            getLog().info(
                          "Skipped \"" + localReportName + "\" report, canGenerateReport() was false." );
            return;
        }
        if ( report.isExternalReport() )
        {
            getLog().info(
                          "Skipped external report, \"" + localReportName + "\" report." );
            return;
        }

        for ( final Iterator it = getGeneratedMavenReports().iterator(); it.hasNext(); )
        {
            MavenReport generatedReport = (MavenReport) it.next();
            if ( report.getName( locale ).equals( generatedReport.getName( locale ) ) )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( report.getName( locale ) + " was already generated." );
                }
                return;
            }
        }

        File outDir = new File( getGeneratedSiteDirectoryTmp(), "xdoc" );
        if ( !locale.getLanguage().equals( defaultLocale.getLanguage() ) )
        {
            outDir = new File( new File( getGeneratedSiteDirectoryTmp(), locale.getLanguage() ), "xdoc" );
        }
        outDir.mkdirs();

        File generatedReport = new File( outDir, report.getOutputName() + ".xml" );

        String excludes = getDefaultExcludesWithLocales( getAvailableLocales(), getDefaultLocale() );
        List files = FileUtils.getFileNames( siteDirectory, "*/" + report.getOutputName() + ".*", excludes, false );
        if ( !locale.getLanguage().equals( defaultLocale.getLanguage() ) )
        {
            files =
                FileUtils.getFileNames( new File( siteDirectory, locale.getLanguage() ), "*/"
                    + report.getOutputName() + ".*", excludes, false );
        }

        if ( files.size() != 0 )
        {
            String displayLanguage = locale.getDisplayLanguage( Locale.ENGLISH );

            getLog().info(
                           "Skipped \"" + report.getName( locale ) + "\" report, file \"" + report.getOutputName()
                               + "\" already exists for the " + displayLanguage + " version." );
            return;
        }

        getLog().info( "Generating \"" + localReportName + "\" report." );

        StringWriter sw = new StringWriter();

        PdfSink sink = new PdfSink( sw );
        try
        {
            report.generate( sink, locale );
        }
        catch ( MavenReportException e )
        {
            throw new MojoExecutionException( "MavenReportException: " + e.getMessage(), e );
        }
        catch ( LinkageError e )
        {
            if ( getLog().isErrorEnabled() )
            {
                getLog().error(
                                report.getClass().getName() + "#generate(...) caused a linkage error ("
                                    + e.getClass().getName() + ") and may be out-of-date. Check the realms:" );

                ClassRealm reportPluginRealm = mojoDescriptor.getPluginDescriptor().getClassRealm();
                StringBuilder sb = new StringBuilder();
                sb.append( "Maven Report Plugin realm = " + reportPluginRealm.getId() ).append( '\n' );
                for ( int i = 0; i < reportPluginRealm.getConstituents().length; i++ )
                {
                    sb.append( "urls[" + i + "] = " + reportPluginRealm.getConstituents()[i] );
                    if ( i != ( reportPluginRealm.getConstituents().length - 1 ) )
                    {
                        sb.append( '\n' );
                    }
                }
                getLog().error( sb.toString() );
            }

            throw e;
        }
        finally
        {
            sink.close();
        }

        writeGeneratedReport( sw.toString(), generatedReport );

        if ( isValidGeneratedReport( mojoDescriptor, generatedReport, localReportName ) )
        {
            getGeneratedMavenReports().add( report );
        }
    }

    /**
     * @return the generated reports
     * @see #generateMavenReport(MojoDescriptor, MavenReport, Locale)
     * @see #isValidGeneratedReport(MojoDescriptor, File, String)
     * @since 1.1
     */
    private List getGeneratedMavenReports()
    {
        if ( this.generatedMavenReports == null )
        {
            this.generatedMavenReports = new ArrayList();
        }

        return this.generatedMavenReports;
    }

    /**
     * Append generated reports to the toc only if <code>generateReports</code> is enabled, for instance:
     * <pre>
     * &lt;item name="Project Reports" ref="/project-info"&gt;
     * &nbsp;&nbsp;&lt;item name="Project License" ref="/license" /&gt;
     * &nbsp;&nbsp;&lt;item name="Project Team" ref="/team-list" /&gt;
     * &nbsp;&nbsp;&lt;item name="Continuous Integration" ref="/integration" /&gt;
     * &nbsp;&nbsp;...
     * &lt;/item&gt;
     * </pre>
     *
     * @param model not null
     * @param locale not null
     * @see #generateMavenReports(Locale)
     * @since 1.1
     */
    private void appendGeneratedReports( DocumentModel model, Locale locale )
    {
        if ( !includeReports )
        {
            return;
        }
        if ( getGeneratedMavenReports().isEmpty() )
        {
            return;
        }

        final DocumentTOCItem documentTOCItem = new DocumentTOCItem();
        documentTOCItem.setName( i18n.getString( "pdf-plugin", getDefaultLocale(), "toc.project-info.item" ) );
        documentTOCItem.setRef( "/project-info" ); // see #generateMavenReports(Locale)

        List addedRef = new ArrayList();

        List items = new ArrayList();

        // append generated report defined as MavenReport
        for ( final Iterator it = getGeneratedMavenReports().iterator(); it.hasNext(); )
        {
            final MavenReport report = (MavenReport) it.next();

            final DocumentTOCItem reportItem = new DocumentTOCItem();
            reportItem.setName( report.getName( locale ) );
            reportItem.setRef( "/" + report.getOutputName() );

            items.add( reportItem );

            addedRef.add( report.getOutputName() );
        }

        // append all generated reports from generated-site
        try
        {
            if ( generatedSiteDirectory.exists() )
            {
                String excludes = getDefaultExcludesWithLocales( getAvailableLocales(), getDefaultLocale() );
                List generatedDirs = FileUtils.getDirectoryNames( generatedSiteDirectory, "*", excludes, true );
                if ( !locale.getLanguage().equals( getDefaultLocale().getLanguage() ) )
                {
                    generatedDirs =
                        FileUtils.getFileNames( new File( generatedSiteDirectory, locale.getLanguage() ), "*",
                                                excludes, true );
                }

                for ( final Iterator it = generatedDirs.iterator(); it.hasNext(); )
                {
                    final String generatedDir = it.next().toString();

                    List generatedFiles = FileUtils.getFileNames( new File( generatedDir ), "**.*", excludes, false );

                    for ( final Iterator it2 = generatedFiles.iterator(); it2.hasNext(); )
                    {
                        final String generatedFile = it2.next().toString();
                        final String ref = generatedFile.substring( 0, generatedFile.lastIndexOf( "." ) );

                        if ( !addedRef.contains( ref ) )
                        {
                            final String title = getGeneratedDocumentTitle( new File( generatedDir, generatedFile ) );

                            if ( title != null )
                            {
                                final DocumentTOCItem reportItem = new DocumentTOCItem();
                                reportItem.setName( title );
                                reportItem.setRef( "/" + ref );

                                items.add( reportItem );
                            }
                        }
                    }
                }
            }
        }
        catch ( IOException e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().error( "IOException: " + e.getMessage(), e );
            }
            else
            {
                getLog().error( "IOException: " + e.getMessage() );
            }
        }

        // append to Toc
        documentTOCItem.setItems( items );
        model.getToc().addItem( documentTOCItem );
    }

    /**
     * Parse a generated Doxia file and returns its title.
     *
     * @param f not null
     * @return the xdoc file title or null if an error occurs.
     * @throws IOException if any
     * @see TitleSink
     * @since 1.1
     */
    private String getGeneratedDocumentTitle( File f )
        throws IOException
    {
        TitleSink titleSink = new TitleSink();

        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( f );

            doxia.parse( reader, f.getParentFile().getName(), titleSink );
        }
        catch ( ParseException e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().error( "ParseException: " + e.getMessage(), e );
            }
            else
            {
                getLog().error( "ParseException: " + e.getMessage() );
            }
            return null;
        }
        catch ( ParserNotFoundException e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().error( "ParserNotFoundException: " + e.getMessage(), e );
            }
            else
            {
                getLog().error( "ParserNotFoundException: " + e.getMessage() );
            }
            return null;
        }
        finally
        {
            IOUtil.close( reader );
        }

        return titleSink.getTitle();
    }

    /**
     * Parsing the generated report to see if it is correct or not. Log the error for the user.
     *
     * @param mojoDescriptor not null
     * @param generatedReport not null
     * @param localReportName not null
     * @return <code>true</code> if Doxia is able to parse the generated report, <code>false</code> otherwise.
     * @since 1.1
     */
    private boolean isValidGeneratedReport( MojoDescriptor mojoDescriptor, File generatedReport,
                                            String localReportName )
    {
        SinkAdapter sinkAdapter = new SinkAdapter();
        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( generatedReport );

            doxia.parse( reader, generatedReport.getParentFile().getName(), sinkAdapter );
        }
        catch ( ParseException e )
        {
            StringBuffer sb = new StringBuffer();

            sb.append( "\n" ).append( "\n" );
            sb.append( "Error when parsing the generated report: " ).append( generatedReport.getAbsolutePath() );
            sb.append( "\n" );
            sb.append( e.getMessage() );
            sb.append( "\n" ).append( "\n" );

            sb.append( "You could:\n" );
            sb.append( "  * exlcude all reports using -DincludeReports=false\n" );
            sb.append( "  * remove the " );
            sb.append( mojoDescriptor.getPluginDescriptor().getGroupId() );
            sb.append( ":" );
            sb.append( mojoDescriptor.getPluginDescriptor().getArtifactId() );
            sb.append( ":" );
            sb.append( mojoDescriptor.getPluginDescriptor().getVersion() );
            sb.append( " from the <reporting/> part. To not affect the site generation, " );
            sb.append( "you could create a PDF profile.\n" );
            sb.append( "\n" );

            MavenProject pluginProject = getReportPluginProject( mojoDescriptor.getPluginDescriptor() );

            if ( pluginProject == null )
            {
                sb.append( "You could also have to contact the Plugin team.\n" );
            }
            else
            {
                sb.append( "You could also have to contact the Plugin team:\n" );
                if ( pluginProject.getMailingLists() != null && pluginProject.getMailingLists().size() != 0 )
                {
                    boolean appended = false;
                    for ( Iterator i = pluginProject.getMailingLists().iterator(); i.hasNext(); )
                    {
                        MailingList mailingList = (MailingList) i.next();

                        if ( StringUtils.isNotEmpty( mailingList.getName() )
                            && StringUtils.isNotEmpty( mailingList.getPost() ) )
                        {
                            if ( !appended )
                            {
                                sb.append( "  Mailing Lists:\n" );
                                appended = true;
                            }
                            sb.append( "    " ).append( mailingList.getName() );
                            sb.append( ": " ).append( mailingList.getPost() );
                            sb.append( "\n" );
                        }
                    }
                }
                if ( StringUtils.isNotEmpty( pluginProject.getUrl() ) )
                {
                    sb.append( "  Web Site:\n" );
                    sb.append( "    " ).append( pluginProject.getUrl() );
                    sb.append( "\n" );
                }
                if ( pluginProject.getIssueManagement() != null
                    && StringUtils.isNotEmpty( pluginProject.getIssueManagement().getUrl() ) )
                {
                    sb.append( "  Issue Tracking:\n" );
                    sb.append( "    " ).append( pluginProject.getIssueManagement().getUrl() );
                    sb.append( "\n" );
                }
            }

            sb.append( "\nIgnoring the \"" + localReportName + "\" report.\n" );

            if ( getLog().isDebugEnabled() )
            {
                getLog().error( sb.toString(), e );
            }
            else
            {
                getLog().error( sb.toString() );
            }

            return false;
        }
        catch ( ParserNotFoundException e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().error( "ParserNotFoundException: " + e.getMessage(), e );
            }
            else
            {
                getLog().error( "ParserNotFoundException: " + e.getMessage() );
            }

            return false;
        }
        catch ( IOException e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().error( "IOException: " + e.getMessage(), e );
            }
            else
            {
                getLog().error( "IOException: " + e.getMessage() );
            }

            return false;
        }
        finally
        {
            IOUtil.close( reader );
        }

        return true;
    }

    /**
     * @param pluginDescriptor not null
     * @return the MavenProject for the current plugin descriptor or null if an error occurred.
     * @since 1.1
     */
    private MavenProject getReportPluginProject( PluginDescriptor pluginDescriptor )
    {
        Artifact artifact =
            artifactFactory.createProjectArtifact( pluginDescriptor.getGroupId(),
                                                   pluginDescriptor.getArtifactId(),
                                                   pluginDescriptor.getVersion(), Artifact.SCOPE_COMPILE );
        try
        {
            return mavenProjectBuilder.buildFromRepository( artifact, remoteRepositories, localRepository );
        }
        catch ( ProjectBuildingException e )
        {
            getLog().debug( e.getMessage(), e );
        }

        return null;
    }

    // ----------------------------------------------------------------------
    // static methods
    // ----------------------------------------------------------------------

    /**
     * Write the given content to the given file.
     * <br/>
     * <b>Note</b>: try also to fix the content due to some issues in the {@link AbstractMavenReport}.
     *
     * @param content the given content
     * @param toFile the report file
     * @throws IOException if any
     * @since 1.1
     */
    private static void writeGeneratedReport( String content, File toFile )
        throws IOException
    {
        if ( StringUtils.isEmpty( content ) )
        {
            return;
        }

        content = StringUtils.replace( content, "<table><table", "<table" );
        Writer writer = null;
        try
        {
            writer = WriterFactory.newXmlWriter( toFile );
            writer.write( content );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    /**
     * @param locales the list of locales dir to exclude
     * @param defaultLocale the default locale.
     * @return the comma separated list of default excludes and locales dir.
     * @see FileUtils#getDefaultExcludesAsString()
     * @since 1.1
     */
    private static String getDefaultExcludesWithLocales( List locales, Locale defaultLocale )
    {
        String excludesLocales = FileUtils.getDefaultExcludesAsString();
        for ( final Iterator it = locales.iterator(); it.hasNext(); )
        {
            final Locale locale = (Locale) it.next();

            if ( !locale.getLanguage().equals( defaultLocale.getLanguage() ) )
            {
                excludesLocales = excludesLocales + ",**/" + locale.getLanguage() + "/*";
            }
        }

        return excludesLocales;
    }

    // ----------------------------------------------------------------------
    // Inner class
    // ----------------------------------------------------------------------

    /**
     * A sink to generate Maven report as xdoc.
     *
     * @since 1.1
     */
    private static class PdfSink
        extends XdocSink
        implements org.codehaus.doxia.sink.Sink
    {
        protected PdfSink( Writer writer )
        {
            super( writer );
        }

        /** {@inheritDoc} */
        public void table()
        {
            // workaround to fix reporting-impl issue
            writeStartTag( HtmlMarkup.TABLE );
        }
    }

    /**
     * Renderer Maven report similar to org.apache.maven.plugins.site.CategorySummaryDocumentRenderer
     *
     * @since 1.1
     */
    private static class ProjectInfoRenderer
        extends AbstractMavenReportRenderer
    {
        private final List generatedReports;

        private final I18N i18n;

        private final Locale locale;

        public ProjectInfoRenderer( Sink sink, List generatedReports, I18N i18n, Locale locale )
        {
            super( sink );

            this.generatedReports = generatedReports;
            this.i18n = i18n;
            this.locale = locale;
        }

        /** {@inheritDoc} */
        public String getTitle()
        {
            return i18n.getString( "pdf-plugin", locale, "report.project-info.title" );
        }

        /** {@inheritDoc} */
        public void renderBody()
        {
            sink.section1();
            sink.sectionTitle1();
            sink.text( i18n.getString( "pdf-plugin", locale, "report.project-info.title" ) );
            sink.sectionTitle1_();

            sink.paragraph();
            sink.text( i18n.getString( "pdf-plugin", locale, "report.project-info.description1" ) + " " );
            sink.link( "http://maven.apache.org" );
            sink.text( "Maven" );
            sink.link_();
            sink.text( " " + i18n.getString( "pdf-plugin", locale, "report.project-info.description2" ) );
            sink.paragraph_();

            sink.section2();
            sink.sectionTitle2();
            sink.text( i18n.getString( "pdf-plugin", locale, "report.project-info.sectionTitle" ) );
            sink.sectionTitle2_();

            sink.table();

            sink.tableRows( new int[] { Sink.JUSTIFY_LEFT, Sink.JUSTIFY_LEFT }, false );

            String name = i18n.getString( "pdf-plugin", locale, "report.project-info.column.document" );
            String description = i18n.getString( "pdf-plugin", locale, "report.project-info.column.description" );

            sink.tableRow();

            sink.tableHeaderCell();

            sink.text( name );

            sink.tableHeaderCell_();

            sink.tableHeaderCell();

            sink.text( description );

            sink.tableHeaderCell_();

            sink.tableRow_();

            if ( generatedReports != null )
            {
                for ( final Iterator it = generatedReports.iterator(); it.hasNext(); )
                {
                    final MavenReport report = (MavenReport) it.next();

                    sink.tableRow();
                    sink.tableCell();
                    sink.link( report.getOutputName() + ".html" );
                    sink.text( report.getName( locale ) );
                    sink.link_();
                    sink.tableCell_();
                    sink.tableCell();
                    sink.text( report.getDescription( locale ) );
                    sink.tableCell_();
                    sink.tableRow_();
                }
            }

            sink.tableRows_();

            sink.table_();

            sink.section2_();

            sink.section1_();
        }
    }

    /**
     * A Sink class to get the Document title when parsing Doxia files.
     *
     * @see PdfMojo#getGeneratedDocumentTitle(File)
     * @since 1.1
     */
    private static class TitleSink
        extends SinkAdapter
    {
        String title;

        boolean isTitle;

        /** {@inheritDoc} */
        public void title()
        {
            isTitle = true;
        }

        /** {@inheritDoc} */
        public void title_()
        {
            isTitle = false;
        }

        /** {@inheritDoc} */
        public void text( String text )
        {
            if ( isTitle )
            {
                title = text;
            }
        }

        /**
         * @return the found title or null.
         */
        public String getTitle()
        {
            return title;
        }
    }
}
