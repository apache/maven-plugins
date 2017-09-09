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

/**
 * Base renderer interface for the <code>document</code>
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id: DocRenderer.java 1072376 2011-02-19 16:28:01Z hboutemy $
 * @deprecated Since 1.1, use {@link DocumentRenderer} instead.
 */
public interface DocRenderer
{
    /** Plexus lookup. */
    String ROLE = DocRenderer.class.getName();

    /**
     * Render all files from a site directory to an output directory
     *
     * @param siteDirectory the input directory contains files to be generated
     * @param outputDirectory the output directory where files are generated
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any
     * @throws java.io.IOException if any
     */
    void render( File siteDirectory, File outputDirectory )
        throws DocumentRendererException, IOException;

    /**
     * Render a document depending a context and a document descriptor
     *
     * @param siteDirectory the input directory contains files to be generated
     * @param outputDirectory the output directory where file are generated
     * @param documentDescriptor the document descriptor
     * @throws org.apache.maven.doxia.docrenderer.DocumentRendererException if any
     * @throws java.io.IOException if any
     */
    void render( File siteDirectory, File outputDirectory, File documentDescriptor )
        throws DocumentRendererException, IOException;

    /**
     * Get the output extension supported
     *
     * @return the ouput extension supported
     */
    String getOutputExtension();
}
