package org.apache.maven.plugins.stage;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @author Jason van Zyl
 * @requiresProject false
 * @goal copy
 */
public class CopyRepositoryMojo
    extends AbstractMojo
{
    /** @parameter expression="${source}" */
    private String source;

    /** @parameter expression="${target}" */
    private String target;

    /**
     * @parameter expression="${version}"
     * @required
     */
    private String version;

    /** @component */
    private RepositoryCopier copier;

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            copier.copy( source, target, version );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException(
                "Error copying repository from " + source + " to " + target, e );
        }
    }
}

