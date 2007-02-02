package org.apache.maven.plugin.announcement;

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
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.changes.ChangesXML;
import org.apache.maven.plugin.changes.Release;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.codehaus.plexus.velocity.VelocityComponent;

/**
 * Goal which generate the template for an announcement.
 *
 * @goal announcement-generate
 * @requiresDependencyResolution test
 * @author aramirez@exist.com
 * @version $Id$
 */
public class AnnouncementMojo
    extends AbstractMojo
{
    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    /**
     * Directory where the template file will be generated.
     *
     * @parameter expression="${project.build.directory}/announcement"
     * @required
     */
    private String outputDirectory;

    /**
     * @parameter expression="${project.groupId}"
     * @readonly
     */
    private String groupId;

    /**
     * @parameter expression="${project.artifactId}"
     * @readonly
     */
    private String artifactId;

    /**
     * Version of the artifact.
     *
     * @parameter expression="${project.version}"
     * @readonly
     */
    private String version;

    /**
     * Distribution url of the artifact.
     *
     * @parameter expression="${project.url}"
     * @required
     */
    private String url;

    /**
     * Packaging structure for the artifact.
     *
     * @parameter expression="${project.packaging}"
     * @readonly
     */
    private String packaging;

    /**
     * The name of the artifact to be used in the announcement.
     *
     * @parameter expression="${project.build.finalName}.${project.packaging}"
     * @required
     */
    private String finalName;

    /**
     * URL where the artifact can be downloaded. If not specified,
     * no URL is used.
     * 
     * @parameter
     */
    private String urlDownload;

    /**
     * The path of the changes.xml file.
     *
     * @parameter expression="${basedir}/src/changes/changes.xml"
     * @required
     */
    private String xmlPath;

    /**
     * Name of the team that develops the artifact.
     *
     * @parameter default-value="${project.artifactId}-team"
     * @required
     */
    private String developmentTeam;

    /**
     * Short description or introduction of the released artifact.
     *
     * @parameter expression="${project.description}"
     */
    private String introduction;

    /**
     * Velocity Component.
     *
     * @parameter expression="${component.org.codehaus.plexus.velocity.VelocityComponent}"
     * @readonly
     */
    private VelocityComponent velocity;

    /**
     * The Velocity template used to format the announcement.
     *
     * @parameter default-value="announcement.vm"
     * @required
     */
    private String template;

    /**
     * Directory that contains the template.
     *
     * @parameter default-value="org/apache/maven/plugin/announcement"
     * @required
     */
    private String templateDirectory;

    private ChangesXML xml;

    //=======================================//
    //  JIRA-Announcement Needed Parameters  //
    //=======================================//

    /**
     * The Maven Project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Settings XML configuration.
     *
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings setting;

    /**
     * Flag to determine if the plugin will generate a JIRA announcement.
     *
     * @parameter expression="${generateJiraAnnouncement}" default-value="false"
     * @required
     */
    private boolean generateJiraAnnouncement;

    /**
     * Only closed issues are needed.
     *
     * @parameter default-value="Closed"
     */
    private String statusId;

    /**
     * Only fixed issues are needed.
     *
     * @parameter default-value="Fixed"
     */
    private String resolutionId;

    /**
     * The path of the XML file of JIRA-announcements to be parsed.
     *
     * @parameter expression="${project.build.directory}/jira-announcement.xml"
     * @required
     * @readonly
     */
    private String jiraXML;

    /**
     * The maximum number of issues to include.
     *
     * @parameter default-value="25"
     * @required
     */
    private int nbEntries;

    //=======================================//
    //    announcement-generate execution    //
    //=======================================//

    /**
     * Generate the template
     *
     * @throws MojoExecutionException
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( !generateJiraAnnouncement )
        {
            setXml( new ChangesXML( getXmlPath(), getLog() ) );

            doGenerate( getXml() );
        }
        else
        {
            doJiraGenerate();
        }
    }

    /**
     * Add the parameters to velocity context
     *
     * @param xml parsed changes.xml
     * @throws MojoExecutionException
     */
    public void doGenerate( ChangesXML xml )
        throws MojoExecutionException
    {
        try
        {
            Context context = new VelocityContext();

            List releaseList = xml.getReleaseList();

            getLog().info( "Creating announcement file from changes.xml..." );

            if ( getIntroduction() == null || getIntroduction().equals( "" ) )
            {
                setIntroduction( getUrl() );
            }

            context.put( "releases", releaseList );

            context.put( "groupId", getGroupId() );

            context.put( "artifactId", getArtifactId() );

            context.put( "version", getVersion() );

            context.put( "packaging", getPackaging() );

            context.put( "url", getUrl() );

            context.put( "release", getLatestRelease( releaseList ) );

            context.put( "introduction", getIntroduction() );

            context.put( "developmentTeam", getDevelopmentTeam() );

            context.put( "finalName", getFinalName() );

            context.put( "urlDownload", getUrlDownload() );

            processTemplate( context, getOutputDirectory(), template );
        }
        catch ( ResourceNotFoundException rnfe )
        {
            throw new MojoExecutionException( "resource not found." );
        }
        catch ( VelocityException ve )
        {
            throw new MojoExecutionException( ve.toString() );
        }
    }

    public void doGenerate( List releases )
        throws MojoExecutionException
    {
        try
        {
            Context context = new VelocityContext();

            getLog().info( "Creating announcement file from JIRA releases..." );

            if ( getIntroduction() == null || getIntroduction().equals( "" ) )
            {
                setIntroduction( getUrl() );
            }

            context.put( "releases", releases );

            context.put( "groupId", getGroupId() );

            context.put( "artifactId", getArtifactId() );

            context.put( "version", getVersion() );

            context.put( "packaging", getPackaging() );

            context.put( "url", getUrl() );

            context.put( "release", getLatestRelease( releases ) );

            context.put( "introduction", getIntroduction() );

            context.put( "developmentTeam", getDevelopmentTeam() );

            context.put( "finalName", getFinalName() );

            context.put( "urlDownload", getUrlDownload() );

            processTemplate( context, getOutputDirectory(), template );
        }
        catch ( ResourceNotFoundException rnfe )
        {
            throw new MojoExecutionException( "resource not found." );
        }
        catch ( VelocityException ve )
        {
            throw new MojoExecutionException( ve.toString() );
        }
    }

    /**
     * Get the latest release by matching the supplied releases
     * with the version in the pom
     *
     * @param releases list of releases
     * @throws MojoExecutionException
     */
    public Release getLatestRelease( List releases )
        throws MojoExecutionException
    {
        boolean isFound = false;

        Release release = null;

        // Remove "-SNAPSHOT" from the end, if it's there
        String pomVersion = getVersion();
        if ( pomVersion != null && pomVersion.endsWith( SNAPSHOT_SUFFIX ) )
        {
            pomVersion = pomVersion.substring( 0, pomVersion.length() - SNAPSHOT_SUFFIX.length() );
        }

        for ( int i = 0; i < releases.size(); i++ )
        {
            release = (Release) releases.get( i );

            if ( release.getVersion().equals( pomVersion ) )
            {
                isFound = true;
                return release;
            }
        }

        if ( !isFound )
        {
            throw new MojoExecutionException( "Couldn't find the release '" + pomVersion
                + "' among the supplied releases." );
        }
        return release;
    }

    /**
     * Create the velocity template
     *
     * @param context velocity context that has the parameter values
     * @param outputDirectory directory where the file will be generated
     * @param template velocity template which will the context be merged
     * @throws ResourceNotFoundException, VelocityException, IOException
     */
    public void processTemplate( Context context, String outputDirectory, String template )
        throws ResourceNotFoundException, VelocityException, MojoExecutionException
    {
        File f;

        try
        {
            f = new File( outputDirectory, template );

            if ( !f.getParentFile().exists() )
            {
                f.getParentFile().mkdirs();
            }

            Writer writer = new FileWriter( f );

            getVelocity().getEngine().mergeTemplate( templateDirectory + "/" + template, context, writer );

            writer.flush();

            writer.close();

            getLog().info( "File created..." );
        }

        catch ( ResourceNotFoundException rnfe )
        {
            throw new ResourceNotFoundException( "Template not found. ( " + templateDirectory + "/" + template + " )" );
        }
        catch ( VelocityException ve )
        {
            throw new VelocityException( ve.toString() );
        }

        catch ( Exception e )
        {
            throw new MojoExecutionException( e.toString(), e.getCause() );
        }
    }

    public void doJiraGenerate()
        throws MojoExecutionException
    {
        JiraDownloader jiraDownloader = new JiraDownloader();

        File jiraXMLFile = new File( jiraXML );

        jiraDownloader.setLog( getLog() );

        jiraDownloader.setOutput( jiraXMLFile );

        jiraDownloader.setStatusIds( statusId );

        jiraDownloader.setResolutionIds( resolutionId );

        jiraDownloader.setMavenProject( project );

        jiraDownloader.setSettings( setting );

        jiraDownloader.setNbEntries( nbEntries );

        try
        {
            jiraDownloader.doExecute();

            if ( jiraXMLFile.exists() )
            {
                JiraAnnouncementParser jiraParser = new JiraAnnouncementParser( jiraXMLFile );

                List issues = jiraParser.getIssues();

                List releases = jiraParser.getReleases( issues );

                doGenerate( releases );
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to download JIRA Announcement", e );
        }
    }

    /*
     * accessors
     */

    public String getXmlPath()
    {
        return xmlPath;
    }

    public void setXmlPath( String xmlPath )
    {
        this.xmlPath = xmlPath;
    }

    public String getOutputDirectory()
    {
        return outputDirectory;
    }

    public void setOutputDirectory( String outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public ChangesXML getXml()
    {
        return xml;
    }

    public void setXml( ChangesXML xml )
    {
        this.xml = xml;
    }

    public String getPackaging()
    {
        return packaging;
    }

    public void setPackaging( String packaging )
    {
        this.packaging = packaging;
    }

    public String getDevelopmentTeam()
    {
        return developmentTeam;
    }

    public void setDevelopmentTeam( String developmentTeam )
    {
        this.developmentTeam = developmentTeam;
    }

    public String getIntroduction()
    {
        return introduction;
    }

    public void setIntroduction( String introduction )
    {
        this.introduction = introduction;
    }

    public VelocityComponent getVelocity()
    {
        return velocity;
    }

    public void setVelocity( VelocityComponent velocity )
    {
        this.velocity = velocity;
    }

    public String getFinalName()
    {
        return finalName;
    }

    public void setFinalName( String finalName )
    {
        this.finalName = finalName;
    }

    public String getUrlDownload()
    {
        return urlDownload;
    }

    public void setUrlDownload( String urlDownload )
    {
        this.urlDownload = urlDownload;
    }
}
