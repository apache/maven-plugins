package org.apache.maven.doxia.docrenderer.itext;

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
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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

import org.apache.maven.doxia.Doxia;
import org.apache.maven.doxia.docrenderer.DocRenderer;
import org.apache.maven.doxia.docrenderer.DocumentRendererException;
import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.doxia.document.DocumentTOCItem;
import org.apache.maven.doxia.document.io.xpp3.DocumentXpp3Reader;
import org.apache.maven.doxia.module.itext.ITextSink;
import org.apache.maven.doxia.module.itext.ITextSinkFactory;
import org.apache.maven.doxia.module.itext.ITextUtil;
import org.apache.maven.doxia.parser.module.ParserModule;
import org.apache.maven.doxia.parser.module.ParserModuleManager;
import org.apache.maven.doxia.parser.ParseException;
import org.apache.maven.doxia.parser.manager.ParserNotFoundException;
import org.apache.xml.utils.DefaultErrorHandler;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.XmlUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.lowagie.text.ElementTags;

/**
 * Abstract <code>document</code> render with the <code>iText</code> framework
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id: AbstractITextRender.java 1726406 2016-01-23 15:06:45Z hboutemy $
 * @deprecated since 1.1, use an implementation of {@link org.apache.maven.doxia.docrenderer.DocumentRenderer}.
 */
