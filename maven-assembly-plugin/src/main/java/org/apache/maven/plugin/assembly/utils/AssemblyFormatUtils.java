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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

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

/**
 * @version $Id$
 */
public final class AssemblyFormatUtils
{
    
    private static final List PROJECT_PREFIXES;
    
    private static final List PROJECT_PROPERTIES_PREFIXES;
    
    static
    {
        List projectPrefixes = new ArrayList();
        projectPrefixes.add( "pom." );
        projectPrefixes.add( "project." );
        
        PROJECT_PREFIXES = Collections.unmodifiableList( projectPrefixes );
        
        List projectPropertiesPrefixes = new ArrayList();
        
        projectPropertiesPrefixes.add( "pom.properties." );
        projectPropertiesPrefixes.add( "project.properties." );
        
        PROJECT_PROPERTIES_PREFIXES = Collections.unmodifiableList( projectPropertiesPrefixes );
    }

    private AssemblyFormatUtils()
    {
    }

    /**
     * Get the full name of the distribution artifact
     *
     * @param assembly
     * @return the distribution name
     */
    public static String getDistributionName( Assembly assembly, AssemblerConfigurationSource configSource )
    {
        String finalName = configSource.getFinalName();
        boolean appendAssemblyId = configSource.isAssemblyIdAppended();
        String classifier = configSource.getClassifier();

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

    public static String getOutputDirectory( String output, MavenProject mainProject, MavenProject artifactProject,
                                             String finalName, AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException
    {
        return getOutputDirectory( output, mainProject, artifactProject, finalName, "artifact.", configSource );
    }

    /*
     * ORDER OF INTERPOLATION PRECEDENCE:
     *
     * 1. Support for special expressions, like ${finalName} (use the assembly plugin configuration not the build config)
     * 2. prefixed with artifactProjectRefName ("module." or "artifact.", normally).
     *    A. MavenProject instance for artifact
     * 3. prefixed with "artifact.", if artifactProjectRefName != "artifact."
     *    A. MavenProject instance for artifact
     * 4. user-defined properties from the command line
     * 5. prefixed with "pom." or "project.", or no prefix at all
     *    A. MavenProject instance from current build
     * 6. properties from main project
     * 7. system properties, from the MavenSession instance (to support IDEs)
     * 8. environment variables.
     * 
     */
    public static String getOutputDirectory( String output, MavenProject mainProject, MavenProject artifactProject,
                                             String finalName, String artifactProjectRefName,
                                             AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException
    {
        if ( artifactProjectRefName == null )
        {
            artifactProjectRefName = "artifact.";
        }

        String value = output;
        if ( value == null )
        {
            value = "";
        }

        if ( ( artifactProjectRefName != null ) && !artifactProjectRefName.endsWith( "." ) )
        {
            artifactProjectRefName += ".";
        }

        StringSearchInterpolator interpolator = new StringSearchInterpolator();

        Properties specialExpressionOverrides = new Properties();

        if ( finalName != null )
        {
            specialExpressionOverrides.setProperty( "finalName", finalName );
            specialExpressionOverrides.setProperty( "build.finalName", finalName );
        }

        // 1
        interpolator.addValueSource( new PropertiesBasedValueSource( specialExpressionOverrides ) );

        if ( artifactProject != null )
        {
            // 2
            interpolator.addValueSource( new PrefixedObjectValueSource( artifactProjectRefName, artifactProject ) );

            if ( !"artifact.".equals( artifactProjectRefName ) )
            {
                // 3
                interpolator.addValueSource( new PrefixedObjectValueSource( "artifact.", artifactProject ) );
            }
        }

        MavenSession session = configSource.getMavenSession();

        if ( session != null )
        {
            Properties userProperties = null;
            try
            {
                userProperties = session.getExecutionProperties();
            }
            catch ( NoSuchMethodError nsmer )
            {
                // OK, so user is using Maven <= 2.0.8. No big deal.
            }
            
            if ( userProperties != null )
            {
                // 4
                interpolator.addValueSource( new PropertiesBasedValueSource( userProperties ) );
            }
        }
        
        if ( mainProject != null )
        {
            // 5
            interpolator.addValueSource( new PrefixedObjectValueSource( PROJECT_PREFIXES, mainProject, true ) );
            
            // 6
            interpolator.addValueSource( new PrefixedPropertiesValueSource( PROJECT_PROPERTIES_PREFIXES, mainProject.getProperties(), true ) );
        }

        Properties commandLineProperties = System.getProperties();
        try
        {
            if ( session != null )
            {
                commandLineProperties = session.getExecutionProperties();
            }

        }
        catch ( NoSuchMethodError nsmer )
        {
            // OK, so user is using Maven <= 2.0.8. No big deal.
        }
        
        // 7
        interpolator.addValueSource( new PropertiesBasedValueSource( commandLineProperties ) );

        try
        {
            // 8
            interpolator.addValueSource( new PrefixedPropertiesValueSource( Collections.singletonList( "env." ), CommandLineUtils.getSystemEnvVars( false ), true ) );
        }
        catch ( IOException e )
        {
            throw new AssemblyFormattingException( "Failed to retrieve OS environment variables. Reason: " + e.getMessage(), e );
        }

        try
        {
            value = interpolator.interpolate( value );
        }
        catch ( InterpolationException e )
        {
            throw new AssemblyFormattingException( "Failed to interpolate output directory. Reason: " + e.getMessage(), e );
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

        return value;
    }

    public static String evaluateFileNameMapping( String expression, Artifact artifact, MavenProject mainProject,
                                                  MavenProject artifactProject,
                                                  AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException
    {
        return evaluateFileNameMapping( expression, artifact, mainProject, artifactProject, "artifact.", configSource );
    }

    /*
     * ORDER OF INTERPOLATION PRECEDENCE:
     *
     * 1. prefixed with artifactProjectRefName, from parameters list above.
     *    A. Artifact instance
     *    B. ArtifactHandler instance for artifact
     *    C. MavenProject instance for artifact
     * 2. prefixed with "artifact.", if artifactProjectRefName != "artifact."
     *    A. Artifact instance
     *    B. ArtifactHandler instance for artifact
     *    C. MavenProject instance for artifact
     * 3. prefixed with "pom." or "project."
     *    A. MavenProject instance from current build
     * 4. no prefix, using main project instance
     *    A. MavenProject instance from current build
     * 5. Support for special expressions, like ${dashClassifier?}
     * 6. user-defined properties from the command line
     * 7. properties from main project
     * 8. system properties, from the MavenSession instance (to support IDEs)
     * 9. environment variables.
     *
     */
    public static String evaluateFileNameMapping( String expression, Artifact artifact, MavenProject mainProject,
                                                  MavenProject artifactProject, String artifactProjectRefName,
                                                  AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException
    {
        String value = expression;

        if ( artifactProjectRefName == null )
        {
            artifactProjectRefName = "artifact.";
        }

        if ( !artifactProjectRefName.endsWith( "." ) )
        {
            artifactProjectRefName += ".";
        }

        // TODO: This is BAD! Accessors SHOULD NOT change the behavior of the object.
        // [jdcasey; 16-Aug-1007] This is fixed in SVN, just waiting for it to pass out of legacy.
        artifact.isSnapshot();

        StringSearchInterpolator interpolator = new StringSearchInterpolator();

        // 1A
        interpolator.addValueSource( new PrefixedObjectValueSource( artifactProjectRefName, artifact ) );

        // 1B
        interpolator.addValueSource( new PrefixedObjectValueSource( artifactProjectRefName, artifact.getArtifactHandler() ) );
        interpolator.addValueSource( new PrefixedObjectValueSource( artifactProjectRefName
                                                                         + ( artifactProjectRefName.endsWith( "." )
                                                                                         ? "" : "." ) + "handler.",
                                                                         artifact.getArtifactHandler() ) );

        // 1C
        if ( artifactProject != null )
        {
            interpolator.addValueSource( new PrefixedObjectValueSource( artifactProjectRefName, artifactProject ) );
        }

        if ( !"artifact.".equals( artifactProjectRefName ) )
        {
            // 2A
            interpolator.addValueSource( new PrefixedObjectValueSource( "artifact.", artifact ) );

            // 2B
            interpolator.addValueSource( new PrefixedObjectValueSource( "artifact.", artifact.getArtifactHandler() ) );
            interpolator.addValueSource( new PrefixedObjectValueSource( "artifact.handler.", artifact.getArtifactHandler() ) );

            // 2C
            if ( artifactProject != null )
            {
                interpolator.addValueSource( new PrefixedObjectValueSource( "artifact.", artifactProject ) );
            }
        }

        if ( mainProject != null )
        {
            // 3
            // 4
            interpolator.addValueSource( new PrefixedObjectValueSource( PROJECT_PREFIXES, mainProject, true ) );
        }

        Properties specialRules = new Properties();

        String classifier = artifact.getClassifier();
        if ( classifier != null )
        {
            specialRules.setProperty( "dashClassifier?",  "-" + classifier );
            specialRules.setProperty( "dashClassifier",  "-" + classifier );
        }
        else
        {
            specialRules.setProperty( "dashClassifier?", "" );
            specialRules.setProperty( "dashClassifier", "" );
        }

        // 5
        interpolator.addValueSource( new PropertiesBasedValueSource( specialRules ) );

        MavenSession session = configSource.getMavenSession();

        if ( session != null )
        {
            Properties userProperties = null;
            try
            {
                userProperties = session.getExecutionProperties();
            }
            catch ( NoSuchMethodError nsmer )
            {
                // OK, so user is using Maven <= 2.0.8. No big deal.
            }
            
            if ( userProperties != null )
            {
                // 6
                interpolator.addValueSource( new PropertiesBasedValueSource( userProperties ) );
            }
        }
        
        if ( mainProject != null )
        {
            // 7
            interpolator.addValueSource( new PrefixedPropertiesValueSource( PROJECT_PROPERTIES_PREFIXES, mainProject.getProperties(), true ) );
        }

        Properties commandLineProperties = System.getProperties();
        try
        {
            if ( session != null )
            {
                commandLineProperties = session.getExecutionProperties();
            }

        }
        catch ( NoSuchMethodError nsmer )
        {
            // OK, so user is using Maven <= 2.0.8. No big deal.
        }
        
        // 8
        interpolator.addValueSource( new PropertiesBasedValueSource( commandLineProperties ) );

        try
        {
            // 9
            interpolator.addValueSource( new PrefixedPropertiesValueSource( Collections.singletonList( "env." ), CommandLineUtils.getSystemEnvVars( false ), true ) );
        }
        catch ( IOException e )
        {
            throw new AssemblyFormattingException( "Failed to retrieve OS environment variables. Reason: " + e.getMessage(), e );
        }

        try
        {
            value = interpolator.interpolate( value );
        }
        catch ( InterpolationException e )
        {
            throw new AssemblyFormattingException( "Failed to interpolate output filename mapping. Reason: " + e.getMessage(), e );
        }

        value = StringUtils.replace( value, "//", "/" );
        value = StringUtils.replace( value, "\\\\", "\\" );
        
        return value;
    }

}
