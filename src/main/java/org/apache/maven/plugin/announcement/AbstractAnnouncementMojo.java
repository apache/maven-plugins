package org.apache.maven.plugin.announcement;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.execution.MavenSession;

/**
 * Abstract superclass for announcement mojos.
 *
 * @version $Id$
 * @since 2.3
 */
public abstract class AbstractAnnouncementMojo extends AbstractMojo {
    /**
     * The current project base directory.
     *
     * @parameter expression="${basedir}"
     * @required
     * @since 2.1
     */
    protected String basedir;

    /**
     * The Maven Session.
     *
     * @parameter expression="${session}"
     * @readonly
     * @required
     * @since 2.3
     */
    protected MavenSession mavenSession;

    /**
     * This will cause the execution to be run only at the top of a given module
     * tree. That is, run in the project contained in the same folder where the
     * mvn execution was launched.
     *
     * @parameter expression="${announcement.runOnlyAtExecutionRoot}" default-value="false"
     * @since 2.3
     */
    protected boolean runOnlyAtExecutionRoot;

    /**
     * Returns <code>true</code> if the current project is located at the
     * Execution Root Directory (where mvn was launched).
     *
     * @return <code>true</code> if the current project is at the Execution Root
     */
    protected boolean isThisTheExecutionRoot()
    {
        getLog().debug( "Root Folder:" + mavenSession.getExecutionRootDirectory() );
        getLog().debug( "Current Folder:" + basedir );
        boolean result = mavenSession.getExecutionRootDirectory().equalsIgnoreCase( basedir.toString() );
        if ( result )
        {
            getLog().debug( "This is the execution root." );
        }
        else
        {
            getLog().debug( "This is NOT the execution root." );
        }
        return result;
    }
}
