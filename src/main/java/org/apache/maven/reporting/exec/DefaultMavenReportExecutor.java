package org.apache.maven.reporting.exec;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginContainerException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.DefaultPluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugin.version.PluginVersionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.filter.ExclusionsDependencyFilter;

/**
 * This component builds some {@link MavenReportExecution} from {@link MavenReportExecutorRequest}.
 * If a {@link MavenReport} need to fork a lifecycle, this fork is executed here. 
 * The component asks the core to get some informations on the plugin configuration in order to correctly setup
 * the {@link MavenReport}.
 * 
 * <h3>version resolution</h3>
 * If no version is defined for the report plugin, the version will be searched 
 * with {@link #getPluginVersion(ReportPlugin, RepositoryRequest, MavenReportExecutorRequest)} method.
 * 
 * <h3>component execution</h3>
 * Following steps are executed:
 * <ul>
 *   <li>get {@link PluginDescriptor} from the {@link MavenPluginManager#getPluginDescriptor(Plugin, RepositoryRequest)} core method</li>
 *   <li>setup a {@link ClassLoader} with the Maven Site plugin {@link ClassLoader} as parent for the report execution,
 *     using {@link MavenPluginManager#setupPluginRealm(PluginDescriptor, org.apache.maven.execution.MavenSession, ClassLoader, List, org.apache.maven.artifact.resolver.filter.ArtifactFilter)} 
 *     Note that some classes are imported from the current Site Mojo {@link ClassRealm} - see {@link #IMPORTS},
 *     and the artifact resolution excludes some artifacts - see {@link #EXCLUDES}.
 *   </li>
 *   <li>
 *     setup the mojo using {@link MavenPluginManager#getConfiguredMojo(Class, org.apache.maven.execution.MavenSession, MojoExecution)}
 *   </li>
 *   <li>
 *     verify with {@link LifecycleExecutor#calculateForkedExecutions(MojoExecution, org.apache.maven.execution.MavenSession)}
 *     if any forked execution is needed: if yes, execute the forked execution here
 *   </li>
 * </ul>
 * 
 * @author Olivier Lamy
 * @since 3.0-beta-1
 */
