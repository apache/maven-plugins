package org.apache.maven.plugin.changes;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Goal which creates a nicely formatted Changes Report in html format from a changes.xml file.
 *
 * @goal changes-report
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 */
public class ChangesMojo
    extends AbstractMavenReport
{
    /**
     * Directory where reports will go.
     *
     * @parameter expression="${project.build.directory}/site "
     * @required
     * @readonly
     */
    private String outputDirectory;

    /**
     * @parameter expression="${component.org.apache.maven.doxia.siterenderer.Renderer}"
     * @required
     * @readonly
     */
    private Renderer siteRenderer;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The path of the changes.xml file that will be converted into an html report.
     *
     * @parameter expression="${basedir}/src/changes/changes.xml"
     * @required
     */
    private String xmlPath;

    /**
     * Template string that is used to discover the URL to use to display an issue report.
     * There are 2 template tokens you can use. %URL%: this is computed by getting the
     * &lt;issueManagement&gt;&lt;url&gt; value from the POM, and removing the context path. %ISSUE% :
     * this is the issue number.
     * <p>
     * <strong>Note:</strong> In versions of this plugin prior to 2.0-beta-2 this parameter was called
     * <code>link_template</code>.
     * </p>
     * 
     * @parameter expression="%URL%/ViewIssue.jspa?key=%ISSUE%"
     *
     */
    private String issueLinkTemplate;

    /**
     * @parameter expression="${project.issueManagement.url}"
     * @readonly
     */
    private String url;

    public boolean canGenerateReport()
    {
        File xmlFile = new File( xmlPath );
        return xmlFile.exists();
    }

    private void copyStaticResources()
        throws MavenReportException
    {
        final String pluginResourcesBase = "org/apache/maven/plugin/changes";
        String resourceNames[] = {
            "images/add.gif",
            "images/fix.gif",
            "images/remove.gif",
            "images/rss.png",
            "images/update.gif" };
        try
        {
            getLog().debug( "Copying static resources." );
            for ( int i = 0; i < resourceNames.length; i++ )
            {
                URL url = this.getClass().getClassLoader().getResource( pluginResourcesBase + "/" + resourceNames[i] );
                FileUtils.copyURLToFile( url, new File( outputDirectory, resourceNames[i] ) );
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
        ChangesReportGenerator report = new ChangesReportGenerator( xmlPath, getLog() );

        if ( ( url == null ) || ( url.trim().equals( "" ) ) )
        {
            getLog().warn( getBundle( locale ).getString( "report.changes.warn.url" ) );
        }

        report.setIssueLink( issueLinkTemplate );
        report.setUrl( url );
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

    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    protected MavenProject getProject()
    {
        return project;
    }

    public String getOutputName()
    {
        return "changes-report";
    }

    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "changes-report", locale, this.getClass().getClassLoader() );
    }
}
