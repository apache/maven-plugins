package org.apache.maven.plugin.assembly.utils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.util.interpolation.RegexBasedInterpolator;

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
    
    public static String getOutputDirectory( String output, MavenProject project, String finalName )
    {
        String value = output;
        if ( value == null )
        {
            value = "";
        }
        
        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
        
        Properties specialExpressionOverrides = new Properties();
        
        if ( finalName != null )
        {
            specialExpressionOverrides.setProperty( "finalName", finalName );
            specialExpressionOverrides.setProperty( "build.finalName", finalName );
        }
        
        interpolator.addValueSource( new PropertiesInterpolationValueSource( specialExpressionOverrides ) );
        
        if ( project != null )
        {
            interpolator.addValueSource( new ObjectBasedValueSource( project ) );
        }
        
        value = interpolator.interpolate( value, "__project" );
        
        if ( value.length() > 0 && !value.endsWith( "/" ) && !value.endsWith( "\\" ) )
        {
            value += "/";
        }
        
        if ( value.length() > 0 && ( value.startsWith( "/" ) || value.startsWith( "\\" ) ) )
        {
            value = value.substring( 1 );
        }

        return value;
    }

    /**
     * Evaluates Filename Mapping
     * 
     * @param expression
     * @param artifact
     * @return expression
     * @throws AssemblyFormattingException 
     * @throws org.apache.maven.plugin.MojoExecutionException
     */
    public static String evaluateFileNameMapping( String expression, Artifact artifact )
        throws AssemblyFormattingException
    {
        String value = expression;
        
        // FIXME: This is BAD! Accessors SHOULD NOT change the behavior of the object.
        artifact.isSnapshot();

        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();
        
        interpolator.addValueSource( new ObjectBasedValueSource( artifact ) );
        interpolator.addValueSource( new ObjectBasedValueSource( artifact.getArtifactHandler() ) );
        
        Properties classifierMask = new Properties();
        classifierMask.setProperty( "classifier", "" );
        
        interpolator.addValueSource( new PropertiesInterpolationValueSource( classifierMask ) );
        
        value = interpolator.interpolate( value, "__artifact" );

        return value;
    }

}
