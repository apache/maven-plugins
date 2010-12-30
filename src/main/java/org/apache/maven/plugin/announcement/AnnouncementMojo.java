package org.apache.maven.plugin.announcement;

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
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.changes.ChangesXML;
import org.apache.maven.plugin.changes.ProjectUtils;
import org.apache.maven.plugin.changes.ReleaseUtils;
import org.apache.maven.plugin.jira.JiraAdapter;
import org.apache.maven.plugin.jira.JiraXML;
import org.apache.maven.plugins.changes.model.Release;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.velocity.VelocityComponent;

/**
 * Goal which generate the template for an announcement.
 *
 * @goal announcement-generate
 * @requiresDependencyResolution test
 * @author aramirez@exist.com
 * @version $Id$
 * @since 2.0-beta-2
 */
public class AnnouncementMojo
    extends AbstractAnnouncementMojo
{
    private ReleaseUtils releaseUtils = new ReleaseUtils( getLog() );

    /**
     * Directory where the template file will be generated.
     *
     * @parameter expression="${project.build.directory}/announcement"
     * @required
     */
    private File outputDirectory;

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
     * @parameter expression="${changes.version}" default-value="${project.version}"
     * @required
     */
    private String version;

    /**
     * Distribution URL of the artifact.
     * This parameter will be passed to the template.
     *
     * @parameter expression="${project.url}"
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
     * @parameter expression="${changes.finalName}" default-value="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * URL where the artifact can be downloaded. If not specified,
     * no URL is used.
     * This parameter will be passed to the template.
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
    private File xmlPath;

    /**
     * Name of the team that develops the artifact.
     * This parameter will be passed to the template.
     *
     * @parameter default-value="${project.name} team" expression="${changes.developmentTeam}"
     * @required
     */
    private String developmentTeam;

    /**
     * Short description or introduction of the released artifact.
     * This parameter will be passed to the template.
     *
     * @parameter default-value="${project.description}"
     */
    private String introduction;

    /**
     * Velocity Component.
     *
     * @component role="org.codehaus.plexus.velocity.VelocityComponent" roleHint="maven-changes-plugin"
     * @readonly
     */
    private VelocityComponent velocity;

    /**
     * The Velocity template used to format the announcement.
     *
     * @parameter default-value="announcement.vm" expression="${changes.template}"
     * @required
     */
    private String template;

    /**
     * Directory that contains the template.
     * <p>
     * <b>Note:</b> This directory must be a subdirectory of
     * <code>/src/main/resources/ or current project base directory</code>.
     * </p>
     *
     * @parameter default-value="org/apache/maven/plugin/announcement" expression="${changes.templateDirectory}"
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
    private Settings settings;

    /**
     * Flag to determine if the plugin will generate a JIRA announcement.
     *
     * @parameter expression="${generateJiraAnnouncement}" default-value="false"
     * @required
     */
    private boolean generateJiraAnnouncement;

    /**
     * Defines the filter parameters to restrict which issues are retrieved
     * from JIRA. The filter parameter uses the same format of url
     * parameters that is used in a JIRA search.
     *
     * @parameter default-value=""
     * @since 2.4
     */
    private String filter;

    /**
     * Include issues from JIRA with these status ids. Multiple status ids can
     * be specified as a comma separated list of ids.
     * <p>
     * <b>Note:</b> In versions 2.0-beta-3 and earlier this parameter was
     * called "statusId".
     * </p>
     *
     * @parameter default-value="Closed" expression="${changes.statusIds}"
     */
    private String statusIds;

    /**
     * Include issues from JIRA with these resolution ids. Multiple resolution
     * ids can be specified as a comma separated list of ids.
     * <p>
     * <b>Note:</b> In versions 2.0-beta-3 and earlier this parameter was
     * called "resolutionId".
     * </p>
     *
     * @parameter default-value="Fixed" expression="${changes.resolutionIds}"
     */
    private String resolutionIds;

    /**
     * The path of the XML file of JIRA-announcements to be parsed.
     *
     * @parameter expression="${project.build.directory}/jira-announcement.xml"
     * @required
     * @readonly
     */
    private File jiraXML;

    /**
     * The maximum number of issues to fetch from JIRA.
     * <p>
     * <b>Note:</b> In versions 2.0-beta-3 and earlier this parameter was
     * called "nbEntries".
     * </p>
     *
     * @parameter default-value="25"  expression="${changes.maxEntries}"
     * @required
     */
    private int maxEntries;

    /**
     * Defines the JIRA username for authentication into a private JIRA installation.
     *
     * @parameter default-value="" expression="${changes.jiraUser}"
     * @since 2.1
     */
    private String jiraUser;

    /**
     * Defines the JIRA password for authentication into a private JIRA installation.
     *
     * @parameter default-value="" expression="${changes.jiraPassword}"
     * @since 2.1
     */
    private String jiraPassword;

    /**
     * Defines the http user for basic authentication into the JIRA webserver.
     *
     * @parameter default-value=""
     * @since 2.4
     */
    private String webUser;

    /**
     * Defines the http password for basic authentication into the JIRA webserver.
     *
     * @parameter default-value=""
     * @since 2.4
     */
    private String webPassword;

    /**
     * The template encoding.
     *
     * @parameter expression="${changes.templateEncoding}" default-value="${project.build.sourceEncoding}"
     * @since 2.1
     */
    private String templateEncoding;

    /**
     * If releases from JIRA should be merged with the releases from a
     * changes.xml file.
     *
     * @parameter expression="${changes.jiraMerge}" default-value="false"
     * @since 2.1
     */
    private boolean jiraMerge;

    /**
     * Map of custom parameters for the announcement.
     * This Map will be passed to the template.
     *
     * @parameter
     * @since 2.1
     */
    private Map announceParameters;

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
        // Run only at the execution root
        if ( runOnlyAtExecutionRoot && !isThisTheExecutionRoot() )
        {
            getLog().info( "Skipping the announcement generation in this project because it's not the Execution Root" );
        }
        else
        {
            if ( this.jiraMerge )
            {
                ChangesXML changesXML =  new ChangesXML( getXmlPath(), getLog() );
                List changesReleases = changesXML.getReleaseList();
                if ( ProjectUtils.validateIfIssueManagementComplete( project, "JIRA", "JIRA announcement", getLog() ) )
                {
                    List jiraReleases = getJiraReleases();
                    List mergedReleases = releaseUtils.mergeReleases( changesReleases, jiraReleases );
                    doGenerate( mergedReleases );
                }
                else
                {
                    throw new MojoExecutionException( "Something is wrong with the Issue Management section."
                        + " See previous error messages." );
                }
            }
            else
            {
                if ( !generateJiraAnnouncement )
                {
                    if ( getXmlPath().exists() )
                    {
                        setXml( new ChangesXML( getXmlPath(), getLog() ) );

                        getLog().info( "Creating announcement file from " + getXmlPath() + "..." );

                        doGenerate( getXml().getReleaseList() );
                    }
                    else
                    {
                        getLog().warn( "changes.xml file " + getXmlPath().getAbsolutePath() + " does not exist." );
                    }
                }
                else
                {
                    doJiraGenerate();
                }
            }
        }
    }

    /**
     * Add the parameters to velocity context
     *
     * @param releases A <code>List</code> of <code>Release</code>s
     * @throws MojoExecutionException
     */
    public void doGenerate( List releases )
        throws MojoExecutionException
    {
        doGenerate( releases, releaseUtils.getLatestRelease( releases, getVersion() )  );
    }

    protected void doGenerate( List releases, Release release )
        throws MojoExecutionException
    {
        try
        {
            Context context = new VelocityContext();

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

            context.put( "release", release );

            context.put( "introduction", getIntroduction() );

            context.put( "developmentTeam", getDevelopmentTeam() );

            context.put( "finalName", getFinalName() );

            context.put( "urlDownload", getUrlDownload() );

            context.put( "project", project );

            if ( announceParameters == null )
            {
                // empty Map to prevent NPE in velocity execution
                context.put( "announceParameters", Collections.EMPTY_MAP );
            }
            else
            {
                context.put( "announceParameters", announceParameters );
            }


            processTemplate( context, getOutputDirectory(), template );
        }
        catch ( ResourceNotFoundException rnfe )
        {
            throw new MojoExecutionException( "Resource not found.", rnfe );
        }
        catch ( VelocityException ve )
        {
            throw new MojoExecutionException( ve.toString(), ve );
        }
    }

    /**
     * Create the velocity template
     *
     * @param context velocity context that has the parameter values
     * @param outputDirectory directory where the file will be generated
     * @param template velocity template which will the context be merged
     * @throws ResourceNotFoundException, VelocityException, IOException
     */
    public void processTemplate( Context context, File outputDirectory, String template )
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

            VelocityEngine engine = velocity.getEngine();

            engine.setApplicationAttribute( "baseDirectory", basedir );

            if ( StringUtils.isEmpty( templateEncoding ) )
            {
                templateEncoding =  ReaderFactory.FILE_ENCODING;
                getLog().warn(
                               "File encoding has not been set, using platform encoding " + templateEncoding
                                   + ", i.e. build is platform dependent!" );
            }

            Writer writer = new OutputStreamWriter( new FileOutputStream( f ), templateEncoding );

            Template velocityTemplate = engine.getTemplate( templateDirectory + "/" + template, templateEncoding );

            velocityTemplate.merge( context, writer );

            writer.flush();

            writer.close();

            getLog().info( "Created template " + f );
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
            if ( e.getCause() != null )
            {
                getLog().warn( e.getCause() );
            }
            throw new MojoExecutionException( e.toString(), e.getCause() );
        }
    }

    public void doJiraGenerate()
        throws MojoExecutionException
    {
        if ( ProjectUtils.validateIfIssueManagementComplete( project, "JIRA", "JIRA announcement", getLog() ) )
        {
            List releases = getJiraReleases();

            getLog().info( "Creating announcement file from JIRA releases..." );

            doGenerate( releases );
        }
        else
        {
            throw new MojoExecutionException( "Something is wrong with the Issue Management section."
                + " See previous error messages." );
        }
    }

    protected List getJiraReleases()
        throws MojoExecutionException
    {
        JiraDownloader jiraDownloader = new JiraDownloader();

        File jiraXMLFile = jiraXML;

        jiraDownloader.setLog( getLog() );

        jiraDownloader.setOutput( jiraXMLFile );

        jiraDownloader.setStatusIds( statusIds );

        jiraDownloader.setResolutionIds( resolutionIds );

        jiraDownloader.setMavenProject( project );

        jiraDownloader.setSettings( settings );

        jiraDownloader.setNbEntries( maxEntries );

        jiraDownloader.setFilter( filter );

        jiraDownloader.setJiraUser( jiraUser );

        jiraDownloader.setJiraPassword( jiraPassword );

        jiraDownloader.setWebUser( webUser );

        jiraDownloader.setWebPassword( webPassword );

        try
        {
            jiraDownloader.doExecute();

            if ( jiraXMLFile.exists() )
            {
                JiraXML jiraParser = new JiraXML( jiraXMLFile );

                List issues = jiraParser.getIssueList();

                return JiraAdapter.getReleases( issues );
            }
            else
            {
                getLog().warn( "jira file " + jiraXMLFile.getPath() + " doesn't exists " );
            }
            return Collections.EMPTY_LIST;
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Failed to extract JIRA issues from the downloaded file", e );
        }
    }

    /*
     * accessors
     */

    public File getXmlPath()
    {
        return xmlPath;
    }

    public void setXmlPath( File xmlPath )
    {
        this.xmlPath = xmlPath;
    }

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public void setOutputDirectory( File outputDirectory )
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
