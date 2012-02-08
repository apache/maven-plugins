package org.apache.maven.plugin.changelog;

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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.model.Developer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.scm.ChangeFile;
import org.apache.maven.scm.ChangeSet;
import org.apache.maven.scm.ScmBranch;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.ScmRevision;
import org.apache.maven.scm.command.changelog.ChangeLogScmResult;
import org.apache.maven.scm.command.changelog.ChangeLogSet;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.ScmProviderRepositoryWithHost;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;

/**
 * Generate a changelog report.
 *
 * @version $Id$
 * @goal changelog
 */
public class ChangeLogReport
    extends AbstractMavenReport
{
    /**
     * A special token that represents the SCM relative path for a file.
     * It can be used in <code>displayFileDetailUrl</code>.
     */
    private static final String FILE_TOKEN = "%FILE%";

    /**
     * A special token that represents a Mantis/Bugzilla/JIRA/etc issue ID.
     * It can be used in the <code>issueLinkUrl</code>.
     */
    private static final String ISSUE_TOKEN = "%ISSUE%";

    /**
    * A special token that represents the SCM revision number.
    * It can be used in <code>displayChangeSetDetailUrl</code>
    * and <code>displayFileRevDetailUrl</code>.
    */
    private static final String REV_TOKEN = "%REV%";

    /**
     * The number of days to use as a range, when this is not specified.
     */
    private static final int DEFAULT_RANGE = 30;

    /**
     * Used to specify the format to use for the dates in the headings of the
     * report.
     *
     * @parameter expression="${changelog.headingDateFormat}" default-value="yyyy-MM-dd"
     * @since 2.1
     */
    private String headingDateFormat = "yyyy-MM-dd";

    /**
     * Used to specify whether to build the log using range, tag or date.
     *
     * @parameter expression="${changelog.type}" default-value="range"
     * @required
     */
    private String type;

    /**
     * Used to specify the number of days of log entries to retrieve.
     *
     * @parameter expression="${changelog.range}" default-value="-1"
     */
    private int range;

    /**
     * Used to specify the absolute date (or list of dates) to start log entries from.
     *
     * @parameter
     */
    private List dates;

    /**
     * Used to specify the tag (or list of tags) to start log entries from.
     *
     * @parameter
     */
    private List tags;

    /**
     * Used to specify the date format of the log entries that are retrieved from your SCM system.
     *
     * @parameter expression="${changelog.dateFormat}" default-value="yyyy-MM-dd HH:mm:ss"
     * @required
     */
    private String dateFormat;

    /**
     * Input dir. Directory where the files under SCM control are located.
     *
     * @parameter expression="${basedir}"
     * @required
     */
    private File basedir;

    /**
     * Output file for xml document
     *
     * @parameter expression="${project.build.directory}/changelog.xml"
     * @required
     */
    private File outputXML;

    /**
     * Allows the user to make changelog regenerate the changelog.xml file for the specified time in minutes.
     *
     * @parameter expression="${outputXMLExpiration}" default-value="60"
     * @required
     */
    private int outputXMLExpiration;

    /**
     * Comment format string used for interrogating
     * the revision control system.
     * Currently only used by the ClearcaseChangeLogGenerator.
     *
     * @parameter expression="${changelog.commentFormat}"
     */
    private String commentFormat;

    /**
     * The file encoding when writing non-HTML reports.
     *
     * @parameter expression="${changelog.outputEncoding}" default-value="${project.reporting.outputEncoding}"
     */
    private String outputEncoding;

    /**
     * The user name (used by svn and starteam protocol).
     *
     * @parameter expression="${username}"
     */
    private String username;

    /**
     * The user password (used by svn and starteam protocol).
     *
     * @parameter expression="${password}"
     */
    private String password;

    /**
     * The private key (used by java svn).
     *
     * @parameter expression="${privateKey}"
     */
    private String privateKey;

    /**
     * The passphrase (used by java svn).
     *
     * @parameter expression="${passphrase}"
     */
    private String passphrase;

    /**
     * The url of tags base directory (used by svn protocol).
     *
     * @parameter expression="${tagBase}"
     */
    private String tagBase;

    /**
     * The URL to view the scm. Basis for external links from the generated report.
     *
     * @parameter expression="${project.scm.url}"
     */
    protected String scmUrl;

    /**
     * The Maven Project Object
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The directory where the report will be generated
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * @component
     */
    private Renderer siteRenderer;

    /**
     * @parameter expression="${settings.offline}"
     * @required
     * @readonly
     */
    private boolean offline;

    /**
     * @component
     */
    private ScmManager manager;

    /**
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    /**
     * Allows the user to choose which scm connection to use when connecting to the scm.
     * Can either be "connection" or "developerConnection".
     *
     * @parameter default-value="connection"
     * @required
     */
    private String connectionType;

    /**
     * A template string that is used to create the URL to the file details.
     * There is a special token that you can use in your template:
     * <ul>
     * <li><code>%FILE%</code> - this is the path to a file</li>
     * </ul>
     * <p>
     * Example:
     * <code>http://checkstyle.cvs.sourceforge.net/checkstyle%FILE%?view=markup</code>
     * </p>
     * <p>
     * <strong>Note:</strong> If you don't supply the token in your template,
     * the path of the file will simply be appended to your template URL.
     * </p>
     *
     * @parameter expression="${displayFileDetailUrl}" default-value="${project.scm.url}"
     */
    protected String displayFileDetailUrl;

    /**
     * A pattern used to identify 'issue tracker' IDs such as those used by JIRA,
     * Bugzilla and alike in the SCM commit messages. Any matched patterns
     * are replaced with <code>issueLinkUrl<code> URL. The default
     * value is a JIRA-style issue identification pattern.
     *
     * @parameter expression="${issueIDRegexPattern}" default-value="[a-zA-Z]{2,}-\\d+"
     * @required
     * @since 2.2
     */
    private String issueIDRegexPattern;

    /**
     * The issue tracker URL used when replacing any matched <code>issueIDRegexPattern</code>
     * found in the SCM commit messages. The default is URL is the codehaus JIRA
     * URL. If %ISSUE% is found in the URL it is replaced with the matched issue ID,
     * otherwise the matched issue ID is appended to the URL.
     *
     * @parameter expression="${issueLinkUrl}" default-value="http://jira.codehaus.org/browse/%ISSUE%"
     * @required
     * @since 2.2
     */
    private String issueLinkUrl;

    /**
     * A template string that is used to create the changeset URL.
     *
     * If not defined no change set link will be created.
     *
     * There is one special token that you can use in your template:
     * <ul>
     * <li><code>%REV%</code> - this is the changeset revision</li>
     * </ul>
     * <p>
     * Example:
     * <code>http://fisheye.sourceforge.net/changelog/a-project/?cs=%REV%</code>
     * </p>
     * <p>
     * <strong>Note:</strong> If you don't supply the %REV% token in your template,
     * the revision will simply be appended to your template URL.
     * </p>
     *
     * @parameter expression="${displayChangeSetDetailUrl}"
     * @since 2.2
     */
    protected String displayChangeSetDetailUrl;

    /**
     * A template string that is used to create the revision aware URL to
     * the file details in a similar fashion to the <code>displayFileDetailUrl</code>.
     * When a report contains both file and file revision information, as in the
     * Change Log report, this template string can be used to create a revision
     * aware URL to the file details.
     *
     * If not defined this template string defaults to the same value as the
     * <code>displayFileDetailUrl</code> and thus revision number aware links will
     * not be used.
     *
     * There are two special tokens that you can use in your template:
     * <ul>
     * <li><code>%FILE%</code> - this is the path to a file</li>
     * <li><code>%REV%</code> - this is the revision of the file</li>
     * </ul>
     * <p>
     * Example:
     * <code>http://fisheye.sourceforge.net/browse/a-project/%FILE%?r=%REV%</code>
     * </p>
     * <p>
     * <strong>Note:</strong> If you don't supply the %FILE% token in your template,
     * the path of the file will simply be appended to your template URL.
     * </p>
     *
     * @parameter expression="${displayFileRevDetailUrl}"
     * @since 2.2
     */
    protected String displayFileRevDetailUrl;

    /**
     * List of developers to be shown on the report.
     *
     * @parameter expression="${project.developers}"
     * @since 2.2
     */
    protected List developers;

    /**
     * List of provider implementations.
     *
     * @parameter
     */
    private Map providerImplementations;
    
    // temporary field holder while generating the report
    private String rptRepository, rptOneRepoParam, rptMultiRepoParam;

    // field for SCM Connection URL
    private String connection;

    // field used to hold a map of the developers by Id
    private HashMap developersById;

    // field used to hold a map of the developers by Name
    private HashMap developersByName;

    /**
     * The system properties to use (needed by the perforce scm provider).
     *
     * @parameter
     */
    private Properties systemProperties;

    /** {@inheritDoc} */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        //check if sources exists <-- required for parent poms
        if ( !basedir.exists() )
        {
            doGenerateEmptyReport( getBundle( locale ), getSink() );

            return;
        }

        if ( providerImplementations != null )
        {
            for ( Iterator i = providerImplementations.keySet().iterator(); i.hasNext(); )
            {
                String providerType = (String) i.next();
                String providerImplementation = (String) providerImplementations.get( providerType );
                getLog().info( "Change the default '" + providerType + "' provider implementation to '"
                    + providerImplementation + "'." );
                manager.setScmProviderImplementation( providerType, providerImplementation );
            }
        }
        
        initializeDefaultConfigurationParameters();

        initializeDeveloperMaps();

        verifySCMTypeParams();

        if ( systemProperties != null )
        {
            // Add all system properties configured by the user
            Iterator iter = systemProperties.keySet().iterator();

            while ( iter.hasNext() )
            {
                String key = (String) iter.next();

                String value = systemProperties.getProperty( key );

                System.setProperty( key, value );

                getLog().debug( "Setting system property: " + key + "=" + value );
            }
        }

        doGenerateReport( getChangedSets(), getBundle( locale ), getSink() );
    }

    /**
     * Initializes any configuration parameters that have not/can not be defined
     * or defaulted by the Mojo API.
     */
    private void initializeDefaultConfigurationParameters()
    {
        if ( displayFileRevDetailUrl == null || displayFileRevDetailUrl.length() == 0 )
        {
            displayFileRevDetailUrl = displayFileDetailUrl;
        }
    }

    /**
     * Creates maps of the project developers by developer Id and developer Name
     * for quick lookups.
     */
    private void initializeDeveloperMaps()
    {
        developersById = new HashMap();
        developersByName = new HashMap();

        if ( developers != null )
        {
            for ( Iterator i = developers.iterator(); i.hasNext(); )
            {
                Developer developer = (Developer) i.next();

                developersById.put( developer.getId(), developer );
                developersByName.put( developer.getName(), developer );
            }
        }
    }

    /**
     * populates the changedSets field by either connecting to the SCM or using an existing XML generated in a previous
     * run of the report
     *
     * @throws MavenReportException
     */
    protected List getChangedSets()
        throws MavenReportException
    {
        List changelogList = null;

        if ( !outputXML.isAbsolute() )
        {
            outputXML = new File( project.getBasedir(), outputXML.getPath() );
        }

        if ( outputXML.exists() )
        {
            if ( outputXMLExpiration > 0
                && outputXMLExpiration * 60000 > System.currentTimeMillis() - outputXML.lastModified() )
            {
                try
                {
                    //ReaderFactory.newReader( outputXML, getOutputEncoding() );
                    //FileInputStream fIn = new FileInputStream( outputXML );

                    getLog().info( "Using existing changelog.xml..." );

                    changelogList = ChangeLog.loadChangedSets( ReaderFactory.newReader( outputXML, getOutputEncoding() ) );
                }
                catch ( FileNotFoundException e )
                {
                    //do nothing, just regenerate
                }
                catch ( Exception e )
                {
                    throw new MavenReportException( "An error occurred while parsing " + outputXML.getAbsolutePath(),
                                                    e );
                }
            }
        }

        if ( changelogList == null )
        {
            if ( offline )
            {
                throw new MavenReportException( "This report requires online mode." );
            }

            getLog().info( "Generating changed sets xml to: " + outputXML.getAbsolutePath() );

            changelogList = generateChangeSetsFromSCM();

            try
            {
                writeChangelogXml( changelogList );
            }
            catch ( FileNotFoundException e )
            {
                throw new MavenReportException( "Can't create " + outputXML.getAbsolutePath(), e );
            }
            catch ( UnsupportedEncodingException e )
            {
                throw new MavenReportException( "Can't create " + outputXML.getAbsolutePath(), e );
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Can't create " + outputXML.getAbsolutePath(), e );
            }
        }

        return changelogList;
    }

    private void writeChangelogXml( List changelogList )
        throws FileNotFoundException, UnsupportedEncodingException, IOException
    {
        StringBuffer changelogXml = new StringBuffer();

        changelogXml.append( "<?xml version=\"1.0\" encoding=\"" ).append( getOutputEncoding() ).append( "\"?>\n" );
        changelogXml.append( "<changelog>" );

        for ( Iterator sets = changelogList.iterator(); sets.hasNext(); )
        {
            changelogXml.append( "\n  " );

            ChangeLogSet changelogSet = (ChangeLogSet) sets.next();
            String changeset = changelogSet.toXML( getOutputEncoding() );

            //remove xml header
            if ( changeset.startsWith( "<?xml" ) )
            {
                int idx = changeset.indexOf( "?>" ) + 2;
                changeset = changeset.substring( idx );
            }

            changelogXml.append( changeset );
        }

        changelogXml.append( "\n</changelog>" );

        outputXML.getParentFile().mkdirs();

        //PrintWriter pw = new PrintWriter( new BufferedOutputStream( new FileOutputStream( outputXML ) ) );
        //pw.write( changelogXml.toString() );
        //pw.flush();
        //pw.close();
        // MCHANGELOG-86
        Writer writer = WriterFactory.newWriter( new BufferedOutputStream( new FileOutputStream( outputXML ) ), getOutputEncoding() );
        writer.write( changelogXml.toString() );
        writer.flush();
        writer.close();
    }

    /**
     * creates a ChangeLog object and then connects to the SCM to generate the changed sets
     *
     * @return changedlogsets generated from the SCM
     * @throws MavenReportException
     */
    protected List generateChangeSetsFromSCM()
        throws MavenReportException
    {
        try
        {
            List changeSets = new ArrayList();

            ScmRepository repository = getScmRepository();

            ScmProvider provider = manager.getProviderByRepository( repository );

            ChangeLogScmResult result;

            if ( "range".equals( type ) )
            {
                result = provider.changeLog( repository, new ScmFileSet( basedir ), null, null, range, (ScmBranch) null,
                                             dateFormat );

                checkResult( result );

                changeSets.add( result.getChangeLog() );
            }
            else if ( "tag".equals( type ) )
            {
                if ( repository.getProvider().equals( "svn" ) )
                {
                    throw new MavenReportException( "The type '" + type + "' isn't supported for svn." );
                }

                Iterator tagsIter = tags.iterator();

                String startTag = (String) tagsIter.next();
                String endTag = null;

                if ( tagsIter.hasNext() )
                {
                    while ( tagsIter.hasNext() )
                    {
                        endTag = (String) tagsIter.next();

                        result = provider.changeLog( repository, new ScmFileSet( basedir ), new ScmRevision( startTag ),
                                                     new ScmRevision( endTag ) );

                        checkResult( result );

                        changeSets.add( result.getChangeLog() );

                        startTag = endTag;
                    }
                }
                else
                {
                    result = provider.changeLog( repository, new ScmFileSet( basedir ), new ScmRevision( startTag ),
                                                 new ScmRevision( endTag ) );

                    checkResult( result );

                    changeSets.add( result.getChangeLog() );
                }
            }
            else if ( "date".equals( type ) )
            {
                Iterator dateIter = dates.iterator();

                String startDate = (String) dateIter.next();
                String endDate = null;

                if ( dateIter.hasNext() )
                {
                    while ( dateIter.hasNext() )
                    {
                        endDate = (String) dateIter.next();

                        result = provider.changeLog( repository, new ScmFileSet( basedir ), parseDate( startDate ),
                                                     parseDate( endDate ), 0, (ScmBranch) null );

                        checkResult( result );

                        changeSets.add( result.getChangeLog() );

                        startDate = endDate;
                    }
                }
                else
                {
                    result = provider.changeLog( repository, new ScmFileSet( basedir ), parseDate( startDate ),
                                                 parseDate( endDate ), 0, (ScmBranch) null );

                    checkResult( result );

                    changeSets.add( result.getChangeLog() );
                }
            }
            else
            {
                throw new MavenReportException( "The type '" + type + "' isn't supported." );
            }

            return changeSets;
        }
        catch ( ScmException e )
        {
            throw new MavenReportException( "Cannot run changelog command : ", e );
        }
        catch ( MojoExecutionException e )
        {
            throw new MavenReportException( "An error has occurred during changelog command : ", e );
        }
    }

    /**
     * Converts the localized date string pattern to date object.
     *
     * @return A date
     */
    private Date parseDate( String date )
        throws MojoExecutionException
    {
        if ( date == null || date.trim().length() == 0 )
        {
            return null;
        }

        SimpleDateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd" );

        try
        {
            return formatter.parse( date );
        }
        catch ( ParseException e )
        {
            throw new MojoExecutionException( "Please use this date pattern: " + formatter.toLocalizedPattern(), e );
        }
    }

    public ScmRepository getScmRepository()
        throws ScmException
    {
        ScmRepository repository;

        try
        {
            repository = manager.makeScmRepository( getConnection() );

            ScmProviderRepository providerRepo = repository.getProviderRepository();

            if ( !StringUtils.isEmpty( username ) )
            {
                providerRepo.setUser( username );
            }

            if ( !StringUtils.isEmpty( password ) )
            {
                providerRepo.setPassword( password );
            }

            if ( repository.getProviderRepository() instanceof ScmProviderRepositoryWithHost )
            {
                ScmProviderRepositoryWithHost repo = (ScmProviderRepositoryWithHost) repository.getProviderRepository();

                loadInfosFromSettings( repo );

                if ( !StringUtils.isEmpty( username ) )
                {
                    repo.setUser( username );
                }

                if ( !StringUtils.isEmpty( password ) )
                {
                    repo.setPassword( password );
                }

                if ( !StringUtils.isEmpty( privateKey ) )
                {
                    repo.setPrivateKey( privateKey );
                }

                if ( !StringUtils.isEmpty( passphrase ) )
                {
                    repo.setPassphrase( passphrase );
                }
            }

            if ( !StringUtils.isEmpty( tagBase ) && repository.getProvider().equals( "svn" ) )
            {
                SvnScmProviderRepository svnRepo = (SvnScmProviderRepository) repository.getProviderRepository();

                svnRepo.setTagBase( tagBase );
            }
        }
        catch ( Exception e )
        {
            throw new ScmException( "Can't load the scm provider.", e );
        }

        return repository;
    }

    /**
     * Load username password from settings if user has not set them in JVM properties
     *
     * @param repo
     */
    private void loadInfosFromSettings( ScmProviderRepositoryWithHost repo )
    {
        if ( username == null || password == null )
        {
            String host = repo.getHost();

            int port = repo.getPort();

            if ( port > 0 )
            {
                host += ":" + port;
            }

            Server server = this.settings.getServer( host );

            if ( server != null )
            {
                if ( username == null )
                {
                    username = this.settings.getServer( host ).getUsername();
                }

                if ( password == null )
                {
                    password = this.settings.getServer( host ).getPassword();
                }

                if ( privateKey == null )
                {
                    privateKey = this.settings.getServer( host ).getPrivateKey();
                }

                if ( passphrase == null )
                {
                    passphrase = this.settings.getServer( host ).getPassphrase();
                }
            }
        }
    }

    public void checkResult( ScmResult result )
        throws MojoExecutionException
    {
        if ( !result.isSuccess() )
        {
            getLog().error( "Provider message:" );

            getLog().error( result.getProviderMessage() == null ? "" : result.getProviderMessage() );

            getLog().error( "Command output:" );

            getLog().error( result.getCommandOutput() == null ? "" : result.getCommandOutput() );

            throw new MojoExecutionException( "Command failed." );
        }
    }

    /**
     * used to retrieve the SCM connection string
     *
     * @return the url string used to connect to the SCM
     * @throws MavenReportException when there is insufficient information to retrieve the SCM connection string
     */
    protected String getConnection()
        throws MavenReportException
    {
        if ( this.connection != null )
        {
            return connection;
        }

        if ( project.getScm() == null )
        {
            throw new MavenReportException( "SCM Connection is not set." );
        }

        String scmConnection = project.getScm().getConnection();
        if ( StringUtils.isNotEmpty( scmConnection ) && "connection".equals( connectionType.toLowerCase() ) )
        {
            connection = scmConnection;
        }

        String scmDeveloper = project.getScm().getDeveloperConnection();
        if ( StringUtils.isNotEmpty( scmDeveloper ) && "developerconnection".equals( connectionType.toLowerCase() ) )
        {
            connection = scmDeveloper;
        }

        if ( StringUtils.isEmpty( connection ) )
        {
            throw new MavenReportException( "SCM Connection is not set." );
        }

        return connection;
    }

    /**
     * checks whether there are enough configuration parameters to generate the report
     *
     * @throws MavenReportException when there is insufficient paramters to generate the report
     */
    private void verifySCMTypeParams()
        throws MavenReportException
    {
        if ( "range".equals( type ) )
        {
            if ( range == -1 )
            {
                range = DEFAULT_RANGE;
            }
        }
        else if ( "date".equals( type ) )
        {
            if ( dates == null )
            {
                throw new MavenReportException(
                    "The dates parameter is required when type=\"date\"."
                    + " The value should be the absolute date for the start of the log." );
            }
        }
        else if ( "tag".equals( type ) )
        {
            if ( tags == null )
            {
                throw new MavenReportException( "The tags parameter is required when type=\"tag\"." );
            }
        }
        else
        {
            throw new MavenReportException( "The type parameter has an invalid value: " + type
                + ".  The value should be \"range\", \"date\", or \"tag\"." );
        }
    }

    /**
     * generates an empty report in case there are no sources to generate a report with
     *
     * @param bundle the resource bundle to retrieve report phrases from
     * @param sink   the report formatting tool
     */
    protected void doGenerateEmptyReport( ResourceBundle bundle, Sink sink )
    {
        sink.head();
        sink.title();
        sink.text( bundle.getString( "report.changelog.header" ) );
        sink.title_();
        sink.head_();

        sink.body();
        sink.section1();

        sink.sectionTitle1();
        sink.text( bundle.getString( "report.changelog.mainTitle" ) );
        sink.sectionTitle1_();

        sink.paragraph();
        sink.text( bundle.getString( "report.changelog.nosources" ) );
        sink.paragraph_();

        sink.section1_();

        sink.body_();
        sink.flush();
        sink.close();
    }

    /**
     * method that generates the report for this mojo. This method is overridden by dev-activity and file-activity mojo
     *
     * @param changeLogSets changed sets to generate the report from
     * @param bundle        the resource bundle to retrieve report phrases from
     * @param sink          the report formatting tool
     */
    protected void doGenerateReport( List changeLogSets, ResourceBundle bundle, Sink sink )
    {
        sink.head();
        sink.title();
        sink.text( bundle.getString( "report.changelog.header" ) );
        sink.title_();
        sink.head_();

        sink.body();
        sink.section1();

        sink.sectionTitle1();
        sink.text( bundle.getString( "report.changelog.mainTitle" ) );
        sink.sectionTitle1_();

        // Summary section
        doSummarySection( changeLogSets, bundle, sink );

        for ( Iterator sets = changeLogSets.iterator(); sets.hasNext(); )
        {
            ChangeLogSet changeLogSet = (ChangeLogSet) sets.next();

            doChangedSet( changeLogSet, bundle, sink );
        }

        sink.section1_();
        sink.body_();

        sink.flush();
        sink.close();
    }

    /**
     * generates the report summary section of the report
     *
     * @param changeLogSets changed sets to generate the report from
     * @param bundle        the resource bundle to retrieve report phrases from
     * @param sink          the report formatting tool
     */
    private void doSummarySection( List changeLogSets, ResourceBundle bundle, Sink sink )
    {
        sink.paragraph();

        sink.text( bundle.getString( "report.changelog.ChangedSetsTotal" ) );
        sink.text( ": " + changeLogSets.size() );

        sink.paragraph_();
    }

    /**
     * generates a section of the report referring to a changeset
     *
     * @param set    the current ChangeSet to generate this section of the report
     * @param bundle the resource bundle to retrieve report phrases from
     * @param sink   the report formatting tool
     */
    private void doChangedSet( ChangeLogSet set, ResourceBundle bundle, Sink sink )
    {
        sink.section2();

        doChangeSetTitle( set, bundle, sink );

        doSummary( set, bundle, sink );

        doChangedSetTable( set.getChangeSets(), bundle, sink );

        sink.section2_();
    }

    /**
     * Generate the title for the report.
     *
     * @param set    change set to generate the report from
     * @param bundle the resource bundle to retrieve report phrases from
     * @param sink   the report formatting tool
     */
    protected void doChangeSetTitle( ChangeLogSet set, ResourceBundle bundle, Sink sink )
    {
        sink.sectionTitle2();

        SimpleDateFormat headingDateFormater = new SimpleDateFormat( headingDateFormat );

        if ( "tag".equals( type ) )
        {
            if ( set.getStartVersion() == null || set.getStartVersion().getName() == null )
            {
                sink.text( bundle.getString( "report.SetTagCreation" ) );
                if ( set.getEndVersion() != null && set.getEndVersion().getName() != null )
                {
                    sink.text( " " + bundle.getString( "report.SetTagUntil" ) + " '" + set.getEndVersion() + "'" );
                }
            }
            else if ( set.getEndVersion() == null || set.getEndVersion().getName() == null )
            {
                sink.text( bundle.getString( "report.SetTagSince" ) );
                sink.text( " '" + set.getStartVersion() + "'" );
            }
            else
            {
                sink.text( bundle.getString( "report.SetTagBetween" ) );
                sink.text( " '" + set.getStartVersion() + "' " + bundle.getString( "report.And" ) + " '"
                    + set.getEndVersion() + "'" );
            }
        }
        else  if ( set.getStartDate() == null )
        {
            sink.text( bundle.getString( "report.SetRangeUnknown" ) );
        }
        else if ( set.getEndDate() == null )
        {
            sink.text( bundle.getString( "report.SetRangeSince" ) );
            sink.text( " " + headingDateFormater.format( set.getStartDate() ) );
        }
        else
        {
            sink.text( bundle.getString( "report.SetRangeBetween" ) );
            sink.text( " " + headingDateFormater.format( set.getStartDate() )
                + " " + bundle.getString( "report.And" ) + " "
                + headingDateFormater.format( set.getEndDate() ) );
        }
        sink.sectionTitle2_();
    }

    /**
     * Generate the summary section of the report.
     *
     * @param set    change set to generate the report from
     * @param bundle the resource bundle to retrieve report phrases from
     * @param sink   the report formatting tool
     */
    protected void doSummary( ChangeLogSet set, ResourceBundle bundle, Sink sink )
    {
        sink.paragraph();
        sink.text( bundle.getString( "report.TotalCommits" ) );
        sink.text( ": " + set.getChangeSets().size() );
        sink.lineBreak();
        sink.text( bundle.getString( "report.changelog.FilesChanged" ) );
        sink.text( ": " + countFilesChanged( set.getChangeSets() ) );
        sink.paragraph_();
    }

    /**
     * counts the number of files that were changed in the specified SCM
     *
     * @param entries a collection of SCM changes
     * @return number of files changed for the changedsets
     */
    protected long countFilesChanged( Collection entries )
    {
        if ( entries == null )
        {
            return 0;
        }

        if ( entries.isEmpty() )
        {
            return 0;
        }

        HashMap fileList = new HashMap();

        for ( Iterator i = entries.iterator(); i.hasNext(); )
        {
            ChangeSet entry = (ChangeSet) i.next();

            List files = entry.getFiles();

            for ( Iterator fileIterator = files.iterator(); fileIterator.hasNext(); )
            {
                ChangeFile file = (ChangeFile) fileIterator.next();

                String name = file.getName();

                if ( fileList.containsKey( name ) )
                {
                    LinkedList list = (LinkedList) fileList.get( name );

                    list.add( file );
                }
                else
                {
                    LinkedList list = new LinkedList();

                    list.add( file );

                    fileList.put( name, list );
                }
            }
        }

        return fileList.size();
    }

    /**
     * generates the report table showing the SCM log entries
     *
     * @param entries a list of change log entries to generate the report from
     * @param bundle  the resource bundle to retrieve report phrases from
     * @param sink    the report formatting tool
     */
    private void doChangedSetTable( Collection entries, ResourceBundle bundle, Sink sink )
    {
        sink.table();

        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.changelog.timestamp" ) );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.changelog.author" ) );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.changelog.details" ) );
        sink.tableHeaderCell_();
        sink.tableRow_();

        initReportUrls();

        List sortedEntries = new ArrayList( entries );
        Collections.sort( sortedEntries, new Comparator()
        {
            public int compare( Object arg0, Object arg1 )
            {
                ChangeSet changeSet0 = (ChangeSet) arg0;
                ChangeSet changeSet1 = (ChangeSet) arg1;
                return changeSet1.getDate().compareTo( changeSet0.getDate() );
            }
        } );

        for ( Iterator i = sortedEntries.iterator(); i.hasNext(); )
        {
            ChangeSet entry = (ChangeSet) i.next();

            doChangedSetDetail( entry, bundle, sink );
        }

        sink.table_();
    }

    /**
     * reports on the details of an SCM entry log
     *
     * @param entry  an SCM entry to generate the report from
     * @param bundle the resource bundle to retrieve report phrases from
     * @param sink   the report formatting tool
     */
    private void doChangedSetDetail( ChangeSet entry, ResourceBundle bundle, Sink sink )
    {
        sink.tableRow();

        sink.tableCell();
        sink.text( entry.getDateFormatted() + " " + entry.getTimeFormatted() );
        sink.tableCell_();

        sink.tableCell();

        sinkAuthorDetails( sink, entry.getAuthor() );

        sink.tableCell_();

        sink.tableCell();
        //doRevision( entry.getFiles(), bundle, sink );
        doChangedFiles( entry.getFiles(), sink );
        sink.lineBreak();
        StringReader sr = new StringReader( entry.getComment() );
        BufferedReader br = new BufferedReader( sr );
        String line;
        try
        {
            if ( ( issueIDRegexPattern != null && issueIDRegexPattern.length() > 0 )
                && ( issueLinkUrl != null && issueLinkUrl.length() > 0 ) )
            {
                Pattern pattern = Pattern.compile( issueIDRegexPattern );

                line = br.readLine();

                while ( line != null )
                {
                    sinkIssueLink( sink, line, pattern );

                    line = br.readLine();
                    if ( line != null )
                    {
                        sink.lineBreak();
                    }
                }
            }
            else
            {
                line = br.readLine();

                while ( line != null )
                {
                    sink.text( line );
                    line = br.readLine();
                    if ( line != null )
                    {
                        sink.lineBreak();
                    }
                }
            }
        }
        catch ( IOException e )
        {
            getLog().warn( "Unable to read the comment of a ChangeSet as a stream." );
        }
        finally
        {
            if ( br != null )
            {
                try
                {
                    br.close();
                }
                catch ( IOException e )
                {
                    getLog().warn( "Unable to close a reader." );
                }
            }
            if ( sr != null )
            {
                sr.close();
            }
        }
        sink.tableCell_();

        sink.tableRow_();
    }

    private void sinkIssueLink( Sink sink, String line, Pattern pattern )
    {
        // replace any ticket patterns found.

        Matcher matcher = pattern.matcher( line );

        int currLoc = 0;

        while ( matcher.find() )
        {
            String match = matcher.group();

            String link;

            if ( issueLinkUrl.indexOf( ISSUE_TOKEN ) > 0 )
            {
                link = issueLinkUrl.replaceAll( ISSUE_TOKEN, match );
            }
            else
            {
                if ( issueLinkUrl.endsWith( "/" ) )
                {
                    link = issueLinkUrl;
                }
                else
                {
                    link = issueLinkUrl + "/";
                }

                link += match;
            }

            int startOfMatch = matcher.start();

            String unmatchedText = line.substring( currLoc, startOfMatch );

            currLoc = matcher.end();

            sink.text( unmatchedText );

            sink.link( link );
            sink.text( match );
            sink.link_();
        }

        sink.text( line.substring( currLoc ) );
    }

    /**
     * If the supplied author is a known developer this method outputs a
     * link to the team members report, or alternatively, if the supplied
     * author is unknown, outputs the author's name as plain text.
     *
     * @param sink Sink to use for outputting
     * @param author The author's name.
     */
    protected void sinkAuthorDetails( Sink sink, String author )
    {
        Developer developer = (Developer) developersById.get( author );

        if ( developer == null )
        {
            developer = (Developer) developersByName.get( author );
        }

        if ( developer != null )
        {
            sink.link( "team-list.html#" + developer.getId() );
            sink.text( developer.getName() );
            sink.link_();
        }
        else
        {
            sink.text( author );
        }
    }

    /**
     * populates the report url used to create links from certain elements of the report
     */
    protected void initReportUrls()
    {
        if ( scmUrl != null )
        {
            int idx = scmUrl.indexOf( '?' );

            if ( idx > 0 )
            {
                rptRepository = scmUrl.substring( 0, idx );

                if ( scmUrl.equals( displayFileDetailUrl ) )
                {
                    String rptTmpMultiRepoParam = scmUrl.substring( rptRepository.length() );

                    rptOneRepoParam = "?" + rptTmpMultiRepoParam.substring( 1 );

                    rptMultiRepoParam = "&" + rptTmpMultiRepoParam.substring( 1 );
                }
            }
            else
            {
                rptRepository = scmUrl;

                rptOneRepoParam = "";

                rptMultiRepoParam = "";
            }
        }
    }

    /**
     * generates the section of the report listing all the files revisions
     *
     * @param files list of files to generate the reports from
     * @param sink  the report formatting tool
     */
    private void doChangedFiles( List files, Sink sink )
    {
        for ( Iterator i = files.iterator(); i.hasNext(); )
        {
            ChangeFile file = (ChangeFile) i.next();
            sinkLogFile( sink, file.getName(), file.getRevision() );
        }
    }

    /**
     * generates the section of the report detailing the revisions made and the files changed
     *
     * @param sink     the report formatting tool
     * @param name     filename of the changed file
     * @param revision the revision code for this file
     */
    private void sinkLogFile( Sink sink, String name, String revision )
    {
        try
        {
            String connection = getConnection();

            generateLinks( connection, name, revision, sink );
        }
        catch ( Exception e )
        {
            getLog().debug( e );

            sink.text( name + " v " + revision );
        }

        sink.lineBreak();
    }

    /**
     * attaches the http links from the changed files
     *
     * @param connection the string used to connect to the SCM
     * @param name       filename of the file that was changed
     * @param sink       the report formatting tool
     */
    protected void generateLinks( String connection, String name, Sink sink )
    {
        generateLinks( connection, name, null, sink );
    }

    /**
     * attaches the http links from the changed files
     *
     * @param connection the string used to connect to the SCM
     * @param name       filename of the file that was changed
     * @param revision   the revision code
     * @param sink       the report formatting tool
     */
    protected void generateLinks( String connection, String name, String revision, Sink sink )
    {
        String linkFile = null;
        String linkRev = null;

        if ( revision != null )
        {
            linkFile = displayFileRevDetailUrl;
        }
        else
        {
            linkFile = displayFileDetailUrl;
        }

        if ( linkFile != null )
        {
            if ( !scmUrl.equals( linkFile ) )
            {
                // Use the given URL to create links to the files

                if ( linkFile.indexOf( FILE_TOKEN ) > 0 )
                {
                    linkFile = linkFile.replaceAll( FILE_TOKEN, name );
                }
                else
                {
                    // This is here so that we are backwards compatible with the
                    // format used before the special token was introduced

                    linkFile = linkFile + name;
                }

                // Use the given URL to create links to the files

                if (  revision != null && linkFile.indexOf( REV_TOKEN ) > 0 )
                {
                    linkFile = linkFile.replaceAll( REV_TOKEN, revision );
                }
            }
            else if ( connection.startsWith( "scm:perforce" ) )
            {
                String path = getAbsolutePath( displayFileDetailUrl, name );
                linkFile = path + "?ac=22";
                if ( revision != null )
                {
                    linkRev = path + "?ac=64&rev=" + revision;
                }
            }
            else if ( connection.startsWith( "scm:clearcase" ) )
            {
                String path = getAbsolutePath( displayFileDetailUrl, name );
                linkFile = path + rptOneRepoParam;
            }
            else if ( connection.indexOf( "cvsmonitor.pl" ) > 0 )
            {
                String module = rptOneRepoParam.replaceAll( "^.*(&amp;module=.*?(?:&amp;|$)).*$", "$1" );
                linkFile = displayFileDetailUrl + "?cmd=viewBrowseFile" + module + "&file=" + name;
                if ( revision != null )
                {
                    linkRev =
                        rptRepository + "?cmd=viewBrowseVersion" + module + "&file=" + name + "&version=" + revision;
                }
            }
            else
            {
                String path = getAbsolutePath( displayFileDetailUrl, name );
                linkFile = path + rptOneRepoParam;
                if ( revision != null )
                {
                    linkRev = path + "?rev=" + revision + "&content-type=text/vnd.viewcvs-markup" + rptMultiRepoParam;
                }
            }
        }

        if ( linkFile != null )
        {
            sink.link( linkFile );
            sinkFileName( name, sink );
            sink.link_();
        }
        else
        {
            sinkFileName( name, sink );
        }

        sink.text( " " );

        if ( linkRev == null && revision != null && displayChangeSetDetailUrl != null )
        {
            if ( displayChangeSetDetailUrl.indexOf( REV_TOKEN ) > 0 )
            {
                linkRev = displayChangeSetDetailUrl.replaceAll( REV_TOKEN, revision );
            }
            else
            {
                linkRev = displayChangeSetDetailUrl + revision;
            }
        }

        if ( linkRev != null )
        {
            sink.link( linkRev );
            sink.text( "v " + revision );
            sink.link_();
        }
        else if ( revision != null )
        {
            sink.text( "v " + revision );
        }
    }

    /**
     * Encapsulates the logic for rendering the name with a bolded markup.
     *
     * @param name filename of the file that was changed
     * @param sink the report formatting tool
     */
    private void sinkFileName( String name, Sink sink )
    {
        name = name.replaceAll( "\\\\", "/" );
        int pos = name.lastIndexOf( '/' );

        String head;
        String tail;
        if ( pos < 0 )
        {
            head = "";
            tail = name;
        }
        else
        {
            head = name.substring( 0, pos ) + "/";
            tail = name.substring( pos + 1 );
        }

        sink.text( head );
        sink.bold();
        sink.text( tail );
        sink.bold_();
    }

    /**
     * calculates the path from a base directory to a target file
     *
     * @param base   base directory to create the absolute path from
     * @param target target file to create the absolute path to
     */
    private String getAbsolutePath( final String base, final String target )
    {
        String absPath = "";

        StringTokenizer baseTokens = new StringTokenizer( base.replaceAll( "\\\\", "/" ), "/", true );

        StringTokenizer targetTokens = new StringTokenizer( target.replaceAll( "\\\\", "/" ), "/" );

        String targetRoot = targetTokens.nextToken();

        while ( baseTokens.hasMoreTokens() )
        {
            String baseToken = baseTokens.nextToken();

            if ( baseToken.equals( targetRoot ) )
            {
                break;
            }

            absPath += baseToken;
        }

        if ( !absPath.endsWith( "/" ) )
        {
            absPath += "/";
        }

        String newTarget = target;
        if ( newTarget.startsWith( "/" ) )
        {
            newTarget = newTarget.substring( 1 );
        }

        return absPath + newTarget;
    }

    /** {@inheritDoc} */
    protected MavenProject getProject()
    {
        return project;
    }

    /** {@inheritDoc} */
    protected String getOutputDirectory()
    {
        if ( !outputDirectory.isAbsolute() )
        {
            outputDirectory = new File( project.getBasedir(), outputDirectory.getPath() );
        }

        return outputDirectory.getAbsolutePath();
    }

    /**
     * Gets the effective reporting output files encoding.
     *
     * @return The effective reporting output file encoding, never <code>null</code>.
     */
    protected String getOutputEncoding()
    {
        return ( outputEncoding != null ) ? outputEncoding : ReaderFactory.UTF_8;
    }

    /** {@inheritDoc} */
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /** {@inheritDoc} */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.changelog.description" );
    }

    /** {@inheritDoc} */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.changelog.name" );
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "changelog";
    }

    /**
     * @param locale
     * @return the current bundle
     */
    protected ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "scm-activity", locale, this.getClass().getClassLoader() );
    }

    /** {@inheritDoc} */
    public boolean canGenerateReport()
    {
        if ( offline && !outputXML.exists() )
        {
            return false;
        }

        return true;
    }
}
