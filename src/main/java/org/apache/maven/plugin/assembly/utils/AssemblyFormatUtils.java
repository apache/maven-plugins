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
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PrefixedPropertiesValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
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
     * @param assembly
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

    /**
     * @deprecated Use
     *             {@link AssemblyFormatUtils#getOutputDirectory(String, MavenProject, MavenProject, String, AssemblerConfigurationSource)}
     *             instead.
     */
    @Deprecated
    public static String getOutputDirectory( final String output, final MavenProject mainProject,
                                             final MavenProject artifactProject, final String finalName )
        throws AssemblyFormattingException
    {
        return getOutputDirectory( output, mainProject, null, artifactProject, finalName, null );
    }

    public static String getOutputDirectory( final String output, final MavenProject mainProject,
                                             final MavenProject artifactProject, final String finalName,
                                             final AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException
    {
        return getOutputDirectory( output, mainProject, null, artifactProject, finalName, configSource );
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
    public static String getOutputDirectory( final String output, final MavenProject mainProject,
                                             final MavenProject moduleProject, final MavenProject artifactProject,
                                             final String finalName, final AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException
    {
        String value = output;
        if ( value == null )
        {
            value = "";
        }

        final StringSearchInterpolator interpolator = new StringSearchInterpolator();

        final Properties specialExpressionOverrides = new Properties();

        if ( finalName != null )
        {
            specialExpressionOverrides.setProperty( "finalName", finalName );
            specialExpressionOverrides.setProperty( "build.finalName", finalName );
        }

        // 1
        interpolator.addValueSource( new PropertiesBasedValueSource( specialExpressionOverrides ) );

        if ( moduleProject != null )
        {
            // 2
            interpolator.addValueSource( new PrefixedObjectValueSource( "module.", moduleProject ) );
            interpolator.addValueSource( new PrefixedPropertiesValueSource( "module.properties.",
                                                                            moduleProject.getProperties() ) );
            if ( moduleProject.getArtifact() != null )
            {
                interpolator.addValueSource( new PrefixedObjectValueSource( "module.", moduleProject.getArtifact() ) );
            }
        }

        if ( artifactProject != null )
        {
            // 3
            interpolator.addValueSource( new PrefixedObjectValueSource( "artifact.", artifactProject ) );
            interpolator.addValueSource( new PrefixedPropertiesValueSource( "artifact.properties.",
                                                                            artifactProject.getProperties() ) );
            if ( artifactProject.getArtifact() != null )
            {
                interpolator.addValueSource( new PrefixedObjectValueSource( "artifact.", artifactProject.getArtifact() ) );
            }
        }

        MavenSession session = null;

        if ( configSource != null )
        {
            session = configSource.getMavenSession();

            if ( session != null )
            {
                Properties userProperties = null;
                try
                {
                    userProperties = session.getExecutionProperties();
                }
                catch ( final NoSuchMethodError nsmer )
                {
                    // OK, so user is using Maven <= 2.0.8. No big deal.
                }

                if ( userProperties != null )
                {
                    // 4
                    interpolator.addValueSource( new PropertiesBasedValueSource( userProperties ) );
                }
            }
        }

        if ( mainProject != null )
        {
            // 5
            interpolator.addValueSource( new PrefixedObjectValueSource( InterpolationConstants.PROJECT_PREFIXES,
                                                                        mainProject, true ) );

            // 6
            interpolator.addValueSource( new PrefixedPropertiesValueSource(
                                                                            InterpolationConstants.PROJECT_PROPERTIES_PREFIXES,
                                                                            mainProject.getProperties(), true ) );
        }

        Properties commandLineProperties = System.getProperties();
        try
        {
            if ( session != null )
            {
                commandLineProperties = session.getExecutionProperties();
            }

        }
        catch ( final NoSuchMethodError nsmer )
        {
            // OK, so user is using Maven <= 2.0.8. No big deal.
        }

        // 7
        interpolator.addValueSource( new PropertiesBasedValueSource( commandLineProperties ) );

        try
        {
            // 8
            interpolator.addValueSource( new PrefixedPropertiesValueSource( Collections.singletonList( "env." ),
                                                                            CommandLineUtils.getSystemEnvVars( false ),
                                                                            true ) );
        }
        catch ( final IOException e )
        {
            throw new AssemblyFormattingException( "Failed to retrieve OS environment variables. Reason: "
                            + e.getMessage(), e );
        }

        try
        {
            value = interpolator.interpolate( value );
        }
        catch ( final InterpolationException e )
        {
            throw new AssemblyFormattingException( "Failed to interpolate output directory. Reason: " + e.getMessage(),
                                                   e );
        }

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
        value = StringUtils.replace( value, "./", "" );
        value = StringUtils.replace( value, ".\\", "" );

        return value;
    }

    /**
     * @deprecated Use
     *             {@link AssemblyFormatUtils#evaluateFileNameMapping(String, Artifact, MavenProject, MavenProject, AssemblerConfigurationSource)}
     *             instead.
     */
    @Deprecated
    public static String evaluateFileNameMapping( final String expression, final Artifact artifact,
                                                  final MavenProject mainProject, final MavenProject artifactProject )
        throws AssemblyFormattingException
    {
        return evaluateFileNameMapping( expression, artifact, mainProject, null, null, artifactProject, null );
    }

    public static String evaluateFileNameMapping( final String expression, final Artifact artifact,
                                                  final MavenProject mainProject, final MavenProject artifactProject,
                                                  final AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException
    {
        return evaluateFileNameMapping( expression, artifact, mainProject, null, null, artifactProject, configSource );
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
    public static String evaluateFileNameMapping( final String expression, final Artifact artifact,
                                                  final MavenProject mainProject, final MavenProject moduleProject,
                                                  final Artifact moduleArtifact, final MavenProject artifactProject,
                                                  final AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException
    {
        String value = expression;

        // TODO: This is BAD! Accessors SHOULD NOT change the behavior of the object.
        // [jdcasey; 16-Aug-1007] This is fixed in SVN, just waiting for it to pass out of legacy.
        artifact.isSnapshot();

        final StringSearchInterpolator interpolator = new StringSearchInterpolator();

        if ( moduleArtifact != null )
        {
            // 1A
            interpolator.addValueSource( new PrefixedObjectValueSource( "module.", moduleArtifact ) );

            // 1B
            interpolator.addValueSource( new PrefixedObjectValueSource( "module.", moduleArtifact.getArtifactHandler() ) );
            interpolator.addValueSource( new PrefixedObjectValueSource( "module.handler.",
                                                                        moduleArtifact.getArtifactHandler() ) );
        }

        // 1C
        if ( moduleProject != null )
        {
            interpolator.addValueSource( new PrefixedObjectValueSource( "module.", moduleProject ) );
            interpolator.addValueSource( new PrefixedPropertiesValueSource( "module.properties.",
                                                                            moduleProject.getProperties() ) );
            if ( moduleProject.getArtifact() != null )
            {
                interpolator.addValueSource( new PrefixedObjectValueSource( "module.", moduleProject.getArtifact() ) );
            }
        }

        // 2A
        interpolator.addValueSource( new PrefixedObjectValueSource( "artifact.", artifact ) );

        // 2B
        interpolator.addValueSource( new PrefixedObjectValueSource( "artifact.", artifact.getArtifactHandler() ) );
        interpolator.addValueSource( new PrefixedObjectValueSource( "artifact.handler.", artifact.getArtifactHandler() ) );

        // 2C
        if ( artifactProject != null )
        {
            interpolator.addValueSource( new PrefixedObjectValueSource( "artifact.", artifactProject ) );
            interpolator.addValueSource( new PrefixedPropertiesValueSource( "artifact.properties.",
                                                                            artifactProject.getProperties() ) );
            if ( artifactProject.getArtifact() != null )
            {
                interpolator.addValueSource( new PrefixedObjectValueSource( "artifact.", artifactProject.getArtifact() ) );
            }
        }

        if ( mainProject != null )
        {
            // 3
            // 4
            interpolator.addValueSource( new PrefixedObjectValueSource( InterpolationConstants.PROJECT_PREFIXES,
                                                                        mainProject, true ) );
        }

        final Properties specialRules = new Properties();

        final String classifier = artifact.getClassifier();
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

        // 5
        interpolator.addValueSource( new PropertiesBasedValueSource( specialRules ) );

        MavenSession session = null;
        if ( configSource != null )
        {
            session = configSource.getMavenSession();

            if ( session != null )
            {
                Properties userProperties = null;
                try
                {
                    userProperties = session.getExecutionProperties();
                }
                catch ( final NoSuchMethodError nsmer )
                {
                    // OK, so user is using Maven <= 2.0.8. No big deal.
                }

                if ( userProperties != null )
                {
                    // 6
                    interpolator.addValueSource( new PropertiesBasedValueSource( userProperties ) );
                }
            }
        }

        if ( mainProject != null )
        {
            // 7
            interpolator.addValueSource( new PrefixedPropertiesValueSource(
                                                                            InterpolationConstants.PROJECT_PROPERTIES_PREFIXES,
                                                                            mainProject.getProperties(), true ) );
        }

        Properties commandLineProperties = System.getProperties();
        try
        {
            if ( session != null )
            {
                commandLineProperties = session.getExecutionProperties();
            }

        }
        catch ( final NoSuchMethodError nsmer )
        {
            // OK, so user is using Maven <= 2.0.8. No big deal.
        }

        // 8
        interpolator.addValueSource( new PropertiesBasedValueSource( commandLineProperties ) );

        try
        {
            // 9
            interpolator.addValueSource( new PrefixedPropertiesValueSource( Collections.singletonList( "env." ),
                                                                            CommandLineUtils.getSystemEnvVars( false ),
                                                                            true ) );
        }
        catch ( final IOException e )
        {
            throw new AssemblyFormattingException( "Failed to retrieve OS environment variables. Reason: "
                            + e.getMessage(), e );
        }

        try
        {
            value = interpolator.interpolate( value );
        }
        catch ( final InterpolationException e )
        {
            throw new AssemblyFormattingException( "Failed to interpolate output filename mapping. Reason: "
                            + e.getMessage(), e );
        }

        value = StringUtils.replace( value, "//", "/" );
        value = StringUtils.replace( value, "\\\\", "\\" );
        value = StringUtils.replace( value, "./", "" );
        value = StringUtils.replace( value, ".\\", "" );

        return value;
    }

}
