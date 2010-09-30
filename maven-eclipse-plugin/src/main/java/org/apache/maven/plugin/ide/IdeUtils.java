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
package org.apache.maven.plugin.ide;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.PropertyUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 * @version $Id$
 */
public class IdeUtils
{
    public static final String JAVA_1_1 = "1.1";

    public static final String JAVA_1_2 = "1.2";

    public static final String JAVA_1_3 = "1.3";

    public static final String JAVA_1_4 = "1.4";

    public static final String JAVA_5_0 = "5.0";

    public static final String JAVA_6_0 = "6.0";

    public static final String PROJECT_NAME_DEFAULT_TEMPLATE = "[artifactId]";

    public static final String PROJECT_NAME_WITH_VERSION_TEMPLATE = "[artifactId]-[version]";

    public static final String PROJECT_NAME_WITH_GROUP_TEMPLATE = "[groupId].[artifactId]";

    public static final String PROJECT_NAME_WITH_GROUP_AND_VERSION_TEMPLATE = "[groupId].[artifactId]-[version]";

    /**
     * compiler plugin id.
     */
    private static final String ARTIFACT_MAVEN_COMPILER_PLUGIN = "maven-compiler-plugin"; //$NON-NLS-1$

    /**
     * 'source' property for maven-compiler-plugin.
     */
    private static final String PROPERTY_SOURCE = "source"; //$NON-NLS-1$

    /**
     * 'encoding' property for maven-compiler-plugin.
     */
    private static final String PROPERTY_ENCODING = "encoding"; //$NON-NLS-1$

    /**
     * 'target' property for maven-compiler-plugin.
     */
    private static final String PROPERTY_TARGET = "target"; //$NON-NLS-1$

    /**
     * The suffix used to mark a file as not available.
     */
    public static final String NOT_AVAILABLE_MARKER_FILE_SUFFIX = "-not-available";

    /**
     * Delete a file, handling log messages and exceptions
     *
     * @param f File to be deleted
     * @throws MojoExecutionException only if a file exists and can't be deleted
     */
    public static void delete( File f, Log log )
        throws MojoExecutionException
    {
        if ( f.isDirectory() )
        {
            log.info( Messages.getString( "EclipseCleanMojo.deletingDirectory", f.getName() ) ); //$NON-NLS-1$
        }
        else
        {
            log.info( Messages.getString( "EclipseCleanMojo.deletingFile", f.getName() ) ); //$NON-NLS-1$
        }

        if ( f.exists() )
        {
            if ( !f.delete() )
            {
                try
                {
                    FileUtils.forceDelete( f );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( Messages.getString( "EclipseCleanMojo.failedtodelete", //$NON-NLS-1$
                                                                          new Object[] { f.getName(),
                                                                              f.getAbsolutePath() } ) );
                }
            }
        }
        else
        {
            log.debug( Messages.getString( "EclipseCleanMojo.nofilefound", f.getName() ) ); //$NON-NLS-1$
        }
    }

