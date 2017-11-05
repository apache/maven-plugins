package org.apache.maven.doxia.module.fo;

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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.maven.doxia.document.DocumentModel;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * <code>FO Sink</code> utilities.
 *
 * @author ltheussl
 * @version $Id: FoUtils.java 785531 2009-06-17 09:47:59Z ltheussl $
 * @since 1.1
 */
public class FoUtils
{
    /** To reuse the FopFactory **/
    private static final FopFactory FOP_FACTORY = FopFactory.newInstance();

    /** To reuse the TransformerFactory **/
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    /**
     * Converts an FO file to a PDF file using FOP.
     *
     * @param fo the FO file, not null.
     * @param pdf the target PDF file, not null.
     * @param resourceDir The base directory for relative path resolution, could be null.
     * If null, defaults to the parent directory of fo.
     * @param documentModel the document model to add PDF metadatas like author, title and keywords, could be null.
     * @throws javax.xml.transform.TransformerException In case of a conversion problem.
     * @since 1.1.1
     */
    public static void convertFO2PDF( File fo, File pdf, String resourceDir, DocumentModel documentModel )
        throws TransformerException
    {
        FOUserAgent foUserAgent = getDefaultUserAgent( fo, resourceDir );

        if ( documentModel != null && documentModel.getMeta() != null )
        {
            // http://xmlgraphics.apache.org/fop/embedding.html#user-agent
            String authors = documentModel.getMeta().getAllAuthorNames();
            if ( StringUtils.isNotEmpty( authors ) )
            {
                foUserAgent.setAuthor( authors );
            }
            if ( StringUtils.isNotEmpty( documentModel.getMeta().getTitle() ) )
            {
                foUserAgent.setTitle( documentModel.getMeta().getTitle() );
            }
            String keywords = documentModel.getMeta().getAllKeyWords();
            if ( StringUtils.isNotEmpty( keywords ) )
            {
                foUserAgent.setKeywords( keywords );
            }
            if ( StringUtils.isNotEmpty( documentModel.getMeta().getCreator() ) )
            {
                foUserAgent.setCreator( documentModel.getMeta().getCreator() );
            }
            if ( StringUtils.isNotEmpty( documentModel.getMeta().getGenerator() ) )
            {
                foUserAgent.setProducer( documentModel.getMeta().getGenerator() );
            }
            if ( documentModel.getMeta().getCreationDate() != null )
            {
                foUserAgent.setCreationDate( documentModel.getMeta().getCreationDate() );
            }
        }

        if ( foUserAgent.getCreator() == null )
        {
            foUserAgent.setCreator( System.getProperty( "user.name" ) );
        }
        if ( foUserAgent.getCreationDate() == null )
        {
            foUserAgent.setCreationDate( new Date() );
        }

        convertFO2PDF( fo, pdf, resourceDir, foUserAgent );
    }

    /**
     * Converts an FO file to a PDF file using FOP.
     *
     * @param fo the FO file, not null.
     * @param pdf the target PDF file, not null.
     * @param resourceDir The base directory for relative path resolution, could be null.
     * If null, defaults to the parent directory of fo.
     * @param foUserAgent the FOUserAgent to use.
     *      May be null, in which case a default user agent will be used.
     * @throws javax.xml.transform.TransformerException In case of a conversion problem.
     * @since 1.1.1
     */
    public static void convertFO2PDF( File fo, File pdf, String resourceDir, FOUserAgent foUserAgent )
        throws TransformerException
    {
        FOUserAgent userAgent = ( foUserAgent == null ? getDefaultUserAgent( fo, resourceDir ) : foUserAgent );

        OutputStream out = null;
        try
        {
            try
            {
                out = new BufferedOutputStream( new FileOutputStream( pdf ) );
            }
            catch ( IOException e )
            {
                throw new TransformerException( e );
            }

            Result res = null;
            try
            {
                Fop fop = FOP_FACTORY.newFop( MimeConstants.MIME_PDF, userAgent, out );
                res = new SAXResult( fop.getDefaultHandler() );
            }
            catch ( FOPException e )
            {
                throw new TransformerException( e );
            }

            Transformer transformer = null;
            try
            {
                // identity transformer
                transformer = TRANSFORMER_FACTORY.newTransformer();
            }
            catch ( TransformerConfigurationException e )
            {
                throw new TransformerException( e );
            }

            transformer.transform( new StreamSource( fo ), res );
        }
        finally
        {
            IOUtil.close( out );
        }
    }

    /**
     * Converts an FO file to a PDF file using FOP.
     *
     * @param fo the FO file, not null.
     * @param pdf the target PDF file, not null.
     * @param resourceDir The base directory for relative path resolution, could be null.
     * If null, defaults to the parent directory of fo.
     * @throws javax.xml.transform.TransformerException In case of a conversion problem.
     * @see #convertFO2PDF(File, File, String, DocumentModel)
     */
    public static void convertFO2PDF( File fo, File pdf, String resourceDir )
        throws TransformerException
    {
        convertFO2PDF( fo, pdf, resourceDir, (DocumentModel) null );
    }

    /**
     * Returns a base URL to be used by the FOUserAgent.
     *
     * @param fo the FO file.
     * @param resourceDir the resource directory.
     * @return String.
     */
    private static String getBaseURL( File fo, String resourceDir )
    {
        String url = null;

        if ( resourceDir == null )
        {
            url = "file:///" + fo.getParent() + "/";
        }
        else
        {
            url = "file:///" + resourceDir + "/";
        }

        return url;
    }

    private static FOUserAgent getDefaultUserAgent( File fo, String resourceDir )
    {
        FOUserAgent foUserAgent = FOP_FACTORY.newFOUserAgent();
        foUserAgent.setBaseURL( getBaseURL( fo, resourceDir ) );

        return foUserAgent;
    }

    private FoUtils()
    {
        // Utility class
    }
}
