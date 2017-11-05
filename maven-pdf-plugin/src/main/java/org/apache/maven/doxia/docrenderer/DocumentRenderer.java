package org.apache.maven.doxia.docrenderer;

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
import java.util.Collection;

import org.apache.maven.doxia.document.DocumentModel;

/**
 * Base interface for rendering documents from a set of input files.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @author ltheussl
 * @version $Id: DocumentRenderer.java 1670514 2015-03-31 23:10:01Z hboutemy $
 * @since 1.1
 */
public interface DocumentRenderer
{
    /** Plexus lookup role. */
    String ROLE = DocumentRenderer.class.getName();

    /**
     * Render a document from a set of files, depending on a rendering context.
     *
     * @param files the path name Strings (relative to a common base directory)
     *              of files to include in the document generation.
     * @param outputDirectory the output directory where the document should be generated.
     * @param documentModel the document model, containing all the metadata, etc.
     *              If the model contains a TOC, only the files found in this TOC are rendered,
     *              otherwise all files from the Collection of files will be processed.
     *              If the model is null, render all files individually.
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any.
     * @throws java.io.IOException if any.
     */
    void render( Collection<String> files, File outputDirectory, DocumentModel documentModel )
        throws DocumentRendererException, IOException;

    /**
     * Render a document from the files found in a source directory, depending on a rendering context.
     *
     * @param baseDirectory the directory containing the source files.
     *              This should follow the standard Maven convention, ie containing all the site modules.
     * @param outputDirectory the output directory where the document should be generated.
     * @param documentModel the document model, containing all the metadata, etc.
     *              If the model contains a TOC, only the files found in this TOC are rendered,
     *              otherwise all files found under baseDirectory will be processed.
     *              If the model is null, render all files from baseDirectory individually.
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any
     * @throws java.io.IOException if any
//     * @deprecated since 1.1.2, use {@link #render(File, File, DocumentModel, DocumentRendererContext)}
     */
    void render( File baseDirectory, File outputDirectory, DocumentModel documentModel )
        throws DocumentRendererException, IOException;

//    /**
//     * Render a document from the files found in a source directory, depending on a rendering context.
//     *
//     * @param baseDirectory the directory containing the source files.
//     *              This should follow the standard Maven convention, ie containing all the site modules.
//     * @param outputDirectory the output directory where the document should be generated.
//     * @param documentModel the document model, containing all the metadata, etc.
//     *              If the model contains a TOC, only the files found in this TOC are rendered,
//     *              otherwise all files found under baseDirectory will be processed.
//     *              If the model is null, render all files from baseDirectory individually.
//     * @param context the rendering context when processing files.
//     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any
//     * @throws java.io.IOException if any
//     * @since 1.1.2
//     */
//    void render( File baseDirectory, File outputDirectory, DocumentModel documentModel,
//                 DocumentRendererContext context )
//        throws DocumentRendererException, IOException;

    /**
     * Read a document model from a file.
     *
     * @param documentDescriptor a document descriptor file that contains the document model.
     * @return the document model, containing all the metadata, etc.
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any
     * @throws java.io.IOException if any
     */
    DocumentModel readDocumentModel( File documentDescriptor )
        throws DocumentRendererException, IOException;

    /**
     * Get the output extension associated with this DocumentRenderer.
     *
     * @return the ouput extension.
     */
    String getOutputExtension();
}
