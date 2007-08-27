package org.apache.maven.plugin.assembly.utils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.util.interpolation.RegexBasedInterpolator;

import java.io.IOException;
import java.util.Properties;

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
                                             String finalName )
        throws AssemblyFormattingException
    {
        return getOutputDirectory( output, mainProject, artifactProject, finalName, "artifact." );
    }

    /*
     * ORDER OF INTERPOLATION PRECEDENCE:
     *
     * 1. Support for special expressions, like ${dashClassifier?}
     * 2. prefixed with artifactProjectRefName, from parameters list above.
     *    A. MavenProject instance for artifact
     * 3. prefixed with "artifact.", if artifactProjectRefName != "artifact."
     *    A. MavenProject instance for artifact
     * 4. prefixed with "pom."
     *    A. MavenProject instance from current build
     * 5. no prefix, using main project instance
     *    A. MavenProject instance from current build
     * 6. System properties
     * 7. environment variables.
     *
     */
    public static String getOutputDirectory( String output, MavenProject mainProject, MavenProject artifactProject,
                                             String finalName, String artifactProjectRefName )
        throws AssemblyFormattingException
    {
        if ( artifactProjectRefName == null )
        {
            artifactProjectRefName = "artifact.";
        }

        if ( !artifactProjectRefName.endsWith( "." ) )
        {
            artifactProjectRefName += ".";
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

        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

        Properties specialExpressionOverrides = new Properties();

        if ( finalName != null )
        {
            specialExpressionOverrides.setProperty( "finalName", finalName );
            specialExpressionOverrides.setProperty( "build.finalName", finalName );
        }

        interpolator.addValueSource( new PropertiesInterpolationValueSource( specialExpressionOverrides ) );

        if ( mainProject != null )
        {
            interpolator.addValueSource( new PrefixedObjectBasedValueSource( "pom.", mainProject ) );
        }

        if ( artifactProject != null )
        {
            interpolator.addValueSource( new PrefixedObjectBasedValueSource( artifactProjectRefName, artifactProject ) );

            if ( !"artifact.".equals( artifactProjectRefName ) )
            {
                interpolator.addValueSource( new PrefixedObjectBasedValueSource( "artifact.", artifactProject ) );
            }
        }

        if ( mainProject != null )
        {
            interpolator.addValueSource( new ObjectBasedValueSource( mainProject ) );
        }

        // 6
        interpolator.addValueSource( new PropertiesInterpolationValueSource( System.getProperties() ) );

        try
        {
            // 7
            interpolator.addValueSource( new PropertiesInterpolationValueSource( CommandLineUtils.getSystemEnvVars( false ) ) );
        }
        catch ( IOException e )
        {
            throw new AssemblyFormattingException( "Failed to retrieve OS environment variables. Reason: " + e.getMessage(), e );
        }

        value = interpolator.interpolate( value, "__project" );

        if ( ( value.length() > 0 ) && !value.endsWith( "/" ) && !value.endsWith( "\\" ) )
        {
            value += "/";
        }

        if ( ( value.length() > 0 ) && ( value.startsWith( "/" ) || value.startsWith( "\\" ) ) )
        {
            value = value.substring( 1 );
        }

        return value;
    }

    public static String evaluateFileNameMapping( String expression, Artifact artifact, MavenProject mainProject, MavenProject artifactProject )
        throws AssemblyFormattingException
    {
        return evaluateFileNameMapping( expression, artifact, mainProject, artifactProject, "artifact." );
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
     * 3. prefixed with "pom."
     *    A. MavenProject instance from current build
     * 4. no prefix, using main project instance
     *    A. MavenProject instance from current build
     * 5. Support for special expressions, like ${dashClassifier?}
     * 6. System properties
     * 7. environment variables.
     *
     */
    public static String evaluateFileNameMapping( String expression, Artifact artifact, MavenProject mainProject, MavenProject artifactProject, String artifactProjectRefName )
        throws AssemblyFormattingException
    {
        System.out.println( "in evaluateFileNameMapping, using expression: " + expression + "\nartifact: "
                            + artifact.getId() + "\nmainProject: "
                            + ( mainProject != null ? mainProject.getId() : "null" ) + "\nartifactProject: "
                            + ( artifactProject != null ? artifactProject.getId() : "null" )
                            + "\nartifactProjectRefName: " + artifactProjectRefName );

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

        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

        // 1A
        interpolator.addValueSource( new PrefixedObjectBasedValueSource( artifactProjectRefName, artifact ) );

        // 1B
        interpolator.addValueSource( new PrefixedObjectBasedValueSource( artifactProjectRefName, artifact.getArtifactHandler() ) );
        interpolator.addValueSource( new PrefixedObjectBasedValueSource( artifactProjectRefName
                                                                         + ( artifactProjectRefName.endsWith( "." )
                                                                                         ? "" : "." ) + "handler.",
                                                                         artifact.getArtifactHandler() ) );

        // 1C
        if ( artifactProject != null )
        {
            interpolator.addValueSource( new PrefixedObjectBasedValueSource( artifactProjectRefName, artifactProject ) );
        }

        if ( !"artifact.".equals( artifactProjectRefName ) )
        {
            // 2A
            interpolator.addValueSource( new PrefixedObjectBasedValueSource( "artifact.", artifact ) );

            // 2B
            interpolator.addValueSource( new PrefixedObjectBasedValueSource( "artifact.", artifact.getArtifactHandler() ) );
            interpolator.addValueSource( new PrefixedObjectBasedValueSource( "artifact.handler.", artifact.getArtifactHandler() ) );

            // 2C
            if ( artifactProject != null )
            {
                interpolator.addValueSource( new PrefixedObjectBasedValueSource( "artifact.", artifactProject ) );
            }
        }

        if ( mainProject != null )
        {
            // 3
            interpolator.addValueSource( new PrefixedObjectBasedValueSource( "pom.", mainProject ) );

            // 4
            interpolator.addValueSource( new ObjectBasedValueSource( mainProject ) );
        }

        Properties specialRules = new Properties();

        String classifier = artifact.getClassifier();
        if ( classifier != null )
        {
            specialRules.setProperty( "dashClassifier?",  "-" + classifier );
        }
        else
        {
            specialRules.setProperty( "dashClassifier?", "" );
        }

        // 5
        interpolator.addValueSource( new PropertiesInterpolationValueSource( specialRules ) );

        // 6
        interpolator.addValueSource( new PropertiesInterpolationValueSource( System.getProperties() ) );

        try
        {
            // 7
            interpolator.addValueSource( new PropertiesInterpolationValueSource( CommandLineUtils.getSystemEnvVars( false ) ) );
        }
        catch ( IOException e )
        {
            throw new AssemblyFormattingException( "Failed to retrieve OS environment variables. Reason: " + e.getMessage(), e );
        }

        // Now, run the interpolation using the rules stated above.
        value = interpolator.interpolate( value, "__artifact" );

        System.out.println( "Result of outputFileNameMapping evaluation: \'" + value + "\'" );

        return value;
    }

}
