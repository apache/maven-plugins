package org.apache.maven.plugin.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.scanner.MojoScanner;

import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public abstract class AbstractGeneratorMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project}"
     * @required
     */
    protected MavenProject project;

    /**
     * @parameter expression="${component.org.apache.maven.tools.plugin.scanner.MojoScanner}"
     * @required
     */
    protected MojoScanner mojoScanner;
    
    protected abstract String getOutputDirectory();

    protected abstract void generate( String outputDirectory, Set mavenMojoDescriptors, MavenProject project )
        throws Exception;

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            Set mavenMojoDescriptors = mojoScanner.execute( project );

            generate( getOutputDirectory(), mavenMojoDescriptors, project );
        }
        catch ( Exception e )
        {
            // TODO: improve error handling
            throw new MojoExecutionException( "Error generating plugin descriptor", e );
        }
    }
}
