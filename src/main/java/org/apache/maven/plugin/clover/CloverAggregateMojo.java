package org.apache.maven.plugin.clover;

import org.apache.maven.project.MavenProject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.clover.internal.AbstractCloverMojo;

import java.io.File;
import java.util.*;

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
public class CloverAggregateMojo extends AbstractCloverMojo
{
    /**
     * The projects in the reactor for aggregation report.
     *
     * <p>Note: This is passed by Maven and must not be configured by the user.</p>
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    private List reactorProjects;

    /**
     * {@inheritDoc}
     * @see org.apache.maven.plugin.clover.internal.AbstractCloverMojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        // If we're in a module with children modules, then aggregate the children clover databases.
        if ( getProject().getModules().size() > 0 )
        {
            super.execute();

            // Ensure all databases are flushed
            AbstractCloverMojo.waitForFlush( getWaitForFlush(), getFlushInterval() );

            if ( getChildrenCloverDatabases().size() > 0 )
            {
                // Ensure the merged database output directory exists
                new File( getCloverMergeDatabase() ).getParentFile().mkdirs();

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
            getCloverDatabase().substring( getProject().getBasedir().getPath().length() );

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

        List parameters = new ArrayList();

        parameters.add( "-i" );
        parameters.add( getCloverMergeDatabase() );

        if ( getLog().isDebugEnabled() )
        {
           parameters.add( "-d" );
        }

        parameters.addAll( dbFiles );

        int mergeResult = CloverMerge.mainImpl( (String[]) parameters.toArray(new String[0]) );
        if ( mergeResult != 0 )
        {
            throw new MojoExecutionException( "Clover has failed to merge the children module databases" );
        }
    }
}
