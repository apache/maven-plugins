package org.apache.maven.doxia.docrenderer.pdf.fo;

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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.maven.doxia.docrenderer.DocumentRendererContext;
import org.apache.maven.doxia.docrenderer.DocumentRendererException;
import org.apache.maven.doxia.docrenderer.pdf.AbstractPdfRenderer;
import org.apache.maven.doxia.docrenderer.pdf.PdfRenderer;
import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.doxia.document.DocumentTOC;
import org.apache.maven.doxia.document.DocumentTOCItem;
import org.apache.maven.doxia.module.fo.FoAggregateSink;
import org.apache.maven.doxia.module.fo.FoSink;
import org.apache.maven.doxia.module.fo.FoSinkFactory;
import org.apache.maven.doxia.module.fo.FoUtils;
import org.apache.maven.doxia.parser.module.ParserModule;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;

import org.xml.sax.SAXParseException;

/**
 * PDF renderer that uses Doxia's FO module.
 *
 * @author ltheussl
 * @version $Id: FoPdfRenderer.java 1726406 2016-01-23 15:06:45Z hboutemy $
 * @since 1.1
 */
@Component( role = PdfRenderer.class, hint = "fo" )
public class FoPdfRenderer
    extends AbstractPdfRenderer
{
    /**
     * {@inheritDoc}
     * @see org.apache.maven.doxia.module.fo.FoUtils#convertFO2PDF(File, File, String)
     */
    public void generatePdf( File inputFile, File pdfFile )
        throws DocumentRendererException
    {
        // Should take care of the document model for the metadata...
        generatePdf( inputFile, pdfFile, null );
    }

    /** {@inheritDoc} */
    @Override
    public void render( Map<String, ParserModule> filesToProcess, File outputDirectory, DocumentModel documentModel )
        throws DocumentRendererException, IOException
    {
        render( filesToProcess, outputDirectory, documentModel, null );
    }

    /** {@inheritDoc} */
    @Override
    public void render( Map<String, ParserModule> filesToProcess, File outputDirectory, DocumentModel documentModel,
                        DocumentRendererContext context )
        throws DocumentRendererException, IOException
    {
        // copy resources, images, etc.
        copyResources( outputDirectory );

        if ( documentModel == null )
        {
            getLogger().debug( "No document model, generating all documents individually." );

            renderIndividual( filesToProcess, outputDirectory, context );
            return;
        }

        String outputName = getOutputName( documentModel );

        File outputFOFile = new File( outputDirectory, outputName + ".fo" );
        if ( !outputFOFile.getParentFile().exists() )
        {
            outputFOFile.getParentFile().mkdirs();
        }

        File pdfOutputFile = new File( outputDirectory, outputName + ".pdf" );
        if ( !pdfOutputFile.getParentFile().exists() )
        {
            pdfOutputFile.getParentFile().mkdirs();
        }

        Writer writer = null;
        try
        {
            writer = WriterFactory.newXmlWriter( outputFOFile );

            FoAggregateSink sink = new FoAggregateSink( writer, context );

            File fOConfigFile = new File( outputDirectory, "pdf-config.xml" );

            if ( fOConfigFile.exists() )
            {
                sink.load( fOConfigFile );
                getLogger().debug( "Loaded pdf config file: " + fOConfigFile.getAbsolutePath() );
            }

            String generateTOC =
                ( context != null && context.get( "generateTOC" ) != null )
                        ? context.get( "generateTOC" ).toString().trim()
                        : "start";
            int tocPosition = 0;
            if ( "start".equalsIgnoreCase( generateTOC ) )
            {
                tocPosition = FoAggregateSink.TOC_START;
            }
            else if ( "end".equalsIgnoreCase( generateTOC ) )
            {
                tocPosition = FoAggregateSink.TOC_END;
            }
            else
            {
                tocPosition = FoAggregateSink.TOC_NONE;
            }
            sink.setDocumentModel( documentModel, tocPosition );

            sink.beginDocument();

            sink.coverPage();

            if ( tocPosition == FoAggregateSink.TOC_START )
            {
                sink.toc();
            }

            if ( ( documentModel.getToc() == null ) || ( documentModel.getToc().getItems() == null ) )
            {
                getLogger().info( "No TOC is defined in the document descriptor. Merging all documents." );

                mergeAllSources( filesToProcess, sink, context );
            }
            else
            {
                getLogger().debug( "Using TOC defined in the document descriptor." );

                mergeSourcesFromTOC( documentModel.getToc(), sink, context );
            }

            if ( tocPosition == FoAggregateSink.TOC_END )
            {
                sink.toc();
            }

            sink.endDocument();
        }
        finally
        {
            IOUtil.close( writer );
        }

        generatePdf( outputFOFile, pdfOutputFile, documentModel );
    }

    /** {@inheritDoc} */
    @Override
    public void renderIndividual( Map<String, ParserModule> filesToProcess, File outputDirectory )
        throws DocumentRendererException, IOException
    {
        renderIndividual( filesToProcess, outputDirectory, null );
    }

    /** {@inheritDoc} */
    @Override
    public void renderIndividual( Map<String, ParserModule> filesToProcess, File outputDirectory,
                                  DocumentRendererContext context )
        throws DocumentRendererException, IOException
    {
        for ( Map.Entry<String, ParserModule> entry : filesToProcess.entrySet() )
        {
            String key = entry.getKey();
            ParserModule module = entry.getValue();

            File fullDoc = new File( getBaseDir(), module.getSourceDirectory() + File.separator + key );

            String output = key;
            for ( String extension : module.getExtensions() )
            {
                String lowerCaseExtension = extension.toLowerCase( Locale.ENGLISH );
                if ( output.toLowerCase( Locale.ENGLISH ).indexOf( "." + lowerCaseExtension ) != -1 )
                {
                    output =
                        output.substring( 0, output.toLowerCase( Locale.ENGLISH ).indexOf( "." + lowerCaseExtension ) );
                }
            }

            File outputFOFile = new File( outputDirectory, output + ".fo" );
            if ( !outputFOFile.getParentFile().exists() )
            {
                outputFOFile.getParentFile().mkdirs();
            }

            File pdfOutputFile = new File( outputDirectory, output + ".pdf" );
            if ( !pdfOutputFile.getParentFile().exists() )
            {
                pdfOutputFile.getParentFile().mkdirs();
            }

            FoSink sink =
                (FoSink) new FoSinkFactory().createSink( outputFOFile.getParentFile(), outputFOFile.getName() );
            sink.beginDocument();
            parse( fullDoc.getAbsolutePath(), module.getParserId(), sink, context );
            sink.endDocument();

            generatePdf( outputFOFile, pdfOutputFile, null );
        }
    }

    private void mergeAllSources( Map<String, ParserModule> filesToProcess, FoAggregateSink sink,
                                  DocumentRendererContext context )
        throws DocumentRendererException, IOException
    {
        for ( Map.Entry<String, ParserModule> entry : filesToProcess.entrySet() )
        {
            String key = entry.getKey();
            ParserModule module = entry.getValue();
            sink.setDocumentName( key );
            File fullDoc = new File( getBaseDir(), module.getSourceDirectory() + File.separator + key );

            parse( fullDoc.getAbsolutePath(), module.getParserId(), sink, context );
        }
    }

    private void mergeSourcesFromTOC( DocumentTOC toc, FoAggregateSink sink, DocumentRendererContext context )
        throws IOException, DocumentRendererException
    {
        parseTocItems( toc.getItems(), sink, context );
    }

    private void parseTocItems( List<DocumentTOCItem> items, FoAggregateSink sink, DocumentRendererContext context )
        throws IOException, DocumentRendererException
    {
        for ( DocumentTOCItem tocItem : items )
        {
            if ( tocItem.getRef() == null )
            {
                if ( getLogger().isInfoEnabled() )
                {
                    getLogger().info( "No ref defined for tocItem " + tocItem.getName() );
                }

                continue;
            }

            String href = StringUtils.replace( tocItem.getRef(), "\\", "/" );
            if ( href.lastIndexOf( '.' ) != -1 )
            {
                href = href.substring( 0, href.lastIndexOf( '.' ) );
            }

            renderModules( href, sink, tocItem, context );

            if ( tocItem.getItems() != null )
            {
                parseTocItems( tocItem.getItems(), sink, context );
            }
        }
    }

    private void renderModules( String href, FoAggregateSink sink, DocumentTOCItem tocItem,
                                DocumentRendererContext context )
        throws DocumentRendererException, IOException
    {
        Collection<ParserModule> modules = parserModuleManager.getParserModules();
        for ( ParserModule module : modules )
        {
            File moduleBasedir = new File( getBaseDir(), module.getSourceDirectory() );

            if ( moduleBasedir.exists() )
            {
                for ( String extension : module.getExtensions() )
                {
                    String doc = href + "." + extension;
                    File source = new File( moduleBasedir, doc );
    
                    // Velocity file?
                    if ( !source.exists() )
                    {
                        if ( href.indexOf( "." + extension ) != -1 )
                        {
                            doc = href + ".vm";
                        }
                        else
                        {
                            doc = href + "." + extension + ".vm";
                        }
                        source = new File( moduleBasedir, doc );
                    }
    
                    if ( source.exists() )
                    {
                        sink.setDocumentName( doc );
                        sink.setDocumentTitle( tocItem.getName() );
    
                        parse( source.getPath(), module.getParserId(), sink, context );
                    }
                }
            }
        }
    }

    /**
     * @param inputFile
     * @param pdfFile
     * @param documentModel could be null
     * @throws DocumentRendererException if any
     * @since 1.1.1
     */
    private void generatePdf( File inputFile, File pdfFile, DocumentModel documentModel )
        throws DocumentRendererException
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Generating: " + pdfFile );
        }

        try
        {
            FoUtils.convertFO2PDF( inputFile, pdfFile, null, documentModel );
        }
        catch ( TransformerException e )
        {
            if ( ( e.getCause() != null ) && ( e.getCause() instanceof SAXParseException ) )
            {
                SAXParseException sax = (SAXParseException) e.getCause();

                StringBuilder sb = new StringBuilder();
                sb.append( "Error creating PDF from " ).append( inputFile.getAbsolutePath() ).append( ":" )
                  .append( sax.getLineNumber() ).append( ":" ).append( sax.getColumnNumber() ).append( "\n" );
                sb.append( e.getMessage() );

                throw new DocumentRendererException( sb.toString() );
            }

            throw new DocumentRendererException( "Error creating PDF from " + inputFile + ": " + e.getMessage() );
        }
    }
}
