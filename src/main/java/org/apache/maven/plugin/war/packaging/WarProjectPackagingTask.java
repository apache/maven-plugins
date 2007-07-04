package org.apache.maven.plugin.war.packaging;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.war.util.PathSet;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.io.IOException;

/**
 * Handles the project own resources, that is:
 * <ul
 * <li>The list of web resources, if any</li>
 * <li>The content of the webapp directory if it exists</li>
 * <li>The dependencies of the project</li>
 * </ul>
 *
 * @author Stephane Nicoll
 */
public class WarProjectPackagingTask
    extends AbstractWarPackagingTask
{
    private final Resource[] webResources;


    public WarProjectPackagingTask( Resource[] webResources )
    {
        if ( webResources != null )
        {
            this.webResources = webResources;
        }
        else
        {
            this.webResources = new Resource[0];
        }
    }

    public void performPackaging( WarPackagingContext context )
        throws MojoExecutionException
    {

        handleWebResources( context );

        handeWebAppSourceDirectory( context );

        handleArtifacts( context );
    }


    /**
     * Handles the web resources.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if a resource could not be copied
     */
    protected void handleWebResources( WarPackagingContext context )
        throws MojoExecutionException
    {
        for ( int i = 0; i < webResources.length; i++ )
        {
            Resource resource = webResources[i];
            if ( !( new File( resource.getDirectory() ) ).isAbsolute() )
            {
                resource.setDirectory( context.getProject().getBasedir() + File.separator + resource.getDirectory() );
            }

            // Make sure that the resource directory is not the same as the webappDirectory
            if ( !resource.getDirectory().equals( context.getWebAppDirectory().getPath() ) )
            {

                try
                {
                    copyResources( context, resource );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Could not copy resource[" + resource.getDirectory() + "]", e );
                }
            }
        }
    }

    /**
     * Handles the webapp sources.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if the sources could not be copied
     */
    protected void handeWebAppSourceDirectory( WarPackagingContext context )
        throws MojoExecutionException
    {

        // Make sure that the resource directory is not the same as the webappDirectory
        if ( !context.getWebAppSourceDirectory().getAbsolutePath().equals( context.getWebAppDirectory().getPath() ) )
        {
            final PathSet sources = getFilesToIncludes( context.getWebAppSourceDirectory(),
                                                        context.getWebAppSourceIncludes(),
                                                        context.getWebAppSourceExcludes() );

            try
            {
                copyFiles( context, context.getWebAppSourceDirectory(), sources );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException(
                    "Could not copy webapp sources[" + context.getWebAppDirectory().getAbsolutePath() + "]", e );
            }
        }
    }

    /**
     * Handles the webapp artifacts.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if the artifacts could not be copied
     */
    protected void handleArtifacts( WarPackagingContext context )
        throws MojoExecutionException
    {
        ArtifactsPackagingTask task = new ArtifactsPackagingTask( context.getProject().getArtifacts() );
        task.performPackaging( context );
    }


    /**
     * Copies webapp webResources from the specified directory.
     *
     * @param context  the war packaging context to use
     * @param resource the resource to copy
     * @throws IOException            if an error occured while copying the resources
     * @throws MojoExecutionException if an error occured while retrieving the filter properties
     */
    public void copyResources( WarPackagingContext context, Resource resource )
        throws IOException, MojoExecutionException
    {
        if ( !context.getWebAppDirectory().exists() )
        {
            context.getLogger().warn( "Not copyuing webapp webResources[" + resource.getDirectory() +
                "]: webapp directory[" + context.getWebAppDirectory().getAbsolutePath() + "] does not exist!" );
        }

        context.getLogger().info( "Copy webapp webResources[" + resource.getDirectory() + "] to[" +
            context.getWebAppDirectory().getAbsolutePath() + "]" );
        String[] fileNames = getFilesToCopy( resource );
        for ( int i = 0; i < fileNames.length; i++ )
        {
            String targetFileName = fileNames[i];
            if ( resource.getTargetPath() != null )
            {
                //TODO make sure this thing is 100% safe
                targetFileName = resource.getTargetPath() + File.separator + targetFileName;
            }
            if ( resource.isFiltering() )
            {
                copyFilteredFile( context, new File( resource.getDirectory(), fileNames[i] ), targetFileName );
            }
            else
            {
                copyFile( context, new File( resource.getDirectory(), fileNames[i] ), targetFileName );
            }
        }
    }


    /**
     * Returns a list of filenames that should be copied
     * over to the destination directory.
     *
     * @param resource the resource to be scanned
     * @return the array of filenames, relative to the sourceDir
     */
    private String[] getFilesToCopy( Resource resource )
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( resource.getDirectory() );
        if ( resource.getIncludes() != null && !resource.getIncludes().isEmpty() )
        {
            scanner.setIncludes(
                (String[]) resource.getIncludes().toArray( new String[resource.getIncludes().size()] ) );
        }
        else
        {
            scanner.setIncludes( DEFAULT_INCLUDES );
        }
        if ( resource.getExcludes() != null && !resource.getExcludes().isEmpty() )
        {
            scanner.setExcludes(
                (String[]) resource.getExcludes().toArray( new String[resource.getExcludes().size()] ) );
        }

        scanner.addDefaultExcludes();

        scanner.scan();

        return scanner.getIncludedFiles();
    }
}
