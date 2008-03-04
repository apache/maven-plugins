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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.javadoc.options.DocletArtifact;
import org.apache.maven.plugin.javadoc.options.Group;
import org.apache.maven.plugin.javadoc.options.JavadocPathArtifact;
import org.apache.maven.plugin.javadoc.options.OfflineLink;
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
 * @since 2.0
 * @requiresDependencyResolution compile
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
     * @since 2.1
     */
    private static final float SINCE_JAVADOC_1_4 = 1.4f;

    /**
     * For Javadoc options appears since Java 1.4.2.
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.2.html#commandlineoptions">
     * What's New in Javadoc 1.4.2</a>
     * @since 2.1
     */
    private static final float SINCE_JAVADOC_1_4_2 = 1.42f;

    /**
     * For Javadoc options appears since Java 5.0.
     * See <a href="http://java.sun.com/j2se/1.5.0/docs/guide/javadoc/whatsnew-1.5.0.html#commandlineoptions">
     * What's New in Javadoc 5.0</a>
     * @since 2.1
     */
    private static final float SINCE_JAVADOC_1_5 = 1.5f;

    /**
     * For Javadoc options appears since Java 6.0.
     * See <a href="http://java.sun.com/javase/6/docs/technotes/guides/javadoc/index.html">
     * Javadoc Technology</a>
     * @since 2.4
     */
    private static final float SINCE_JAVADOC_1_6 = 1.6f;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The Maven Settings.
     *
     * @since 2.3
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
     * Specify if the javadoc should operate in offline mode.
     *
     * @parameter default-value="${settings.offline}"
     * @required
     * @readonly
     */
    private boolean isOffline;

    /**
     * Specifies the Javadoc ressources directory to be included in the Javadoc (i.e. package.html, images...).
     *
     * @since 2.1
     * @parameter expression="${basedir}/src/main/javadoc"
     */
    private File javadocDirectory;

    /**
     * Set an additional parameter(s) on the command line. This value should include quotes as necessary for
     * parameters that include spaces. Useful for a custom doclet.
     *
     * @parameter expression="${additionalparam}"
     */
    private String additionalparam;

    /**
     * Set an additional Javadoc option(s) (i.e. JVM options) on the command line.
     * Example:
     * <pre>
     * &lt;additionalJOption&gt;-J-Xss128m&lt;/additionalJOption&gt;
     * </pre>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#J">Jflag</a>.
     * See <a href="http://java.sun.com/javase/technologies/hotspot/vmoptions.jsp">vmoptions</a>.
     *
     * @since 2.3
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
     * @since 2.1
     * @component
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * Set this to 'true' to debug Javadoc plugin. With this, 'options' and 'files' files are provided.
     *
     * @since 2.1
     * @parameter expression="${debug}" default-value="false"
     */
    private boolean debug;

    /**
     * Sets the path of the Javadoc Tool executable to use.
     *
     * @since 2.3
     * @parameter expression="${javadocExecutable}"
     */
    private String javadocExecutable;

    /**
     * Version of the Javadoc Tool executable to use, ex. "1.3", "1.5".
     *
     * @since 2.3
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
    private boolean breakiterator;

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
     * @since 2.1
     * @parameter expression="${docletArtifacts}"
     */
    private DocletArtifact[] docletArtifacts;

    /**
     * Specifies the encoding name of the source files.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#encoding">encoding</a>.
     * <br/>
     * <b>Note</b>: Since 2.4, the default value is locked to <code>ISO-8859-1</code> to better reproducing build.
     *
     * @parameter expression="${encoding}" default-value="ISO-8859-1"
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
     * Specifies the maximum Java heap size to be used when launching the Javadoc tool.
     * JVMs refer to this property as the <code>-Xmx</code> parameter. Example: '512' or '512m'.
     * The memory unit depends on the JVM used. The units supported could be: <code>k</code>, <code>kb</code>,
     * <code>m</code>, <code>mb</code>, <code>g</code>, <code>gb</code>, <code>t</code>, <code>tb</code>.
     *  If no unit specified, the default unit is <code>m</code>.
     *
     * @parameter expression="${maxmemory}"
     */
    private String maxmemory;

    /**
     * Specifies the minimum Java heap size to be used when launching the Javadoc tool.
     * JVMs refer to this property as the <code>-Xms</code> parameter. Example: '512' or '512m'.
     * The memory unit depends on the JVM used. The units supported could be: <code>k</code>, <code>kb</code>,
     * <code>m</code>, <code>mb</code>, <code>g</code>, <code>gb</code>, <code>t</code>, <code>tb</code>.
     *  If no unit specified, the default unit is <code>m</code>.
     *
     * @parameter expression="${minmemory}"
     */
    private String minmemory;

    /**
     * Specifies the proxy host where the javadoc web access in <code>-link</code> would pass through.
     * It defaults to the proxy host of the active proxy set in the <code>settings.xml</code>, otherwise it gets the proxy
     * configuration set in the pom.
     *
     * @parameter expression="${proxyHost}" default-value="${settings.activeProxy.host}"
     * @deprecated since 2.4. Instead of, configure an active proxy host in <code>settings.xml</code>.
     */
    private String proxyHost;

    /**
     * Specifies the proxy port where the javadoc web access in <code>-link</code> would pass through.
     * It defaults to the proxy port of the active proxy set in the <code>settings.xml</code>, otherwise it gets the proxy
     * configuration set in the pom.
     *
     * @parameter expression="${proxyPort}" default-value="${settings.activeProxy.port}"
     * @deprecated since 2.4. Instead of, configure an active proxy port in <code>settings.xml</code>.
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
    private boolean old;

    /**
     * Specifies that javadoc should retrieve the text for the overview documentation from the "source" file
     * specified by path/filename and place it on the Overview page (overview-summary.html).
     * <br/>
     * <b>Note</b>: could be in conflict with &lt;nooverview/&gt;.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#overview">overview</a>.
     *
     * @parameter expression="${overview}" default-value="${basedir}/src/main/javadoc/overview.html"
     */
    private File overview;

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
    private String show;

    /**
     * Shuts off non-error and non-warning messages, leaving only the warnings and errors appear, making them
     * easier to view.
     * <br/>
     * Note: was a standard doclet in Java 1.4.2 (refer to bug ID
     * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4714350">4714350</a>).
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.5.0/docs/tooldocs/windows/javadoc.html#quiet">quiet</a>.
     * <br/>
     * Since Java 5.0.
     *
     * @parameter expression="${quiet}" default-value="false"
     */
    private boolean quiet;

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
    private boolean verbose;

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
    private boolean author;

    /**
     * Specifies the text to be placed at the bottom of each output file.<br/>
     * If you want to use html you have to put it in a CDATA section, <br/>
     * eg. <code>&lt;![CDATA[Copyright 2005, &lt;a href="http://www.mycompany.com">MyCompany, Inc.&lt;a>]]&gt;</code>
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#bottom">bottom</a>.
     *
     * @parameter expression="${bottom}"
     * default-value="Copyright &#169; {inceptionYear}-{currentYear} {organizationName}. All Rights Reserved."
     */
    private String bottom;

    /**
     * Specifies the HTML character set for this document.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#charset">charset</a>.
     *
     * @parameter expression="${charset}" default-value="ISO-8859-1"
     */
    private String charset;

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
    private boolean docfilessubdirs;

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
     * <b>Note</b>: could be in conflict with &lt;nohelp/&gt;.
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
     * @since 2.1
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
     * <br/>
     * Example:
     * <pre>
     * &lt;offlineLinks&gt;<br/>
     *   &lt;offlineLink&gt;<br/>
     *     &lt;url&gt;http://java.sun.com/j2se/1.5.0/docs/api/&lt;/url&gt;<br/>
     *     &lt;location&gt;../javadoc/jdk-5.0/&lt;/location&gt;<br/>
     *   &lt;/offlineLink&gt;<br/>
     *  &lt;/offlineLinks&gt;
     * </pre>
     *
     * @parameter expression="${offlineLinks}"
     */
    private OfflineLink[] offlineLinks;

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
    private boolean linksource;

    /**
     * Suppress the entire comment body, including the main description and all tags, generating only declarations.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nocomment">nocomment</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${nocomment}" default-value="false"
     */
    private boolean nocomment;

    /**
     * Prevents the generation of any deprecated API at all in the documentation.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nodeprecated">nodeprecated</a>.
     *
     * @parameter expression="${nodeprecated}" default-value="false"
     */
    private boolean nodeprecated;

    /**
     * Prevents the generation of the file containing the list of deprecated APIs (deprecated-list.html) and the
     * link in the navigation bar to that page.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nodeprecatedlist">
     * nodeprecatedlist</a>.
     *
     * @parameter expression="${nodeprecatedlist}" default-value="false"
     */
    private boolean nodeprecatedlist;

    /**
     * Omits the HELP link in the navigation bars at the top and bottom of each page of output.
     * <br/>
     * <b>Note</b>: could be in conflict with &lt;helpfile/&gt;.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nohelp">nohelp</a>.
     *
     * @parameter expression="${nohelp}" default-value="false"
     */
    private boolean nohelp;

    /**
     * Omits the index from the generated docs.
     * <br/>
     * <b>Note</b>: could be in conflict with &lt;splitindex/&gt;.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#noindex">noindex</a>.
     *
     * @parameter expression="${noindex}" default-value="false"
     */
    private boolean noindex;

    /**
     * Omits the navigation bar from the generated docs.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#nonavbar">nonavbar</a>.
     *
     * @parameter expression="${nonavbar}" default-value="false"
     */
    private boolean nonavbar;

    /**
     * Omits the entire overview page from the generated docs.
     * <br/>
     * <b>Note</b>: could be in conflict with &lt;overview/&gt;.
     * <br/>
     * Standard Doclet undocumented option.
     *
     * @since 2.4
     * @parameter expression="${nooverview}" default-value="false"
     */
    private boolean nooverview;

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
    private boolean nosince;

    /**
     * Suppresses the timestamp, which is hidden in an HTML comment in the generated HTML near the top of each page.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.5.0/docs/tooldocs/windows/javadoc.html#notimestamp">notimestamp</a>.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.5.0/docs/guide/javadoc/whatsnew-1.5.0.html#commandlineoptions">
     * Java 5.0</a>.
     *
     * @since 2.1
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
    private boolean notree;

    /**
     * Specify the text for upper left frame.
     * <br/>
     * Since <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.2.html#commandlineoptions">
     * Java 1.4.2</a>.
     *
     * @since 2.1
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
    private boolean serialwarn;

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
     * @since 2.1
     * @parameter expression="${sourcetab}" alias="linksourcetab"
     */
    private String sourcetab;

    /**
     * Splits the index file into multiple files, alphabetically, one file per letter, plus a file for any index
     * entries that start with non-alphabetical characters.
     * <br/>
     * <b>Note</b>: could be in conflict with &lt;noindex/&gt;.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#splitindex">splitindex</a>.
     *
     * @parameter expression="${splitindex}" default-value="false"
     */
    private boolean splitindex;

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
     * @since 2.1
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
     * @since 2.1
     * @parameter expression="${taglets}"
     */
    private Taglet[] taglets;

    /**
     * Specifies the top text to be placed at the top of each output file.
     * <br/>
     * See <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6227616">6227616</a>.
     * <br/>
     * Since Java 6.0
     *
     * @since 2.4
     * @parameter expression="${top}"
     */
    private String top;

    /**
     * Includes one "Use" page for each documented class and package.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#use">use</a>.
     *
     * @parameter expression="${use}" default-value="true"
     */
    private boolean use;

    /**
     * Includes the version text in the generated docs.
     * <br/>
     * See <a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#version">version</a>.
     *
     * @parameter expression="${version}" default-value="true"
     */
    private boolean version;

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
        if ( StringUtils.isEmpty( p.getBuild().getOutputDirectory() ) )
        {
            return Collections.EMPTY_LIST;
        }

        return Collections.singletonList( p.getBuild().getOutputDirectory() );
    }

    /**
     * @param p a maven project
     * @return the list of source paths for the given project
     */
    protected List getProjectSourceRoots( MavenProject p )
    {
        if ( "pom".equals( p.getPackaging().toLowerCase() ) )
        {
            return Collections.EMPTY_LIST;
        }

        return p.getCompileSourceRoots();
    }

    /**
     * @param p a maven project
     * @return the list of source paths for the execution project of the given project
     */
    protected List getExecutionProjectSourceRoots( MavenProject p )
    {
        if ( "pom".equals( p.getExecutionProject().getPackaging().toLowerCase() ) )
        {
            return Collections.EMPTY_LIST;
        }

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
    protected File getJavadocDirectory()
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
    protected File getOverview()
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
     * @param locale the wanted locale (actually unused).
     * @throws MavenReportException if any
     */
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        if ( aggregate && !project.isExecutionRoot() )
        {
            return;
        }

        validateJavadocOptions();

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
            jVersion = JavadocUtil.getJavadocVersion( new File( jExecutable ) );
        }
        catch ( IOException e )
        {
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn( "Unable to find the javadoc version: " + e.getMessage() );
                getLog().warn( "Using the Java version instead of, i.e. " + SystemUtils.JAVA_VERSION_FLOAT );
            }
            jVersion = SystemUtils.JAVA_VERSION_FLOAT;
        }
        catch ( CommandLineException e )
        {
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn( "Unable to find the javadoc version: " + e.getMessage() );
                getLog().warn( "Using the Java the version instead of, i.e. " + SystemUtils.JAVA_VERSION_FLOAT );
            }
            jVersion = SystemUtils.JAVA_VERSION_FLOAT;
        }
        catch ( IllegalArgumentException e )
        {
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn( "Unable to find the javadoc version: " + e.getMessage() );
                getLog().warn( "Using the Java the version instead of, i.e. " + SystemUtils.JAVA_VERSION_FLOAT );
            }
            jVersion = SystemUtils.JAVA_VERSION_FLOAT;
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
                if ( getLog().isWarnEnabled() )
                {
                    getLog().warn( "Are you sure about the <javadocVersion/> parameter? It seems to be " + jVersion );
                }
            }
        }
        else
        {
            fJavadocVersion = jVersion;
        }

        File javadocOutputDirectory = new File( getOutputDirectory() );
        if ( javadocOutputDirectory.exists() && !javadocOutputDirectory.isDirectory() )
        {
            throw new MavenReportException( "IOException: " + getOutputDirectory() + " is not a directory." );
        }
        if ( javadocOutputDirectory.exists() && !javadocOutputDirectory.canWrite() )
        {
            throw new MavenReportException( "IOException: " + getOutputDirectory() + " is not writable." );
        }
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

        if ( docfilessubdirs )
        {
            try
            {
                copyJavadocResources( javadocOutputDirectory );
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Unable to copy javadoc resources: " + e.getMessage(), e );
            }
        }

        // ----------------------------------------------------------------------
        // Wrap javadoc options
        // ----------------------------------------------------------------------

        StringBuffer options = new StringBuffer();
        if ( StringUtils.isNotEmpty( this.locale ) )
        {
            options.append( "-locale " );
            options.append( JavadocUtil.quotedArgument( this.locale ) );
            options.append( SystemUtils.LINE_SEPARATOR );
        }

        String classpath = getClasspath();
        if ( classpath.length() > 0 )
        {
            options.append( "-classpath " );
            options.append( JavadocUtil.quotedPathArgument( classpath ) );
            options.append( SystemUtils.LINE_SEPARATOR );
        }

        // ----------------------------------------------------------------------
        // Wrap javadoc arguments
        // ----------------------------------------------------------------------

        Commandline cmd = new Commandline();
        cmd.getShell().setQuotedArgumentsEnabled( false ); // for Javadoc JVM args
        cmd.setWorkingDirectory( javadocOutputDirectory.getAbsolutePath() );
        cmd.setExecutable( jExecutable );

        // Javadoc JVM args
        addMemoryArg( cmd, "-Xmx", this.maxmemory );

        addMemoryArg( cmd, "-Xms", this.minmemory );

        addProxyArg( cmd );

        if ( StringUtils.isNotEmpty( additionalJOption ) )
        {
            cmd.createArgument().setValue( additionalJOption );
        }

        // General javadoc arguments
        List arguments = new ArrayList();

        addArgIf( arguments, breakiterator, "-breakiterator", SINCE_JAVADOC_1_4 );
        if ( StringUtils.isNotEmpty( doclet ) )
        {
            addArgIfNotEmpty( arguments, "-doclet", JavadocUtil.quotedArgument( doclet ) );
            addArgIfNotEmpty( arguments, "-docletpath", JavadocUtil.quotedPathArgument( getDocletPath() ) );
        }
        addArgIfNotEmpty( arguments, "-encoding", JavadocUtil.quotedArgument( encoding ) );
        addArgIfNotEmpty( arguments, "-extdirs", JavadocUtil.quotedPathArgument( extdirs ) );

        if ( old && isJavaDocVersionAtLeast( SINCE_JAVADOC_1_4 ) )
        {
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn( "Javadoc 1.4+ doesn't support the -1.1 switch anymore. Ignore this option." );
            }
        }
        else
        {
            addArgIf( arguments, old, "-1.1" );
        }

        if ( ( getOverview() != null ) && ( getOverview().exists() ) )
        {
            addArgIfNotEmpty( arguments, "-overview", JavadocUtil.quotedPathArgument( getOverview().getAbsolutePath() ) );
        }
        arguments.add( getAccessLevel() );
        addArgIf( arguments, quiet, "-quiet", SINCE_JAVADOC_1_5 );
        addArgIfNotEmpty( arguments, "-source", JavadocUtil.quotedArgument( source ), SINCE_JAVADOC_1_4 );
        addArgIf( arguments, verbose, "-verbose" );
        addArgIfNotEmpty( arguments, null, additionalparam );

        if ( ( StringUtils.isEmpty( sourcepath ) ) && ( StringUtils.isNotEmpty( subpackages ) ) )
        {
            sourcepath = StringUtils.join( sourcePaths.iterator(), File.pathSeparator );
        }

        addArgIfNotEmpty( arguments, "-sourcepath", JavadocUtil.quotedPathArgument( getSourcePath( sourcePaths ) ) );

        if ( StringUtils.isNotEmpty( sourcepath ) )
        {
            addArgIfNotEmpty( arguments, "-subpackages", subpackages, SINCE_JAVADOC_1_4 );
        }

        addArgIfNotEmpty( arguments, "-exclude", getExcludedPackages( sourcePaths ), SINCE_JAVADOC_1_4 );

        // ----------------------------------------------------------------------
        // Wrap arguments for standard doclet
        // ----------------------------------------------------------------------

        if ( StringUtils.isEmpty( doclet ) )
        {
            validateStandardDocletOptions();

            addArgIf( arguments, author, "-author" );
            addArgIfNotEmpty( arguments, "-bottom", JavadocUtil.quotedArgument( getBottomText() ), false, false );
            addArgIf( arguments, breakiterator, "-breakiterator", SINCE_JAVADOC_1_4 );
            addArgIfNotEmpty( arguments, "-charset", JavadocUtil.quotedArgument( charset ) );
            addArgIfNotEmpty( arguments, "-d", JavadocUtil.quotedPathArgument( javadocOutputDirectory.toString() ) );
            addArgIf( arguments, docfilessubdirs, "-docfilessubdirs", SINCE_JAVADOC_1_4 );
            addArgIfNotEmpty( arguments, "-docencoding", JavadocUtil.quotedArgument( docencoding ) );
            addArgIfNotEmpty( arguments, "-doctitle", JavadocUtil.quotedArgument( getDoctitle() ), false, false );
            if ( docfilessubdirs )
            {
                addArgIfNotEmpty( arguments, "-excludedocfilessubdir", JavadocUtil
                    .quotedPathArgument( excludedocfilessubdir ), SINCE_JAVADOC_1_4 );
            }
            addArgIfNotEmpty( arguments, "-footer", JavadocUtil.quotedArgument( footer ), false, false );
            if ( groups != null )
            {
                for ( int i = 0; i < groups.length; i++ )
                {
                    if ( groups[i] == null || StringUtils.isEmpty( groups[i].getTitle() )
                        || StringUtils.isEmpty( groups[i].getPackages() ) )
                    {
                        if ( getLog().isWarnEnabled() )
                        {
                            getLog().warn( "A group option is empty. Ignore this option." );
                        }
                    }
                    else
                    {
                        String groupTitle = StringUtils.replace( groups[i].getTitle(), ",", "&#44;" );
                        addArgIfNotEmpty( arguments, "-group", JavadocUtil.quotedArgument( groupTitle ) + " "
                            + JavadocUtil.quotedArgument( groups[i].getPackages() ), true );
                    }
                }
            }
            addArgIfNotEmpty( arguments, "-header", JavadocUtil.quotedArgument( header ), false, false );
            addArgIfNotEmpty( arguments, "-helpfile", JavadocUtil.quotedPathArgument( helpfile ) );
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
            addArgIf( arguments, nooverview, "-nooverview" );
            addArgIfNotEmpty( arguments, "-noqualifier", JavadocUtil.quotedArgument( noqualifier ), SINCE_JAVADOC_1_4 );
            addArgIf( arguments, nosince, "-nosince" );
            addArgIf( arguments, notimestamp, "-notimestamp", SINCE_JAVADOC_1_5 );
            addArgIf( arguments, notree, "-notree" );
            addArgIfNotEmpty( arguments, "-packagesheader", JavadocUtil.quotedArgument( packagesheader ), SINCE_JAVADOC_1_4_2 );
            if ( fJavadocVersion >= SINCE_JAVADOC_1_4 && fJavadocVersion < SINCE_JAVADOC_1_5 ) // Sun bug: 4714350
            {
                addArgIf( arguments, quiet, "-quiet" );
            }
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
                              JavadocUtil.quotedPathArgument( getStylesheetFile( javadocOutputDirectory ) ) );

            addArgIfNotEmpty( arguments, "-taglet", JavadocUtil.quotedArgument( taglet ), SINCE_JAVADOC_1_4 );
            if ( taglets != null )
            {
                for ( int i = 0; i < taglets.length; i++ )
                {
                    if ( ( taglets[i] == null ) || ( StringUtils.isEmpty( taglets[i].getTagletClass() ) ) )
                    {
                        if ( getLog().isWarnEnabled() )
                        {
                            getLog().warn( "A taglet option is empty. Ignore this option." );
                        }
                    }
                    else
                    {
                        addArgIfNotEmpty( arguments, "-taglet", JavadocUtil.quotedArgument( taglets[i].getTagletClass() ),
                                          SINCE_JAVADOC_1_4 );
                    }
                }
            }
            addArgIfNotEmpty( arguments, "-tagletpath", JavadocUtil.quotedPathArgument( getTagletPath() ), SINCE_JAVADOC_1_4 );

            if ( tags != null )
            {
                for ( int i = 0; i < tags.length; i++ )
                {
                    if ( StringUtils.isEmpty( tags[i].getName() ) )
                    {
                        if ( getLog().isWarnEnabled() )
                        {
                            getLog().warn( "A tag name is empty. Ignore this option." );
                        }
                    }
                    else
                    {
                        String value = "\"" + tags[i].getName();
                        if ( StringUtils.isNotEmpty( tags[i].getPlacement() ) )
                        {
                            value += ":" + tags[i].getPlacement();
                            if ( StringUtils.isNotEmpty( tags[i].getHead() ) )
                            {
                                value += ":" + tags[i].getHead();
                            }
                        }
                        value += "\"";
                        addArgIfNotEmpty( arguments, "-tag", value, SINCE_JAVADOC_1_4 );
                    }
                }
            }

            addArgIfNotEmpty( arguments, "-top", JavadocUtil.quotedArgument( top ), false, false, SINCE_JAVADOC_1_6 );
            addArgIf( arguments, use, "-use" );
            addArgIf( arguments, version, "-version" );
            addArgIfNotEmpty( arguments, "-windowtitle", JavadocUtil.quotedArgument( getWindowtitle() ), false, false );
        }

        // ----------------------------------------------------------------------
        // Write options file and include it in the command line
        // ----------------------------------------------------------------------

        if ( options.length() > 0 || arguments.size() > 0 )
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

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( Commandline.toString( cmd.getCommandline() ).replaceAll( "'", "" ) ); // no quoted arguments
        }

        if ( debug )
        {
            File commandLineFile = new File( javadocOutputDirectory, "javadoc." + ( SystemUtils.IS_OS_WINDOWS ? "bat" : "sh" ) );

            try
            {
                FileUtils.fileWrite( commandLineFile.getAbsolutePath(), Commandline.toString( cmd.getCommandline() ).replaceAll( "'", "" ) );

                if ( !SystemUtils.IS_OS_WINDOWS )
                {
                    Runtime.getRuntime().exec( new String[] { "chmod", "a+x", commandLineFile.getAbsolutePath() } );
                }
            }
            catch ( IOException e )
            {
                if ( getLog().isWarnEnabled() )
                {
                    getLog().warn( "Unable to write '" + commandLineFile.getName() + "' debug script file", e );
                }
            }
        }

        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
        try
        {
            int exitCode = CommandLineUtils.executeCommandLine( cmd, new DefaultConsumer(), err );

            if ( exitCode != 0 )
            {
                String cmdLine = Commandline.toString( cmd.getCommandline() ).replaceAll( "'", "" );
                cmdLine = JavadocUtil.hideProxyPassword( cmdLine, settings );

                StringBuffer msg = new StringBuffer( "Exit code: " + exitCode + " - " + err.getOutput() );
                msg.append( '\n' );
                msg.append( "Command line was:" + cmdLine );
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
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn( "Javadoc Warnings" );

                StringTokenizer token = new StringTokenizer( err.getOutput(), "\n" );
                while ( token.hasMoreTokens() )
                {
                    String current = token.nextToken().trim();

                    getLog().warn( current );
                }
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
                JavadocUtil.addFilesFromSource( files, sourceDirectory, excludedPackages );
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
                File javadocDir = getJavadocDirectory();
                if ( javadocDir.exists() && javadocDir.isDirectory() )
                {
                    sourcePaths.add( getJavadocDirectory().getAbsolutePath() );
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

                        ArtifactHandler artifactHandler = subProject.getArtifact().getArtifactHandler();
                        if ( "java".equals( artifactHandler.getLanguage() ) )
                        {
                            sourcePaths.addAll( sourceRoots );
                        }

                        if ( subProject.getExecutionProject() != null )
                        {
                            String javadocDirRelative = PathUtils.toRelative( project.getBasedir(), getJavadocDirectory().getAbsolutePath() );
                            File javadocDir = new File( subProject.getExecutionProject().getBasedir(), javadocDirRelative );
                            if ( javadocDir.exists() && javadocDir.isDirectory() )
                            {
                                sourcePaths.add( javadocDir.getAbsolutePath() );
                            }
                        }
                    }
                }
            }
        }
        else
        {
            sourcePaths = new ArrayList( Arrays.asList( sourcepath.split( "[;]" ) ) );
            if ( getJavadocDirectory() != null )
            {
                sourcePaths.add( getJavadocDirectory().getAbsolutePath() );
            }
        }

        sourcePaths = JavadocUtil.pruneDirs( sourcePaths );
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

            excludedNames = JavadocUtil.getExcludedNames( sourcePaths, subpackagesList, excludedPackages );
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
                            ArtifactResolutionResult result = null;
                            try
                            {
                                result = resolver.resolveTransitively( dependencyArtifacts, subProject.getArtifact(),
                                                                       subProject.getRemoteArtifactRepositories(),
                                                                       localRepository, artifactMetadataSource );
                            }
                            catch ( MultipleArtifactsNotFoundException e )
                            {
                                if ( checkMissingArtifactsInReactor( dependencyArtifacts, e.getMissingArtifacts() ) )
                                {
                                    getLog().warn( "IGNORED to add some artifacts in the classpath. See above." );
                                }
                                else
                                {
                                    //we can't find all the artifacts in the reactor so bubble the exception up.
                                    throw new MavenReportException( e.getMessage(), e );
                                }
                            }
                            catch ( ArtifactNotFoundException e )
                            {
                                throw new MavenReportException( e.getMessage(), e );
                            }
                            catch ( ArtifactResolutionException e )
                            {
                                throw new MavenReportException( e.getMessage(), e );
                            }

                            if ( result == null )
                            {
                                continue;
                            }

                            populateCompileArtifactMap( compileArtifactMap, JavadocUtil.getCompileArtifacts( result.getArtifacts() ) );

                            if ( getLog().isDebugEnabled() )
                            {
                                StringBuffer sb = new StringBuffer();

                                sb.append( "Compiled artifacts for " );
                                sb.append( subProject.getGroupId() ).append( ":" );
                                sb.append( subProject.getArtifactId() ).append( ":" );
                                sb.append( subProject.getVersion() ).append( '\n' );
                                for ( Iterator it = compileArtifactMap.keySet().iterator(); it.hasNext(); )
                                {
                                    String key = it.next().toString();

                                    Artifact a = (Artifact) compileArtifactMap.get( key );
                                    sb.append( a.getFile() ).append( '\n' );
                                }

                                getLog().debug( sb.toString() );
                            }
                        }
                    }
                }
            }
            catch ( InvalidDependencyVersionException e )
            {
                throw new MavenReportException( e.getMessage(), e );
            }
        }

        for ( Iterator it = compileArtifactMap.keySet().iterator(); it.hasNext(); )
        {
            String key = it.next().toString();

            Artifact a = (Artifact) compileArtifactMap.get( key );
            classpathElements.add( a.getFile() );
        }

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
                Artifact newArtifact = (Artifact) i.next();

                File file = newArtifact.getFile();

                if ( file == null )
                {
                    throw new MavenReportException( "Error in plugin descriptor - "
                        + "dependency was not resolved for artifact: " + newArtifact.getGroupId() + ":"
                        + newArtifact.getArtifactId() + ":" + newArtifact.getVersion() );
                }

                if ( compileArtifactMap.get( newArtifact.getDependencyConflictId() ) != null )
                {
                    Artifact oldArtifact = (Artifact) compileArtifactMap.get( newArtifact.getDependencyConflictId() );

                    ArtifactVersion oldVersion = new DefaultArtifactVersion( oldArtifact.getVersion() );
                    ArtifactVersion newVersion = new DefaultArtifactVersion( newArtifact.getVersion() );
                    if ( newVersion.compareTo( oldVersion ) > 0 )
                    {
                        compileArtifactMap.put( newArtifact.getDependencyConflictId(), newArtifact );
                    }
                }
                else
                {
                    compileArtifactMap.put( newArtifact.getDependencyConflictId(), newArtifact );
                }
            }
        }
    }

    /**
     * Method that sets the bottom text that will be displayed on the bottom of the
     * javadocs.
     *
     * @return a String that contains the text that will be displayed at the bottom of the javadoc
     */
    private String getBottomText()
    {
        int actualYear = Calendar.getInstance().get( Calendar.YEAR );
        String year = String.valueOf( actualYear );

        String inceptionYear = project.getInceptionYear();

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
            if ( StringUtils.isNotEmpty( project.getOrganization().getName() ) )
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
            if ( getLog().isErrorEnabled() )
            {
                getLog().error( "Unrecognized access level to show '" + show + "'. Defaulting to protected." );
            }
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
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn(
                               "No docletpath option was found. Please review <docletpath/> or <docletArtifact/>"
                                   + " or <doclets/>." );
            }
        }

        return path.toString();
    }

    /**
     * Verify if a doclet artifact is empty or not
     *
     * @param aDocletArtifact
     * @return true if aDocletArtifact or the groupId/artifactId/version of the doclet artifact is null, false otherwise.
     */
    private boolean isDocletArtifactEmpty( DocletArtifact aDocletArtifact )
    {
        if ( aDocletArtifact == null )
        {
            return true;
        }

        return ( StringUtils.isEmpty( aDocletArtifact.getGroupId() )
            && StringUtils.isEmpty( aDocletArtifact.getArtifactId() ) && StringUtils.isEmpty( aDocletArtifact
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
            List tagletsPath = new ArrayList();
            for ( int i = 0; i < taglets.length; i++ )
            {
                Taglet current = taglets[i];

                if ( current == null )
                {
                    continue;
                }

                if ( current.getTagletArtifact() != null )
                {
                    tagletsPath.add( getArtifactAbsolutePath( current.getTagletArtifact() ) );
                }
                else if ( ( current.getTagletArtifact() != null )
                    && ( StringUtils.isNotEmpty( current.getTagletArtifact().getGroupId() ) )
                    && ( StringUtils.isNotEmpty( current.getTagletArtifact().getArtifactId() ) )
                    && ( StringUtils.isNotEmpty( current.getTagletArtifact().getVersion() ) ) )
                {
                    tagletsPath.add( getArtifactAbsolutePath( current.getTagletArtifact() ) );
                }
                else if ( StringUtils.isNotEmpty( current.getTagletpath() ) )
                {
                    tagletsPath.add( current.getTagletpath() );
                }
            }

            tagletsPath = JavadocUtil.pruneFiles( tagletsPath );

            path.append( StringUtils.join( tagletsPath.iterator(), File.pathSeparator ) );
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
     * @see JavadocUtil#parseJavadocMemory(String)
     */
    private void addMemoryArg( Commandline cmd, String arg, String memory )
    {
        if ( StringUtils.isNotEmpty( memory ) )
        {
            try
            {
                cmd.createArgument().setValue( "-J" + arg + JavadocUtil.parseJavadocMemory( memory ) );
            }
            catch ( IllegalArgumentException e )
            {
                if ( getLog().isErrorEnabled() )
                {
                    getLog().error( "Malformed memory pattern for '" + arg + memory + "'. Ignore this option." );
                }
            }
        }
    }

    /**
     * Method that adds/sets the javadoc proxy parameters in the command line execution.
     *
     * @param cmd    the command line execution object where the argument will be added
     */
    private void addProxyArg( Commandline cmd )
    {
        // backward compatible
        if ( StringUtils.isNotEmpty( proxyHost ) )
        {
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn( "The Javadoc plugin parameter 'proxyHost' is deprecated since 2.4. " +
                        "Please configure an active proxy in your settings.xml." );
            }
            cmd.createArgument().setValue( "-J-DproxyHost=" + proxyHost );

            if ( proxyPort > 0 )
            {
                if ( getLog().isWarnEnabled() )
                {
                    getLog().warn( "The Javadoc plugin parameter 'proxyPort' is deprecated since 2.4. " +
                        "Please configure an active proxy in your settings.xml." );
                }
                cmd.createArgument().setValue( "-J-DproxyPort=" + proxyPort );
            }
        }

        if ( settings == null )
        {
            return;
        }

        Proxy activeProxy = settings.getActiveProxy();
        if ( activeProxy != null )
        {
            String protocol = StringUtils.isNotEmpty( activeProxy.getProtocol() ) ? activeProxy.getProtocol() + "."
                                                                                 : "";

            if ( StringUtils.isNotEmpty( activeProxy.getHost() ) )
            {
                cmd.createArgument().setValue( "-J-D" + protocol + "proxySet=true" );
                cmd.createArgument().setValue( "-J-D" + protocol + "proxyHost=" + activeProxy.getHost() );

                if ( activeProxy.getPort() > 0 )
                {
                    cmd.createArgument().setValue( "-J-D" + protocol + "proxyPort=" + activeProxy.getPort() );
                }

                if ( StringUtils.isNotEmpty( activeProxy.getNonProxyHosts() ) )
                {
                    cmd.createArgument().setValue( "-J-D" + protocol + "nonProxyHosts=\"" + activeProxy.getNonProxyHosts() + "\"" );
                }

                if ( StringUtils.isNotEmpty( activeProxy.getUsername() ) )
                {
                    cmd.createArgument().setValue( "-J-Dhttp.proxyUser=\"" + activeProxy.getUsername() + "\"" );

                    if ( StringUtils.isNotEmpty( activeProxy.getPassword() ) )
                    {
                        cmd.createArgument().setValue( "-J-Dhttp.proxyPassword=\"" + activeProxy.getPassword() + "\"" );
                    }
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
     * @see #isJavaDocVersionAtLeast(float)
     */
    private void addArgIf( List arguments, boolean b, String value, float requiredJavaVersion )
    {
        if ( isJavaDocVersionAtLeast( requiredJavaVersion ) )
        {
            addArgIf( arguments, b, value );
        }
        else
        {
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn( value + " option is not supported on Java version < " + requiredJavaVersion
                               + ". Ignore this option." );
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
     * @param requiredJavaVersion the required Java version, for example 1.31f or 1.4f
     * @see #addArgIfNotEmpty(List, String, String, boolean, boolean)
     * @see #isJavaDocVersionAtLeast(float)
     */
    private void addArgIfNotEmpty( List arguments, String key, String value, boolean repeatKey, boolean splitValue, float requiredJavaVersion )
    {
        if ( isJavaDocVersionAtLeast( requiredJavaVersion ) )
        {
            addArgIfNotEmpty( arguments, key, value, repeatKey, splitValue );
        }
        else
        {
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn( key + " option is not supported on Java version < " + requiredJavaVersion
                               + ". Ignore this option." );
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
     * @see #isJavaDocVersionAtLeast(float)
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
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn( key + " option is not supported on Java version < " + requiredJavaVersion );
            }
        }
    }

    /**
     * Convenience method to process offlineLink values as individual -linkoffline javadoc options
     *
     * @param arguments argument list
     */
    private void addLinkofflineArguments( List arguments )
    {
        List offlineLinksList = ( offlineLinks != null ? new ArrayList( Arrays.asList( offlineLinks ) ): new ArrayList() );

        if ( !aggregate && reactorProjects != null )
        {
            String javadocDirRelative = PathUtils.toRelative( project.getBasedir(), getOutputDirectory() );

            for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
            {
                MavenProject p = (MavenProject) it.next();

                // don't add projects that have not built yet.
                if ( p.getId().equals( project.getId() ) )
                {
                    break;
                }

                if ( p.getUrl() != null )
                {
                    String url = p.getUrl() + "/apidocs";
                    File location = new File( p.getBasedir(), javadocDirRelative );

                    if ( location.exists() )
                    {
                        OfflineLink ol = new OfflineLink();
                        ol.setUrl( url );
                        ol.setLocation( location.getAbsolutePath() );

                        offlineLinksList.add( ol );
                    }
                }
            }
        }

        if ( offlineLinksList != null )
        {
            for ( int i = 0; i < offlineLinksList.size(); i++ )
            {
                OfflineLink offlineLink = (OfflineLink) offlineLinksList.get( i );
                addArgIfNotEmpty( arguments, "-linkoffline", JavadocUtil.quotedPathArgument( offlineLink.getUrl() ) + " "
                    + JavadocUtil.quotedPathArgument( offlineLink.getLocation() ), true );
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
                    // XXX links can be relative paths or files - they're not necessarily URLs.
                    URL linkUrl = new URL( link + "/package-list" );
                    JavadocUtil.fetchURL( settings, linkUrl );
                    addArgIfNotEmpty( arguments, "-link", JavadocUtil.quotedPathArgument( link ), true );
                }
                catch ( MalformedURLException e )
                {
                    if ( getLog().isErrorEnabled() )
                    {
                        getLog().error( "Malformed link: " + link + "/package-list. Ignored it." );
                    }
                }
                catch ( IOException e )
                {
                    if ( getLog().isErrorEnabled() )
                    {
                        getLog().error( "Error fetching link: " + link + "/package-list. Ignored it." );
                    }
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
            JavadocUtil.copyJavadocResources( outputDirectory, getJavadocDirectory() );
        }

        if ( aggregate && project.isExecutionRoot() )
        {
            for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
            {
                MavenProject subProject = (MavenProject) i.next();

                if ( subProject != project )
                {
                    String javadocDirRelative = PathUtils.toRelative( project.getBasedir(), getJavadocDirectory().getAbsolutePath() );
                    File javadocDir = new File( subProject.getBasedir(), javadocDirRelative );
                    JavadocUtil.copyJavadocResources( outputDirectory, javadocDir );
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
     * @see #isJavaDocVersionAtLeast(float)
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

    /**
     * Checks for the validity of the Javadoc options used by the user.
     *
     * @throws MavenReportException if error
     */
    private void validateJavadocOptions()
        throws MavenReportException
    {
        // encoding
        if ( StringUtils.isNotEmpty( encoding ) && !JavadocUtil.validateEncoding( encoding ) )
        {
            throw new MavenReportException( "Encoding not supported: " + encoding );
        }
    }

    /**
     * Checks for the validity of the Standard Doclet options.
     * <br/>
     * For example, throw an exception if &lt;nohelp/&gt; and &lt;helpfile/&gt; options are used together.
     *
     * @throws MavenReportException if error or conflict found
     */
    private void validateStandardDocletOptions()
        throws MavenReportException
    {
        // docencoding
        if ( StringUtils.isNotEmpty( docencoding ) && !JavadocUtil.validateEncoding( docencoding ) )
        {
            throw new MavenReportException( "Encoding not supported: " + docencoding );
        }

        // helpfile
        if ( StringUtils.isNotEmpty( helpfile ) && nohelp )
        {
            throw new MavenReportException( "Option <nohelp/> conflicts with <helpfile/>" );
        }
        if ( ( StringUtils.isNotEmpty( helpfile ) ) && ( !new File( helpfile ).exists() ) )
        {
            throw new MavenReportException( "File not found: " + helpfile );
        }

        // overview
        if ( ( getOverview() != null ) && nooverview )
        {
            throw new MavenReportException( "Option <nooverview/> conflicts with <overview/>" );
        }

        // index
        if ( splitindex && noindex )
        {
            throw new MavenReportException( "Option <noindex/> conflicts with <splitindex/>" );
        }
    }

    /**
     * This method is checking to see if the artifacts that can't be resolved are all
     * part of this reactor. This is done to prevent a chicken or egg scenario with
     * fresh projects. See MJAVADOC-116 for more info.
     *
     * @param dependencyArtifacts the sibling projects in the reactor
     * @param missing the artifacts that can't be found
     * @return true if ALL missing artifacts are found in the reactor.
     * @see DefaultPluginManager#checkRequiredMavenVersion( plugin, localRepository, remoteRepositories )
     */
    private boolean checkMissingArtifactsInReactor( Collection dependencyArtifacts, Collection missing )
    {
        Set foundInReactor = new HashSet();
        Iterator iter = missing.iterator();
        while ( iter.hasNext() )
        {
            Artifact mArtifact = (Artifact) iter.next();
            Iterator pIter = reactorProjects.iterator();
            while ( pIter.hasNext() )
            {
                MavenProject p = (MavenProject) pIter.next();
                if ( p.getArtifactId().equals( mArtifact.getArtifactId() )
                    && p.getGroupId().equals( mArtifact.getGroupId() )
                    && p.getVersion().equals( mArtifact.getVersion() ) )
                {
                    getLog()
                        .warn(
                               "The dependency: [" + p.getId()
                                   + "} can't be resolved but has been found in the reactor (probably snapshots).\n"
                                   + "This dependency has been excluded from the Javadoc classpath. "
                                   + "You should rerun javadoc after executing mvn install." );

                    //found it, move on.
                    foundInReactor.add( p );
                    break;
                }
            }
        }

        //if all of them have been found, we can continue.
        return foundInReactor.size() == missing.size();
    }
}