    public static String getCanonicalPath( File file )
        throws MojoExecutionException
    {
        try
        {
            return file.getCanonicalPath();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.cantcanonicalize", file //$NON-NLS-1$
            .getAbsolutePath() ), e );
        }
    }

    /**
     * Returns a compiler plugin settings, considering also settings altered in plugin executions .
     *
     * @param project maven project
     * @return option value (may be null)
     */
    public static String getCompilerPluginSetting( MavenProject project, String optionName )
    {
        String value = findCompilerPluginSettingInPlugins( project.getModel().getBuild().getPlugins(), optionName );
        if ( value == null && project.getModel().getBuild().getPluginManagement() != null )
        {
            value =
                findCompilerPluginSettingInPlugins( project.getModel().getBuild().getPluginManagement().getPlugins(),
                                                    optionName );
        }
        return value;
    }

    /**
     * Returns the source version configured for the compiler plugin. Returns the minimum version required to compile
     * both standard and test sources, if settings are different.
     *
     * @param project maven project
     * @return java source version
     */
    public static String getCompilerSourceVersion( MavenProject project )
    {
        return IdeUtils.getCompilerPluginSetting( project, PROPERTY_SOURCE );
    }

    /**
     * Returns the source encoding configured for the compiler plugin. Returns the minimum version required to compile
     * both standard and test sources, if settings are different.
     *
     * @param project maven project
     * @return java source version
     */
    public static String getCompilerSourceEncoding( MavenProject project )
    {
        String value = IdeUtils.getCompilerPluginSetting( project, PROPERTY_ENCODING );
		if ( value == null )
        {
            project.getProperties().getProperty( "project.build.sourceEncoding" );
        }
		return value;
    }

    /**
     * Returns the target version configured for the compiler plugin. Returns the minimum version required to compile
     * both standard and test sources, if settings are different.
     *
     * @param project maven project
     * @return java target version
     */
    public static String getCompilerTargetVersion( MavenProject project )
    {
        return IdeUtils.getCompilerPluginSetting( project, PROPERTY_TARGET );
    }

    // /**
    // * Extracts the version of the first matching dependency in the given list.
    // *
    // * @param artifactIds artifact names to compare against for extracting version
    // * @param dependencies Collection of dependencies for our project
    // * @param len expected length of the version sub-string
    // * @return
    // */
    // public static String getDependencyVersion( String[] artifactIds, List dependencies, int len )
    // {
    // for ( int j = 0; j < artifactIds.length; j++ )
    // {
    // String id = artifactIds[j];
    // for ( Iterator itr = dependencies.iterator(); itr.hasNext(); )
    // {
    // Dependency dependency = (Dependency) itr.next();
    // if ( id.equals( dependency.getArtifactId() ) )
    // {
    // return StringUtils.substring( dependency.getVersion(), 0, len );
    // }
    // }
    // }
    // return null;
    // }

    /**
     * Extracts the version of the first matching artifact in the given list.
     *
     * @param artifactIds artifact names to compare against for extracting version
     * @param artifacts Set of artifacts for our project
     * @param len expected length of the version sub-string
     * @return
     */
    public static String getArtifactVersion( String[] artifactIds, List dependencies, int len )
    {
        for ( int j = 0; j < artifactIds.length; j++ )
        {
            String id = artifactIds[j];
            Iterator depIter = dependencies.iterator();
            while ( depIter.hasNext() )
            {
                Dependency dep = (Dependency) depIter.next();
                if ( id.equals( dep.getArtifactId() ) )
                {
                    return StringUtils.substring( dep.getVersion(), 0, len );
                }

            }
        }
        return null;
    }

    /**
     * Search for a configuration setting of an other plugin for a configuration setting.
     *
     * @todo there should be a better way to do this
     * @param project the current maven project to get the configuration from.
     * @param pluginId the group id and artifact id of the plugin to search for
     * @param optionName the option to get from the configuration
     * @param defaultValue the default value if the configuration was not found
     * @return the value of the option configured in the plugin configuration
     */
    public static String getPluginSetting( MavenProject project, String pluginId, String optionName, String defaultValue )
    {
        Xpp3Dom dom = getPluginConfigurationDom( project, pluginId );
        if ( dom != null && dom.getChild( optionName ) != null )
        {
            return dom.getChild( optionName ).getValue();
        }
        return defaultValue;
    }

    /**
     * Search for the configuration Xpp3 dom of an other plugin.
     *
     * @todo there should be a better way to do this
     * @param project the current maven project to get the configuration from.
     * @param pluginId the group id and artifact id of the plugin to search for
     * @return the value of the option configured in the plugin configuration
     */
    public static Xpp3Dom getPluginConfigurationDom( MavenProject project, String pluginId )
    {

        Plugin plugin = (org.apache.maven.model.Plugin) project.getBuild().getPluginsAsMap().get( pluginId );
        if ( plugin != null )
        {
            // TODO: This may cause ClassCastExceptions eventually, if the dom impls differ.
            return (Xpp3Dom) plugin.getConfiguration();
        }
        return null;
    }

    /**
     * Search for the configuration Xpp3 dom of an other plugin.
     *
     * @todo there should be a better way to do this
     * @param project the current maven project to get the configuration from.
     * @param artifactId the artifact id of the plugin to search for
     * @return the value of the option configured in the plugin configuration
     */
    public static Xpp3Dom[] getPluginConfigurationDom( MavenProject project, String artifactId,
                                                       String[] subConfiguration )
    {
        ArrayList configurationDomList = new ArrayList();
        Xpp3Dom configuration = getPluginConfigurationDom( project, artifactId );
        if ( configuration != null )
        {
            configurationDomList.add( configuration );
            for ( int index = 0; !configurationDomList.isEmpty() && subConfiguration != null
                && index < subConfiguration.length; index++ )
            {
                ArrayList newConfigurationDomList = new ArrayList();
                for ( Iterator childElement = configurationDomList.iterator(); childElement.hasNext(); )
                {
                    Xpp3Dom child = (Xpp3Dom) childElement.next();
                    Xpp3Dom[] deeperChild = child.getChildren( subConfiguration[index] );
                    for ( int deeperIndex = 0; deeperIndex < deeperChild.length; deeperIndex++ )
                    {
                        if ( deeperChild[deeperIndex] != null )
                        {
                            newConfigurationDomList.add( deeperChild[deeperIndex] );
                        }
                    }
                }
                configurationDomList = newConfigurationDomList;
            }
        }
        return (Xpp3Dom[]) configurationDomList.toArray( new Xpp3Dom[configurationDomList.size()] );
    }

    /**
     * Calculate the project name template from the specified value <code>projectNameTemplate</code>,
     * <code>addVersionToProjectName</code> and <code>addGroupIdToProjectName</code>
     * <p>
     * Note: if projectNameTemplate is not null then that value will be used regardless of the values for
     * addVersionToProjectName or addGroupIdToProjectName and a warning will be issued.
     *
     * @param projectNameTemplate the current projectNameTemplate, if available
     * @param addVersionToProjectName whether to include Version in the project name
     * @param addGroupIdToProjectName whether to include GroupId in the project name.
     * @return the project name template.
     */
    public static String calculateProjectNameTemplate( String projectNameTemplate, boolean addVersionToProjectName,
                                                       boolean addGroupIdToProjectName, Log log )
    {
        if ( projectNameTemplate != null )
        {
            if ( addVersionToProjectName || addGroupIdToProjectName )
            {
                log.warn( "projectNameTemplate definition overrides "
                    + "addVersionToProjectName or addGroupIdToProjectName" );
            }
            return projectNameTemplate;
        }
        else if ( addVersionToProjectName && addGroupIdToProjectName )
        {
            return IdeUtils.PROJECT_NAME_WITH_GROUP_AND_VERSION_TEMPLATE;
        }
        else if ( addVersionToProjectName )
        {
            return IdeUtils.PROJECT_NAME_WITH_VERSION_TEMPLATE;
        }
        else if ( addGroupIdToProjectName )
        {
            return IdeUtils.PROJECT_NAME_WITH_GROUP_TEMPLATE;
        }
        return IdeUtils.PROJECT_NAME_DEFAULT_TEMPLATE;
    }

    /**
     * Use {@link IdeDependency#getEclipseProjectName()} instead.
     */
    protected static String getProjectName( String template, IdeDependency dep )
    {
        return getProjectName( template, dep.getGroupId(), dep.getArtifactId(), dep.getVersion() );
    }

    /**
     * Use the project name template to create an eclipse project.
     *
     * @param template Template for the project name
     * @param artifact the artifact to create the project name for
     * @return the created ide project name
     */
    public static String getProjectName( String template, Artifact artifact )
    {
        return getProjectName( template, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
    }

    public static String getProjectName( String template, MavenProject project )
    {
        return getProjectName( template, project.getGroupId(), project.getArtifactId(), project.getVersion() );
    }

    private static String getProjectName( IdeDependency dep, boolean addVersionToProjectName )
    {
        return getProjectName( addVersionToProjectName ? PROJECT_NAME_WITH_VERSION_TEMPLATE
                        : PROJECT_NAME_DEFAULT_TEMPLATE, dep );
    }

    public static String getProjectName( MavenProject project, boolean addVersionToProjectName )
    {
        return getProjectName( addVersionToProjectName ? PROJECT_NAME_WITH_VERSION_TEMPLATE
                        : PROJECT_NAME_DEFAULT_TEMPLATE, project );
    }

    /**
     * @param artifact the artifact
     * @return the not-available marker file for the specified artifact
     */
    public static File getNotAvailableMarkerFile( ArtifactRepository localRepository, Artifact artifact )
    {
        return new File( localRepository.getBasedir(), localRepository.pathOf( artifact )
            + NOT_AVAILABLE_MARKER_FILE_SUFFIX );
    }

    /**
     * Wrapper around {@link ArtifactResolver#resolve(Artifact, List, ArtifactRepository)}
     *
     * @param artifactResolver see {@link ArtifactResolver#resolve(Artifact, List, ArtifactRepository)}
     * @param artifact see {@link ArtifactResolver#resolve(Artifact, List, ArtifactRepository)}
     * @param remoteRepos see {@link ArtifactResolver#resolve(Artifact, List, ArtifactRepository)}
     * @param localRepository see {@link ArtifactResolver#resolve(Artifact, List, ArtifactRepository)}
     * @param log Logger
     * @return the artifact, resolved if possible.
     */
    public static Artifact resolveArtifact( ArtifactResolver artifactResolver, Artifact artifact, List remoteRepos,
                                            ArtifactRepository localRepository, Log log )

    {
        try
        {
            artifactResolver.resolve( artifact, remoteRepos, localRepository );
        }
        catch ( ArtifactNotFoundException e )
        {
            // ignore, the jar has not been found

            /*
             * This method gets called with no remote repositories to avoid remote trips (which would ideally be
             * realized by means of a per-request offline flag), the set of available remote repos can however affect
             * the resolution from the local repo as well, in particular in Maven 3. So double check whether the local
             * file is really not present.
             */
            if ( artifact.getFile() != null && artifact.getFile().isFile() )
            {
                artifact.setResolved( true );
            }
        }
        catch ( ArtifactResolutionException e )
        {
            String message =
                Messages.getString( "IdeUtils.errorresolving", new Object[] { artifact.getClassifier(),
                    artifact.getId(), e.getMessage() } );

            log.warn( message );
        }

        return artifact;
    }

    /**
     * Wrap {@link ArtifactFactory#createArtifactWithClassifier} so that the type and classifier are set correctly for
     * "sources" and "javadoc".
     *
     * @param groupId see {@link ArtifactFactory#createArtifactWithClassifier}
     * @param artifactId see {@link ArtifactFactory#createArtifactWithClassifier}
     * @param version see {@link ArtifactFactory#createArtifactWithClassifier}
     * @param depClassifier see {@link ArtifactFactory#createArtifactWithClassifier}
     * @param inClassifier either "sources" of "javadoc"
     * @param artifactFactory see {@link ArtifactFactory#createArtifactWithClassifier}
     * @return see {@link ArtifactFactory#createArtifactWithClassifier}
     * @see ArtifactFactory#createArtifactWithClassifier
     */
    public static Artifact createArtifactWithClassifier( String groupId, String artifactId, String version,
                                                         String depClassifier, String inClassifier,
                                                         ArtifactFactory artifactFactory )
    {
        String type = null;

        // the "sources" classifier maps to the "java-source" type
        if ( "sources".equals( inClassifier ) )
        {
            type = "java-source";
        }
        else
        {
            type = inClassifier;
        }

        String finalClassifier = null;
        if ( depClassifier == null )
        {
            finalClassifier = inClassifier;
        }
        else if ( "sources".equals( inClassifier ) && "tests".equals( depClassifier ) )
        {
            // MECLIPSE-151 - if the dependency is a test, get the correct classifier for it. (ignore for javadocs)
            finalClassifier = "test-sources";
        }
        else
        {
            finalClassifier = depClassifier + "-" + inClassifier;
        }

        return artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type, finalClassifier );
    }

    public static String resolveJavaVersion( MavenProject project )
    {
        String version = IdeUtils.getCompilerTargetVersion( project );
        if ( version == null )
        {
            version = IdeUtils.getCompilerSourceVersion( project );
        }

        if ( "1.5".equals( version ) ) //$NON-NLS-1$
        {
            version = IdeUtils.JAVA_5_0;// see MECLIPSE-47 eclipse only accept 5.0 as a valid version
        }
        else if ( "1.6".equals( version ) ) //$NON-NLS-1$
        {
            version = IdeUtils.JAVA_6_0;
        }
        else if ( version != null && version.length() == 1 )
        {
            version = version + ".0";// 5->5.0 6->6.0 7->7.0 //$NON-NLS-1$
        }

        return version == null ? IdeUtils.JAVA_1_4 : version;
    }

    public static String toRelativeAndFixSeparator( File basedir, File fileToAdd, boolean replaceSlashesWithDashes )
        throws MojoExecutionException
    {
        if ( !fileToAdd.isAbsolute() )
        {
            fileToAdd = new File( basedir, fileToAdd.getPath() );
        }

        String basedirPath = getCanonicalPath( basedir );
        String absolutePath = getCanonicalPath( fileToAdd );

        String relative = null;

        if ( absolutePath.equals( basedirPath ) )
        {
            relative = "."; //$NON-NLS-1$
        }
        else if ( absolutePath.startsWith( basedirPath ) )
        {
            // MECLIPSE-261
            // The canonical form of a windows root dir ends in a slash, whereas the canonical form of any other file
            // does not.
            // The absolutePath is assumed to be: basedirPath + Separator + fileToAdd
            // In the case of a windows root directory the Separator is missing since it is contained within
            // basedirPath.
            int length = basedirPath.length() + 1;
            if ( basedirPath.endsWith( "\\" ) )
            {
                length--;
            }
            relative = absolutePath.substring( length );
        }
        else
        {
            relative = absolutePath;
        }

        relative = fixSeparator( relative );

        if ( replaceSlashesWithDashes )
        {
            relative = StringUtils.replace( relative, '/', '-' );
            relative = StringUtils.replace( relative, ':', '-' ); // remove ":" for absolute paths in windows
        }

        return relative;
    }

    /**
     * Convert the provided filename from a Windows separator \\ to a unix/java separator /
     *
     * @param filename file name to fix separator
     * @return filename with all \\ replaced with /
     */
    public static String fixSeparator( String filename )
    {
        return StringUtils.replace( filename, '\\', '/' );
    }

    /**
     * NOTE: This is to account for the unfortunate fact that "file:" URIs differ between Windows and Unix. On a Windows
     * box, the path "C:\dir" is mapped to "file:/C:/dir". On a Unix box, the path "/home/dir" is mapped to
     * "file:/home/dir". So, in the first case the slash after "file:" is not part of the corresponding filesystem path
     * while in the later case it is. This discrepancy makes verifying the javadoc attachments in ".classpath" a little
     * tricky.
     *
     * @param input string input that may contain a windows URI
     * @return all windows URI convert "file:C:/dir" to "file:/C:/dir"
     */
    public static String fixWindowsDriveURI( String input )
    {
        return input.replaceAll( "file:([a-zA-Z]):", "file:/$1:" );
    }

    /**
     * Returns a compiler plugin settings from a list of plugins .
     *
     * @param project maven project
     * @return option value (may be null)
     */
    private static String findCompilerPluginSettingInPlugins( List plugins, String optionName )
    {
        String value = null;

        for ( Iterator it = plugins.iterator(); it.hasNext(); )
        {
            Plugin plugin = (Plugin) it.next();

            if ( plugin.getArtifactId().equals( ARTIFACT_MAVEN_COMPILER_PLUGIN ) )
            {
                // TODO: This may cause ClassCastExceptions eventually, if the dom impls differ.
                Xpp3Dom o = (Xpp3Dom) plugin.getConfiguration();

                // this is the default setting
                if ( o != null && o.getChild( optionName ) != null )
                {
                    value = o.getChild( optionName ).getValue();
                }

                List executions = plugin.getExecutions();

                // a different source/target version can be configured for test sources compilation
                for ( Iterator iter = executions.iterator(); iter.hasNext(); )
                {
                    PluginExecution execution = (PluginExecution) iter.next();

                    // TODO: This may cause ClassCastExceptions eventually, if the dom impls differ.
                    o = (Xpp3Dom) execution.getConfiguration();

                    if ( o != null && o.getChild( optionName ) != null )
                    {
                        value = o.getChild( optionName ).getValue();
                    }
                }
            }
        }
        return value;
    }

    private static String getProjectName( String template, String groupId, String artifactId, String version )
    {
        String s = template;
        s = s.replaceAll( "\\[groupId\\]", groupId );
        s = s.replaceAll( "\\[artifactId\\]", artifactId );
        s = s.replaceAll( "\\[version\\]", version );
        return s;
    }

    private IdeUtils()
    {
        // don't instantiate
    }
}
