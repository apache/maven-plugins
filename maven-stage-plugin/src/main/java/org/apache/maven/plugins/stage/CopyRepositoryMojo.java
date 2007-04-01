package org.apache.maven.plugins.stage;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @author Jason van Zyl
 * @goal copy
 */
public class CopyRepositoryMojo
    extends AbstractMojo
{
    /** @param expression="${sourceRepository}" */
    private String sourceRepository;

    /** @param expression="${targetRepository}" */
    private String targetRepository;

    public void execute()
        throws MojoExecutionException
    {
    }
}

