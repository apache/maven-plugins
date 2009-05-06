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
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.doxia.docrenderer.DocumentRenderer;
import org.apache.maven.doxia.docrenderer.DocumentRendererException;
import org.apache.maven.doxia.document.DocumentMeta;
import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.doxia.document.DocumentTOC;
import org.apache.maven.doxia.document.DocumentTOCItem;
import org.apache.maven.doxia.document.io.xpp3.DocumentXpp3Reader;
import org.apache.maven.doxia.document.io.xpp3.DocumentXpp3Writer;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.Menu;
import org.apache.maven.doxia.site.decoration.MenuItem;
import org.apache.maven.doxia.site.decoration.io.xpp3.DecorationXpp3Reader;
import org.apache.maven.doxia.tools.SiteToolException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.PathUtils;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

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

    /**
     * Default locale
     */
    private Locale defaultLocale;

    /**
     * Default decoration model
     */
    private DecorationModel defaultDecorationModel;

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
            List localesList = getSiteTool().getAvailableLocales( getLocales() );

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
     * @throws MojoExecutionException if any
     * @see #readAndFilterDocumentDescriptor(MavenProject, File, Log)
     */
    private DocumentModel getDocumentModel()
        throws DocumentRendererException, IOException, MojoExecutionException
    {
        if ( docDescriptor.exists() )
        {
            return readAndFilterDocumentDescriptor( project, docDescriptor, getLog() );
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
     * @throws MojoExecutionException if any
     */
    private DocumentModel generateDefaultDocDescriptor()
        throws IOException, MojoExecutionException
    {
        File outputDir = new File( project.getBuild().getDirectory(), "pdf" );

        if ( outputDir.isFile() )
        {
            throw new IOException( outputDir + " is not a directory!" );
        }

        if ( !outputDir.exists() )
        {
            outputDir.mkdirs();
        }

        // TODO Improve metadata
        DocumentMeta meta = new DocumentMeta();
        meta.setAuthor( getDocumentAuthor() );
        meta.setTitle( getDocumentTitle() );

        DocumentModel docModel = new DocumentModel();
        docModel.setModelEncoding( getModelEncoding() );
        docModel.setOutputName( project.getArtifactId() );
        docModel.setMeta( meta );

        // Populate docModel from defaultDecirationModel
        DecorationModel decorationModel = getDefaultDecorationModel();
        if ( decorationModel != null )
        {
            DocumentTOC toc = new DocumentTOC();

            toc.setName( getI18n().getString( "pdf-plugin", getDefaultLocale(), "toc.title" ) );
            for ( Iterator it = decorationModel.getMenus().iterator(); it.hasNext(); )
            {
                Menu menu = (Menu) it.next();

                for ( Iterator it2 = menu.getItems().iterator(); it2.hasNext(); )
                {
                    MenuItem item = (MenuItem) it2.next();

                    DocumentTOCItem documentTOCItem = new DocumentTOCItem();
                    documentTOCItem.setName( item.getName() );
                    documentTOCItem.setRef( item.getHref() );
                    toc.addItem( documentTOCItem );
                }
            }

            docModel.setToc( toc );
        }

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

    private String getDocumentAuthor()
    {
        return ( project.getOrganization() != null
            && StringUtils.isNotEmpty( project.getOrganization().getName() )
                ? project.getOrganization().getName()
                : System.getProperty( "user.name" ) );
    }

    private String getDocumentTitle()
    {
        return ( StringUtils.isEmpty( project.getName() )
                ? project.getGroupId() + ":" + project.getArtifactId()
                : project.getName() );
    }

    private String getModelEncoding()
    {
        return ( StringUtils.isEmpty( project.getModel().getModelEncoding() )
                ? "UTF-8"
                : project.getModel().getModelEncoding() );
    }

    /**
     * @return the default locale from <code>siteTool</code>.
     */
    private Locale getDefaultLocale()
    {
        if ( this.defaultLocale == null )
        {
            List localesList = getSiteTool().getAvailableLocales( getLocales() );
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
            Locale locale = getDefaultLocale();

            File descriptorFile =
                getSiteTool()
                             .getSiteDescriptorFromBasedir(
                                                            PathUtils.toRelative( project.getBasedir(),
                                                                                  siteDirectory.getAbsolutePath() ),
                                                            project.getBasedir(), locale );
            DecorationModel decoration = null;
            if ( descriptorFile.exists() )
            {
                Map props = new HashMap();

                XmlStreamReader reader = null;
                try
                {
                    reader = ReaderFactory.newXmlReader( descriptorFile );
                    String siteDescriptorContent = IOUtil.toString( reader );

                    siteDescriptorContent =
                        getSiteTool().getInterpolatedSiteDescriptorContent( props, project, siteDescriptorContent,
                                                                            reader.getEncoding(), "UTF-8" );

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
     * Read and filter the <code>docDescriptor</code> file.
     *
     * @param project not null
     * @param docDescriptor not null
     * @param log not null
     * @return a DocumentModel instance
     * @throws DocumentRendererException if any
     * @throws IOException if any
     */
    private static DocumentModel readAndFilterDocumentDescriptor( MavenProject project, File docDescriptor, Log log )
        throws DocumentRendererException, IOException
    {
        Reader reader = null;
        try
        {
            // System properties
            Properties filterProperties = new Properties( System.getProperties() );
            // Project properties
            if ( project != null && project.getProperties() != null )
            {
                filterProperties.putAll( project.getProperties() );
            }

            reader =
                new InterpolationFilterReader( ReaderFactory.newXmlReader( docDescriptor ), filterProperties,
                                               "${", "}" );
            reader = new InterpolationFilterReader( reader, new ReflectionProperties( project, log ), "${", "}" );

            return new DocumentXpp3Reader().read( reader );
        }
        catch ( XmlPullParserException e )
        {
            throw new DocumentRendererException( "Error parsing document descriptor", e );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    static class ReflectionProperties
        extends Properties
    {
        private MavenProject project;

        private Log log;

        public ReflectionProperties( MavenProject aProject, Log aLog )
        {
            super();

            this.project = aProject;
            this.log = aLog;
        }

        /** {@inheritDoc} */
        public Object get( Object key )
        {
            Object value = null;
            try
            {
                value = ReflectionValueExtractor.evaluate( key.toString(), project );
            }
            catch ( Exception e )
            {
                log.error( e.getMessage(), e );
            }

            return value;
        }
    }
}
