package org.apache.maven.plugin.javadoc;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;

/**
 * Generates documentation for the Java code in the project using the standard
 * <a href="http://java.sun.com/j2se/javadoc/">Javadoc Tool</a> tool.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal javadoc
 * @requiresDependencyResolution compile
 * @phase generate-sources
 * @see <a href="http://java.sun.com/j2se/javadoc/">Javadoc Tool</a>
 * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#options">Javadoc Options </a>
 */
public class JavadocReport
    extends AbstractMavenReport
{
    /**
     * The current class directory
     */
    private static final String RESOURCE_DIR = ClassUtils.getPackageName( JavadocReport.class ).replace( '.', '/' );

    /**
     * Default location for css
     */
    private static final String DEFAULT_CSS_NAME = "stylesheet.css";

    private static final String RESOURCE_CSS_DIR = RESOURCE_DIR + "/css";

    // Using for the plugin:xdoc goal. Best way?

    private static final String PATH_SEPARATOR = System.getProperty( "path.separator" );

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * @parameter default-value="${settings.offline}"
     * @required
     * @readonly
     */
    private boolean isOffline;

    /**
     * Specifies the destination directory where javadoc saves the generated HTML files.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#d">d</a>.
     *
     * @parameter expression="${destDir}" alias="destDir" default-value="${project.build.directory}/javadoc/"
     * @required
     */
    private File outputDirectory;

    /**
     * @component
     */
    private SiteRenderer siteRenderer;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    // JavaDoc Options
    // @see http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#options

    /**
     * Set an additional parameter(s) on the command line.  This value should include quotes as necessary for parameters
     * that include spaces.
     *
     * @parameter expression="${additionalparam}"
     */
    private String additionalparam;

    /**
     * Uses the sentence break iterator to determine the end of the first sentence.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#breakiterator">breakiterator</a>.
     *
     * @parameter expression="${breakiterator}" default-value="false"
     */
    private boolean breakiterator = false;

    /**
     * Specifies the class file that starts the doclet used in generating the documentation.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#doclet">doclet</a>.
     *
     * @parameter expression="${doclet}"
     */
    private String doclet;

    /**
     * Specifies the path to the doclet starting class file (specified with the -doclet option) and any jar files it depends on.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#docletpath">docletpath</a>.
     *
     * @parameter expression="${docletPath}"
     */
    private String docletPath;

    /**
     * Specifies the encoding name of the source files.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#encoding">encoding</a>.
     *
     * @parameter expression="${encoding}"
     */
    private String encoding;

    /**
     * Unconditionally excludes the specified packages and their subpackages from the list formed by -subpackages.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#exclude">exclude</a>.
     *
     * @parameter expression="${excludePackageNames}"
     */
    private String excludePackageNames;

    /**
     * Specifies the directories where extension classes reside.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#extdirs">extdirs</a>.
     *
     * @parameter expression="${extdirs}"
     */
    private String extdirs;

    /**
     * Specifies the locale that javadoc uses when generating documentation.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#locale">locale</a>.
     *
     * @parameter expression="${locale}"
     */
    private String locale;

    /**
     * Specifies the maximum Java heap size to be used when launching the javadoc executable.
     * Some JVMs refer to this property as the -Xmx parameter. Example: '512' or '512m'.
     *
     * @parameter expression="${maxmemory}"
     */
    private String maxmemory;

    /**
     * Specifies the minimum Java heap size to be used when launching the javadoc executable.
     * Some JVMs refer to this property as the -Xms parameter. Example: '128' or '128m'.
     *
     * @parameter expression="${minmemory}"
     */
    private String minmemory;

    /**
     * This option created documentation with the appearance and functionality of documentation generated by Javadoc 1.1.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#1.1">1.1</a>.
     *
     * @parameter expression="${old}" default-value="false"
     */
    private boolean old = false;

    /**
     * Specifies that javadoc should retrieve the text for the overview documentation from the "source" file specified by path/filename and place it on the Overview page (overview-summary.html).
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#overview">overview</a>.
     *
     * @parameter expression="${overview}"
     */
    private String overview;

    /**
     * Specifies the access level for classes and members to show in the Javadocs.
     * Possible values are: 
     * <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#public">public</a>
     *      (shows only public classes and members),
     * <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#protected">protected</a>
     *      (shows only public and protected classes and members),
     * <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#package">package</a>
     *      (shows all classes and members not marked private), and
     * <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#private">private</a>
     *      (shows all classes and members).
     * 
     * @parameter expression="${show}" default-value="protected"
     */
    private String show = "protected";

    /**
     * Shuts off non-error and non-warning messages, leaving only the warnings and errors appear, making them easier to view.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#quiet">quiet</a>.
     *
     * @parameter expression="${quiet}" default-value="false"
     */
    private boolean quiet = false;

    /**
     * Necessary to enable javadoc to handle assertions present in J2SE v 1.4 source code.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#source">source</a>.
     *
     * @parameter expression="${source}"
     */
    private String source;

    /**
     * Provides more detailed messages while javadoc is running.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#verbose">verbose</a>.
     *
     * @parameter expression="${verbose}" default-value="false"
     */
    private boolean verbose = false;

    // Options Provided by the Standard Doclet
    // @see http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#standard

    /**
     * Specifies whether or not the author text is included in the generated Javadocs.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#author">author</a>.
     *
     * @parameter expression="${author}" default-value="true"
     */
    private boolean author = true;

    /**
     * Specifies the text to be placed at the bottom of each output file.<br/>
     * If you want to use html you have to put it in a CDATA section, <br/>
     * eg. <code>&lt;![CDATA[Copyright 2005, &lt;a href="http://www.mycompany.com">MyCompany, Inc.&lt;a>]]&gt;</code><br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#bottom">bottom</a>.
     *
     * @parameter expression="${bottom}" default-value="Copyright {inceptionYear}-{currentYear} ${project.organization.name}. All Rights Reserved."
     */
    private String bottom;

    /**
     * Specifies the HTML character set for this document.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#charset">charset</a>.
     *
     * @parameter expression="${charset}" default-value="ISO-8859-1"
     */
    private String charset = "ISO-8859-1";

    /**
     * Enables deep copying of "doc-files" directories.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#docfilessubdirs">docfilessubdirs</a>.
     *
     * @parameter expression="${docfilessubdirs}" default-value="false"
     */
    private boolean docfilessubdirs = false;

    /**
     * Specifies the encoding of the generated HTML files.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#docencoding">docencoding</a>.
     *
     * @parameter expression="${docencoding}"
     */
    private String docencoding;

    /**
     * Specifies the title to be placed near the top of the overview summary file.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#doctitle">doctitle</a>.
     *
     * @parameter expression="${doctitle}" default-value="${project.name} ${project.version} API"
     */
    private String doctitle;

    /**
     * Excludes any "doc-files" subdirectories with the given names.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#excludedocfilessubdir">excludedocfilessubdir</a>.
     *
     * @parameter expression="${excludedocfilessubdir}"
     */
    private String excludedocfilessubdir;

    /**
     * Specifies the footer text to be placed at the bottom of each output file.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#footer">footer</a>.
     *
     * @parameter expression="${footer}"
     */
    private String footer;

    /**
     * Separates packages on the overview page into whatever groups you specify, one group per table.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#group">group</a>.
     * It is a comma separated String.
     *
     * @parameter expression="${group}"
     */
    private String group;

    /**
     * Specifies the header text to be placed at the top of each output file.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#header">header</a>.
     *
     * @parameter expression="${header}"
     */
    private String header;

    /**
     * Specifies the path of an alternate help file path\filename that the HELP link in the top and bottom navigation bars link to.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#helpfile">helpfile</a>.
     *
     * @parameter expression="${helpfile}"
     */
    private String helpfile;

    /**
     * Creates links to existing javadoc-generated documentation of external referenced classes.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#link">link</a>.
     * It is a comma separated String.
     *
     * @parameter expression="${links}"
     */
    private ArrayList links;

    /**
     * This option is a variation of -link; they both create links to javadoc-generated documentation for external referenced classes.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#linkoffline">linkoffline</a>.
     * It is a comma separated String.
     *
     * @parameter expression="${offlineLinks}"
     */
    private ArrayList offlineLinks;

    /**
     * Creates an HTML version of each source file (with line numbers) and adds links to them from the standard HTML documentation.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#linksource">linksource</a>.
     *
     * @parameter expression="${linksource}" default-value="false"
     */
    private boolean linksource = false;

    /**
     * Suppress the entire comment body, including the main description and all tags, generating only declarations.
     * Ssee <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nocomment">nocomment</a>.
     *
     * @parameter expression="${nocomment}" default-value="false"
     */
    private boolean nocomment = false;

    /**
     * Prevents the generation of any deprecated API at all in the documentation.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nodeprecated">nodeprecated</a>.
     *
     * @parameter expression="${nodeprecated}" default-value="false"
     */
    private boolean nodeprecated = false;

    /**
     * Prevents the generation of the file containing the list of deprecated APIs (deprecated-list.html) and the link in the navigation bar to that page.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nodeprecatedlist">nodeprecatedlist</a>.
     *
     * @parameter expression="${nodeprecatedlist}" default-value="false"
     */
    private boolean nodeprecatedlist = false;

    /**
     * Omits the HELP link in the navigation bars at the top and bottom of each page of output.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nohelp">nohelp</a>.
     *
     * @parameter expression="${nohelp}" default-value="false"
     */
    private boolean nohelp = false;

    /**
     * Omits the index from the generated docs.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#noindex">noindex</a>.
     *
     * @parameter expression="${noindex}" default-value="false"
     */
    private boolean noindex = false;

    /**
     * Omits the index from the generated docs. The default value is 'false'.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nonavbar">nonavbar</a>.
     *
     * @parameter expression="${nonavbar}" default-value="false"
     */
    private boolean nonavbar = false;

    /**
     * Omits qualifying package name from ahead of class names in output.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#noqualifier">noqualifier</a>.
     *
     * @parameter expression="${noqualifier}"
     */
    private String noqualifier;

    /**
     * Omits from the generated docs the "Since" sections associated with the since tags.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nosince">nosince</a>.
     *
     * @parameter expression="${nosince}" default-value="false"
     */
    private boolean nosince = false;

    /**
     * Omits the class/interface hierarchy pages from the generated docs.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#notree">notree</a>.
     *
     * @parameter expression="${notree}" default-value="false"
     */
    private boolean notree = false;

    /**
     * Generates compile-time warnings for missing serial tags.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#serialwarn">serialwarn</a>
     *
     * @parameter expression="${serialwarn}" default-value="false"
     */
    private boolean serialwarn = false;

    /**
     * Splits the index file into multiple files, alphabetically, one file per letter, plus a file for any index entries that
     * start with non-alphabetical characters.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#splitindex">splitindex</a>.
     *
     * @parameter expression="${splitindex}" default-value="false"
     */
    private boolean splitindex = false;

    /**
     * Specifies the path of an alternate HTML stylesheet file.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#stylesheetfile">stylesheetfile</a>.
     *
     * @parameter expression="${stylesheetfile}"
     */
    private String stylesheetfile;

    /**
     * Contains a list of tag names, which will result in conversion to support the following, from the javadoc
     * API:
     * <br/>
     * Enables the Javadoc tool to interpret a simple, one-argument custom block tag tagname in doc comments.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#tag">tag</a>.
     * It is a comma separated String.
     *
     * @parameter expression="${tags}"
     */
    private ArrayList tags;

    /**
     * Enables the Javadoc tool to interpret a simple, one-argument custom block tag tagname in doc comments.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#tag">tag</a>.
     * It is a comma separated String.
     *
     * @parameter expression="${tag}"
     */
    private String tag;

    /**
     * Specifies the class file that starts the taglet used in generating the documentation for that tag.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#taglet">taglet</a>.
     *
     * @parameter expression="${taglet}"
     */
    private String taglet;

    /**
     * Specifies the search paths for finding taglet class files (.class).
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#tagletpath">tagletpath</a>.
     *
     * @parameter expression="${tagletpath}"
     */
    private String tagletpath;

    /**
     * Includes one "Use" page for each documented class and package.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#use">use</a>.
     *
     * @parameter expression="${use}" default-value="true"
     */
    private boolean use = true;

    /**
     * Includes the version text in the generated docs.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#version">version</a>.
     *
     * @parameter expression="${version}" default-value="true"
     */
    private boolean version = true;

    /**
     * Specifies the title to be placed in the HTML title tag.
     * The default is '${project.name} ${project.version} API'.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#windowtitle">windowtitle</a>.
     *
     * @parameter expression="${windowtitle}" default-value="${project.name} ${project.version} API"
     */
    private String windowtitle;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return "JavaDocs";
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        // TODO i18n
        return "JavaDoc API documentation.";
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsoluteFile().toString();
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected SiteRenderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#generate(org.codehaus.doxia.sink.Sink, java.util.Locale)
     */
    public void generate( Sink sink, Locale locale )
        throws MavenReportException
    {
        executeReport( locale );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        ArtifactHandler artifactHandler = project.getArtifact().getArtifactHandler();
        if ( !"java".equals( artifactHandler.getLanguage() ) )
        {
            getLog().info( "Not executing Javadoc as the project is not a Java classpath-capable package" );
            return;
        }

        Model model = getProject().getModel();

        StringBuffer options = new StringBuffer();
        StringBuffer classpath = new StringBuffer();
        try
        {
            for ( Iterator i = getProject().getCompileClasspathElements().iterator(); i.hasNext(); )
            {
                classpath.append( (String) i.next() );

                if ( i.hasNext() )
                {
                    classpath.append( PATH_SEPARATOR );
                }
            }
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MavenReportException( "Error in plugin descriptor - compile dependencies were not resolved", e );
        }

        if ( classpath.length() > 0 )
        {
            options.append( "-classpath " );
            options.append( quotedPathArgument( classpath.toString() ) );
        }

        char FILE_SEPARATOR = System.getProperty( "file.separator" ).charAt( 0 );
        String[] excludePackages = {};
        if ( excludePackageNames != null )
        {
            excludePackages = excludePackageNames.split( "[ ,:;]" );
        }
        for ( int i = 0; i < excludePackages.length; i++ )
        {
            excludePackages[i] = excludePackages[i].replace( '.', FILE_SEPARATOR );
        }

        StringBuffer sourcePath = new StringBuffer();
        StringBuffer files = new StringBuffer();
        for ( Iterator i = getProject().getCompileSourceRoots().iterator(); i.hasNext(); )
        {
            String sourceDirectory = (String) i.next();
            String[] fileList = FileUtils.getFilesFromExtension( sourceDirectory, new String[] { "java" } );
            if ( fileList != null && fileList.length != 0 )
            {
                for ( int j = 0; j < fileList.length; j++ )
                {
                    boolean include = true;
                    for ( int k = 0; k < excludePackages.length && include; k++ )
                    {
                        if ( fileList[j].startsWith( sourceDirectory + FILE_SEPARATOR + excludePackages[k] ) )
                        {
                            include = false;
                        }
                    }
                    if ( include )
                    {
                        files.append( quotedPathArgument( fileList[j] ) );
                        files.append( "\n" );
                    }
                }
            }

            sourcePath.append( sourceDirectory );

            if ( i.hasNext() )
            {
                sourcePath.append( PATH_SEPARATOR );
            }
        }

        if ( files.length() == 0 )
        {
            return;
        }

        File javadocDirectory = getReportOutputDirectory();

        if ( !javadocDirectory.getAbsolutePath().equals( getOutputDirectory() ) )
        {
            // we're in site-embedded report mode, so Doxia has set the
            // reportOutputDirectory to the basedir of the site.
            // Append 'apidocs'.
            javadocDirectory = new File( javadocDirectory, "apidocs" );
        }
        javadocDirectory.mkdirs();

        File file = new File( javadocDirectory, "files" );
        file.deleteOnExit();
        try
        {
            FileUtils.fileWrite( file.getAbsolutePath(), files.toString() );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to write temporary file for command execution", e );
        }

        try
        {
            // Copy default style sheet
            copyDefaultStylesheet( javadocDirectory );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to copy default stylesheet", e );
        }

        Commandline cmd = new Commandline();

        if ( !StringUtils.isEmpty( maxmemory ) )
        {
            // Allow '128' or '128m'
            if ( NumberUtils.isDigits( maxmemory ) )
            {
                cmd.createArgument().setValue( "-J-Xmx" + maxmemory + "m" );
            }
            else
            {
                if ( ( NumberUtils.isDigits( maxmemory.substring( 0, maxmemory.length() - 1 ) ) )
                    && ( maxmemory.toLowerCase().endsWith( "m" ) ) )
                {
                    cmd.createArgument().setValue( "-J-Xmx" + maxmemory );
                }
                else
                {
                    getLog().error( "The maxmemory '" + maxmemory + "' is not a valid number. Ignore this option." );
                }
            }
        }

        if ( !StringUtils.isEmpty( minmemory ) )
        {
            // Allow '128' or '128m'
            if ( NumberUtils.isDigits( minmemory ) )
            {
                cmd.createArgument().setValue( "-J-Xms" + minmemory + "m" );
            }
            else
            {
                if ( ( NumberUtils.isDigits( minmemory.substring( 0, minmemory.length() - 1 ) ) )
                    && ( minmemory.toLowerCase().endsWith( "m" ) ) )
                {
                    cmd.createArgument().setValue( "-J-Xms" + minmemory );
                }
                else
                {
                    getLog().error( "The minmemory '" + minmemory + "' is not a valid number. Ignore this option." );
                }
            }
        }

        List arguments = new ArrayList();

        cmd.setWorkingDirectory( javadocDirectory.getAbsolutePath() );
        cmd.setExecutable( getJavadocPath() );

        // General javadoc arguments
        addArgIfNotEmpty( arguments, "-locale", quotedArgument( this.locale ) );
        addArgIf( arguments, breakiterator, "-breakiterator", 1.4f );
        if ( !StringUtils.isEmpty( doclet ) )
        {
            addArgIfNotEmpty( arguments, "-doclet", quotedArgument( doclet ) );
            addArgIfNotEmpty( arguments, "-docletpath", quotedPathArgument( docletPath ) );
        }
        addArgIfNotEmpty( arguments, "-encoding", quotedArgument( encoding ) );
        addArgIfNotEmpty( arguments, "-extdirs", quotedPathArgument( extdirs ) );

        if ( old && SystemUtils.isJavaVersionAtLeast( 1.4f ) )
        {
            getLog().warn( "Javadoc 1.4 doesn't support the -1.1 switch anymore. Ignore this option." );
        }
        else
        {
            addArgIf( arguments, old, "-1.1" );
        }

        final int LEVEL_PUBLIC = 1;
        final int LEVEL_PROTECTED = 2;
        final int LEVEL_PACKAGE = 3;
        final int LEVEL_PRIVATE = 4;
        int accessLevel = 0;
        if ( "public".equalsIgnoreCase( show ) )
        {
            accessLevel = LEVEL_PUBLIC;
        }
        else if ( "protected".equalsIgnoreCase( show ) )
        {
            accessLevel = LEVEL_PROTECTED;
        }
        else if ( "package".equalsIgnoreCase( show ) )
        {
            accessLevel = LEVEL_PACKAGE;
        }
        else if ( "private".equalsIgnoreCase( show ) )
        {
            accessLevel = LEVEL_PRIVATE;
        }
        else
        {
            getLog().error( "Unrecognized access level to show '" + show + "'. Defaulting to protected." );
            accessLevel = LEVEL_PROTECTED;
        }

        addArgIfNotEmpty( arguments, "-overview", quotedPathArgument( overview ) );
        addArgIf( arguments, accessLevel == LEVEL_PUBLIC, "-public" );
        addArgIf( arguments, accessLevel == LEVEL_PROTECTED, "-protected" );
        addArgIf( arguments, accessLevel == LEVEL_PACKAGE, "-package" );
        addArgIf( arguments, accessLevel == LEVEL_PRIVATE, "-private" );
        addArgIf( arguments, quiet, "-quiet", 1.4f );
        addArgIfNotEmpty( arguments, "-source", quotedArgument( source ), 1.4f );
        addArgIf( arguments, verbose, "-verbose" );
        addArgIfNotEmpty( arguments, null, additionalparam );
        addArgIfNotEmpty( arguments, "-sourcepath", quotedPathArgument( sourcePath.toString() ) );

        // javadoc arguments for default doclet
        if ( StringUtils.isEmpty( doclet ) )
        {
            int actualYear = Calendar.getInstance().get( Calendar.YEAR );
            String year = String.valueOf( actualYear );

            bottom = StringUtils.replace( bottom, "{currentYear}", year );

            if ( model != null  && model.getInceptionYear() != null )
            {
                if ( model.getInceptionYear().equals( year ) )
                {
                    bottom = StringUtils.replace( bottom, "{inceptionYear}-", "" );                
                }
                else
                {
                    bottom = StringUtils.replace( bottom, "{inceptionYear}", model.getInceptionYear() );                
                }
            }

            if ( StringUtils.isEmpty( stylesheetfile ) )
            {
                stylesheetfile = javadocDirectory + File.separator + DEFAULT_CSS_NAME;
            }
            // End Specify default values

            addArgIf( arguments, author, "-author" );
            addArgIfNotEmpty( arguments, "-bottom", quotedArgument( bottom ) );
            addArgIf( arguments, breakiterator, "-breakiterator", 1.4f );
            addArgIfNotEmpty( arguments, "-charset", quotedArgument( charset ) );
            addArgIfNotEmpty( arguments, "-d", quotedPathArgument( javadocDirectory.toString() ) );
            addArgIf( arguments, docfilessubdirs, "-docfilessubdirs", 1.4f );
            addArgIfNotEmpty( arguments, "-docencoding", quotedArgument( docencoding ) );
            addArgIfNotEmpty( arguments, "-doctitle", quotedArgument( doctitle ) );
            addArgIfNotEmpty( arguments, "-excludedocfilessubdir", quotedPathArgument( excludedocfilessubdir ), 1.4f );
            addArgIfNotEmpty( arguments, "-footer", quotedArgument( footer ) );
            addArgIfNotEmpty( arguments, "-group", quotedArgument( group ), true );
            addArgIfNotEmpty( arguments, "-header", quotedArgument( header ) );
            addArgIfNotEmpty( arguments, "-helpfile", quotedPathArgument( helpfile ) );

            if ( !isOffline )
            {
                addLinkArguments( arguments );
                addLinkofflineArguments( arguments );
                addArgIf( arguments, linksource, "-linksource", 1.4f );
            }

            addArgIf( arguments, nodeprecated, "-nodeprecated" );
            addArgIf( arguments, nodeprecatedlist, "-nodeprecatedlist" );
            addArgIf( arguments, nocomment, "-nocomment", 1.4f );
            addArgIf( arguments, nohelp, "-nohelp" );
            addArgIf( arguments, noindex, "-noindex" );
            addArgIf( arguments, nonavbar, "-nonavbar" );
            addArgIfNotEmpty( arguments, "-noqualifier", quotedArgument( noqualifier ), 1.4f );
            addArgIf( arguments, nosince, "-nosince" );
            addArgIf( arguments, notree, "-notree" );
            addArgIf( arguments, serialwarn, "-serialwarn" );
            addArgIf( arguments, splitindex, "-splitindex" );
            addArgIfNotEmpty( arguments, "-stylesheetfile", quotedPathArgument( stylesheetfile ) );

            addArgIfNotEmpty( arguments, "-tag", quotedArgument( tag ), 1.4f, true );

            if ( tags != null && !tags.isEmpty() )
            {
                for ( Iterator it = tags.iterator(); it.hasNext(); )
                {
                    String tag = (String) it.next();

                    addArgIfNotEmpty( arguments, "-tag", quotedArgument( tag ), 1.4f, true );
                }
            }

            addArgIfNotEmpty( arguments, "-taglet", quotedArgument( taglet ), 1.4f );
            addArgIfNotEmpty( arguments, "-tagletpath", quotedPathArgument( tagletpath ), 1.4f );
            addArgIf( arguments, use, "-use" );
            addArgIf( arguments, version, "-version" );
            addArgIfNotEmpty( arguments, "-windowtitle", quotedArgument( windowtitle ) );
        }

        if ( options.length() > 0 )
        {
            File optionsFile = new File( javadocDirectory, "options" );
            for ( Iterator it = arguments.iterator(); it.hasNext(); )
            {
                options.append( " " );
                options.append( (String) it.next() );
            }
            try
            {
                FileUtils.fileWrite( optionsFile.getAbsolutePath(), options.toString() );
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Unable to write temporary file for command execution", e );
            }
            cmd.createArgument().setValue( "@options" );
            optionsFile.deleteOnExit();
        }

        cmd.createArgument().setValue( "@files" );

        getLog().debug( Commandline.toString( cmd.getCommandline() ) );

        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
        try
        {
            int exitCode = CommandLineUtils.executeCommandLine( cmd, new DefaultConsumer(), err );

            if ( exitCode != 0 )
            {
                throw new MavenReportException( "Exit code: " + exitCode + " - " + err.getOutput() );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MavenReportException( "Unable to execute javadoc command", e );
        }
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "apidocs/index";
    }

    /**
     * Get the path of Javadoc tool depending the OS.
     *
     * @return the path of the Javadoc tool
     */
    private String getJavadocPath()
    {
        String javadocCommand = "javadoc" + ( SystemUtils.IS_OS_WINDOWS ? ".exe" : "" );

        File javadocExe;

        // For IBM's JDK 1.2
        if ( SystemUtils.IS_OS_AIX )
        {
            javadocExe = new File( SystemUtils.getJavaHome() + "/../sh", javadocCommand );
        }
        else if ( SystemUtils.IS_OS_MAC_OSX )
        {
            javadocExe = new File( SystemUtils.getJavaHome() + "/bin", javadocCommand );
        }
        else
        {
            javadocExe = new File( SystemUtils.getJavaHome() + "/../bin", javadocCommand );
        }

        getLog().debug( "Javadoc executable=[" + javadocExe.getAbsolutePath() + "]" );

        return javadocExe.getAbsolutePath();
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * conditionally based on the given flag.
     *
     * @param arguments
     * @param b         the flag which controls if the argument is added or not.
     * @param value     the argument value to be added.
     */
    private void addArgIf( List arguments, boolean b, String value )
    {
        if ( b )
        {
            arguments.add( value );
        }
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * regarding the requested Java version.
     *
     * @param b                   the flag which controls if the argument is added or not.
     * @param value               the argument value to be added.
     * @param requiredJavaVersion the required Java version, for example 1.31f or 1.4f
     * @see #addArgIf(java.util.List,boolean,String)
     * @see <a href="http://jakarta.apache.org/commons/lang/api/org/apache/commons/lang/SystemUtils.html#isJavaVersionAtLeast(float)">SystemUtils.html#isJavaVersionAtLeast(float)</a>
     */
    private void addArgIf( List arguments, boolean b, String value, float requiredJavaVersion )
    {
        if ( SystemUtils.isJavaVersionAtLeast( requiredJavaVersion ) )
        {
            addArgIf( arguments, b, value );
        }
        else
        {
            getLog().warn( value + " option is not supported on Java version < " + requiredJavaVersion );
        }
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * if the the value is not null or empty.
     * <p/>
     * Moreover, the value could be comma separated.
     *
     * @param arguments
     * @param key       the argument name.
     * @param value     the argument value to be added.
     * @see #addArgIfNotEmpty(java.util.List,String,String,boolean)
     */
    private void addArgIfNotEmpty( List arguments, String key, String value )
    {
        addArgIfNotEmpty( arguments, key, value, false );
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * if the the value is not null or empty.
     * <p/>
     * Moreover, the value could be comma separated.
     *
     * @param arguments
     * @param key       the argument name.
     * @param value     the argument value to be added.
     * @param repeatKey repeat or not the key in the command line
     */
    private void addArgIfNotEmpty( List arguments, String key, String value, boolean repeatKey )
    {
        if ( !StringUtils.isEmpty( value ) )
        {
            if ( !StringUtils.isEmpty( key ) )
            {
                arguments.add( key );
            }

            StringTokenizer token = new StringTokenizer( value, "," );
            while ( token.hasMoreTokens() )
            {
                String current = token.nextToken().trim();

                if ( !StringUtils.isEmpty( current ) )
                {
                    arguments.add( current );

                    if ( token.hasMoreTokens() && repeatKey )
                    {
                        arguments.add( key );
                    }
                }
            }
        }
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * regarding the requested Java version.
     *
     * @param arguments
     * @param key                 the argument name.
     * @param value               the argument value to be added.
     * @param requiredJavaVersion the required Java version, for example 1.31f or 1.4f
     * @see #addArgIfNotEmpty(List, String, String, float, boolean)
     */
    private void addArgIfNotEmpty( List arguments, String key, String value, float requiredJavaVersion )
    {
        addArgIfNotEmpty( arguments, key, value, requiredJavaVersion, false );
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * regarding the requested Java version.
     *
     * @param key                 the argument name.
     * @param value               the argument value to be added.
     * @param requiredJavaVersion the required Java version, for example 1.31f or 1.4f
     * @param repeatKey           repeat or not the key in the command line
     * @see #addArgIfNotEmpty(java.util.List,String,String)
     * @see <a href="http://jakarta.apache.org/commons/lang/api/org/apache/commons/lang/SystemUtils.html#isJavaVersionAtLeast(float)">SystemUtils.html#isJavaVersionAtLeast(float)</a>
     */
    private void addArgIfNotEmpty( List arguments, String key, String value, float requiredJavaVersion,
                                  boolean repeatKey )
    {
        if ( SystemUtils.isJavaVersionAtLeast( requiredJavaVersion ) )
        {
            addArgIfNotEmpty( arguments, key, value, repeatKey );
        }
        else
        {
            getLog().warn( key + " option is not supported on Java version < " + requiredJavaVersion );
        }
    }

    /**
     * Convenience method to wrap an argument value in quotes. Intended for values which may contain whitespaces.
     *
     * @param value the argument value.
     */
    private String quotedArgument( String value )
    {
        if ( !StringUtils.isEmpty( value ) )
        {
            return "'" + value + "'";
        }

        return value;
    }

    /**
     * Convenience method to format a path argument so that it is properly interpreted by the javadoc tool. Intended
     * for path values which may contain whitespaces.
     *
     * @param value the argument value.
     */
    private String quotedPathArgument( String value )
    {
        if ( !StringUtils.isEmpty( value ) )
        {
            return "'" + value.replace( '\\', '/' ) + "'";
        }

        return value;
    }

    /**
     * Convenience method to process offlineLink values as individual -linkoffline javadoc options
     *
     * @param arguments argument list
     */
    private void addLinkofflineArguments( List arguments )
    {
        if ( offlineLinks != null )
        {
            for ( int i = 0; i < offlineLinks.size(); i++ )
            {
                OfflineLink offlineLink = (OfflineLink) offlineLinks.get( i );
                addArgIfNotEmpty( arguments, "-linkoffline", quotedPathArgument( offlineLink.getUrl() ) + " "
                    + quotedPathArgument( offlineLink.getLocation().getAbsolutePath() ), true );
            }
        }
    }

    /**
     * Convenience method to process link values as individual -link javadoc options
     *
     * @param arguments argument list
     */
    private void addLinkArguments( List arguments )
    {
        if ( links != null )
        {
            for ( int i = 0; i < links.size(); i++ )
            {
                addArgIfNotEmpty( arguments, "-link", quotedPathArgument( (String) links.get( i ) ), true );
            }
        }
    }

    /**
     * Returns an input stream for reading the specified resource from the
     * current class loader.
     *
     * @param resource the resource
     * @return InputStream An input stream for reading the resource, or <tt>null</tt>
     *         if the resource could not be found
     */
    private static InputStream getStream( String resource )
    {
        return JavadocReport.class.getClassLoader().getResourceAsStream( resource );
    }

    /**
     * Convenience method that copy the <code>DEFAULT_STYLESHEET_NAME</code> file from the current class
     * loader to the output directory.
     *
     * @param outputDirectory the output directory
     * @throws IOException if any
     * @see #DEFAULT_CSS_NAME
     */
    private void copyDefaultStylesheet( File outputDirectory )
        throws IOException
    {

        if ( outputDirectory == null || !outputDirectory.exists() )
        {
            throw new IOException( "The outputDirectory " + outputDirectory + " doesn't exists." );
        }

        InputStream is = getStream( RESOURCE_CSS_DIR + "/" + DEFAULT_CSS_NAME );

        if ( is == null )
        {
            throw new IOException( "The resource " + DEFAULT_CSS_NAME + " doesn't exists." );
        }

        File outputFile = new File( outputDirectory, DEFAULT_CSS_NAME );

        if ( !outputFile.getParentFile().exists() )
        {
            outputFile.getParentFile().mkdirs();
        }

        FileOutputStream w = new FileOutputStream( outputFile );

        IOUtil.copy( is, w );

        IOUtil.close( is );

        IOUtil.close( w );
    }

    public boolean isExternalReport()
    {
        return true;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    public boolean canGenerateReport()
    {
        ArtifactHandler artifactHandler = project.getArtifact().getArtifactHandler();
        return ( "java".equals( artifactHandler.getLanguage() ) );
    }
}
