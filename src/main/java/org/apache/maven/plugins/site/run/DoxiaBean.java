package org.apache.maven.plugins.site.run;

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

import java.util.Map;

import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;

/**
 * Bean to handle Doxia in a servlet context attribute
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class DoxiaBean
{
    private SiteRenderingContext context;

    private Map<String, DocumentRenderer> documents;

    private SiteRenderingContext generatedSiteContext;

    /**
     * @param context
     * @param documents
     * @param generatedSiteDirectory
     */
    public DoxiaBean( SiteRenderingContext context, Map<String, DocumentRenderer> documents,
                      SiteRenderingContext generatedSiteContext )
    {
        this.context = context;
        this.documents = documents;
        this.generatedSiteContext = generatedSiteContext;
    }

    public SiteRenderingContext getContext()
    {
        return context;
    }

    public void setContext( SiteRenderingContext context )
    {
        this.context = context;
    }

    public Map<String, DocumentRenderer> getDocuments()
    {
        return documents;
    }

    public void setDocuments( Map<String, DocumentRenderer> documents )
    {
        this.documents = documents;
    }

    public SiteRenderingContext getGeneratedSiteContext()
    {
        return generatedSiteContext;
    }

    public void setGeneratedSiteContext( SiteRenderingContext generatedSiteContext )
    {
        this.generatedSiteContext = generatedSiteContext;
    }
}
