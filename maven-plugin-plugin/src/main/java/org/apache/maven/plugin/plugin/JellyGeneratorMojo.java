package org.apache.maven.plugin.plugin;

import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.generator.jelly.JellyHarnessGenerator;

import java.util.Set;


/**
 * @goal jelly
 *
 * @description Goal for generating a plugin descriptor.
 *
 * @parameter
 *  name="mojoScanner"
 *  type="org.apache.maven.tools.plugin.scanner.MojoScanner"
 *  required="true"
 *  validator=""
 *  expression="#component.org.apache.maven.tools.plugin.scanner.MojoScanner"
 *  description="Scanner used to discover mojo descriptors from this project"
 * @parameter
 *  name="project"
 *  type="org.apache.maven.project.MavenProject"
 *  required="true"
 *  validator=""
 *  expression="#project"
 *  description=""
 * @parameter
 *  name="outputDirectory"
 *  type="String"
 *  required="true" 
 *  validator=""
 *  expression="#project.build.directory/generated-sources"
 *  description=""
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class JellyGeneratorMojo
    extends AbstractGeneratorMojo
{
    protected void generate( String outputDirectory, Set mavenMojoDescriptors, MavenProject project )
        throws Exception
    {
        JellyHarnessGenerator generator = new JellyHarnessGenerator();

        generator.execute( outputDirectory, mavenMojoDescriptors, project );
    }
}
