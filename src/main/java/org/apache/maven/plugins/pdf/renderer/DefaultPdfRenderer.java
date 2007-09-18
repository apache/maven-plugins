package org.apache.maven.plugins.pdf.renderer;

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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Iterator;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.maven.doxia.Doxia;
import org.apache.maven.doxia.docrenderer.DocRendererException;
import org.apache.maven.doxia.docrenderer.document.DocumentModel;
import org.apache.maven.doxia.docrenderer.document.DocumentTOCItem;
import org.apache.maven.doxia.docrenderer.document.io.xpp3.DocumentXpp3Reader;
import org.apache.maven.doxia.module.site.SiteModule;
import org.apache.maven.doxia.module.site.manager.SiteModuleManager;
import org.apache.maven.doxia.module.fo.FoAggregateSink;
import org.apache.maven.doxia.module.fo.FoUtils;
import org.apache.maven.doxia.parser.ParseException;
import org.apache.maven.doxia.parser.manager.ParserNotFoundException;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Default PDF renderer using Doxia's FOP module.
 *
 * @author ltheussl
 * @version $Id$
 */
public class DefaultPdfRenderer
    implements PdfRenderer
{
    /**
     * @plexus.requirement
     */
    protected SiteModuleManager siteModuleManager;

    /**
     * @plexus.requirement
     */
    protected Doxia doxia;


    /** {@inheritDoc} */
    public String getOutputExtension()
    {
        return "pdf";
    }

    /**
     * Converts an FO file to a PDF file using FOP.
     * @param foFile the FO file.
     * @param pdfFile the target PDF file.
     * @throws DocRendererException In case of a conversion problem.
     * @see org.apache.maven.doxia.module.fo.FoUtils#convertFO2PDF(File,File,String);
     */

    public void generatePdf( File foFile, File pdfFile )
        throws DocRendererException
    {
        // TODO: getLogger().debug( "Generating : " + pdfFile );

        try
        {
            FoUtils.convertFO2PDF( foFile, pdfFile, null );
        }
        catch ( TransformerException e )
        {
            throw new DocRendererException( "Error creating PDF from " + foFile + ": " + e.getMessage() );
        }
    }

    /** {@inheritDoc} */
    public void render( File baseDirectory, File outputDirectory )
        throws DocRendererException, IOException
    {
        render( baseDirectory, outputDirectory, "target" );
    }

    private void render( File baseDirectory, File outputDirectory, String outputName )
        throws DocRendererException, IOException
    {
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

        FoAggregateSink sink = new FoAggregateSink( new FileWriter( outputFOFile ) );

        sink.beginDocument();

        for ( Iterator i = siteModuleManager.getSiteModules().iterator(); i.hasNext(); )
        {
            SiteModule module = (SiteModule) i.next();

            File moduleBasedir = new File( baseDirectory, module.getSourceDirectory() );

            // exclude fml, see http://jira.codehaus.org/browse/DOXIA-148
            if ( moduleBasedir.exists() && !"fml".equals( module.getExtension() ) )
            {
                List docs = FileUtils.getFileNames( moduleBasedir, "**/*." + module.getExtension(), null, false );

                for ( Iterator j = docs.iterator(); j.hasNext(); )
                {
                    String doc = (String) j.next();

                    String fullDocPath = new File( moduleBasedir, doc ).getPath();

                    sink.setDocumentName( doc );

                    parse( fullDocPath, module, sink );
                }
            }
        }

        sink.endDocument();

        generatePdf( outputFOFile, pdfOutputFile );

    }

    /** {@inheritDoc} */
    public void render( File baseDirectory, File outputDirectory, File documentDescriptor )
        throws DocRendererException, IOException
    {
        if ( documentDescriptor == null || !documentDescriptor.exists() )
        {
            // TODO: log
            render( baseDirectory, outputDirectory );
            return;
        }

        DocumentModel documentModel;
        try
        {
            documentModel = new DocumentXpp3Reader().read( new FileReader( documentDescriptor ) );
        }
        catch ( XmlPullParserException e )
        {
            throw new DocRendererException( "Error parsing document descriptor", e );
        }
        catch ( IOException e )
        {
            throw new DocRendererException( "Error reading document descriptor", e );
        }

        String name = documentModel.getOutputName();
        if ( name == null )
        {
            // TODO: getLogger().info( "No outputName is defined in the document descriptor. Using 'target.pdf'" );
            documentModel.setOutputName( "target" );
        }
        else if ( name.lastIndexOf( "." ) != -1 )
        {
            documentModel.setOutputName( name.substring( 0, name.lastIndexOf( "." ) ) );
        }

        if ( ( documentModel.getToc() == null ) || ( documentModel.getToc().getItems() == null ) )
        {
            // TODO: getLogger().info( "No TOC is defined in the document descriptor. Merging all documents." );
            render( baseDirectory, outputDirectory, documentModel.getOutputName() );
            return;
        }

        File outputFOFile = new File( outputDirectory, documentModel.getOutputName() + ".fo" );

        if ( !outputFOFile.getParentFile().exists() )
        {
            outputFOFile.getParentFile().mkdirs();
        }

        File pdfOutputFile = new File( outputDirectory, documentModel.getOutputName() + ".pdf" );

        if ( !pdfOutputFile.getParentFile().exists() )
        {
            pdfOutputFile.getParentFile().mkdirs();
        }


        FoAggregateSink sink = new FoAggregateSink( new FileWriter( outputFOFile ) );

        sink.beginDocument();

        sink.coverPage( documentModel.getMeta() );

        sink.toc( documentModel.getToc() );

        for ( Iterator k = documentModel.getToc().getItems().iterator(); k.hasNext(); )
        {
            DocumentTOCItem tocItem = (DocumentTOCItem) k.next();

            if ( tocItem.getRef() == null )
            {
                // TODO: getLogger().info( "No ref defined for an tocItem in the document descriptor." );
                continue;
            }

            String href = StringUtils.replace( tocItem.getRef(), "\\", "/" );

            if ( href.lastIndexOf( "." ) != -1 )
            {
                href = href.substring( 0, href.lastIndexOf( "." ) );
            }

            for ( Iterator i = siteModuleManager.getSiteModules().iterator(); i.hasNext(); )
            {
                SiteModule module = (SiteModule) i.next();

                File moduleBasedir = new File( baseDirectory, module.getSourceDirectory() );

                if ( moduleBasedir.exists() && !"fml".equals( module.getExtension() ) )
                {
                    String doc = href + "." + module.getExtension();

                    File source = new File( moduleBasedir, doc );

                    if ( source.exists() )
                    {
                        sink.setDocumentName( doc );

                        sink.setDocumentTitle( tocItem.getName() );

                        parse( source.getPath(), module, sink );
                    }
                }
            }
        }

        sink.endDocument();

        generatePdf( outputFOFile, pdfOutputFile );
    }

    /**
     * Parse a source document into a FO sink.
     *
     * @param fullDocPath full path to the source document.
     * @param module the SiteModule that the source document belongs to (determines the parser to use).
     * @param sink the sink to receive the events.
     * @throws DocRendererException in case of a parsing error.
     * @throws IOException if the source document cannot be opened.
     */
    private void parse( String fullDocPath, SiteModule module, FoAggregateSink sink )
        throws DocRendererException, IOException
    {
        try
        {
            FileReader reader = new FileReader( fullDocPath );

            doxia.parse( reader, module.getParserId(), sink );
        }
        catch ( ParserNotFoundException e )
        {
            throw new DocRendererException( "No parser '" + module.getParserId()
                        + "' found for " + fullDocPath + ": " + e.getMessage() );
        }
        catch ( ParseException e )
        {
            throw new DocRendererException( "Error parsing " + fullDocPath + ": " + e.getMessage(), e );
        }
        finally
        {
            sink.flush();
        }
    }


}
