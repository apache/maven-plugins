package org.apache.maven.plugin.javadoc;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
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
import org.apache.maven.plugin.javadoc.options.DocletArtifact;
import org.apache.maven.plugin.javadoc.options.JavadocPathArtifact;
import org.apache.maven.plugin.javadoc.options.Tag;
import org.apache.maven.plugin.javadoc.options.Taglet;
import org.apache.maven.plugin.javadoc.options.TagletArtifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.PathUtils;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;

/**
 * Base class with majority of Javadoc functionalities.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @requiresDependencyResolution compile
 * @execute phase="generate-sources"
 * @aggregator
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
     * For Javadoc options appears since Java 1.4.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">
     * What's New in Javadoc 1.4</a>
     */
    private static final float SINCE_JAVADOC_1_4 = 1.4f;

    /**
     * For Javadoc options appears since Java 1.4.2.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.2.html#commandlineoptions">
     * What's New in Javadoc 1.4.2</a>
     */
    private static final float SINCE_JAVADOC_1_4_2 = 1.42f;

    /**
     * For Javadoc options appears since Java 5.0.
     * See <a href="http://java.sun.com/j2se/1.5.0/docs/guide/javadoc/whatsnew-1.5.0.html#commandlineoptions">
     * What's New in Javadoc 5.0</a>
     */
    private static final float SINCE_JAVADOC_1_5 = 1.5f;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The Maven Settings.
     *
     * @parameter default-value="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    /**
     * The Maven Project Object
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter default-value="${settings.offline}"
     * @required
     * @readonly
     */
    private boolean isOffline;

    /**
     * Specifies the Javadoc ressources directory to be included in the Javadoc (i.e. package.html, images...).
     *
     * @parameter expression="${basedir}/src/main/javadoc"
     */
    private String javadocDirectory;

    /**
     * Set an additional parameter(s) on the command line. This value should include quotes as necessary for
     * parameters that include spaces.
     *
     * @parameter expression="${additionalparam}"
     */
    private String additionalparam;

    /**
     * Set an additional J option(s) on the command line.
     * Example:
     * <pre>
     * &lt;additionalJOption&gt;-J-Xss128m&lt;/additionalJOption&gt;
     * </pre>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#J">Jflag</a>.
     *
     * @parameter expression="${additionalJOption}"
     */
    private String additionalJOption;

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

    /**
     * Set this to 'true' to debug Javadoc plugin. With this, 'options' and 'files' files are provided.
     *
     * @parameter expression="${debug}" default-value="false"
     */
    private boolean debug;

    /**
     * Sets the path of the Javadoc Tool executable to use.
     *
     * @parameter expression="${javadocExecutable}"
     */
    private String javadocExecutable;

    /**
     * Version of the Javadoc Tool executable to use, ex. "1.3", "1.5".
     *
     * @parameter expression="${javadocVersion}"
     */
    private String javadocVersion;

    private float fJavadocVersion = 0.0f;

    // ----------------------------------------------------------------------
    // Javadoc Options
    // ----------------------------------------------------------------------

    /**
     * Uses the sentence break iterator to determine the end of the first sentence.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#breakiterator">breakiterator</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${breakiterator}" default-value="false"
     */
    private boolean breakiterator = false;

    /**
     * Specifies the class file that starts the doclet used in generating the documentation.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#doclet">doclet</a>.
     *
     * @parameter expression="${doclet}"
     */
    private String doclet;

    /**
     * Specifies the path to the doclet starting class file (specified with the -doclet option) and any jar files
     * it depends on. The docletPath can contain multiple paths by separating them with a colon (<code>:</code>)
     * on Solaris and a semi-colon (<code>;</code>) on Windows.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#docletpath">docletpath</a>.
     *
     * @parameter expression="${docletPath}"
     */
    private String docletPath;

    /**
     * Specifies the artifact containing the doclet starting class file (specified with the -doclet option).
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#docletpath">docletpath</a>.
     * <br/>
     * Example:
     * <pre>
     * &lt;docletArtifact&gt;<br/>
     *   &lt;groupId&gt;com.sun.tools.doclets&lt;/groupId&gt;<br/>
     *   &lt;artifactId&gt;doccheck&lt;/artifactId&gt;<br/>
     *   &lt;version&gt;1.2b2&lt;/version&gt;<br/>
     * &lt;/docletArtifact&gt;
     * </pre>
     *
     * @parameter expression="${docletArtifact}"
     */
    private DocletArtifact docletArtifact;

    /**
     * Specifies multiple artifacts containing the path for the doclet starting class file (specified with the
     *  -doclet option).
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#docletpath">docletpath</a>.
     * <br/>
     * Example:
     * <pre>
     * &lt;docletArtifacts&gt;<br/>
     *   &lt;docletArtifact&gt;<br/>
     *     &lt;groupId&gt;com.sun.tools.doclets&lt;/groupId&gt;<br/>
     *     &lt;artifactId&gt;doccheck&lt;/artifactId&gt;<br/>
     *     &lt;version&gt;1.2b2&lt;/version&gt;<br/>
     *   &lt;/docletArtifact&gt;<br/>
     * &lt;/docletArtifacts&gt;
     * </pre>
     *
     * @parameter expression="${docletArtifacts}"
     */
    private DocletArtifact[] docletArtifacts;

    /**
     * Specifies the encoding name of the source files.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#encoding">encoding</a>.
     *
     * @parameter expression="${encoding}"
     */
    private String encoding;

    /**
     * Unconditionally excludes the specified packages and their subpackages from the list formed by -subpackages.
     * Multiple packages can be separated by colons (<code>:</code>).
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#exclude">exclude</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${excludePackageNames}"
     */
    private String excludePackageNames;

    /**
     * Specifies the directories where extension classes reside. Separate directories in dirlist with a colon
     * (<code>:</code>) on Solaris and a semi-colon (<code>;</code>) on Windows.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#extdirs">extdirs</a>.
     *
     * @parameter expression="${extdirs}"
     */
    private String extdirs;

    /**
     * Specifies the locale that javadoc uses when generating documentation.
     * <br/>
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
     * This option creates documentation with the appearance and functionality of documentation generated by
     * Javadoc 1.1.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#1.1">1.1</a>.
     *
     * @parameter expression="${old}" default-value="false"
     */
    private boolean old = false;

    /**
     * Specifies that javadoc should retrieve the text for the overview documentation from the "source" file
     * specified by path/filename and place it on the Overview page (overview-summary.html).
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#overview">overview</a>.
     *
     * @parameter expression="${overview}" default-value="${basedir}/src/main/javadoc/overview.html"
     */
    private String overview;

    /**
     * Specifies the access level for classes and members to show in the Javadocs.
     * Possible values are:
     * <ul>
     * <li><a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#public">public</a>
     * (shows only public classes and members)</li>
     * <li><a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#protected">protected</a>
     * (shows only public and protected classes and members)</li>
     * <li><a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#package">package</a>
     * (shows all classes and members not marked private)</li>
     * <li><a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#private">private</a>
     * (shows all classes and members)</li>
     * </ul>
     *
     * @parameter expression="${show}" default-value="protected"
     */
    private String show = "protected";

    /**
     * Shuts off non-error and non-warning messages, leaving only the warnings and errors appear, making them
     * easier to view.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#quiet">quiet</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${quiet}" default-value="false"
     */
    private boolean quiet = false;

    /**
     * Necessary to enable javadoc to handle assertions present in J2SE v 1.4 source code.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#source">source</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${source}"
     */
    private String source;

    /**
     * Specifies the source paths where the subpackages are located. The paths are separated with a colon
     * (<code>:</code>) on Solaris and a semi-colon (<code>;</code>) on Windows.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#sourcepath">sourcepath</a>.
     *
     * @parameter expression="${sourcepath}"
     */
    private String sourcepath;

    /**
     * Specifies the package directory where javadoc will be executed. Multiple packages can be separated by
     * colons (<code>:</code>).
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#subpackages">subpackages</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${subpackages}"
     */
    private String subpackages;

    /**
     * Provides more detailed messages while javadoc is running.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#verbose">verbose</a>.
     *
     * @parameter expression="${verbose}" default-value="false"
     */
    private boolean verbose = false;

    // ----------------------------------------------------------------------
    // Standard Doclet Options
    // ----------------------------------------------------------------------

    /**
     * Specifies whether or not the author text is included in the generated Javadocs.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#author">author</a>.
     *
     * @parameter expression="${author}" default-value="true"
     */
    private boolean author = true;

    /**
     * Specifies the text to be placed at the bottom of each output file.<br/>
     * If you want to use html you have to put it in a CDATA section, <br/>
     * eg. <code>&lt;![CDATA[Copyright 2005, &lt;a href="http://www.mycompany.com">MyCompany, Inc.&lt;a>]]&gt;</code>
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#bottom">bottom</a>.
     *
     * @parameter expression="${bottom}"
     * default-value="Copyright &copy; {inceptionYear}-{currentYear} {organizationName}. All Rights Reserved."
     */
    private String bottom;

    /**
     * Specifies the HTML character set for this document.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#charset">charset</a>.
     *
     * @parameter expression="${charset}" default-value="ISO-8859-1"
     */
    private String charset = "ISO-8859-1";

    /**
     * Specifies the destination directory where javadoc saves the generated HTML files.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#d">d</a>.
     *
     * @parameter expression="${destDir}" alias="destDir" default-value="${project.build.directory}/apidocs"
     * @required
     */
    protected File outputDirectory;

    /**
     * Enables deep copying of "doc-files" directories.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#docfilessubdirs">docfilessubdirs</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${docfilessubdirs}" default-value="false"
     */
    private boolean docfilessubdirs = false;

    /**
     * Specifies the encoding of the generated HTML files.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#docencoding">docencoding</a>.
     *
     * @parameter expression="${docencoding}"
     */
    private String docencoding;

    /**
     * Specifies the title to be placed near the top of the overview summary file.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#doctitle">doctitle</a>.
     *
     * @parameter expression="${doctitle}" default-value="${project.name} ${project.version} API"
     */
    private String doctitle;

    /**
     * Excludes any "doc-files" subdirectories with the given names.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#excludedocfilessubdir">
     * excludedocfilessubdir</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${excludedocfilessubdir}"
     */
    private String excludedocfilessubdir;

    /**
     * Specifies the footer text to be placed at the bottom of each output file.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#footer">footer</a>.
     *
     * @parameter expression="${footer}"
     */
    private String footer;

    /**
     * Separates packages on the overview page into whatever groups you specify, one group per table. The
     * packages pattern can be any package name, or can be the start of any package name followed by an asterisk
     * (<code>*</code>) meaning "match any characters". Multiple patterns can be included in a group
     * by separating them with colons (<code>:</code>).
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#group">group</a>.
     * <br/>
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
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#header">header</a>.
     *
     * @parameter expression="${header}"
     */
    private String header;

    /**
     * Specifies the path of an alternate help file path\filename that the HELP link in the top and bottom
     * navigation bars link to.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#helpfile">helpfile</a>.
     *
     * @parameter expression="${helpfile}"
     */
    private String helpfile;

    /**
     * Adds HTML meta keyword tags to the generated file for each class.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.5.0/docs/tooldocs/windows/javadoc.html#keywords">keywords</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.2.html#commandlineoptions">
     * Java 1.4.2</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.5.0/docs/guide/javadoc/whatsnew-1.5.0.html#commandlineoptions">
     * Java 5.0</a>.
     *
     * @parameter expression="${keywords}" default-value="false"
     */
    private boolean keywords;

    /**
     * Creates links to existing javadoc-generated documentation of external referenced classes.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#link">link</a>.
     *
     * @parameter expression="${links}"
     */
    private ArrayList links;

    /**
     * This option is a variation of -link; they both create links to javadoc-generated documentation for external
     * referenced classes.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#linkoffline">linkoffline</a>.
     *
     * @parameter expression="${offlineLinks}"
     */
    private ArrayList offlineLinks;

    /**
     * Creates an HTML version of each source file (with line numbers) and adds links to them from the standard
     * HTML documentation.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#linksource">linksource</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${linksource}" default-value="false"
     */
    private boolean linksource = false;

    /**
     * Suppress the entire comment body, including the main description and all tags, generating only declarations.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nocomment">nocomment</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${nocomment}" default-value="false"
     */
    private boolean nocomment = false;

    /**
     * Prevents the generation of any deprecated API at all in the documentation.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nodeprecated">nodeprecated</a>.
     *
     * @parameter expression="${nodeprecated}" default-value="false"
     */
    private boolean nodeprecated = false;

    /**
     * Prevents the generation of the file containing the list of deprecated APIs (deprecated-list.html) and the
     * link in the navigation bar to that page.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nodeprecatedlist">
     * nodeprecatedlist</a>.
     *
     * @parameter expression="${nodeprecatedlist}" default-value="false"
     */
    private boolean nodeprecatedlist = false;

    /**
     * Omits the HELP link in the navigation bars at the top and bottom of each page of output.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nohelp">nohelp</a>.
     *
     * @parameter expression="${nohelp}" default-value="false"
     */
    private boolean nohelp = false;

    /**
     * Omits the index from the generated docs.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#noindex">noindex</a>.
     *
     * @parameter expression="${noindex}" default-value="false"
     */
    private boolean noindex = false;

    /**
     * Omits the navigation bar from the generated docs.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nonavbar">nonavbar</a>.
     *
     * @parameter expression="${nonavbar}" default-value="false"
     */
    private boolean nonavbar = false;

    /**
     * Omits qualifying package name from ahead of class names in output.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#noqualifier">noqualifier</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${noqualifier}"
     */
    private String noqualifier;

    /**
     * Omits from the generated docs the "Since" sections associated with the since tags.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nosince">nosince</a>.
     *
     * @parameter expression="${nosince}" default-value="false"
     */
    private boolean nosince = false;

    /**
     * Suppresses the timestamp, which is hidden in an HTML comment in the generated HTML near the top of each page.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.5.0/docs/tooldocs/windows/javadoc.html#notimestamp">notimestamp</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.5.0/docs/guide/javadoc/whatsnew-1.5.0.html#commandlineoptions">
     * Java 5.0</a>.
     *
     * @parameter expression="${notimestamp}" default-value="false"
     */
    private boolean notimestamp;

    /**
     * Omits the class/interface hierarchy pages from the generated docs.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#notree">notree</a>.
     *
     * @parameter expression="${notree}" default-value="false"
     */
    private boolean notree = false;

    /**
     * Specify the text for upper left frame.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.2.html#commandlineoptions">
     * Java 1.4.2</a>.
     *
     * @parameter expression="${packagesheader}"
     */
    private String packagesheader;

    /**
     * Generates compile-time warnings for missing serial tags.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#serialwarn">serialwarn</a>
     *
     * @parameter expression="${serialwarn}" default-value="false"
     */
    private boolean serialwarn = false;

    /**
     * Specify the number of spaces each tab takes up in the source. If no tab is used in source, the default
     * space is used.
     * <br/>
     * Note: was <code>linksourcetab</code> in Java 1.4.2 (refer to bug ID
     * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4788919">4788919</a>).
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.2.html#commandlineoptions">
     * 1.4.2</a>.
     * <br/>
     * Since Java 5.0.
     *
     * @parameter expression="${sourcetab}" alias="linksourcetab"
     */
    private String sourcetab;

    /**
     * Splits the index file into multiple files, alphabetically, one file per letter, plus a file for any index
     * entries that start with non-alphabetical characters.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#splitindex">splitindex</a>.
     *
     * @parameter expression="${splitindex}" default-value="false"
     */
    private boolean splitindex = false;

    /**
     * Specifies whether the stylesheet to be used is the maven javadoc stylesheet or java's default stylesheet
     * when a <i>stylesheetfile</i> parameter is not specified. Possible values: "maven" or "java".
     *
     * @parameter expression="${stylesheet}" default-value="java"
     */
    private String stylesheet;

    /**
     * Specifies the path of an alternate HTML stylesheet file.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#stylesheetfile">
     * stylesheetfile</a>.
     *
     * @parameter expression="${stylesheetfile}"
     */
    private String stylesheetfile;

    /**
     * Enables the Javadoc tool to interpret a simple, one-argument custom block tag tagname in doc comments.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#tag">tag</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     * <br/>
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
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#taglet">taglet</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${taglet}"
     */
    private String taglet;

    /**
     * Specifies the search paths for finding taglet class files (.class). The tagletPath can contain
     * multiple paths by separating them with a colon (<code>:</code>) on Solaris and a semi-colon (<code>;</code>)
     * on Windows.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#tagletpath">tagletpath</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${tagletpath}"
     */
    private String tagletpath;

    /**
     * Specifies the artifact containing the taglet class files (.class).
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#tagletpath">tagletpath</a>.
     * <br/>
     * Example:
     * <pre>
     * &lt;tagletArtifact&gt;<br/>
     *   &lt;groupId&gt;group-Taglet&lt;/groupId&gt;<br/>
     *   &lt;artifactId&gt;artifact-Taglet&lt;/artifactId&gt;<br/>
     *   &lt;version&gt;version-Taglet&lt;/version&gt;<br/>
     * &lt;/tagletArtifact&gt;
     * </pre>
     *
     * @parameter expression="${tagletArtifact}"
     */
    private TagletArtifact tagletArtifact;

    /**
     * Enables the Javadoc tool to interpret multiple taglets.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#taglet">taglet</a>.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#tagletpath">tagletpath</a>.
     * <br/>
     * Example:
     * <pre>
     * &lt;taglets&gt;<br/>
     *   &lt;taglet&gt;<br/>
     *     &lt;tagletClass&gt;com.sun.tools.doclets.ToDoTaglet&lt;/tagletClass&gt;<br/>
     *     &lt;!--&lt;tagletpath&gt;/home/taglets&lt;/tagletpath&gt;--&gt;<br/>
     *     &lt;tagletArtifact&gt;<br/>
     *       &lt;groupId&gt;group-Taglet&lt;/groupId&gt;<br/>
     *       &lt;artifactId&gt;artifact-Taglet&lt;/artifactId&gt;<br/>
     *       &lt;version&gt;version-Taglet&lt;/version&gt;<br/>
     *     &lt;/tagletArtifact&gt;<br/>
     *   &lt;/taglet&gt;<br/>
     *  &lt;/taglets&gt;
     * </pre>
     *
     * @parameter expression="${taglets}"
     */
    private Taglet[] taglets;

    /**
     * Includes one "Use" page for each documented class and package.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldoldocs/windows/javadoc.html#use">use</a>.
     *
     * @parameter expression="${use}" default-value="true"
     */
    private boolean use = true;

    /**
     * Includes the version text in the generated docs.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#version">version</a>.
     *
     * @parameter expression="${version}" default-value="true"
     */
    private boolean version = true;

    /**
     * Specifies the title to be placed in the HTML title tag.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#windowtitle">windowtitle</a>.
     *
     * @parameter expression="${windowtitle}" default-value="${project.name} ${project.version} API"
     */
    private String windowtitle;

    // ----------------------------------------------------------------------
    // protected methods
    // ----------------------------------------------------------------------

    /**
     * @return the output directory
     */
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsoluteFile().toString();
    }

    /**
     * @param p a maven project
     * @return the list of directories where compiled classes are placed for the given project. These dirs are
     * added in the javadoc classpath.
     */
    protected List getProjectBuildOutputDirs( MavenProject p )
    {
        return Collections.singletonList( p.getBuild().getOutputDirectory() );
    }

    /**
     * @param p a maven project
     * @return the list of source paths for the given project
     */
    protected List getProjectSourceRoots( MavenProject p )
    {
        return p.getCompileSourceRoots();
    }

    /**
     * @param p a maven project
     * @return the list of source paths for the execution project of the given project
     */
    protected List getExecutionProjectSourceRoots( MavenProject p )
    {
        return p.getExecutionProject().getCompileSourceRoots();
    }

    /**
     * @param p a maven project
     * @return the list of artifacts for the given project
     */
    protected List getProjectArtifacts( MavenProject p )
    {
        return p.getCompileArtifacts();
    }

    /**
     * @return the current javadoc directory
     */
    protected String getJavadocDirectory()
    {
        return javadocDirectory;
    }

    /**
     * @return the title to be placed near the top of the overview summary file
     */
    protected String getDoctitle()
    {
        return doctitle;
    }

    /**
     * @return the overview documentation file from the user parameter or from the <code>javadocdirectory</code>
     */
    protected String getOverview()
    {
        return overview;
    }

    /**
     * @return the title to be placed in the HTML title tag
     */
    protected String getWindowtitle()
    {
        return windowtitle;
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

        List packageNames = getPackageNames( sourcePaths, files );

        List filesWithUnnamedPackages = getFilesWithUnnamedPackages( sourcePaths, files );

        if ( !canGenerateReport( files ) )
        {
            return;
        }

        // ----------------------------------------------------------------------
        // Find the javadoc executable and version
        // ----------------------------------------------------------------------

        String jExecutable;
        try
        {
            jExecutable = getJavadocExecutable();
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to find javadoc command: " + e.getMessage(), e );
        }

        float jVersion;
        try
        {
            jVersion = getJavadocVersion( new File( jExecutable ) );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to find javadoc version: " + e.getMessage(), e );
        }
        catch ( CommandLineException e )
        {
            throw new MavenReportException( "Unable to find javadoc version: " + e.getMessage(), e );
        }
        if ( StringUtils.isNotEmpty( javadocVersion ) )
        {
            try
            {
                fJavadocVersion = Float.parseFloat( javadocVersion );
            }
            catch ( NumberFormatException e )
            {
                throw new MavenReportException( "Unable to parse javadoc version: " + e.getMessage(), e );
            }

            if ( fJavadocVersion != jVersion )
            {
                getLog().warn( "Are you sure about the <javadocVersion/> parameter? It seems to be " + jVersion );
            }
        }
        else
        {
            fJavadocVersion = jVersion;
        }

        File javadocOutputDirectory = new File( getOutputDirectory() );
        javadocOutputDirectory.mkdirs();

        // ----------------------------------------------------------------------
        // Copy default resources
        // ----------------------------------------------------------------------

        try
        {
            copyDefaultStylesheet( javadocOutputDirectory );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to copy default stylesheet: " + e.getMessage(), e );
        }

        // ----------------------------------------------------------------------
        // Copy javadoc resources
        // ----------------------------------------------------------------------

        try
        {
            copyJavadocResources( javadocOutputDirectory );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to copy javadoc resources: " + e.getMessage(), e );
        }

        // ----------------------------------------------------------------------
        // Wrap javadoc options
        // ----------------------------------------------------------------------

        StringBuffer options = new StringBuffer();
        if ( StringUtils.isNotEmpty( this.locale ) )
        {
            options.append( "-locale " );
            options.append( quotedArgument( this.locale ) );
            options.append( SystemUtils.LINE_SEPARATOR );
        }

        String classpath = getClasspath();
        if ( classpath.length() > 0 )
        {
            options.append( "-classpath " );
            options.append( quotedPathArgument( classpath ) );
            options.append( SystemUtils.LINE_SEPARATOR );
        }

        // ----------------------------------------------------------------------
        // Wrap javadoc arguments
        // ----------------------------------------------------------------------

        Commandline cmd = new Commandline();

        // Set the proxy host and port
        if ( StringUtils.isNotEmpty( proxyHost ) && proxyPort > 0 )
        {
            cmd.createArgument().setValue( "-J-DproxyHost=" + proxyHost );
            cmd.createArgument().setValue( "-J-DproxyPort=" + proxyPort );
        }

        addMemoryArg( cmd, "-Xmx", this.maxmemory );

        addMemoryArg( cmd, "-Xms", this.minmemory );

        if ( StringUtils.isNotEmpty( additionalJOption ) )
        {
            cmd.createArgument().setValue( additionalJOption );
        }

        List arguments = new ArrayList();

        cmd.setWorkingDirectory( javadocOutputDirectory.getAbsolutePath() );
        cmd.setExecutable( jExecutable );

        // General javadoc arguments
        addArgIf( arguments, breakiterator, "-breakiterator", SINCE_JAVADOC_1_4 );
        if ( StringUtils.isNotEmpty( doclet ) )
        {
            addArgIfNotEmpty( arguments, "-doclet", quotedArgument( doclet ) );
            addArgIfNotEmpty( arguments, "-docletpath", quotedPathArgument( getDocletPath() ) );
        }
        addArgIfNotEmpty( arguments, "-encoding", quotedArgument( encoding ) );
        addArgIfNotEmpty( arguments, "-extdirs", quotedPathArgument( extdirs ) );

        if ( old && isJavaDocVersionAtLeast( SINCE_JAVADOC_1_4 ) )
        {
            getLog().warn( "Javadoc 1.4 doesn't support the -1.1 switch anymore. Ignore this option." );
        }
        else
        {
            addArgIf( arguments, old, "-1.1" );
        }

        if ( ( StringUtils.isNotEmpty( getOverview() ) ) && ( new File( getOverview() ).exists() ) )
        {
            addArgIfNotEmpty( arguments, "-overview", quotedPathArgument( getOverview() ) );
        }
        arguments.add( getAccessLevel() );
        addArgIf( arguments, quiet, "-quiet", SINCE_JAVADOC_1_4 );
        addArgIfNotEmpty( arguments, "-source", quotedArgument( source ), SINCE_JAVADOC_1_4 );
        addArgIf( arguments, verbose, "-verbose" );
        addArgIfNotEmpty( arguments, null, additionalparam );

        if ( ( StringUtils.isEmpty( sourcepath ) ) && ( StringUtils.isNotEmpty( subpackages ) ) )
        {
            sourcepath = StringUtils.join( sourcePaths.iterator(), File.pathSeparator );
        }

        addArgIfNotEmpty( arguments, "-sourcepath", quotedPathArgument( getSourcePath( sourcePaths ) ) );

        if ( StringUtils.isNotEmpty( sourcepath ) )
        {
            addArgIfNotEmpty( arguments, "-subpackages", subpackages, SINCE_JAVADOC_1_4 );
        }

        addArgIfNotEmpty( arguments, "-exclude", getExcludedPackages( sourcePaths ), SINCE_JAVADOC_1_4 );

        // ----------------------------------------------------------------------
        // Wrap arguments for default doclet
        // ----------------------------------------------------------------------

        if ( StringUtils.isEmpty( doclet ) )
        {
            addArgIf( arguments, author, "-author" );
            addArgIfNotEmpty( arguments, "-bottom", quotedArgument( getBottomText( project.getInceptionYear() ) ),
                              false, false );
            addArgIf( arguments, breakiterator, "-breakiterator", SINCE_JAVADOC_1_4 );
            addArgIfNotEmpty( arguments, "-charset", quotedArgument( charset ) );
            addArgIfNotEmpty( arguments, "-d", quotedPathArgument( javadocOutputDirectory.toString() ) );
            addArgIf( arguments, docfilessubdirs, "-docfilessubdirs", SINCE_JAVADOC_1_4 );
            addArgIfNotEmpty( arguments, "-docencoding", quotedArgument( docencoding ) );
            addArgIfNotEmpty( arguments, "-doctitle", quotedArgument( getDoctitle() ), false, false );
            addArgIfNotEmpty( arguments, "-excludedocfilessubdir", quotedPathArgument( excludedocfilessubdir ),
                              SINCE_JAVADOC_1_4 );
            addArgIfNotEmpty( arguments, "-footer", quotedArgument( footer ), false, false );
            if ( groups != null )
            {
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
            }
            addArgIfNotEmpty( arguments, "-header", quotedArgument( header ), false, false );
            addArgIfNotEmpty( arguments, "-helpfile", quotedPathArgument( helpfile ) );
            addArgIf( arguments, keywords, "-keywords", SINCE_JAVADOC_1_4_2 );

            if ( !isOffline )
            {
                addLinkArguments( arguments );
            }
            addLinkofflineArguments( arguments );

            addArgIf( arguments, nodeprecated, "-nodeprecated" );
            addArgIf( arguments, nodeprecatedlist, "-nodeprecatedlist" );
            addArgIf( arguments, nocomment, "-nocomment", SINCE_JAVADOC_1_4 );
            addArgIf( arguments, nohelp, "-nohelp" );
            addArgIf( arguments, noindex, "-noindex" );
            addArgIf( arguments, nonavbar, "-nonavbar" );
            addArgIfNotEmpty( arguments, "-noqualifier", quotedArgument( noqualifier ), SINCE_JAVADOC_1_4 );
            addArgIf( arguments, nosince, "-nosince" );
            addArgIf( arguments, notimestamp, "-notimestamp", SINCE_JAVADOC_1_5 );
            addArgIf( arguments, notree, "-notree" );
            addArgIfNotEmpty( arguments, "-packagesheader", packagesheader, SINCE_JAVADOC_1_4_2 );
            addArgIf( arguments, serialwarn, "-serialwarn" );
            addArgIf( arguments, linksource, "-linksource", SINCE_JAVADOC_1_4 );
            if ( fJavadocVersion == SINCE_JAVADOC_1_4_2 )
            {
                addArgIfNotEmpty( arguments, "-linksourcetab", sourcetab );
            }
            else
            {
                addArgIfNotEmpty( arguments, "-sourcetab", sourcetab, SINCE_JAVADOC_1_5 );
            }
            addArgIf( arguments, splitindex, "-splitindex" );
            addArgIfNotEmpty( arguments, "-stylesheetfile",
                              quotedPathArgument( getStylesheetFile( javadocOutputDirectory ) ) );

            addArgIfNotEmpty( arguments, "-taglet", quotedArgument( taglet ), SINCE_JAVADOC_1_4 );
            if ( taglets != null )
            {
                for ( int i = 0; i < taglets.length; i++ )
                {
                    if ( ( taglets[i] == null ) || ( StringUtils.isEmpty( taglets[i].getTagletClass() ) ) )
                    {
                        getLog().info( "A taglet option is empty. Ignore this option." );
                    }
                    else
                    {
                        addArgIfNotEmpty( arguments, "-taglet", quotedArgument( taglets[i].getTagletClass() ),
                                          SINCE_JAVADOC_1_4 );
                    }
                }
            }
            addArgIfNotEmpty( arguments, "-tagletpath", quotedPathArgument( getTagletPath() ), SINCE_JAVADOC_1_4 );

            if ( tags != null )
            {
                for ( int i = 0; i < tags.length; i++ )
                {
                    if ( ( tags[i] == null ) || ( StringUtils.isEmpty( tags[i].getName() ) )
                        || ( StringUtils.isEmpty( tags[i].getPlacement() ) ) )
                    {
                        getLog().info( "A tag option is empty. Ignore this option." );
                    }
                    else
                    {
                        String value = "\"" + tags[i].getName() + ":" + tags[i].getPlacement();
                        if ( StringUtils.isNotEmpty( tags[i].getHead() ) )
                        {
                            value += ":" + quotedArgument( tags[i].getHead() );
                        }
                        value += "\"";
                        addArgIfNotEmpty( arguments, "-tag", value, SINCE_JAVADOC_1_4 );
                    }
                }
            }

            addArgIf( arguments, use, "-use" );
            addArgIf( arguments, version, "-version" );
            addArgIfNotEmpty( arguments, "-windowtitle", quotedArgument( getWindowtitle() ), false, false );
        }

        // ----------------------------------------------------------------------
        // Write options file and include it in the command line
        // ----------------------------------------------------------------------

        if ( options.length() > 0 )
        {
            addCommandLineOptions( cmd, options, arguments, javadocOutputDirectory );
        }

        // ----------------------------------------------------------------------
        // Write packages file and include it in the command line
        // ----------------------------------------------------------------------

        if ( !packageNames.isEmpty() )
        {
            addCommandLinePackages( cmd, javadocOutputDirectory, packageNames );

            // ----------------------------------------------------------------------
            // Write argfile file and include it in the command line
            // ----------------------------------------------------------------------

            if ( !filesWithUnnamedPackages.isEmpty() )
            {
                addCommandLineArgFile( cmd, javadocOutputDirectory, filesWithUnnamedPackages );
            }
        }
        else
        {
            // ----------------------------------------------------------------------
            // Write argfile file and include it in the command line
            // ----------------------------------------------------------------------

            if ( !files.isEmpty() )
            {
                addCommandLineArgFile( cmd, javadocOutputDirectory, files );
            }
        }

        // ----------------------------------------------------------------------
        // Execute command line
        // ----------------------------------------------------------------------

        getLog().debug( Commandline.toString( cmd.getCommandline() ) );

        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
        try
        {
            int exitCode = CommandLineUtils.executeCommandLine( cmd, new DefaultConsumer(), err );

            if ( exitCode != 0 )
            {
                StringBuffer msg = new StringBuffer( "Exit code: " + exitCode + " - " + err.getOutput() );
                msg.append( '\n' );
                msg.append( "Command line was:" + Commandline.toString( cmd.getCommandline() ) );
                throw new MavenReportException( msg.toString() );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MavenReportException( "Unable to execute javadoc command: " + e.getMessage(), e );
        }

        // ----------------------------------------------------------------------
        // Handle Javadoc warnings
        // ----------------------------------------------------------------------

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
                File sourceDirectory = new File( (String) i.next() );
                addFilesFromSource( files, sourceDirectory, excludedPackages );
            }
        }

        return files;
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
            sourcePaths = new ArrayList( getProjectSourceRoots( project ) );

            if ( project.getExecutionProject() != null )
            {
                sourcePaths.addAll( getExecutionProjectSourceRoots( project ) );
            }

            if ( getJavadocDirectory() != null )
            {
                File javadocDir = new File( getJavadocDirectory() );
                if ( javadocDir.exists() && javadocDir.isDirectory() )
                {
                    sourcePaths.add( getJavadocDirectory() );
                }
            }

            if ( aggregate && project.isExecutionRoot() )
            {
                for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
                {
                    MavenProject subProject = (MavenProject) i.next();

                    if ( subProject != project )
                    {
                        List sourceRoots = getProjectSourceRoots( subProject );

                        if ( subProject.getExecutionProject() != null )
                        {
                            sourceRoots.addAll( getExecutionProjectSourceRoots( subProject ) );
                        }

                        ArtifactHandler artifactHandler = subProject.getArtifact().getArtifactHandler();
                        if ( "java".equals( artifactHandler.getLanguage() ) )
                        {
                            sourcePaths.addAll( sourceRoots );
                        }

                        String javadocDirRelative = PathUtils.toRelative( project.getBasedir(), getJavadocDirectory() );
                        File javadocDir = new File( subProject.getExecutionProject().getBasedir(), javadocDirRelative );
                        if ( javadocDir.exists() && javadocDir.isDirectory() )
                        {
                            sourcePaths.add( javadocDir.getAbsolutePath() );
                        }
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
     * Method that indicates whether the javadoc can be generated or not. If the project does not contain any source
     * files and no subpackages are specified, the plugin will terminate.
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

    // ----------------------------------------------------------------------
    // private methods
    // ----------------------------------------------------------------------

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

        if ( StringUtils.isNotEmpty( sourcepath ) && StringUtils.isNotEmpty( subpackages ) )
        {
            String[] excludedPackages = getExcludedPackages();
            String[] subpackagesList = subpackages.split( "[:]" );

            excludedNames = getExcludedNames( sourcePaths, subpackagesList, excludedPackages );
        }

        String excludeArg = "";
        if ( StringUtils.isNotEmpty( subpackages ) && excludedNames != null )
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

        if ( StringUtils.isEmpty( subpackages ) || StringUtils.isNotEmpty( sourcepath ) )
        {
            sourcePath = StringUtils.join( sourcePaths.iterator(), File.pathSeparator );
        }

        return sourcePath;
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

        classpathElements.addAll( getProjectBuildOutputDirs( project ) );

        populateCompileArtifactMap( compileArtifactMap, getProjectArtifacts( project ) );

        if ( aggregate && project.isExecutionRoot() )
        {
            try
            {
                for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
                {
                    MavenProject subProject = (MavenProject) i.next();
                    if ( subProject != project )
                    {
                        classpathElements.addAll( getProjectBuildOutputDirs( subProject ) );

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
                    throw new MavenReportException( "Error in plugin descriptor - "
                        + "dependency was not resolved for artifact: " + a.getGroupId() + ":" + a.getArtifactId() + ":"
                        + a.getVersion() );
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
                && ( StringUtils.isNotEmpty( project.getOrganization().getName() ) ) )
            {
                if ( StringUtils.isNotEmpty( project.getOrganization().getUrl() ) )
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
     * Method to get the path of the doclet artifacts used in the -docletpath option.
     *
     * Either docletArtifact or doclectArtifacts can be defined and used, not both, docletArtifact
     * takes precedence over doclectArtifacts. docletPath is always appended to any result path
     * definition.
     *
     * @return the path to jar file that contains doclet class file separated with a colon (<code>:</code>)
     * on Solaris and a semi-colon (<code>;</code>) on Windows
     * @throws MavenReportException
     */
    private String getDocletPath()
        throws MavenReportException
    {
        StringBuffer path = new StringBuffer();
        if ( !isDocletArtifactEmpty( docletArtifact ) )
        {
            path.append( getArtifactAbsolutePath( docletArtifact ) );
        }
        else if ( docletArtifacts != null )
        {
            for ( int i = 0; i < docletArtifacts.length; i++ )
            {
                if ( !isDocletArtifactEmpty( docletArtifacts[i] ) )
                {
                    path.append( getArtifactAbsolutePath( docletArtifacts[i] ) );

                    if ( i < docletArtifacts.length - 1 )
                    {
                        path.append( File.pathSeparator );
                    }
                }
            }
        }

        if ( !StringUtils.isEmpty( docletPath ) )
        {
            path.append( docletPath );
        }

        if ( StringUtils.isEmpty( path.toString() ) )
        {
            getLog().warn(
                           "No docletpath option was found. Please review <docletpath/> or <docletArtifact/>"
                               + " or <doclets/>." );
        }

        return path.toString();
    }

    private boolean isDocletArtifactEmpty( DocletArtifact docletArtifact )
    {
        if ( docletArtifact == null )
        {
            return true;
        }
        return ( StringUtils.isEmpty( docletArtifact.getGroupId() )
            && StringUtils.isEmpty( docletArtifact.getArtifactId() ) && StringUtils.isEmpty( docletArtifact
            .getVersion() ) );
    }

    /**
     * Method to get the path of the taglet artifacts used in the -tagletpath option.
     *
     * @return the path to jar file that contains taglet class file separated with a colon (<code>:</code>)
     * on Solaris and a semi-colon (<code>;</code>) on Windows
     * @throws MavenReportException
     */
    private String getTagletPath()
        throws MavenReportException
    {
        StringBuffer path = new StringBuffer();

        if ( ( tagletArtifact != null ) && ( StringUtils.isNotEmpty( tagletArtifact.getGroupId() ) )
            && ( StringUtils.isNotEmpty( tagletArtifact.getArtifactId() ) )
            && ( StringUtils.isNotEmpty( tagletArtifact.getVersion() ) ) )
        {
            path.append( getArtifactAbsolutePath( tagletArtifact ) );
        }
        else if ( taglets != null )
        {
            for ( int i = 0; i < taglets.length; i++ )
            {
                Taglet current = taglets[i];
                if ( current != null )
                {
                    boolean separated = false;
                    if ( current.getTagletArtifact() != null )
                    {
                        path.append( getArtifactAbsolutePath( current.getTagletArtifact() ) );
                        separated = true;
                    }
                    else if ( ( current.getTagletArtifact() != null )
                        && ( StringUtils.isNotEmpty( current.getTagletArtifact().getGroupId() ) )
                        && ( StringUtils.isNotEmpty( current.getTagletArtifact().getArtifactId() ) )
                        && ( StringUtils.isNotEmpty( current.getTagletArtifact().getVersion() ) ) )
                    {
                        path.append( getArtifactAbsolutePath( current.getTagletArtifact() ) );
                        separated = true;
                    }
                    else if ( StringUtils.isNotEmpty( current.getTagletpath() ) )
                    {
                        path.append( current.getTagletpath() );
                        separated = true;
                    }

                    if ( separated && ( i < taglets.length - 1 ) )
                    {
                        path.append( File.pathSeparator );
                    }
                }
            }
        }
        else
        {
            path.append( tagletpath );
        }

        return path.toString();
    }

    /**
     * Return the Javadoc artifact path from the local repository
     *
     * @param javadocArtifact
     * @return the locale artifact path
     * @throws MavenReportException
     */
    private String getArtifactAbsolutePath( JavadocPathArtifact javadocArtifact )
        throws MavenReportException
    {
        if ( ( StringUtils.isEmpty( javadocArtifact.getGroupId() ) )
            && ( StringUtils.isEmpty( javadocArtifact.getArtifactId() ) )
            && ( StringUtils.isEmpty( javadocArtifact.getVersion() ) ) )
        {
            return "";
        }

        Artifact artifact = factory.createArtifact( javadocArtifact.getGroupId(), javadocArtifact.getArtifactId(),
                                                    javadocArtifact.getVersion(), "compile", "jar" );
        try
        {
            resolver.resolve( artifact, remoteRepositories, localRepository );

            return artifact.getFile().getAbsolutePath();
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MavenReportException( "Unable to resolve artifact:" + javadocArtifact, e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MavenReportException( "Unable to find artifact:" + javadocArtifact, e );
        }
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
        if ( StringUtils.isNotEmpty( memory ) )
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
     * Get the path of the Javadoc tool executable depending the user entry or try to find it depending the OS
     * or the <code>java.home</code> system property or the <code>JAVA_HOME</code> environment variable.
     *
     * @return the path of the Javadoc tool
     * @throws IOException if not found
     */
    private String getJavadocExecutable()
        throws IOException
    {
        String javadocCommand = "javadoc" + ( SystemUtils.IS_OS_WINDOWS ? ".exe" : "" );

        File javadocExe;

        // ----------------------------------------------------------------------
        // The javadoc executable is defined by the user
        // ----------------------------------------------------------------------
        if ( StringUtils.isNotEmpty( javadocExecutable ) )
        {
            javadocExe = new File( javadocExecutable );

            if ( !javadocExe.exists() || !javadocExe.isFile() )
            {
                throw new IOException( "The javadoc executable '" + javadocExe + "' doesn't exist or is not a file. "
                    + "Verify the <javadocExecutable/> parameter." );
            }

            return javadocExe.getAbsolutePath();
        }

        // ----------------------------------------------------------------------
        // Try to find javadocExe from System.getProperty( "java.home" )
        // By default, System.getProperty( "java.home" ) = JRE_HOME and JRE_HOME
        // should be in the JDK_HOME
        // ----------------------------------------------------------------------
        // For IBM's JDK 1.2
        if ( SystemUtils.IS_OS_AIX )
        {
            javadocExe = new File( SystemUtils.getJavaHome() + File.separator + ".." + File.separator + "sh",
                                   javadocCommand );
        }
        else if ( SystemUtils.IS_OS_MAC_OSX )
        {
            javadocExe = new File( SystemUtils.getJavaHome() + File.separator + "bin", javadocCommand );
        }
        else
        {
            javadocExe = new File( SystemUtils.getJavaHome() + File.separator + ".." + File.separator + "bin",
                                   javadocCommand );
        }

        // ----------------------------------------------------------------------
        // Try to find javadocExe from JAVA_HOME environment variable
        // ----------------------------------------------------------------------
        if ( !javadocExe.exists() || !javadocExe.isFile() )
        {
            Properties env = CommandLineUtils.getSystemEnvVars();
            String javaHome = env.getProperty( "JAVA_HOME" );
            if ( StringUtils.isEmpty( javaHome ) )
            {
                throw new IOException( "The environment variable JAVA_HOME is not correctly set." );
            }
            if ( ( !new File( javaHome ).exists() ) || ( !new File( javaHome ).isDirectory() ) )
            {
                throw new IOException( "The environment variable JAVA_HOME=" + javaHome + " doesn't exist or is "
                    + "not a valid directory." );
            }

            javadocExe = new File( env.getProperty( "JAVA_HOME" ) + File.separator + "bin", javadocCommand );
        }

        if ( !javadocExe.exists() || !javadocExe.isFile() )
        {
            throw new IOException( "The javadoc executable '" + javadocExe + "' doesn't exist or is not a file. "
                + "Verify the JAVA_HOME environment variable." );
        }

        return javadocExe.getAbsolutePath();
    }

    /**
     * Is the Javadoc version at least the requested version.
     *
     * @param requiredVersion the required version, for example 1.5f
     * @return <code>true</code> if the javadoc version is equal or greater than the
     * required version
     */
    private boolean isJavaDocVersionAtLeast( float requiredVersion )
    {
        return fJavadocVersion >= requiredVersion;
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
        if ( isJavaDocVersionAtLeast( requiredJavaVersion ) )
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
     * @param key         the argument name.
     * @param value       the argument value to be added.
     * @param repeatKey   repeat or not the key in the command line
     * @param splitValue  if <code>true</code> given value will be tokenized by comma
     */
    private void addArgIfNotEmpty( List arguments, String key, String value, boolean repeatKey, boolean splitValue )
    {
        if ( StringUtils.isNotEmpty( value ) )
        {
            if ( StringUtils.isNotEmpty( key ) )
            {
                arguments.add( key );
            }

            if ( splitValue )
            {
                StringTokenizer token = new StringTokenizer( value, "," );
                while ( token.hasMoreTokens() )
                {
                    String current = token.nextToken().trim();

                    if ( StringUtils.isNotEmpty( current ) )
                    {
                        arguments.add( current );

                        if ( token.hasMoreTokens() && repeatKey )
                        {
                            arguments.add( key );
                        }
                    }
                }
            }
            else
            {
                arguments.add( value );
            }
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
     * @param repeatKey repeat or not the key in the command line
     */
    private void addArgIfNotEmpty( List arguments, String key, String value, boolean repeatKey )
    {
        addArgIfNotEmpty( arguments, key, value, repeatKey, true );
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
        if ( isJavaDocVersionAtLeast( requiredJavaVersion ) )
        {
            addArgIfNotEmpty( arguments, key, value, repeatKey );
        }
        else
        {
            getLog().warn( key + " option is not supported on Java version < " + requiredJavaVersion );
        }
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
     * Convenience method to process link values as individual -link javadoc options.
     * If a <code>package-list</code> in a configured link is not available, remove the link.
     * <br/>
     * <b>Note</b>: if a link is not fetchable:
     * <ul>
     * <li>Javadoc 1.4 and less throw an exception</li>
     * <li>Javadoc 1.5 and more display a warning</li>
     * </ul>
     *
     * @param arguments argument list
     */
    private void addLinkArguments( List arguments )
    {
        if ( links != null )
        {
            for ( int i = 0; i < links.size(); i++ )
            {
                String link = (String) links.get( i );

                if ( StringUtils.isEmpty( link ) )
                {
                    continue;
                }

                if ( link.endsWith( "/" ) )
                {
                    link = link.substring( 0, link.length() - 1 );
                }

                try
                {
                    URL linkUrl = new URL( link + "/package-list" );
                    fetchURL( settings, linkUrl );
                    addArgIfNotEmpty( arguments, "-link", quotedPathArgument( link ), true );
                }
                catch ( MalformedURLException e )
                {
                    getLog().error( "Malformed link: " + link + "/package-list. IGNORED IT." );
                }
                catch ( IOException e )
                {
                    getLog().error( "Error fetching link: " + link + "/package-list. IGNORED IT." );
                }
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
     * Method that copy the <code>DEFAULT_STYLESHEET_NAME</code> file from the current class
     * loader to the <code>outputDirectory</code>.
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
     * Method that copy all <code>doc-files</code> directories from <code>javadocDirectory</code> of
     * the current projet or of the projects in the reactor to the <code>outputDirectory</code>.
     *
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.2.html#docfiles">Reference
     * Guide, Copies new "doc-files" directory for holding images and examples</a>
     *
     * @param outputDirectory the output directory
     * @throws java.io.IOException if any
     */
    private void copyJavadocResources( File outputDirectory )
        throws IOException
    {
        if ( outputDirectory == null || !outputDirectory.exists() )
        {
            throw new IOException( "The outputDirectory " + outputDirectory + " doesn't exists." );
        }

        if ( getJavadocDirectory() != null )
        {
            copyJavadocResources( outputDirectory, new File( getJavadocDirectory() ) );
        }

        if ( aggregate && project.isExecutionRoot() )
        {
            for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
            {
                MavenProject subProject = (MavenProject) i.next();

                if ( subProject != project )
                {
                    String javadocDirRelative = PathUtils.toRelative( project.getBasedir(), getJavadocDirectory() );
                    File javadocDir = new File( subProject.getBasedir(), javadocDirRelative );
                    copyJavadocResources( outputDirectory, javadocDir );
                }
            }
        }
    }

    /**
     * @param sourcePaths
     * @param files
     * @return the list of package names for files in the sourcePaths
     */
    private List getPackageNames( List sourcePaths, List files )
    {
        return getPackageNamesOrFilesWithUnnamedPackages( sourcePaths, files, true );
    }

    /**
     * @param sourcePaths
     * @param files
     * @return a list files with unnamed package names for files in the sourecPaths
     */
    private List getFilesWithUnnamedPackages( List sourcePaths, List files )
    {
        return getPackageNamesOrFilesWithUnnamedPackages( sourcePaths, files, false );
    }

    /**
     * @param sourcePaths
     * @param files
     * @param onlyPackageName
     * @return a list of package names or files with unnamed package names, depending the value of the unnamed flag
     */
    private List getPackageNamesOrFilesWithUnnamedPackages( List sourcePaths, List files, boolean onlyPackageName )
    {
        List returnList = new ArrayList();

        if ( !StringUtils.isEmpty( sourcepath ) )
        {
            return returnList;
        }

        for ( Iterator it = files.iterator(); it.hasNext(); )
        {
            String currentFile = (String) it.next();
            currentFile = currentFile.replace( '\\', '/' );

            for ( Iterator it2 = sourcePaths.iterator(); it2.hasNext(); )
            {
                String currentSourcePath = (String) it2.next();
                currentSourcePath = currentSourcePath.replace( '\\', '/' );

                if ( !currentSourcePath.endsWith( "/" ) )
                {
                    currentSourcePath += "/";
                }

                if ( currentFile.indexOf( currentSourcePath ) != -1 )
                {
                    String packagename = currentFile.substring( currentSourcePath.length() + 1 );
                    if ( onlyPackageName && packagename.lastIndexOf( "/" ) != -1 )
                    {
                        packagename = packagename.substring( 0, packagename.lastIndexOf( "/" ) );
                        packagename = packagename.replace( '/', '.' );

                        if ( !returnList.contains( packagename ) )
                        {
                            returnList.add( packagename );
                        }
                    }
                    if ( !onlyPackageName && packagename.lastIndexOf( "/" ) == -1 )
                    {
                        returnList.add( currentFile );
                    }
                }
            }
        }

        return returnList;
    }

    /**
     * Generate an "options" file for all options and arguments and add the "@options" in the command line.
     *
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#argumentfiles">
     * Reference Guide, Command line argument files</a>
     *
     * @param cmd
     * @param options
     * @param arguments
     * @param javadocOutputDirectory
     * @throws MavenReportException if any
     */
    private void addCommandLineOptions( Commandline cmd, StringBuffer options, List arguments,
                                        File javadocOutputDirectory )
        throws MavenReportException
    {
        File optionsFile = new File( javadocOutputDirectory, "options" );

        options.append( " " );
        options.append( StringUtils.join( arguments.toArray( new String[0] ), SystemUtils.LINE_SEPARATOR ) );

        try
        {
            FileUtils.fileWrite( optionsFile.getAbsolutePath(), options.toString() );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to write '" + optionsFile.getName()
                + "' temporary file for command execution", e );
        }

        cmd.createArgument().setValue( "@options" );

        if ( !debug )
        {
            optionsFile.deleteOnExit();
        }
    }

    /**
     * Generate a file called "argfile" (or "files", depending the JDK) to hold files and add the "@argfile"
     * (or "@file", depending the JDK) in the command line.
     *
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#argumentfiles">
     * Reference Guide, Command line argument files
     * </a>
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#runningjavadoc">
     * What s New in Javadoc 1.4
     * </a>
     *
     * @param cmd
     * @param javadocOutputDirectory
     * @param files
     * @throws MavenReportException if any
     */
    private void addCommandLineArgFile( Commandline cmd, File javadocOutputDirectory, List files )
        throws MavenReportException
    {
        File argfileFile;
        if ( isJavaDocVersionAtLeast( SINCE_JAVADOC_1_4 ) )
        {
            argfileFile = new File( javadocOutputDirectory, "argfile" );
        }
        else
        {
            argfileFile = new File( javadocOutputDirectory, "files" );
        }

        try
        {
            FileUtils.fileWrite( argfileFile.getAbsolutePath(), StringUtils.join( files.iterator(),
                                                                                  SystemUtils.LINE_SEPARATOR ) );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to write '" + argfileFile.getName()
                + "' temporary file for command execution", e );
        }

        if ( isJavaDocVersionAtLeast( SINCE_JAVADOC_1_4 ) )
        {
            cmd.createArgument().setValue( "@argfile" );
        }
        else
        {
            cmd.createArgument().setValue( "@files" );
        }

        if ( !debug )
        {
            argfileFile.deleteOnExit();
        }
    }

    /**
     * Generate a file called "packages" to hold all package namesand add the "@packages" in the command line.
     *
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#argumentfiles">
     * Reference Guide, Command line argument files</a>
     *
     * @param cmd
     * @param javadocOutputDirectory
     * @param packageNames
     * @throws MavenReportException if any
     */
    private void addCommandLinePackages( Commandline cmd, File javadocOutputDirectory, List packageNames )
        throws MavenReportException
    {
        File packagesFile = new File( javadocOutputDirectory, "packages" );

        try
        {
            FileUtils.fileWrite( packagesFile.getAbsolutePath(), StringUtils
                .join( packageNames.toArray( new String[0] ), SystemUtils.LINE_SEPARATOR ) );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to write '" + packagesFile.getName()
                + "' temporary file for command execution", e );
        }

        cmd.createArgument().setValue( "@packages" );

        if ( !debug )
        {
            packagesFile.deleteOnExit();
        }
    }

    // ----------------------------------------------------------------------
    // private static methods
    // ----------------------------------------------------------------------

    /**
     * Method that removes the invalid directories in the specified source directories
     *
     * @param sourceDirs the list of source directories that will be validated
     * @return a List of valid source directories
     */
    // TODO: could be better aligned with JXR, including getFiles() vs hasSources that finds java files.
    private static List pruneSourceDirs( List sourceDirs )
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
    private static List getExcludedNames( List sourcePaths, String[] subpackagesList, String[] excludedPackages )
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
     * Copy from {@link MavenProject#getCompileArtifacts()}
     * @param artifacts
     * @return list of compile artifacts
     */
    private static List getCompileArtifacts( Set artifacts )
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
     * Convenience method to wrap an argument value in quotes. Intended for values which may contain whitespaces.
     *
     * @param value the argument value.
     * @return argument with quote
     */
    private static String quotedArgument( String value )
    {
        String arg = value;
        if ( StringUtils.isNotEmpty( arg ) )
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
     * @return path argument with quote
     */
    private static String quotedPathArgument( String value )
    {
        String path = value;

        if ( StringUtils.isNotEmpty( path ) )
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
     * Convenience method that copy all <code>doc-files</code> directories from <code>javadocDir</code>
     * to the <code>outputDirectory</code>.
     *
     * @param outputDirectory the output directory
     * @param javadocDir the javadoc directory
     * @throws java.io.IOException if any
     */
    private static void copyJavadocResources( File outputDirectory, File javadocDir )
        throws IOException
    {
        if ( javadocDir.exists() && javadocDir.isDirectory() )
        {
            List docFiles = FileUtils.getDirectoryNames( javadocDir, "**/doc-files", null, false, true );
            for ( Iterator it = docFiles.iterator(); it.hasNext(); )
            {
                String docFile = (String) it.next();

                File docFileOutput = new File( outputDirectory, docFile );
                FileUtils.mkdir( docFileOutput.getAbsolutePath() );
                FileUtils.copyDirectory( new File( javadocDir, docFile ), docFileOutput );
            }
        }
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
    private static List getIncludedFiles( File sourceDirectory, String[] fileList, String[] excludePackages )
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
                    if ( fileList[j].startsWith( sourceDirectory.toString() + File.separatorChar + excludeName[0] ) )
                    {
                        if ( excludeName[0].endsWith( String.valueOf( File.separatorChar ) ) )
                        {
                            int i = fileList[j].lastIndexOf( File.separatorChar );
                            String packageName = fileList[j].substring( 0, i + 1 );
                            File currentPackage = new File( packageName );
                            File excludedPackage = new File( sourceDirectory, excludeName[0] );
                            if ( currentPackage.equals( excludedPackage )
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
    private static List getExcludedPackages( String sourceDirectory, String[] excludePackagenames )
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
    private static void addFilesFromSource( List files, File sourceDirectory, String[] excludePackages )
    {
        String[] fileList = FileUtils.getFilesFromExtension( sourceDirectory.getPath(), new String[] { "java" } );
        if ( fileList != null && fileList.length != 0 )
        {
            List tmpFiles = getIncludedFiles( sourceDirectory, fileList, excludePackages );
            files.addAll( tmpFiles );
        }
    }

    /**
     * Call the javadoc tool to have its version
     *
     * @param javadocExe
     * @return the javadoc version as float
     * @throws IOException if any
     * @throws CommandLineException if any
     */
    private static float getJavadocVersion( File javadocExe )
        throws IOException, CommandLineException
    {
        if ( !javadocExe.exists() || !javadocExe.isFile() )
        {
            throw new IOException( "The javadoc executable '" + javadocExe + "' doesn't exist or is not a file. " );
        }

        Commandline cmd = new Commandline();
        cmd.setExecutable( javadocExe.getAbsolutePath() );
        cmd.setWorkingDirectory( javadocExe.getParentFile() );
        cmd.createArgument().setValue( "-J-fullversion" );

        CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();

        int exitCode = CommandLineUtils.executeCommandLine( cmd, out, err );

        if ( exitCode != 0 )
        {
            StringBuffer msg = new StringBuffer( "Exit code: " + exitCode + " - " + err.getOutput() );
            msg.append( '\n' );
            msg.append( "Command line was:" + Commandline.toString( cmd.getCommandline() ) );
            throw new CommandLineException( msg.toString() );
        }

        /*
         * Exemple: java full version "1.5.0_11-b03"
         *
         * @see com.sun.tools.javac.main.JavaCompiler#fullVersion()
         */
        StringTokenizer token = new StringTokenizer( err.getOutput(), "\"" );
        token.nextToken();

        String version = token.nextToken();
        String str = version.substring( 0, 3 );
        if ( version.length() >= 5 )
        {
            str = str + version.substring( 4, 5 );
        }

        return Float.parseFloat( str );
    }

    /**
     * Fetch an URL
     *
     * @param settings the user settings used to fetch the url like a proxy
     * @param url the url to fetch
     * @throws IOException if any
     */
    private static void fetchURL( Settings settings, URL url )
        throws IOException
    {
        if ( url == null )
        {
            throw new IOException( "The url is null" );
        }

        if ( settings != null )
        {
            String scheme = url.getProtocol();
            if ( !"file".equals( scheme ) )
            {
                Proxy proxy = settings.getActiveProxy();
                if ( proxy != null )
                {
                    if ( "http".equals( scheme ) || "https".equals( scheme ) )
                    {
                        scheme = "http.";
                    }
                    else if ( "ftp".equals( scheme ) )
                    {
                        scheme = "ftp.";
                    }
                    else
                    {
                        scheme = "";
                    }

                    String host = proxy.getHost();
                    if ( !StringUtils.isEmpty( host ) )
                    {
                        Properties p = System.getProperties();
                        p.setProperty( scheme + "proxySet", "true" );
                        p.setProperty( scheme + "proxyHost", host );
                        p.setProperty( scheme + "proxyPort", String.valueOf( proxy.getPort() ) );
                        if ( !StringUtils.isEmpty( proxy.getNonProxyHosts() ) )
                        {
                            p.setProperty( scheme + "nonProxyHosts", proxy.getNonProxyHosts() );
                        }

                        final String userName = proxy.getUsername();
                        if ( !StringUtils.isEmpty( userName ) )
                        {
                            final String pwd = StringUtils.isEmpty( proxy.getPassword() ) ? "" : proxy.getPassword();
                            Authenticator.setDefault( new Authenticator()
                            {
                                protected PasswordAuthentication getPasswordAuthentication()
                                {
                                    return new PasswordAuthentication( userName, pwd.toCharArray() );
                                }
                            } );
                        }
                    }
                }
            }
        }

        InputStream in = null;
        try
        {
            in = url.openStream();
        }
        finally
        {
            IOUtil.close( in );
        }
    }
}
