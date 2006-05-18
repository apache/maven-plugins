package org.apache.maven.changes;

/*
 * Copyright 2001-2005 The Codehaus.
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
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.site.renderer.SiteRenderer;

/**
 * @goal changes-report
 * @description Goal wich creates a nicely formatted Changes Report in html format from changes.xml.
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 */
public class ChangesMojo
    extends AbstractMavenReport
{
    /**
     * Directory where reports will go.
     * @parameter expression="${project.build.directory}/site "
     * @required
     * @readonly
     */
    private String outputDirectory;

    /**
     * @parameter expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required
     * @readonly
     */
    private SiteRenderer siteRenderer;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The changes.xml that will be converted into an html report.
     * @parameter expression="${basedir}/src/changes/changes.xml"
     * @required
     */
    private String xmlPath;

    /**
     * Template string that is used to discover the URL to use to display a bug report.
     * There are 2 template tokens you can use. %URL%: this is computed by getting the
     * &lt;issueManagement&gt;&lt;url&gt; value from the POM, and removing the context path. %ISSUE% :
     * this is the issue number.
     * @parameter expression="%URL%/ViewIssue.jspa?key=%ISSUE%"
     *
     */
    private String link_template;

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

    public void executeReport( Locale locale )
        throws MavenReportException
    {
		ChangesReportGenerator report = new ChangesReportGenerator( xmlPath );

		if ( ( url == null ) || ( url.trim().equals( "" ) ) )
		{
			getLog().warn( getBundle( locale ).getString( "report.changes.warn.url" ) );
		}

		report.setIssueLink( link_template );
		report.setUrl( url );
		report.doGenerateReport( getBundle( locale ), getSink() );
    }

    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.changes.name" );
    }

    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.changes.description" );
    }

    protected SiteRenderer getSiteRenderer()
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
