package org.apache.maven.plugin.war.packaging;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.war.util.WebappStructureSerializer;

import java.io.File;
import java.io.IOException;

/**
 * Saves the webapp structure cache.
 *
 * @author Stephane Nicoll
 */
public class SaveWebappStructurePostPackagingTask
    implements WarPostPackagingTask
{

    private final File targetFile;

    private final WebappStructureSerializer serialier;


    public SaveWebappStructurePostPackagingTask( File targetFile )
    {
        this.targetFile = targetFile;
        this.serialier = new WebappStructureSerializer();
    }

    public void performPostPackaging( WarPackagingContext context )
        throws MojoExecutionException, MojoFailureException
    {
        if ( targetFile == null )
        {
            context.getLog().debug( "Cache usage is disabled, not saving webapp structure." );
        }
        else
        {
            try
            {
                serialier.toXml( context.getWebappStructure(), targetFile );
                context.getLog().debug( "Cache saved successfully." );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Could not save webapp structure", e );
            }
        }
    }
}
