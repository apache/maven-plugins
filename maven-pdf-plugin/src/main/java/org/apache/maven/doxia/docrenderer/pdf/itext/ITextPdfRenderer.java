package org.apache.maven.doxia.docrenderer.pdf.itext;

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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.maven.doxia.docrenderer.DocumentRendererContext;
import org.apache.maven.doxia.docrenderer.DocumentRendererException;
import org.apache.maven.doxia.docrenderer.pdf.AbstractPdfRenderer;
import org.apache.maven.doxia.docrenderer.pdf.PdfRenderer;
import org.apache.maven.doxia.document.DocumentCover;
import org.apache.maven.doxia.document.DocumentMeta;
import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.doxia.document.DocumentTOCItem;
import org.apache.maven.doxia.module.itext.ITextSink;
import org.apache.maven.doxia.module.itext.ITextSinkFactory;
import org.apache.maven.doxia.module.itext.ITextUtil;
import org.apache.maven.doxia.parser.module.ParserModule;
import org.apache.xml.utils.DefaultErrorHandler;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.lowagie.text.ElementTags;

/**
 * Abstract <code>document</code> render with the <code>iText</code> framework
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @author ltheussl
 * @version $Id: ITextPdfRenderer.java 1726406 2016-01-23 15:06:45Z hboutemy $
 * @since 1.1
 */
