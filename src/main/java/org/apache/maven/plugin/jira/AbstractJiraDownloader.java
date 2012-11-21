package org.apache.maven.plugin.jira;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.issues.Issue;
import org.apache.maven.plugin.issues.IssueUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.proxy.ProxyInfo;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Abstract API, more or less, to retrieving issue information from JIRA.
 * Intended to have subclasses for the old (RSS) and new (REST) ways of doing things.
 *
 * @author mfranken@xebia.com
 * @author jruiz@exist.com
 * @version $Id$
 */
public abstract class AbstractJiraDownloader
{
    protected static final String UTF_8 = "UTF-8";

    /** Log for debug output. */
    protected Log log;
    /** Output file for xml document. */
    protected File output;
    /** The maximum number of entries to show. */
    protected int nbEntriesMax;
    /** The filter to apply to query to JIRA. */
    protected String filter;
    /** Ids of fix versions to show, as comma separated string. */
    protected String fixVersionIds;
    /** Ids of status to show, as comma separated string. */
    protected String statusIds;
    /** Ids of resolution to show, as comma separated string. */
    protected String resolutionIds;
    /** Ids of priority to show, as comma separated string. */
    protected String priorityIds;
    /** The component to show. */
    protected String component;
    /** Ids of types to show, as comma separated string. */
    protected String typeIds;
    /** Column names to sort by, as comma separated string. */
    protected String sortColumnNames;
    /** The username to log into JIRA. */
    protected String jiraUser;
    /** The password to log into JIRA. */
    protected String jiraPassword;
    /** The username to log into webserver. */
    protected String webUser;
    /** The password to log into webserver. */
    protected String webPassword;
    /** The maven project. */
    protected MavenProject project;
    /** The maven settings. */
    protected Settings settings;
    /** Use JQL, JIRA query language, instead of URL parameter based queries.
     * Note that this is down here to make it easier for the mojo to deal with
     * both new and old flavors. */
    protected boolean useJql;
    /** Filter the JIRA query based on the current version */
    protected boolean onlyCurrentVersion;
    /** The versionPrefix to apply to the POM version */
    protected String versionPrefix;
    /** The pattern used to parse dates from the JIRA xml file. */
    protected String jiraDatePattern;
    protected String proxyHost;
    protected int proxyPort;
    protected String proxyUser;
    protected String proxyPass;

    /**
     * Execute the query on the JIRA server.
     *
     * @throws Exception on error
     */
    public abstract void doExecute() throws Exception;


    /**
     * Check to see if we think that JIRA authentication is needed.
     *
     * @return <code>true</code> if jiraUser and jiraPassword are set, otherwise <code>false</code>
     */
    protected boolean isJiraAuthenticationConfigured()
    {
        return ( jiraUser != null ) && ( jiraUser.length() > 0 ) && ( jiraPassword != null );
    }


    protected void getProxyInfo( String jiraUrl )
    {
        // see whether there is any proxy defined in maven
        Proxy proxy = null;

        if ( project == null )
        {
            getLog().error( "No project set. No proxy info available." );

            return;
        }

        if ( settings != null )
        {
            proxy = settings.getActiveProxy();
        }

        if ( proxy != null )
        {

            ProxyInfo proxyInfo = new ProxyInfo();
            proxyInfo.setNonProxyHosts( proxy.getNonProxyHosts() );

            // Get the host out of the JIRA URL
            URL url = null;
            try
            {
                url = new URL( jiraUrl );
            }
            catch( MalformedURLException e )
            {
                getLog().error( "Invalid JIRA URL: " + jiraUrl + ". " + e.getMessage() );
            }
            String jiraHost = null;
            if ( url != null )
            {
                jiraHost = url.getHost();
            }

            // Validation of proxy method copied from org.apache.maven.wagon.proxy.ProxyUtils.
            // @todo Can use original when maven-changes-plugin requires a more recent version of Maven

            //if ( ProxyUtils.validateNonProxyHosts( proxyInfo, jiraHost ) )
            if ( JiraHelper.validateNonProxyHosts( proxyInfo, jiraHost ) )
            {
                return;
            }

            proxyHost = settings.getActiveProxy().getHost();
            proxyPort = settings.getActiveProxy().getPort();
            proxyUser = settings.getActiveProxy().getUsername();
            proxyPass = settings.getActiveProxy().getPassword();
        }
    }

    /**
     * Override this method if you need to get issues for a specific Fix For.
     *
     * @return A Fix For id or <code>null</code> if you don't have that need
     */
    protected String getFixFor()
    {
        if ( onlyCurrentVersion && useJql )
        {
            // Let JIRA do the filtering of the current version instead of the JIRA mojo.
            // This way JIRA returns less issues and we do not run into the "nbEntriesMax" limit that easily.

            String version = ( versionPrefix == null ? "" : versionPrefix ) + project.getVersion();

            // Remove "-SNAPSHOT" from the end of the version, if it's there
            if ( version.endsWith( IssueUtils.SNAPSHOT_SUFFIX ) )
            {
                return version.substring( 0, version.length() - IssueUtils.SNAPSHOT_SUFFIX.length() );
            }
            else
            {
                return version;
            }
        }
        else
        {
            return null;
        }
    }


