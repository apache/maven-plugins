package org.apache.maven.plugin.clover;

import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.doxia.sink.Sink;

import java.io.File;
import java.util.*;

import com.cenqua.clover.reporters.html.HtmlReporter;
import com.cenqua.clover.CloverMerge;

/**
 * Aggregate children module Clover databases if there are any. This mojo should not exist. It's only there because
 * the site plugin doesn't handle @aggregators properly at the moment...
 *
 * @goal aggregate
 * @aggregator
 *
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @version $Id$
 */
public class CloverAggregateMojo extends AbstractMojo
{
    /**
     * The location of the <a href="http://cenqua.com/clover/doc/adv/database.html">Clover database</a>.
     *
     * @parameter expression="${project.build.directory}/clover/clover.db"
     * @required
     */
    private String cloverDatabase;

    /**
     * The location of the merged clover database to create when running a report in a multimodule build.
     *
     * @parameter expression="${project.build.directory}/clover/cloverMerge.db"
     * @required
     */
    private String cloverMergeDatabase;

    /**
     * The directory where the Clover report will be generated.
     *
     * @parameter expression="${project.reporting.outputDirectory}/clover"
     * @required
     */
    private File outputDirectory;

    /**
     * When the Clover Flush Policy is set to "interval" or threaded this value is the minimum
     * period between flush operations (in milliseconds).
     *
     * @parameter default-value="500"
     */
    protected int flushInterval;

    /**
     * If true we'll wait 2*flushInterval to ensure coverage data is flushed to the Clover
     * database before running any query on it.
     *
     * Note: The only use case where you would want to turn this off is if you're running your
     * tests in a separate JVM. In that case the coverage data will be flushed by default upon
     * the JVM shutdown and there would be no need to wait for the data to be flushed. As we
     * can't control whether users want to fork their tests or not, we're offering this parameter
     * to them.
     *
     * @parameter default-value="true"
     */
    protected boolean waitForFlush;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The projects in the reactor for aggregation report.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    private List reactorProjects;

    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.clover.AbstractCloverMojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        // If we're in a module with children modules, then aggregate the children clover databases.
        if ( this.project.getModules().size() > 0 )
        {
            // Ensure all databases are flushed
            AbstractCloverMojo.waitForFlush( this.waitForFlush, this.flushInterval );

            if ( getChildrenCloverDatabases().size() > 0 )
            {
                // Ensure the merged database output directory exists
                new File( this.cloverMergeDatabase ).getParentFile().mkdirs();

                // Merge the databases
                mergeCloverDatabases();
            }
            else
            {
                getLog().warn("No Clover databases found in children projects - No merge done");
            }
        }
    }

    private List getChildrenCloverDatabases()
    {
        // Ideally we'd need to find out where each module stores its Clover database. However that's not
        // currently possible in m2 (see http://jira.codehaus.org/browse/MNG-2180). Thus we'll assume for now
        // that all modules use the cloverDatabase configuration from the top level module.

        // Find out the location of the clover DB relative to the root module.
        // Note: This is a pretty buggy algorithm and we really need a proper solution (see MNG-2180)
        String relativeCloverDatabasePath =
            this.cloverDatabase.substring(this.project.getBasedir().getPath().length());

        List dbFiles = new ArrayList();
        for ( Iterator projects = this.reactorProjects.iterator(); projects.hasNext(); )
        {
            MavenProject project = (MavenProject) projects.next();

            File cloverDb = new File(project.getBasedir(), relativeCloverDatabasePath);
            if (cloverDb.exists())
            {
                dbFiles.add(cloverDb.getPath());
            }
            else
            {
                getLog().debug("Skipping [" + cloverDb.getPath() + "] as it doesn't exist.");
            }
        }

        return dbFiles;
    }

    private void mergeCloverDatabases() throws MojoExecutionException
    {
        List dbFiles = getChildrenCloverDatabases();

        String[] args = new String[dbFiles.size() + 2];
        args[0] = "-i";
        args[1] = this.cloverMergeDatabase;

        int i = 2;
        for ( Iterator dbs = dbFiles.iterator(); dbs.hasNext(); )
        {
            args[i] = (String) dbs.next();
            i++;
        }

        int mergeResult = CloverMerge.mainImpl( args );
        if ( mergeResult != 0 )
        {
            throw new MojoExecutionException( "Clover has failed to merge the children module databases" );
        }
    }
}
