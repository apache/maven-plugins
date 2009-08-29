/**
 * 
 */
package org.apache.maven.plugins.site;

import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.reporting.MavenReport;

/**
 *  
 * @author Olivier Lamy
 * @since 3.0-beta-1
 */
public interface MavenReportExecutor
{
    Map<MavenReport, ClassLoader> buildMavenReports(MavenReportExecutorRequest mavenReportExecutorRequest)
        throws MojoExecutionException;
}
