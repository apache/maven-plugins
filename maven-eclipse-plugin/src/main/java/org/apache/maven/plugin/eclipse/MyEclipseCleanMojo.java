package org.apache.maven.plugin.eclipse;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Deletes configuration files used by MyEclipse
 * 
 * @author Olivier Jacob
 * @goal myeclipse-clean
 * @since 2.5
 * @phase
 */
public class MyEclipseCleanMojo
    extends EclipseCleanMojo
{
    /**
     * @throws MojoExecutionException
     */
    protected void cleanExtras()
        throws MojoExecutionException
    {
        delete( new File( getBasedir(), ".mymetadata" ) );
        delete( new File( getBasedir(), ".mystrutsdata" ) );
        delete( new File( getBasedir(), ".myhibernatedata" ) );
        delete( new File( getBasedir(), ".springBeans" ) );
    }
}
