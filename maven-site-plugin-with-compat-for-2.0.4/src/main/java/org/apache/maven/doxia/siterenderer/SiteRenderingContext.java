package org.apache.maven.doxia.siterenderer;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.doxia.site.decoration.DecorationModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id:DefaultSiteRenderer.java 348612 2005-11-24 12:54:19 +1100 (Thu, 24 Nov 2005) brett $
 */
public class SiteRenderingContext
{
    private static final String DEFAULT_INPUT_ENCODING = "UTF-8";

    private static final String DEFAULT_OUTPUT_ENCODING = "UTF-8";

    private String inputEncoding = DEFAULT_INPUT_ENCODING;

    private String outputEncoding = DEFAULT_OUTPUT_ENCODING;

    private String templateName;

    private ClassLoader templateClassLoader;

    private Map templateProperties;

    private Locale locale = Locale.getDefault();

    private DecorationModel decoration;

    private String defaultWindowTitle;

    private File skinJarFile;

    private boolean usingDefaultTemplate;

    private List siteDirectories = new ArrayList();

    private Map moduleExcludes;

    private List modules = new ArrayList();

    public String getTemplateName()
    {
        return templateName;
    }

    public ClassLoader getTemplateClassLoader()
    {
        return templateClassLoader;
    }

    public void setTemplateClassLoader( ClassLoader templateClassLoader )
    {
        this.templateClassLoader = templateClassLoader;
    }

    public Map getTemplateProperties()
    {
        return templateProperties;
    }

    public void setTemplateProperties( Map templateProperties )
    {
        this.templateProperties = Collections.unmodifiableMap( templateProperties );
    }

    public Locale getLocale()
    {
        return locale;
    }

    public void setLocale( Locale locale )
    {
        this.locale = locale;
    }

    public DecorationModel getDecoration()
    {
        return decoration;
    }

    public void setDecoration( DecorationModel decoration )
    {
        this.decoration = decoration;
    }

    public void setDefaultWindowTitle( String defaultWindowTitle )
    {
        this.defaultWindowTitle = defaultWindowTitle;
    }

    public String getDefaultWindowTitle()
    {
        return defaultWindowTitle;
    }

    public File getSkinJarFile()
    {
        return skinJarFile;
    }

    public void setSkinJarFile( File skinJarFile )
    {
        this.skinJarFile = skinJarFile;
    }

    public void setTemplateName( String templateName )
    {
        this.templateName = templateName;
    }

    public void setUsingDefaultTemplate( boolean usingDefaultTemplate )
    {
        this.usingDefaultTemplate = usingDefaultTemplate;
    }

    public boolean isUsingDefaultTemplate()
    {
        return usingDefaultTemplate;
    }

    public void addSiteDirectory( File file )
    {
        this.siteDirectories.add( file );
    }

    public void addModuleDirectory( File file, String moduleParserId )
    {
        this.modules.add( new ModuleReference( moduleParserId, file ) );
    }

    public List getSiteDirectories()
    {
        return siteDirectories;
    }

    public List getModules()
    {
        return modules;
    }

    public Map getModuleExcludes()
    {
        return moduleExcludes;
    }

    public void setModuleExcludes( Map moduleExcludes )
    {
        this.moduleExcludes = moduleExcludes;
    }

    public String getInputEncoding()
    {
        return inputEncoding;
    }

    public void setInputEncoding( String inputEncoding )
    {
        this.inputEncoding = inputEncoding;
    }

    public String getOutputEncoding()
    {
        return outputEncoding;
    }

    public void setOutputEncoding( String outputEncoding )
    {
        this.outputEncoding = outputEncoding;
    }
}
