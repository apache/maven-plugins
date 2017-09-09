package org.apache.maven.doxia.docrenderer.pdf;

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
import java.util.Map;

import org.apache.maven.doxia.docrenderer.AbstractDocumentRenderer;
import org.apache.maven.doxia.docrenderer.DocumentRendererException;
import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.doxia.parser.module.ParserModule;

/**
 * Abstract pdf renderer, this doesn't depend on the framework.
 *
 * @author ltheussl
 * @version $Id: AbstractPdfRenderer.java 1726406 2016-01-23 15:06:45Z hboutemy $
 * @since 1.1
 */
public abstract class AbstractPdfRenderer
    extends AbstractDocumentRenderer
    implements PdfRenderer
{
    /** {@inheritDoc} */
    public String getOutputExtension()
    {
        return "pdf";
    }

    /** {@inheritDoc} */
    public void render( Map<String, ParserModule> filesToProcess, File outputDirectory, DocumentModel documentModel )
        throws DocumentRendererException, IOException
    {
        render( filesToProcess, outputDirectory, documentModel, null );
    }
}
