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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
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
import org.codehaus.plexus.component.repository.ComponentRequirement;

/**
 * Describes the attributes of a plugin and/or plugin mojo.
 *
 * @goal describe
 * @requiresProject false
 * @aggregator
 */
public class DescribeMojo
    extends AbstractMojo
{

    /**
     * The plugin/mojo to describe. This must be specified in one of three ways:
     * <br/>
     * 1. plugin-prefix
     * 2. groupId:artifactId
     * 3. groupId:artifactId:version
     *
     * @parameter expression="${plugin}" alias="prefix"
     */
    private String plugin;

    /**
     * The plugin groupId to describe.
     * (Used with artifactId specification).
     *
     * @parameter expression="${groupId}"
     */
    private String groupId;

    /**
     * The plugin artifactId to describe.
     * (Used with groupId specification).
     *
     * @parameter expression="${artifactId}"
     */
    private String artifactId;

    /**
     * The plugin version to describe.
     * <br/>
     * (Used with groupId/artifactId specification).
     *
     * @parameter expression="${version}"
     */
    private String version;

    /**
     * The goal name of a mojo to describe within the specified plugin.
     * If this parameter is specified, only the corresponding mojo will
     * be described, rather than the whole plugin.
     *
     * @parameter expression="${mojo}"
     */
    private String mojo;

    /**
     * The plugin manager instance used to resolve plugin descriptors.
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

    /**
     * The current project, if there is one. This is listed as optional, since
     * the help plugin should be able to function on its own. If this
     * parameter is empty at execution time, this mojo will instead use the
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
     * If specified, this parameter will cause the plugin/mojo descriptions
     * to be written to the path specified, instead of writing to the console.
     *
     * @parameter expression="${output}"
     */
    private File output;

    /**
     * This flag specifies that full (verbose) information should be
     * given. Use true/false.
     *
     * @parameter expression="${full}" default-value="false"
     */
    private boolean full;

    /**
     * This flag specifies that a short list of mojo information should be
     * given. Use true/false.
     *
     * @parameter expression="${medium}" default-value="false"
     * @since 2.0.2
     */
    private boolean medium;

    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
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

        PluginInfo pi = new PluginInfo();

        parsePluginLookupInfo( pi );

        PluginDescriptor descriptor = lookupPluginDescriptor( pi );

        StringBuffer descriptionBuffer = new StringBuffer();

        if ( mojo != null && mojo.length() > 0 )
        {
            describeMojo( descriptor.getMojo( mojo ), descriptionBuffer );
        }
        else
        {
            describePlugin( descriptor, descriptionBuffer );
        }

        writeDescription( descriptionBuffer );
    }

    /**
     * Method to write the mojo description into the output file
     *
     * @param descriptionBuffer contains the description to be written to the file
     * @throws MojoExecutionException
     */
    private void writeDescription( StringBuffer descriptionBuffer )
        throws MojoExecutionException
    {
        if ( output != null )
        {
            Writer out = null;
            try
            {
                output.getParentFile().mkdirs();

                out = new FileWriter( output );

                out.write( descriptionBuffer.toString() );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot write plugin/mojo description.", e );
            }
            finally
            {
                if ( out != null )
                {
                    try
                    {
                        out.close();
                    }
                    catch ( IOException e )
                    {
                        getLog().debug( "Error closing file output.", e );
                    }
                }
            }

            getLog().info( "Wrote descriptions to: " + output );
        }
        else
        {
            getLog().info( descriptionBuffer.toString() );
        }
    }

    /**
     * Method for retrieving the description of the plugin
     *
     * @param pi    holds information of the plugin whose description is to be retrieved
     * @return  a PluginDescriptor where the plugin description is to be retrieved
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    private PluginDescriptor lookupPluginDescriptor( PluginInfo pi )
        throws MojoExecutionException, MojoFailureException
    {
        PluginDescriptor descriptor = null;

        Plugin forLookup = null;

        if ( pi.prefix != null )
        {
            descriptor = pluginManager.getPluginDescriptorForPrefix( pi.prefix );

            if ( descriptor == null )
            {
                forLookup = pluginManager.getPluginDefinitionForPrefix( pi.prefix, session, project );
            }
        }
        else if ( pi.groupId != null && pi.artifactId != null )
        {
            forLookup = new Plugin();

            forLookup.setGroupId( pi.groupId );
            forLookup.setArtifactId( pi.artifactId );

            if ( pi.version != null )
            {
                forLookup.setVersion( pi.version );
            }
        }
        else
        {
            throw new MojoFailureException(
                "You must either specify \'groupId\' and \'artifactId\', or a valid \'plugin\' parameter." );
        }

        if ( descriptor == null && forLookup != null )
        {
            try
            {
                descriptor = pluginManager.verifyPlugin( forLookup, project, settings, localRepository );
            }
            catch ( ArtifactResolutionException e )
            {
                throw new MojoExecutionException( "Error retrieving plugin descriptor for:\n\ngroupId: \'" + groupId +
                    "\'\nartifactId: \'" + artifactId + "\'\nversion: \'" + version + "\'\n\n", e );
            }
            catch ( PluginManagerException e )
            {
                throw new MojoExecutionException( "Error retrieving plugin descriptor for:\n\ngroupId: \'" + groupId +
                    "\'\nartifactId: \'" + artifactId + "\'\nversion: \'" + version + "\'\n\n", e );
            }
            catch ( PluginVersionResolutionException e )
            {
                throw new MojoExecutionException( "Error retrieving plugin descriptor for:\n\ngroupId: \'" + groupId +
                    "\'\nartifactId: \'" + artifactId + "\'\nversion: \'" + version + "\'\n\n", e );
            }
            catch ( ArtifactNotFoundException e )
            {
                throw new MojoExecutionException( "Plugin dependency does not exist: " + e.getMessage(), e );
            }
            catch ( InvalidVersionSpecificationException e )
            {
                throw new MojoExecutionException( "Error retrieving plugin descriptor for:\n\ngroupId: \'" + groupId +
                    "\'\nartifactId: \'" + artifactId + "\'\nversion: \'" + version + "\'\n\n", e );
            }
            catch ( InvalidPluginException e )
            {
                throw new MojoExecutionException( "Error retrieving plugin descriptor for:\n\ngroupId: \'" + groupId +
                    "\'\nartifactId: \'" + artifactId + "\'\nversion: \'" + version + "\'\n\n", e );
            }
            catch ( PluginNotFoundException e )
            {
                getLog().debug( "Unable to find plugin", e );
                throw new MojoFailureException( "Plugin does not exist: " + e.getMessage() );
            }
            catch ( PluginVersionNotFoundException e )
            {
                getLog().debug( "Unable to find plugin version", e );
                throw new MojoFailureException( e.getMessage() );
            }
        }

        if ( descriptor == null )
        {
            throw new MojoFailureException(
                "Plugin could not be found. If you believe it is correct, check your pluginGroups setting, and run with -U to update the remote configuration" );
        }

        return descriptor;
    }

    /**
     * Method for parsing the plugin parameter
     *
     * @param pi    contains information about the plugin whose description is to be retrieved
     * @throws MojoFailureException
     */
    private void parsePluginLookupInfo( PluginInfo pi )
        throws MojoFailureException
    {
        if ( plugin != null && plugin.length() > 0 )
        {
            if ( plugin.indexOf( ":" ) > -1 )
            {
                String[] pluginParts = plugin.split( ":" );

                switch ( pluginParts.length )
                {
                    case ( 1 ):
                    {
                        pi.prefix = pluginParts[0];
                        break;
                    }
                    case ( 2 ):
                    {
                        pi.groupId = pluginParts[0];
                        pi.artifactId = pluginParts[1];
                        break;
                    }
                    case ( 3 ):
                    {
                        pi.groupId = pluginParts[0];
                        pi.artifactId = pluginParts[1];
                        pi.version = pluginParts[2];
                        break;
                    }
                    default:
                    {
                        throw new MojoFailureException(
                            "plugin parameter must be a plugin prefix, or conform to: 'groupId:artifactId[:version]." );
                    }
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
     * @param pd        contains the plugin description
     * @param buffer    contains the information to be displayed or printed
     */
    private void describePlugin( PluginDescriptor pd, StringBuffer buffer )
    {
        String name = pd.getName();
        if ( name == null )
        {
            name = pd.getId();
        }

        buffer.append( "Plugin: \'" ).append( name ).append( '\'' );
        buffer.append( "\n-----------------------------------------------" );
        buffer.append( "\nGroup Id:  " ).append( pd.getGroupId() );
        buffer.append( "\nArtifact Id: " ).append( pd.getArtifactId() );
        buffer.append( "\nVersion:     " ).append( pd.getVersion() );
        buffer.append( "\nGoal Prefix: " ).append( pd.getGoalPrefix() );

        buffer.append( "\nDescription:\n\n" );
        prettyAppend( formatDescription( pd.getDescription() ), buffer );
        buffer.append( "\n" );

        if ( full || medium )
        {
            buffer.append( "\nMojos:\n" );

            String line = "\n===============================================";

            for ( Iterator it = pd.getMojos().iterator(); it.hasNext(); )
            {
                MojoDescriptor md = (MojoDescriptor) it.next();

                if( full )
                {
                    buffer.append( line );
                    buffer.append( "\nGoal: \'" ).append( md.getGoal() ).append( '\'' );
                    buffer.append( line );

                    describeMojoGuts( md, buffer, true );

                    buffer.append( line );
                    buffer.append( "\n\n" );
                }
                else
                {
                    buffer.append( "\nGoal: \'" ).append( md.getGoal() ).append( '\'' );

                    describeMojoGuts( md, buffer, false );

                    buffer.append( "\n" );
                }
            }
        }
    }

    /**
     * Convenience method for formatting the description.
     *
     * @param description   the plugin description
     * @return a String of the formatted plugin description
     */
    private String formatDescription( String description )
    {
        if ( description == null )
        {
            return null;
        }

        String result = description.replaceAll( " ?\\<br\\/?\\> ?", "\n" );

        result = result.replaceAll( " ?\\<p\\> ?", "" );
        result = result.replaceAll( " ?\\</p\\> ?", "\n\n" );

        return result;
    }

    /**
     * Convenience method for putting the appropriate value to the plugin description
     *
     * @param messagePart   the plugin description
     * @param buffer        contains information to be printed or displayed
     */
    private void prettyAppend( String messagePart, StringBuffer buffer )
    {
        if ( messagePart != null && messagePart.length() > 0 )
        {
            buffer.append( messagePart );
        }
        else
        {
            buffer.append( "Unknown" );
        }
    }

    /**
     * Displays information about the plugin mojo
     *
     * @param md        contains the description of the plugin mojo
     * @param buffer    the displayed output
     */
    private void describeMojo( MojoDescriptor md, StringBuffer buffer )
    {
        String line = "\n===============================================";

        buffer.append( "Mojo: \'" ).append( md.getFullGoalName() ).append( '\'' );
        buffer.append( line );
        buffer.append( "\nGoal: \'" ).append( md.getGoal() ).append( "\'" );

        describeMojoGuts( md, buffer, full );

        buffer.append( line );
        buffer.append( "\n\n" );
    }

    /**
     * Displays detailed information about the plugin mojo
     *
     * @param md                contains the description of the plugin mojo
     * @param buffer            contains information to be printed or displayed
     * @param fullDescription   specifies whether all the details about the plugin mojo is to  be displayed
     */
    private void describeMojoGuts( MojoDescriptor md, StringBuffer buffer, boolean fullDescription )
    {
        buffer.append( "\nDescription:\n" );
        prettyAppend( formatDescription( md.getDescription() ), buffer );
        if( fullDescription )
        {
            buffer.append( "\n" );
        }

        String deprecation = md.getDeprecated();

        if ( deprecation != null )
        {
            buffer.append( "\n\nNOTE: This mojo is deprecated.\n" ).append( deprecation ).append( "\n" );
        }

        if ( fullDescription )
        {
            buffer.append( "\nImplementation: " ).append( md.getImplementation() );
            buffer.append( "\nLanguage: " ).append( md.getLanguage() );

            String phase = md.getPhase();
            if ( phase != null )
            {
                buffer.append( "\nBound to Phase: " ).append( phase );
            }

            String eGoal = md.getExecuteGoal();
            String eLife = md.getExecuteLifecycle();
            String ePhase = md.getExecutePhase();

            if ( eGoal != null || ePhase != null )
            {
                buffer.append( "\n\nBefore this mojo executes, it will call:\n" );

                if ( eGoal != null )
                {
                    buffer.append( "\nSingle mojo: \'" ).append( eGoal ).append( "\'" );
                }

                if ( ePhase != null )
                {
                    buffer.append( "\nPhase: \'" ).append( ePhase ).append( "\'" );

                    if ( eLife != null )
                    {
                        buffer.append( " in Lifecycle Overlay: \'" ).append( eLife ).append( "\'" );
                    }
                }
            }

            describeMojoParameters( md, buffer );

            describeMojoRequirements( md, buffer );
        }
    }

    /**
     * Method for displaying the component requirements of the plugin mojo
     *
     * @param md        contains the description of the plugin mojo
     * @param buffer    contains information to be printed or displayed
     */
    private void describeMojoRequirements( MojoDescriptor md, StringBuffer buffer )
    {
        buffer.append( "\n" );

        List reqs = md.getRequirements();

        if ( reqs == null || reqs.isEmpty() )
        {
            buffer.append( "\nThis mojo doesn't have any component requirements." );
        }
        else
        {
            buffer.append( "\nComponent Requirements:\n" );

            String line = "\n-----------------------------------------------";

            int idx = 0;
            for ( Iterator it = reqs.iterator(); it.hasNext(); idx++ )
            {
                ComponentRequirement req = (ComponentRequirement) it.next();

                buffer.append( line );

                buffer.append( "\n[" ).append( idx ).append( "] " );
                buffer.append( "Role: " ).append( req.getRole() );

                String hint = req.getRoleHint();
                if ( hint != null )
                {
                    buffer.append( "\nRole-Hint: " ).append( hint );
                }

                buffer.append( "\n" );
            }

            buffer.append( line );
        }
    }

    /**
     * Displays parameter information of the plugin mojo
     *
     * @param md        contains the description of the plugin mojo
     * @param buffer    contains information to be printed or displayed
     */
    private void describeMojoParameters( MojoDescriptor md, StringBuffer buffer )
    {
        buffer.append( "\n" );

        List params = md.getParameters();

        if ( params == null || params.isEmpty() )
        {
            buffer.append( "\nThis mojo doesn't use any parameters." );
        }
        else
        {
            buffer.append( "\nParameters:" );

            String line = "\n-----------------------------------------------";

            int idx = 0;
            for ( Iterator it = params.iterator(); it.hasNext(); )
            {
                Parameter parameter = (Parameter) it.next();

                buffer.append( line );
                buffer.append( "\n\n[" ).append( idx++ ).append( "] " );
                buffer.append( "Name: " );
                prettyAppend( parameter.getName(), buffer );

                String alias = parameter.getAlias();
                if ( alias != null )
                {
                    buffer.append( " (Alias: " ).append( alias ).append( ")" );
                }

                buffer.append( "\nType: " );
                prettyAppend( parameter.getType(), buffer );

                String expression = parameter.getExpression();
                if ( expression != null )
                {
                    buffer.append( "\nExpression: " ).append( expression );
                }

                String defaultVal = parameter.getDefaultValue();
                if ( defaultVal != null )
                {
                    buffer.append( "\nDefault value: \'" ).append( defaultVal );
                }

                buffer.append( "\nRequired: " ).append( parameter.isRequired() );
                buffer.append( "\nDirectly editable: " ).append( parameter.isEditable() );

                buffer.append( "\nDescription:\n" );
                prettyAppend( formatDescription( parameter.getDescription() ), buffer );

                String deprecation = parameter.getDeprecated();

                if ( deprecation != null )
                {
                    buffer.append( "\n\nNOTE: This parameter is deprecated.\n" ).append( deprecation ).append( "\n" );
                }

                buffer.append( "\n" );
            }

            buffer.append( line );
        }
    }

    /**
     *
     * @return a String of the plugin parameter value
     */
    public final String getPlugin()
    {
        return plugin;
    }

    /**
     *
     * @param plugin    the plugin value to be set
     */
    public final void setPlugin( String plugin )
    {
        this.plugin = plugin;
    }

    /**
     *
     * @return a PluginManager object
     */
    public final PluginManager getPluginManager()
    {
        return pluginManager;
    }

    /**
     *
     * @param pluginManager the plugin manager to be set
     */
    public final void setPluginManager( PluginManager pluginManager )
    {
        this.pluginManager = pluginManager;
    }

    /**
     *
     * @return a String that contains the artifactId
     */
    public final String getArtifactId()
    {
        return artifactId;
    }

    /**
     *
     * @param artifactId    the artifactId to be set
     */
    public final void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    /**
     *
     * @return a String that contains the groupId
     */
    public final String getGroupId()
    {
        return groupId;
    }

    /**
     *
     * @param groupId   the groupId value to be set
     */
    public final void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    /**
     *
     * @return an ArtifactRepository object of the local repository
     */
    public final ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    /**
     *
     * @param localRepository   the local repository value to be set
     */
    public final void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    /**
     *
     * @return a String of the mojo parameter value
     */
    public final String getMojo()
    {
        return mojo;
    }

    /**
     *
     * @param mojo  the mojo parameter value to be set
     */
    public final void setMojo( String mojo )
    {
        this.mojo = mojo;
    }

    /**
     *
     * @return a File object where the description will be written to
     */
    public final File getOutput()
    {
        return output;
    }

    /**
     *
     * @param output    the output file value to be set
     */
    public final void setOutput( File output )
    {
        this.output = output;
    }

    /**
     *
     * @return a MavenProject object of the current build
     */
    public final MavenProject getProject()
    {
        return project;
    }

    /**
     *
     * @param project   the project value to be set
     */
    public final void setProject( MavenProject project )
    {
        this.project = project;
    }

    public final Settings getSettings()
    {
        return settings;
    }

    public final void setSettings( Settings settings )
    {
        this.settings = settings;
    }

    /**
     *
     * @return a String that contains the value of the version parameter
     */
    public final String getVersion()
    {
        return version;
    }

    /**
     *
     * @param version   the version parameter value to be set
     */
    public final void setVersion( String version )
    {
        this.version = version;
    }

    private static class PluginInfo
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
