package org.apache.maven.plugins.help;

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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.DefaultLifecycleExecutor;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Displays a list of the attributes for a Maven Plugin and/or goals (aka Mojo - Maven plain Old Java Object).
 *
 * @version $Id$
 * @see <a href="http://maven.apache.org/general.html#What_is_a_Mojo">What is a Mojo?</a>
 * @since 2.0
 */
@Mojo( name = "describe", requiresProject = false, aggregator = true )
public class DescribeMojo
    extends AbstractHelpMojo
{
    /**
     * The default indent size when writing description's Mojo.
     */
    private static final int INDENT_SIZE = 2;

    /**
     * For unknown values
     */
    private static final String UNKNOWN = "Unknown";

    /**
     * For not defined values
     */
    private static final String NOT_DEFINED = "Not defined";

    /**
     * For deprecated values
     */
    private static final String NO_REASON = "No reason given";
    
    private static final Pattern EXPRESSION = Pattern.compile( "^\\$\\{([^}]+)\\}$" );

    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Maven Artifact Factory component.
     *
     * @since 2.1
     */
    @Component
    private ArtifactFactory artifactFactory;

    /**
     * The Plugin manager instance used to resolve Plugin descriptors.
     */
    @Component( role = PluginManager.class )
    private PluginManager pluginManager;

    /**
     * The project builder instance used to retrieve the super-project instance
     * in the event there is no current MavenProject instance. Some MavenProject
     * instance has to be present to use in the plugin manager APIs.
     */
    @Component( role = MavenProjectBuilder.class )
    private MavenProjectBuilder projectBuilder;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The current project, if there is one. This is listed as optional, since
     * the help plugin should be able to function on its own. If this
     * parameter is empty at execution time, this Mojo will instead use the
     * super-project.
     */
    @Component
    private MavenProject project;

    /**
     * The current build session instance. This is used for
     * plugin manager API calls.
     */
    @Component
    private MavenSession session;

    /**
     * The local repository ArtifactRepository instance. This is used
     * for plugin manager API calls.
     */
    @org.apache.maven.plugins.annotations.Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    private ArtifactRepository localRepository;

    /**
     * Remote repositories used for the project.
     *
     * @since 2.1
     */
    @org.apache.maven.plugins.annotations.Parameter( defaultValue = "${project.remoteArtifactRepositories}",
                    required = true, readonly = true )
    private List<ArtifactRepository> remoteRepositories;

    /**
     * The Maven Plugin to describe. This must be specified in one of three ways:
     * <br/>
     * <ol>
     * <li>plugin-prefix, i.e. 'help'</li>
     * <li>groupId:artifactId, i.e. 'org.apache.maven.plugins:maven-help-plugin'</li>
     * <li>groupId:artifactId:version, i.e. 'org.apache.maven.plugins:maven-help-plugin:2.0'</li>
     * </ol>
     */
    @org.apache.maven.plugins.annotations.Parameter( property = "plugin", alias = "prefix" )
    private String plugin;

    /**
     * The Maven Plugin <code>groupId</code> to describe.
     * <br/>
     * <b>Note</b>: Should be used with <code>artifactId</code> parameter.
     */
    @org.apache.maven.plugins.annotations.Parameter( property = "groupId" )
    private String groupId;

    /**
     * The Maven Plugin <code>artifactId</code> to describe.
     * <br/>
     * <b>Note</b>: Should be used with <code>groupId</code> parameter.
     */
    @org.apache.maven.plugins.annotations.Parameter( property = "artifactId" )
    private String artifactId;

    /**
     * The Maven Plugin <code>version</code> to describe.
     * <br/>
     * <b>Note</b>: Should be used with <code>groupId/artifactId</code> parameters.
     */
    @org.apache.maven.plugins.annotations.Parameter( property = "version" )
    private String version;

    /**
     * The goal name of a Mojo to describe within the specified Maven Plugin.
     * If this parameter is specified, only the corresponding goal (Mojo) will be described,
     * rather than the whole Plugin.
     *
     * @since 2.1, was <code>mojo</code> in 2.0.x
     */
    @org.apache.maven.plugins.annotations.Parameter( property = "goal", alias = "mojo" )
    private String goal;

    /**
     * This flag specifies that a detailed (verbose) list of goal (Mojo) information should be given.
     *
     * @since 2.1, was <code>full</code> in 2.0.x
     */
    @org.apache.maven.plugins.annotations.Parameter( property = "detail", defaultValue = "false", alias = "full" )
    private boolean detail;

    /**
     * This flag specifies that a medium list of goal (Mojo) information should be given.
     *
     * @since 2.0.2
     */
    @org.apache.maven.plugins.annotations.Parameter( property = "medium", defaultValue = "true" )
    private boolean medium;

    /**
     * This flag specifies that a minimal list of goal (Mojo) information should be given.
     *
     * @since 2.1
     */
    @org.apache.maven.plugins.annotations.Parameter( property = "minimal", defaultValue = "false" )
    private boolean minimal;

    /**
     * A Maven command like a single goal or a single phase following the Maven command line:
     * <br/>
     * <code>mvn [options] [&lt;goal(s)&gt;] [&lt;phase(s)&gt;]</code>
     *
     * @since 2.1
     */
    @org.apache.maven.plugins.annotations.Parameter( property = "cmd" )
    private String cmd;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        validateParameters();

        if ( project == null )
        {
            try
            {
                project = projectBuilder.buildStandaloneSuperProject( localRepository );
            }
            catch ( ProjectBuildingException e )
            {
                throw new MojoExecutionException( "Error while retrieving the super-project.", e );
            }
        }

        StringBuilder descriptionBuffer = new StringBuilder();

        boolean describePlugin = true;
        if ( StringUtils.isNotEmpty( cmd ) )
        {
            describePlugin = describeCommand( descriptionBuffer );
        }

        if ( describePlugin )
        {
            PluginInfo pi = new PluginInfo();

            parsePluginLookupInfo( pi );

            PluginDescriptor descriptor = lookupPluginDescriptor( pi );

            if ( StringUtils.isNotEmpty( goal ) )
            {
                MojoDescriptor mojo = descriptor.getMojo( goal );
                if ( mojo == null )
                {
                    throw new MojoFailureException(
                        "The mojo '" + goal + "' does not exist in the plugin '" + pi.getPrefix() + "'" );
                }

                describeMojo( mojo, descriptionBuffer );
            }
            else
            {
                describePlugin( descriptor, descriptionBuffer );
            }
        }

        writeDescription( descriptionBuffer );
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Validate parameters
     */
    private void validateParameters()
    {
        // support legacy parameters "mojo" and "full"
        if ( goal == null && session.getExecutionProperties().get( "mojo" ) != null )
        {
            goal = session.getExecutionProperties().getProperty( "mojo" );
        }

        if ( !detail && session.getExecutionProperties().get( "full" ) != null )
        {
            String full = session.getExecutionProperties().getProperty( "full" );
            detail = new Boolean( full ).booleanValue();
        }

        if ( detail || minimal )
        {
            medium = false;
        }
    }

    /**
     * Method to write the Mojo description into the output file
     *
     * @param descriptionBuffer contains the description to be written to the file
     * @throws MojoExecutionException if any
     */
    private void writeDescription( StringBuilder descriptionBuffer )
        throws MojoExecutionException
    {
        if ( output != null )
        {
            try
            {
                writeFile( output, descriptionBuffer );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot write plugin/mojo description to output: " + output, e );
            }

            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "Wrote descriptions to: " + output );
            }
        }
        else
        {
            if ( getLog().isInfoEnabled() )
            {
                getLog().info( descriptionBuffer.toString() );
            }
        }
    }

    /**
     * Method for retrieving the description of the plugin
     *
     * @param pi holds information of the plugin whose description is to be retrieved
     * @return a PluginDescriptor where the plugin description is to be retrieved
     * @throws MojoExecutionException if the plugin could not be verify
     * @throws MojoFailureException   if groupId or artifactId is empty
     */
    private PluginDescriptor lookupPluginDescriptor( PluginInfo pi )
        throws MojoExecutionException, MojoFailureException
    {
        PluginDescriptor descriptor = null;

        Plugin forLookup = null;

        if ( StringUtils.isNotEmpty( pi.getPrefix() ) )
        {
            descriptor = pluginManager.getPluginDescriptorForPrefix( pi.getPrefix() );
            if ( descriptor == null )
            {
                forLookup = pluginManager.getPluginDefinitionForPrefix( pi.getPrefix(), session, project );
            }
        }
        else if ( StringUtils.isNotEmpty( pi.getGroupId() ) && StringUtils.isNotEmpty( pi.getArtifactId() ) )
        {
            forLookup = new Plugin();

            forLookup.setGroupId( pi.getGroupId() );
            forLookup.setArtifactId( pi.getArtifactId() );

            if ( StringUtils.isNotEmpty( pi.getVersion() ) )
            {
                forLookup.setVersion( pi.getVersion() );
            }
        }
        else
        {
            StringBuilder msg = new StringBuilder();
            msg.append( "You must specify either: both 'groupId' and 'artifactId' parameters OR a 'plugin' parameter"
                            + " OR a 'cmd' parameter. For instance:\n" );
            msg.append( "  # mvn help:describe -Dcmd=install\n" );
            msg.append( "or\n" );
            msg.append( "  # mvn help:describe -Dcmd=help:describe\n" );
            msg.append( "or\n" );
            msg.append( "  # mvn help:describe -Dplugin=org.apache.maven.plugins:maven-help-plugin\n" );
            msg.append( "or\n" );
            msg.append( "  # mvn help:describe -DgroupId=org.apache.maven.plugins -DartifactId=maven-help-plugin\n\n" );
            msg.append( "Try 'mvn help:help -Ddetail=true' for more information." );

            throw new MojoFailureException( msg.toString() );
        }

        if ( descriptor == null && forLookup != null )
        {
            try
            {
                descriptor = pluginManager.loadPluginDescriptor( forLookup, project, session );
            }
            catch ( ArtifactResolutionException e )
            {
                throw new MojoExecutionException(
                    "Error retrieving plugin descriptor for:\n\ngroupId: '" + groupId + "'\nartifactId: '" + artifactId
                        + "'\nversion: '" + version + "'\n\n", e );
            }
            catch ( PluginManagerException e )
            {
                throw new MojoExecutionException(
                    "Error retrieving plugin descriptor for:\n\ngroupId: '" + groupId + "'\nartifactId: '" + artifactId
                        + "'\nversion: '" + version + "'\n\n", e );
            }
            catch ( PluginVersionResolutionException e )
            {
                throw new MojoExecutionException(
                    "Error retrieving plugin descriptor for:\n\ngroupId: '" + groupId + "'\nartifactId: '" + artifactId
                        + "'\nversion: '" + version + "'\n\n", e );
            }
            catch ( ArtifactNotFoundException e )
            {
                throw new MojoExecutionException( "Plugin dependency does not exist: " + e.getMessage(), e );
            }
            catch ( InvalidVersionSpecificationException e )
            {
                throw new MojoExecutionException(
                    "Error retrieving plugin descriptor for:\n\ngroupId: '" + groupId + "'\nartifactId: '" + artifactId
                        + "'\nversion: '" + version + "'\n\n", e );
            }
            catch ( InvalidPluginException e )
            {
                throw new MojoExecutionException(
                    "Error retrieving plugin descriptor for:\n\ngroupId: '" + groupId + "'\nartifactId: '" + artifactId
                        + "'\nversion: '" + version + "'\n\n", e );
            }
            catch ( PluginNotFoundException e )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "Unable to find plugin", e );
                }
                throw new MojoFailureException( "Plugin does not exist: " + e.getMessage() );
            }
            catch ( PluginVersionNotFoundException e )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "Unable to find plugin version", e );
                }
                throw new MojoFailureException( e.getMessage() );
            }
        }

        if ( descriptor == null )
        {
            throw new MojoFailureException( "Plugin could not be found. If you believe it is correct,"
                                                + " check your pluginGroups setting, and run with -U to update the remote configuration" );
        }

        return descriptor;
    }

    /**
     * Method for parsing the plugin parameter
     *
     * @param pi contains information about the plugin whose description is to be retrieved
     * @throws MojoFailureException if <code>plugin<*code> parameter is not conform to
     *                              <code>groupId:artifactId[:version]</code>
     */
    private void parsePluginLookupInfo( PluginInfo pi )
        throws MojoFailureException
    {
        if ( StringUtils.isNotEmpty( plugin ) )
        {
            if ( plugin.indexOf( ":" ) > -1 )
            {
                String[] pluginParts = plugin.split( ":" );

                switch ( pluginParts.length )
                {
                    case ( 1 ):
                        pi.setPrefix( pluginParts[0] );
                        break;
                    case ( 2 ):
                        pi.setGroupId( pluginParts[0] );
                        pi.setArtifactId( pluginParts[1] );
                        break;
                    case ( 3 ):
                        pi.setGroupId( pluginParts[0] );
                        pi.setArtifactId( pluginParts[1] );
                        pi.setVersion( pluginParts[2] );
                        break;
                    default:
                        throw new MojoFailureException( "plugin parameter must be a plugin prefix,"
                                                            + " or conform to: 'groupId:artifactId[:version]'." );
                }
            }
            else
            {
                pi.setPrefix( plugin );
            }
        }
        else
        {
            pi.setGroupId( groupId );
            pi.setArtifactId( artifactId );
            pi.setVersion( version );
        }
    }

    /**
     * Method for retrieving the plugin description
     *
     * @param pd     contains the plugin description
     * @param buffer contains the information to be displayed or printed
     * @throws MojoFailureException   if any reflection exceptions occur.
     * @throws MojoExecutionException if any
     */
    private void describePlugin( PluginDescriptor pd, StringBuilder buffer )
        throws MojoFailureException, MojoExecutionException
    {
        append( buffer, pd.getId(), 0 );
        buffer.append( "\n" );

        String name = pd.getName();
        if ( name == null )
        {
            // Always null see MPLUGIN-137
            // TODO remove when maven-plugin-tools-api:2.4.4
            try
            {
                Artifact artifact = artifactFactory.createPluginArtifact( pd.getGroupId(), pd.getArtifactId(),
                                                                          VersionRange.createFromVersion(
                                                                              pd.getVersion() ) );
                MavenProject pluginProject =
                    projectBuilder.buildFromRepository( artifact, remoteRepositories, localRepository );

                name = pluginProject.getName();
            }
            catch ( ProjectBuildingException e )
            {
                // oh well, we tried our best.
                name = pd.getId();
            }
        }
        append( buffer, "Name", name, 0 );
        appendAsParagraph( buffer, "Description", toDescription( pd.getDescription() ), 0 );
        append( buffer, "Group Id", pd.getGroupId(), 0 );
        append( buffer, "Artifact Id", pd.getArtifactId(), 0 );
        append( buffer, "Version", pd.getVersion(), 0 );
        append( buffer, "Goal Prefix", pd.getGoalPrefix(), 0 );
        buffer.append( "\n" );

        @SuppressWarnings( "unchecked" )
        List<MojoDescriptor> mojos = pd.getMojos();

        if ( mojos == null )
        {
            append( buffer, "This plugin has no goals.", 0 );
            return;
        }

        if ( ( detail || medium ) && !minimal )
        {
            append( buffer, "This plugin has " + mojos.size() + " goal" + ( mojos.size() > 1 ? "s" : "" ) + ":", 0 );
            buffer.append( "\n" );

            mojos = new ArrayList<MojoDescriptor>( mojos );
            PluginUtils.sortMojos( mojos );

            for ( MojoDescriptor md : mojos )
            {
                if ( detail )
                {
                    describeMojoGuts( md, buffer, true );
                }
                else
                {
                    describeMojoGuts( md, buffer, false );
                }

                buffer.append( "\n" );
            }
        }

        if ( !detail )
        {
            buffer.append( "For more information, run 'mvn help:describe [...] -Ddetail'" );
            buffer.append( "\n" );
        }
    }

    /**
     * Displays information about the Plugin Mojo
     *
     * @param md     contains the description of the Plugin Mojo
     * @param buffer the displayed output
     * @throws MojoFailureException   if any reflection exceptions occur.
     * @throws MojoExecutionException if any
     */
    private void describeMojo( MojoDescriptor md, StringBuilder buffer )
        throws MojoFailureException, MojoExecutionException
    {
        buffer.append( "Mojo: '" ).append( md.getFullGoalName() ).append( "'" );
        buffer.append( '\n' );

        describeMojoGuts( md, buffer, detail );
        buffer.append( "\n" );

        if ( !detail )
        {
            buffer.append( "For more information, run 'mvn help:describe [...] -Ddetail'" );
            buffer.append( "\n" );
        }
    }

    /**
     * Displays detailed information about the Plugin Mojo
     *
     * @param md              contains the description of the Plugin Mojo
     * @param buffer          contains information to be printed or displayed
     * @param fullDescription specifies whether all the details about the Plugin Mojo is to  be displayed
     * @throws MojoFailureException   if any reflection exceptions occur.
     * @throws MojoExecutionException if any
     */
    private void describeMojoGuts( MojoDescriptor md, StringBuilder buffer, boolean fullDescription )
        throws MojoFailureException, MojoExecutionException
    {
        append( buffer, md.getFullGoalName(), 0 );

        // indent 1
        appendAsParagraph( buffer, "Description", toDescription( md.getDescription() ), 1 );

        String deprecation = md.getDeprecated();
        if ( deprecation != null && deprecation.length() <= 0 )
        {
            deprecation = NO_REASON;
        }

        if ( StringUtils.isNotEmpty( deprecation ) )
        {
            append( buffer, "Deprecated. " + deprecation, 1 );
        }

        if ( !fullDescription )
        {
            return;
        }

        append( buffer, "Implementation", md.getImplementation(), 1 );
        append( buffer, "Language", md.getLanguage(), 1 );

        String phase = md.getPhase();
        if ( StringUtils.isNotEmpty( phase ) )
        {
            append( buffer, "Bound to phase", phase, 1 );
        }

        String eGoal = md.getExecuteGoal();
        String eLife = md.getExecuteLifecycle();
        String ePhase = md.getExecutePhase();

        if ( StringUtils.isNotEmpty( eGoal ) || StringUtils.isNotEmpty( ePhase ) )
        {
            append( buffer, "Before this mojo executes, it will call:", 1 );

            if ( StringUtils.isNotEmpty( eGoal ) )
            {
                append( buffer, "Single mojo", "'" + eGoal + "'", 2 );
            }

            if ( StringUtils.isNotEmpty( ePhase ) )
            {
                String s = "Phase: '" + ePhase + "'";

                if ( StringUtils.isNotEmpty( eLife ) )
                {
                    s += " in Lifecycle Overlay: '" + eLife + "'";
                }

                append( buffer, s, 2 );
            }
        }

        buffer.append( "\n" );

        describeMojoParameters( md, buffer );
    }

    /**
     * Displays parameter information of the Plugin Mojo
     *
     * @param md     contains the description of the Plugin Mojo
     * @param buffer contains information to be printed or displayed
     * @throws MojoFailureException   if any reflection exceptions occur.
     * @throws MojoExecutionException if any
     */
    private void describeMojoParameters( MojoDescriptor md, StringBuilder buffer )
        throws MojoFailureException, MojoExecutionException
    {
        @SuppressWarnings( "unchecked" )
        List<Parameter> params = md.getParameters();

        if ( params == null || params.isEmpty() )
        {
            append( buffer, "This mojo doesn't use any parameters.", 1 );
            return;
        }

        params = new ArrayList<Parameter>( params );
        // TODO remove when maven-plugin-tools-api:2.4.4 is out see PluginUtils.sortMojoParameters()
        Collections.sort( params, new Comparator<Parameter>()
        {
            /** {@inheritDoc} */
            public int compare( Parameter parameter1, Parameter parameter2 )
            {
                return parameter1.getName().compareToIgnoreCase( parameter2.getName() );
            }
        } );

        append( buffer, "Available parameters:", 1 );

        // indent 2
        for ( Parameter parameter : params )
        {
            if ( !parameter.isEditable() )
            {
                continue;
            }

            buffer.append( "\n" );

            // DGF wouldn't it be nice if this worked?
            String defaultVal = parameter.getDefaultValue();
            if ( defaultVal == null )
            {
                // defaultVal is ALWAYS null, this is a bug in PluginDescriptorBuilder (cf. MNG-4941)
                defaultVal =
                    md.getMojoConfiguration().getChild( parameter.getName() ).getAttribute( "default-value", null );
            }

            if ( StringUtils.isNotEmpty( defaultVal ) )
            {
                defaultVal = " (Default: " + defaultVal + ")";
            }
            else
            {
                defaultVal = "";
            }
            append( buffer, parameter.getName() + defaultVal, 2 );

            if ( parameter.isRequired() )
            {
                append( buffer, "Required", "true", 3 );
            }

            String expression = parameter.getExpression();
            if ( StringUtils.isEmpty( expression ) )
            {
                // expression is ALWAYS null, this is a bug in PluginDescriptorBuilder (cf. MNG-4941).
                // Fixed with Maven-3.0.1
                expression = md.getMojoConfiguration().getChild( parameter.getName() ).getValue( null );
            }
            if ( StringUtils.isNotEmpty( expression ) )
            {
                Matcher matcher = EXPRESSION.matcher( expression );
                if ( matcher.matches() )
                {
                    append( buffer, "User property", matcher.group( 1 ), 3 );
                }
                else
                {
                    append( buffer, "Expression", expression, 3 );
                }
            }

            append( buffer, toDescription( parameter.getDescription() ), 3 );

            String deprecation = parameter.getDeprecated();
            if ( deprecation != null && deprecation.length() <= 0 )
            {
                deprecation = NO_REASON;
            }

            if ( StringUtils.isNotEmpty( deprecation ) )
            {
                append( buffer, "Deprecated. " + deprecation, 3 );
            }
        }
    }

    /**
     * Describe the <code>cmd</code> parameter
     *
     * @param descriptionBuffer not null
     * @return <code>true</code> if it implies to describe a plugin, <code>false</code> otherwise.
     * @throws MojoFailureException   if any reflection exceptions occur or missing components.
     * @throws MojoExecutionException if any
     */
    private boolean describeCommand( StringBuilder descriptionBuffer )
        throws MojoFailureException, MojoExecutionException
    {
        if ( cmd.indexOf( ":" ) == -1 )
        {
            // phase
            try
            {
                DefaultLifecycleExecutor lifecycleExecutor =
                    (DefaultLifecycleExecutor) session.lookup( LifecycleExecutor.ROLE );

                Lifecycle lifecycle = (Lifecycle) lifecycleExecutor.getPhaseToLifecycleMap().get( cmd );
                if ( lifecycle == null )
                {
                    throw new MojoExecutionException( "The given phase '" + cmd + "' is an unknown phase." );
                }

                LifecycleMapping lifecycleMapping =
                    (LifecycleMapping) session.lookup( LifecycleMapping.ROLE, project.getPackaging() );
                @SuppressWarnings( "unchecked" )
                List<String> phases = lifecycle.getPhases();

                if ( lifecycle.getDefaultPhases() == null )
                {
                    descriptionBuffer.append( "'" + cmd + "' is a phase corresponding to this plugin:\n" );
                    for ( String key : phases )
                    {
                        if ( !key.equals( cmd ) )
                        {
                            continue;
                        }

                        if ( lifecycleMapping.getPhases( "default" ).get( key ) != null )
                        {
                            descriptionBuffer.append( lifecycleMapping.getPhases( "default" ).get( key ) );
                            descriptionBuffer.append( "\n" );
                        }
                    }

                    descriptionBuffer.append( "\n" );
                    descriptionBuffer.append(
                        "It is a part of the lifecycle for the POM packaging '" + project.getPackaging()
                            + "'. This lifecycle includes the following phases:" );
                    descriptionBuffer.append( "\n" );
                    for ( String key : phases )
                    {
                        descriptionBuffer.append( "* " + key + ": " );
                        String value = (String) lifecycleMapping.getPhases( "default" ).get( key );
                        if ( StringUtils.isNotEmpty( value ) )
                        {
                            for ( StringTokenizer tok = new StringTokenizer( value, "," ); tok.hasMoreTokens(); )
                            {
                                descriptionBuffer.append( tok.nextToken().trim() );

                                if ( !tok.hasMoreTokens() )
                                {
                                    descriptionBuffer.append( "\n" );
                                }
                                else
                                {
                                    descriptionBuffer.append( ", " );
                                }
                            }
                        }
                        else
                        {
                            descriptionBuffer.append( NOT_DEFINED ).append( "\n" );
                        }
                    }
                }
                else
                {
                    descriptionBuffer.append( "'" + cmd + "' is a lifecycle with the following phases: " );
                    descriptionBuffer.append( "\n" );

                    for ( String key : phases )
                    {
                        descriptionBuffer.append( "* " + key + ": " );
                        if ( lifecycle.getDefaultPhases().get( key ) != null )
                        {
                            descriptionBuffer.append( lifecycle.getDefaultPhases().get( key ) ).append( "\n" );
                        }
                        else
                        {
                            descriptionBuffer.append( NOT_DEFINED ).append( "\n" );
                        }
                    }
                }
            }
            catch ( ComponentLookupException e )
            {
                throw new MojoFailureException( "ComponentLookupException: " + e.getMessage() );
            }
            catch ( LifecycleExecutionException e )
            {
                throw new MojoFailureException( "LifecycleExecutionException: " + e.getMessage() );
            }

            return false;
        }

        // goals
        MojoDescriptor mojoDescriptor = HelpUtil.getMojoDescriptor( cmd, session, project, cmd, true, false );

        descriptionBuffer.append( "'" + cmd + "' is a plugin goal (aka mojo)" ).append( ".\n" );
        plugin = mojoDescriptor.getPluginDescriptor().getId();
        goal = mojoDescriptor.getGoal();

        return true;
    }

    /**
     * Invoke the following private method
     * <code>HelpMojo#toLines(String, int, int, int)</code>
     *
     * @param text       The text to split into lines, must not be <code>null</code>.
     * @param indent     The base indentation level of each line, must not be negative.
     * @param indentSize The size of each indentation, must not be negative.
     * @param lineLength The length of the line, must not be negative.
     * @return The sequence of display lines, never <code>null</code>.
     * @throws MojoFailureException   if any can not invoke the method
     * @throws MojoExecutionException if no line was found for <code>text</code>
     * @see HelpMojo#toLines(String, int, int, int)
     */
    private static List<String> toLines( String text, int indent, int indentSize, int lineLength )
        throws MojoFailureException, MojoExecutionException
    {
        try
        {
            Method m = HelpMojo.class.getDeclaredMethod( "toLines",
                                                         new Class[]{ String.class, Integer.TYPE, Integer.TYPE,
                                                             Integer.TYPE } );
            m.setAccessible( true );
            List<String> output = (List<String>) m.invoke( HelpMojo.class,
                                           new Object[]{ text, Integer.valueOf( indent ), Integer.valueOf( indentSize ),
                                               Integer.valueOf( lineLength ) } );

            if ( output == null )
            {
                throw new MojoExecutionException( "No output was specified." );
            }

            return output;
        }
        catch ( SecurityException e )
        {
            throw new MojoFailureException( "SecurityException: " + e.getMessage() );
        }
        catch ( IllegalArgumentException e )
        {
            throw new MojoFailureException( "IllegalArgumentException: " + e.getMessage() );
        }
        catch ( NoSuchMethodException e )
        {
            throw new MojoFailureException( "NoSuchMethodException: " + e.getMessage() );
        }
        catch ( IllegalAccessException e )
        {
            throw new MojoFailureException( "IllegalAccessException: " + e.getMessage() );
        }
        catch ( InvocationTargetException e )
        {
            Throwable cause = e.getCause();

            if ( cause instanceof NegativeArraySizeException )
            {
                throw new MojoFailureException( "NegativeArraySizeException: " + cause.getMessage() );
            }

            throw new MojoFailureException( "InvocationTargetException: " + e.getMessage() );
        }
    }

    /**
     * Append a description to the buffer by respecting the indentSize and lineLength parameters.
     * <b>Note</b>: The last character is always a new line.
     *
     * @param sb          The buffer to append the description, not <code>null</code>.
     * @param description The description, not <code>null</code>.
     * @param indent      The base indentation level of each line, must not be negative.
     * @throws MojoFailureException   if any reflection exceptions occur.
     * @throws MojoExecutionException if any
     * @see #toLines(String, int, int, int)
     */
    private static void append( StringBuilder sb, String description, int indent )
        throws MojoFailureException, MojoExecutionException
    {
        if ( StringUtils.isEmpty( description ) )
        {
            sb.append( UNKNOWN ).append( '\n' );
            return;
        }

        for ( String line : toLines( description, indent, INDENT_SIZE, LINE_LENGTH ) )
        {
            sb.append( line ).append( '\n' );
        }
    }

    /**
     * Append a description to the buffer by respecting the indentSize and lineLength parameters.
     * <b>Note</b>: The last character is always a new line.
     *
     * @param sb     The buffer to append the description, not <code>null</code>.
     * @param key    The key, not <code>null</code>.
     * @param value  The value associated to the key, could be <code>null</code>.
     * @param indent The base indentation level of each line, must not be negative.
     * @throws MojoFailureException   if any reflection exceptions occur.
     * @throws MojoExecutionException if any
     * @see #toLines(String, int, int, int)
     */
    private static void append( StringBuilder sb, String key, String value, int indent )
        throws MojoFailureException, MojoExecutionException
    {
        if ( StringUtils.isEmpty( key ) )
        {
            throw new IllegalArgumentException( "Key is required!" );
        }

        if ( StringUtils.isEmpty( value ) )
        {
            value = UNKNOWN;
        }

        String description = key + ": " + value;
        for ( String line : toLines( description, indent, INDENT_SIZE, LINE_LENGTH ) )
        {
            sb.append( line ).append( '\n' );
        }
    }

    /**
     * Append a description to the buffer by respecting the indentSize and lineLength parameters for the first line,
     * and append the next lines with <code>indent + 1</code> like a paragraph.
     * <b>Note</b>: The last character is always a new line.
     *
     * @param sb     The buffer to append the description, not <code>null</code>.
     * @param key    The key, not <code>null</code>.
     * @param value  The value, could be <code>null</code>.
     * @param indent The base indentation level of each line, must not be negative.
     * @throws MojoFailureException   if any reflection exceptions occur.
     * @throws MojoExecutionException if any
     * @see #toLines(String, int, int, int)
     */
    private static void appendAsParagraph( StringBuilder sb, String key, String value, int indent )
        throws MojoFailureException, MojoExecutionException
    {
        if ( StringUtils.isEmpty( value ) )
        {
            value = UNKNOWN;
        }

        String description;
        if ( key == null )
        {
            description = value;
        }
        else
        {
            description = key + ": " + value;
        }

        List<String> l1 = toLines( description, indent, INDENT_SIZE, LINE_LENGTH - INDENT_SIZE );
        List<String> l2 = toLines( description, indent + 1, INDENT_SIZE, LINE_LENGTH );
        l2.set( 0, l1.get( 0 ) );
        for ( String line : l2 )
        {
            sb.append( line ).append( '\n' );
        }
    }

    /**
     * Gets the effective string to use for the plugin/mojo/parameter description.
     *
     * @param description The description of the element, may be <code>null</code>.
     * @return The effective description string, never <code>null</code>.
     */
    private static String toDescription( String description )
    {
        if ( StringUtils.isNotEmpty( description ) )
        {
            return PluginUtils.toText( description );
        }

        return "(no description available)";
    }

    /**
     * Class to wrap Plugin information.
     */
    static class PluginInfo
    {
        private String prefix;

        private String groupId;

        private String artifactId;

        private String version;

        private String mojo;

        private Plugin plugin;

        private PluginDescriptor pluginDescriptor;

        /**
         * @return the prefix
         */
        public String getPrefix()
        {
            return prefix;
        }

        /**
         * @param prefix the prefix to set
         */
        public void setPrefix( String prefix )
        {
            this.prefix = prefix;
        }

        /**
         * @return the groupId
         */
        public String getGroupId()
        {
            return groupId;
        }

        /**
         * @param groupId the groupId to set
         */
        public void setGroupId( String groupId )
        {
            this.groupId = groupId;
        }

        /**
         * @return the artifactId
         */
        public String getArtifactId()
        {
            return artifactId;
        }

        /**
         * @param artifactId the artifactId to set
         */
        public void setArtifactId( String artifactId )
        {
            this.artifactId = artifactId;
        }

        /**
         * @return the version
         */
        public String getVersion()
        {
            return version;
        }

        /**
         * @param version the version to set
         */
        public void setVersion( String version )
        {
            this.version = version;
        }

        /**
         * @return the mojo
         */
        public String getMojo()
        {
            return mojo;
        }

        /**
         * @param mojo the mojo to set
         */
        public void setMojo( String mojo )
        {
            this.mojo = mojo;
        }

        /**
         * @return the plugin
         */
        public Plugin getPlugin()
        {
            return plugin;
        }

        /**
         * @param plugin the plugin to set
         */
        public void setPlugin( Plugin plugin )
        {
            this.plugin = plugin;
        }

        /**
         * @return the pluginDescriptor
         */
        public PluginDescriptor getPluginDescriptor()
        {
            return pluginDescriptor;
        }

        /**
         * @param pluginDescriptor the pluginDescriptor to set
         */
        public void setPluginDescriptor( PluginDescriptor pluginDescriptor )
        {
            this.pluginDescriptor = pluginDescriptor;
        }
    }
}
