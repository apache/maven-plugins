package org.apache.maven.plugin.assembly.utils;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.Os;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.interpolation.fixed.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.fixed.PrefixedPropertiesValueSource;
import org.codehaus.plexus.interpolation.fixed.PropertiesBasedValueSource;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

/**
 * @version $Id$
 */
public final class AssemblyFormatUtils
{

    private AssemblyFormatUtils()
    {
    }

    /**
     * Get the full name of the distribution artifact
     *
     * @param assembly the assembly
     * @return the distribution name
     */
    public static String getDistributionName( final Assembly assembly, final AssemblerConfigurationSource configSource )
    {
        final String finalName = configSource.getFinalName();
        final boolean appendAssemblyId = configSource.isAssemblyIdAppended();
        final String classifier = configSource.getClassifier();

        String distributionName = finalName;
        if ( appendAssemblyId )
        {
            if ( !StringUtils.isEmpty( assembly.getId() ) )
            {
                distributionName = finalName + "-" + assembly.getId();
            }
        }
        else if ( classifier != null )
        {
            distributionName = finalName + "-" + classifier;
        }

        return distributionName;
    }


    @Nonnull
    public static FixedStringSearchInterpolator finalNameInterpolator( String finalName )
    {
        final Properties specialExpressionOverrides = new Properties();

        if ( finalName != null )
        {
            specialExpressionOverrides.setProperty( "finalName", finalName );
            specialExpressionOverrides.setProperty( "build.finalName", finalName );
        }
        else
        {
            return FixedStringSearchInterpolator.empty();
        }

        return FixedStringSearchInterpolator.create( new PropertiesBasedValueSource( specialExpressionOverrides ) );
    }

    @Nonnull
    public static FixedStringSearchInterpolator moduleProjectInterpolator( final MavenProject moduleProject )
    {
        if ( moduleProject != null )
        {
            return FixedStringSearchInterpolator.createWithPermittedNulls(
                new PrefixedObjectValueSource( "module.", moduleProject ),
                new PrefixedPropertiesValueSource( "module.properties.", moduleProject.getProperties() ),
                moduleProject.getArtifact() != null
                    ? new PrefixedObjectValueSource( "module.", moduleProject.getArtifact() )
                    : null );
        }
        else
        {
            return FixedStringSearchInterpolator.empty();
        }

    }

    public static FixedStringSearchInterpolator moduleArtifactInterpolator( Artifact moduleArtifact )
    {
        if ( moduleArtifact != null )
        {
            // CHECKSTYLE_OFF: LineLength
            return FixedStringSearchInterpolator.create( new PrefixedObjectValueSource( "module.", moduleArtifact ),
                                                         new PrefixedObjectValueSource( "module.",
                                                                                        moduleArtifact.getArtifactHandler() ),
                                                         new PrefixedObjectValueSource( "module.handler.",
                                                                                        moduleArtifact.getArtifactHandler() ) );
            // CHECKSTYLE_ON: LineLength
        }
        else
        {
            return FixedStringSearchInterpolator.empty();
        }

    }

    @Nonnull
    public static FixedStringSearchInterpolator artifactProjectInterpolator( final MavenProject artifactProject )
    {
        if ( artifactProject != null )
        {
            PrefixedObjectValueSource vs = null;
            if ( artifactProject.getArtifact() != null )
            {
                vs = new PrefixedObjectValueSource( "artifact.", artifactProject.getArtifact() );
            }

            return FixedStringSearchInterpolator.createWithPermittedNulls(
                new PrefixedObjectValueSource( "artifact.", artifactProject ),
                new PrefixedPropertiesValueSource( "artifact.properties.", artifactProject.getProperties() ), vs );


        }
        else
        {
            return FixedStringSearchInterpolator.empty();
        }


    }

