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

import org.apache.maven.doxia.docrenderer.DocRenderer;

/**
 * PDF renderer interface for the <code>iText</code> framework
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id: PdfRenderer.java 1670514 2015-03-31 23:10:01Z hboutemy $
 * @deprecated since 1.1, use an implementation of {@link org.apache.maven.doxia.docrenderer.DocumentRenderer}.
 */
@SuppressWarnings( "checkstyle:interfaceistype" )
public interface PdfRenderer
    extends DocRenderer
{
    String ROLE = PdfRenderer.class.getName();
}
