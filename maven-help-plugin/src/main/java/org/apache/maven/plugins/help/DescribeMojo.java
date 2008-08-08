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
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.maven.BuildFailureException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
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
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.settings.Settings;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Displays a list of the attributes for a Maven Plugin and/or Mojo (Maven plain Old Java Object).
 *
 * @version $Id$
 * @since 2.0
 * @goal describe
 * @requiresProject false
 * @aggregator
 * @see <a href="http://maven.apache.org/general.html#What_is_a_Mojo">What is a Mojo?</a>
 */
public class DescribeMojo
    extends AbstractHelpMojo
{
    /** The default indent size when writing description's Mojo. */
    private static final int INDENT_SIZE = 2;

    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * The Plugin manager instance used to resolve Plugin descriptors.
     *
     * @component role="org.apache.maven.plugin.PluginManager"
     */
    private PluginManager pluginManager;

    /**
     * The project builder instance used to retrieve the super-project instance
     * in the event there is no current MavenProject instance. Some MavenProject
     * instance has to be present to use in the plugin manager APIs.
     *
     * @component role="org.apache.maven.project.MavenProjectBuilder"
     */
    private MavenProjectBuilder projectBuilder;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The current project, if there is one. This is listed as optional, since
     * the help plugin should be able to function on its own. If this
     * parameter is empty at execution time, this Mojo will instead use the
     * super-project.
     *
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;

    /**
     * The current user system settings for use in Maven. This is used for
     * plugin manager API calls.
     *
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    /**
     * The current build session instance. This is used for
     * plugin manager API calls.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    /**
     * The local repository ArtifactRepository instance. This is used
     * for plugin manager API calls.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * The Maven Plugin to describe. This must be specified in one of three ways:
     * <br/>
     * <ol>
     * <li>plugin-prefix, i.e. 'help'</li>
     * <li>groupId:artifactId, i.e. 'org.apache.maven.plugins:maven-help-plugin'</li>
     * <li>groupId:artifactId:version, i.e. 'org.apache.maven.plugins:maven-help-plugin:2.0'</li>
     * </ol>
     *
     * @parameter expression="${plugin}" alias="prefix"
     */
    private String plugin;

    /**
     * The Maven Plugin <code>groupId</code> to describe.
     * <br/>
     * <b>Note</b>: Should be used with <code>artifactId</code> parameter.
     *
     * @parameter expression="${groupId}"
     */
    private String groupId;

    /**
     * The Maven Plugin <code>artifactId</code> to describe.
     * <br/>
     * <b>Note</b>: Should be used with <code>groupId</code> parameter.
     *
     * @parameter expression="${artifactId}"
     */
    private String artifactId;

    /**
     * The Maven Plugin <code>version</code> to describe.
     * <br/>
     * <b>Note</b>: Should be used with <code>groupId/artifactId</code> parameters.
     *
     * @parameter expression="${version}"
     */
    private String version;

    /**
     * The goal name of a Mojo to describe within the specified Maven Plugin.
     * If this parameter is specified, only the corresponding Mojo (goal) will be described, rather than the whole Plugin.
     *
     * @parameter expression="${mojo}"
     */
    private String mojo;

    /**
     * This flag specifies that full (verbose) information should be given.
     *
     * @parameter expression="${full}" default-value="false"
     */
    private boolean full;

    /**
     * This flag specifies that a short list of Mojo information should be given.
     *
     * @parameter expression="${medium}" default-value="false"
     * @since 2.0.2
     */
    private boolean medium;

    /**
     * A Maven command like a single goal or a single phase following the Maven command line:
     * <br/>
     * <code>mvn [options] [&lt;goal(s)&gt;] [&lt;phase(s)&gt;]</code>
     *
     * @parameter expression="${cmd}"
     * @since 2.1
     */
    private String cmd;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
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

        StringBuffer descriptionBuffer = new StringBuffer();

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

            if ( StringUtils.isNotEmpty( mojo ) )
            {
                describeMojo( descriptor.getMojo( mojo ), descriptionBuffer );
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
     * Method to write the Mojo description into the output file
     *
     * @param descriptionBuffer contains the description to be written to the file
     * @throws MojoExecutionException
     */
    private void writeDescription( StringBuffer descriptionBuffer )
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
     * @return  a PluginDescriptor where the plugin description is to be retrieved
     * @throws MojoExecutionException if the plugin could not be verify
     * @throws MojoFailureException if groupId or artifactId is empty
     */
    private PluginDescriptor lookupPluginDescriptor( PluginInfo pi )
        throws MojoExecutionException, MojoFailureException
    {
        PluginDescriptor descriptor = null;

        Plugin forLookup = null;

        if ( StringUtils.isNotEmpty( pi.prefix ) )
        {
            descriptor = pluginManager.getPluginDescriptorForPrefix( pi.prefix );
            if ( descriptor == null )
            {
                forLookup = pluginManager.getPluginDefinitionForPrefix( pi.prefix, session, project );
            }
        }
        else if ( StringUtils.isNotEmpty( pi.groupId ) && StringUtils.isNotEmpty( pi.artifactId ) )
        {
            forLookup = new Plugin();

            forLookup.setGroupId( pi.groupId );
            forLookup.setArtifactId( pi.artifactId );

            if ( StringUtils.isNotEmpty( pi.version ) )
            {
                forLookup.setVersion( pi.version );
            }
        }
        else
        {
            StringBuffer msg = new StringBuffer();
            msg.append( "You must either specify 'groupId' and 'artifactId' both parameters, or a valid 'plugin' " +
                    "parameter. For instance:\n" );
            msg.append( "  # mvn help:describe -Dplugin=org.apache.maven.plugins:maven-help-plugin\n" );
            msg.append( "or\n" );
            msg.append( "  # mvn help:describe -DgroupId=org.apache.maven.plugins -DartifactId=maven-help-plugin\n\n" );
            msg.append( "Try 'mvn help:help -Ddetail=true' for more informations." );

            throw new MojoFailureException( msg.toString() );
        }

        if ( descriptor == null && forLookup != null )
        {
            try
            {
                descriptor = pluginManager.verifyPlugin( forLookup, project, settings, localRepository );
            }
            catch ( ArtifactResolutionException e )
            {
                throw new MojoExecutionException( "Error retrieving plugin descriptor for:\n\ngroupId: '"
                    + groupId + "'\nartifactId: '" + artifactId + "'\nversion: '" + version + "'\n\n", e );
            }
            catch ( PluginManagerException e )
            {
                throw new MojoExecutionException( "Error retrieving plugin descriptor for:\n\ngroupId: '"
                    + groupId + "'\nartifactId: '" + artifactId + "'\nversion: '" + version + "'\n\n", e );
            }
            catch ( PluginVersionResolutionException e )
            {
                throw new MojoExecutionException( "Error retrieving plugin descriptor for:\n\ngroupId: '"
                    + groupId + "'\nartifactId: '" + artifactId + "'\nversion: '" + version + "'\n\n", e );
            }
            catch ( ArtifactNotFoundException e )
            {
                throw new MojoExecutionException( "Plugin dependency does not exist: " + e.getMessage(), e );
            }
            catch ( InvalidVersionSpecificationException e )
            {
                throw new MojoExecutionException( "Error retrieving plugin descriptor for:\n\ngroupId: '"
                    + groupId + "'\nartifactId: '" + artifactId + "'\nversion: '" + version + "'\n\n", e );
            }
            catch ( InvalidPluginException e )
            {
                throw new MojoExecutionException( "Error retrieving plugin descriptor for:\n\ngroupId: '"
                    + groupId + "'\nartifactId: '" + artifactId + "'\nversion: '" + version + "'\n\n", e );
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
     * <code>groupId:artifactId[:version]</code>
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
                        pi.prefix = pluginParts[0];
                        break;
                    case ( 2 ):
                        pi.groupId = pluginParts[0];
                        pi.artifactId = pluginParts[1];
                        break;
                    case ( 3 ):
                        pi.groupId = pluginParts[0];
                        pi.artifactId = pluginParts[1];
                        pi.version = pluginParts[2];
                        break;
                    default:
                        throw new MojoFailureException( "plugin parameter must be a plugin prefix,"
                            + " or conform to: 'groupId:artifactId[:version]'." );
                }
            }
            else
            {
                pi.prefix = plugin;
            }
        }
        else
        {
            pi.groupId = groupId;
            pi.artifactId = artifactId;
            pi.version = version;
        }
    }

    /**
     * Method for retrieving the plugin description
     *
     * @param pd contains the plugin description
     * @param buffer contains the information to be displayed or printed
     * @throws MojoFailureException if any reflection exceptions occur.
     * @throws MojoExecutionException if any
     */
    private void describePlugin( PluginDescriptor pd, StringBuffer buffer )
        throws MojoFailureException, MojoExecutionException
    {
        String name = pd.getName();
        if ( name == null )
        {
            name = pd.getId();
        }

        append( buffer, name, 0 );
        append( buffer, "Group Id", pd.getGroupId(), 0 );
        append( buffer, "Artifact Id", pd.getArtifactId(), 0 );
        append( buffer, "Version", pd.getVersion(), 0 );
        append( buffer, "Goal Prefix", pd.getGoalPrefix(), 0 );
        appendAsParagraph( buffer, "Description", toDescription( pd.getDescription() ), 0 );
        buffer.append( "\n" );

        if ( full || medium )
        {
            append( buffer, "This plugin has " + pd.getMojos().size() + " goals:", 0 );
            buffer.append( "\n" );

            for ( Iterator it = pd.getMojos().iterator(); it.hasNext(); )
            {
                MojoDescriptor md = (MojoDescriptor) it.next();

                if ( full )
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
        else
        {
            buffer.append( "For more information, run 'mvn help:describe [...] -Dfull'" );
            buffer.append( "\n" );
        }
    }

    /**
     * Displays information about the Plugin Mojo
     *
     * @param md contains the description of the Plugin Mojo
     * @param buffer the displayed output
     * @throws MojoFailureException if any reflection exceptions occur.
     * @throws MojoExecutionException if any
     */
    private void describeMojo( MojoDescriptor md, StringBuffer buffer )
        throws MojoFailureException, MojoExecutionException
    {
        buffer.append( "Mojo: '" ).append( md.getFullGoalName() ).append( "'" );
        buffer.append( '\n' );

        describeMojoGuts( md, buffer, full );
        buffer.append( "\n" );

        if ( !( full || medium ) )
        {
            buffer.append( "For more information, run 'mvn help:describe [...] -Dfull'" );
            buffer.append( "\n" );
        }
    }

    /**
     * Displays detailed information about the Plugin Mojo
     *
     * @param md contains the description of the Plugin Mojo
     * @param buffer contains information to be printed or displayed
     * @param fullDescription specifies whether all the details about the Plugin Mojo is to  be displayed
     * @throws MojoFailureException if any reflection exceptions occur.
     * @throws MojoExecutionException if any
     */
    private void describeMojoGuts( MojoDescriptor md, StringBuffer buffer, boolean fullDescription )
        throws MojoFailureException, MojoExecutionException
    {
        append( buffer, "Goal", "'" + md.getGoal() + "'", 0 );

        // indent 1
        append( buffer, "Full Goal Name", "'" + md.getFullGoalName() + "'", 1 );
        appendAsParagraph( buffer, "Description", toDescription( md.getDescription() ), 1 );

        String deprecation = md.getDeprecated();
        if ( StringUtils.isNotEmpty( deprecation ) )
        {
            append( buffer, "NOTE: This mojo is deprecated. " + deprecation, 1 );
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
            append( buffer, "Before this mojo executes, it will call:", 1);

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

        buffer.append( "\n" );

        describeMojoRequirements( md, buffer );
    }

    /**
     * Method for displaying the component requirements of the Plugin Mojo
     *
     * @param md contains the description of the Plugin Mojo
     * @param buffer contains information to be printed or displayed
     * @throws MojoFailureException if any reflection exceptions occur.
     * @throws MojoExecutionException if any
     */
    private void describeMojoRequirements( MojoDescriptor md, StringBuffer buffer )
        throws MojoFailureException, MojoExecutionException
    {
        List reqs = md.getRequirements();

        if ( reqs == null || reqs.isEmpty() )
        {
            append( buffer, "This mojo doesn't have any component requirements.", 1 );
            return;
        }

        append( buffer, "Component Requirements:", 1 );

        // indent 2
        int idx = 0;
        for ( Iterator it = reqs.iterator(); it.hasNext(); idx++ )
        {
            ComponentRequirement req = (ComponentRequirement) it.next();

            buffer.append( "\n" );

            append( buffer, "[" + idx + "] Role", req.getRole(), 2 );

            String hint = req.getRoleHint();
            if ( StringUtils.isNotEmpty( hint ) )
            {
                append( buffer, "Role-Hint", hint, 2 );
            }
        }
    }

    /**
     * Displays parameter information of the Plugin Mojo
     *
     * @param md contains the description of the Plugin Mojo
     * @param buffer contains information to be printed or displayed
     * @throws MojoFailureException if any reflection exceptions occur.
     * @throws MojoExecutionException if any
     */
    private void describeMojoParameters( MojoDescriptor md, StringBuffer buffer )
        throws MojoFailureException, MojoExecutionException
    {
        List params = md.getParameters();

        if ( params == null || params.isEmpty() )
        {
            append( buffer, "This mojo doesn't use any parameters.", 1 );
            return;
        }

        append( buffer, "Parameters:", 1 );

        // indent 2
        int idx = 0;
        for ( Iterator it = params.iterator(); it.hasNext(); )
        {
            Parameter parameter = (Parameter) it.next();

            buffer.append( "\n" );

            append( buffer, "[" + idx++ + "] Name", parameter.getName()
                + ( StringUtils.isEmpty( parameter.getAlias() ) ? "" : " (Alias: " + parameter.getAlias() + ")" ),
                    2 );

            append( buffer, "Type", parameter.getType(), 2 );

            String expression = parameter.getExpression();
            if ( StringUtils.isNotEmpty( expression ) )
            {
                append( buffer, "Expression", expression, 2 );
            }

            String defaultVal = parameter.getDefaultValue();
            if ( StringUtils.isNotEmpty( defaultVal ) )
            {
                append( buffer, "Default value", "'" + defaultVal + "'", 2 );
            }

            append( buffer, "Required", parameter.isRequired() + "", 2 );
            append( buffer, "Directly editable", parameter.isEditable() + "", 2 );

            appendAsParagraph( buffer, "Description", toDescription( parameter.getDescription() ), 2 );

            String deprecation = parameter.getDeprecated();
            if ( StringUtils.isNotEmpty( deprecation ) )
            {
                append( buffer, "NOTE: This parameter is deprecated." + deprecation, 2 );
            }
        }
    }

    /**
     * Describe the <code>cmd</code> parameter
     *
     * @param descriptionBuffer not null
     * @return <code>true</code> if it implies to describe a plugin, <code>false</code> otherwise.
     * @throws MojoFailureException if any reflection exceptions occur or missing components.
     * @throws MojoExecutionException if any
     */
    private boolean describeCommand( StringBuffer descriptionBuffer )
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

                LifecycleMapping lifecycleMapping =
                    (LifecycleMapping) session.lookup( LifecycleMapping.ROLE, project.getPackaging() );
                if ( lifecycle.getDefaultPhases() == null )
                {
                    descriptionBuffer.append( "'" + cmd + "' is a phase corresponding to this plugin:\n" );
                    for ( Iterator it = lifecycle.getPhases().iterator(); it.hasNext(); )
                    {
                        String key = (String) it.next();

                        if ( !key.equals( cmd ) )
                        {
                            continue;
                        }

                        if ( lifecycleMapping.getPhases( "default" ).get( key ) != null )
                        {
                            descriptionBuffer.append( lifecycleMapping.getPhases( "default" ).get( key ) ).append(
                                                                                                                   "\n" );
                        }
                    }

                    descriptionBuffer.append( "\n" );
                    descriptionBuffer.append(
                                              "It is a part of the lifecycle for the POM packaging '"
                                                  + project.getPackaging()
                                                  + "'. This lifecycle includes the following phases:" ).append(
                                                                                                                 "\n" );
                    for ( Iterator it = lifecycle.getPhases().iterator(); it.hasNext(); )
                    {
                        String key = (String) it.next();

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
                            descriptionBuffer.append( "NOT DEFINED" ).append( "\n" );
                        }
                    }
                }
                else
                {
                    descriptionBuffer.append( "'" + cmd + "' is a lifecycle with the following phases: " );
                    descriptionBuffer.append( "\n" );

                    for ( Iterator it = lifecycle.getPhases().iterator(); it.hasNext(); )
                    {
                        String key = (String) it.next();

                        descriptionBuffer.append( "* " + key + ": " );
                        if ( lifecycle.getDefaultPhases().get( key ) != null )
                        {
                            descriptionBuffer.append( lifecycle.getDefaultPhases().get( key ) ).append( "\n" );
                        }
                        else
                        {
                            descriptionBuffer.append( "NOT DEFINED" ).append( "\n" );
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
        MojoDescriptor mojoDescriptor = getMojoDescriptor( cmd, session, project, cmd, true, false );

        descriptionBuffer.append( "'" + cmd + "' is a plugin" ).append( ".\n" );
        plugin = mojoDescriptor.getPluginDescriptor().getId();

        return true;
    }

    /**
     * Invoke the following private method
     * <code>DefaultLifecycleExecutor#getMojoDescriptor(String, MavenSession, MavenProject, String, boolean, boolean)</code>
     *
     * @param task not null
     * @param session not null
     * @param project not null
     * @param invokedVia not null
     * @param canUsePrefix not null
     * @param isOptionalMojo not null
     * @return MojoDescriptor for the task
     * @throws MojoFailureException if any can not invoke the method
     * @throws MojoExecutionException if no descriptor was found for <code>task</code>
     * @see DefaultLifecycleExecutor#getMojoDescriptor(String, MavenSession, MavenProject, String, boolean, boolean)
     */
    private static MojoDescriptor getMojoDescriptor( String task, MavenSession session, MavenProject project,
                                              String invokedVia, boolean canUsePrefix, boolean isOptionalMojo )
        throws MojoFailureException, MojoExecutionException
    {
        try
        {
            DefaultLifecycleExecutor lifecycleExecutor =
                (DefaultLifecycleExecutor) session.lookup( LifecycleExecutor.ROLE );

            Method m =
                lifecycleExecutor.getClass().getDeclaredMethod(
                                                                "getMojoDescriptor",
                                                                new Class[] { String.class, MavenSession.class,
                                                                    MavenProject.class, String.class,
                                                                    Boolean.TYPE, Boolean.TYPE } );
            m.setAccessible( true );
            MojoDescriptor mojoDescriptor =
                (MojoDescriptor) m.invoke( lifecycleExecutor, new Object[] { task, session, project, invokedVia,
                    Boolean.valueOf( canUsePrefix ), Boolean.valueOf( isOptionalMojo ) } );

            if ( mojoDescriptor == null )
            {
                throw new MojoExecutionException( "No MOJO exists for '" + task + "'." );
            }

            return mojoDescriptor;
        }
        catch ( SecurityException e )
        {
            throw new MojoFailureException( "SecurityException: " + e.getMessage() );
        }
        catch ( IllegalArgumentException e )
        {
            throw new MojoFailureException( "IllegalArgumentException: " + e.getMessage() );
        }
        catch ( ComponentLookupException e )
        {
            throw new MojoFailureException( "ComponentLookupException: " + e.getMessage() );
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

            if ( cause instanceof BuildFailureException )
            {
                throw new MojoFailureException( "BuildFailureException: " + cause.getMessage() );
            }
            else if ( cause instanceof LifecycleExecutionException )
            {
                throw new MojoFailureException( "LifecycleExecutionException: " + cause.getMessage() );
            }
            else if ( cause instanceof PluginNotFoundException )
            {
                throw new MojoFailureException( "PluginNotFoundException: " + cause.getMessage() );
            }

            throw new MojoFailureException( "InvocationTargetException: " + e.getMessage() );
        }
    }

    /**
     * Invoke the following private method
     * <code>HelpMojo#toLines(String, int, int, int)</code>
     *
     * @param text The text to split into lines, must not be <code>null</code>.
     * @param indent The base indentation level of each line, must not be negative.
     * @param indentSize The size of each indentation, must not be negative.
     * @param lineLength The length of the line, must not be negative.
     * @return The sequence of display lines, never <code>null</code>.
     * @throws MojoFailureException if any can not invoke the method
     * @throws MojoExecutionException if no line was found for <code>text</code>
     * @see HelpMojo#toLines(String, int, int, int)
     */
    private static List toLines( String text, int indent, int indentSize, int lineLength )
        throws MojoFailureException, MojoExecutionException
    {
        try
        {
            Method m =
                HelpMojo.class.getDeclaredMethod( "toLines", new Class[] { String.class, Integer.TYPE,
                    Integer.TYPE, Integer.TYPE } );
            m.setAccessible( true );
            List output =
                (List) m.invoke( HelpMojo.class, new Object[] { text, new Integer( indent ),
                    new Integer( indentSize ), new Integer( lineLength ) } );

            if ( output == null )
            {
                throw new MojoExecutionException( "No output was exist '." );
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
     * @param sb The buffer to append the description, not <code>null</code>.
     * @param description The description, not <code>null</code>.
     * @param indent The base indentation level of each line, must not be negative.
     * @throws MojoFailureException if any reflection exceptions occur.
     * @throws MojoExecutionException if any
     * @see #toLines(String, int, int, int)
     */
    private static void append( StringBuffer sb, String description, int indent )
        throws MojoFailureException, MojoExecutionException
    {
        if ( StringUtils.isEmpty( description ) )
        {
            sb.append( "Unknown" ).append( '\n' );
            return;
        }

        for ( Iterator it = toLines( description, indent, INDENT_SIZE, LINE_LENGTH ).iterator(); it.hasNext(); )
        {
            sb.append( it.next().toString() ).append( '\n' );
        }
    }

    /**
     * Append a description to the buffer by respecting the indentSize and lineLength parameters.
     * <b>Note</b>: The last character is always a new line.
     *
     * @param sb The buffer to append the description, not <code>null</code>.
     * @param key The key, not <code>null</code>.
     * @param value The value associated to the key, could be <code>null</code>.
     * @param indent The base indentation level of each line, must not be negative.
     * @throws MojoFailureException if any reflection exceptions occur.
     * @throws MojoExecutionException if any
     * @see #toLines(String, int, int, int)
     */
    private static void append( StringBuffer sb, String key, String value, int indent )
        throws MojoFailureException, MojoExecutionException
    {
        if ( StringUtils.isEmpty( key ) )
        {
            throw new IllegalArgumentException( "Key is required!" );
        }

        if ( StringUtils.isEmpty( value ) )
        {
            value = "Unknown";
        }

        String description = key + ": " + value;
        for ( Iterator it = toLines( description, indent, INDENT_SIZE, LINE_LENGTH ).iterator(); it.hasNext(); )
        {
            sb.append( it.next().toString() ).append( '\n' );
        }
    }

    /**
     * Append a description to the buffer by respecting the indentSize and lineLength parameters for the first line,
     * and append the next lines with <code>indent + 1</code> like a paragraph.
     * <b>Note</b>: The last character is always a new line.
     *
     * @param sb The buffer to append the description, not <code>null</code>.
     * @param description The description, not <code>null</code>.
     * @param indent The base indentation level of each line, must not be negative.
     * @throws MojoFailureException if any reflection exceptions occur.
     * @throws MojoExecutionException if any
     * @see #toLines(String, int, int, int)
     */
    private static void appendAsParagraph( StringBuffer sb, String key, String value, int indent )
        throws MojoFailureException, MojoExecutionException
    {
        if ( StringUtils.isEmpty( key ) )
        {
            throw new IllegalArgumentException( "Key is required!" );
        }

        if ( StringUtils.isEmpty( value ) )
        {
            value = "Unknown";
        }

        String description = key + ": " + value;

        List l1 = toLines( description, indent, INDENT_SIZE, LINE_LENGTH - INDENT_SIZE );
        List l2 = toLines( description, indent + 1, INDENT_SIZE, LINE_LENGTH );
        l2.set( 0, l1.get( 0 ) );
        for ( Iterator it = l2.iterator(); it.hasNext(); )
        {
            sb.append( it.next().toString() ).append( '\n' );
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

    protected static class PluginInfo
    {
        String prefix;

        String groupId;

        String artifactId;

        String version;

        String mojo;

        Plugin plugin;

        PluginDescriptor pluginDescriptor;
    }
}
