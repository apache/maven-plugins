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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Copy resources for the main source code to the main output directory.
 * Always uses the project.build.resources element to specify the resources to copy.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author Andreas Hoheneder
 * @author William Ferguson
 */
@Mojo( name = "resources", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true )
public class ResourcesMojo
    extends AbstractMojo
    implements Contextualizable
{

    /**
     * The character encoding scheme to be applied when filtering resources.
     */
    @Parameter( property = "encoding", defaultValue = "${project.build.sourceEncoding}" )
    protected String encoding;

    /**
     * The output directory into which to copy the resources.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}", required = true )
    private File outputDirectory;

    /**
     * The list of resources we want to transfer.
     */
    @Parameter( defaultValue = "${project.resources}", required = true, readonly = true )
    private List<Resource> resources;

    /**
     *
     */
    @Parameter( defaultValue = "${project}", required = true, readonly = true )
    protected MavenProject project;

    /**
     * The list of additional filter properties files to be used along with System and project
     * properties, which would be used for the filtering.
     * <br/>
     * See also: {@link ResourcesMojo#filters}.
     *
     * @since 2.4
     */
    @Parameter( defaultValue = "${project.build.filters}", readonly = true )
    protected List<String> buildFilters;

    /**
     * The list of extra filter properties files to be used along with System properties,
     * project properties, and filter properties files specified in the POM build/filters section,
     * which should be used for the filtering during the current mojo execution.
     * <br/>
     * Normally, these will be configured from a plugin's execution section, to provide a different
     * set of filters for a particular execution. For instance, starting in Maven 2.2.0, you have the
     * option of configuring executions with the id's <code>default-resources</code> and
     * <code>default-testResources</code> to supply different configurations for the two
     * different types of resources. By supplying <code>extraFilters</code> configurations, you
     * can separate which filters are used for which type of resource.
     */
    @Parameter
    protected List<String> filters;

    /**
     * If false, don't use the filters specified in the build/filters section of the POM when
     * processing resources in this mojo execution.
     * <br/>
     * See also: {@link ResourcesMojo#buildFilters} and {@link ResourcesMojo#filters}
     *
     * @since 2.4
     */
    @Parameter( defaultValue = "true" )
    protected boolean useBuildFilters;

    /**
     *
     */
    @Component( role = MavenResourcesFiltering.class, hint = "default" )
    protected MavenResourcesFiltering mavenResourcesFiltering;

    /**
     *
     */
    @Parameter( defaultValue = "${session}", required = true, readonly = true )
    protected MavenSession session;

    /**
     * Expression preceded with the String won't be interpolated
     * \${foo} will be replaced with ${foo}
     *
     * @since 2.3
     */
    @Parameter( property = "maven.resources.escapeString" )
    protected String escapeString;

    /**
     * Overwrite existing files even if the destination files are newer.
     *
     * @since 2.3
     */
    @Parameter( property = "maven.resources.overwrite", defaultValue = "false" )
    private boolean overwrite;

    /**
     * Copy any empty directories included in the Resources.
     *
     * @since 2.3
     */
    @Parameter( property = "maven.resources.includeEmptyDirs", defaultValue = "false" )
    protected boolean includeEmptyDirs;

    /**
     * Additional file extensions to not apply filtering (already defined are : jpg, jpeg, gif, bmp, png)
     *
     * @since 2.3
     */
    @Parameter
    protected List<String> nonFilteredFileExtensions;

    /**
     * Whether to escape backslashes and colons in windows-style paths.
     *
     * @since 2.4
     */
    @Parameter( property = "maven.resources.escapeWindowsPaths", defaultValue = "true" )
    protected boolean escapeWindowsPaths;

    /**
     * <p>
     * Set of delimiters for expressions to filter within the resources. These delimiters are specified in the
     * form 'beginToken*endToken'. If no '*' is given, the delimiter is assumed to be the same for start and end.
     * </p><p>
     * So, the default filtering delimiters might be specified as:
     * </p>
     * <pre>
     * &lt;delimiters&gt;
     *   &lt;delimiter&gt;${*}&lt/delimiter&gt;
     *   &lt;delimiter&gt;@&lt/delimiter&gt;
     * &lt;/delimiters&gt;
     * </pre>
     * <p>
     * Since the '@' delimiter is the same on both ends, we don't need to specify '@*@' (though we can).
     * </p>
     *
     * @since 2.4
     */
    @Parameter
    protected List<String> delimiters;

    /**
     * @since 2.4
     */
    @Parameter( defaultValue = "true" )
    protected boolean useDefaultDelimiters;

    /**
     * <p>
     * List of plexus components hint which implements {@link MavenResourcesFiltering#filterResources(MavenResourcesExecution)}.
     * They will be executed after the resources copying/filtering.
     * </p>
     *
     * @since 2.4
     */
    @Parameter
    private List<String> mavenFilteringHints;

    /**
     * @since 2.4
     */
    private PlexusContainer plexusContainer;

    /**
     * @since 2.4
     */
    private List<MavenResourcesFiltering> mavenFilteringComponents = new ArrayList<MavenResourcesFiltering>();

    /**
     * stop searching endToken at the end of line
     *
     * @since 2.5
     */
    @Parameter( property = "maven.resources.supportMultiLineFiltering", defaultValue = "false" )
    private boolean supportMultiLineFiltering;

    public void contextualize( Context context )
        throws ContextException
    {
        plexusContainer = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    public void execute()
        throws MojoExecutionException
    {
        try
        {

            if ( StringUtils.isEmpty( encoding ) && isFilteringEnabled( getResources() ) )
            {
                getLog().warn( "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                                   + ", i.e. build is platform dependent!" );
            }

            List filters = getCombinedFiltersList();

            MavenResourcesExecution mavenResourcesExecution =
                new MavenResourcesExecution( getResources(), getOutputDirectory(), project, encoding, filters,
                                             Collections.<String>emptyList(), session );

            mavenResourcesExecution.setEscapeWindowsPaths( escapeWindowsPaths );

            // never include project build filters in this call, since we've already accounted for the POM build filters
            // above, in getCombinedFiltersList().
            mavenResourcesExecution.setInjectProjectBuildFilters( false );

            mavenResourcesExecution.setEscapeString( escapeString );
            mavenResourcesExecution.setOverwrite( overwrite );
            mavenResourcesExecution.setIncludeEmptyDirs( includeEmptyDirs );
            mavenResourcesExecution.setSupportMultiLineFiltering( supportMultiLineFiltering );

            // if these are NOT set, just use the defaults, which are '${*}' and '@'.
            if ( delimiters != null && !delimiters.isEmpty() )
            {
                LinkedHashSet<String> delims = new LinkedHashSet<String>();
                if ( useDefaultDelimiters )
                {
                    delims.addAll( mavenResourcesExecution.getDelimiters() );
                }

                for ( String delim : delimiters )
                {
                    if ( delim == null )
                    {
                        // FIXME: ${filter:*} could also trigger this condition. Need a better long-term solution.
                        delims.add( "${*}" );
                    }
                    else
                    {
                        delims.add( delim );
                    }
                }

                mavenResourcesExecution.setDelimiters( delims );
            }

            if ( nonFilteredFileExtensions != null )
            {
                mavenResourcesExecution.setNonFilteredFileExtensions( nonFilteredFileExtensions );
            }
            mavenResourcesFiltering.filterResources( mavenResourcesExecution );

            executeUserFilterComponents( mavenResourcesExecution );
        }
        catch ( MavenFilteringException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    /**
     * @since 2.5
     */
    protected void executeUserFilterComponents( MavenResourcesExecution mavenResourcesExecution )
        throws MojoExecutionException, MavenFilteringException
    {

        if ( mavenFilteringHints != null )
        {
            for ( Iterator ite = mavenFilteringHints.iterator(); ite.hasNext(); )
            {
                String hint = (String) ite.next();
                try
                {
                    mavenFilteringComponents.add(
                        (MavenResourcesFiltering) plexusContainer.lookup( MavenResourcesFiltering.class.getName(),
                                                                          hint ) );
                }
                catch ( ComponentLookupException e )
                {
                    throw new MojoExecutionException( e.getMessage(), e );
                }
            }
        }
        else
        {
            getLog().debug( "no use filter components" );
        }

        if ( mavenFilteringComponents != null && !mavenFilteringComponents.isEmpty() )
        {
            getLog().debug( "execute user filters" );
            for ( MavenResourcesFiltering filter : mavenFilteringComponents )
            {
                filter.filterResources( mavenResourcesExecution );
            }
        }
    }

    protected List<String> getCombinedFiltersList()
    {
        if ( filters == null || filters.isEmpty() )
        {
            return useBuildFilters ? buildFilters : null;
        }
        else
        {
            List<String> result = new ArrayList<String>();

            if ( useBuildFilters && buildFilters != null && !buildFilters.isEmpty() )
            {
                result.addAll( buildFilters );
            }

            result.addAll( filters );

            return result;
        }
    }

    /**
     * Determines whether filtering has been enabled for any resource.
     *
     * @param resources The set of resources to check for filtering, may be <code>null</code>.
     * @return <code>true</code> if at least one resource uses filtering, <code>false</code> otherwise.
     */
    private boolean isFilteringEnabled( Collection<Resource> resources )
    {
        if ( resources != null )
        {
            for ( Resource resource : resources )
            {
                if ( resource.isFiltering() )
                {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Resource> getResources()
    {
        return resources;
    }

    public void setResources( List<Resource> resources )
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

    public boolean isOverwrite()
    {
        return overwrite;
    }

    public void setOverwrite( boolean overwrite )
    {
        this.overwrite = overwrite;
    }

    public boolean isIncludeEmptyDirs()
    {
        return includeEmptyDirs;
    }

    public void setIncludeEmptyDirs( boolean includeEmptyDirs )
    {
        this.includeEmptyDirs = includeEmptyDirs;
    }

    public List<String> getFilters()
    {
        return filters;
    }

    public void setFilters( List<String> filters )
    {
        this.filters = filters;
    }

    public List<String> getDelimiters()
    {
        return delimiters;
    }

    public void setDelimiters( List<String> delimiters )
    {
        this.delimiters = delimiters;
    }

    public boolean isUseDefaultDelimiters()
    {
        return useDefaultDelimiters;
    }

    public void setUseDefaultDelimiters( boolean useDefaultDelimiters )
    {
        this.useDefaultDelimiters = useDefaultDelimiters;
    }

}
