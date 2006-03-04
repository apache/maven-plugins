package org.apache.maven.plugin.idea;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;

/**
 * Plugin to remove existing idea files on the project
 *
 * @goal clean
 * @author Edwin Punzalan
 */
public class IdeaCleanMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    public void initParams( MavenProject project )
    {
        this.project = project;
    }

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        File files[] = project.getBasedir().listFiles();

        for ( int idx = 0; idx < files.length; idx++ )
        {
            File file = files[ idx ];

            if ( file.getName().endsWith( ".ipr" ) ||
                 file.getName().endsWith( ".iml" ) ||
                 file.getName().endsWith( ".iws" ) )
            {
                getLog().debug( "Deleting " + file.getAbsolutePath() + "...");
                FileUtils.fileDelete( file.getAbsolutePath() );
            }
        }
    }
}
