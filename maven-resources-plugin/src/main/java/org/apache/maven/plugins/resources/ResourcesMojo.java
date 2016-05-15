package org.apache.maven.plugins.resources;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

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

/**
 * Copy resources for the main source code to the main output directory. Always uses the project.build.resources element
 * to specify the resources to copy.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author Andreas Hoheneder
 * @author William Ferguson
 */
@Mojo( name = "resources", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresProject = true, threadSafe = true )
public class ResourcesMojo
    extends AbstractMojo
    implements Contextualizable
{

    /**
     * The character encoding scheme to be applied when filtering resources.
     */
    @Parameter( defaultValue = "${project.build.sourceEncoding}" )
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
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    /**
     * The list of additional filter properties files to be used along with System and project properties, which would
     * be used for the filtering. <br/>
     * See also: {@link ResourcesMojo#filters}.
     *
     * @since 2.4
     */
    @Parameter( defaultValue = "${project.build.filters}", readonly = true )
    protected List<String> buildFilters;

    /**
     * The list of extra filter properties files to be used along with System properties, project properties, and filter
     * properties files specified in the POM build/filters section, which should be used for the filtering during the
     * current mojo execution. <br/>
     * Normally, these will be configured from a plugin's execution section, to provide a different set of filters for a
     * particular execution. For instance, starting in Maven 2.2.0, you have the option of configuring executions with
     * the id's <code>default-resources</code> and <code>default-testResources</code> to supply different configurations
     * for the two different types of resources. By supplying <code>extraFilters</code> configurations, you can separate
     * which filters are used for which type of resource.
     */
    @Parameter
    protected List<String> filters;

    /**
     * If false, don't use the filters specified in the build/filters section of the POM when processing resources in
     * this mojo execution. <br/>
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
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    protected MavenSession session;

    /**
     * Expression preceded with the String won't be interpolated. For
     * example {@code \${foo}} will be replaced with {@code ${foo}}.
     * <br/>
     * <b>Note: With 3.0.0 a default value has been defined.</b>
     * @since 2.3
     */
    @Parameter( defaultValue = "\\" )
    protected String escapeString;

    /**
     * Overwrite existing files even if the destination files are newer.
     *
     * @since 2.3
     */
    @Parameter( defaultValue = "false" )
    private boolean overwrite;

    /**
     * Copy any empty directories included in the Resources.
     *
     * @since 2.3
     */
    @Parameter( defaultValue = "false" )
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
    @Parameter( defaultValue = "true" )
    protected boolean escapeWindowsPaths;

    /**
     * <p>
     * Set of delimiters for expressions to filter within the resources. These delimiters are specified in the form
     * {@code beginToken*endToken}. If no {@code *} is given, the delimiter is assumed to be the same for start and end.
     * </p>
     * <p>
     * So, the default filtering delimiters might be specified as:
     * </p>
     * 
     * <pre>
     * &lt;delimiters&gt;
     *   &lt;delimiter&gt;${*}&lt;/delimiter&gt;
     *   &lt;delimiter&gt;@&lt;/delimiter&gt;
     * &lt;/delimiters&gt;
     * </pre>
     * <p>
     * Since the {@code @} delimiter is the same on both ends, we don't need to specify {@code @*@} (though we can).
     * </p>
     *
     * @since 2.4
     */
    @Parameter
    protected LinkedHashSet<String> delimiters;

    /**
     * Use default delimiters in addition to custom delimiters, if any.
     *
     * @since 2.4
     */
    @Parameter( defaultValue = "true" )
    protected boolean useDefaultDelimiters;

    /**
     * By default files like {@code .gitignore}, {@code .cvsignore} etc. are excluded which means they will not being
     * copied. If you need them for a particular reason you can do that by settings this to {@code false}. This means
     * all files like the following will be copied.
     * <ul>
     * <li>Misc: &#42;&#42;/&#42;~, &#42;&#42;/#&#42;#, &#42;&#42;/.#&#42;, &#42;&#42;/%&#42;%, &#42;&#42;/._&#42;</li>
     * <li>CVS: &#42;&#42;/CVS, &#42;&#42;/CVS/&#42;&#42;, &#42;&#42;/.cvsignore</li>
     * <li>RCS: &#42;&#42;/RCS, &#42;&#42;/RCS/&#42;&#42;</li>
     * <li>SCCS: &#42;&#42;/SCCS, &#42;&#42;/SCCS/&#42;&#42;</li>
     * <li>VSSercer: &#42;&#42;/vssver.scc</li>
     * <li>MKS: &#42;&#42;/project.pj</li>
     * <li>SVN: &#42;&#42;/.svn, &#42;&#42;/.svn/&#42;&#42;</li>
     * <li>GNU: &#42;&#42;/.arch-ids, &#42;&#42;/.arch-ids/&#42;&#42;</li>
     * <li>Bazaar: &#42;&#42;/.bzr, &#42;&#42;/.bzr/&#42;&#42;</li>
     * <li>SurroundSCM: &#42;&#42;/.MySCMServerInfo</li>
     * <li>Mac: &#42;&#42;/.DS_Store</li>
     * <li>Serena Dimension: &#42;&#42;/.metadata, &#42;&#42;/.metadata/&#42;&#42;</li>
     * <li>Mercurial: &#42;&#42;/.hg, &#42;&#42;/.hg/&#42;&#42;</li>
     * <li>GIT: &#42;&#42;/.git, &#42;&#42;/.gitignore, &#42;&#42;/.gitattributes, &#42;&#42;/.git/&#42;&#42;</li>
     * <li>Bitkeeper: &#42;&#42;/BitKeeper, &#42;&#42;/BitKeeper/&#42;&#42;, &#42;&#42;/ChangeSet,
     * &#42;&#42;/ChangeSet/&#42;&#42;</li>
     * <li>Darcs: &#42;&#42;/_darcs, &#42;&#42;/_darcs/&#42;&#42;, &#42;&#42;/.darcsrepo,
     * &#42;&#42;/.darcsrepo/&#42;&#42;&#42;&#42;/-darcs-backup&#42;, &#42;&#42;/.darcs-temp-mail
     * </ul>
     *
     * @since 3.0.0
     */
    @Parameter( defaultValue = "true" )
    protected boolean addDefaultExcludes;

    /**
     * <p>
     * List of plexus components hint which implements
     * {@link MavenResourcesFiltering#filterResources(MavenResourcesExecution)}. They will be executed after the
     * resources copying/filtering.
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
    @Parameter( defaultValue = "false" )
    private boolean supportMultiLineFiltering;

    /**
     * Support filtering of filenames folders etc.
     * 
     * @since 3.0.0
     */
    @Parameter( defaultValue = "false" )
    private boolean fileNameFiltering;

    /**
     * You can skip the execution of the plugin if you need to. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     * 
     * @since 3.0.0
     */
    @Parameter( property = "maven.resources.skip", defaultValue = "false" )
    private boolean skip;

    /** {@inheritDoc} */
    public void contextualize( Context context )
        throws ContextException
    {
        plexusContainer = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        if ( isSkip() )
        {
            getLog().info( "Skipping the execution." );
            return;
        }

        if ( StringUtils.isEmpty( encoding ) && isFilteringEnabled( getResources() ) )
        {
            getLog().warn( "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                + ", i.e. build is platform dependent!" );
        }

        try
        {

            List<String> combinedFilters = getCombinedFiltersList();

            MavenResourcesExecution mavenResourcesExecution =
                new MavenResourcesExecution( getResources(), getOutputDirectory(), project, encoding, combinedFilters,
                                             Collections.<String>emptyList(), session );

            mavenResourcesExecution.setEscapeWindowsPaths( escapeWindowsPaths );

            // never include project build filters in this call, since we've already accounted for the POM build filters
            // above, in getCombinedFiltersList().
            mavenResourcesExecution.setInjectProjectBuildFilters( false );

            mavenResourcesExecution.setEscapeString( escapeString );
            mavenResourcesExecution.setOverwrite( overwrite );
            mavenResourcesExecution.setIncludeEmptyDirs( includeEmptyDirs );
            mavenResourcesExecution.setSupportMultiLineFiltering( supportMultiLineFiltering );
            mavenResourcesExecution.setFilterFilenames( fileNameFiltering );
            mavenResourcesExecution.setAddDefaultExcludes( addDefaultExcludes );

            // Handle subject of MRESOURCES-99
            Properties additionalProperties = addSeveralSpecialProperties();
            mavenResourcesExecution.setAdditionalProperties( additionalProperties );

            // if these are NOT set, just use the defaults, which are '${*}' and '@'.
            mavenResourcesExecution.setDelimiters( delimiters, useDefaultDelimiters );

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
     * This solves https://issues.apache.org/jira/browse/MRESOURCES-99.<br/>
     * BUT:<br/>
     * This should be done different than defining those properties a second time, cause they have already being defined
     * in Maven Model Builder (package org.apache.maven.model.interpolation) via BuildTimestampValueSource. But those
     * can't be found in the context which can be got from the maven core.<br/>
     * A solution could be to put those values into the context by Maven core so they are accessible everywhere. (I'm
     * not sure if this is a good idea). Better ideas are always welcome.
     * 
     * The problem at the moment is that maven core handles usage of properties and replacements in 
     * the model, but does not the resource filtering which needed some of the properties.
     * 
     * @return the new instance with those properties.
     */
    private Properties addSeveralSpecialProperties()
    {
        String timeStamp = new MavenBuildTimestamp().formattedTimestamp();
        Properties additionalProperties = new Properties();
        additionalProperties.put( "maven.build.timestamp", timeStamp );
        if ( project.getBasedir() != null )
        {
            additionalProperties.put( "project.baseUri", project.getBasedir().getAbsoluteFile().toURI().toString() );
        }

        return additionalProperties;
    }

    /**
     * @param mavenResourcesExecution {@link MavenResourcesExecution} 
     * @throws MojoExecutionException in case of wrong lookup.
     * @throws MavenFilteringException in case of failure.
     * @since 2.5
     */
    protected void executeUserFilterComponents( MavenResourcesExecution mavenResourcesExecution )
        throws MojoExecutionException, MavenFilteringException
    {

        if ( mavenFilteringHints != null )
        {
            for ( String hint : mavenFilteringHints )
            {
                try
                {
                    // CHECKSTYLE_OFF: LineLength
                    mavenFilteringComponents.add( (MavenResourcesFiltering) plexusContainer.lookup( MavenResourcesFiltering.class.getName(),
                                                                                                    hint ) );
                    // CHECKSTYLE_ON: LineLength
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

    /**
     * @return The combined filters.
     */
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
     * @param theResources The set of resources to check for filtering, may be <code>null</code>.
     * @return <code>true</code> if at least one resource uses filtering, <code>false</code> otherwise.
     */
    private boolean isFilteringEnabled( Collection<Resource> theResources )
    {
        if ( theResources != null )
        {
            for ( Resource resource : theResources )
            {
                if ( resource.isFiltering() )
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return {@link #resources}
     */
    public List<Resource> getResources()
    {
        return resources;
    }

    /**
     * @param resources set {@link #resources}
     */
    public void setResources( List<Resource> resources )
    {
        this.resources = resources;
    }

    /**
     * @return {@link #outputDirectory}
     */
    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * @param outputDirectory the output folder.
     */
    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    /**
     * @return {@link #overwrite}
     */
    public boolean isOverwrite()
    {
        return overwrite;
    }

    /**
     * @param overwrite true to overwrite false otherwise.
     */
    public void setOverwrite( boolean overwrite )
    {
        this.overwrite = overwrite;
    }

    /**
     * @return {@link #includeEmptyDirs}
     */
    public boolean isIncludeEmptyDirs()
    {
        return includeEmptyDirs;
    }

    /**
     * @param includeEmptyDirs true/false.
     */
    public void setIncludeEmptyDirs( boolean includeEmptyDirs )
    {
        this.includeEmptyDirs = includeEmptyDirs;
    }

    /**
     * @return {@link #filters}
     */
    public List<String> getFilters()
    {
        return filters;
    }

    /**
     * @param filters The filters to use.
     */
    public void setFilters( List<String> filters )
    {
        this.filters = filters;
    }

    /**
     * @return {@link #delimiters}
     */
    public LinkedHashSet<String> getDelimiters()
    {
        return delimiters;
    }

    /**
     * @param delimiters The delimiters to use.
     */
    public void setDelimiters( LinkedHashSet<String> delimiters )
    {
        this.delimiters = delimiters;
    }

    /**
     * @return {@link #useDefaultDelimiters}
     */
    public boolean isUseDefaultDelimiters()
    {
        return useDefaultDelimiters;
    }

    /**
     * @param useDefaultDelimiters true to use {@code ${*}}
     */
    public void setUseDefaultDelimiters( boolean useDefaultDelimiters )
    {
        this.useDefaultDelimiters = useDefaultDelimiters;
    }

    /**
     * @return {@link #skip}
     */
    public boolean isSkip()
    {
        return skip;
    }

}
