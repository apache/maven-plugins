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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.release.config.ReleaseConfiguration;
import org.apache.maven.profiles.Profile;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Base class with shared configuration.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractReleaseMojo
    extends AbstractMojo
{
    /**
     * The SCM username to use.
     *
     * @parameter expression="${username}"
     */
    private String username;

    /**
     * The SCM password to use.
     *
     * @parameter expression="${password}"
     */
    private String password;

    /**
     * The SCM tag to use.
     *
     * @parameter expression="${tag}" alias="releaseLabel"
     */
    private String tag;

    /**
     * The tag base directory in SVN, you must define it if you don't use the standard svn layout (trunk/tags/branches).
     * For example, <code>http://svn.apache.org/repos/asf/maven/plugins/tags</code>. The URL is an SVN URL and does not
     * include the SCM provider and protocol.
     *
     * @parameter expression="${tagBase}"
     */
    private String tagBase;

    /**
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    private File basedir;

    /**
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @component
     */
    protected ReleaseManager releaseManager;

    /**
     * Additional arguments to pass to the Maven executions, separated by spaces.
     *
     * @parameter expression="${arguments}"
     */
    private String arguments;

    /**
     * The file name of the POM to execute any goals against.
     *
     * @parameter expression="${pomFileName}"
     */
    private String pomFileName;

    protected ReleaseConfiguration createReleaseConfiguration()
    {
        ReleaseConfiguration config = new ReleaseConfiguration();
        config.setInteractive( settings.isInteractiveMode() );
        config.setPassword( password );
        config.setReleaseLabel( tag );
        config.setSettings( settings );
        config.setTagBase( tagBase );
        config.setUsername( username );
        config.setWorkingDirectory( basedir );
        config.setPomFileName( pomFileName );

        List profiles = project.getActiveProfiles();

        String arguments = this.arguments;
        if ( profiles != null && !profiles.isEmpty() )
        {
            arguments += "-P ";

            for ( Iterator it = profiles.iterator(); it.hasNext(); )
            {
                Profile profile = (Profile) it.next();

                arguments += profile.getId();
                if ( it.hasNext() )
                {
                    arguments += ",";
                }
            }
        }
        config.setAdditionalArguments( arguments );

        return config;
    }
}