    @Nonnull
    public static FixedStringSearchInterpolator artifactInterpolator( @Nonnull final Artifact artifact )
    {
        return FixedStringSearchInterpolator.create( new PrefixedObjectValueSource( "artifact.", artifact ),
                                                     new PrefixedObjectValueSource( "artifact.",
                                                                                    artifact.getArtifactHandler() ),
                                                     new PrefixedObjectValueSource( "artifact.handler.",
                                                                                    artifact.getArtifactHandler() ) );
    }


    @Nonnull
    public static FixedStringSearchInterpolator classifierRules( final Artifact artifact )
    {
        final Properties specialRules = new Properties();

        final String classifier = ProjectUtils.getClassifier( artifact );
        if ( classifier != null )
        {
            specialRules.setProperty( "dashClassifier?", "-" + classifier );
            specialRules.setProperty( "dashClassifier", "-" + classifier );
        }
        else
        {
            specialRules.setProperty( "dashClassifier?", "" );
            specialRules.setProperty( "dashClassifier", "" );
        }

        return FixedStringSearchInterpolator.create( new PropertiesBasedValueSource( specialRules ) );
    }


    /**
     * ORDER OF INTERPOLATION PRECEDENCE:
     * <ol>
     * <li>Support for special expressions, like ${finalName} (use the assembly plugin configuration not the build
     * config)</li>
     * <li>prefixed with "module." if moduleProject is non-null
     * <ol>
     * <li>MavenProject instance for module being assembled</li>
     * </ol>
     * </li>
     * <li>prefixed with "artifact." if artifactProject is non-null
     * <ol>
     * <li>MavenProject instance for artifact</li>
     * </ol>
     * </li>
     * <li>user-defined properties from the command line</li>
     * <li>prefixed with "pom." or "project.", or no prefix at all
     * <ol>
     * <li>MavenProject instance from current build</li>
     * </ol>
     * </li>
     * <li>properties from main project</li>
     * <li>system properties, from the MavenSession instance (to support IDEs)</li>
     * <li>environment variables.</li>
     * </ol>
     */
    public static String getOutputDirectory( final String output, final MavenProject artifactProject,
                                             final String finalName, final AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException
    {
        return getOutputDirectory( output, finalName, configSource, moduleProjectInterpolator( null ),
                                   artifactProjectInterpolator( artifactProject ) );
    }


    private static FixedStringSearchInterpolator executionPropertiesInterpolator(
        AssemblerConfigurationSource configSource )
    {
        MavenSession session;

        if ( configSource != null )
        {
            session = configSource.getMavenSession();

            if ( session != null )
            {
                Properties userProperties = session.getExecutionProperties(); // this is added twice....

                if ( userProperties != null )
                {
                    return FixedStringSearchInterpolator.create( new PropertiesBasedValueSource( userProperties ) );
                }
            }
        }
        return FixedStringSearchInterpolator.empty();
    }

    private static FixedStringSearchInterpolator mainProjectOnlyInterpolator( MavenProject mainProject )
    {
        if ( mainProject != null )
        {
            // 5
            return FixedStringSearchInterpolator.create(
                new org.codehaus.plexus.interpolation.fixed.PrefixedObjectValueSource(
                    InterpolationConstants.PROJECT_PREFIXES, mainProject, true ) );
        }
        else
        {
            return FixedStringSearchInterpolator.empty();
        }
    }


    /**
     * ORDER OF INTERPOLATION PRECEDENCE:
     * <ol>
     * <li>prefixed with "module.", if moduleProject != null
     * <ol>
     * <li>Artifact instance for module, if moduleArtifact != null</li>
     * <li>ArtifactHandler instance for module, if moduleArtifact != null</li>
     * <li>MavenProject instance for module</li>
     * </ol>
     * </li>
     * <li>prefixed with "artifact."
     * <ol>
     * <li>Artifact instance</li>
     * <li>ArtifactHandler instance for artifact</li>
     * <li>MavenProject instance for artifact</li>
     * </ol>
     * </li>
     * <li>prefixed with "pom." or "project."
     * <ol>
     * <li>MavenProject instance from current build</li>
     * </ol>
     * </li>
     * <li>no prefix, using main project instance
     * <ol>
     * <li>MavenProject instance from current build</li>
     * </ol>
     * </li>
     * <li>Support for special expressions, like ${dashClassifier?}</li>
     * <li>user-defined properties from the command line</li>
     * <li>properties from main project</li>
     * <li>system properties, from the MavenSession instance (to support IDEs)</li>
     * <li>environment variables.</li>
     * </ol>
     */


    @Nonnull
    public static String fixRelativeRefs( @Nonnull String src )
    {
        String value = src;

        String[] separators = { "/", "\\" };

        String finalSep = null;
        for ( String sep : separators )
        {
            if ( value.endsWith( sep ) )
            {
                finalSep = sep;
            }

            if ( value.contains( "." + sep ) )
            {
                List<String> parts = new ArrayList<String>();
                parts.addAll( Arrays.asList( value.split( sep.replace( "\\", "\\\\" ) ) ) );

                for ( ListIterator<String> it = parts.listIterator(); it.hasNext(); )
                {
                    String part = it.next();
                    if ( ".".equals( part ) )
                    {
                        it.remove();
                    }
                    else if ( "..".equals( part ) )
                    {
                        it.remove();
                        if ( it.hasPrevious() )
                        {
                            it.previous();
                            it.remove();
                        }
                    }
                }

                value = StringUtils.join( parts.iterator(), sep );
            }
        }

        if ( finalSep != null && value.length() > 0 && !value.endsWith( finalSep ) )
        {
            value += finalSep;
        }

        return value;
    }


    /**
     * ORDER OF INTERPOLATION PRECEDENCE:
     * <ol>
     * <li>prefixed with "module.", if moduleProject != null
     * <ol>
     * <li>Artifact instance for module, if moduleArtifact != null</li>
     * <li>ArtifactHandler instance for module, if moduleArtifact != null</li>
     * <li>MavenProject instance for module</li>
     * </ol>
     * </li>
     * <li>prefixed with "artifact."
     * <ol>
     * <li>Artifact instance</li>
     * <li>ArtifactHandler instance for artifact</li>
     * <li>MavenProject instance for artifact</li>
     * </ol>
     * </li>
     * <li>prefixed with "pom." or "project."
     * <ol>
     * <li>MavenProject instance from current build</li>
     * </ol>
     * </li>
     * <li>no prefix, using main project instance
     * <ol>
     * <li>MavenProject instance from current build</li>
     * </ol>
     * </li>
     * <li>Support for special expressions, like ${dashClassifier?}</li>
     * <li>user-defined properties from the command line</li>
     * <li>properties from main project</li>
     * <li>system properties, from the MavenSession instance (to support IDEs)</li>
     * <li>environment variables.</li>
     * </ol>
     */
    public static String evaluateFileNameMapping( final String expression, @Nonnull final Artifact artifact,
                                                  @Nullable final MavenProject mainProject,
                                                  @Nullable final Artifact moduleArtifact,
                                                  @Nonnull final AssemblerConfigurationSource configSource,
                                                  FixedStringSearchInterpolator moduleProjectInterpolator,
                                                  FixedStringSearchInterpolator artifactProjectInterpolator )
        throws AssemblyFormattingException
    {
        String value = expression;

        final FixedStringSearchInterpolator interpolator =
            FixedStringSearchInterpolator.create( moduleArtifactInterpolator( moduleArtifact ),
                                                  moduleProjectInterpolator, artifactInterpolator( artifact ),
                                                  artifactProjectInterpolator,
                                                  mainProjectOnlyInterpolator( mainProject ),
                                                  classifierRules( artifact ),
                                                  executionPropertiesInterpolator( configSource ),
                                                  configSource.getMainProjectInterpolator(),
                                                  configSource.getCommandLinePropsInterpolator(),
                                                  configSource.getEnvInterpolator() );

        value = interpolator.interpolate( value );

        value = StringUtils.replace( value, "//", "/" );
        value = StringUtils.replace( value, "\\\\", "\\" );
        value = fixRelativeRefs( value );

        return value;
    }

    /**
     * ORDER OF INTERPOLATION PRECEDENCE:
     * <ol>
     * <li>Support for special expressions, like ${finalName} (use the assembly plugin configuration not the build
     * config)</li>
     * <li>prefixed with "module." if moduleProject is non-null
     * <ol>
     * <li>MavenProject instance for module being assembled</li>
     * </ol>
     * </li>
     * <li>prefixed with "artifact." if artifactProject is non-null
     * <ol>
     * <li>MavenProject instance for artifact</li>
     * </ol>
     * </li>
     * <li>user-defined properties from the command line</li>
     * <li>prefixed with "pom." or "project.", or no prefix at all
     * <ol>
     * <li>MavenProject instance from current build</li>
     * </ol>
     * </li>
     * <li>properties from main project</li>
     * <li>system properties, from the MavenSession instance (to support IDEs)</li>
     * <li>environment variables.</li>
     * </ol>
     */
    public static String getOutputDirectory( final String output, final String finalName,
                                             final AssemblerConfigurationSource configSource,
                                             FixedStringSearchInterpolator moduleProjectIntrpolator,
                                             FixedStringSearchInterpolator artifactProjectInterpolator )
        throws AssemblyFormattingException
    {
        String value = output;
        if ( value == null )
        {
            value = "";
        }

        final FixedStringSearchInterpolator interpolator =
            FixedStringSearchInterpolator.create( finalNameInterpolator( finalName ), moduleProjectIntrpolator,
                                                  artifactProjectInterpolator,
                                                  executionPropertiesInterpolator( configSource ),
                                                  configSource.getMainProjectInterpolator(),
                                                  configSource.getCommandLinePropsInterpolator(),
                                                  configSource.getEnvInterpolator() );

        value = interpolator.interpolate( value );

        if ( ( value.length() > 0 ) && !value.endsWith( "/" ) && !value.endsWith( "\\" ) )
        {
            value += "/";
        }

        if ( ( value.length() > 0 ) && ( value.startsWith( "/" ) || value.startsWith( "\\" ) ) )
        {
            value = value.substring( 1 );
        }

        value = StringUtils.replace( value, "//", "/" );
        value = StringUtils.replace( value, "\\\\", "\\" );
        value = fixRelativeRefs( value );

        return value;
    }

    public static void warnForPlatformSpecifics( Logger logger, String destDirectory )
    {
        if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
        {
            if ( isLinuxRootReference( destDirectory ) )
            {
                logger.error( "OS=Windows and the assembly descriptor contains a *nix-specific root-relative-reference"
                                  + " (starting with slash) " + destDirectory );
            }
            else if ( isWindowsPath( destDirectory ) )
            {
                logger.warn( "The assembly descriptor contains a *nix-specific root-relative-reference"
                                 + " (starting with slash). This is non-portable and will fail on windows "
                                 + destDirectory );
            }
        }
        else
        {
            if ( isWindowsPath( destDirectory ) )
            {
                logger.error(
                    "OS=Non-Windows and the assembly descriptor contains a windows-specific directory reference"
                        + " (with a drive letter) " + destDirectory );
            }
            else if ( isLinuxRootReference( destDirectory ) )
            {
                logger.warn( "The assembly descriptor contains a filesystem-root relative reference,"
                                 + " which is not cross platform compatible " + destDirectory );
            }
        }
    }

    static boolean isWindowsPath( String destDirectory )
    {
        return ( destDirectory != null && destDirectory.length() >= 2 && destDirectory.charAt( 1 ) == ':' );
    }

    static boolean isLinuxRootReference( String destDirectory )
    {
        return ( destDirectory != null && destDirectory.startsWith( "/" ) );
    }


}
