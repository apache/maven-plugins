package org.apache.maven.plugin.source;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Aggregrate sources for all modules in a aggregator project.
 *
 * @goal aggregate
 * @phase package
 * @aggregator
 * @execute phase="generate-sources"
 */
public class AggregatorSourceJarMojo
    extends SourceJarMojo
{
    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( "pom".equals( project.getPackaging() ) )
        {
            packageSources( reactorProjects );
        }
    }
}
