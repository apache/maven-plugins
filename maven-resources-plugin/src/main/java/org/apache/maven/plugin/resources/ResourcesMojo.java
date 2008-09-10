package org.apache.maven.plugin.resources;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

/**
 * Copy resources for the main source code to the main output directory.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author Andreas Hoheneder
 * @author William Ferguson
 * @version $Id$
 * @goal resources
 * @phase process-resources
 */
public class ResourcesMojo
    extends AbstractMojo
{

    /**
     * The character encoding scheme to be applied when filtering resources.
     *
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     */
    protected String encoding;

    /**
     * The output directory into which to copy the resources.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * The list of resources we want to transfer.
     *
     * @parameter expression="${project.resources}"
     * @required
     */
    private List resources;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The list of additional key-value pairs aside from that of the System,
     * and that of the project, which would be used for the filtering.
     *
     * @parameter expression="${project.build.filters}"
     */
    protected List filters;
    
    /**
     * 
     * @component role="org.apache.maven.shared.filtering.MavenResourcesFiltering" role-hint="default"
     * @required
     */    
    protected MavenResourcesFiltering mavenResourcesFiltering;    
    
    /**
     * @parameter expression="${session}"
     * @readonly
     * @required
     */
    protected MavenSession session;   
    
    /**
     * Expression preceded with the String won't be interpolated 
     * \${foo} will be replaced with ${foo}
     * @parameter expression="${maven.resources.escapeString}"
     * @since 2.3
     */    
    protected String escapeString;

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            
            if ( StringUtils.isEmpty( encoding ) && isFilteringEnabled( getResources() ) )
            {
                getLog().warn(
                               "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                                   + ", i.e. build is platform dependent!" );
            }
            
            
            MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution( getResources(), getOutputDirectory(),
                                                                                           project, encoding, filters,
                                                                                           Collections.EMPTY_LIST,
                                                                                           session );
            mavenResourcesExecution.setEscapeString( escapeString );
            mavenResourcesFiltering.filterResources( mavenResourcesExecution );
        }
        catch ( MavenFilteringException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }
    
    /**
     * Determines whether filtering has been enabled for any resource.
     * 
     * @param resources The set of resources to check for filtering, may be <code>null</code>.
     * @return <code>true</code> if at least one resource uses filtering, <code>false</code> otherwise.
     */
    private boolean isFilteringEnabled( Collection resources )
    {
        if ( resources != null )
        {
            for ( Iterator i = resources.iterator(); i.hasNext(); )
            {
                Resource resource = (Resource) i.next();
                if ( resource.isFiltering() )
                {
                    return true;
                }
            }
        }
        return false;
    }

    public List getResources()
    {
        return resources;
    }

    public void setResources( List resources )
    {
        this.resources = resources;
    }

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }
    
    
}