@Component( role = PdfRenderer.class, hint = "itext" )
public class ITextPdfRenderer
    extends AbstractPdfRenderer
{
    /** The xslt style sheet used to transform a Document to an iText file. */
    private static final String XSLT_RESOURCE = "TOC.xslt";

    /** The TransformerFactory. */
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    /** The DocumentBuilderFactory. */
    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();

    /** The DocumentBuilder. */
    private static final DocumentBuilder DOCUMENT_BUILDER;

    static
    {
        TRANSFORMER_FACTORY.setErrorListener( new DefaultErrorHandler() );

        try
        {
            DOCUMENT_BUILDER = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
        }
        catch ( ParserConfigurationException e )
        {
            throw new RuntimeException( "Error building document :" + e.getMessage() );
        }
    }

    /** {@inheritDoc} */
    public void generatePdf( File inputFile, File pdfFile )
        throws DocumentRendererException
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Generating : " + pdfFile );
        }

        try
        {
            ITextUtil.writePdf( new FileInputStream( inputFile ), new FileOutputStream( pdfFile ) );
        }
        catch ( IOException e )
        {
            throw new DocumentRendererException( "Cannot create PDF from " + inputFile + ": " + e.getMessage(), e );
        }
        catch ( RuntimeException e )
        {
            throw new DocumentRendererException( "Error creating PDF from " + inputFile + ": " + e.getMessage(), e );
        }
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

        File outputITextFile = new File( outputDirectory, outputName + ".xml" );
        if ( !outputITextFile.getParentFile().exists() )
        {
            outputITextFile.getParentFile().mkdirs();
        }

        File pdfOutputFile = new File( outputDirectory, outputName + ".pdf" );
        if ( !pdfOutputFile.getParentFile().exists() )
        {
            pdfOutputFile.getParentFile().mkdirs();
        }

        List<File> iTextFiles;
        if ( ( documentModel.getToc() == null ) || ( documentModel.getToc().getItems() == null ) )
        {
            getLogger().info( "No TOC is defined in the document descriptor. Merging all documents." );

            iTextFiles = parseAllFiles( filesToProcess, outputDirectory, context );
        }
        else
        {
            getLogger().debug( "Using TOC defined in the document descriptor." );

            iTextFiles = parseTOCFiles( outputDirectory, documentModel, context );
        }

        String generateTOC =
            ( context != null && context.get( "generateTOC" ) != null ? context.get( "generateTOC" ).toString()
                            : "start" );

        File iTextFile = new File( outputDirectory, outputName + ".xml" );
        File iTextOutput = new File( outputDirectory, outputName + "." + getOutputExtension() );
        Document document = generateDocument( iTextFiles );
        transform( documentModel, document, iTextFile, generateTOC );
        generatePdf( iTextFile, iTextOutput );
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

            File outputITextFile = new File( outputDirectory, output + ".xml" );
            if ( !outputITextFile.getParentFile().exists() )
            {
                outputITextFile.getParentFile().mkdirs();
            }

            File pdfOutputFile = new File( outputDirectory, output + ".pdf" );
            if ( !pdfOutputFile.getParentFile().exists() )
            {
                pdfOutputFile.getParentFile().mkdirs();
            }

            parse( fullDoc, module, outputITextFile, context );

            generatePdf( outputITextFile, pdfOutputFile );
        }
    }

      //--------------------------------------------
     //
    //--------------------------------------------


    /**
     * Parse a source document and emit results into a sink.
     *
     * @param fullDocPath file to the source document.
     * @param module the site module associated with the source document (determines the parser to use).
     * @param iTextFile the resulting iText xml file.
     * @throws DocumentRendererException in case of a parsing problem.
     * @throws IOException if the source and/or target document cannot be opened.
     */
    private void parse( File fullDoc, ParserModule module, File iTextFile, DocumentRendererContext context )
        throws DocumentRendererException, IOException
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Parsing file " + fullDoc.getAbsolutePath() );
        }

        System.setProperty( "itext.basedir", iTextFile.getParentFile().getAbsolutePath() );

        Writer writer = null;
        ITextSink sink = null;
        try
        {
            writer = WriterFactory.newXmlWriter( iTextFile );
            sink = (ITextSink) new ITextSinkFactory().createSink( writer );

            sink.setClassLoader( new URLClassLoader( new URL[] { iTextFile.getParentFile().toURI().toURL() } ) );

            parse( fullDoc.getAbsolutePath(), module.getParserId(), sink, context );
        }
        finally
        {
            if ( sink != null )
            {
                sink.flush();
                sink.close();
            }
            IOUtil.close( writer );
            System.getProperties().remove( "itext.basedir" );
        }
    }

    /**
     * Merge all iTextFiles to a single one.
     *
     * @param iTextFiles list of iText xml files.
     * @return Document.
     * @throws DocumentRendererException if any.
     * @throws IOException if any.
     */
    private Document generateDocument( List<File> iTextFiles )
        throws DocumentRendererException, IOException
    {
        Document document = DOCUMENT_BUILDER.newDocument();
        document.appendChild( document.createElement( ElementTags.ITEXT ) ); // Used only to set a root

        for ( File iTextFile : iTextFiles )
        {
            Document iTextDocument;

            try
            {
                iTextDocument = DOCUMENT_BUILDER.parse( iTextFile );
            }
            catch ( SAXException e )
            {
                throw new DocumentRendererException( "SAX Error : " + e.getMessage() );
            }

            // Only one chapter per doc
            Node chapter = iTextDocument.getElementsByTagName( ElementTags.CHAPTER ).item( 0 );

            try
            {
                document.getDocumentElement().appendChild( document.importNode( chapter, true ) );
            }
            catch ( DOMException e )
            {
                throw new DocumentRendererException( "Error appending chapter for "
                        + iTextFile + " : " + e.getMessage() );
            }
        }

        return document;
    }

    /**
     * Initialize the transformer object.
     *
     * @return an instance of a transformer object.
     * @throws DocumentRendererException if any.
     */
    private Transformer initTransformer()
        throws DocumentRendererException
    {
        try
        {
            Transformer transformer = TRANSFORMER_FACTORY.newTransformer( new StreamSource( ITextPdfRenderer.class
                .getResourceAsStream( XSLT_RESOURCE ) ) );

            transformer.setErrorListener( TRANSFORMER_FACTORY.getErrorListener() );

            transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "false" );

            transformer.setOutputProperty( OutputKeys.INDENT, "yes" );

            transformer.setOutputProperty( OutputKeys.METHOD, "xml" );

            transformer.setOutputProperty( OutputKeys.ENCODING, "UTF-8" );

            // No doctype since itext doctype is not up to date!

            return transformer;
        }
        catch ( TransformerConfigurationException e )
        {
            throw new DocumentRendererException( "Error configuring Transformer for " + XSLT_RESOURCE + ": "
                + e.getMessage() );
        }
        catch ( IllegalArgumentException e )
        {
            throw new DocumentRendererException( "Error configuring Transformer for " + XSLT_RESOURCE + ": "
                + e.getMessage() );
        }
    }

    /**
     * Add transformer parameters from a DocumentModel.
     *
     * @param transformer the Transformer to set the parameters.
     * @param documentModel the DocumentModel to take the parameters from, could be null.
     * @param iTextFile the iTextFile not null for the relative paths.
     * @param generateTOC not null, possible values are: 'none', 'start' and 'end'.
     */
    private void addTransformerParameters( Transformer transformer, DocumentModel documentModel, File iTextFile,
                                           String generateTOC )
    {
        if ( documentModel == null )
        {
            return;
        }

        // TOC
        addTransformerParameter( transformer, "toc.position", generateTOC );

        // Meta parameters
        boolean hasNullMeta = false;
        if ( documentModel.getMeta() == null )
        {
            hasNullMeta = true;
            documentModel.setMeta( new DocumentMeta() );
        }
        addTransformerParameter( transformer, "meta.author", documentModel.getMeta().getAllAuthorNames(),
                                 System.getProperty( "user.name", "null" ) );
        addTransformerParameter( transformer, "meta.creator", documentModel.getMeta().getCreator(),
                                 System.getProperty( "user.name", "null" ) );
        // see com.lowagie.text.Document#addCreationDate()
        SimpleDateFormat sdf = new SimpleDateFormat( "EEE MMM dd HH:mm:ss zzz yyyy" );
        addTransformerParameter( transformer, "meta.creationdate", documentModel.getMeta().getCreationdate(),
                                 sdf.format( new Date() ) );
        addTransformerParameter( transformer, "meta.keywords", documentModel.getMeta().getAllKeyWords() );
        addTransformerParameter( transformer, "meta.pagesize", documentModel.getMeta().getPageSize(),
                                 ITextUtil.getPageSize( ITextUtil.getDefaultPageSize() ) );
        addTransformerParameter( transformer, "meta.producer", documentModel.getMeta().getGenerator(),
                                 "Apache Doxia iText" );
        addTransformerParameter( transformer, "meta.subject", documentModel.getMeta().getSubject(),
                                 ( documentModel.getMeta().getTitle() != null ? documentModel.getMeta().getTitle()
                                                 : "" ) );
        addTransformerParameter( transformer, "meta.title", documentModel.getMeta().getTitle() );
        if ( hasNullMeta )
        {
            documentModel.setMeta( null );
        }

        // cover parameter
        boolean hasNullCover = false;
        if ( documentModel.getCover() == null )
        {
            hasNullCover = true;
            documentModel.setCover( new DocumentCover() );
        }
        addTransformerParameter( transformer, "cover.author", documentModel.getCover().getAllAuthorNames(),
                                 System.getProperty( "user.name", "null" ) );
        String companyLogo = getLogoURL( documentModel.getCover().getCompanyLogo(), iTextFile.getParentFile() );
        addTransformerParameter( transformer, "cover.companyLogo", companyLogo );
        addTransformerParameter( transformer, "cover.companyName", documentModel.getCover().getCompanyName() );
        if ( documentModel.getCover().getCoverdate() == null )
        {
            documentModel.getCover().setCoverDate( new Date() );
            addTransformerParameter( transformer, "cover.date", documentModel.getCover().getCoverdate() );
            documentModel.getCover().setCoverDate( null );
        }
        else
        {
            addTransformerParameter( transformer, "cover.date", documentModel.getCover().getCoverdate() );
        }
        addTransformerParameter( transformer, "cover.subtitle", documentModel.getCover().getCoverSubTitle() );
        addTransformerParameter( transformer, "cover.title", documentModel.getCover().getCoverTitle() );
        addTransformerParameter( transformer, "cover.type", documentModel.getCover().getCoverType() );
        addTransformerParameter( transformer, "cover.version", documentModel.getCover().getCoverVersion() );
        String projectLogo = getLogoURL( documentModel.getCover().getProjectLogo(), iTextFile.getParentFile() );
        addTransformerParameter( transformer, "cover.projectLogo", projectLogo );
        addTransformerParameter( transformer, "cover.projectName", documentModel.getCover().getProjectName() );
        if ( hasNullCover )
        {
            documentModel.setCover( null );
        }
    }

    /**
     * @param transformer not null
     * @param name not null
     * @param value could be empty
     * @param defaultValue could be empty
     * @since 1.1.1
     */
    private void addTransformerParameter( Transformer transformer, String name, String value, String defaultValue )
    {
        if ( StringUtils.isEmpty( value ) )
        {
            addTransformerParameter( transformer, name, defaultValue );
        }
        else
        {
            addTransformerParameter( transformer, name, value );
        }
    }

    /**
     * @param transformer not null
     * @param name not null
     * @param value could be empty
     * @since 1.1.1
     */
    private void addTransformerParameter( Transformer transformer, String name, String value )
    {
        if ( StringUtils.isEmpty( value ) )
        {
            return;
        }

        transformer.setParameter( name, value );
    }

    /**
     * Transform a document to an iTextFile.
     *
     * @param documentModel the DocumentModel to take the parameters from, could be null.
     * @param document the Document to transform.
     * @param iTextFile the resulting iText xml file.
     * @param generateTOC not null, possible values are: 'none', 'start' and 'end'.
     * @throws DocumentRendererException in case of a transformation error.
     */
    private void transform( DocumentModel documentModel, Document document, File iTextFile, String generateTOC )
        throws DocumentRendererException
    {
        Transformer transformer = initTransformer();

        addTransformerParameters( transformer, documentModel, iTextFile, generateTOC );

        // need a writer for StreamResult to prevent FileNotFoundException when iTextFile contains spaces
        Writer writer = null;
        try
        {
            writer = WriterFactory.newXmlWriter( iTextFile );
            transformer.transform( new DOMSource( document ), new StreamResult( writer ) );
        }
        catch ( TransformerException e )
        {
            throw new DocumentRendererException(
                                                 "Error transforming Document " + document + ": " + e.getMessage(),
                                                 e );
        }
        catch ( IOException e )
        {
            throw new DocumentRendererException(
                                                 "Error transforming Document " + document + ": " + e.getMessage(),
                                                 e );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    /**
     * @param filesToProcess not null
     * @param outputDirectory not null
     * @return a list of all parsed files.
     * @throws DocumentRendererException if any
     * @throws IOException if any
     * @since 1.1.1
     */
    private List<File> parseAllFiles( Map<String, ParserModule> filesToProcess, File outputDirectory,
                                      DocumentRendererContext context )
        throws DocumentRendererException, IOException
    {
        List<File> iTextFiles = new LinkedList<File>();
        for ( Map.Entry<String, ParserModule> entry : filesToProcess.entrySet() )
        {
            String key = entry.getKey();
            ParserModule module = entry.getValue();
            File fullDoc = new File( getBaseDir(), module.getSourceDirectory() + File.separator + key );

            String outputITextName = key.substring( 0, key.lastIndexOf( '.' ) + 1 ) + "xml";
            File outputITextFileTmp = new File( outputDirectory, outputITextName );
            outputITextFileTmp.deleteOnExit();
            if ( !outputITextFileTmp.getParentFile().exists() )
            {
                outputITextFileTmp.getParentFile().mkdirs();
            }

            iTextFiles.add( outputITextFileTmp );
            parse( fullDoc, module, outputITextFileTmp, context );
        }

        return iTextFiles;
    }

    /**
     * @param filesToProcess not null
     * @param outputDirectory not null
     * @return a list of all parsed files.
     * @throws DocumentRendererException if any
     * @throws IOException if any
     * @since 1.1.1
     */
    private List<File> parseTOCFiles( File outputDirectory, DocumentModel documentModel,
                                      DocumentRendererContext context )
        throws DocumentRendererException, IOException
    {
        List<File> iTextFiles = new LinkedList<File>();
        for ( Iterator<DocumentTOCItem> it = documentModel.getToc().getItems().iterator(); it.hasNext(); )
        {
            DocumentTOCItem tocItem = it.next();

            if ( tocItem.getRef() == null )
            {
                getLogger().debug(
                                   "No ref defined for the tocItem '" + tocItem.getName()
                                       + "' in the document descriptor. IGNORING" );
                continue;
            }

            String href = StringUtils.replace( tocItem.getRef(), "\\", "/" );
            if ( href.lastIndexOf( '.' ) != -1 )
            {
                href = href.substring( 0, href.lastIndexOf( '.' ) );
            }

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
                            String outputITextName = doc.substring( 0, doc.lastIndexOf( '.' ) + 1 ) + "xml";
                            File outputITextFileTmp = new File( outputDirectory, outputITextName );
                            outputITextFileTmp.deleteOnExit();
                            if ( !outputITextFileTmp.getParentFile().exists() )
                            {
                                outputITextFileTmp.getParentFile().mkdirs();
                            }
    
                            iTextFiles.add( outputITextFileTmp );
                            parse( source, module, outputITextFileTmp, context );
                        }
                    }
                }
            }
        }

        return iTextFiles;
    }

    /**
     * @param logo
     * @param parentFile
     * @return the logo url or null if unable to create it.
     * @since 1.1.1
     */
    private String getLogoURL( String logo, File parentFile )
    {
        if ( logo == null )
        {
            return null;
        }

        try
        {
            return new URL( logo ).toString();
        }
        catch ( MalformedURLException e )
        {
            try
            {
                File f = new File( parentFile, logo );
                if ( !f.exists() )
                {
                    getLogger().warn( "The logo " + f.getAbsolutePath() + " doesnt exist. IGNORING" );
                }
                else
                {
                    return f.toURI().toURL().toString();
                }
            }
            catch ( MalformedURLException e1 )
            {
                getLogger().debug( "Failed to convert to URL: " + logo, e1 );
            }
        }

        return null;
    }
}
