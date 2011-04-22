package org.apache.maven.plugin.changes;

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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFileFilterRequest;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;

/**
 * Goal which creates a nicely formatted Changes Report in html format from a changes.xml file.
 *
 * @goal changes-report
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 * @threadSafe
 */
public class ChangesMojo
    extends AbstractChangesReport
{
    /**
     * A flag whether the report should also include changes from child modules. If set to <code>false</code>, only
     * the changes from current project will be written to the report.
     *
     * @parameter default-value="false"
     * @since 2.5
     */
    private boolean aggregated;

    /**
     * A flag whether the report should also include the dates of individual actions. If set to <code>false</code>, only
     * the dates of releases will be written to the report.
     *
     * @parameter expression="${changes.addActionDate}" default-value="false"
     * @since 2.1
     */
    private boolean addActionDate;

    /**
     * Whether HTML code within an action should be escaped. By changing this to
     * <code>false</code> you can restore the behavior that was in version 2.2
     * of this plugin, allowing you to use HTML code to format the content of an
     * action.
     * <p>
     * <strong>Note:</strong> If you use HTML code in an action you need to
     * place it inside a CDATA section.
     * </p>
     * <strong>Note:</strong> Putting any kind of markup inside a CDATA section
     * might mess up the Changes Report or other generated documents, such as
     * PDFs, that are based on your <code>changes.xml</code> file if you are not
     * careful.
     *
     * @parameter default-value="true"
     * @since 2.4
     * @deprecated using markup inside CDATA sections does not work for all output formats!
     */
    private boolean escapeHTML;

    /**
     * The directory for interpolated changes.xml.
     *
     * @parameter expression="${project.build.directory}/changes"
     * @required
     * @readonly
     * @since 2.2
     */
    private File filteredOutputDirectory;

    /**
     * applying filtering filtering "a la" resources plugin
     *
     * @parameter default-value="false"
     * @since 2.2
     */
    private boolean filteringChanges;

    /**
     * Template string that is used to discover the URL to use to display an issue report.
     * There are 2 template tokens you can use. <code>%URL%</code>: this is computed by getting the
     * <code>&lt;issueManagement&gt;/&lt;url&gt;</code> value from the POM, and removing the last '/'
     * and everything that comes after it. <code>%ISSUE%</code>: this is the issue number.
     * <p>
     * <strong>Note:</strong> In versions of this plugin prior to 2.0-beta-2 this parameter was called
     * <code>link_template</code>.
     * </p>
     *
     * @parameter expression="${changes.issueLinkTemplate}" default-value="%URL%/ViewIssue.jspa?key=%ISSUE%"
     * @since 2.0-beta-2
     * @deprecated As of 2.1 use issueLinkTemplatePerSystem : this one will be with system default
     */
    private String issueLinkTemplate;

    /**
     * Template strings per system that is used to discover the URL to use to display an issue report. Each key in this
     * map denotes the (case-insensitive) identifier of the issue tracking system and its value gives the URL template.
     * <p>
     * There are 2 template tokens you can use. <code>%URL%</code>: this is computed by getting the
     * <code>&lt;issueManagement&gt;/&lt;url&gt;</code> value from the POM, and removing the last '/'
     * and everything that comes after it. <code>%ISSUE%</code>: this is the issue number.
     * </p>
     * <p>
     * <strong>Note:</strong> The deprecated issueLinkTemplate will be used for a system called "default".
     * </p>
     * <p>
     * <strong>Note:</strong> Starting with version 2.4 you usually don't need
     * to specify this, unless you need to link to an issue management system in
     * your Changes report that isn't supported out of the box. See the
     * <a href="./usage.html">Usage page</a> for more
     * information.
     * </p>
     *
     * @parameter
     * @since 2.1
     */
    private Map issueLinkTemplatePerSystem;

    /**
     * @component
     * @since 2.2
     */
    private MavenFileFilter mavenFileFilter;

    /**
     * Format to use for publishDate. The value will be available with the following expression ${publishDate}
     *
     * @see SimpleDateFormat
     * @parameter default-value="yyyy-MM-dd"
     * @since 2.2
     */
    private String publishDateFormat;

   /**
    * Locale to use for publishDate when formatting
    *
    * @see Locale
    * @parameter default-value="en"
    * @since 2.2
    */
    private String publishDateLocale;

    /**
     * @parameter expression="${session}"
     * @readonly
     * @required
     * @since 2.2
     */
    protected MavenSession session;

    /**
     * @parameter default-value="${project.issueManagement.system}"
     * @readonly
     * @since 2.4
     */
    private String system;

    /**
     * The URI of a file containing all the team members. If this is set to the
     * special value "none", no links will be generated for the team members.
     *
     * @parameter default-value="team-list.html"
     * @since 2.4
     */
    private String teamlist;

    /**
     * @parameter default-value="${project.issueManagement.url}"
     * @readonly
     */
    private String url;

    /**
     * The path of the <code>changes.xml</code> file that will be converted into an HTML report.
     *
     * @parameter expression="${changes.xmlPath}" default-value="src/changes/changes.xml"
     */
    private File xmlPath;

    private ReleaseUtils releaseUtils = new ReleaseUtils( getLog() );

    private CaseInsensitiveMap caseInsensitiveIssueLinkTemplatePerSystem;

    /* --------------------------------------------------------------------- */
    /* Public methods                                                        */
    /* --------------------------------------------------------------------- */

    public boolean canGenerateReport()
    {
        return xmlPath.isFile();
    }

    public void executeReport( Locale locale )
        throws MavenReportException
    {
        Date now = new Date();
        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat(publishDateFormat, new Locale(publishDateLocale));
        Properties additionalProperties = new Properties();
        additionalProperties.put("publishDate", simpleDateFormat.format(now));

        ChangesXML changesXml = getChangesFromFile( xmlPath, project, additionalProperties);
        if ( changesXml == null ) return;

        if ( aggregated )
        {
            final String basePath = project.getBasedir().getAbsolutePath();
            final String absolutePath = xmlPath.getAbsolutePath();
            if ( !absolutePath.startsWith( basePath ) )
            {
                getLog().warn( "xmlPath should be within the project dir for aggregated changes report." );
                return;
            }
            final String relativePath = absolutePath.substring( basePath.length() );

            List releaseList = changesXml.getReleaseList();
            for ( Iterator iterator = project.getCollectedProjects().iterator(); iterator.hasNext(); )
            {
                final MavenProject childProject = (MavenProject) iterator.next();
                final File changesFile = new File( childProject.getBasedir(), relativePath );
                final ChangesXML childXml = getChangesFromFile( changesFile, childProject, additionalProperties );
                if ( childXml != null )
                {
                    releaseList = releaseUtils.mergeReleases( releaseList, childProject.getName(), childXml.getReleaseList() );
                }
            }
            changesXml.setReleaseList( releaseList );
        }

        ChangesReportGenerator report = new ChangesReportGenerator( changesXml.getReleaseList() );

        report.setAuthor( changesXml.getAuthor() );
        report.setTitle( changesXml.getTitle() );

        report.setEscapeHTML ( escapeHTML );

        // Create a case insensitive version of issueLinkTemplatePerSystem
        // We need something case insensitive to maintain backward compatibility
        if ( issueLinkTemplatePerSystem == null )
        {
            caseInsensitiveIssueLinkTemplatePerSystem = new CaseInsensitiveMap();
        }
        else
        {
            caseInsensitiveIssueLinkTemplatePerSystem = new CaseInsensitiveMap( issueLinkTemplatePerSystem );
        }

        // Set good default values for issue management systems here, but only
        // if they have not been configured already by the user
        addIssueLinkTemplate( ChangesReportGenerator.DEFAULT_ISSUE_SYSTEM_KEY, issueLinkTemplate );
        addIssueLinkTemplate( "Bitbucket", "%URL%/issue/%ISSUE%" );
        addIssueLinkTemplate( "Bugzilla", "%URL%/show_bug.cgi?id=%ISSUE%" );
        addIssueLinkTemplate( "GitHub", "%URL%/%ISSUE%" );
        addIssueLinkTemplate( "GoogleCode", "%URL%/detail?id=%ISSUE%" );
        addIssueLinkTemplate( "JIRA", "%URL%/%ISSUE%" );
        addIssueLinkTemplate( "Mantis", "%URL%/view.php?id=%ISSUE%" );
        addIssueLinkTemplate( "Redmine", "%URL%/issues/show/%ISSUE%" );
        addIssueLinkTemplate( "Scarab", "%URL%/issues/id/%ISSUE%" );
        addIssueLinkTemplate( "SourceForge", "http://sourceforge.net/support/tracker.php?aid=%ISSUE%" );
        addIssueLinkTemplate( "Trac", "%URL%/ticket/%ISSUE%" );
        addIssueLinkTemplate( "YouTrack", "%URL%/issue/%ISSUE%" );
        // @todo Add more issue management systems here

        // Show the current issueLinkTemplatePerSystem configuration
        logIssueLinkTemplatePerSystem( caseInsensitiveIssueLinkTemplatePerSystem );

        report.setIssueLinksPerSystem( caseInsensitiveIssueLinkTemplatePerSystem );

        report.setSystem( system );

        report.setTeamlist ( teamlist );

        report.setUrl( url );

        report.setAddActionDate( addActionDate );

        if ( StringUtils.isEmpty( url ) )
        {
            getLog().warn( "No issue management URL defined in POM. Links to your issues will not work correctly." );
        }

        report.doGenerateReport( getBundle( locale ), getSink() );

        // Copy the images
        copyStaticResources();
    }

    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.issues.description" );
    }

    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.issues.name" );
    }

    public String getOutputName()
    {
        return "changes-report";
    }

    /* --------------------------------------------------------------------- */
    /* Private methods                                                       */
    /* --------------------------------------------------------------------- */

    /**
     * Parses specified changes.xml file. It also makes filtering if needed. If specified file doesn't exist
     * it will log warning and return <code>null</code>.
     *
     * @param changesXml changes xml file to parse
     * @param project maven project to parse changes for
     * @param additionalProperties additional properties used for filtering
     * @return parsed <code>ChangesXML</code> instance or null if file doesn't exist
     * @throws MavenReportException if any errors occurs while parsing
     */
    private ChangesXML getChangesFromFile( File changesXml, MavenProject project, Properties additionalProperties )
        throws MavenReportException
    {
        if ( !changesXml.exists() )
        {
            getLog().warn( "changes.xml file " + changesXml.getAbsolutePath() + " does not exist." );
            return null;
        }

        if ( filteringChanges )
        {
            if ( !filteredOutputDirectory.exists() )
            {
                filteredOutputDirectory.mkdirs();
            }
            XmlStreamReader xmlStreamReader = null;
            try
            {
                // so we get encoding from the file itself
                xmlStreamReader = ReaderFactory.newXmlReader( changesXml );
                String encoding = xmlStreamReader.getEncoding();
                File resultFile = new File( filteredOutputDirectory, project.getGroupId() + "." + project.getArtifactId() + "-changes.xml" );

                final MavenFileFilterRequest mavenFileFilterRequest =
                        new MavenFileFilterRequest( changesXml, resultFile, true, project, Collections.EMPTY_LIST, false,
                                encoding, session, additionalProperties );
                mavenFileFilter.copyFile( mavenFileFilterRequest );
                changesXml = resultFile;
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Exception during filtering changes file : " + e.getMessage(), e );
            }
            catch ( MavenFilteringException e )
            {
                throw new MavenReportException( "Exception during filtering changes file : " + e.getMessage(), e );
            }
            finally
            {
                if ( xmlStreamReader != null )
                {
                    IOUtil.close( xmlStreamReader );
                }
            }

        }
        return new ChangesXML( changesXml, getLog() );
    }

    /**
     * Add the issue link template for the given issue management system,
     * but only if it has not already been configured.
     *
     * @param system The issue management system
     * @param issueLinkTemplate The issue link template to use
     * @since 2.4
     */
    private void addIssueLinkTemplate( String system, String issueLinkTemplate )
    {
        if ( caseInsensitiveIssueLinkTemplatePerSystem == null )
        {
            caseInsensitiveIssueLinkTemplatePerSystem = new CaseInsensitiveMap();
        }
        if ( !caseInsensitiveIssueLinkTemplatePerSystem.containsKey( system ) )
        {
            caseInsensitiveIssueLinkTemplatePerSystem.put( system, issueLinkTemplate );
        }
    }

    private void copyStaticResources()
        throws MavenReportException
    {
        final String pluginResourcesBase = "org/apache/maven/plugin/changes";
        String resourceNames[] = {
            "images/add.gif",
            "images/fix.gif",
            "images/icon_help_sml.gif",
            "images/remove.gif",
            "images/rss.png",
            "images/update.gif" };
        try
        {
            getLog().debug( "Copying static resources." );
            for ( int i = 0; i < resourceNames.length; i++ )
            {
                URL url = this.getClass().getClassLoader().getResource( pluginResourcesBase + "/" + resourceNames[i] );
                FileUtils.copyURLToFile( url, new File( getReportOutputDirectory(), resourceNames[i] ) );
            }
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to copy static resources." );
        }
    }

    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "changes-report", locale, this.getClass().getClassLoader() );
    }

    protected String getTeamlist()
    {
        return teamlist;
    }

    private void logIssueLinkTemplatePerSystem( Map issueLinkTemplatePerSystem )
    {
        if ( getLog().isDebugEnabled() )
        {
            if ( issueLinkTemplatePerSystem == null )
            {
                getLog().debug( "No issueLinkTemplatePerSystem configuration was found" );
            }
            else
            {
                Iterator iterator = issueLinkTemplatePerSystem.entrySet().iterator();
                while ( iterator.hasNext() )
                {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    getLog().debug( "issueLinkTemplatePerSystem[" + entry.getKey() + "] = " + entry.getValue() );
                }
            }
        }
    }
}