@Component( role = MavenReportExecutor.class )
public class DefaultMavenReportExecutor
    implements MavenReportExecutor
{
    @Requirement
    private Logger logger;

    @Requirement
    protected MavenPluginManager mavenPluginManager;

    @Requirement
    protected LifecycleExecutor lifecycleExecutor;

    @Requirement
    protected PluginVersionResolver pluginVersionResolver;

    protected static final List<String> IMPORTS = Arrays.asList( "org.apache.maven.reporting.MavenReport",
                                                                 "org.apache.maven.reporting.MavenMultiPageReport",
                                                                 "org.apache.maven.doxia.siterenderer.Renderer",
                                                                 "org.apache.maven.doxia.sink.SinkFactory",
                                                                 "org.codehaus.doxia.sink.Sink",
                                                                 "org.apache.maven.doxia.sink.Sink",
                                                                 "org.apache.maven.doxia.sink.SinkEventAttributes" );

    protected static final ExclusionsDependencyFilter EXCLUDES =
        new ExclusionsDependencyFilter( Arrays.asList( "doxia-site-renderer", "doxia-sink-api", "maven-reporting-api" ) );

    public List<MavenReportExecution> buildMavenReports( MavenReportExecutorRequest mavenReportExecutorRequest )
        throws MojoExecutionException
    {
        if ( mavenReportExecutorRequest.getReportPlugins() == null )
        {
            return Collections.emptyList();
        }
        getLog().debug( "DefaultMavenReportExecutor.buildMavenReports()" );

        RepositoryRequest repositoryRequest = new DefaultRepositoryRequest();
        repositoryRequest.setLocalRepository( mavenReportExecutorRequest.getLocalRepository() );
        repositoryRequest.setRemoteRepositories( mavenReportExecutorRequest.getProject().getPluginArtifactRepositories() );

        MavenSession session = mavenReportExecutorRequest.getMavenSession();
        List<String> reportPluginKeys = new ArrayList<String>();
        List<MavenReportExecution> reports = new ArrayList<MavenReportExecution>();

        String pluginKey = "";
        try
        {
            for ( ReportPlugin reportPlugin : mavenReportExecutorRequest.getReportPlugins() )
            {
                Plugin plugin = new Plugin();
                plugin.setGroupId( reportPlugin.getGroupId() );
                plugin.setArtifactId( reportPlugin.getArtifactId() );

                pluginKey = reportPlugin.getGroupId() + ":" + reportPlugin.getArtifactId();
                if ( reportPluginKeys.contains( pluginKey ) )
                {
                    logger.info( "plugin " + pluginKey + " will be executed more than one time" );
                }
                else
                {
                    reportPluginKeys.add( pluginKey );
                }

                plugin.setVersion( getPluginVersion( reportPlugin, repositoryRequest, mavenReportExecutorRequest ) );
                mergePluginToReportPlugin( mavenReportExecutorRequest, plugin, reportPlugin );

                logger.info( "configuring report plugin " + plugin.getId() );

                Map<String, PlexusConfiguration> goalsWithConfiguration = new TreeMap<String, PlexusConfiguration>();

                List<RemoteRepository> remoteRepositories = session.getCurrentProject().getRemotePluginRepositories();

                PluginDescriptor pluginDescriptor = mavenPluginManager
                    .getPluginDescriptor( plugin, remoteRepositories, session.getRepositorySession() );

                if ( reportPlugin.getReportSets().isEmpty() && reportPlugin.getReports().isEmpty() )
                {
                    List<MojoDescriptor> mojoDescriptors = pluginDescriptor.getMojos();
                    for ( MojoDescriptor mojoDescriptor : mojoDescriptors )
                    {
                        goalsWithConfiguration.put( mojoDescriptor.getGoal(), mojoDescriptor.getConfiguration() );
                    }
                }
                else
                {
                    if ( reportPlugin.getReportSets() != null )
                    {
                        for ( ReportSet reportSet : reportPlugin.getReportSets() )
                        {
                            for ( String report : reportSet.getReports() )
                            {
                                goalsWithConfiguration.put( report, reportSet.getConfiguration() );
                            }
                        }
                    }
                    if ( !reportPlugin.getReports().isEmpty() )
                    {
                        for ( String report : reportPlugin.getReports() )
                        {
                            goalsWithConfiguration.put( report, reportPlugin.getConfiguration() );
                        }
                    }
                }

                for ( Entry<String, PlexusConfiguration> entry : goalsWithConfiguration.entrySet() )
                {
                    MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( entry.getKey() );
                    if ( mojoDescriptor == null )
                    {
                        throw new MojoNotFoundException( entry.getKey(), pluginDescriptor );
                    }

                    MojoExecution mojoExecution = new MojoExecution( plugin, entry.getKey(), "report:" + entry.getKey() );

                    mojoExecution.setConfiguration( convert( mojoDescriptor ) );

                    if ( reportPlugin.getConfiguration() != null || entry.getValue() != null )
                    {
                        Xpp3Dom reportConfiguration =
                            reportPlugin.getConfiguration() == null ? new Xpp3Dom( "fake" )
                                            : convert( reportPlugin.getConfiguration() );

                        // MSITE-512 configuration from ReportSet must win
                        Xpp3Dom mergedConfigurationWithReportSet = Xpp3DomUtils
                            .mergeXpp3Dom( convert( entry.getValue() ), reportConfiguration );

                        Xpp3Dom mergedConfiguration = Xpp3DomUtils.mergeXpp3Dom( mergedConfigurationWithReportSet,
                                                                                 convert( mojoDescriptor ) );

                        Xpp3Dom cleanedConfiguration = new Xpp3Dom( "configuration" );
                        if ( mergedConfiguration.getChildren() != null )
                        {
                            for ( Xpp3Dom parameter : mergedConfiguration.getChildren() )
                            {
                                if ( mojoDescriptor.getParameterMap().containsKey( parameter.getName() ) )
                                {
                                    cleanedConfiguration.addChild( parameter );
                                }
                            }
                        }
                        if ( getLog().isDebugEnabled() )
                        {
                            getLog().debug( "mojoExecution mergedConfiguration: " + mergedConfiguration );
                            getLog().debug( "mojoExecution cleanedConfiguration: " + cleanedConfiguration );
                        }

                        mojoExecution.setConfiguration( cleanedConfiguration );
                    }                    

                    mojoExecution.setMojoDescriptor( mojoDescriptor );

                    mavenPluginManager.setupPluginRealm( pluginDescriptor,
                                                         mavenReportExecutorRequest.getMavenSession(),
                                                         Thread.currentThread().getContextClassLoader(), IMPORTS,
                                                         EXCLUDES );
                    MavenReport mavenReport =
                        getConfiguredMavenReport( mojoExecution, pluginDescriptor, mavenReportExecutorRequest );

                    if ( mavenReport == null )
                    {
                        continue;
                    }
                    MavenReportExecution mavenReportExecution =
                        new MavenReportExecution( mojoExecution.getPlugin(), mavenReport,
                                                  pluginDescriptor.getClassRealm() );

                    lifecycleExecutor.calculateForkedExecutions( mojoExecution,
                                                                 mavenReportExecutorRequest.getMavenSession() );

                    if ( !mojoExecution.getForkedExecutions().isEmpty() )
                    {
                        lifecycleExecutor.executeForkedExecutions( mojoExecution,
                                                                   mavenReportExecutorRequest.getMavenSession() );
                    }

                    if ( canGenerateReport( mavenReport, mojoExecution ) )
                    {
                        reports.add( mavenReportExecution );
                    }
                }
            }
            return reports;
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "failed to get report for " + pluginKey, e );
        }
    }

    private boolean canGenerateReport( MavenReport mavenReport, MojoExecution mojoExecution )
    {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader( mojoExecution.getMojoDescriptor().getRealm() );

            return mavenReport.canGenerateReport();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( originalClassLoader );
        } 
    }

    private MavenReport getConfiguredMavenReport( MojoExecution mojoExecution, PluginDescriptor pluginDescriptor,
                                                  MavenReportExecutorRequest mavenReportExecutorRequest )
        throws PluginContainerException, PluginConfigurationException
    {
        try
        {
            if ( !isMavenReport( mojoExecution, pluginDescriptor ) )
            {
                return null;
            }

            Mojo mojo = mavenPluginManager.getConfiguredMojo( Mojo.class,
                                                              mavenReportExecutorRequest.getMavenSession(),
                                                              mojoExecution );

            return (MavenReport) mojo;
        }
        catch ( ClassCastException e )
        {
            getLog().warn( "skip ClassCastException " + e.getMessage() );
            return null;
        }
        catch ( PluginContainerException e )
        {
            /**
             * ignore old plugin which are using removed PluginRegistry
             * [INFO] Caused by: java.lang.NoClassDefFoundError: org/apache/maven/plugin/registry/PluginRegistry
             */
            if ( e.getCause() != null && e.getCause() instanceof NoClassDefFoundError
                && e.getMessage().contains( "PluginRegistry" ) )
            {
                getLog().warn( "skip NoClassDefFoundError with PluginRegistry " );
                // too noisy, only in debug mode + e.getMessage() );
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( e.getMessage(), e );
                }
                return null;
            }
            throw e;
        }
    }

    private boolean isMavenReport( MojoExecution mojoExecution, PluginDescriptor pluginDescriptor )
    {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Class<?> mojoClass;
        Thread.currentThread().setContextClassLoader( mojoExecution.getMojoDescriptor().getRealm() );
        try
        {
            mojoClass =
                pluginDescriptor.getClassRealm().loadClass( mojoExecution.getMojoDescriptor().getImplementation() );
        }
        catch ( ClassNotFoundException e )
        {
            getLog().warn( "skip ClassNotFoundException mojoExecution.goal : " + mojoExecution.getGoal() + " : "
                               + e.getMessage(), e );
            return false;
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( originalClassLoader );
        }       

        try
        {
            Thread.currentThread().setContextClassLoader( mojoExecution.getMojoDescriptor().getRealm() );
            MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( mojoExecution.getGoal() );

            boolean isMavenReport = MavenReport.class.isAssignableFrom( mojoClass );

            if ( getLog().isDebugEnabled() )
            {
                if ( mojoDescriptor != null && mojoDescriptor.getImplementationClass() != null )
                {
                    getLog().debug( "class " + mojoDescriptor.getImplementationClass().getName() + " isMavenReport: "
                                        + isMavenReport );
                }

                if ( !isMavenReport )
                {
                    getLog().debug( "skip non MavenReport " + mojoExecution.getMojoDescriptor().getId() );
                }
            }

            return isMavenReport;
        }
        catch ( LinkageError e )
        {
            getLog().warn( "skip LinkageError mojoExecution.goal : " + mojoExecution.getGoal() + " : " + e.getMessage(),
                           e );
            return false;
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( originalClassLoader );
        }
    }

    private Xpp3Dom convert( MojoDescriptor mojoDescriptor )
    {
        PlexusConfiguration config = mojoDescriptor.getMojoConfiguration();
        return ( config != null ) ? convert( config ) : new Xpp3Dom( "configuration" );
    }

    private Xpp3Dom convert( PlexusConfiguration config )
    {
        if ( config == null )
        {
            return null;
        }

        Xpp3Dom dom = new Xpp3Dom( config.getName() );
        dom.setValue( config.getValue( null ) );

        for ( String attrib : config.getAttributeNames() )
        {
            dom.setAttribute( attrib, config.getAttribute( attrib, null ) );
        }

        for ( int n = config.getChildCount(), i = 0; i < n; i++ )
        {
            dom.addChild( convert( config.getChild( i ) ) );
        }

        return dom;
    }

    private Logger getLog()
    {
        return logger;
    }

    /**
     * Resolve report plugin version.
     * These steps are followed, stopping if a non <code>null</code> value has been found:
     * <ol>
     *   <li>use the one defined in the <code>reportPlugin</code> configuration,</li>
     *   <li>search similar (same groupId and artifactId) mojo in the <code>build/plugins</code> section of the pom,</li>
     *   <li>search similar (same groupId and artifactId) mojo in the <code>build/pluginManagement</code> section of the pom,</li>
     *   <li>ask {@link PluginVersionResolver} to get a version and display a warning as it's not a recommended use.</li>  
     * </ol>
     *
     * @param reportPlugin the report plugin to resolve the version
     * @param repositoryRequest TODO: unused, to be removed?
     * @param mavenReportExecutorRequest the current report execution context
     * @return the report plugin version
     * @throws PluginVersionResolutionException
     */
    protected String getPluginVersion( ReportPlugin reportPlugin, RepositoryRequest repositoryRequest,
                                       MavenReportExecutorRequest mavenReportExecutorRequest )
        throws PluginVersionResolutionException
    {
        String reportPluginKey = reportPlugin.getGroupId() + ':' + reportPlugin.getArtifactId();
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "resolving version for " + reportPluginKey );
        }

        // look for version defined in the reportPlugin configuration
        if ( reportPlugin.getVersion() != null )
        {
            if ( getLog().isDebugEnabled() )
            {
                logger.debug( "resolved " + reportPluginKey + " version from the reporting.plugins section: "
                    + reportPlugin.getVersion() );
            }
            return reportPlugin.getVersion();
        }

        MavenProject project = mavenReportExecutorRequest.getProject();

        // search in the build section
        if ( project.getBuild() != null )
        {
            Plugin plugin = find( reportPlugin, project.getBuild().getPlugins() );

            if ( plugin != null && plugin.getVersion() != null )
            {
                if ( getLog().isDebugEnabled() )
                {
                    logger.debug( "resolved " + reportPluginKey + " version from the build.plugins section: "
                        + plugin.getVersion() );
                }
                return plugin.getVersion();
            }
        }

        // search in pluginManagement section
        if ( project.getBuild() != null && project.getBuild().getPluginManagement() != null )
        {
            Plugin plugin = find( reportPlugin, project.getBuild().getPluginManagement().getPlugins() );

            if ( plugin != null && plugin.getVersion() != null )
            {
                if ( getLog().isDebugEnabled() )
                {
                    logger.debug( "resolved " + reportPluginKey
                        + " version from the build.pluginManagement.plugins section: " + plugin.getVersion() );
                }
                return plugin.getVersion();
            }
        }


        logger.warn( "Report plugin " + reportPluginKey + " has an empty version." );
        logger.warn( "" );
        logger.warn( "It is highly recommended to fix these problems"
            + " because they threaten the stability of your build." );
        logger.warn( "" );
        logger.warn( "For this reason, future Maven versions might no"
            + " longer support building such malformed projects." );

        Plugin plugin = new Plugin();
        plugin.setGroupId( reportPlugin.getGroupId() );
        plugin.setArtifactId( reportPlugin.getArtifactId() );
        
        PluginVersionRequest pluginVersionRequest =
            new DefaultPluginVersionRequest( plugin, mavenReportExecutorRequest.getMavenSession() );

        PluginVersionResult result = pluginVersionResolver.resolve( pluginVersionRequest );
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "resolved " + reportPluginKey + " version from repository: " + result.getVersion() );
        }
        return result.getVersion();
    }

    /**
     * Search similar (same groupId and artifactId) mojo as a given report plugin.
     * 
     * @param reportPlugin the report plugin to search for a similar mojo
     * @param plugins the candidate mojos 
     * @return the first similar mojo
     */
    private Plugin find( ReportPlugin reportPlugin, List<Plugin> plugins )
    {
        if ( plugins == null )
        {
            return null;
        }
        for ( Plugin plugin : plugins )
        {
            if ( StringUtils.equals( plugin.getArtifactId(), reportPlugin.getArtifactId() )
                && StringUtils.equals( plugin.getGroupId(), reportPlugin.getGroupId() ) )
            {
                return plugin;
            }
        }
        return null;
    }

    /**
     * TODO other stuff to merge ?
     * <p>
     * this method will "merge" some part of the plugin declaration existing in the build section 
     * to the fake plugin build for report execution:
     * <ul>
     *   <li>dependencies</li>
     * </ul>
     * </p>
     * @param mavenReportExecutorRequest
     * @param buildPlugin
     * @param reportPlugin
     */
    private void mergePluginToReportPlugin( MavenReportExecutorRequest mavenReportExecutorRequest, Plugin buildPlugin,
                                            ReportPlugin reportPlugin )
    {
        Plugin configuredPlugin = find( reportPlugin, mavenReportExecutorRequest.getProject().getBuild().getPlugins() );
        if ( configuredPlugin != null )
        {
            if ( !configuredPlugin.getDependencies().isEmpty() )
            {
                buildPlugin.getDependencies().addAll( configuredPlugin.getDependencies() );
            }
        }
    }   
}
