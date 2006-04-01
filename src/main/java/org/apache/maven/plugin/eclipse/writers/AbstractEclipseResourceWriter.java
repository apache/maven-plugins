/**
 * 
 */
package org.apache.maven.plugin.eclipse.writers;

import java.io.File;

import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * Common base class for all Eclipse Writers.
 * 
 * @author <a href="mailto:rahul.thakur.xdev@gmail.com">Rahul Thakur</a>
 * @version $Id$
 */
public abstract class AbstractEclipseResourceWriter
{

    private Log log;

    private File eclipseProjectDir;

    private MavenProject project;

    protected IdeDependency[] deps;

    /**
     * @param log
     * @param eclipseProjectDir
     * @param project
     */
    public AbstractEclipseResourceWriter( Log log, File eclipseProjectDir, MavenProject project, IdeDependency[] deps )
    {
        this.log = log;
        this.eclipseProjectDir = eclipseProjectDir;
        this.project = project;
        this.deps = deps;
    }

    /**
     * @return the eclipseProjectDir
     */
    public File getEclipseProjectDirectory()
    {
        return eclipseProjectDir;
    }

    /**
     * @return the log
     */
    public Log getLog()
    {
        return log;
    }

    /**
     * @return the project
     */
    public MavenProject getProject()
    {
        return project;
    }

}
