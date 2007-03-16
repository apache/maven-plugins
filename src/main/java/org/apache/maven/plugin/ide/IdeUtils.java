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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 * @version $Id$
 */
public class IdeUtils
{
    /**
     * compiler plugin id.
     */
    private static final String ARTIFACT_MAVEN_COMPILER_PLUGIN = "maven-compiler-plugin"; //$NON-NLS-1$

    /**
     * 'target' property for maven-compiler-plugin.
     */
    private static final String PROPERTY_TARGET = "target"; //$NON-NLS-1$

    /**
     * 'source' property for maven-compiler-plugin.
     */
    private static final String PROPERTY_SOURCE = "source"; //$NON-NLS-1$

    private IdeUtils()
    {
        // don't instantiate
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
            throw new MojoExecutionException( Messages.getString( "cantcanonicalize", file //$NON-NLS-1$
                .getAbsolutePath() ), e );
        }
    }
    
    public static String getProjectName( MavenProject project, boolean addVersionToProjectName )
    {
        return getProjectName( project.getArtifactId(), project.getVersion(), addVersionToProjectName );
    }
    
    public static String getProjectName( IdeDependency dep, boolean addVersionToProjectName )
    {
        return getProjectName( dep.getArtifactId(), dep.getVersion(), addVersionToProjectName );
    }
    
    private static String getProjectName( String artifactId, String version, boolean addVersionToProjectName )
    {
        if( addVersionToProjectName )
        {
            return artifactId + '-' + version;
        }
        else
        {
            return artifactId;
        }
    }

    public static String toRelativeAndFixSeparator( File basedir, File fileToAdd, boolean replaceSlashesWithDashes )
        throws MojoExecutionException
    {
        String basedirpath;
        String absolutePath;

        basedirpath = getCanonicalPath( basedir );
        absolutePath = getCanonicalPath( fileToAdd );

        String relative;

        if ( absolutePath.equals( basedirpath ) )
        {
            relative = "."; //$NON-NLS-1$
        }
        else if ( absolutePath.startsWith( basedirpath ) )
        {
            relative = absolutePath.substring( basedirpath.length() + 1 );
        }
        else
        {
            relative = absolutePath;
        }

        relative = StringUtils.replace( relative, '\\', '/' );

        if ( replaceSlashesWithDashes )
        {
            relative = StringUtils.replace( relative, '/', '-' );
            relative = StringUtils.replace( relative, ':', '-' ); // remove ":" for absolute paths in windows
        }

        return relative;
    }

    /**
     * @todo there should be a better way to do this
     */
    public static String getPluginSetting( MavenProject project, String artifactId, String optionName,
                                           String defaultValue )
    {
        for ( Iterator it = project.getModel().getBuild().getPlugins().iterator(); it.hasNext(); )
        {
            Plugin plugin = (Plugin) it.next();

            if ( plugin.getArtifactId().equals( artifactId ) )
            {
                Xpp3Dom o = (Xpp3Dom) plugin.getConfiguration();

                if ( o != null && o.getChild( optionName ) != null )
                {
                    return o.getChild( optionName ).getValue();
                }
            }
        }

        return defaultValue;
    }

    public static Artifact resolveArtifactWithClassifier( String groupId, String artifactId, String version,
                                                          String classifier, ArtifactRepository localRepository,
                                                          ArtifactResolver artifactResolver,
                                                          ArtifactFactory artifactFactory, List remoteRepos, Log log )

    {
        String type = classifier;

        // the "sources" classifier maps to the "java-source" type
        if ( "sources".equals( type ) ) //$NON-NLS-1$
        {
            type = "java-source"; //$NON-NLS-1$
        }

        Artifact resolvedArtifact = artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, type,
                                                                                  classifier );

        try
        {
            artifactResolver.resolve( resolvedArtifact, remoteRepos, localRepository );
        }
        catch ( ArtifactNotFoundException e )
        {
            // ignore, the jar has not been found
        }
        catch ( ArtifactResolutionException e )
        {
            String message = Messages.getString( "errorresolving", new Object[] { //$NON-NLS-1$
                                                 classifier, resolvedArtifact.getId(), e.getMessage() } );

            log.warn( message );
        }

        return resolvedArtifact;
    }

    /**
     * Extracts the version of the first matching dependency in the given list.
     * 
     * @param artifactIds artifact names to compare against for extracting version
     * @param dependencies Collection of dependencies for our project
     * @param len expected length of the version sub-string
     * @return
     */
    public static String getDependencyVersion( String[] artifactIds, List dependencies, int len )
    {
        for ( int j = 0; j < artifactIds.length; j++ )
        {
            String id = artifactIds[j];
            for ( Iterator itr = dependencies.iterator(); itr.hasNext(); )
            {
                Dependency dependency = (Dependency) itr.next();
                if ( id.equals( dependency.getArtifactId() ) )
                {
                    return StringUtils.substring( dependency.getVersion(), 0, len );
                }
            }
        }
        return null;
    }

    /**
     * Returns the target version configured for the compiler plugin. Returns the minimum version required to compile
     * both standard and test sources, if settings are different.
     * @param project maven project
     * @return java target version
     */
    public static String getCompilerTargetVersion( MavenProject project )
    {
        return IdeUtils.getCompilerPluginSetting( project, PROPERTY_TARGET );
    }

    /**
     * Returns the source version configured for the compiler plugin. Returns the minimum version required to compile
     * both standard and test sources, if settings are different.
     * @param project maven project
     * @return java source version
     */
    public static String getCompilerSourceVersion( MavenProject project )
    {
        return IdeUtils.getCompilerPluginSetting( project, PROPERTY_SOURCE );
    }

    /**
     * Returns a compiler plugin settings, considering also settings altered in plugin executions .
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
     * Returns a compiler plugin settings from a list of plugins .
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
}
