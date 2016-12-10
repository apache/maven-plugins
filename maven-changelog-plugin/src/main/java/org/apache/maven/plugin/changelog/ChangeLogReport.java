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

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.model.Developer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
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
import org.apache.maven.scm.command.info.InfoItem;
import org.apache.maven.scm.command.info.InfoScmResult;
import org.apache.maven.scm.log.DefaultLog;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.ScmProviderRepositoryWithHost;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.provider.svn.svnexe.command.info.SvnInfoCommandExpanded;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
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

/**
 * Generate a changelog report.
 *
 * @version $Id$
 */
@Mojo( name = "changelog" )
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

    public static final String DEFAULT_ISSUE_ID_REGEX_PATTERN = "[a-zA-Z]{2,}-\\d+";

    /**
     * Used to specify the format to use for the dates in the headings of the
     * report.
     *
     * @since 2.1
     */
    @Parameter( property = "changelog.headingDateFormat", defaultValue = "yyyy-MM-dd" )
    private String headingDateFormat = "yyyy-MM-dd";

    /**
     * Used to specify whether to build the log using range, tag or date.
     */
    @Parameter( property = "changelog.type", defaultValue = "range", required = true )
    private String type;

    /**
     * Used to specify the number of days of log entries to retrieve.
     */
    @Parameter( property = "changelog.range", defaultValue = "-1" )
    private int range;

    /**
     * Used to specify the absolute date (or list of dates) to start log entries from.
     */
    @Parameter
    private List<String> dates;

    /**
     * Used to specify the tag (or list of tags) to start log entries from.
     */
    @Parameter
    private List<String> tags;

    /**
     * Used to specify the date format of the log entries that are retrieved from your SCM system.
     */
    @Parameter( property = "changelog.dateFormat", defaultValue = "yyyy-MM-dd HH:mm:ss", required = true )
    private String dateFormat;

    /**
     * Input dir. Directory where the files under SCM control are located.
     */
    @Parameter( property = "basedir", required = true )
    private File basedir;

    /**
     * Output file for xml document
     */
    @Parameter( defaultValue = "${project.build.directory}/changelog.xml", required = true )
    private File outputXML;

    /**
     * Allows the user to make changelog regenerate the changelog.xml file for the specified time in minutes.
     */
    @Parameter( property = "outputXMLExpiration", defaultValue = "60", required = true )
    private int outputXMLExpiration;

    /**
     * Comment format string used for interrogating
     * the revision control system.
     * Currently only used by the ClearcaseChangeLogGenerator.
     */
    @Parameter( property = "changelog.commentFormat" )
    private String commentFormat;

    /**
     * The file encoding when writing non-HTML reports.
     */
    @Parameter( property = "changelog.outputEncoding", defaultValue = "${project.reporting.outputEncoding}" )
    private String outputEncoding;

    /**
     * The user name (used by svn and starteam protocol).
     */
    @Parameter( property = "username" )
    private String username;

    /**
     * The user password (used by svn and starteam protocol).
     */
    @Parameter( property = "password" )
    private String password;

    /**
     * The private key (used by java svn).
     */
    @Parameter( property = "privateKey" )
    private String privateKey;

    /**
     * The passphrase (used by java svn).
     */
    @Parameter( property = "passphrase" )
    private String passphrase;

    /**
     * The url of tags base directory (used by svn protocol).
     */
    @Parameter( property = "tagBase" )
    private String tagBase;

    /**
     * The URL to view the scm. Basis for external links from the generated report.
     */
    @Parameter( property = "project.scm.url" )
    protected String scmUrl;

    /**
     * Skip the Changelog report generation.  Most useful on the command line
     * via "-Dchangelog.skip=true".
     *
     * @since 2.3
     */
    @Parameter( property = "changelog.skip", defaultValue = "false" )
    protected boolean skip;

    /**
     * Encodes slashes in file uri. Required for some repository browsers like gitblit
     *
     * @since 2.3
     */
    @Parameter( property = "encodeFileUri", defaultValue = "false" )
    protected boolean encodeFileUri;

    /**
     * List of files to include. Specified as fileset patterns of files to include in the report
     *
     * @since 2.3
     */
    @Parameter
    private String[] includes;

    /**
     * List of files to include. Specified as fileset patterns of files to omit in the report
     *
     * @since 2.3
     */
    @Parameter
    private String[] excludes;


    /**
     * The Maven Project Object
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    /**
     * The directory where the report will be generated
     */
    @Parameter( property = "project.reporting.outputDirectory", required = true, readonly = true )
    private File outputDirectory;

    /**
     */
    @Component
    private Renderer siteRenderer;

    /**
     */
    @Parameter( property = "settings.offline", required = true, readonly = true )
    private boolean offline;

    /**
     */
    @Component
    private ScmManager manager;

    /**
     */
    @Parameter( defaultValue = "${settings}", readonly = true, required = true )
    private Settings settings;

    /**
     * Allows the user to choose which scm connection to use when connecting to the scm.
     * Can either be "connection" or "developerConnection".
     */
    @Parameter( defaultValue = "connection", required = true )
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
     */
    @Parameter( property = "displayFileDetailUrl", defaultValue = "${project.scm.url}" )
    protected String displayFileDetailUrl;

    /**
     * A pattern used to identify 'issue tracker' IDs such as those used by JIRA,
     * Bugzilla and alike in the SCM commit messages. Any matched patterns
     * are replaced with <code>issueLinkUrl<code> URL. The default
     * value is a JIRA-style issue identification pattern.
     * <p/>
     * <strong>Note:</strong> Default value is [a-zA-Z]{2,}-\d+
     *
     * @since 2.2
     */
    @Parameter( property = "issueIDRegexPattern", defaultValue = DEFAULT_ISSUE_ID_REGEX_PATTERN, required = true )
    private String issueIDRegexPattern;

    /**
     * The issue tracker URL used when replacing any matched <code>issueIDRegexPattern</code>
     * found in the SCM commit messages. The default is URL is the codehaus JIRA
     * URL. If %ISSUE% is found in the URL it is replaced with the matched issue ID,
     * otherwise the matched issue ID is appended to the URL.
     *
     * @since 2.2
     */
    @Parameter( property = "issueLinkUrl", defaultValue = "https://issues.apache.org/jira/browse/%ISSUE%", required = true )
    private String issueLinkUrl;

    /**
     * A template string that is used to create the changeset URL.
     * <p/>
     * If not defined no change set link will be created.
     * <p/>
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
     * @since 2.2
     */
    @Parameter( property = "displayChangeSetDetailUrl" )
    protected String displayChangeSetDetailUrl;

    /**
     * A template string that is used to create the revision aware URL to
     * the file details in a similar fashion to the <code>displayFileDetailUrl</code>.
     * When a report contains both file and file revision information, as in the
     * Change Log report, this template string can be used to create a revision
     * aware URL to the file details.
     * <p/>
     * If not defined this template string defaults to the same value as the
     * <code>displayFileDetailUrl</code> and thus revision number aware links will
     * not be used.
     * <p/>
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
     * @since 2.2
     */
    @Parameter( property = "displayFileRevDetailUrl" )
    protected String displayFileRevDetailUrl;

    /**
     * List of developers to be shown on the report.
     *
     * @since 2.2
     */
    @Parameter( property = "project.developers" )
    protected List<Developer> developers;

    /**
     * List of provider implementations.
     */
    @Parameter
    private Map<String, String> providerImplementations;

    // temporary field holder while generating the report
    private String rptRepository, rptOneRepoParam, rptMultiRepoParam;

    // field for SCM Connection URL
    private String connection;

    // field used to hold a map of the developers by Id
    private HashMap<String, Developer> developersById;

    // field used to hold a map of the developers by Name
    private HashMap<String, Developer> developersByName;

    /**
     * The system properties to use (needed by the perforce scm provider).
     */
    @Parameter
    private Properties systemProperties;

    private final Pattern sinkFileNamePattern = Pattern.compile( "\\\\" );

    /**
     * {@inheritDoc}
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

        if ( providerImplementations != null )
        {
            for ( Map.Entry<String, String> entry : providerImplementations.entrySet() )
            {
                String providerType = entry.getKey();
                String providerImplementation = entry.getValue();
                getLog().info(
                    "Change the default '" + providerType + "' provider implementation to '" + providerImplementation
                        + "'." );
                manager.setScmProviderImplementation( providerType, providerImplementation );
            }
        }

        initializeDefaultConfigurationParameters();

        initializeDeveloperMaps();

        verifySCMTypeParams();

        if ( systemProperties != null )
        {
            // Add all system properties configured by the user
            Iterator<?> iter = systemProperties.keySet().iterator();

            while ( iter.hasNext() )
            {
                String key = (String) iter.next();

                String value = systemProperties.getProperty( key );

                System.setProperty( key, value );

                getLog().debug( "Setting system property: " + key + '=' + value );
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
        developersById = new HashMap<String, Developer>();
        developersByName = new HashMap<String, Developer>();

        if ( developers != null )
        {
            for ( Developer developer : developers )
            {
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
    protected List<ChangeLogSet> getChangedSets()
        throws MavenReportException
    {
        List<ChangeLogSet> changelogList = null;

        if ( !outputXML.isAbsolute() )
        {
            outputXML = new File( project.getBasedir(), outputXML.getPath() );
        }

        if ( outputXML.exists() )
        {
            // CHECKSTYLE_OFF: MagicNumber
            if ( outputXMLExpiration > 0
                && outputXMLExpiration * 60000 > System.currentTimeMillis() - outputXML.lastModified() )
                // CHECKSTYLE_ON: MagicNumber
            {
                try
                {
                    //ReaderFactory.newReader( outputXML, getOutputEncoding() );
                    //FileInputStream fIn = new FileInputStream( outputXML );

                    getLog().info( "Using existing changelog.xml..." );

                    changelogList =
                        ChangeLog.loadChangedSets( ReaderFactory.newReader( outputXML, getOutputEncoding() ) );
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

    private void writeChangelogXml( List<ChangeLogSet> changelogList )
        throws IOException
    {
        StringBuilder changelogXml = new StringBuilder();

        changelogXml.append( "<?xml version=\"1.0\" encoding=\"" ).append( getOutputEncoding() ).append( "\"?>\n" );
        changelogXml.append( "<changelog>" );

        for ( ChangeLogSet changelogSet : changelogList )
        {
            changelogXml.append( "\n  " );

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
        Writer writer = WriterFactory.newWriter( new BufferedOutputStream( new FileOutputStream( outputXML ) ),
                                                 getOutputEncoding() );
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
    protected List<ChangeLogSet> generateChangeSetsFromSCM()
        throws MavenReportException
    {
        try
        {
            List<ChangeLogSet> changeSets = new ArrayList<ChangeLogSet>();

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

                Iterator<String> tagsIter = tags.iterator();

                String startTag = tagsIter.next();
                String endTag = null;

                if ( tagsIter.hasNext() )
                {
                    while ( tagsIter.hasNext() )
                    {
                        endTag = tagsIter.next();
                        String endRevision = getRevisionForTag( endTag, repository, provider );
                        String startRevision = getRevisionForTag( startTag, repository, provider );
                        result = provider.changeLog( repository, new ScmFileSet( basedir ),
                                                     new ScmRevision( startRevision ),
                                                     new ScmRevision( endRevision ) );

                        checkResult( result );
                        result.getChangeLog().setStartVersion( new ScmRevision( startTag ) );
                        result.getChangeLog().setEndVersion( new ScmRevision( endTag ) );

                        changeSets.add( result.getChangeLog() );

                        startTag = endTag;
                    }
                }
                else
                {
                    String startRevision = getRevisionForTag( startTag, repository, provider );
                    String endRevision = getRevisionForTag( endTag, repository, provider );
                    result = provider.changeLog( repository, new ScmFileSet( basedir ),
                                                 new ScmRevision( startRevision ),
                                                 new ScmRevision( endRevision ) );

                    checkResult( result );
                    result.getChangeLog().setStartVersion( new ScmRevision( startTag ) );
                    result.getChangeLog().setEndVersion( null );
                    changeSets.add( result.getChangeLog() );
                }
            }
            else if ( "date".equals( type ) )
            {
                Iterator<String> dateIter = dates.iterator();

                String startDate = dateIter.next();
                String endDate = null;

                if ( dateIter.hasNext() )
                {
                    while ( dateIter.hasNext() )
                    {
                        endDate = dateIter.next();

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
            filter( changeSets );
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
     * Resolves the given tag to the revision number.
     *
     * @param tag
     * @param repository
     * @param provider
     * @return
     * @throws ScmException
     */
    private String getRevisionForTag( final String tag, final ScmRepository repository, final ScmProvider provider )
        throws ScmException
    {
        if ( repository.getProvider().equals( "svn" ) )
        {
            if ( tag == null )
            {
                return "HEAD";
            }
            SvnInfoCommandExpanded infoCommand = new SvnInfoCommandExpanded();
            infoCommand.setLogger( new DefaultLog() );

            InfoScmResult infoScmResult =
                infoCommand.executeInfoTagCommand( (SvnScmProviderRepository) repository.getProviderRepository(),
                                                   new ScmFileSet( basedir ), tag, null, false, null );
            if ( infoScmResult.getInfoItems().size() == 0 )
            {
                throw new ScmException( "There is no tag named '" + tag + "' in the Subversion repository." );
            }
            InfoItem infoItem = infoScmResult.getInfoItems().get( 0 );
            String revision = infoItem.getLastChangedRevision();
            getLog().info( String.format( "Resolved tag '%s' to revision '%s'", tag, revision ) );
            return revision;
        }
        return tag;
    }

    /**
     * filters out unwanted files from the changesets
     */
    private void filter( List<ChangeLogSet> changeSets )
    {
        List<Pattern> include = compilePatterns( includes );
        List<Pattern> exclude = compilePatterns( excludes );
        if ( includes == null && excludes == null )
        {
            return;
        }
        for ( ChangeLogSet changeLogSet : changeSets )
        {
            List<ChangeSet> set = changeLogSet.getChangeSets();
            filter( set, include, exclude );
        }

    }

    private List<Pattern> compilePatterns( String[] patternArray )
    {
        if ( patternArray == null )
        {
            return new ArrayList<Pattern>();
        }
        List<Pattern> patterns = new ArrayList<Pattern>( patternArray.length );
        for ( String string : patternArray )
        {
            //replaces * with [/\]* (everything but file seperators)
            //replaces ** with .*
            //quotes the rest of the string
            string = "\\Q" + string + "\\E";
            string = string.replace( "**", "\\E.?REPLACEMENT?\\Q" );
            string = string.replace( "*", "\\E[^/\\\\]?REPLACEMENT?\\Q" );
            string = string.replace( "?REPLACEMENT?", "*" );
            string = string.replace( "\\Q\\E", "" );
            patterns.add( Pattern.compile( string ) );
        }
        return patterns;
    }

    private void filter( List<ChangeSet> sets, List<Pattern> includes, List<Pattern> excludes )
    {
        Iterator<ChangeSet> it = sets.iterator();
        while ( it.hasNext() )
        {
            ChangeSet changeSet = it.next();
            List<ChangeFile> files = changeSet.getFiles();
            Iterator<ChangeFile> iterator = files.iterator();
            while ( iterator.hasNext() )
            {
                ChangeFile changeFile = iterator.next();
                String name = changeFile.getName();
                if ( !isIncluded( includes, name ) || isExcluded( excludes, name ) )
                {
                    iterator.remove();
                }
            }
            if ( files.isEmpty() )
            {
                it.remove();
            }
        }
    }

    private boolean isExcluded( List<Pattern> excludes, String name )
    {
        if ( excludes == null || excludes.isEmpty() )
        {
            return false;
        }
        for ( Pattern pattern : excludes )
        {
            if ( pattern.matcher( name ).matches() )
            {
                return true;
            }
        }
        return false;
    }

    private boolean isIncluded( List<Pattern> includes, String name )
    {
        if ( includes == null || includes.isEmpty() )
        {
            return true;
        }
        for ( Pattern pattern : includes )
        {
            if ( pattern.matcher( name ).matches() )
            {
                return true;
            }
        }
        return false;
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
                throw new MavenReportException( "The dates parameter is required when type=\"date\". The value "
                                                    + "should be the absolute date for the start of the log." );
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
    protected void doGenerateReport( List<ChangeLogSet> changeLogSets, ResourceBundle bundle, Sink sink )
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

        for ( ChangeLogSet changeLogSet : changeLogSets )
        {
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
    private void doSummarySection( List<ChangeLogSet> changeLogSets, ResourceBundle bundle, Sink sink )
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
                    sink.text( ' ' + bundle.getString( "report.SetTagUntil" ) + " '" + set.getEndVersion() + '\'' );
                }
            }
            else if ( set.getEndVersion() == null || set.getEndVersion().getName() == null )
            {
                sink.text( bundle.getString( "report.SetTagSince" ) );
                sink.text( " '" + set.getStartVersion() + '\'' );
            }
            else
            {
                sink.text( bundle.getString( "report.SetTagBetween" ) );
                sink.text(
                    " '" + set.getStartVersion() + "' " + bundle.getString( "report.And" ) + " '" + set.getEndVersion()
                        + '\'' );
            }
        }
        else if ( set.getStartDate() == null )
        {
            sink.text( bundle.getString( "report.SetRangeUnknown" ) );
        }
        else if ( set.getEndDate() == null )
        {
            sink.text( bundle.getString( "report.SetRangeSince" ) );
            sink.text( ' ' + headingDateFormater.format( set.getStartDate() ) );
        }
        else
        {
            sink.text( bundle.getString( "report.SetRangeBetween" ) );
            sink.text(
                ' ' + headingDateFormater.format( set.getStartDate() ) + ' ' + bundle.getString( "report.And" ) + ' '
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
    protected long countFilesChanged( Collection<ChangeSet> entries )
    {
        if ( entries == null )
        {
            return 0;
        }

        if ( entries.isEmpty() )
        {
            return 0;
        }

        HashMap<String, List<ChangeFile>> fileList = new HashMap<String, List<ChangeFile>>();

        for ( ChangeSet entry : entries )
        {
            for ( ChangeFile file : entry.getFiles() )
            {
                String name = file.getName();
                List<ChangeFile> list = fileList.get( name );

                if ( list != null )
                {
                    list.add( file );
                }
                else
                {
                    list = new LinkedList<ChangeFile>();

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
    private void doChangedSetTable( Collection<ChangeSet> entries, ResourceBundle bundle, Sink sink )
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

        List<ChangeSet> sortedEntries = new ArrayList<ChangeSet>( entries );
        Collections.sort( sortedEntries, new Comparator<ChangeSet>()
        {
            public int compare( ChangeSet changeSet0, ChangeSet changeSet1 )
            {
                return changeSet1.getDate().compareTo( changeSet0.getDate() );
            }
        } );

        for ( ChangeSet entry : sortedEntries )
        {
            doChangedSetDetail( entry, sink );
        }

        sink.table_();
    }

    /**
     * reports on the details of an SCM entry log
     *
     * @param entry an SCM entry to generate the report from
     * @param sink  the report formatting tool
     */
    private void doChangedSetDetail( ChangeSet entry, Sink sink )
    {
        sink.tableRow();

        sink.tableCell();
        sink.text( entry.getDateFormatted() + ' ' + entry.getTimeFormatted() );
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
            if ( ( issueIDRegexPattern != null && issueIDRegexPattern.length() > 0 ) && ( issueLinkUrl != null
                && issueLinkUrl.length() > 0 ) )
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
            try
            {
                br.close();
            }
            catch ( IOException e )
            {
                getLog().warn( "Unable to close a reader." );
            }
            sr.close();
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
                    link = issueLinkUrl + '/';
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
     * @param sink   Sink to use for outputting
     * @param author The author's name.
     */
    protected void sinkAuthorDetails( Sink sink, String author )
    {
        Developer developer = developersById.get( author );

        if ( developer == null )
        {
            developer = developersByName.get( author );
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

                    rptOneRepoParam = '?' + rptTmpMultiRepoParam.substring( 1 );

                    rptMultiRepoParam = '&' + rptTmpMultiRepoParam.substring( 1 );
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
    private void doChangedFiles( List<ChangeFile> files, Sink sink )
    {
        for ( ChangeFile file : files )
        {
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
        String linkFile;
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
            if ( !linkFile.equals( scmUrl ) )
            {
                String linkName = name;
                if ( encodeFileUri )
                {
                    try
                    {
                        linkName = URLEncoder.encode( linkName, "UTF-8" );
                    }
                    catch ( UnsupportedEncodingException e )
                    {
                        // UTF-8 is always supported
                    }
                }

                // Use the given URL to create links to the files

                if ( linkFile.indexOf( FILE_TOKEN ) > 0 )
                {
                    linkFile = linkFile.replaceAll( FILE_TOKEN, linkName );
                }
                else
                {
                    // This is here so that we are backwards compatible with the
                    // format used before the special token was introduced

                    linkFile += linkName;
                }

                // Use the given URL to create links to the files

                if ( revision != null && linkFile.indexOf( REV_TOKEN ) > 0 )
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
                Pattern cvsMonitorRegex = Pattern.compile( "^.*(&amp;module=.*?(?:&amp;|$)).*$" );
                String module = cvsMonitorRegex.matcher( rptOneRepoParam ).replaceAll( "$1" );
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
        name = sinkFileNamePattern.matcher( name ).replaceAll( "/" );
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
            head = name.substring( 0, pos ) + '/';
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

        StringTokenizer baseTokens =
            new StringTokenizer( sinkFileNamePattern.matcher( base ).replaceAll( "/" ), "/", true );

        StringTokenizer targetTokens =
            new StringTokenizer( sinkFileNamePattern.matcher( target ).replaceAll( "/" ), "/" );

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
     * {@inheritDoc}
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * {@inheritDoc}
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
     * Gets the effective reporting output files encoding.
     *
     * @return The effective reporting output file encoding, never <code>null</code>.
     */
    protected String getOutputEncoding()
    {
        return ( outputEncoding != null ) ? outputEncoding : ReaderFactory.UTF_8;
    }

    /**
     * {@inheritDoc}
     */
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.changelog.description" );
    }

    /**
     * {@inheritDoc}
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.changelog.name" );
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    public boolean canGenerateReport()
    {
        if ( offline && !outputXML.exists() )
        {
            return false;
        }

        return !skip;

    }
}
