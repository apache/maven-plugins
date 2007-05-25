package org.apache.maven.plugin.docck;

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

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.docck.reports.DocumentationReport;
import org.apache.maven.plugin.docck.reports.DocumentationReporter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Performs the heavy lifting for documentation checks. This is designed to be
 * reused for other types of projects, too.
 *
 * @author jdcasey
 */
public abstract class AbstractCheckDocumentationMojo
    extends AbstractMojo
{
    /**
     * @parameter default-value="${reactorProjects}"
     * @readonly
     * @required
     */
    private List reactorProjects;

    /**
     * An optional location where the results will be written to. If this is
     * not specified the results will be written to the console.
     *
     * @parameter expression="${output}"
     */
    private File output;

    /**
     * Directory where the site source for the project is located.
     *
     * @parameter expression="${siteDirectory}" default-value="src/site"
     * @todo should be determined programmatically
     */
    protected String siteDirectory;

    /**
     * Sets whether this plugin is running in offline or online mode. Also
     * useful when you don't want to verify http URLs.
     *
     * @parameter expression="${settings.offline}"
     */
    private boolean offline;

    /**
     * The current user system settings for use in Maven.
     *
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    private HttpClient httpClient;

    private FileSetManager fileSetManager = new FileSetManager();

    private List validUrls = new ArrayList();

    protected AbstractCheckDocumentationMojo()
    {
        httpClient = new HttpClient();

        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout( 5000 );
    }

    protected List getReactorProjects()
    {
        return reactorProjects;
    }

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        setupProxy();

        if ( output != null )
        {
            getLog().info( "Writing documentation survey results to: " + output );
        }

        Map reporters = new LinkedHashMap();
        boolean hasErrors = false;

        for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            if ( approveProjectPackaging( project.getPackaging() ) )
            {
                getLog().info( "Checking project: " + project.getName() );

                DocumentationReporter reporter = new DocumentationReporter();

                checkProject( project, reporter );

                if ( !hasErrors && reporter.hasErrors() )
                {
                    hasErrors = true;
                }

                reporters.put( project, reporter );
            }
            else
            {
                getLog().info( "Skipping unsupported project: " + project.getName() );
            }
        }

        String messages;

        messages = buildErrorMessages( reporters );

        if ( !hasErrors )
        {
            messages += "\nNo documentation errors were found.";
        }

        try
        {
            writeMessages( messages );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error writing results to output file: " + output );
        }

        if ( hasErrors )
        {
            String logLocation;
            if ( output == null )
            {
                logLocation = "Please see the console output above for more information.";
            }
            else
            {
                logLocation = "Please see \'" + output + "\' for more information.";
            }

            throw new MojoFailureException( "documentation check", "Documentation errors were found.", logLocation );
        }
    }

    /**
     * Setup proxy access if needed.
     */
    private void setupProxy()
    {
        Proxy settingsProxy = settings.getActiveProxy();

        if ( settingsProxy != null )
        {
            String proxyUsername = settingsProxy.getUsername();

            String proxyPassword = settingsProxy.getPassword();

            String proxyHost = settingsProxy.getHost();

            int proxyPort = settingsProxy.getPort();

            if ( StringUtils.isNotEmpty( proxyHost ) )
            {
                httpClient.getHostConfiguration().setProxy( proxyHost, proxyPort );

                getLog().info( "Using proxy[" + proxyHost + "] at port [" + proxyPort + "]." );

                if ( StringUtils.isNotEmpty( proxyUsername ) )
                {
                    getLog().info( "Using proxy user[" + proxyUsername + "]." );

                    Credentials creds = new UsernamePasswordCredentials( proxyUsername, proxyPassword );

                    httpClient.getState().setProxyCredentials( new AuthScope( proxyHost, proxyPort ), creds );
                    httpClient.getParams().setAuthenticationPreemptive( true );
                }
            }
        }
    }

    private String buildErrorMessages( Map reporters )
    {
        String messages = "";
        StringBuffer buffer = new StringBuffer();

        for ( Iterator it = reporters.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();

            MavenProject project = (MavenProject) entry.getKey();
            DocumentationReporter reporter = (DocumentationReporter) entry.getValue();

            if ( !reporter.getMessages().isEmpty() )
            {
                buffer.append( "\no " ).append( project.getName() );
                buffer.append( " (" ).append( reporter.getMessagesByType( DocumentationReport.TYPE_ERROR ).size() )
                    .append( " errors," );
                buffer.append( " " ).append( reporter.getMessagesByType( DocumentationReport.TYPE_WARN ).size() )
                    .append( " warnings)" );
                for ( Iterator errorIterator = reporter.getMessages().iterator(); errorIterator.hasNext(); )
                {
                    String error = (String) errorIterator.next();

                    buffer.append( "\n\t" ).append( error );
                }

                buffer.append( "\n" );
            }
        }

        if ( buffer.length() > 0 )
        {
            messages = "\nThe following documentation problems were found:\n" + buffer.toString();
        }

        return messages;
    }

    protected abstract boolean approveProjectPackaging( String packaging );

    private void writeMessages( String messages )
        throws IOException
    {
        if ( output != null )
        {
            FileWriter writer = null;

            try
            {
                writer = new FileWriter( output );
                writer.write( messages );
                writer.flush();
            }
            finally
            {
                IOUtil.close( writer );
            }
        }
        else
        {
            getLog().info( messages );
        }
    }

    private void checkProject( MavenProject project, DocumentationReporter reporter )
    {
        checkPomRequirements( project, reporter );

        checkPackagingSpecificDocumentation( project, reporter );
    }

    private void checkPomRequirements( MavenProject project, DocumentationReporter reporter )
    {
        checkProjectLicenses( project, reporter );

        if ( StringUtils.isEmpty( project.getName() ) )
        {
            reporter.error( "pom.xml is missing the <name> tag." );
        }

        if ( StringUtils.isEmpty( project.getDescription() ) )
        {
            reporter.error( "pom.xml is missing the <description> tag." );
        }

        if ( StringUtils.isEmpty( project.getUrl() ) )
        {
            reporter.error( "pom.xml is missing the <url> tag." );
        }
        else
        {
            checkURL( project.getUrl(), "project site", reporter );
        }

        if ( project.getIssueManagement() == null )
        {
            reporter.error( "pom.xml is missing the <issueManagement> tag." );
        }
        else
        {
            IssueManagement issueMngt = project.getIssueManagement();
            if ( StringUtils.isEmpty( issueMngt.getUrl() ) )
            {
                reporter.error( "pom.xml is missing the <url> tag in <issueManagement>." );
            }
            else
            {
                checkURL( issueMngt.getUrl(), "Issue Management", reporter );
            }
        }

        if ( project.getPrerequisites() == null )
        {
            reporter.error( "pom.xml is missing the <prerequisites> tag." );
        }
        else
        {
            Prerequisites prereq = project.getPrerequisites();
            if ( StringUtils.isEmpty( prereq.getMaven() ) )
            {
                reporter.error( "pom.xml is missing the <maven> tag in <prerequisites>." );
            }
        }

        if ( StringUtils.isEmpty( project.getInceptionYear() ) )
        {
            reporter.error( "pom.xml is missing the <inceptionYear> tag." );
        }

        if ( project.getMailingLists().size() == 0 )
        {
            reporter.warn( "pom.xml has no <mailingList> specified." );
        }

        if ( project.getScm() == null )
        {
            reporter.warn( "pom.xml is missing the <scm> tag." );
        }
        else
        {
            Scm scm = project.getScm();
            if ( StringUtils.isEmpty( scm.getConnection() ) && StringUtils.isEmpty( scm.getDeveloperConnection() )
                && StringUtils.isEmpty( scm.getUrl() ) )
            {
                reporter.warn( "pom.xml is missing the child tags under the <scm> tag." );
            }
            else if ( scm.getUrl() != null )
            {
                checkURL( scm.getUrl(), "scm", reporter );
            }
        }

        if ( project.getOrganization() == null )
        {
            reporter.error( "pom.xml is missing the <organization> tag." );
        }
        else
        {
            Organization org = project.getOrganization();
            if ( StringUtils.isEmpty( org.getName() ) )
            {
                reporter.error( "pom.xml is missing the <name> tag in <organization>." );
            }
            else if ( org.getUrl() != null )
            {
                checkURL( org.getUrl(), org.getName() + " site", reporter );
            }
        }
    }

    private void checkProjectLicenses( MavenProject project, DocumentationReporter reporter )
    {
        List licenses = project.getLicenses();

        if ( licenses == null || licenses.isEmpty() )
        {
            reporter.error( "pom.xml has no <license> specified." );
        }
        else
        {
            for ( Iterator it = licenses.iterator(); it.hasNext(); )
            {
                License license = (License) it.next();

                if ( StringUtils.isEmpty( license.getName() ) )
                {
                    reporter.error( "pom.xml is missing the <name> tag in <license>." );
                }
                else
                {
                    String url = license.getUrl();
                    if ( StringUtils.isEmpty( url ) )
                    {
                        reporter.error( "pom.xml is missing the <url> tag for license " + license.getName() + "." );
                    }
                    else
                    {
                        checkURL( url, "license " + license.getName(), reporter );
                    }
                }
            }
        }
    }

    private String getURLProtocol( String url )
        throws MalformedURLException
    {
        String protocol;

        URL licenseUrl = new URL( url );
        protocol = licenseUrl.getProtocol();

        if ( protocol != null )
        {
            protocol = protocol.toLowerCase();
        }

        return protocol;
    }

    private void checkURL( String url, String description, DocumentationReporter reporter )
    {
        try
        {
            String protocol = getURLProtocol( url );

            if ( protocol.startsWith( "http" ) )
            {
                if ( offline )
                {
                    reporter.warn( "Cannot verify " + description + " in offline mode with URL: \'" + url + "\'." );
                }
                else if ( !validUrls.contains( url ) )
                {
                    HeadMethod headMethod = new HeadMethod( url );
                    headMethod.setFollowRedirects( true );
                    headMethod.setDoAuthentication( false );

                    try
                    {
                        getLog().debug( "Verifying http url: " + url );
                        if ( httpClient.executeMethod( headMethod ) != 200 )
                        {
                            reporter.error( "Cannot reach " + description + " with URL: \'" + url + "\'." );
                        }
                        else
                        {
                            validUrls.add( url );
                        }
                    }
                    catch ( HttpException e )
                    {
                        reporter.error( "Cannot reach " + description + " with URL: \'" + url + "\'.\nError: "
                            + e.getMessage() );
                    }
                    catch ( IOException e )
                    {
                        reporter.error( "Cannot reach " + description + " with URL: \'" + url + "\'.\nError: "
                            + e.getMessage() );
                    }
                    finally
                    {
                        headMethod.releaseConnection();
                    }
                }
            }
            else
            {
                reporter.warn( "Non-HTTP " + description + " URL not verified." );
            }
        }
        catch ( MalformedURLException e )
        {
            reporter.warn( description + " appears to have an invalid URL: \'" + url + "\'.\nError: " + e.getMessage()
                + "\n\nTrying to access it as a file instead." );

            checkFile( url, description, reporter );
        }
    }

    private void checkFile( String url, String description, DocumentationReporter reporter )
    {
        File licenseFile = new File( url );
        if ( !licenseFile.exists() )
        {
            reporter.error( description + " file: \'" + licenseFile.getPath() + " does not exist." );
        }
    }

    protected abstract void checkPackagingSpecificDocumentation( MavenProject project, DocumentationReporter reporter );

    protected boolean findFiles( File siteDirectory, String pattern )
    {
        FileSet fs = new FileSet();
        fs.setDirectory( siteDirectory.getAbsolutePath() );
        fs.setFollowSymlinks( false );

        fs.addInclude( "apt/" + pattern + ".apt" );
        fs.addInclude( "xdoc/" + pattern + ".xml" );
        fs.addInclude( "fml/" + pattern + ".fml" );
        fs.addInclude( "resources/" + pattern + ".html" );

        String[] includedFiles = fileSetManager.getIncludedFiles( fs );

        return includedFiles != null && includedFiles.length > 0;
    }
}
