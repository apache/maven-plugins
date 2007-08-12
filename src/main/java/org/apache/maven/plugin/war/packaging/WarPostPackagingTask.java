package org.apache.maven.plugin.war.packaging;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Defines tasks that should be performed after the packaging.
 *
 * @author Stephane Nicoll
 */
public interface WarPostPackagingTask
{

    /**
     * Executes the post packaging task.
     * <p/>
     * The packaging context hold all information regarding the webapp that
     * has been packaged.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if an error occured
     * @throws MojoFailureException   if a falure occured
     */
    void performPostPackaging( WarPackagingContext context )
        throws MojoExecutionException, MojoFailureException;

}
