package org.apache.maven.plugins;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo( name="ear" )
public class EarMojo extends AbstractMojo
{
    @Component
    private MavenProject project;
    
    /**
     * Directory containing the generated EAR.
     */
    @Parameter( defaultValue = "${project.build.directory}", required=true )
    private File outputDirectory;

    /**
     * Name of the generated EAR.
     */
    @Parameter( alias = "earName", property = "ear.finalName", defaultValue = "${project.build.finalName}", required = true )
    private String finalName;

    public void execute() throws MojoExecutionException
    {
        File targetFile = new File( outputDirectory, finalName + ".ear" );
        
        try
        {
            targetFile.getParentFile().mkdirs();
            targetFile.createNewFile();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        
        project.getArtifact().setFile( targetFile );
    }
}