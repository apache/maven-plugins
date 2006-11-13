package org.apache.maven.plugin.eclipse;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.EclipseCleanMojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

/**
 * Deletes the config files used by Rad-6. the files .j2ee and the file
 * .websettings
 * 
 * @author <a href="mailto:nir@cfc.at">Richard van Nieuwenhoven</a>
 * @goal rad-clean
 */
public class RadCleanMojo
    extends EclipseCleanMojo
{
    /**
     * The project whose project files to clean.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    protected void cleanExtras()
        throws MojoExecutionException
    {
        delete( new File( getBasedir(), ".j2ee" ) );
        delete( new File( getBasedir(), ".websettings" ) );
        delete( new File( getBasedir(), ".website-config" ) );

        handleLibs();
    }

    /**
     * getter for the instancevarriable project.
     * 
     * @return the maven project decriptor
     */
    public MavenProject getProject()
    {
        return this.project;
    }

    /**
     * getter for the instancevarriable project.
     * 
     * @param project
     *            the maven project decriptor
     */
    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    /**
     * Delete all jars in the EAR project root directory.
     * 
     * @throws MojoExecutionException
     *             only if a file exists and can't be deleted
     */
    private void handleEarLibs()
        throws MojoExecutionException
    {
        File targetDir = this.project.getBasedir();
        deleteJarArtifactsInDirectory( targetDir );
    }

    /**
     * Delete all jars in the project that were required by rad6.
     * 
     * @throws MojoExecutionException
     *             only if a file exists and can't be deleted
     */
    private void handleLibs()
        throws MojoExecutionException
    {
        String packaging = this.project.getPackaging();
        
        if ( Constants.PROJECT_PACKAGING_EAR.equals( packaging ) )
        {
            handleEarLibs();
        }
        else if ( Constants.PROJECT_PACKAGING_WAR.equals( packaging ) )
        {
            handleWarLibs();
        }
    }

    /**
     * Delete all jars in the WAR project WEB-INF/lib directory.
     * 
     * @throws MojoExecutionException
     *             only if a file exists and can't be deleted
     */
    private void handleWarLibs()
        throws MojoExecutionException
    {
        String srcMainWebappWebInfLibDirname = this.project.getBasedir().getAbsolutePath() + File.separatorChar + "src"
            + File.separatorChar + "main" + File.separatorChar + "webapp" + File.separatorChar + "WEB-INF"
            + File.separatorChar + "lib";

        File srcMainWebappWebInfLibDir = new File( srcMainWebappWebInfLibDirname );
        srcMainWebappWebInfLibDir.mkdirs();

        deleteJarArtifactsInDirectory( srcMainWebappWebInfLibDir );
    }

    /**
     * delete all Jar artifacts in the spedified directory.
     * 
     * @param directory
     *            to delete the jars from
     * @throws MojoExecutionException
     *             only if a file exists and can't be deleted
     */
    protected void deleteJarArtifactsInDirectory( File directory )
        throws MojoExecutionException
    {
        String[] oldFiles = FileUtils.getFilesFromExtension( directory.getAbsolutePath(),
                                                             new String[] { Constants.PROJECT_PACKAGING_JAR } );
        for ( int index = 0; index < oldFiles.length; index++ )
        {
            File f = new File( oldFiles[index] );
            
            delete( f );
        }
    }
}
