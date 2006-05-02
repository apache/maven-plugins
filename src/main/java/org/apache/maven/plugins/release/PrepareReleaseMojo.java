package org.apache.maven.plugins.release;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.release.config.ReleaseConfiguration;

import java.util.List;

/**
 * Prepare for a release in SCM.
 *
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @aggregator
 * @goal prepare
 * @requiresDependencyResolution test
 * @todo [!] check how this works with version ranges
 */
public class PrepareReleaseMojo
    extends AbstractReleaseMojo
{
    /**
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    /**
     * Resume a previous release attempt from the point that it was stopped.
     *
     * @parameter expression="${resume}" default-value="true"
     */
    private boolean resume;

    /**
     * Whether to generate <code>release-pom.xml</code> files that contain resolved information about the project.
     *
     * @parameter default-value="false" expression="${generateReleasePoms}"
     */
    private boolean generateReleasePoms;

    /**
     * Whether to use "edit" mode on the SCM, to lock the file for editing during SCM operations.
     *
     * @parameter expression="${useEditMode}" default-value="false"
     */
    private boolean useEditMode;

    /**
     * Dry run: don't checkin or tag anything in the scm repository, or modify the checkout.
     * Running <code>mvn -Dtestmode=true release:prepare</code> could be useful in order to check that modifications to
     * poms and scm operations (only listed in console) are working as expected.
     * Modified POMs are written alongside the originals without modifying them.
     *
     * @parameter expression="${dryRun}" default-value="false"
     */
    private boolean dryRun;

    /**
     * @component
     */
    private ReleaseManager releaseManager;

    /**
     * Whether to add a schema to the POM if it was previously missing on release.
     *
     * @parameter expression="${addSchema}" default-value="true"
     */
    private boolean addSchema;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( dryRun )
        {
            getLog()
                .info( "\n*****\n" + "Warning, release:perform is run in TEST MODE.\n" +
                    "Nothing will be committed or tagged in the repository, but you pom files will be updated!\n" +
                    "*****" );
        }

        ReleaseConfiguration config = createReleaseConfiguration();
        config.setAddSchema( addSchema );
        config.setGenerateReleasePoms( generateReleasePoms );
        config.setReactorProjects( reactorProjects );
        config.setUseEditMode( useEditMode );
        // TODO [!]: prep goals not configurable
        // TODO [!]: resume not configured
        // TODO: move to abstract?

        try
        {
            // TODO [!]: differentiate failures from exceptions
            releaseManager.prepare( config );
        }
        catch ( ReleaseExecutionException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

}