public abstract class AbstractITextRender
    extends AbstractLogEnabled
    implements DocRenderer
{
    private static final String XSLT_RESOURCE = "org/apache/maven/doxia/docrenderer/pdf/itext/TOC.xslt";

    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();

    /**
     * @plexus.requirement
     */
    protected ParserModuleManager parserModuleManager;

    /**
     * @plexus.requirement
     */
    protected Doxia doxia;

    static
    {
        TRANSFORMER_FACTORY.setErrorListener( new DefaultErrorHandler() );
    }

    private List<String> getModuleFileNames( ParserModule module, File moduleBasedir )
        throws IOException
    {
        StringBuilder includes = new StringBuilder();

        for ( String extension: module.getExtensions() )
        {
            if ( includes.length() > 0 )
            {
                includes.append( ',' );
            }
            includes.append( "**/*." );
            includes.append( extension );
        }

        return FileUtils.getFileNames( moduleBasedir, includes.toString(), null, false );
    }

    /** {@inheritDoc} */
    public void render( File siteDirectory, File outputDirectory )
        throws DocumentRendererException, IOException
    {
        Collection<ParserModule> modules = parserModuleManager.getParserModules();
        for ( ParserModule module : modules )
        {
            File moduleBasedir = new File( siteDirectory, module.getSourceDirectory() );

            if ( moduleBasedir.exists() )
            {
                List<String> docs = getModuleFileNames( module, moduleBasedir );

                for ( String doc : docs )
                {
                    String fullPathDoc = new File( moduleBasedir, doc ).getPath();

                    String outputITextName = doc.substring( 0, doc.indexOf( '.' ) + 1 ) + "xml";
                    File outputITextFile = new File( outputDirectory, outputITextName );
                    if ( !outputITextFile.getParentFile().exists() )
                    {
                        outputITextFile.getParentFile().mkdirs();
                    }
                    String iTextOutputName = doc.substring( 0, doc.indexOf( '.' ) + 1 ) + getOutputExtension();
                    File iTextOutputFile = new File( outputDirectory, iTextOutputName );
                    if ( !iTextOutputFile.getParentFile().exists() )
                    {
                        iTextOutputFile.getParentFile().mkdirs();
                    }

                    parse( fullPathDoc, module, outputITextFile );

                    generateOutput( outputITextFile, iTextOutputFile );
                }
            }
        }
    }

    /** {@inheritDoc} */
    public void render( File siteDirectory, File outputDirectory, File documentDescriptor )
        throws DocumentRendererException, IOException
    {
        if ( ( documentDescriptor == null ) || ( !documentDescriptor.exists() ) )
        {
            if ( getLogger().isInfoEnabled() )
            {
                getLogger().info( "No documentDescriptor is found. Generate all documents." );
            }
            render( siteDirectory, outputDirectory );
            return;
        }

        DocumentModel documentModel;
        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( documentDescriptor );
            documentModel = new DocumentXpp3Reader().read( reader );
        }
        catch ( XmlPullParserException e )
        {
            throw new DocumentRendererException( "Error parsing document descriptor", e );
        }
        catch ( IOException e )
        {
            throw new DocumentRendererException( "Error reading document descriptor", e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        if ( documentModel.getOutputName() == null )
        {
            if ( getLogger().isInfoEnabled() )
            {
                getLogger().info( "No outputName is defined in the document descriptor. Using 'generated_itext'" );
            }
            documentModel.setOutputName( "generated_itext" );
        }

        if ( ( documentModel.getToc() == null ) || ( documentModel.getToc().getItems() == null ) )
        {
            if ( getLogger().isInfoEnabled() )
            {
                getLogger().info( "No TOC is defined in the document descriptor. Merging all documents." );
            }
        }

        List<File> iTextFiles = new LinkedList<File>();
        Collection<ParserModule> modules = parserModuleManager.getParserModules();
        for ( ParserModule module : modules )
        {
            File moduleBasedir = new File( siteDirectory, module.getSourceDirectory() );

            if ( moduleBasedir.exists() )
            {
                @SuppressWarnings ( "unchecked" )
                List<String> docs = getModuleFileNames( module, moduleBasedir );

                for ( String doc : docs )
                {
                    String fullPathDoc = new File( moduleBasedir, doc ).getPath();

                    String outputITextName = doc.substring( 0, doc.lastIndexOf( '.' ) + 1 ) + "xml";
                    File outputITextFile = new File( outputDirectory, outputITextName );

                    if ( ( documentModel.getToc() == null ) || ( documentModel.getToc().getItems() == null ) )
                    {
                        iTextFiles.add( outputITextFile );

                        if ( !outputITextFile.getParentFile().exists() )
                        {
                            outputITextFile.getParentFile().mkdirs();
                        }

                        parse( fullPathDoc, module, outputITextFile );
                    }
                    else
                    {
                        for ( Iterator<DocumentTOCItem> k = documentModel.getToc().getItems().iterator(); k.hasNext(); )
                        {
                            DocumentTOCItem tocItem = k.next();

                            if ( tocItem.getRef() == null )
                            {
                                if ( getLogger().isInfoEnabled() )
                                {
                                    getLogger().info( "No ref defined for an tocItem in the document descriptor." );
                                }
                                continue;
                            }

                            String outTmp = StringUtils.replace( outputITextFile.getAbsolutePath(), "\\", "/" );
                            outTmp = outTmp.substring( 0, outTmp.lastIndexOf( '.' ) );

                            String outRef = StringUtils.replace( tocItem.getRef(), "\\", "/" );
                            if ( outRef.lastIndexOf( '.' ) != -1 )
                            {
                                outRef = outRef.substring( 0, outRef.lastIndexOf( '.' ) );
                            }
                            else
                            {
                                outRef = outRef.substring( 0, outRef.length() );
                            }

                            if ( outTmp.indexOf( outRef ) != -1 )
                            {
                                iTextFiles.add( outputITextFile );

                                if ( !outputITextFile.getParentFile().exists() )
                                {
                                    outputITextFile.getParentFile().mkdirs();
                                }

                                parse( fullPathDoc, module, outputITextFile );
                            }
                        }
                    }
                }
            }
        }

        File iTextFile = new File( outputDirectory, documentModel.getOutputName() + ".xml" );
        File iTextOutput = new File( outputDirectory, documentModel.getOutputName() + "." + getOutputExtension() );
        Document document = generateDocument( iTextFiles );
        transform( documentModel, document, iTextFile );
        generateOutput( iTextFile, iTextOutput );
    }

    /**
     * Generate an ouput file with the iText framework
     *
     * @param iTextFile
     * @param iTextOutput
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any
     * @throws java.io.IOException if any
     */
    public abstract void generateOutput( File iTextFile, File iTextOutput )
        throws DocumentRendererException, IOException;

    /**
     * Parse a sink
     *
     * @param fullPathDoc
     * @param module
     * @param outputITextFile
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException
     * @throws java.io.IOException
     */
    private void parse( String fullPathDoc, ParserModule module, File outputITextFile )
        throws DocumentRendererException, IOException
    {
        Writer writer = WriterFactory.newXmlWriter( outputITextFile );
        ITextSink sink = (ITextSink) new ITextSinkFactory().createSink( writer );

        sink.setClassLoader( new URLClassLoader( new URL[] { outputITextFile.getParentFile().toURI().toURL() } ) );

        Reader reader = null;
        try
        {
            File f = new File( fullPathDoc );
            if ( XmlUtil.isXml( f ) )
            {
                reader = ReaderFactory.newXmlReader( f );
            }
            else
            {
                // TODO Platform dependent?
                reader = ReaderFactory.newPlatformReader( f );
            }

            System.setProperty( "itext.basedir", outputITextFile.getParentFile().getAbsolutePath() );

            doxia.parse( reader, module.getParserId(), sink );
        }
        catch ( ParserNotFoundException e )
        {
            throw new DocumentRendererException( "Error getting a parser for '"
                    + fullPathDoc + "': " + e.getMessage() );
        }
        catch ( ParseException e )
        {
            throw new DocumentRendererException( "Error parsing '"
                    + fullPathDoc + "': line [" + e.getLineNumber() + "] " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( reader );

            sink.flush();

            sink.close();

            IOUtil.close( writer );

            System.getProperties().remove( "itext.basedir" );
        }
    }

    /**
     * Merge all iTextFiles to a single one
     *
     * @param iTextFiles
     * @return a document
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any
     * @throws java.io.IOException if any
     */
    private Document generateDocument( List<File> iTextFiles )
        throws DocumentRendererException, IOException
    {
        Document document;
        try
        {
            document = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder().newDocument();
        }
        catch ( ParserConfigurationException e )
        {
            throw new DocumentRendererException( "Error building document :" + e.getMessage() );
        }
        document.appendChild( document.createElement( ElementTags.ITEXT ) ); // Used only to set a root

        for ( File iTextFile : iTextFiles )
        {
            Document iTextDocument;
            try
            {
                iTextDocument = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder().parse( iTextFile );
            }
            catch ( SAXException e )
            {
                throw new DocumentRendererException( "SAX Error : " + e.getMessage() );
            }
            catch ( ParserConfigurationException e )
            {
                throw new DocumentRendererException( "Error parsing configuration : " + e.getMessage() );
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
     * Init the transformer object
     *
     * @return an instanced transformer object
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any
     */
    private Transformer initTransformer()
        throws DocumentRendererException
    {
        try
        {
            Transformer transformer = TRANSFORMER_FACTORY.newTransformer( new StreamSource( DefaultPdfRenderer.class
                .getResourceAsStream( "/" + XSLT_RESOURCE ) ) );
            transformer.setErrorListener( TRANSFORMER_FACTORY.getErrorListener() );

            transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "false" );
            transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
            transformer.setOutputProperty( OutputKeys.METHOD, "xml" );
            transformer.setOutputProperty( OutputKeys.ENCODING, "UTF-8" );

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
     * Add transformer parameters
     *
     * @param transformer
     * @param documentModel
     */
    private void addTransformerParameters( Transformer transformer, DocumentModel documentModel )
    {
        if ( documentModel.getMeta().getTitle() != null )
        {
            transformer.setParameter( "title", documentModel.getMeta().getTitle() );
        }
        if ( documentModel.getMeta().getAuthor() != null )
        {
            transformer.setParameter( "author", documentModel.getMeta().getAuthor() );
        }
        transformer.setParameter( "creationdate", new Date().toString() );
        if ( documentModel.getMeta().getSubject() != null )
        {
            transformer.setParameter( "subject", documentModel.getMeta().getSubject() );
        }
        if ( documentModel.getMeta().getKeywords() != null )
        {
            transformer.setParameter( "keywords", documentModel.getMeta().getKeywords() );
        }
        transformer.setParameter( "producer", "Generated with Doxia by " + System.getProperty( "user.name" ) );
        if ( ITextUtil.isPageSizeSupported( documentModel.getMeta().getTitle() ) )
        {
            transformer.setParameter( "pagesize", documentModel.getMeta().getPageSize() );
        }
        else
        {
            transformer.setParameter( "pagesize", "A4" );
        }

        transformer.setParameter( "frontPageHeader", "" );
        if ( documentModel.getMeta().getTitle() != null )
        {
            transformer.setParameter( "frontPageTitle", documentModel.getMeta().getTitle() );
        }
        transformer.setParameter( "frontPageFooter", "Generated date " + new Date().toString() );
    }

    /**
     * Transform a document to an iTextFile
     *
     * @param documentModel
     * @param document
     * @param iTextFile
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any.
     */
    private void transform( DocumentModel documentModel, Document document, File iTextFile )
        throws DocumentRendererException
    {
        Transformer transformer = initTransformer();

        addTransformerParameters( transformer, documentModel );

        try
        {
            transformer.transform( new DOMSource( document ), new StreamResult( iTextFile ) );
        }
        catch ( TransformerException e )
        {
            throw new DocumentRendererException( "Error transformer Document from "
                    + document + ": " + e.getMessage() );
        }
    }
}
