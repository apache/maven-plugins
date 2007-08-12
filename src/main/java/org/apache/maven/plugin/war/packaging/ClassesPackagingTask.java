package org.apache.maven.plugin.war.packaging;

import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.war.Overlay;
import org.apache.maven.plugin.war.util.PathSet;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.ManifestException;

import java.io.File;
import java.io.IOException;

/**
 * Handles the classes directory that needs to be packaged in the web application.
 * <p/>
 * Based on the {@link WarPackagingContext#archiveClasses()} flag the resources
 * either copied into to <tt>WEB-INF/classes</tt> directory or archived in a jar
 * within the <tt>WEB-INF/lib</tt> directory.
 *
 * @author Stephane Nicoll
 */
public class ClassesPackagingTask
    extends AbstractWarPackagingTask
{

    public void performPackaging( WarPackagingContext context )
        throws MojoExecutionException
    {
        final File webappClassesDirectory = new File( context.getWebappDirectory(), CLASSES_PATH );
        if ( !webappClassesDirectory.exists() )
        {
            webappClassesDirectory.mkdirs();
        }

        if ( context.getClassesDirectory().exists() && !context.getClassesDirectory().equals( webappClassesDirectory ) )
        {
            if ( context.archiveClasses() )
            {
                generateJarArchive( context );
            }
            else
            {
                final PathSet sources = getFilesToIncludes( context.getClassesDirectory(), null, null );
                try
                {
                    copyFiles( Overlay.currentProjectInstance().getId(), context, context.getClassesDirectory(),
                               sources, CLASSES_PATH );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException(
                        "Could not copy webapp classes[" + context.getClassesDirectory().getAbsolutePath() + "]", e );
                }
            }
        }
    }

    protected void generateJarArchive( WarPackagingContext context )
        throws MojoExecutionException
    {
        //TODO use ArtifactFactory and resolve the final name the usual way instead
        final String archiveName = context.getProject().getBuild().getFinalName() + ".jar";
        final String targetFilename = LIB_PATH + archiveName;

        if ( context.getWebappStructure().registerFile( Overlay.currentProjectInstance().getId(), targetFilename ) )
        {

            final File libDirectory = new File( context.getWebappDirectory(), LIB_PATH );
            final File jarFile = new File( libDirectory, archiveName );

            try
            {
                final MavenArchiver archiver = new MavenArchiver();
                archiver.setArchiver( context.getJarArchiver() );
                archiver.setOutputFile( jarFile );
                archiver.getArchiver().addDirectory( context.getClassesDirectory(), context.getWebappSourceIncludes(),
                                                     context.getWebappSourceExcludes() );
                archiver.createArchive( context.getProject(), context.getArchive() );
            }
            catch ( ArchiverException e )
            {
                throw new MojoExecutionException( "Could not create classes archive", e );
            }
            catch ( ManifestException e )
            {
                throw new MojoExecutionException( "Could not create classes archive", e );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Could not create classes archive", e );
            }
            catch ( DependencyResolutionRequiredException e )
            {
                throw new MojoExecutionException( "Could not create classes archive", e );
            }
        }
        else
        {
            context.getLog().warn(
                "Could not generate archive classes file[" + targetFilename + "] has already been copied." );
        }
    }
}
