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
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.StringUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
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
import java.util.ResourceBundle;
import java.util.StringTokenizer;

/**
 * Generate a changelog report.
 *
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
     * @parameter expression="${changelog.dateFormat}" default-value="yyyy-MM-dd"
     * @required
     */
    private String dateFormat;

    /**
     * Input dir.  Directory where the sources are located.
     *
     * @parameter expression="${project.build.sourceDirectory}"
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
     * Output encoding for the xml document
     *
     * @parameter expression="${changelog.outputEncoding}" default-value="ISO-8859-1"
     * @required
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
     * @parameter expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required
     * @readonly
     */
    private SiteRenderer siteRenderer;

    /**
     * @parameter expression="${settings.offline}"
     * @required
     * @readonly
     */
    private boolean offline;

    /**
     * @parameter expression="${component.org.apache.maven.scm.manager.ScmManager}"
     * @required
     * @readonly
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
     * <p/>
     * Example:
     * <code>http://checkstyle.cvs.sourceforge.net/checkstyle%FILE%?view=markup</code>
     * </p>
     * <p/>
     * <strong>Note:</strong> If you don't supply the token in your template,
     * the path of the file will simply be appended to your template URL.
     * </p>
     *
     * @parameter expression="${project.scm.url}"
     */
    protected String displayFileDetailUrl;

    // temporary field holder while generating the report
    private String rptRepository, rptOneRepoParam, rptMultiRepoParam;

    // field for SCM Connection URL
    private String connection;

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        //check if sources exists <-- required for parent poms
        if ( !basedir.exists() )
        {
            doGenerateEmptyReport( getBundle( locale ), getSink() );

            return;
        }

        verifySCMTypeParams();

        doGenerateReport( getChangedSets(), getBundle( locale ), getSink() );
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
            if ( outputXMLExpiration > 0 &&
                outputXMLExpiration * 60000 > System.currentTimeMillis() - outputXML.lastModified() )
            {
                try
                {
                    FileInputStream fIn = new FileInputStream( outputXML );

                    getLog().info( "Using existing changelog.xml..." );

                    changelogList = ChangeLog.loadChangedSets( fIn );
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
        }

        return changelogList;
    }

    private void writeChangelogXml( List changelogList )
        throws FileNotFoundException
    {
        StringBuffer changelogXml = new StringBuffer();

        changelogXml.append( "<?xml version=\"1.0\" encoding=\"" ).append( outputEncoding ).append( "\"?>\n" );
        changelogXml.append( "<changelog>" );

        for ( Iterator sets = changelogList.iterator(); sets.hasNext(); )
        {
            changelogXml.append( "\n  " );

            ChangeLogSet changelogSet = (ChangeLogSet) sets.next();
            String changeset = changelogSet.toXML( outputEncoding );

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

        PrintWriter pw = new PrintWriter( new BufferedOutputStream( new FileOutputStream( outputXML ) ) );
        pw.write( changelogXml.toString() );
        pw.flush();
        pw.close();
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
                    result = provider.changeLog( repository, new ScmFileSet( basedir ), startTag, endTag );

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
            throw new MavenReportException( "An error is occurred during changelog command : ", e );
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
                range = 30;
            }
        }
        else if ( "date".equals( type ) )
        {
            if ( dates == null )
            {
                throw new MavenReportException(
                    "The dates parameter is required when type=\"date\". The value should be the absolute date for the start of the log." );
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
            throw new MavenReportException( "The type parameter has an invalid value: " + type +
                ".  The value should be \"range\", \"date\", or \"tag\"." );
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
        sink.text( "No sources found to create a report." );
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
        sink.section1();

        sink.sectionTitle2();
        if ( set.getStartDate() == null )
        {
            sink.text( bundle.getString( "report.SetRangeUnknown" ) );
        }
        else if ( set.getEndDate() == null )
        {
            sink.text( bundle.getString( "report.SetRangeSince" ) );
            sink.text( " " + set.getStartDate() );
        }
        else
        {
            sink.text( bundle.getString( "report.SetRangeBetween" ) );
            sink.text( " " + set.getStartDate() + " " + bundle.getString( "report.To" ) + " " + set.getEndDate() );
        }
        sink.sectionTitle2_();

        sink.paragraph();
        sink.text( bundle.getString( "report.TotalCommits" ) );
        sink.text( ": " + set.getChangeSets().size() );
        sink.lineBreak();
        sink.text( bundle.getString( "report.changelog.FilesChanged" ) );
        sink.text( ": " + countFilesChanged( set.getChangeSets() ) );
        sink.paragraph_();

        doChangedSetTable( set.getChangeSets(), bundle, sink );

        sink.section1_();
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
        sink.text( entry.getAuthor() );
        sink.tableCell_();

        sink.tableCell();
        //doRevision( entry.getFiles(), bundle, sink );
        doChangedFiles( entry.getFiles(), sink );
        sink.lineBreak();
        sink.text( entry.getComment() );
        sink.tableCell_();

        sink.tableRow_();
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
        sink.paragraph();

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

        sink.paragraph_();
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

        if ( displayFileDetailUrl != null )
        {
            if ( !scmUrl.equals( displayFileDetailUrl ) )
            {
                // Use the given URL to create links to the files
                if ( displayFileDetailUrl.indexOf( FILE_TOKEN ) > 0 )
                {
                    linkFile = displayFileDetailUrl.replaceAll( FILE_TOKEN, name );
                }
                else
                {
                    // This is here so that we are backwards compatible with the
                    // format used before the special token was introduced
                    linkFile = displayFileDetailUrl + name;
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
            sink.text( name );
            sink.link_();
        }
        else
        {
            sink.text( name );
        }

        sink.text( " " );

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

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        if ( !outputDirectory.isAbsolute() )
        {
            outputDirectory = new File( project.getBasedir(), outputDirectory.getPath() );
        }

        return outputDirectory.getAbsolutePath();
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected SiteRenderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return "Generated change log report from SCM.";
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return "Change Log";
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "changelog";
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    protected ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "scm-activity", locale, this.getClass().getClassLoader() );
    }

    public boolean canGenerateReport()
    {
        if ( offline && !outputXML.exists() )
        {
            return false;
        }
        else
        {
            return true;
        }
    }
}