    public abstract List<Issue> getIssueList() throws MojoExecutionException;

    public void setJiraDatePattern( String jiraDatePattern )
    {
        this.jiraDatePattern = jiraDatePattern;
    }

    /**
     * Set the output file for the log.
     *
     * @param thisOutput the output file
     */
    public void setOutput( File thisOutput )
    {
        this.output = thisOutput;
    }

    public File getOutput()
    {
        return this.output;
    }

    /**
     * Sets the project.
     *
     * @param thisProject  The project to set
     */
    public void setMavenProject( Object thisProject )
    {
        this.project = (MavenProject) thisProject;
    }

    /**
     * Sets the maximum number of Issues to show.
     *
     * @param nbEntries  The maximum number of Issues
     */
    public void setNbEntries( final int nbEntries )
    {
        nbEntriesMax = nbEntries;
    }

    /**
     * Sets the statusIds.
     *
     * @param thisStatusIds   The id(s) of the status to show, as comma separated string
     */
    public void setStatusIds( String thisStatusIds )
    {
        statusIds = thisStatusIds;
    }

    /**
     * Sets the priorityIds.
     *
     * @param thisPriorityIds  The id(s) of the priority to show, as comma separated string
     */
    public void setPriorityIds( String thisPriorityIds )
    {
        priorityIds = thisPriorityIds;
    }

    /**
     * Sets the resolutionIds.
     *
     * @param thisResolutionIds  The id(s) of the resolution to show, as comma separated string
     */
    public void setResolutionIds( String thisResolutionIds )
    {
        resolutionIds = thisResolutionIds;
    }

    /**
     * Sets the sort column names.
     *
     * @param thisSortColumnNames The column names to sort by
     */
    public void setSortColumnNames( String thisSortColumnNames )
    {
        sortColumnNames = thisSortColumnNames;
    }

    /**
     * Sets the password for authentication against the webserver.
     *
     * @param thisWebPassword  The password of the webserver
     */
    public void setWebPassword( String thisWebPassword )
    {
        this.webPassword = thisWebPassword;
    }

    /**
     * Sets the username for authentication against the webserver.
     *
     * @param thisWebUser   The username of the webserver
     */
    public void setWebUser( String thisWebUser )
    {
        this.webUser = thisWebUser;
    }

    /**
     * Sets the password to log into a secured JIRA.
     *
     * @param thisJiraPassword  The password for JIRA
     */
    public void setJiraPassword( final String thisJiraPassword )
    {
        this.jiraPassword = thisJiraPassword;
    }

    /**
     * Sets the username to log into a secured JIRA.
     *
     * @param thisJiraUser  The username for JIRA
     */
    public void setJiraUser( String thisJiraUser )
    {
        this.jiraUser = thisJiraUser;
    }

    /**
     * Sets the filter to apply to query to JIRA.
     *
     * @param thisFilter  The filter to query JIRA
     */
    public void setFilter( String thisFilter )
    {
        this.filter = thisFilter;
    }

    /**
     * Sets the component(s) to apply to query JIRA.
     *
     * @param theseComponents   The id(s) of components to show, as comma separated string
     */
    public void setComponent( String theseComponents )
    {
        this.component = theseComponents;
    }

    /**
     * Sets the fix version id(s) to apply to query JIRA.
     *
     * @param theseFixVersionIds The id(s) of fix versions to show, as comma separated string
     */
    public void setFixVersionIds( String theseFixVersionIds )
    {
        this.fixVersionIds = theseFixVersionIds;
    }

    /**
     * Sets the typeIds.
     *
     * @param theseTypeIds  The id(s) of the types to show, as comma separated string
     */
    public void setTypeIds( String theseTypeIds )
    {
        typeIds = theseTypeIds;
    }

    public void setLog( Log log )
    {
        this.log = log;
    }

    protected Log getLog()
    {
        return log;
    }

    public void setSettings( Settings settings )
    {
        this.settings = settings;
    }

    public boolean isUseJql()
    {
        return useJql;
    }

    public void setUseJql( boolean useJql )
    {
        this.useJql = useJql;
    }

    public boolean isOnlyCurrentVersion()
    {
        return onlyCurrentVersion;
    }

    public void setOnlyCurrentVersion( boolean onlyCurrentVersion )
    {
        this.onlyCurrentVersion = onlyCurrentVersion;
    }

    public String getVersionPrefix()
    {
        return versionPrefix;
    }

    public void setVersionPrefix( String versionPrefix )
    {
        this.versionPrefix = versionPrefix;
    }
}
