package org.apache.maven.plugins.release;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.StringUtils;

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
    protected File basedir;

    /**
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    protected Settings settings;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @component
     */
    protected ReleaseManager releaseManager;

    /**
     * Additional arguments to pass to the Maven executions, separated by spaces.
     *
     * @parameter expression="${arguments}" alias="prepareVerifyArgs"
     */
    private String arguments;

    /**
     * The file name of the POM to execute any goals against.
     *
     * @parameter expression="${pomFileName}"
     */
    private String pomFileName;

    /**
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    protected List reactorProjects;

    protected ReleaseDescriptor createReleaseDescriptor()
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();

        descriptor.setInteractive( settings.isInteractiveMode() );

        descriptor.setScmPassword( password );
        descriptor.setScmReleaseLabel( tag );
        descriptor.setScmTagBase( tagBase );
        descriptor.setScmUsername( username );

        descriptor.setWorkingDirectory( basedir.getAbsolutePath() );

        descriptor.setPomFileName( pomFileName );

        List profiles = project.getActiveProfiles();

        String arguments = this.arguments;
        if ( profiles != null && !profiles.isEmpty() )
        {
            if ( !StringUtils.isEmpty( arguments ) )
            {
                arguments += " -P ";
            }
            else
            {
                arguments = "-P ";
            }

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
        descriptor.setAdditionalArguments( arguments );

        return descriptor;
    }

    void setReleaseManager( ReleaseManager releaseManager )
    {
        this.releaseManager = releaseManager;
    }

    Settings getSettings()
    {
        return settings;
    }

    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    public List getReactorProjects()
    {
        return reactorProjects;
    }
}
