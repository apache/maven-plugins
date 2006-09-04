package org.apache.maven.plugin.javadoc;

/*
 * Copyright 2004-2006 The Apache Software Foundation.
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.javadoc.options.Group;
import org.apache.maven.plugin.javadoc.options.Tag;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;

/**
 * Base class with majority of Javadoc functionality.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @requiresDependencyResolution compile
 * @execute phase="generate-sources"
 */
public abstract class AbstractJavadocMojo
    extends AbstractMojo
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
     * @parameter expression="${destDir}" alias="destDir" default-value="${project.build.directory}/apidocs"
     * @required
     */
    protected File outputDirectory;

    /**
     * The Maven Project Object
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Specifies the Javadoc ressources directory to be included in the Javadoc (i.e. package.html, images...).
     *
     * @parameter expression="${basedir}/src/main/javadoc"
     */
    private String javadocDirectory;

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
     * Specifies the artifact containing the doclet starting class file (specified with the -docletpath option).
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#docletpath">docletpath</a>.
     *
     * @parameter
     */
    //TODO: May need to allow multiple artifacts
    private DocletArtifact docletArtifact;

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
     * Specifies the proxy host where the javadoc web access in -link would pass through.
     * It defaults to the proxy host of the active proxy set in the settings.xml, otherwise it gets the proxy
     * configuration set in the pom.
     *
     * @parameter expression="${proxyHost}" default-value="${settings.activeProxy.host}"
     */
    private String proxyHost;

    /**
     * Specifies the proxy port where the javadoc web access in -link would pass through.
     * It defaults to the proxy port of the active proxy set in the settings.xml, otherwise it gets the proxy
     * configuration set in the pom.
     *
     * @parameter expression="${proxyPort}" default-value="${settings.activeProxy.port}"
     */
    private int proxyPort;

    /**
     * This option creates documentation with the appearance and functionality of documentation generated by Javadoc 1.1.
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
     * (shows only public classes and members),
     * <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#protected">protected</a>
     * (shows only public and protected classes and members),
     * <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#package">package</a>
     * (shows all classes and members not marked private), and
     * <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#private">private</a>
     * (shows all classes and members).
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
     * Specifies the source paths where the subpackages are located. The paths are separated by '<code>;</code>'.
     *
     * @parameter expression="${sourcepath}"
     */
    private String sourcepath;

    /**
     * Specifies the package directory where javadoc will be executed. The packages are separated by '<code>:</code>'.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#subpackages">subpackages</a>.
     *
     * @parameter expression="${subpackages}"
     */
    private String subpackages;

    /**
     * Provides more detailed messages while javadoc is running.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#verbose">verbose</a>.
     *
     * @parameter expression="${verbose}" default-value="false"
     */
    private boolean verbose = false;

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
     * @parameter expression="${bottom}" default-value="Copyright &copy; {inceptionYear}-{currentYear} {organizationName}. All Rights Reserved."
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
     * Example:
     * <pre>
     * &lt;groups&gt;<br/>
     *   &lt;group&gt;<br/>
     *     &lt;title&gt;Core Packages&lt;/title&gt;<br/>
     *     &lt;packages&gt;org.apache.core&lt;/packages&gt;<br/>
     *   &lt;/group&gt;<br/>
     *  &lt;/groups&gt;
     * </pre>
     *
     * @parameter expression="${groups}"
     */
    private Group[] groups;

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
     *
     * @parameter expression="${links}"
     */
    private ArrayList links;

    /**
     * This option is a variation of -link; they both create links to javadoc-generated documentation for external referenced classes.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#linkoffline">linkoffline</a>.
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
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nocomment">nocomment</a>.
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
     * Omits the navigation bar from the generated docs.
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
     * Specifies whether the stylesheet to be used is the maven javadoc stylesheet or java's default stylesheet when a <i>stylesheetfile</i> parameter is not specified. Possible values: maven or java.
     *
     * @parameter expression="${stylesheet}" default-value="java"
     */
    private String stylesheet;

    /**
     * Specifies the path of an alternate HTML stylesheet file.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#stylesheetfile">stylesheetfile</a>.
     *
     * @parameter expression="${stylesheetfile}"
     */
    private String stylesheetfile;

    /**
     * Enables the Javadoc tool to interpret a simple, one-argument custom block tag tagname in doc comments.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#tag">tag</a>.
     * Example:
     * <pre>
     * &lt;tags&gt;<br/>
     *   &lt;tag&gt;<br/>
     *     &lt;name&gt;todo&lt;/name&gt;<br/>
     *     &lt;placement&gt;a&lt;/placement&gt;<br/>
     *     &lt;head&gt;To Do:&lt;/head&gt;<br/>
     *   &lt;/tag&gt;<br/>
     *  &lt;/tags&gt;
     * </pre>
     *
     * @parameter expression="${tags}"
     */
    private Tag[] tags;

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
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldoldocs/windows/javadoc.html#use">use</a>.
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
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#windowtitle">windowtitle</a>.
     *
     * @parameter expression="${windowtitle}" default-value="${project.name} ${project.version} API"
     */
    private String windowtitle;

    /**
     * Used for resolving artifacts
     *
     * @component
     */
    private ArtifactResolver resolver;

    /**
     * Factory for creating artifact objects
     *
     * @component
     */
    private ArtifactFactory factory;

    /**
     * The local repository where the artifacts are located
     *
     * @parameter expression="${localRepository}"
     */
    private ArtifactRepository localRepository;

    /**
     * The remote repositories where artifacts are located
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     */
    private List remoteRepositories;

    /**
     * The projects in the reactor for aggregation report.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    private List reactorProjects;

    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     *
     * @parameter expression="${aggregate}" default-value="false"
     */
    protected boolean aggregate;

    /**
     * Used to resolve artifacts of aggregated modules
     *
     * @component
     */
    private ArtifactMetadataSource artifactMetadataSource;

    private static final float MIN_JAVA_VERSION = 1.4f;

    /**
     * @return the output directory
     */
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsoluteFile().toString();
    }

    /**
     * @param locale
     * @throws MavenReportException
     */
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        if ( aggregate && !project.isExecutionRoot() )
        {
            return;
        }

        List sourcePaths = getSourcePaths();

        List files = getFiles( sourcePaths );

        if ( !canGenerateReport( files ) )
        {
            return;
        }

        File javadocOutputDirectory = new File( getOutputDirectory() );
        javadocOutputDirectory.mkdirs();

        if ( !files.isEmpty() )
        {
            File file = new File( javadocOutputDirectory, "files" );
            file.deleteOnExit();
            try
            {
                FileUtils.fileWrite( file.getAbsolutePath(), StringUtils.join( files.iterator(), "\n" ) );
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Unable to write temporary file for command execution", e );
            }
        }

        try
        {
            // Copy default style sheet
            copyDefaultStylesheet( javadocOutputDirectory );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to copy default stylesheet", e );
        }

        StringBuffer options = new StringBuffer();
        if ( !StringUtils.isEmpty( this.locale ) )
        {
            options.append( "-locale " );
            options.append( quotedArgument( this.locale ) );
            options.append( " " );
        }

        String classpath = getClasspath();
        if ( classpath.length() > 0 )
        {
            options.append( "-classpath " );
            options.append( quotedPathArgument( classpath ) );
        }

        Commandline cmd = new Commandline();

        // Set the proxy host and port
        if ( !StringUtils.isEmpty( proxyHost ) && proxyPort > 0 )
        {
            cmd.createArgument().setValue( "-J-DproxyHost=" + proxyHost );
            cmd.createArgument().setValue( "-J-DproxyPort=" + proxyPort );
        }

        addMemoryArg( cmd, "-Xmx", this.maxmemory );

        addMemoryArg( cmd, "-Xms", this.minmemory );

        List arguments = new ArrayList();

        cmd.setWorkingDirectory( javadocOutputDirectory.getAbsolutePath() );
        cmd.setExecutable( getJavadocPath() );

        // General javadoc arguments
        addArgIf( arguments, breakiterator, "-breakiterator", MIN_JAVA_VERSION );
        if ( !StringUtils.isEmpty( doclet ) )
        {
            addArgIfNotEmpty( arguments, "-doclet", quotedArgument( doclet ) );
            addArgIfNotEmpty( arguments, "-docletpath", quotedPathArgument( getDocletPath() ) );
        }
        addArgIfNotEmpty( arguments, "-encoding", quotedArgument( encoding ) );
        addArgIfNotEmpty( arguments, "-extdirs", quotedPathArgument( extdirs ) );

        if ( old && SystemUtils.isJavaVersionAtLeast( MIN_JAVA_VERSION ) )
        {
            getLog().warn( "Javadoc 1.4 doesn't support the -1.1 switch anymore. Ignore this option." );
        }
        else
        {
            addArgIf( arguments, old, "-1.1" );
        }

        addArgIfNotEmpty( arguments, "-overview", quotedPathArgument( overview ) );
        arguments.add( getAccessLevel() );
        addArgIf( arguments, quiet, "-quiet", MIN_JAVA_VERSION );
        addArgIfNotEmpty( arguments, "-source", quotedArgument( source ), MIN_JAVA_VERSION );
        addArgIf( arguments, verbose, "-verbose" );
        addArgIfNotEmpty( arguments, null, additionalparam );

        if ( ( StringUtils.isEmpty( sourcepath ) ) && ( !StringUtils.isEmpty( subpackages ) ) )
        {
            sourcepath = StringUtils.join( sourcePaths.iterator(), File.pathSeparator );
        }

        addArgIfNotEmpty( arguments, "-sourcepath", quotedPathArgument( getSourcePath( sourcePaths ) ) );

        if ( !StringUtils.isEmpty( sourcepath ) )
        {
            addArgIfNotEmpty( arguments, "-subpackages", subpackages );
        }

        addArgIfNotEmpty( arguments, "-exclude", getExcludedPackages( sourcePaths ) );

        // javadoc arguments for default doclet
        if ( StringUtils.isEmpty( doclet ) )
        {
            addArgIf( arguments, author, "-author" );
            addArgIfNotEmpty( arguments, "-bottom", quotedArgument( getBottomText( project.getInceptionYear() ) ) );
            addArgIf( arguments, breakiterator, "-breakiterator", MIN_JAVA_VERSION );
            addArgIfNotEmpty( arguments, "-charset", quotedArgument( charset ) );
            addArgIfNotEmpty( arguments, "-d", quotedPathArgument( javadocOutputDirectory.toString() ) );
            addArgIf( arguments, docfilessubdirs, "-docfilessubdirs", MIN_JAVA_VERSION );
            addArgIfNotEmpty( arguments, "-docencoding", quotedArgument( docencoding ) );
            addArgIfNotEmpty( arguments, "-doctitle", quotedArgument( doctitle ) );
            addArgIfNotEmpty( arguments, "-excludedocfilessubdir", quotedPathArgument( excludedocfilessubdir ),
                              MIN_JAVA_VERSION );
            addArgIfNotEmpty( arguments, "-footer", quotedArgument( footer ) );
            for ( int i = 0; i < groups.length; i++ )
            {
                if ( groups[i] == null || StringUtils.isEmpty( groups[i].getTitle() )
                    || StringUtils.isEmpty( groups[i].getPackages() ) )
                {
                    getLog().info( "A group option is empty. Ignore this option." );
                }
                else
                {
                    String groupTitle = StringUtils.replace( groups[i].getTitle(), ",", "&#44;" );
                    addArgIfNotEmpty( arguments, "-group", quotedArgument( groupTitle ) + " "
                        + quotedArgument( groups[i].getPackages() ), true );
                }
            }
            addArgIfNotEmpty( arguments, "-header", quotedArgument( header ) );
            addArgIfNotEmpty( arguments, "-helpfile", quotedPathArgument( helpfile ) );

            if ( !isOffline )
            {
                addLinkArguments( arguments );
                addLinkofflineArguments( arguments );
                addArgIf( arguments, linksource, "-linksource", MIN_JAVA_VERSION );
            }
            else
            {
                addLinkofflineArguments( arguments );
            }

            addArgIf( arguments, nodeprecated, "-nodeprecated" );
            addArgIf( arguments, nodeprecatedlist, "-nodeprecatedlist" );
            addArgIf( arguments, nocomment, "-nocomment", MIN_JAVA_VERSION );
            addArgIf( arguments, nohelp, "-nohelp" );
            addArgIf( arguments, noindex, "-noindex" );
            addArgIf( arguments, nonavbar, "-nonavbar" );
            addArgIfNotEmpty( arguments, "-noqualifier", quotedArgument( noqualifier ), MIN_JAVA_VERSION );
            addArgIf( arguments, nosince, "-nosince" );
            addArgIf( arguments, notree, "-notree" );
            addArgIf( arguments, serialwarn, "-serialwarn" );
            addArgIf( arguments, splitindex, "-splitindex" );
            addArgIfNotEmpty( arguments, "-stylesheetfile", quotedPathArgument( getStylesheetFile( javadocOutputDirectory ) ) );

            addArgIfNotEmpty( arguments, "-taglet", quotedArgument( taglet ), MIN_JAVA_VERSION );
            addArgIfNotEmpty( arguments, "-tagletpath", quotedPathArgument( tagletpath ), MIN_JAVA_VERSION );

            for ( int i = 0; i < tags.length; i++ )
            {
                if ( tags[i] == null || StringUtils.isEmpty( tags[i].getName() )
                    || StringUtils.isEmpty( tags[i].getPlacement() ) )
                {
                    getLog().info( "A tag option is empty. Ignore this option." );
                }
                else
                {
                    String value = "\"" + tags[i].getName() + ":" + tags[i].getPlacement();
                    if ( !StringUtils.isEmpty( tags[i].getHead() ) )
                    {
                        value += ":" + quotedArgument( tags[i].getHead() );
                    }
                    value += "\"";
                    addArgIfNotEmpty( arguments, "-tag", value, MIN_JAVA_VERSION, false );
                }
            }

            addArgIf( arguments, use, "-use" );
            addArgIf( arguments, version, "-version" );
            addArgIfNotEmpty( arguments, "-windowtitle", quotedArgument( windowtitle ) );
        }

        if ( options.length() > 0 )
        {
            File optionsFile = new File( javadocOutputDirectory, "options" );
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
            if ( !getLog().isDebugEnabled() )
            {
                optionsFile.deleteOnExit();
            }
        }

        if ( !files.isEmpty() )
        {
            cmd.createArgument().setValue( "@files" );
        }

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

        // Javadoc warnings
        if ( StringUtils.isNotEmpty( err.getOutput() ) )
        {
            getLog().info( "Javadoc Warnings" );

            StringTokenizer token = new StringTokenizer( err.getOutput(), "\n" );
            while ( token.hasMoreTokens() )
            {
                String current = token.nextToken().trim();

                getLog().warn( current );
            }
        }
    }

    /**
     * Method to get the files on the specified source paths
     *
     * @param sourcePaths a List that contains the paths to the source files
     * @return a List that contains the specific path for every source file
     */
    protected List getFiles( List sourcePaths )
    {
        List files = new ArrayList();
        if ( StringUtils.isEmpty( subpackages ) )
        {
            String[] excludedPackages = getExcludedPackages();

            for ( Iterator i = sourcePaths.iterator(); i.hasNext(); )
            {
                String sourceDirectory = (String) i.next();
                addFilesFromSource( files, sourceDirectory, excludedPackages );
            }
        }
        return files;
    }

    /**
     * Method to get the excluded source files from the javadoc and create the argument string
     * that will be included in the javadoc commandline execution.
     *
     * @param sourcePaths the list of paths to the source files
     * @return a String that contains the exclude argument that will be used by javadoc
     */
    private String getExcludedPackages( List sourcePaths )
    {
        List excludedNames = null;

        if ( !StringUtils.isEmpty( sourcepath ) && !StringUtils.isEmpty( subpackages ) )
        {
            String[] excludedPackages = getExcludedPackages();
            String[] subpackagesList = subpackages.split( "[:]" );

            excludedNames = getExcludedNames( sourcePaths, subpackagesList, excludedPackages );
        }

        String excludeArg = "";
        if ( !StringUtils.isEmpty( subpackages ) && excludedNames != null )
        {
            //add the excludedpackage names
            for ( Iterator it = excludedNames.iterator(); it.hasNext(); )
            {
                String str = (String) it.next();
                excludeArg = excludeArg + str;

                if ( it.hasNext() )
                {
                    excludeArg = excludeArg + ":";
                }
            }
        }
        return excludeArg;
    }

    /**
     * Method to format the specified source paths that will be accepted by the javadoc tool.
     *
     * @param sourcePaths the list of paths to the source files that will be included in the javadoc
     * @return a String that contains the formatted source path argument
     */
    private String getSourcePath( List sourcePaths )
    {
        String sourcePath = null;

        if ( StringUtils.isEmpty( subpackages ) || !StringUtils.isEmpty( sourcepath ) )
        {
            sourcePath = StringUtils.join( sourcePaths.iterator(), File.pathSeparator );
        }
        return sourcePath;
    }

    /**
     * Method to get the source paths. If no source path is specified in the parameter, the compile source roots
     * of the project will be used.
     *
     * @return a List of the project source paths
     */
    protected List getSourcePaths()
    {
        List sourcePaths;
        if ( StringUtils.isEmpty( sourcepath ) )
        {
            sourcePaths = new ArrayList( project.getCompileSourceRoots() );

            if ( project.getExecutionProject() != null )
            {
                sourcePaths.addAll( project.getExecutionProject().getCompileSourceRoots() );
            }

            if ( javadocDirectory != null )
            {
                File javadocDir = new File( javadocDirectory );
                if ( !javadocDir.exists() || !javadocDir.isDirectory() )
                {
                    getLog().warn( "The file '" + javadocDirectory + "' doesn't exists or it is not a directory." );
                }
                else
                {
                    sourcePaths.add( javadocDirectory );
                }
            }

            if ( aggregate && project.isExecutionRoot() )
            {
                for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
                {
                    MavenProject project = (MavenProject) i.next();

                    List sourceRoots = project.getCompileSourceRoots();
                    ArtifactHandler artifactHandler = project.getArtifact().getArtifactHandler();
                    if ( "java".equals( artifactHandler.getLanguage() ) )
                    {
                        sourcePaths.addAll( sourceRoots );
                    }
                }
            }

            sourcePaths = pruneSourceDirs( sourcePaths );
        }
        else
        {
            sourcePaths = Arrays.asList( sourcepath.split( "[;]" ) );
        }
        return sourcePaths;
    }

    /**
     * Method that removes the invalid directories in the specified source directories
     *
     * @param sourceDirs the list of source directories that will be validated
     * @return a List of valid source directories
     */
    // TODO: could be better aligned with JXR, including getFiles() vs hasSources that finds java files.
    private List pruneSourceDirs( List sourceDirs )
    {
        List pruned = new ArrayList( sourceDirs.size() );
        for ( Iterator i = sourceDirs.iterator(); i.hasNext(); )
        {
            String dir = (String) i.next();
            File directory = new File( dir );
            if ( directory.exists() && directory.isDirectory() )
            {
                if ( !pruned.contains( dir ) )
                {
                    pruned.add( dir );
                }
            }
        }
        return pruned;
    }

    /**
     * Method that gets all the source files to be excluded from the javadoc on the given
     * source paths.
     *
     * @param sourcePaths      the path to the source files
     * @param subpackagesList  list of subpackages to be included in the javadoc
     * @param excludedPackages the package names to be excluded in the javadoc
     * @return a List of the source files to be excluded in the generated javadoc
     */
    private List getExcludedNames( List sourcePaths, String[] subpackagesList, String[] excludedPackages )
    {
        List excludedNames = new ArrayList();
        for ( Iterator i = sourcePaths.iterator(); i.hasNext(); )
        {
            String path = (String) i.next();
            for ( int j = 0; j < subpackagesList.length; j++ )
            {
                List excludes = getExcludedPackages( path, excludedPackages );
                excludedNames.addAll( excludes );
            }
        }
        return excludedNames;
    }

    /**
     * Method to get the packages specified in the excludePackageNames parameter. The packages are split
     * with ',', ':', or ';' and then formatted.
     *
     * @return an array of String objects that contain the package names
     */
    private String[] getExcludedPackages()
    {
        String[] excludePackages = {};

        // for the specified excludePackageNames
        if ( excludePackageNames != null )
        {
            excludePackages = excludePackageNames.split( "[ ,:;]" );
        }
        for ( int i = 0; i < excludePackages.length; i++ )
        {
            excludePackages[i] = excludePackages[i].replace( '.', File.separatorChar );
        }
        return excludePackages;
    }

    /**
     * Method that sets the classpath elements that will be specified in the javadoc -classpath parameter.
     *
     * @return a String that contains the concatenated classpath elements
     * @throws MavenReportException
     */
    private String getClasspath()
        throws MavenReportException
    {
        List classpathElements = new ArrayList();
        Map compileArtifactMap = new HashMap();

        classpathElements.add( project.getBuild().getOutputDirectory() );
        populateCompileArtifactMap( compileArtifactMap, project.getCompileArtifacts() );

        if ( aggregate && project.isExecutionRoot() )
        {
            try
            {
                for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
                {
                    MavenProject subProject = (MavenProject) i.next();
                    if ( subProject != project )
                    {
                        classpathElements.add( subProject.getBuild().getOutputDirectory() );
                        Set dependencyArtifacts = subProject.createArtifacts( factory, null, null );
                        if ( !dependencyArtifacts.isEmpty() )
                        {
                            ArtifactResolutionResult result = resolver
                                .resolveTransitively( dependencyArtifacts, subProject.getArtifact(), subProject
                                    .getRemoteArtifactRepositories(), localRepository, artifactMetadataSource );
                            populateCompileArtifactMap( compileArtifactMap, getCompileArtifacts( result.getArtifacts() ) );
                        }
                    }
                }
            }
            catch ( AbstractArtifactResolutionException e )
            {
                throw new MavenReportException( e.getMessage(), e );
            }
            catch ( InvalidDependencyVersionException e )
            {
                throw new MavenReportException( e.getMessage(), e );
            }
        }

        classpathElements.addAll( compileArtifactMap.values() );
        return StringUtils.join( classpathElements.iterator(), File.pathSeparator );
    }

    /**
     * Copy from {@link MavenProject#getCompileArtifacts()}
     * @param artifacts
     * @return
     */
    private List getCompileArtifacts( Set artifacts )
    {
        List list = new ArrayList( artifacts.size() );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact a = (Artifact) i.next();

            // TODO: classpath check doesn't belong here - that's the other method
            if ( a.getArtifactHandler().isAddedToClasspath() )
            {
                // TODO: let the scope handler deal with this
                if ( Artifact.SCOPE_COMPILE.equals( a.getScope() ) || Artifact.SCOPE_PROVIDED.equals( a.getScope() )
                    || Artifact.SCOPE_SYSTEM.equals( a.getScope() ) )
                {
                    list.add( a );
                }
            }
        }
        return list;
    }

    /**
     * Method to put the artifacts in the hashmap.
     *
     * @param compileArtifactMap the hashmap that will contain the artifacts
     * @param artifactList       the list of artifacts that will be put in the map
     * @throws MavenReportException
     */
    private void populateCompileArtifactMap( Map compileArtifactMap, List artifactList )
        throws MavenReportException
    {
        if ( artifactList != null )
        {
            for ( Iterator i = artifactList.iterator(); i.hasNext(); )
            {
                Artifact a = (Artifact) i.next();

                File file = a.getFile();

                if ( file == null )
                {
                    throw new MavenReportException(
                                                    "Error in plugin descriptor - compile dependencies were not resolved" );
                }
                compileArtifactMap.put( a.getDependencyConflictId(), file.getAbsolutePath() );
            }
        }
    }

    /**
     * Method that sets the bottom text that will be displayed on the bottom of the
     * javadocs.
     *
     * @param inceptionYear the year when the project was started
     * @return a String that contains the text that will be displayed at the bottom of the javadoc
     */
    private String getBottomText( String inceptionYear )
    {
        int actualYear = Calendar.getInstance().get( Calendar.YEAR );
        String year = String.valueOf( actualYear );

        String theBottom = StringUtils.replace( this.bottom, "{currentYear}", year );

        if ( inceptionYear != null )
        {
            if ( inceptionYear.equals( year ) )
            {
                theBottom = StringUtils.replace( theBottom, "{inceptionYear}-", "" );
            }
            else
            {
                theBottom = StringUtils.replace( theBottom, "{inceptionYear}", inceptionYear );
            }
        }
        else
        {
            theBottom = StringUtils.replace( theBottom, "{inceptionYear}-", "" );
        }

        if ( project.getOrganization() == null )
        {
            theBottom = StringUtils.replace( theBottom, " {organizationName}", "" );
        }
        else
        {
            if ( ( project.getOrganization() != null )
                && ( !StringUtils.isEmpty( project.getOrganization().getName() ) ) )
            {
                if ( !StringUtils.isEmpty( project.getOrganization().getUrl() ) )
                {
                    theBottom = StringUtils.replace( theBottom, "{organizationName}", "<a href=\""
                        + project.getOrganization().getUrl() + "\">" + project.getOrganization().getName() + "</a>" );
                }
                else
                {
                    theBottom = StringUtils.replace( theBottom, "{organizationName}", project.getOrganization()
                        .getName() );
                }
            }
            else
            {
                theBottom = StringUtils.replace( theBottom, " {organizationName}", "" );
            }
        }

        return theBottom;
    }

    /**
     * Method to get the stylesheet file to be used in the javadocs. If a custom stylesheet file is not specified,
     * either the stylesheet included in the plugin or the stylesheet file used by the javadoc tool
     * will be used.
     *
     * @param javadocOutputDirectory the base directory of the plugin
     * @return a String that contains the path to the stylesheet file
     */
    private String getStylesheetFile( File javadocOutputDirectory )
    {
        String stylesheetfile = this.stylesheetfile;
        if ( StringUtils.isEmpty( stylesheetfile ) )
        {
            if ( "maven".equals( stylesheet ) )
            {
                stylesheetfile = javadocOutputDirectory + File.separator + DEFAULT_CSS_NAME;
            }
        }
        return stylesheetfile;
    }

    /**
     * Method to get the access level for the classes and members to be shown in the generated javadoc.
     * If the specified access level is not public, protected, package or private, the access level
     * is set to protected.
     *
     * @return the access level
     */
    private String getAccessLevel()
    {
        String accessLevel;
        if ( "public".equalsIgnoreCase( show ) || "protected".equalsIgnoreCase( show )
            || "package".equalsIgnoreCase( show ) || "private".equalsIgnoreCase( show ) )
        {
            accessLevel = "-" + show;
        }
        else
        {
            getLog().error( "Unrecognized access level to show '" + show + "'. Defaulting to protected." );
            accessLevel = "-protected";
        }
        return accessLevel;
    }

    /**
     * Method to get the path to the doclet to be used in the javadoc
     *
     * @return the path to the doclet
     * @throws MavenReportException
     */
    private String getDocletPath()
        throws MavenReportException
    {
        String path;
        if ( docletArtifact != null )
        {
            Artifact artifact = factory.createArtifact( docletArtifact.getGroupId(), docletArtifact.getArtifactId(),
                                                        docletArtifact.getVersion(), "compile", "jar" );
            try
            {
                resolver.resolve( artifact, remoteRepositories, localRepository );
                path = artifact.getFile().getAbsolutePath();
            }
            catch ( ArtifactResolutionException e )
            {
                throw new MavenReportException( "Unable to resolve artifact.", e );
            }
            catch ( ArtifactNotFoundException e )
            {
                throw new MavenReportException( "Unable to find artifact.", e );
            }
        }
        else
        {
            path = docletPath;
        }
        return path;
    }

    /**
     * Method that adds/sets the java memory parameters in the command line execution.
     *
     * @param cmd    the command line execution object where the argument will be added
     * @param arg    the argument parameter name
     * @param memory the JVM memory value to be set
     */
    private void addMemoryArg( Commandline cmd, String arg, String memory )
    {
        if ( !StringUtils.isEmpty( memory ) )
        {
            // Allow '128' or '128m'
            if ( NumberUtils.isDigits( memory ) )
            {
                cmd.createArgument().setValue( "-J" + arg + memory + "m" );
            }
            else
            {
                if ( NumberUtils.isDigits( memory.substring( 0, memory.length() - 1 ) )
                    && memory.toLowerCase().endsWith( "m" ) )
                {
                    cmd.createArgument().setValue( "-J" + arg + memory );
                }
                else
                {
                    getLog().error( arg + " '" + memory + "' is not a valid number. Ignore this option." );
                }
            }
        }
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
     * @param arguments           a list of arguments
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
     * @see #addArgIfNotEmpty(java.util.List, String, String, float, boolean)
     */
    private void addArgIfNotEmpty( List arguments, String key, String value, float requiredJavaVersion )
    {
        addArgIfNotEmpty( arguments, key, value, requiredJavaVersion, false );
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * regarding the requested Java version.
     *
     * @param arguments           a list of arguments
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
        String arg = value;
        if ( !StringUtils.isEmpty( arg ) )
        {
            if ( arg.indexOf( "'" ) != -1 )
            {
                arg = StringUtils.replace( arg, "'", "\\'" );
            }
            arg = "'" + arg + "'";
        }

        return arg;
    }

    /**
     * Convenience method to format a path argument so that it is properly interpreted by the javadoc tool. Intended
     * for path values which may contain whitespaces.
     *
     * @param value the argument value.
     */
    private String quotedPathArgument( String value )
    {
        String path = value;

        if ( !StringUtils.isEmpty( path ) )
        {
            path = path.replace( '\\', '/' );
            if ( path.indexOf( "\'" ) != -1 )
            {
                String split[] = path.split( "\'" );
                path = "";

                for ( int i = 0; i < split.length; i++ )
                {
                    if ( i != split.length - 1 )
                    {
                        path = path + split[i] + "\\'";
                    }
                    else
                    {
                        path = path + split[i];
                    }
                }
            }
            path = "'" + path + "'";
        }

        return path;
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
    private InputStream getStream( String resource )
    {
        return getClass().getClassLoader().getResourceAsStream( resource );
    }

    /**
     * Convenience method that copy the <code>DEFAULT_STYLESHEET_NAME</code> file from the current class
     * loader to the output directory.
     *
     * @param outputDirectory the output directory
     * @throws java.io.IOException if any
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

    /**
     * Method that indicates whether the javadoc can be generated or not. If the project does not contain
     * any source files and no subpackages are specified, the plugin will terminate.
     *
     * @param files the project files
     * @return a boolean that indicates whether javadoc report can be generated or not
     */
    protected boolean canGenerateReport( List files )
    {
        boolean canGenerate = true;

        if ( files.isEmpty() && StringUtils.isEmpty( subpackages ) )
        {
            canGenerate = false;
        }

        return canGenerate;
    }

    /**
     * Method that gets the files or classes that would be included in the javadocs using the subpackages
     * parameter.
     *
     * @param sourceDirectory the directory where the source files are located
     * @param fileList        the list of all files found in the sourceDirectory
     * @param excludePackages package names to be excluded in the javadoc
     * @return a StringBuffer that contains the appended file names of the files to be included in the javadoc
     */
    private List getIncludedFiles( String sourceDirectory, String[] fileList, String[] excludePackages )
    {
        List files = new ArrayList();

        for ( int j = 0; j < fileList.length; j++ )
        {
            boolean include = true;
            for ( int k = 0; k < excludePackages.length && include; k++ )
            {
                // handle wildcards (*) in the excludePackageNames
                String[] excludeName = excludePackages[k].split( "[*]" );

                if ( excludeName.length > 1 )
                {
                    int u = 0;
                    while ( include && u < excludeName.length )
                    {
                        if ( !"".equals( excludeName[u].trim() ) && fileList[j].indexOf( excludeName[u] ) != -1 )
                        {
                            include = false;
                        }
                        u++;
                    }
                }
                else
                {
                    if ( fileList[j].startsWith( sourceDirectory + File.separatorChar + excludeName[0] ) )
                    {
                        if ( excludeName[0].endsWith( String.valueOf( File.separatorChar ) ) )
                        {
                            int i = fileList[j].lastIndexOf( File.separatorChar );
                            String packageName = fileList[j].substring( 0, i + 1 );
                            if ( packageName.equals( sourceDirectory + File.separatorChar + excludeName[0] )
                                && fileList[j].substring( i ).indexOf( ".java" ) != -1 )
                            {
                                include = true;
                            }
                            else
                            {
                                include = false;
                            }
                        }
                        else
                        {
                            include = false;
                        }
                    }
                }
            }

            if ( include )
            {
                files.add( quotedPathArgument( fileList[j] ) );
            }
        }

        return files;
    }

    /**
     * Method that gets the complete package names (including subpackages) of the packages that were defined
     * in the excludePackageNames parameter.
     *
     * @param sourceDirectory     the directory where the source files are located
     * @param excludePackagenames package names to be excluded in the javadoc
     * @return a List of the packagenames to be excluded
     */
    private List getExcludedPackages( String sourceDirectory, String[] excludePackagenames )
    {
        List files = new ArrayList();
        for ( int i = 0; i < excludePackagenames.length; i++ )
        {
            String[] fileList = FileUtils.getFilesFromExtension( sourceDirectory, new String[] { "java" } );
            for ( int j = 0; j < fileList.length; j++ )
            {
                String[] excludeName = excludePackagenames[i].split( "[*]" );
                int u = 0;
                while ( u < excludeName.length )
                {
                    if ( !"".equals( excludeName[u].trim() ) && fileList[j].indexOf( excludeName[u] ) != -1
                        && sourceDirectory.indexOf( excludeName[u] ) == -1 )
                    {
                        files.add( fileList[j] );
                    }
                    u++;
                }
            }
        }

        List excluded = new ArrayList();
        for ( Iterator it = files.iterator(); it.hasNext(); )
        {
            String file = (String) it.next();
            int idx = file.lastIndexOf( File.separatorChar );
            String tmpStr = file.substring( 0, idx );
            tmpStr = tmpStr.replace( '\\', '/' );
            String[] srcSplit = tmpStr.split( sourceDirectory.replace( '\\', '/' ) + '/' );
            String excludedPackage = srcSplit[1].replace( '/', '.' );

            if ( !excluded.contains( excludedPackage ) )
            {
                excluded.add( excludedPackage );
            }
        }

        return excluded;
    }

    /**
     * Convenience method that gets the files to be included in the javadoc.
     *
     * @param sourceDirectory the directory where the source files are located
     * @param files           the variable that contains the appended filenames of the files to be included in the javadoc
     * @param excludePackages the packages to be excluded in the javadocs
     */
    private void addFilesFromSource( List files, String sourceDirectory, String[] excludePackages )
    {
        String[] fileList = FileUtils.getFilesFromExtension( sourceDirectory, new String[] { "java" } );
        if ( fileList != null && fileList.length != 0 )
        {
            List tmpFiles = getIncludedFiles( sourceDirectory, fileList, excludePackages );
            files.addAll( tmpFiles );
        }
    }
}
