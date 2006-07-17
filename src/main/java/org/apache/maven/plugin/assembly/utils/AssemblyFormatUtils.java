package org.apache.maven.plugin.assembly.utils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    public static String getOutputDirectory( String output, MavenProject project, String finalName,
                                             boolean includeBaseDirectory )
    {
        String value = output;
        if ( value == null )
        {
            value = "";
        }
        
        if ( !value.endsWith( "/" ) && !value.endsWith( "\\" ) )
        {
            // TODO: shouldn't archiver do this?
            value += '/';
        }

        if ( includeBaseDirectory )
        {
            if ( value.startsWith( "/" ) )
            {
                value = finalName + value;
            }
            else
            {
                value = finalName + "/" + value;
            }
        }
        else
        {
            if ( value.startsWith( "/" ) )
            {
                value = value.substring( 1 );
            }
        }

        if ( project != null )
        {
            value = StringUtils.replace( value, "${groupId}", project.getGroupId() );
            value = StringUtils.replace( value, "${artifactId}", project.getArtifactId() );
            value = StringUtils.replace( value, "${version}", project.getVersion() );

            Build build = project.getBuild();
            value = StringUtils.replace( value, "${build.finalName}", build.getFinalName() );
            value = StringUtils.replace( value, "${finalName}", build.getFinalName() );
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

        // insert the classifier if exist
        if ( !StringUtils.isEmpty( artifact.getClassifier() ) )
        {
            int dotIdx = value.lastIndexOf( "." );

            if ( dotIdx >= 0 )
            {
                String extension = value.substring( dotIdx + 1, value.length() );
                String artifactWithoutExt = value.substring( 0, dotIdx );

                value = artifactWithoutExt + "-" + artifact.getClassifier() + "." + extension;
            }
            else
            {
                value = value + "-" + artifact.getClassifier();
            }
        }

        // this matches the last ${...} string
        Pattern pat = Pattern.compile( "^(.*)\\$\\{([^\\}]+)\\}(.*)$" );
        Matcher mat = pat.matcher( expression );

        if ( mat.matches() )
        {
            Object middle;
            String left = evaluateFileNameMapping( mat.group( 1 ), artifact );
            try
            {
                middle = ReflectionValueExtractor.evaluate( mat.group( 2 ), artifact, false );
            }
            catch ( Exception e )
            {
                throw new AssemblyFormattingException( "Cannot evaluate filenameMapping: '" + mat.group( 2 ) + "': "
                    + e.getMessage(), e );
            }
            String right = mat.group( 3 );

            if ( middle == null )
            {
                // TODO: There should be a more generic way dealing with that.
                // Having magic words is not good at all.
                // probe for magic word
                if ( "extension".equals( mat.group( 2 ).trim() ) )
                {
                    ArtifactHandler artifactHandler = artifact.getArtifactHandler();
                    middle = artifactHandler.getExtension();
                }
                else
                {
                    middle = "${" + mat.group( 2 ) + "}";
                }
            }

            value = left + middle + right;
        }

        return value;
    }

}
