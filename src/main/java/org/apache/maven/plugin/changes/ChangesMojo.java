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
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFileFilterRequest;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;

/**
 * Goal which creates a nicely formatted Changes Report in html format from a changes.xml file.
 *
 * @goal changes-report
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 */
public class ChangesMojo
    extends AbstractChangesReport
{
    /**
     * The path of the <code>changes.xml</code> file that will be converted into an HTML report.
     *
     * @parameter expression="${changes.xmlPath}" default-value="src/changes/changes.xml"
     */
    private File xmlPath;

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
     * map denotes the (case-sensitive) identifier of the issue tracking system and its value gives the URL template.
     * <p>
     * There are 2 template tokens you can use. <code>%URL%</code>: this is computed by getting the
     * <code>&lt;issueManagement&gt;/&lt;url&gt;</code> value from the POM, and removing the last '/'
     * and everything that comes after it. <code>%ISSUE%</code>: this is the issue number.
     * </p>
     * <p>
     * <strong>Note:</strong> The deprecated issueLinkTemplate will be used for a system called "default".
     * </p>
     *
     * @parameter
     * @since 2.1
     */
    private Map issueLinkTemplatePerSystem;

    /**
     * @parameter default-value="${project.issueManagement.url}"
     * @readonly
     */
    private String url;

    /**
     * A flag whether the report should also include the dates of individual actions. If set to <code>false</code>, only
     * the dates of releases will be written to the report.
     *
     * @parameter expression="${changes.addActionDate}" default-value="false"
     * @since 2.1
     */
    private boolean addActionDate;
    
    /**
     *
     * @component
     *
     * @since 2.2
     */
    private MavenFileFilter mavenFileFilter;
    
    /**
     * @parameter expression="${session}"
     * @readonly
     * @required
     *
     * @since 2.2
     *
     */
    protected MavenSession session;
    
    /**
     * The URI of a file containing all the team members. If this is set to the
     * special value "none", no links will be generated for the team members.
     *
     * @parameter default-value="team-list.html"
     * @since 2.4
     */
    private String teamlist;

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
     */
    private boolean escapeHTML;

    /**
     * applying filtering filtering "a la" resources plugin
     *
     * @parameter default-value="false"
     *
     * @since 2.2
     */
    private boolean filteringChanges;
    
    /**
     * The directory for interpolated changes.xml.
     *
     * @parameter expression="${project.build.directory}/changes"
     * @required
     * @readonly
     *
     * @since 2.2
     *
     */
    private File filteredOutputDirectory;    
    
    /**
     *
     * Format to use for publishDate. The value will be available with the following expression ${publishDate}
     *
     * @see SimpleDateFormat
     *
     * @parameter default-value="yyyy-MM-dd"
     *
     * @since 2.2
     *
     */
    private String publishDateFormat;
    
    /**
    *
    * Locale to use for publishDate when formatting
    *
    * @see Locale
    *
    * @parameter default-value="en"
    *
    * @since 2.2
    *
    */
    private String publishDateLocale;
    
    
    public boolean canGenerateReport()
    {
        return xmlPath.isFile();
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

    public void executeReport( Locale locale )
        throws MavenReportException
    {
        
        if ( !xmlPath.exists() )
        {
            getLog().warn( "changes.xml file " + xmlPath.getAbsolutePath() + " does not exist." );
            return;
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
                xmlStreamReader = ReaderFactory.newXmlReader( xmlPath );
                String encoding = xmlStreamReader.getEncoding();
                File resultFile = new File( filteredOutputDirectory, "changes.xml" );
                Date now = new Date();
                SimpleDateFormat simpleDateFormat =
                    new SimpleDateFormat( publishDateFormat, new Locale( publishDateLocale ) );
                Properties additionnalProperties = new Properties();
                additionnalProperties.put( "publishDate", simpleDateFormat.format( now ) );
                MavenFileFilterRequest mavenFileFilterRequest =
                    new MavenFileFilterRequest( xmlPath, resultFile, true, project, Collections.EMPTY_LIST, false,
                                                encoding, session, additionnalProperties );
                mavenFileFilter.copyFile( mavenFileFilterRequest );
                xmlPath = resultFile;
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

        ChangesReportGenerator report = new ChangesReportGenerator( xmlPath, getLog() );
        
        report.setEscapeHTML ( escapeHTML );

        report.setIssueLinksPerSystem( issueLinkTemplatePerSystem );
        report.setIssueLink( issueLinkTemplate );
        
        report.setTeamlist ( teamlist );

        report.setUrl( url );

        report.setAddActionDate( addActionDate );
        
        if ( !report.canGenerateIssueLinks() )
        {
            getLog().warn( "No issue management URL defined in POM. Links to your issues will not work correctly." );
        }

        report.doGenerateReport( getBundle( locale ), getSink() );

        // Copy the images
        copyStaticResources();
    }

    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.changes.name" );
    }

    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.changes.description" );
    }

    public String getOutputName()
    {
        return "changes-report";
    }

    protected String getTeamlist()
    {
        return teamlist;
    }

    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "changes-report", locale, this.getClass().getClassLoader() );
    }
}
