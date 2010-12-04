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

import static org.codehaus.plexus.util.IOUtil.close;
import static org.apache.maven.plugin.javadoc.JavadocUtil.toList;
import static org.apache.maven.plugin.javadoc.JavadocUtil.toRelative;
import static org.apache.maven.plugin.javadoc.JavadocUtil.isNotEmpty;
import static org.apache.maven.plugin.javadoc.JavadocUtil.isEmpty;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.javadoc.options.BootclasspathArtifact;
import org.apache.maven.plugin.javadoc.options.DocletArtifact;
import org.apache.maven.plugin.javadoc.options.Group;
import org.apache.maven.plugin.javadoc.options.JavadocOptions;
import org.apache.maven.plugin.javadoc.options.JavadocPathArtifact;
import org.apache.maven.plugin.javadoc.options.OfflineLink;
import org.apache.maven.plugin.javadoc.options.ResourcesArtifact;
import org.apache.maven.plugin.javadoc.options.Tag;
import org.apache.maven.plugin.javadoc.options.Taglet;
import org.apache.maven.plugin.javadoc.options.TagletArtifact;
import org.apache.maven.plugin.javadoc.options.io.xpp3.JavadocOptionsXpp3Writer;
import org.apache.maven.plugin.javadoc.resolver.JavadocBundle;
import org.apache.maven.plugin.javadoc.resolver.ResourceResolver;
import org.apache.maven.plugin.javadoc.resolver.SourceResolverConfig;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.apache.maven.wagon.PathUtils;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Base class with majority of Javadoc functionalities.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.0
 * @requiresDependencyResolution compile
 * @see <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html">
 * The Java API Documentation Generator, 1.4.2</a>
 */
public abstract class AbstractJavadocMojo
    extends AbstractMojo
{
    /**
     * Classifier used in the name of the javadoc-options XML file, and in the resources bundle 
     * artifact that gets attached to the project. This one is used for non-test javadocs.
     * 
     * @since 2.7
     * @see #TEST_JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER
     */
    public static final String JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER = "javadoc-resources";
    
    /**
     * Classifier used in the name of the javadoc-options XML file, and in the resources bundle 
     * artifact that gets attached to the project. This one is used for test-javadocs.
     * 
     * @since 2.7
     * @see #JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER
     */
    public static final String TEST_JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER = "test-javadoc-resources";

    /**
     * The default Javadoc API urls according the
     * <a href="http://java.sun.com/reference/api/index.html">Sun API Specifications</a>:
     * <pre>
     * &lt;javaApiLinks&gt;
     *   &lt;property&gt;
     *     &lt;name&gt;api_1.3&lt;/name&gt;
     *     &lt;value&gt;http://download.oracle.com/javase/1.3/docs/api/&lt;/value&gt;
     *   &lt;/property&gt;
     *   &lt;property&gt;
     *     &lt;name&gt;api_1.4&lt;/name&gt;
     *     &lt;value&gt;http://download.oracle.com/javase/1.4.2/docs/api/&lt;/value&gt;
     *   &lt;/property&gt;
     *   &lt;property&gt;
     *     &lt;name&gt;api_1.5&lt;/name&gt;
     *     &lt;value&gt;http://download.oracle.com/javase/1.5.0/docs/api/&lt;/value&gt;
     *   &lt;/property&gt;
     *   &lt;property&gt;
     *     &lt;name&gt;api_1.6&lt;/name&gt;
     *     &lt;value&gt;http://download.oracle.com/javase/6/docs/api/&lt;/value&gt;
     *   &lt;/property&gt;
     * &lt;/javaApiLinks&gt;
     * </pre>
     *
     * @since 2.6
     */
    public static final Properties DEFAULT_JAVA_API_LINKS = new Properties();

    /** The Javadoc script file name when <code>debug</code> parameter is on, i.e. javadoc.bat or javadoc.sh */
    protected static final String DEBUG_JAVADOC_SCRIPT_NAME =
        "javadoc." + ( SystemUtils.IS_OS_WINDOWS ? "bat" : "sh" );

    /** The <code>options</code> file name in the output directory when calling:
     * <code>javadoc.exe(or .sh) &#x40;options &#x40;packages | &#x40;argfile | &#x40;files</code> */
    protected static final String OPTIONS_FILE_NAME = "options";

    /** The <code>packages</code> file name in the output directory when calling:
     * <code>javadoc.exe(or .sh) &#x40;options &#x40;packages | &#x40;argfile | &#x40;files</code> */
    protected static final String PACKAGES_FILE_NAME = "packages";

    /** The <code>argfile</code> file name in the output directory when calling:
     * <code>javadoc.exe(or .sh) &#x40;options &#x40;packages | &#x40;argfile | &#x40;files</code> */
    protected static final String ARGFILE_FILE_NAME = "argfile";

    /** The <code>files</code> file name in the output directory when calling:
     * <code>javadoc.exe(or .sh) &#x40;options &#x40;packages | &#x40;argfile | &#x40;files</code> */
    protected static final String FILES_FILE_NAME = "files";

    /** The current class directory */
    private static final String RESOURCE_DIR = ClassUtils.getPackageName( JavadocReport.class ).replace( '.', '/' );

    /** Default css file name */
    private static final String DEFAULT_CSS_NAME = "stylesheet.css";

    /** Default location for css */
    private static final String RESOURCE_CSS_DIR = RESOURCE_DIR + "/css";

    /**
     * For Javadoc options appears since Java 1.4.
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">
     * What's New in Javadoc 1.4</a>
     * @since 2.1
     */
    private static final float SINCE_JAVADOC_1_4 = 1.4f;

    /**
     * For Javadoc options appears since Java 1.4.2.
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.2.html#commandlineoptions">
     * What's New in Javadoc 1.4.2</a>
     * @since 2.1
     */
    private static final float SINCE_JAVADOC_1_4_2 = 1.42f;

    /**
     * For Javadoc options appears since Java 5.0.
     * See <a href="http://download.oracle.com/javase/1.5.0/docs/guide/javadoc/whatsnew-1.5.0.html#commandlineoptions">
     * What's New in Javadoc 5.0</a>
     * @since 2.1
     */
    private static final float SINCE_JAVADOC_1_5 = 1.5f;

    /**
     * For Javadoc options appears since Java 6.0.
     * See <a href="http://download.oracle.com/javase/6/docs/technotes/guides/javadoc/index.html">
     * Javadoc Technology</a>
     * @since 2.4
     */
    private static final float SINCE_JAVADOC_1_6 = 1.6f;

    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Archiver manager
     *
     * @since 2.5
     * @component
     */
    private ArchiverManager archiverManager;

    /**
     * Factory for creating artifact objects
     *
     * @component
     */
    private ArtifactFactory factory;

    /**
     * Used to resolve artifacts of aggregated modules
     *
     * @since 2.1
     * @component
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * Used for resolving artifacts
     *
     * @component
     */
    private ArtifactResolver resolver;

    /**
     * Project builder
     *
     * @since 2.5
     * @component
     */
    private MavenProjectBuilder mavenProjectBuilder;

    /** @component */
    private ToolchainManager toolchainManager;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The current build session instance. This is used for
     * toolchain manager API calls.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

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
     * Specify if the Javadoc should operate in offline mode.
     *
     * @parameter default-value="${settings.offline}"
     * @required
     * @readonly
     */
    private boolean isOffline;

    /**
     * Specifies the Javadoc resources directory to be included in the Javadoc (i.e. package.html, images...).
     * <br/>
     * Could be used in addition of <code>docfilessubdirs</code> parameter.
     * <br/>
     * See <a href="#docfilessubdirs">docfilessubdirs</a>.
     *
     * @since 2.1
     * @parameter expression="${basedir}/src/main/javadoc"
     * @see #docfilessubdirs
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
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#J">Jflag</a>.
     * <br/>
     * See <a href="http://java.sun.com/javase/technologies/hotspot/vmoptions.jsp">vmoptions</a>.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/guide/net/properties.html">Networking Properties</a>.
     *
     * @since 2.3
     * @parameter expression="${additionalJOption}"
     */
    private String additionalJOption;

    /**
     * A list of artifacts containing resources which should be copied into the
     * Javadoc output directory (like stylesheets, icons, etc.).
     * <br/>
     * Example:
     * <pre>
     * &lt;resourcesArtifacts&gt;
     * &nbsp;&nbsp;&lt;resourcesArtifact&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;groupId&gt;external.group.id&lt;/groupId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;artifactId&gt;external-resources&lt;/artifactId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;version&gt;1.0&lt;/version&gt;
     * &nbsp;&nbsp;&lt;/resourcesArtifact&gt;
     * &lt;/resourcesArtifacts&gt;
     * </pre>
     * <br/>
     * See <a href="./apidocs/org/apache/maven/plugin/javadoc/options/ResourcesArtifact.html">Javadoc</a>.
     * <br/>
     *
     * @since 2.5
     * @parameter expression="${resourcesArtifacts}"
     */
    private ResourcesArtifact[] resourcesArtifacts;

    /**
     * The local repository where the artifacts are located.
     *
     * @parameter expression="${localRepository}"
     */
    private ArtifactRepository localRepository;

    /**
     * The remote repositories where artifacts are located.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     */
    private List<ArtifactRepository> remoteRepositories;

    /**
     * The projects in the reactor for aggregation report.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    private List<MavenProject> reactorProjects;

    /**
     * Whether to build an aggregated report at the root, or build individual reports.
     *
     * @parameter expression="${aggregate}" default-value="false"
     * @deprecated since 2.5. Use the goals <code>javadoc:aggregate</code> and <code>javadoc:test-aggregate</code> instead.
     */
    protected boolean aggregate;

    /**
     * Set this to <code>true</code> to debug the Javadoc plugin. With this, <code>javadoc.bat(or.sh)</code>,
     * <code>options</code>, <code>@packages</code> or <code>argfile</code> files are provided in the output directory.
     * <br/>
     *
     * @since 2.1
     * @parameter expression="${debug}" default-value="false"
     */
    private boolean debug;

    /**
     * Sets the absolute path of the Javadoc Tool executable to use. Since version 2.5, a mere directory specification
     * is sufficient to have the plugin use "javadoc" or "javadoc.exe" respectively from this directory.
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

    /**
     * Version of the Javadoc Tool executable to use as float.
     */
    private float fJavadocVersion = 0.0f;

    /**
     * Specifies whether the Javadoc generation should be skipped.
     *
     * @since 2.5
     * @parameter expression="${maven.javadoc.skip}" default-value="false"
     */
    protected boolean skip;

    /**
     * Specifies whether the build will continue even if there are errors.
     *
     * @parameter expression="${maven.javadoc.failOnError}" default-value="true"
     * @since 2.5
     */
    protected boolean failOnError;

    /**
     * Specifies to use the <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#standard">
     * options provided by the Standard Doclet</a> for a custom doclet.
     * <br/>
     * Example:
     * <pre>
     * &lt;docletArtifacts&gt;
     * &nbsp;&nbsp;&lt;docletArtifact&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;groupId&gt;com.sun.tools.doclets&lt;/groupId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;artifactId&gt;doccheck&lt;/artifactId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;version&gt;1.2b2&lt;/version&gt;
     * &nbsp;&nbsp;&lt;/docletArtifact&gt;
     * &lt;/docletArtifacts&gt;
     * &lt;useStandardDocletOptions&gt;true&lt;/useStandardDocletOptions&gt;
     * </pre>
     *
     * @parameter expression="${useStandardDocletOptions}" default-value="true"
     * @since 2.5
     */
    protected boolean useStandardDocletOptions;

    /**
     * Detect the Javadoc links for all dependencies defined in the project. The detection is based on the default
     * Maven conventions, i.e.: <code>${project.url}/apidocs</code>.
     * <br/>
     * For instance, if the project has a dependency to
     * <a href="http://commons.apache.org/lang/">Apache Commons Lang</a> i.e.:
     * <pre>
     * &lt;dependency&gt;
     *   &lt;groupId&gt;commons-lang&lt;/groupId&gt;
     *   &lt;artifactId&gt;commons-lang&lt;/artifactId&gt;
     * &lt;/dependency&gt;
     * </pre>
     * The added Javadoc <code>-link</code> parameter will be <code>http://commons.apache.org/lang/apidocs</code>.
     *
     * @parameter expression="${detectLinks}" default-value="false"
     * @see #links
     * @since 2.6
     */
    private boolean detectLinks;

    /**
     * Detect the links for all modules defined in the project.
     * <br/>
     * If {@link #reactorProjects} is defined in a non-aggregator way, it generates default offline links
     * between modules based on the defined project's urls. For instance, if a parent project has two projects
     * <code>module1</code> and <code>module2</code>, the <code>-linkoffline</code> will be:
     * <br/>
     * The added Javadoc <code>-linkoffline</code> parameter for <b>module1</b> will be
     * <code>/absolute/path/to/</code><b>module2</b><code>/target/site/apidocs</code>
     * <br/>
     * The added Javadoc <code>-linkoffline</code> parameter for <b>module2</b> will be
     * <code>/absolute/path/to/</code><b>module1</b><code>/target/site/apidocs</code>
     *
     * @parameter expression="${detectOfflineLinks}" default-value="true"
     * @see #offlineLinks
     * @since 2.6
     */
    private boolean detectOfflineLinks;

    /**
     * Detect the Java API link for the current build, i.e. <code>http://download.oracle.com/javase/1.4.2/docs/api/</code>
     * for Java source 1.4.
     * <br/>
     * By default, the goal detects the Javadoc API link depending the value of the <code>source</code>
     * parameter in the <code>org.apache.maven.plugins:maven-compiler-plugin</code>
     * (defined in <code>${project.build.plugins}</code> or in <code>${project.build.pluginManagement}</code>),
     * or try to compute it from the {@link #javadocExecutable} version.
     * <br/>
     * See <a href="./apidocs/org/apache/maven/plugin/javadoc/AbstractJavadocMojo.html#DEFAULT_JAVA_API_LINKS">Javadoc</a> for the default values.
     * <br/>
     *
     * @parameter expression="${detectJavaApiLink}" default-value="true"
     * @see #links
     * @see #javaApiLinks
     * @see #DEFAULT_JAVA_API_LINKS
     * @since 2.6
     */
    private boolean detectJavaApiLink;

    /**
     * Use this parameter <b>only</b> if the <a href="http://java.sun.com/reference/api/index.html">Sun Javadoc API</a>
     * urls have been changed or to use custom urls for Javadoc API url.
     * <br/>
     * See <a href="./apidocs/org/apache/maven/plugin/javadoc/AbstractJavadocMojo.html#DEFAULT_JAVA_API_LINKS">Javadoc</a>
     * for the default values.
     * <br/>
     *
     * @parameter expression="${javaApiLinks}"
     * @see #DEFAULT_JAVA_API_LINKS
     * @since 2.6
     */
    private Properties javaApiLinks;

    // ----------------------------------------------------------------------
    // Javadoc Options - all alphabetical
    // ----------------------------------------------------------------------

    /**
     * Specifies the paths where the boot classes reside. The <code>bootclasspath</code> can contain multiple paths
     * by separating them with a colon (<code>:</code>) or a semi-colon (<code>;</code>).
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#bootclasspath">bootclasspath</a>.
     * <br/>
     *
     * @parameter expression="${bootclasspath}"
     * @since 2.5
     */
    private String bootclasspath;

    /**
     * Specifies the artifacts where the boot classes reside.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#bootclasspath">bootclasspath</a>.
     * <br/>
     * Example:
     * <pre>
     * &lt;bootclasspathArtifacts&gt;
     * &nbsp;&nbsp;&lt;bootclasspathArtifact&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;groupId&gt;my-groupId&lt;/groupId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;artifactId&gt;my-artifactId&lt;/artifactId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;version&gt;my-version&lt;/version&gt;
     * &nbsp;&nbsp;&lt;/bootclasspathArtifact&gt;
     * &lt;/bootclasspathArtifacts&gt;
     * </pre>
     * <br/>
     * See <a href="./apidocs/org/apache/maven/plugin/javadoc/options/BootclasspathArtifact.html">Javadoc</a>.
     * <br/>
     *
     * @parameter expression="${bootclasspathArtifacts}"
     * @since 2.5
     */
    private BootclasspathArtifact[] bootclasspathArtifacts;

    /**
     * Uses the sentence break iterator to determine the end of the first sentence.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#breakiterator">breakiterator</a>.
     * <br/>
     * Since <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     * <br/>
     *
     * @parameter expression="${breakiterator}" default-value="false"
     */
    private boolean breakiterator;

    /**
     * Specifies the class file that starts the doclet used in generating the documentation.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#doclet">doclet</a>.
     *
     * @parameter expression="${doclet}"
     */
    private String doclet;

    /**
     * Specifies the artifact containing the doclet starting class file (specified with the <code>-doclet</code>
     * option).
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#docletpath">docletpath</a>.
     * <br/>
     * Example:
     * <pre>
     * &lt;docletArtifact&gt;
     * &nbsp;&nbsp;&lt;groupId&gt;com.sun.tools.doclets&lt;/groupId&gt;
     * &nbsp;&nbsp;&lt;artifactId&gt;doccheck&lt;/artifactId&gt;
     * &nbsp;&nbsp;&lt;version&gt;1.2b2&lt;/version&gt;
     * &lt;/docletArtifact&gt;
     * </pre>
     * <br/>
     * See <a href="./apidocs/org/apache/maven/plugin/javadoc/options/DocletArtifact.html">Javadoc</a>.
     * <br/>
     *
     * @parameter expression="${docletArtifact}"
     */
    private DocletArtifact docletArtifact;

    /**
     * Specifies multiple artifacts containing the path for the doclet starting class file (specified with the
     * <code>-doclet</code> option).
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#docletpath">docletpath</a>.
     * <br/>
     * Example:
     * <pre>
     * &lt;docletArtifacts&gt;
     * &nbsp;&nbsp;&lt;docletArtifact&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;groupId&gt;com.sun.tools.doclets&lt;/groupId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;artifactId&gt;doccheck&lt;/artifactId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;version&gt;1.2b2&lt;/version&gt;
     * &nbsp;&nbsp;&lt;/docletArtifact&gt;
     * &lt;/docletArtifacts&gt;
     * </pre>
     * <br/>
     * See <a href="./apidocs/org/apache/maven/plugin/javadoc/options/DocletArtifact.html">Javadoc</a>.
     * <br/>
     *
     * @since 2.1
     * @parameter expression="${docletArtifacts}"
     */
    private DocletArtifact[] docletArtifacts;

    /**
     * Specifies the path to the doclet starting class file (specified with the <code>-doclet</code> option) and
     * any jar files it depends on. The <code>docletPath</code> can contain multiple paths by separating them with
     * a colon (<code>:</code>) or a semi-colon (<code>;</code>).
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#docletpath">docletpath</a>.
     *
     * @parameter expression="${docletPath}"
     */
    private String docletPath;

    /**
     * Specifies the encoding name of the source files. If not specificed, the encoding value will be the value of the
     * <code>file.encoding</code> system property.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#encoding">encoding</a>.
     * <br/>
     * <b>Note</b>: In 2.4, the default value was locked to <code>ISO-8859-1</code> to ensure reproducing build, but
     * this was reverted in 2.5.
     * <br/>
     *
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     */
    private String encoding;

    /**
     * Unconditionally excludes the specified packages and their subpackages from the list formed by
     * <code>-subpackages</code>. Multiple packages can be separated by commas (<code>,</code>), colons (<code>:</code>)
     * or semicolons (<code>;</code>).
     * <br/>
     * Example:
     * <pre>
     * &lt;excludePackageNames&gt;*.internal:org.acme.exclude1.*:org.acme.exclude2&lt;/excludePackageNames&gt;
     * </pre>
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#exclude">exclude</a>.
     * <br/>
     * Since <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${excludePackageNames}"
     */
    private String excludePackageNames;

    /**
     * Specifies the directories where extension classes reside. Separate directories in <code>extdirs</code> with a
     * colon (<code>:</code>) or a semi-colon (<code>;</code>).
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#extdirs">extdirs</a>.
     *
     * @parameter expression="${extdirs}"
     */
    private String extdirs;

    /**
     * Specifies the locale that javadoc uses when generating documentation.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#locale">locale</a>.
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
     * This option creates documentation with the appearance and functionality of documentation generated by
     * Javadoc 1.1.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#1.1">1.1</a>.
     * <br/>
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
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#overview">overview</a>.
     * <br/>
     *
     * @parameter expression="${overview}" default-value="${basedir}/src/main/javadoc/overview.html"
     */
    private File overview;

    /**
     * Specifies the proxy host where the javadoc web access in <code>-link</code> would pass through.
     * It defaults to the proxy host of the active proxy set in the <code>settings.xml</code>, otherwise it gets the
     * proxy configuration set in the pom.
     * <br/>
     *
     * @parameter expression="${proxyHost}"
     * @deprecated since 2.4. Instead of, configure an active proxy host in <code>settings.xml</code>.
     */
    private String proxyHost;

    /**
     * Specifies the proxy port where the javadoc web access in <code>-link</code> would pass through.
     * It defaults to the proxy port of the active proxy set in the <code>settings.xml</code>, otherwise it gets the
     * proxy configuration set in the pom.
     * <br/>
     *
     * @parameter expression="${proxyPort}"
     * @deprecated since 2.4. Instead of, configure an active proxy port in <code>settings.xml</code>.
     */
    private int proxyPort;

    /**
     * Shuts off non-error and non-warning messages, leaving only the warnings and errors appear, making them
     * easier to view.
     * <br/>
     * Note: was a standard doclet in Java 1.4.2 (refer to bug ID
     * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4714350">4714350</a>).
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.5.0/docs/tooldocs/windows/javadoc.html#quiet">quiet</a>.
     * <br/>
     * Since Java 5.0.
     * <br/>
     *
     * @parameter expression="${quiet}" default-value="false"
     */
    private boolean quiet;

    /**
     * Specifies the access level for classes and members to show in the Javadocs.
     * Possible values are:
     * <ul>
     * <li><a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#public">public</a>
     * (shows only public classes and members)</li>
     * <li><a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#protected">protected</a>
     * (shows only public and protected classes and members)</li>
     * <li><a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#package">package</a>
     * (shows all classes and members not marked private)</li>
     * <li><a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#private">private</a>
     * (shows all classes and members)</li>
     * </ul>
     * <br/>
     *
     * @parameter expression="${show}" default-value="protected"
     */
    private String show;

    /**
     * Necessary to enable javadoc to handle assertions present in J2SE v 1.4 source code.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#source">source</a>.
     * <br/>
     * Since <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${source}"
     */
    private String source;

    /**
     * Specifies the source paths where the subpackages are located. The <code>sourcepath</code> can contain
     * multiple paths by separating them with a colon (<code>:</code>) or a semi-colon (<code>;</code>).
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#sourcepath">sourcepath</a>.
     *
     * @parameter expression="${sourcepath}"
     */
    private String sourcepath;

    /**
     * Specifies the package directory where javadoc will be executed. Multiple packages can be separated by
     * colons (<code>:</code>).
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#subpackages">subpackages</a>.
     * <br/>
     * Since <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${subpackages}"
     */
    private String subpackages;

    /**
     * Provides more detailed messages while javadoc is running.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#verbose">verbose</a>.
     * <br/>
     *
     * @parameter expression="${verbose}" default-value="false"
     */
    private boolean verbose;

    // ----------------------------------------------------------------------
    // Standard Doclet Options - all alphabetical
    // ----------------------------------------------------------------------

    /**
     * Specifies whether or not the author text is included in the generated Javadocs.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#author">author</a>.
     * <br/>
     *
     * @parameter expression="${author}" default-value="true"
     */
    private boolean author;

    /**
     * Specifies the text to be placed at the bottom of each output file.<br/>
     * If you want to use html you have to put it in a CDATA section, <br/>
     * eg. <code>&lt;![CDATA[Copyright 2005, &lt;a href="http://www.mycompany.com">MyCompany, Inc.&lt;a>]]&gt;</code>
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#bottom">bottom</a>.
     * <br/>
     *
     * @parameter expression="${bottom}"
     * default-value="Copyright &#169; {inceptionYear}-{currentYear} {organizationName}. All Rights Reserved."
     */
    private String bottom;

    /**
     * Specifies the HTML character set for this document. If not specificed, the charset value will be the value of
     * the <code>docencoding</code> parameter.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#charset">charset</a>.
     * <br/>
     *
     * @parameter expression="${charset}"
     */
    private String charset;

    /**
     * Specifies the encoding of the generated HTML files. If not specificed, the docencoding value will be
     * <code>UTF-8</code>.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#docencoding">docencoding</a>.
     *
     * @parameter expression="${docencoding}" default-value="${project.reporting.outputEncoding}"
     */
    private String docencoding;

    /**
     * Enables deep copying of the <code>&#42;&#42;/doc-files</code> directories and the specifc <code>resources</code>
     * directory from the <code>javadocDirectory</code> directory (for instance,
     * <code>src/main/javadoc/com/mycompany/myapp/doc-files</code> and <code>src/main/javadoc/resources</code>).
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#docfilessubdirs">
     * docfilessubdirs</a>.
     * <br/>
     * Since <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     * <br/>
     * See <a href="#javadocDirectory">javadocDirectory</a>.
     * <br/>
     *
     * @parameter expression="${docfilessubdirs}" default-value="false"
     * @see #excludedocfilessubdir
     * @see #javadocDirectory
     */
    private boolean docfilessubdirs;

    /**
     * Specifies the title to be placed near the top of the overview summary file.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#doctitle">doctitle</a>.
     * <br/>
     *
     * @parameter expression="${doctitle}" default-value="${project.name} ${project.version} API"
     */
    private String doctitle;

    /**
     * Excludes any "doc-files" subdirectories with the given names. Multiple patterns can be excluded
     * by separating them with colons (<code>:</code>).
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#excludedocfilessubdir">
     * excludedocfilessubdir</a>.
     * <br/>
     * Since <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${excludedocfilessubdir}"
     * @see #docfilessubdirs
     */
    private String excludedocfilessubdir;

    /**
     * Specifies the footer text to be placed at the bottom of each output file.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#footer">footer</a>.
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
     * Example:
     * <pre>
     * &lt;groups&gt;
     * &nbsp;&nbsp;&lt;group&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;title&gt;Core Packages&lt;/title&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;!-- To includes java.lang, java.lang.ref,
     * &nbsp;&nbsp;&nbsp;&nbsp;java.lang.reflect and only java.util
     * &nbsp;&nbsp;&nbsp;&nbsp;(i.e. not java.util.jar) --&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;packages&gt;java.lang*:java.util&lt;/packages&gt;
     * &nbsp;&nbsp;&lt;/group&gt;
     * &nbsp;&nbsp;&lt;group&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;title&gt;Extension Packages&lt;/title&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;!-- To include javax.accessibility,
     * &nbsp;&nbsp;&nbsp;&nbsp;javax.crypto, ... (among others) --&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;packages&gt;javax.*&lt;/packages&gt;
     * &nbsp;&nbsp;&lt;/group&gt;
     * &lt;/groups&gt;
     * </pre>
     * <b>Note</b>: using <code>java.lang.*</code> for <code>packages</code> would omit the <code>java.lang</code>
     * package but using <code>java.lang*</code> will include it.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#group">group</a>.
     * <br/>
     * See <a href="./apidocs/org/apache/maven/plugin/javadoc/options/Group.html">Javadoc</a>.
     * <br/>
     *
     * @parameter expression="${groups}"
     */
    private Group[] groups;

    /**
     * Specifies the header text to be placed at the top of each output file.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#header">header</a>.
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
     * The <code>helpfile</code> could be an absolute File path.
     * <br/>
     * Since 2.6, it could be also be a path from a resource in the current project source directories
     * (i.e. <code>src/main/java</code>, <code>src/main/resources</code> or <code>src/main/javadoc</code>)
     *  or from a resource in the Javadoc plugin dependencies, for instance:
     * <pre>
     * &lt;helpfile&gt;path/to/your/resource/yourhelp-doc.html&lt;/helpfile&gt;
     * </pre>
     * Where <code>path/to/your/resource/yourhelp-doc.html</code> could be in <code>src/main/javadoc</code>.
     * <pre>
     * &lt;build&gt;
     * &nbsp;&nbsp;&lt;plugins&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;plugin&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;artifactId&gt;maven-javadoc-plugin&lt;/artifactId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;configuration&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;helpfile&gt;path/to/your/resource/yourhelp-doc.html&lt;/helpfile&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;...
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/configuration&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;dependencies&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;dependency&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;groupId&gt;groupId&lt;/groupId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;artifactId&gt;artifactId&lt;/artifactId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;version&gt;version&lt;/version&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/dependency&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/dependencies&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/plugin&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;...
     * &nbsp;&nbsp;&lt;plugins&gt;
     * &lt;/build&gt;
     * </pre>
     * Where <code>path/to/your/resource/yourhelp-doc.html</code> is defined in the
     * <code>groupId:artifactId:version</code> javadoc plugin dependency.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#helpfile">helpfile</a>.
     *
     * @parameter expression="${helpfile}"
     */
    private String helpfile;

    /**
     * Adds HTML meta keyword tags to the generated file for each class.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.5.0/docs/tooldocs/windows/javadoc.html#keywords">keywords</a>.
     * <br/>
     * Since <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.2.html#commandlineoptions">
     * Java 1.4.2</a>.
     * <br/>
     * Since <a href="http://download.oracle.com/javase/1.5.0/docs/guide/javadoc/whatsnew-1.5.0.html#commandlineoptions">
     * Java 5.0</a>.
     * <br/>
     *
     * @since 2.1
     * @parameter expression="${keywords}" default-value="false"
     */
    private boolean keywords;

    /**
     * Creates links to existing javadoc-generated documentation of external referenced classes.
     * <br/>
     * <b>Notes</b>:
     * <ol>
     * <li>only used is {@link #isOffline} is set to <code>false</code>.</li>
     * <li>all given links should have a fetchable <code>/package-list</code> file. For instance:
     * <pre>
     * &lt;links&gt;
     * &nbsp;&nbsp;&lt;link&gt;http://download.oracle.com/javase/1.4.2/docs/api&lt;/link&gt;
     * &lt;links&gt;
     * </pre>
     * will be used because <code>http://download.oracle.com/javase/1.4.2/docs/api/package-list</code> exists.</li>
     * <li>if {@link #detectLinks} is defined, the links between the project dependencies are
     * automatically added.</li>
     * <li>if {@link #detectJavaApiLink} is defined, a Java API link, based on the Java verion of the
     * project's sources, will be added automatically.</li>
     * </ol>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#link">link</a>.
     *
     * @parameter expression="${links}"
     * @see #detectLinks
     * @see #detectJavaApiLink
     */
    protected ArrayList<String> links;

    /**
     * Creates an HTML version of each source file (with line numbers) and adds links to them from the standard
     * HTML documentation.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#linksource">linksource</a>.
     * <br/>
     * Since <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     * <br/>
     *
     * @parameter expression="${linksource}" default-value="false"
     */
    private boolean linksource;

    /**
     * Suppress the entire comment body, including the main description and all tags, generating only declarations.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#nocomment">nocomment</a>.
     * <br/>
     * Since <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     * <br/>
     *
     * @parameter expression="${nocomment}" default-value="false"
     */
    private boolean nocomment;

    /**
     * Prevents the generation of any deprecated API at all in the documentation.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#nodeprecated">nodeprecated</a>.
     * <br/>
     *
     * @parameter expression="${nodeprecated}" default-value="false"
     */
    private boolean nodeprecated;

    /**
     * Prevents the generation of the file containing the list of deprecated APIs (deprecated-list.html) and the
     * link in the navigation bar to that page.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#nodeprecatedlist">
     * nodeprecatedlist</a>.
     * <br/>
     *
     * @parameter expression="${nodeprecatedlist}" default-value="false"
     */
    private boolean nodeprecatedlist;

    /**
     * Omits the HELP link in the navigation bars at the top and bottom of each page of output.
     * <br/>
     * <b>Note</b>: could be in conflict with &lt;helpfile/&gt;.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#nohelp">nohelp</a>.
     * <br/>
     *
     * @parameter expression="${nohelp}" default-value="false"
     */
    private boolean nohelp;

    /**
     * Omits the index from the generated docs.
     * <br/>
     * <b>Note</b>: could be in conflict with &lt;splitindex/&gt;.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#noindex">noindex</a>.
     * <br/>
     *
     * @parameter expression="${noindex}" default-value="false"
     */
    private boolean noindex;

    /**
     * Omits the navigation bar from the generated docs.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#nonavbar">nonavbar</a>.
     * <br/>
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
     * <br/>
     *
     * @since 2.4
     * @parameter expression="${nooverview}" default-value="false"
     */
    private boolean nooverview;

    /**
     * Omits qualifying package name from ahead of class names in output.
     * Example:
     * <pre>
     * &lt;noqualifier&gt;all&lt;/noqualifier&gt;
     * or
     * &lt;noqualifier&gt;packagename1:packagename2&lt;/noqualifier&gt;
     * </pre>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#noqualifier">noqualifier</a>.
     * <br/>
     * Since <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${noqualifier}"
     */
    private String noqualifier;

    /**
     * Omits from the generated docs the "Since" sections associated with the since tags.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#nosince">nosince</a>.
     * <br/>
     *
     * @parameter expression="${nosince}" default-value="false"
     */
    private boolean nosince;

    /**
     * Suppresses the timestamp, which is hidden in an HTML comment in the generated HTML near the top of each page.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.5.0/docs/tooldocs/windows/javadoc.html#notimestamp">notimestamp</a>.
     * <br/>
     * Since <a href="http://download.oracle.com/javase/1.5.0/docs/guide/javadoc/whatsnew-1.5.0.html#commandlineoptions">
     * Java 5.0</a>.
     * <br/>
     *
     * @since 2.1
     * @parameter expression="${notimestamp}" default-value="false"
     */
    private boolean notimestamp;

    /**
     * Omits the class/interface hierarchy pages from the generated docs.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#notree">notree</a>.
     * <br/>
     *
     * @parameter expression="${notree}" default-value="false"
     */
    private boolean notree;

    /**
     * This option is a variation of <code>-link</code>; they both create links to javadoc-generated documentation
     * for external referenced classes.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#linkoffline">linkoffline</a>.
     * <br/>
     * Example:
     * <pre>
     * &lt;offlineLinks&gt;
     * &nbsp;&nbsp;&lt;offlineLink&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;url&gt;http://download.oracle.com/javase/1.5.0/docs/api/&lt;/url&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;location&gt;../javadoc/jdk-5.0/&lt;/location&gt;
     * &nbsp;&nbsp;&lt;/offlineLink&gt;
     * &lt;/offlineLinks&gt;
     * </pre>
     * <br/>
     * <b>Note</b>: if {@link #detectOfflineLinks} is defined, the offline links between the project modules are
     * automatically added if the goal is calling in a non-aggregator way.
     * <br/>
     * See <a href="./apidocs/org/apache/maven/plugin/javadoc/options/OfflineLink.html">Javadoc</a>.
     * <br/>
     *
     * @parameter expression="${offlineLinks}"
     */
    private OfflineLink[] offlineLinks;

    /**
     * Specifies the destination directory where javadoc saves the generated HTML files.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#d">d</a>.
     * <br/>
     *
     * @parameter expression="${destDir}" alias="destDir" default-value="${project.build.directory}/apidocs"
     * @required
     */
    protected File outputDirectory;

    /**
     * Specify the text for upper left frame.
     * <br/>
     * Since <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.2.html#commandlineoptions">
     * Java 1.4.2</a>.
     *
     * @since 2.1
     * @parameter expression="${packagesheader}"
     */
    private String packagesheader;

    /**
     * Generates compile-time warnings for missing serial tags.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#serialwarn">serialwarn</a>
     * <br/>
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
     * Since <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.2.html#commandlineoptions">
     * 1.4.2</a>.
     * <br/>
     * Since Java 5.0.
     *
     * @since 2.1
     * @parameter expression="${sourcetab}" alias="linksourcetab"
     */
    private int sourcetab;

    /**
     * Splits the index file into multiple files, alphabetically, one file per letter, plus a file for any index
     * entries that start with non-alphabetical characters.
     * <br/>
     * <b>Note</b>: could be in conflict with &lt;noindex/&gt;.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#splitindex">splitindex</a>.
     * <br/>
     *
     * @parameter expression="${splitindex}" default-value="false"
     */
    private boolean splitindex;

    /**
     * Specifies whether the stylesheet to be used is the <code>maven</code>'s javadoc stylesheet or
     * <code>java</code>'s default stylesheet when a <i>stylesheetfile</i> parameter is not specified.
     * <br/>
     * Possible values: <code>maven<code> or <code>java</code>.
     * <br/>
     *
     * @parameter expression="${stylesheet}" default-value="java"
     */
    private String stylesheet;

    /**
     * Specifies the path of an alternate HTML stylesheet file.
     * <br/>
     * The <code>stylesheetfile</code> could be an absolute File path.
     * <br/>
     * Since 2.6, it could be also be a path from a resource in the current project source directories
     * (i.e. <code>src/main/java</code>, <code>src/main/resources</code> or <code>src/main/javadoc</code>)
     *  or from a resource in the Javadoc plugin dependencies, for instance:
     * <pre>
     * &lt;stylesheetfile&gt;path/to/your/resource/yourstylesheet.css&lt;/stylesheetfile&gt;
     * </pre>
     * Where <code>path/to/your/resource/yourstylesheet.css</code> could be in <code>src/main/javadoc</code>.
     * <pre>
     * &lt;build&gt;
     * &nbsp;&nbsp;&lt;plugins&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;plugin&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;artifactId&gt;maven-javadoc-plugin&lt;/artifactId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;configuration&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;stylesheetfile&gt;path/to/your/resource/yourstylesheet.css&lt;/stylesheetfile&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;...
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/configuration&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;dependencies&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;dependency&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;groupId&gt;groupId&lt;/groupId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;artifactId&gt;artifactId&lt;/artifactId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;version&gt;version&lt;/version&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/dependency&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/dependencies&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/plugin&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;...
     * &nbsp;&nbsp;&lt;plugins&gt;
     * &lt;/build&gt;
     * </pre>
     * Where <code>path/to/your/resource/yourstylesheet.css</code> is defined in the
     * <code>groupId:artifactId:version</code> javadoc plugin dependency.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#stylesheetfile">
     * stylesheetfile</a>.
     *
     * @parameter expression="${stylesheetfile}"
     */
    private String stylesheetfile;

    /**
     * Specifies the class file that starts the taglet used in generating the documentation for that tag.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#taglet">taglet</a>.
     * <br/>
     * Since <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${taglet}"
     */
    private String taglet;

    /**
     * Specifies the Taglet artifact containing the taglet class files (.class).
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#tagletpath">tagletpath</a>.
     * <br/>
     * Example:
     * <pre>
     * &lt;taglets&gt;
     * &nbsp;&nbsp;&lt;taglet&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;tagletClass&gt;com.sun.tools.doclets.ToDoTaglet&lt;/tagletClass&gt;
     * &nbsp;&nbsp;&lt;/taglet&gt;
     * &nbsp;&nbsp;&lt;taglet&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;tagletClass&gt;package.to.AnotherTagletClass&lt;/tagletClass&gt;
     * &nbsp;&nbsp;&lt;/taglet&gt;
     * &nbsp;&nbsp;...
     * &lt;/taglets&gt;
     * &lt;tagletArtifact&gt;
     * &nbsp;&nbsp;&lt;groupId&gt;group-Taglet&lt;/groupId&gt;
     * &nbsp;&nbsp;&lt;artifactId&gt;artifact-Taglet&lt;/artifactId&gt;
     * &nbsp;&nbsp;&lt;version&gt;version-Taglet&lt;/version&gt;
     * &lt;/tagletArtifact&gt;
     * </pre>
     * <br/>
     * See <a href="./apidocs/org/apache/maven/plugin/javadoc/options/TagletArtifact.html">Javadoc</a>.
     * <br/>
     *
     * @since 2.1
     * @parameter expression="${tagletArtifact}"
     */
    private TagletArtifact tagletArtifact;

    /**
     * Specifies several Taglet artifacts containing the taglet class files (.class). These taglets class names will be
     * auto-detect and so no need to specify them.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#taglet">taglet</a>.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#tagletpath">tagletpath</a>.
     * <br/>
     * Example:
     * <pre>
     * &lt;tagletArtifacts&gt;
     * &nbsp;&nbsp;&lt;tagletArtifact&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;groupId&gt;group-Taglet&lt;/groupId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;artifactId&gt;artifact-Taglet&lt;/artifactId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;version&gt;version-Taglet&lt;/version&gt;
     * &nbsp;&nbsp;&lt;/tagletArtifact&gt;
     * &nbsp;&nbsp;...
     * &lt;/tagletArtifacts&gt;
     * </pre>
     * <br/>
     * See <a href="./apidocs/org/apache/maven/plugin/javadoc/options/TagletArtifact.html">Javadoc</a>.
     * <br/>
     *
     * @since 2.5
     * @parameter expression="${tagletArtifacts}"
     */
    private TagletArtifact[] tagletArtifacts;

    /**
     * Specifies the search paths for finding taglet class files (.class). The <code>tagletpath</code> can contain
     * multiple paths by separating them with a colon (<code>:</code>) or a semi-colon (<code>;</code>).
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#tagletpath">tagletpath</a>.
     * <br/>
     * Since <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     *
     * @parameter expression="${tagletpath}"
     */
    private String tagletpath;

    /**
     * Enables the Javadoc tool to interpret multiple taglets.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#taglet">taglet</a>.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#tagletpath">tagletpath</a>.
     * <br/>
     * Example:
     * <pre>
     * &lt;taglets&gt;
     * &nbsp;&nbsp;&lt;taglet&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;tagletClass&gt;com.sun.tools.doclets.ToDoTaglet&lt;/tagletClass&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;!--&lt;tagletpath&gt;/home/taglets&lt;/tagletpath&gt;--&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;tagletArtifact&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;groupId&gt;group-Taglet&lt;/groupId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;artifactId&gt;artifact-Taglet&lt;/artifactId&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;version&gt;version-Taglet&lt;/version&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/tagletArtifact&gt;
     * &nbsp;&nbsp;&lt;/taglet&gt;
     * &lt;/taglets&gt;
     * </pre>
     * <br/>
     * See <a href="./apidocs/org/apache/maven/plugin/javadoc/options/Taglet.html">Javadoc</a>.
     * <br/>
     *
     * @since 2.1
     * @parameter expression="${taglets}"
     */
    private Taglet[] taglets;

    /**
     * Enables the Javadoc tool to interpret a simple, one-argument custom block tag tagname in doc comments.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#tag">tag</a>.
     * <br/>
     * Since <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#summary">Java 1.4</a>.
     * <br/>
     * Example:
     * <pre>
     * &lt;tags&gt;
     * &nbsp;&nbsp;&lt;tag&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;name&gt;todo&lt;/name&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;placement&gt;a&lt;/placement&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;head&gt;To Do:&lt;/head&gt;
     * &nbsp;&nbsp;&lt;/tag&gt;
     * &lt;/tags&gt;
     * </pre>
     * <b>Note</b>: the placement should be a combinaison of Xaoptcmf letters:
     * <ul>
     *   <li><b><code>X</code></b> (disable tag)</li>
     *   <li><b><code>a</code></b> (all)</li>
     *   <li><b><code>o</code></b> (overview)</li>
     *   <li><b><code>p</code></b> (packages)</li>
     *   <li><b><code>t</code></b> (types, that is classes and interfaces)</li>
     *   <li><b><code>c</code></b> (constructors)</li>
     *   <li><b><code>m</code></b> (methods)</li>
     *   <li><b><code>f</code></b> (fields)</li>
     * </ul>
     * See <a href="./apidocs/org/apache/maven/plugin/javadoc/options/Tag.html">Javadoc</a>.
     * <br/>
     *
     * @parameter expression="${tags}"
     */
    private Tag[] tags;

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
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#use">use</a>.
     * <br/>
     *
     * @parameter expression="${use}" default-value="true"
     */
    private boolean use;

    /**
     * Includes the version text in the generated docs.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#version">version</a>.
     * <br/>
     *
     * @parameter expression="${version}" default-value="true"
     */
    private boolean version;

    /**
     * Specifies the title to be placed in the HTML title tag.
     * <br/>
     * See <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#windowtitle">windowtitle</a>.
     * <br/>
     *
     * @parameter expression="${windowtitle}" default-value="${project.name} ${project.version} API"
     */
    private String windowtitle;

    /**
     * Whether dependency -sources jars should be resolved and included as source paths for javadoc generation.
     * This is useful when creating javadocs for a distribution project.
     * 
     * @parameter default-value="false"
     * @since 2.7
     */
    private boolean includeDependencySources;

    /**
     * Directory where unpacked project sources / test-sources should be cached.
     *
     * @parameter default-value="${project.build.directory}/distro-javadoc-sources"
     * @since 2.7
     * @see #includeDependencySources
     */
    private File sourceDependencyCacheDir;

    /**
     * Whether to include transitive dependencies in the list of dependency -sources jars to include
     * in this javadoc run.
     * 
     * @parameter default-value="false"
     * @since 2.7
     * @see #includeDependencySources
     */
    private boolean includeTransitiveDependencySources;
    
    /**
     * List of included dependency-source patterns. Example: org.apache.maven:*
     *
     * 
     * @parameter
     * @since 2.7
     * @see #includeDependencySources
     */
    private List<String> dependencySourceIncludes;

    /**
     * List of excluded dependency-source patterns. Example: org.apache.maven.shared:*
     *
     * 
     * @parameter
     * @since 2.7
     * @see #includeDependencySources
     */
    private List<String> dependencySourceExcludes;
    
    /**
     * Directory into which assembled {@link JavadocOptions} instances will be written before they
     * are added to javadoc resources bundles.
     * 
     * @parameter default-value="${project.build.directory}/javadoc-bundle-options"
     * @readonly
     * @since 2.7
     */
    private File javadocOptionsDir;

    /**
     * Transient variable to allow lazy-resolution of javadoc bundles from dependencies, so they can
     * be used at various points in the javadoc generation process.
     * 
     * @since 2.7
     */
    private transient List<JavadocBundle> dependencyJavadocBundles;
    
    // ----------------------------------------------------------------------
    // static
    // ----------------------------------------------------------------------

    static
    {
        DEFAULT_JAVA_API_LINKS.put( "api_1.3", "http://download.oracle.com/javase/1.3/docs/api/" );
        DEFAULT_JAVA_API_LINKS.put( "api_1.4", "http://download.oracle.com/javase/1.4.2/docs/api/" );
        DEFAULT_JAVA_API_LINKS.put( "api_1.5", "http://download.oracle.com/javase/1.5.0/docs/api/" );
        DEFAULT_JAVA_API_LINKS.put( "api_1.6", "http://download.oracle.com/javase/6/docs/api/" );
    }

    // ----------------------------------------------------------------------
    // protected methods
    // ----------------------------------------------------------------------

    /**
     * Indicates whether this goal is flagged with <code>@aggregator</code>.
     *
     * @return <code>true</code> if the goal is designed as an aggregator, <code>false</code> otherwise.
     * @see AggregatorJavadocReport
     * @see AggregatorTestJavadocReport
     */
    protected boolean isAggregator()
    {
        return false;
    }

    /**
     * @return the output directory
     */
    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsoluteFile().toString();
    }
    
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * @param p not null maven project
     * @return the list of directories where compiled classes are placed for the given project. These dirs are
     * added in the javadoc classpath.
     */
    protected List<String> getProjectBuildOutputDirs( MavenProject p )
    {
        if ( StringUtils.isEmpty( p.getBuild().getOutputDirectory() ) )
        {
            return Collections.emptyList();
        }

        return Collections.singletonList( p.getBuild().getOutputDirectory() );
    }

    /**
     * @param p not null maven project
     * @return the list of source paths for the given project
     */
    protected List<String> getProjectSourceRoots( MavenProject p )
    {
        if ( "pom".equals( p.getPackaging().toLowerCase() ) )
        {
            return Collections.emptyList();
        }

        return ( p.getCompileSourceRoots() == null ? Collections.EMPTY_LIST
                        : new LinkedList( p.getCompileSourceRoots() ) );
    }

    /**
     * @param p not null maven project
     * @return the list of source paths for the execution project of the given project
     */
    protected List<String> getExecutionProjectSourceRoots( MavenProject p )
    {
        if ( "pom".equals( p.getExecutionProject().getPackaging().toLowerCase() ) )
        {
            return Collections.emptyList();
        }

        return ( p.getExecutionProject().getCompileSourceRoots() == null ? Collections.EMPTY_LIST
                        : new LinkedList( p.getExecutionProject().getCompileSourceRoots() ) );
    }

    /**
     * @param p not null maven project
     * @return the list of artifacts for the given project
     */
    protected List<Artifact> getProjectArtifacts( MavenProject p )
    {
        return ( p.getCompileArtifacts() == null ? Collections.EMPTY_LIST
                        : new LinkedList<Artifact>( p.getCompileArtifacts() ) );
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
     * @return the charset attribute or the value of {@link #getDocencoding()} if <code>null</code>.
     */
    private String getCharset()
    {
        return ( StringUtils.isEmpty( charset ) ) ? getDocencoding() : charset;
    }

    /**
     * @return the docencoding attribute or <code>UTF-8</code> if <code>null</code>.
     */
    private String getDocencoding()
    {
        return ( StringUtils.isEmpty( docencoding ) ) ? ReaderFactory.UTF_8 : docencoding;
    }

    /**
     * @return the encoding attribute or the value of <code>file.encoding</code> system property if <code>null</code>.
     */
    private String getEncoding()
    {
        return ( StringUtils.isEmpty( encoding ) ) ? ReaderFactory.FILE_ENCODING : encoding;
    }

    /**
     * The <a href="package-summary.html">package documentation</a> details the
     * Javadoc Options used by this Plugin.
     *
     * @param unusedLocale the wanted locale (actually unused).
     * @throws MavenReportException if any
     */
    protected void executeReport( Locale unusedLocale )
        throws MavenReportException
    {
        if ( skip )
        {
            getLog().info( "Skipping javadoc generation" );
            return;
        }

        if ( isAggregator() && !project.isExecutionRoot() )
        {
            return;
        }

        if ( getLog().isDebugEnabled() )
        {
            this.debug = true;
        }

        // NOTE: Always generate this file, to allow javadocs from modules to be aggregated via
        // useDependencySources in a distro module build.
        try
        {
            buildJavadocOptions();
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Failed to generate javadoc options file: " + e.getMessage(), e );
        }
        
        List<String> sourcePaths = getSourcePaths();
        List<String> files = getFiles( sourcePaths );
        if ( !canGenerateReport( files ) )
        {
            return;
        }

        List<String> packageNames = getPackageNames( sourcePaths, files );
        List<String> filesWithUnnamedPackages = getFilesWithUnnamedPackages( sourcePaths, files );

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
        setFJavadocVersion( new File( jExecutable ) );

        // ----------------------------------------------------------------------
        // Javadoc output directory as File
        // ----------------------------------------------------------------------

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
        // Copy all resources
        // ----------------------------------------------------------------------

        copyAllResources( javadocOutputDirectory );

        // ----------------------------------------------------------------------
        // Create command line for Javadoc
        // ----------------------------------------------------------------------

        Commandline cmd = new Commandline();
        cmd.getShell().setQuotedArgumentsEnabled( false ); // for Javadoc JVM args
        cmd.setWorkingDirectory( javadocOutputDirectory.getAbsolutePath() );
        cmd.setExecutable( jExecutable );

        // ----------------------------------------------------------------------
        // Wrap Javadoc JVM args
        // ----------------------------------------------------------------------

        addMemoryArg( cmd, "-Xmx", this.maxmemory );
        addMemoryArg( cmd, "-Xms", this.minmemory );
        addProxyArg( cmd );

        if ( StringUtils.isNotEmpty( additionalJOption ) )
        {
            cmd.createArg().setValue( additionalJOption );
        }

        List<String> arguments = new ArrayList<String>();

        // ----------------------------------------------------------------------
        // Wrap Javadoc options
        // ----------------------------------------------------------------------

        addJavadocOptions( arguments, sourcePaths );

        // ----------------------------------------------------------------------
        // Wrap Standard doclet Options
        // ----------------------------------------------------------------------

        if ( StringUtils.isEmpty( doclet ) || useStandardDocletOptions )
        {
            addStandardDocletOptions( javadocOutputDirectory, arguments );
        }

        // ----------------------------------------------------------------------
        // Write options file and include it in the command line
        // ----------------------------------------------------------------------

        if ( arguments.size() > 0 )
        {
            addCommandLineOptions( cmd, arguments, javadocOutputDirectory );
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

        executeJavadocCommandLine( cmd, javadocOutputDirectory );

        // delete generated javadoc files only if no error and no debug mode
        if ( !debug )
        {
            for ( int i = 0; i < cmd.getArguments().length; i++)
            {
                String arg = cmd.getArguments()[i].trim();

                if ( !arg.startsWith( "@" ))
                {
                    continue;
                }

                File argFile = new File( javadocOutputDirectory, arg.substring( 1 ) );
                if ( argFile.exists() )
                {
                    argFile.deleteOnExit();
                }
            }

            File scriptFile = new File( javadocOutputDirectory, DEBUG_JAVADOC_SCRIPT_NAME );
            if ( scriptFile.exists() )
            {
                scriptFile.deleteOnExit();
            }
        }
    }

    /**
     * Method to get the files on the specified source paths
     *
     * @param sourcePaths a List that contains the paths to the source files
     * @return a List that contains the specific path for every source file
     * @throws MavenReportException 
     */
    protected List<String> getFiles( List<String> sourcePaths )
        throws MavenReportException
    {
        List<String> files = new ArrayList<String>();
        if ( StringUtils.isEmpty( subpackages ) )
        {
            String[] excludedPackages = getExcludedPackages();

            for ( String sourcePath : sourcePaths )
            {
                File sourceDirectory = new File( sourcePath );
                JavadocUtil.addFilesFromSource( files, sourceDirectory, excludedPackages );
            }
        }

        return files;
    }

    /**
     * Method to get the source paths. If no source path is specified in the parameter, the compile source roots
     * of the project will be used.
     *
     * @return a List of the project absolute source paths as <code>String</code>
     * @see JavadocUtil#pruneDirs(MavenProject, List)
     */
    protected List<String> getSourcePaths()
        throws MavenReportException
    {
        List<String> sourcePaths;

        if ( StringUtils.isEmpty( sourcepath ) )
        {
            sourcePaths = new ArrayList<String>( JavadocUtil.pruneDirs( project, getProjectSourceRoots( project ) ) );

            if ( project.getExecutionProject() != null )
            {
                sourcePaths.addAll( JavadocUtil.pruneDirs( project, getExecutionProjectSourceRoots( project ) ) );
            }

            /*
             * Should be after the source path (i.e. -sourcepath '.../src/main/java;.../src/main/javadoc') and
             * *not* the opposite. If not, the javadoc tool always copies doc files, even if -docfilessubdirs is
             * not setted.
             */
            if ( getJavadocDirectory() != null )
            {
                File javadocDir = getJavadocDirectory();
                if ( javadocDir.exists() && javadocDir.isDirectory() )
                {
                    List<String> l =
                        JavadocUtil.pruneDirs( project,
                                               Collections.singletonList( getJavadocDirectory().getAbsolutePath() ) );
                    sourcePaths.addAll( l );
                }
            }

            if ( includeDependencySources )
            {
                sourcePaths.addAll( getDependencySourcePaths() );
            }
            
            if ( isAggregator() && project.isExecutionRoot() )
            {
                for ( MavenProject subProject : reactorProjects )
                {
                    if ( subProject != project )
                    {
                        List<String> sourceRoots = getProjectSourceRoots( subProject );

                        if ( subProject.getExecutionProject() != null )
                        {
                            sourceRoots.addAll( getExecutionProjectSourceRoots( subProject ) );
                        }

                        ArtifactHandler artifactHandler = subProject.getArtifact().getArtifactHandler();
                        if ( "java".equals( artifactHandler.getLanguage() ) )
                        {
                            sourcePaths.addAll( JavadocUtil.pruneDirs( subProject, sourceRoots ) );
                        }

                        String javadocDirRelative =
                            PathUtils.toRelative( project.getBasedir(), getJavadocDirectory().getAbsolutePath() );
                        File javadocDir = new File( subProject.getBasedir(), javadocDirRelative );
                        if ( javadocDir.exists() && javadocDir.isDirectory() )
                        {
                            List<String> l =
                                JavadocUtil.pruneDirs( subProject,
                                                       Collections.singletonList( javadocDir.getAbsolutePath() ) );
                            sourcePaths.addAll( l );
                        }
                    }
                }
            }
        }
        else
        {
            sourcePaths = new ArrayList<String>( Arrays.asList( JavadocUtil.splitPath( sourcepath ) ) );
            sourcePaths = JavadocUtil.pruneDirs( project, sourcePaths );
            if ( getJavadocDirectory() != null )
            {
                List<String> l =
                    JavadocUtil.pruneDirs( project,
                                           Collections.singletonList( getJavadocDirectory().getAbsolutePath() ) );
                sourcePaths.addAll( l );
            }
        }

        sourcePaths = JavadocUtil.pruneDirs( project, sourcePaths );

        return sourcePaths;
    }

    /**
     * Override this method to customize the configuration for resolving dependency sources. The default
     * behavior enables the resolution of -sources jar files.
     */
    protected SourceResolverConfig configureDependencySourceResolution( final SourceResolverConfig config )
    {
        return config.withCompileSources();
    }

    /**
     * Resolve dependency sources so they can be included directly in the javadoc process. To customize this,
     * override {@link AbstractJavadocMojo#configureDependencySourceResolution(SourceResolverConfig)}.
     */
    protected final List<String> getDependencySourcePaths()
        throws MavenReportException
    {
        try
        {
            if ( sourceDependencyCacheDir.exists() )
            {
                FileUtils.forceDelete( sourceDependencyCacheDir );
                sourceDependencyCacheDir.mkdirs();
            }
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Failed to delete cache directory: " + sourceDependencyCacheDir + "\nReason: " + e.getMessage(), e );
        }
        
        final SourceResolverConfig config = getDependencySourceResolverConfig();

        final AndArtifactFilter andFilter = new AndArtifactFilter();

        final List<String> dependencyIncludes = dependencySourceIncludes;
        final List<String> dependencyExcludes = dependencySourceExcludes;

        if ( isNotEmpty( dependencyIncludes ) || isNotEmpty( dependencyExcludes ) )
        {
            if ( isNotEmpty( dependencyIncludes ) )
            {
                andFilter.add( new PatternIncludesArtifactFilter( dependencyIncludes,
                                                                  !includeTransitiveDependencySources ) );
            }

            if ( isNotEmpty( dependencyExcludes ) )
            {
                andFilter.add( new PatternExcludesArtifactFilter( dependencyExcludes,
                                                                  !includeTransitiveDependencySources ) );
            }

            config.withFilter( andFilter );
        }

        try
        {
            return ResourceResolver.resolveDependencySourcePaths( config );
        }
        catch ( final ArtifactResolutionException e )
        {
            throw new MavenReportException( "Failed to resolve one or more javadoc source/resource artifacts:\n\n"
                + e.getMessage(), e );
        }
        catch ( final ArtifactNotFoundException e )
        {
            throw new MavenReportException( "Failed to resolve one or more javadoc source/resource artifacts:\n\n"
                + e.getMessage(), e );
        }
    }

    /**
     * Construct a SourceResolverConfig for resolving dependency sources and resources in a consistent
     * way, so it can be reused for both source and resource resolution.
     * 
     * @since 2.7
     */
    private SourceResolverConfig getDependencySourceResolverConfig()
    {
        return configureDependencySourceResolution( new SourceResolverConfig( getLog(), project, localRepository,
                                                                              sourceDependencyCacheDir, resolver,
                                                                              factory, artifactMetadataSource,
                                                                              archiverManager ).withReactorProjects( reactorProjects ) );
    }

    /**
     * Method that indicates whether the javadoc can be generated or not. If the project does not contain any source
     * files and no subpackages are specified, the plugin will terminate.
     *
     * @param files the project files
     * @return a boolean that indicates whether javadoc report can be generated or not
     */
    protected boolean canGenerateReport( List<String> files )
    {
        boolean canGenerate = true;

        if ( files.isEmpty() && StringUtils.isEmpty( subpackages ) )
        {
            canGenerate = false;
        }

        return canGenerate;
    }

    /**
     * @param result not null
     * @return the compile artifacts from the result
     * @see JavadocUtil#getCompileArtifacts(Set, boolean)
     */
    protected List<Artifact> getCompileArtifacts( ArtifactResolutionResult result )
    {
        return JavadocUtil.getCompileArtifacts( result.getArtifacts(), false );
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
     * @throws MavenReportException 
     */
    private String getExcludedPackages( List<String> sourcePaths )
        throws MavenReportException
    {
        List<String> excludedNames = null;

        if ( StringUtils.isNotEmpty( sourcepath ) && StringUtils.isNotEmpty( subpackages ) )
        {
            String[] excludedPackages = getExcludedPackages();
            String[] subpackagesList = subpackages.split( "[:]" );

            excludedNames = JavadocUtil.getExcludedNames( sourcePaths, subpackagesList, excludedPackages );
        }

        String excludeArg = "";
        if ( StringUtils.isNotEmpty( subpackages ) && excludedNames != null )
        {
            // add the excludedpackage names
            for ( Iterator<String> it = excludedNames.iterator(); it.hasNext(); )
            {
                String str = it.next();
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
     * @param sourcePaths the list of paths to the source files that will be included in the javadoc.
     * @return a String that contains the formatted source path argument, separated by the System pathSeparator
     * string (colon (<code>:</code>) on Solaris or semi-colon (<code>;</code>) on Windows).
     * @see File#pathSeparator
     */
    private String getSourcePath( List<String> sourcePaths )
    {
        String sourcePath = null;

        if ( StringUtils.isEmpty( subpackages ) || StringUtils.isNotEmpty( sourcepath ) )
        {
            sourcePath = StringUtils.join( sourcePaths.iterator(), File.pathSeparator );
        }

        return sourcePath;
    }

    /**
     * Method to get the packages specified in the <code>excludePackageNames</code> parameter. The packages are split
     * with ',', ':', or ';' and then formatted.
     *
     * @return an array of String objects that contain the package names
     * @throws MavenReportException 
     */
    private String[] getExcludedPackages()
        throws MavenReportException
    {
        Set<String> excluded = new LinkedHashSet<String>();
        
        if ( includeDependencySources )
        {
            try
            {
                resolveDependencyBundles();
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Failed to resolve javadoc bundles from dependencies: " + e.getMessage(), e );
            }
            
            if ( isNotEmpty( dependencyJavadocBundles ) )
            {
                for ( JavadocBundle bundle : dependencyJavadocBundles )
                {
                    JavadocOptions options = bundle.getOptions();
                    if ( options != null && isNotEmpty( options.getExcludePackageNames() ) )
                    {
                        excluded.addAll( options.getExcludePackageNames() );
                    }
                }
            }
        }
        
        // for the specified excludePackageNames
        if ( StringUtils.isNotEmpty( excludePackageNames ) )
        {
            excluded.addAll( Arrays.asList( excludePackageNames.split( "[,:;]" ) ) );
        }
        
        String[] result = new String[excluded.size()];
        if ( isNotEmpty( excluded ) )
        {
            int idx = 0;
            for ( String exclude : excluded )
            {
                result[idx] = exclude.replace( '.', File.separatorChar );
                idx++;
            }
        }

        return result;
    }

    /**
     * Method that sets the classpath elements that will be specified in the javadoc <code>-classpath</code>
     * parameter.
     *
     * @return a String that contains the concatenated classpath elements, separated by the System pathSeparator
     * string (colon (<code>:</code>) on Solaris or semi-colon (<code>;</code>) on Windows).
     * @throws MavenReportException if any.
     * @see File#pathSeparator
     */
    private String getClasspath()
        throws MavenReportException
    {
        List<String> classpathElements = new ArrayList<String>();
        Map<String, Artifact> compileArtifactMap = new HashMap<String, Artifact>();

        classpathElements.addAll( getProjectBuildOutputDirs( project ) );

        populateCompileArtifactMap( compileArtifactMap, getProjectArtifacts( project ) );

        if ( isAggregator() && project.isExecutionRoot() )
        {
            try
            {
                for ( MavenProject subProject : reactorProjects )
                {
                    if ( subProject != project )
                    {
                        classpathElements.addAll( getProjectBuildOutputDirs( subProject ) );

                        Set<Artifact> dependencyArtifacts = subProject.createArtifacts( factory, null, null );
                        if ( !dependencyArtifacts.isEmpty() )
                        {
                            ArtifactResolutionResult result = null;
                            try
                            {
                                result =
                                    resolver.resolveTransitively( dependencyArtifacts, subProject.getArtifact(),
                                                                  subProject.getManagedVersionMap(),
                                                                  localRepository,
                                                                  subProject.getRemoteArtifactRepositories(),
                                                                  artifactMetadataSource );
                            }
                            catch ( MultipleArtifactsNotFoundException e )
                            {
                                if ( checkMissingArtifactsInReactor( dependencyArtifacts, e.getMissingArtifacts() ) )
                                {
                                    getLog().warn( "IGNORED to add some artifacts in the classpath. See above." );
                                }
                                else
                                {
                                    // we can't find all the artifacts in the reactor so bubble the exception up.
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

                            populateCompileArtifactMap( compileArtifactMap, getCompileArtifacts( result ) );

                            if ( getLog().isDebugEnabled() )
                            {
                                StringBuffer sb = new StringBuffer();

                                sb.append( "Compiled artifacts for " );
                                sb.append( subProject.getGroupId() ).append( ":" );
                                sb.append( subProject.getArtifactId() ).append( ":" );
                                sb.append( subProject.getVersion() ).append( '\n' );
                                for ( String key : compileArtifactMap.keySet() )
                                {
                                    Artifact a = compileArtifactMap.get( key );
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

        for ( String key : compileArtifactMap.keySet() )
        {
            Artifact a = compileArtifactMap.get( key );
            classpathElements.add( a.getFile().toString() );
        }

        return StringUtils.join( classpathElements.iterator(), File.pathSeparator );
    }

    /**
     * TODO remove the part with ToolchainManager lookup once we depend on
     * 3.0.9 (have it as prerequisite). Define as regular component field then.
     *
     * @return Toolchain instance
     */
    private Toolchain getToolchain()
    {
        Toolchain tc = null;
        if ( toolchainManager != null )
        {
            tc = toolchainManager.getToolchainFromBuildContext( "jdk", session );
        }

        return tc;
    }

    /**
     * Method to put the artifacts in the hashmap.
     *
     * @param compileArtifactMap the hashmap that will contain the artifacts
     * @param artifactList the list of artifacts that will be put in the map
     * @throws MavenReportException if any
     */
    private void populateCompileArtifactMap( Map<String, Artifact> compileArtifactMap, Collection<Artifact> artifactList )
        throws MavenReportException
    {
        if ( artifactList == null )
        {
            return;
        }

        for ( Artifact newArtifact : artifactList )
        {
            File file = newArtifact.getFile();

            if ( file == null )
            {
                throw new MavenReportException( "Error in plugin descriptor - "
                    + "dependency was not resolved for artifact: " + newArtifact.getGroupId() + ":"
                    + newArtifact.getArtifactId() + ":" + newArtifact.getVersion() );
            }

            if ( compileArtifactMap.get( newArtifact.getDependencyConflictId() ) != null )
            {
                Artifact oldArtifact =
                    (Artifact) compileArtifactMap.get( newArtifact.getDependencyConflictId() );

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
                    theBottom =
                        StringUtils.replace( theBottom, "{organizationName}", "<a href=\""
                            + project.getOrganization().getUrl() + "\">" + project.getOrganization().getName()
                            + "</a>" );
                }
                else
                {
                    theBottom =
                        StringUtils.replace( theBottom, "{organizationName}", project.getOrganization().getName() );
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
     * Method to get the stylesheet path file to be used by the Javadoc Tool.
     * <br/>
     * If the {@link #stylesheetfile} is empty, return the file as String definded by {@link #stylesheet} value.
     * <br/>
     * If the {@link #stylesheetfile} is defined, return the file as String.
     * <br/>
     * Note: since 2.6, the {@link #stylesheetfile} could be a path from a resource in the project source
     * directories (i.e. <code>src/main/java</code>, <code>src/main/resources</code> or <code>src/main/javadoc</code>)
     * or from a resource in the Javadoc plugin dependencies.
     *
     * @param javadocOutputDirectory the output directory
     * @return the stylesheet file absolute path as String.
     * @see #getResource(List, String)
     */
    private String getStylesheetFile( final File javadocOutputDirectory )
    {
        if ( StringUtils.isEmpty( stylesheetfile ) )
        {
            if ( "java".equalsIgnoreCase( stylesheet ) )
            {
                // use the default Javadoc tool stylesheet
                return null;
            }

            // maven, see #copyDefaultStylesheet(File)
            return new File( javadocOutputDirectory, DEFAULT_CSS_NAME ).getAbsolutePath();
        }

        if ( new File( stylesheetfile ).exists() )
        {
            return new File( stylesheetfile ).getAbsolutePath();
        }

        return getResource( new File( javadocOutputDirectory, DEFAULT_CSS_NAME ), stylesheetfile );
    }

    /**
     * Method to get the help file to be used by the Javadoc Tool.
     * <br/>
     * Since 2.6, the {@link #helpfile} could be a path from a resource in the project source
     * directories (i.e. <code>src/main/java</code>, <code>src/main/resources</code> or <code>src/main/javadoc</code>)
     * or from a resource in the Javadoc plugin dependencies.
     *
     * @param javadocOutputDirectory the output directory.
     * @return the help file absolute path as String.
     * @since 2.6
     * @see #getResource(File, String)
     */
    private String getHelpFile( final File javadocOutputDirectory )
    {
        if ( StringUtils.isEmpty( helpfile ) )
        {
            return null;
        }

        if ( new File( helpfile ).exists() )
        {
            return new File( helpfile ).getAbsolutePath();
        }

        return getResource( new File( javadocOutputDirectory, "help-doc.html" ), helpfile );
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
     * Method to get the path of the bootclass artifacts used in the <code>-bootclasspath</code> option.
     *
     * @return a string that contains bootclass path, separated by the System pathSeparator string
     * (colon (<code>:</code>) on Solaris or semi-colon (<code>;</code>) on Windows).
     * @throws MavenReportException if any
     * @see File#pathSeparator
     */
    private String getBootclassPath()
        throws MavenReportException
    {
        Set<BootclasspathArtifact> bootclasspathArtifacts = collectBootClasspathArtifacts();
        
        List<String> bootclassPath = new ArrayList<String>();
        for ( BootclasspathArtifact aBootclasspathArtifact : bootclasspathArtifacts )
        {
            if ( ( StringUtils.isNotEmpty( aBootclasspathArtifact.getGroupId() ) )
                && ( StringUtils.isNotEmpty( aBootclasspathArtifact.getArtifactId() ) )
                && ( StringUtils.isNotEmpty( aBootclasspathArtifact.getVersion() ) ) )
            {
                bootclassPath.addAll( getArtifactsAbsolutePath( aBootclasspathArtifact ) );
            }
        }

        bootclassPath = JavadocUtil.pruneFiles( bootclassPath );

        StringBuffer path = new StringBuffer();
        path.append( StringUtils.join( bootclassPath.iterator(), File.pathSeparator ) );

        if ( StringUtils.isNotEmpty( bootclasspath ) )
        {
            path.append( JavadocUtil.unifyPathSeparator( bootclasspath ) );
        }

        return path.toString();
    }

    /**
     * Method to get the path of the doclet artifacts used in the <code>-docletpath</code> option.
     *
     * Either docletArtifact or doclectArtifacts can be defined and used, not both, docletArtifact
     * takes precedence over doclectArtifacts. docletPath is always appended to any result path
     * definition.
     *
     * @return a string that contains doclet path, separated by the System pathSeparator string
     * (colon (<code>:</code>) on Solaris or semi-colon (<code>;</code>) on Windows).
     * @throws MavenReportException if any
     * @see File#pathSeparator
     */
    private String getDocletPath()
        throws MavenReportException
    {
        Set<DocletArtifact> docletArtifacts = collectDocletArtifacts();
        List<String> pathParts = new ArrayList<String>();
        
        for ( DocletArtifact docletArtifact : docletArtifacts )
        {
            if ( !isDocletArtifactEmpty( docletArtifact ) )
            {
                pathParts.addAll( getArtifactsAbsolutePath( docletArtifact ) );
            }
        }
        
        StringBuffer path = new StringBuffer();
        path.append( StringUtils.join( pathParts.iterator(), File.pathSeparator ) );

        if ( !StringUtils.isEmpty( docletPath ) )
        {
            path.append( JavadocUtil.unifyPathSeparator( docletPath ) );
        }

        if ( StringUtils.isEmpty( path.toString() ) && getLog().isWarnEnabled() )
        {
            getLog().warn(
                           "No docletpath option was found. Please review <docletpath/> or <docletArtifact/>"
                               + " or <doclets/>." );
        }

        return path.toString();
    }

    /**
     * Verify if a doclet artifact is empty or not
     *
     * @param aDocletArtifact could be null
     * @return <code>true</code> if aDocletArtifact or the groupId/artifactId/version of the doclet artifact is null,
     * <code>false</code> otherwise.
     */
    private boolean isDocletArtifactEmpty( DocletArtifact aDocletArtifact )
    {
        if ( aDocletArtifact == null )
        {
            return true;
        }

        return StringUtils.isEmpty( aDocletArtifact.getGroupId() )
            && StringUtils.isEmpty( aDocletArtifact.getArtifactId() )
            && StringUtils.isEmpty( aDocletArtifact.getVersion() );
    }

    /**
     * Method to get the path of the taglet artifacts used in the <code>-tagletpath</code> option.
     *
     * @return a string that contains taglet path, separated by the System pathSeparator string
     * (colon (<code>:</code>) on Solaris or semi-colon (<code>;</code>) on Windows).
     * @throws MavenReportException if any
     * @see File#pathSeparator
     */
    private String getTagletPath()
        throws MavenReportException
    {
        Set<TagletArtifact> tArtifacts = collectTagletArtifacts();
        List<String> pathParts = new ArrayList<String>();
        
        for ( TagletArtifact tagletArtifact : tArtifacts )
        {
            if ( ( tagletArtifact != null ) && ( StringUtils.isNotEmpty( tagletArtifact.getGroupId() ) )
                && ( StringUtils.isNotEmpty( tagletArtifact.getArtifactId() ) )
                && ( StringUtils.isNotEmpty( tagletArtifact.getVersion() ) ) )
            {
                pathParts.addAll( getArtifactsAbsolutePath( tagletArtifact ) );
            }
        }

        
        Set<Taglet> taglets = collectTaglets();
        for ( Taglet taglet : taglets )
        {
            if ( taglet == null )
            {
                continue;
            }

            if ( ( taglet.getTagletArtifact() != null )
                && ( StringUtils.isNotEmpty( taglet.getTagletArtifact().getGroupId() ) )
                && ( StringUtils.isNotEmpty( taglet.getTagletArtifact().getArtifactId() ) )
                && ( StringUtils.isNotEmpty( taglet.getTagletArtifact().getVersion() ) ) )
            {
                pathParts.addAll( getArtifactsAbsolutePath( taglet.getTagletArtifact() ) );

                pathParts = JavadocUtil.pruneFiles( pathParts );
            }
            else if ( StringUtils.isNotEmpty( taglet.getTagletpath() ) )
            {
                pathParts.add( taglet.getTagletpath() );

                pathParts = JavadocUtil.pruneDirs( project, pathParts );
            }
        }
        
        StringBuffer path = new StringBuffer();
        path.append( StringUtils.join( pathParts.iterator(), File.pathSeparator ) );

        if ( StringUtils.isNotEmpty( tagletpath ) )
        {
            path.append( JavadocUtil.unifyPathSeparator( tagletpath ) );
        }

        return path.toString();
    }
    
    private Set<String> collectLinks()
        throws MavenReportException
    {
        Set<String> links = new LinkedHashSet<String>();
        
        if ( includeDependencySources )
        {
            try
            {
                resolveDependencyBundles();
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Failed to resolve javadoc bundles from dependencies: " + e.getMessage(), e );
            }
            
            if ( isNotEmpty( dependencyJavadocBundles ) )
            {
                for ( JavadocBundle bundle : dependencyJavadocBundles )
                {
                    JavadocOptions options = bundle.getOptions();
                    if ( options != null && isNotEmpty( options.getLinks() ) )
                    {
                        links.addAll( options.getLinks() );
                    }
                }
            }
        }
        
        if ( isNotEmpty( this.links ) )
        {
            links.addAll( this.links );
        }
        
        String javaApiLink = getDefaultJavadocApiLink();
        if ( javaApiLink != null )
        {
            links.add( javaApiLink );
        }

        links.addAll( getDependenciesLinks() );
        
        return links;
    }
    
    private Set<Group> collectGroups()
        throws MavenReportException
    {
        Set<Group> groups = new LinkedHashSet<Group>();
        
        if ( includeDependencySources )
        {
            try
            {
                resolveDependencyBundles();
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Failed to resolve javadoc bundles from dependencies: " + e.getMessage(), e );
            }
            
            if ( isNotEmpty( dependencyJavadocBundles ) )
            {
                for ( JavadocBundle bundle : dependencyJavadocBundles )
                {
                    JavadocOptions options = bundle.getOptions();
                    if ( options != null && isNotEmpty( options.getGroups() ) )
                    {
                        groups.addAll( options.getGroups() );
                    }
                }
            }
        }
        
        if ( this.groups != null && this.groups.length > 0 )
        {
            groups.addAll( Arrays.asList( this.groups ) );
        }
        
        return groups;
    }

    private Set<ResourcesArtifact> collectResourcesArtifacts()
        throws MavenReportException
    {
        Set<ResourcesArtifact> result = new LinkedHashSet<ResourcesArtifact>();

        if ( includeDependencySources )
        {
            try
            {
                resolveDependencyBundles();
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Failed to resolve javadoc bundles from dependencies: "
                    + e.getMessage(), e );
            }

            if ( isNotEmpty( dependencyJavadocBundles ) )
            {
                for ( JavadocBundle bundle : dependencyJavadocBundles )
                {
                    JavadocOptions options = bundle.getOptions();
                    if ( options != null && isNotEmpty( options.getResourcesArtifacts() ) )
                    {
                        result.addAll( options.getResourcesArtifacts() );
                    }
                }
            }
        }

        if ( this.resourcesArtifacts != null && this.resourcesArtifacts.length > 0 )
        {
            result.addAll( Arrays.asList( this.resourcesArtifacts ) );
        }

        return result;
    }

    private Set<BootclasspathArtifact> collectBootClasspathArtifacts()
        throws MavenReportException
    {
        Set<BootclasspathArtifact> result = new LinkedHashSet<BootclasspathArtifact>();

        if ( includeDependencySources )
        {
            try
            {
                resolveDependencyBundles();
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Failed to resolve javadoc bundles from dependencies: "
                    + e.getMessage(), e );
            }

            if ( isNotEmpty( dependencyJavadocBundles ) )
            {
                for ( JavadocBundle bundle : dependencyJavadocBundles )
                {
                    JavadocOptions options = bundle.getOptions();
                    if ( options != null && isNotEmpty( options.getBootclasspathArtifacts() ) )
                    {
                        result.addAll( options.getBootclasspathArtifacts() );
                    }
                }
            }
        }

        if ( this.bootclasspathArtifacts != null && this.bootclasspathArtifacts.length > 0 )
        {
            result.addAll( Arrays.asList( this.bootclasspathArtifacts ) );
        }

        return result;
    }

    private Set<OfflineLink> collectOfflineLinks()
        throws MavenReportException
    {
        Set<OfflineLink> result = new LinkedHashSet<OfflineLink>();

        if ( includeDependencySources )
        {
            try
            {
                resolveDependencyBundles();
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Failed to resolve javadoc bundles from dependencies: "
                    + e.getMessage(), e );
            }

            if ( isNotEmpty( dependencyJavadocBundles ) )
            {
                for ( JavadocBundle bundle : dependencyJavadocBundles )
                {
                    JavadocOptions options = bundle.getOptions();
                    if ( options != null && isNotEmpty( options.getOfflineLinks() ) )
                    {
                        result.addAll( options.getOfflineLinks() );
                    }
                }
            }
        }

        if ( this.offlineLinks != null && this.offlineLinks.length > 0 )
        {
            result.addAll( Arrays.asList( this.offlineLinks ) );
        }

        return result;
    }

    private Set<Tag> collectTags()
        throws MavenReportException
    {
        Set<Tag> tags = new LinkedHashSet<Tag>();

        if ( includeDependencySources )
        {
            try
            {
                resolveDependencyBundles();
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Failed to resolve javadoc bundles from dependencies: "
                    + e.getMessage(), e );
            }

            if ( isNotEmpty( dependencyJavadocBundles ) )
            {
                for ( JavadocBundle bundle : dependencyJavadocBundles )
                {
                    JavadocOptions options = bundle.getOptions();
                    if ( options != null && isNotEmpty( options.getTags() ) )
                    {
                        tags.addAll( options.getTags() );
                    }
                }
            }
        }

        if ( this.tags != null && this.tags.length > 0 )
        {
            tags.addAll( Arrays.asList( this.tags ) );
        }

        return tags;
    }

    private Set<TagletArtifact> collectTagletArtifacts()
        throws MavenReportException
    {
        Set<TagletArtifact> tArtifacts = new LinkedHashSet<TagletArtifact>();
        
        if ( includeDependencySources )
        {
            try
            {
                resolveDependencyBundles();
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Failed to resolve javadoc bundles from dependencies: " + e.getMessage(), e );
            }
            
            if ( isNotEmpty( dependencyJavadocBundles ) )
            {
                for ( JavadocBundle bundle : dependencyJavadocBundles )
                {
                    JavadocOptions options = bundle.getOptions();
                    if ( options != null && isNotEmpty( options.getTagletArtifacts() ) )
                    {
                        tArtifacts.addAll( options.getTagletArtifacts() );
                    }
                }
            }
        }
        
        if ( tagletArtifact != null )
        {
            tArtifacts.add( tagletArtifact );
        }
        
        if ( tagletArtifacts != null && tagletArtifacts.length > 0 )
        {
            tArtifacts.addAll( Arrays.asList( tagletArtifacts ) );
        }
        
        return tArtifacts;
    }

    private Set<DocletArtifact> collectDocletArtifacts()
        throws MavenReportException
    {
        Set<DocletArtifact> dArtifacts = new LinkedHashSet<DocletArtifact>();

        if ( includeDependencySources )
        {
            try
            {
                resolveDependencyBundles();
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Failed to resolve javadoc bundles from dependencies: "
                    + e.getMessage(), e );
            }

            if ( isNotEmpty( dependencyJavadocBundles ) )
            {
                for ( JavadocBundle bundle : dependencyJavadocBundles )
                {
                    JavadocOptions options = bundle.getOptions();
                    if ( options != null && isNotEmpty( options.getDocletArtifacts() ) )
                    {
                        dArtifacts.addAll( options.getDocletArtifacts() );
                    }
                }
            }
        }

        if ( docletArtifact != null )
        {
            dArtifacts.add( docletArtifact );
        }

        if ( docletArtifacts != null && docletArtifacts.length > 0 )
        {
            dArtifacts.addAll( Arrays.asList( docletArtifacts ) );
        }

        return dArtifacts;
    }

    private Set<Taglet> collectTaglets()
        throws MavenReportException
    {
        Set<Taglet> result = new LinkedHashSet<Taglet>();

        if ( includeDependencySources )
        {
            try
            {
                resolveDependencyBundles();
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Failed to resolve javadoc bundles from dependencies: "
                    + e.getMessage(), e );
            }

            if ( isNotEmpty( dependencyJavadocBundles ) )
            {
                for ( JavadocBundle bundle : dependencyJavadocBundles )
                {
                    JavadocOptions options = bundle.getOptions();
                    if ( options != null && isNotEmpty( options.getTaglets() ) )
                    {
                        result.addAll( options.getTaglets() );
                    }
                }
            }
        }

        if ( taglets != null && taglets.length > 0 )
        {
            result.addAll( Arrays.asList( taglets ) );
        }

        return result;
    }

    /**
     * Return the Javadoc artifact path and its transitive dependencies path from the local repository
     *
     * @param javadocArtifact not null
     * @return a list of locale artifacts absolute path
     * @throws MavenReportException if any
     */
    private List<String> getArtifactsAbsolutePath( JavadocPathArtifact javadocArtifact )
        throws MavenReportException
    {
        if ( ( StringUtils.isEmpty( javadocArtifact.getGroupId() ) )
            && ( StringUtils.isEmpty( javadocArtifact.getArtifactId() ) )
            && ( StringUtils.isEmpty( javadocArtifact.getVersion() ) ) )
        {
            return Collections.emptyList();
        }

        List<String> path = new ArrayList<String>();

        try
        {
            Artifact artifact = createAndResolveArtifact( javadocArtifact );
            path.add( artifact.getFile().getAbsolutePath() );

            // Find its transitive dependencies in the local repo
            MavenProject artifactProject =
                mavenProjectBuilder.buildFromRepository( artifact, remoteRepositories, localRepository );
            Set<Artifact> dependencyArtifacts = artifactProject.createArtifacts( factory, null, null );
            if ( !dependencyArtifacts.isEmpty() )
            {
                ArtifactResolutionResult result =
                    resolver.resolveTransitively( dependencyArtifacts, artifactProject.getArtifact(),
                                                  artifactProject.getRemoteArtifactRepositories(),
                                                  localRepository, artifactMetadataSource );
                Set<Artifact> artifacts = result.getArtifacts();

                Map<String, Artifact> compileArtifactMap = new HashMap<String, Artifact>();
                populateCompileArtifactMap( compileArtifactMap, artifacts );

                for ( String key : compileArtifactMap.keySet() )
                {
                    Artifact a = compileArtifactMap.get( key );
                    path.add( a.getFile().getAbsolutePath() );
                }
            }

            return path;
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MavenReportException( "Unable to resolve artifact:" + javadocArtifact, e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MavenReportException( "Unable to find artifact:" + javadocArtifact, e );
        }
        catch ( ProjectBuildingException e )
        {
            throw new MavenReportException( "Unable to build the Maven project for the artifact:"
                + javadocArtifact, e );
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new MavenReportException( "Unable to resolve artifact:" + javadocArtifact, e );
        }
    }

    /**
     * creates an {@link Artifact} representing the configured {@link JavadocPathArtifact} and resolves it.
     *
     * @param javadocArtifact the {@link JavadocPathArtifact} to resolve
     * @return a resolved {@link Artifact}
     * @throws ArtifactResolutionException if the resolution of the artifact failed.
     * @throws ArtifactNotFoundException if the artifact hasn't been found.
     * @throws ProjectBuildingException if the artifact POM could not be build.
     */
    private Artifact createAndResolveArtifact( JavadocPathArtifact javadocArtifact )
        throws ArtifactResolutionException, ArtifactNotFoundException, ProjectBuildingException
    {
        Artifact artifact =
            factory.createProjectArtifact( javadocArtifact.getGroupId(), javadocArtifact.getArtifactId(),
                                           javadocArtifact.getVersion(), Artifact.SCOPE_COMPILE );

        if ( artifact.getFile() == null )
        {
            MavenProject pluginProject =
                mavenProjectBuilder.buildFromRepository( artifact, remoteRepositories, localRepository );
            artifact = pluginProject.getArtifact();

            resolver.resolve( artifact, remoteRepositories, localRepository );
        }

        return artifact;
    }

    /**
     * Method that adds/sets the java memory parameters in the command line execution.
     *
     * @param cmd the command line execution object where the argument will be added
     * @param arg the argument parameter name
     * @param memory the JVM memory value to be set
     * @see JavadocUtil#parseJavadocMemory(String)
     */
    private void addMemoryArg( Commandline cmd, String arg, String memory )
    {
        if ( StringUtils.isNotEmpty( memory ) )
        {
            try
            {
                cmd.createArg().setValue( "-J" + arg + JavadocUtil.parseJavadocMemory( memory ) );
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
     * @param cmd the command line execution object where the argument will be added
     */
    private void addProxyArg( Commandline cmd )
    {
        // backward compatible
        if ( StringUtils.isNotEmpty( proxyHost ) )
        {
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn(
                               "The Javadoc plugin parameter 'proxyHost' is deprecated since 2.4. "
                                   + "Please configure an active proxy in your settings.xml." );
            }
            cmd.createArg().setValue( "-J-DproxyHost=" + proxyHost );

            if ( proxyPort > 0 )
            {
                if ( getLog().isWarnEnabled() )
                {
                    getLog().warn(
                                   "The Javadoc plugin parameter 'proxyPort' is deprecated since 2.4. "
                                       + "Please configure an active proxy in your settings.xml." );
                }
                cmd.createArg().setValue( "-J-DproxyPort=" + proxyPort );
            }
        }

        if ( settings == null || settings.getActiveProxy() == null )
        {
            return;
        }

        Proxy activeProxy = settings.getActiveProxy();
        String protocol =
            StringUtils.isNotEmpty( activeProxy.getProtocol() ) ? activeProxy.getProtocol() + "." : "";

        if ( StringUtils.isNotEmpty( activeProxy.getHost() ) )
        {
            cmd.createArg().setValue( "-J-D" + protocol + "proxySet=true" );
            cmd.createArg().setValue( "-J-D" + protocol + "proxyHost=" + activeProxy.getHost() );

            if ( activeProxy.getPort() > 0 )
            {
                cmd.createArg().setValue( "-J-D" + protocol + "proxyPort=" + activeProxy.getPort() );
            }

            if ( StringUtils.isNotEmpty( activeProxy.getNonProxyHosts() ) )
            {
                cmd.createArg().setValue(
                                          "-J-D" + protocol + "nonProxyHosts=\""
                                              + activeProxy.getNonProxyHosts() + "\"" );
            }

            if ( StringUtils.isNotEmpty( activeProxy.getUsername() ) )
            {
                cmd.createArg().setValue( "-J-Dhttp.proxyUser=\"" + activeProxy.getUsername() + "\"" );

                if ( StringUtils.isNotEmpty( activeProxy.getPassword() ) )
                {
                    cmd.createArg().setValue( "-J-Dhttp.proxyPassword=\"" + activeProxy.getPassword() + "\"" );
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
        Toolchain tc = getToolchain();

        if ( tc != null )
        {
            getLog().info( "Toolchain in javadoc-plugin: " + tc );
            if ( javadocExecutable != null )
            {
                getLog().warn(
                               "Toolchains are ignored, 'javadocExecutable' parameter is set to "
                                   + javadocExecutable );
            }
            else
            {
                javadocExecutable = tc.findTool( "javadoc" );
            }
        }

        String javadocCommand = "javadoc" + ( SystemUtils.IS_OS_WINDOWS ? ".exe" : "" );

        File javadocExe;

        // ----------------------------------------------------------------------
        // The javadoc executable is defined by the user
        // ----------------------------------------------------------------------
        if ( StringUtils.isNotEmpty( javadocExecutable ) )
        {
            javadocExe = new File( javadocExecutable );

            if ( javadocExe.isDirectory() )
            {
                javadocExe = new File( javadocExe, javadocCommand );
            }

            if ( SystemUtils.IS_OS_WINDOWS && javadocExe.getName().indexOf( '.' ) < 0 )
            {
                javadocExe = new File( javadocExe.getPath() + ".exe" );
            }

            if ( !javadocExe.isFile() )
            {
                throw new IOException( "The javadoc executable '" + javadocExe
                    + "' doesn't exist or is not a file. Verify the <javadocExecutable/> parameter." );
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
            javadocExe =
                new File( SystemUtils.getJavaHome() + File.separator + ".." + File.separator + "sh",
                          javadocCommand );
        }
        else if ( SystemUtils.IS_OS_MAC_OSX )
        {
            javadocExe = new File( SystemUtils.getJavaHome() + File.separator + "bin", javadocCommand );
        }
        else
        {
            javadocExe =
                new File( SystemUtils.getJavaHome() + File.separator + ".." + File.separator + "bin",
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
                throw new IOException( "The environment variable JAVA_HOME=" + javaHome
                    + " doesn't exist or is not a valid directory." );
            }

            javadocExe = new File( env.getProperty( "JAVA_HOME" ) + File.separator + "bin", javadocCommand );
        }

        if ( !javadocExe.exists() || !javadocExe.isFile() )
        {
            throw new IOException( "The javadoc executable '" + javadocExe
                + "' doesn't exist or is not a file. Verify the JAVA_HOME environment variable." );
        }

        return javadocExe.getAbsolutePath();
    }

    /**
     * Set a new value for <code>fJavadocVersion</code>
     *
     * @param jExecutable not null
     * @throws MavenReportException if not found
     * @see JavadocUtil#getJavadocVersion(File)
     */
    private void setFJavadocVersion( File jExecutable )
        throws MavenReportException
    {
        float jVersion;
        try
        {
            jVersion = JavadocUtil.getJavadocVersion( jExecutable );
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

            if ( fJavadocVersion != jVersion && getLog().isWarnEnabled() )
            {
                getLog().warn( "Are you sure about the <javadocVersion/> parameter? It seems to be " + jVersion );
            }
        }
        else
        {
            fJavadocVersion = jVersion;
        }
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
     * @param arguments a list of arguments, not null
     * @param b the flag which controls if the argument is added or not.
     * @param value the argument value to be added.
     */
    private void addArgIf( List<String> arguments, boolean b, String value )
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
     * @param arguments a list of arguments, not null
     * @param b the flag which controls if the argument is added or not.
     * @param value the argument value to be added.
     * @param requiredJavaVersion the required Java version, for example 1.31f or 1.4f
     * @see #addArgIf(java.util.List,boolean,String)
     * @see #isJavaDocVersionAtLeast(float)
     */
    private void addArgIf( List<String> arguments, boolean b, String value, float requiredJavaVersion )
    {
        if ( b )
        {
            if ( isJavaDocVersionAtLeast( requiredJavaVersion ) )
            {
                addArgIf( arguments, b, value );
            }
            else
            {
                if ( getLog().isWarnEnabled() )
                {
                    getLog().warn(
                                   value + " option is not supported on Java version < " + requiredJavaVersion
                                       + ". Ignore this option." );
                }
            }
        }
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * if the the value is not null or empty.
     * <p/>
     * Moreover, the value could be comma separated.
     *
     * @param arguments a list of arguments, not null
     * @param key the argument name.
     * @param value the argument value to be added.
     * @see #addArgIfNotEmpty(java.util.List,String,String,boolean)
     */
    private void addArgIfNotEmpty( List<String> arguments, String key, String value )
    {
        addArgIfNotEmpty( arguments, key, value, false );
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * if the the value is not null or empty.
     * <p/>
     * Moreover, the value could be comma separated.
     *
     * @param arguments a list of arguments, not null
     * @param key the argument name.
     * @param value the argument value to be added.
     * @param repeatKey repeat or not the key in the command line
     * @param splitValue if <code>true</code> given value will be tokenized by comma
     * @param requiredJavaVersion the required Java version, for example 1.31f or 1.4f
     * @see #addArgIfNotEmpty(List, String, String, boolean, boolean)
     * @see #isJavaDocVersionAtLeast(float)
     */
    private void addArgIfNotEmpty( List<String> arguments, String key, String value, boolean repeatKey,
                                   boolean splitValue, float requiredJavaVersion )
    {
        if ( StringUtils.isNotEmpty( value ) )
        {
            if ( isJavaDocVersionAtLeast( requiredJavaVersion ) )
            {
                addArgIfNotEmpty( arguments, key, value, repeatKey, splitValue );
            }
            else
            {
                if ( getLog().isWarnEnabled() )
                {
                    getLog().warn(
                                   key + " option is not supported on Java version < " + requiredJavaVersion
                                       + ". Ignore this option." );
                }
            }
        }
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * if the the value is not null or empty.
     * <p/>
     * Moreover, the value could be comma separated.
     *
     * @param arguments a list of arguments, not null
     * @param key the argument name.
     * @param value the argument value to be added.
     * @param repeatKey repeat or not the key in the command line
     * @param splitValue if <code>true</code> given value will be tokenized by comma
     */
    private void addArgIfNotEmpty( List<String> arguments, String key, String value, boolean repeatKey,
                                   boolean splitValue )
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
     * @param arguments a list of arguments, not null
     * @param key the argument name.
     * @param value the argument value to be added.
     * @param repeatKey repeat or not the key in the command line
     */
    private void addArgIfNotEmpty( List<String> arguments, String key, String value, boolean repeatKey )
    {
        addArgIfNotEmpty( arguments, key, value, repeatKey, true );
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * regarding the requested Java version.
     *
     * @param arguments a list of arguments, not null
     * @param key the argument name.
     * @param value the argument value to be added.
     * @param requiredJavaVersion the required Java version, for example 1.31f or 1.4f
     * @see #addArgIfNotEmpty(java.util.List, String, String, float, boolean)
     */
    private void addArgIfNotEmpty( List<String> arguments, String key, String value, float requiredJavaVersion )
    {
        addArgIfNotEmpty( arguments, key, value, requiredJavaVersion, false );
    }

    /**
     * Convenience method to add an argument to the <code>command line</code>
     * regarding the requested Java version.
     *
     * @param arguments a list of arguments, not null
     * @param key the argument name.
     * @param value the argument value to be added.
     * @param requiredJavaVersion the required Java version, for example 1.31f or 1.4f
     * @param repeatKey repeat or not the key in the command line
     * @see #addArgIfNotEmpty(java.util.List,String,String)
     * @see #isJavaDocVersionAtLeast(float)
     */
    private void addArgIfNotEmpty( List<String> arguments, String key, String value, float requiredJavaVersion,
                                   boolean repeatKey )
    {
        if ( StringUtils.isNotEmpty( value ) )
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
    }

    /**
     * Convenience method to process {@link #offlineLinks} values as individual <code>-linkoffline</code>
     * javadoc options.
     * <br/>
     * If {@link #detectOfflineLinks}, try to add javadoc apidocs according Maven conventions for all modules given
     * in the project.
     *
     * @param arguments a list of arguments, not null
     * @throws MavenReportException if any
     * @see #offlineLinks
     * @see #getModulesLinks()
     * @see <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#package-list">package-list spec</a>
     */
    private void addLinkofflineArguments( List<String> arguments )
        throws MavenReportException
    {
        Set<OfflineLink> offlineLinksList = collectOfflineLinks();

        offlineLinksList.addAll( getModulesLinks() );

        for ( OfflineLink offlineLink : offlineLinksList )
        {
            String url = offlineLink.getUrl();
            if ( StringUtils.isEmpty( url ) )
            {
                continue;
            }
            url = cleanUrl( url );

            String location = offlineLink.getLocation();
            if ( StringUtils.isEmpty( location ) )
            {
                continue;
            }
            if ( isValidJavadocLink( location ) )
            {
                addArgIfNotEmpty( arguments, "-linkoffline", JavadocUtil.quotedPathArgument( url ) + " "
                    + JavadocUtil.quotedPathArgument( location ), true );
            }
        }
    }

    /**
     * Convenience method to process {@link #links} values as individual <code>-link</code> javadoc options.
     * If {@link #detectLinks}, try to add javadoc apidocs according Maven conventions for all dependencies given
     * in the project.
     * <br/>
     * According the Javadoc documentation, all defined link should have <code>${link}/package-list</code> fetchable.
     * <br/>
     * <b>Note</b>: when a link is not fetchable:
     * <ul>
     * <li>Javadoc 1.4 and less throw an exception</li>
     * <li>Javadoc 1.5 and more display a warning</li>
     * </ul>
     *
     * @param arguments a list of arguments, not null
     * @throws MavenReportException 
     * @see #detectLinks
     * @see #getDependenciesLinks()
     * @see JavadocUtil#fetchURL(Settings, URL)
     * @see <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#package-list">package-list spec</a>
     */
    private void addLinkArguments( List<String> arguments )
        throws MavenReportException
    {
        Set<String> links = collectLinks();

        for ( String link : links )
        {
            if ( StringUtils.isEmpty( link ) )
            {
                continue;
            }

            while ( link.endsWith( "/" ) )
            {
                link = link.substring( 0, link.lastIndexOf( "/" ) );
            }

            if ( isValidJavadocLink( link ) )
            {
                addArgIfNotEmpty( arguments, "-link", JavadocUtil.quotedPathArgument( link ), true );
            }
        }
    }

    /**
     * Coppy all resources to the output directory
     *
     * @param javadocOutputDirectory not null
     * @throws MavenReportException if any
     * @see #copyDefaultStylesheet(File)
     * @see #copyJavadocResources(File)
     * @see #copyAdditionalJavadocResources(File)
     */
    private void copyAllResources( File javadocOutputDirectory )
        throws MavenReportException
    {
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
            /*
             * Workaround since -docfilessubdirs doesn't seem to be used correctly by the javadoc tool
             * (see other note about -sourcepath). Take care of the -excludedocfilessubdir option.
             */
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
        // Copy additional javadoc resources in artifacts
        // ----------------------------------------------------------------------

        copyAdditionalJavadocResources( javadocOutputDirectory );
    }

    /**
     * Copies the {@link #DEFAULT_CSS_NAME} css file from the current class
     * loader to the <code>outputDirectory</code> only if {@link #stylesheetfile} is empty and
     * {@link #stylesheet} is equals to <code>maven</code>.
     *
     * @param anOutputDirectory the output directory
     * @throws java.io.IOException if any
     * @see #DEFAULT_CSS_NAME
     * @see JavadocUtil#copyResource(File, URL)
     */
    private void copyDefaultStylesheet( File anOutputDirectory )
        throws IOException
    {
        if ( StringUtils.isNotEmpty( stylesheetfile ) )
        {
            return;
        }

        if ( !stylesheet.equalsIgnoreCase( "maven" ) )
        {
            return;
        }

        URL url = getClass().getClassLoader().getResource( RESOURCE_CSS_DIR + "/" + DEFAULT_CSS_NAME );
        File outFile = new File( anOutputDirectory, DEFAULT_CSS_NAME );
        JavadocUtil.copyResource( url, outFile );
    }

    /**
     * Method that copy all <code>doc-files</code> directories from <code>javadocDirectory</code> of
     * the current projet or of the projects in the reactor to the <code>outputDirectory</code>.
     *
     * @see <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.2.html#docfiles">Reference
     * Guide, Copies new "doc-files" directory for holding images and examples</a>
     * @see #docfilessubdirs
     *
     * @param anOutputDirectory the output directory
     * @throws java.io.IOException if any
     */
    private void copyJavadocResources( File anOutputDirectory )
        throws IOException
    {
        if ( anOutputDirectory == null || !anOutputDirectory.exists() )
        {
            throw new IOException( "The outputDirectory " + anOutputDirectory + " doesn't exists." );
        }

        if ( includeDependencySources )
        {
            resolveDependencyBundles();
            if ( isNotEmpty( dependencyJavadocBundles ) )
            {
                for ( JavadocBundle bundle : dependencyJavadocBundles )
                {
                    File dir = bundle.getResourcesDirectory();
                    JavadocOptions options = bundle.getOptions();
                    if ( dir != null && dir.isDirectory() )
                    {
                        JavadocUtil.copyJavadocResources( anOutputDirectory, dir, options == null ? null
                                        : options.getExcludedDocfilesSubdirs() );
                    }
                }
            }
        }
        
        if ( getJavadocDirectory() != null )
        {
            JavadocUtil.copyJavadocResources( anOutputDirectory, getJavadocDirectory(), excludedocfilessubdir );
        }

        if ( isAggregator() && project.isExecutionRoot() )
        {
            for ( MavenProject subProject : reactorProjects )
            {
                if ( subProject != project )
                {
                    String javadocDirRelative =
                        PathUtils.toRelative( project.getBasedir(), getJavadocDirectory().getAbsolutePath() );
                    File javadocDir = new File( subProject.getBasedir(), javadocDirRelative );
                    JavadocUtil.copyJavadocResources( anOutputDirectory, javadocDir, excludedocfilessubdir );
                }
            }
        }
    }

    private synchronized void resolveDependencyBundles()
        throws IOException
    {
        if ( dependencyJavadocBundles == null )
        {
            dependencyJavadocBundles = ResourceResolver.resolveDependencyJavadocBundles( getDependencySourceResolverConfig() );
            if ( dependencyJavadocBundles == null )
            {
                dependencyJavadocBundles = new ArrayList<JavadocBundle>();
            }
        }
    }

    /**
     * Method that copy additional Javadoc resources from given artifacts.
     *
     * @see #resourcesArtifacts
     * @param anOutputDirectory the output directory
     * @throws MavenReportException if any
     */
    private void copyAdditionalJavadocResources( File anOutputDirectory )
        throws MavenReportException
    {
        Set<ResourcesArtifact> resourcesArtifacts = collectResourcesArtifacts();
        if ( isEmpty( resourcesArtifacts ) )
        {
            return;
        }

        UnArchiver unArchiver;
        try
        {
            unArchiver = archiverManager.getUnArchiver( "jar" );
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MavenReportException( "Unable to extract resources artifact. "
                + "No archiver for 'jar' available.", e );
        }

        for ( ResourcesArtifact item : resourcesArtifacts )
        {
            Artifact artifact;
            try
            {
                artifact = createAndResolveArtifact( item );
            }
            catch ( ArtifactResolutionException e )
            {
                throw new MavenReportException( "Unable to resolve artifact:" + item, e );
            }
            catch ( ArtifactNotFoundException e )
            {
                throw new MavenReportException( "Unable to find artifact:" + item, e );
            }
            catch ( ProjectBuildingException e )
            {
                throw new MavenReportException( "Unable to build the Maven project for the artifact:" + item,
                                                e );
            }

            unArchiver.setSourceFile( artifact.getFile() );
            unArchiver.setDestDirectory( anOutputDirectory );
            // remove the META-INF directory from resource artifact
            IncludeExcludeFileSelector[] selectors =
                new IncludeExcludeFileSelector[] { new IncludeExcludeFileSelector() };
            selectors[0].setExcludes( new String[] { "META-INF/**" } );
            unArchiver.setFileSelectors( selectors );

            getLog().info( "Extracting contents of resources artifact: " + artifact.getArtifactId() );
            try
            {
                unArchiver.extract();
            }
            catch ( ArchiverException e )
            {
                throw new MavenReportException( "Extraction of resources failed. Artifact that failed was: "
                    + artifact.getArtifactId(), e );
            }
        }
    }

    /**
     * @param sourcePaths could be null
     * @param files not null
     * @return the list of package names for files in the sourcePaths
     */
    private List<String> getPackageNames( List<String> sourcePaths, List<String> files )
    {
        return getPackageNamesOrFilesWithUnnamedPackages( sourcePaths, files, true );
    }

    /**
     * @param sourcePaths could be null
     * @param files not null
     * @return a list files with unnamed package names for files in the sourecPaths
     */
    private List<String> getFilesWithUnnamedPackages( List<String> sourcePaths, List<String> files )
    {
        return getPackageNamesOrFilesWithUnnamedPackages( sourcePaths, files, false );
    }

    /**
     * @param sourcePaths not null, containing absolute and relative paths
     * @param files not null, containing list of quoted files
     * @param onlyPackageName boolean for only package name
     * @return a list of package names or files with unnamed package names, depending the value of the unnamed flag
     * @see #getFiles(List)
     * @see #getSourcePaths()
     */
    private List<String> getPackageNamesOrFilesWithUnnamedPackages( List<String> sourcePaths, List<String> files,
                                                                    boolean onlyPackageName )
    {
        List<String> returnList = new ArrayList<String>();

        if ( !StringUtils.isEmpty( sourcepath ) )
        {
            return returnList;
        }

        for ( String currentFile : files )
        {
            currentFile = currentFile.replace( '\\', '/' );

            for ( String currentSourcePath : sourcePaths )
            {
                currentSourcePath = currentSourcePath.replace( '\\', '/' );

                if ( !currentSourcePath.endsWith( "/" ) )
                {
                    currentSourcePath += "/";
                }

                if ( currentFile.indexOf( currentSourcePath ) != -1 )
                {
                    String packagename = currentFile.substring( currentSourcePath.length() + 1 );

                    /*
                     * Remove the miscellaneous files
                     * http://download.oracle.com/javase/1.4.2/docs/tooldocs/solaris/javadoc.html#unprocessed
                     */
                    if ( packagename.indexOf( "doc-files" ) != -1 )
                    {
                        continue;
                    }

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
     * Generate an <code>options</code> file for all options and arguments and add the <code>@options</code> in the
     * command line.
     *
     * @see <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#argumentfiles">
     * Reference Guide, Command line argument files</a>
     *
     * @param cmd not null
     * @param arguments not null
     * @param javadocOutputDirectory not null
     * @throws MavenReportException if any
     * @see #OPTIONS_FILE_NAME
     */
    private void addCommandLineOptions( Commandline cmd, List<String> arguments, File javadocOutputDirectory )
        throws MavenReportException
    {
        File optionsFile = new File( javadocOutputDirectory, OPTIONS_FILE_NAME );

        StringBuffer options = new StringBuffer();
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

        cmd.createArg().setValue( "@" + OPTIONS_FILE_NAME );
    }

    /**
     * Generate a file called <code>argfile</code> (or <code>files</code>, depending the JDK) to hold files and add
     * the <code>@argfile</code> (or <code>@file</code>, depending the JDK) in the command line.
     *
     * @see <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#argumentfiles">
     * Reference Guide, Command line argument files
     * </a>
     * @see <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/javadoc/whatsnew-1.4.html#runningjavadoc">
     * What s New in Javadoc 1.4
     * </a>
     *
     * @param cmd not null
     * @param javadocOutputDirectory not null
     * @param files not null
     * @throws MavenReportException if any
     * @see #isJavaDocVersionAtLeast(float)
     * @see #ARGFILE_FILE_NAME
     * @see #FILES_FILE_NAME
     */
    private void addCommandLineArgFile( Commandline cmd, File javadocOutputDirectory, List<String> files )
        throws MavenReportException
    {
        File argfileFile;
        if ( isJavaDocVersionAtLeast( SINCE_JAVADOC_1_4 ) )
        {
            argfileFile = new File( javadocOutputDirectory, ARGFILE_FILE_NAME );
        }
        else
        {
            argfileFile = new File( javadocOutputDirectory, FILES_FILE_NAME );
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
            cmd.createArg().setValue( "@" + ARGFILE_FILE_NAME );
        }
        else
        {
            cmd.createArg().setValue( "@" + FILES_FILE_NAME );
        }
    }

    /**
     * Generate a file called <code>packages</code> to hold all package names and add the <code>@packages</code> in
     * the command line.
     *
     * @see <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#argumentfiles">
     * Reference Guide, Command line argument files</a>
     *
     * @param cmd not null
     * @param javadocOutputDirectory not null
     * @param packageNames not null
     * @throws MavenReportException if any
     * @see #PACKAGES_FILE_NAME
     */
    private void addCommandLinePackages( Commandline cmd, File javadocOutputDirectory, List<String> packageNames )
        throws MavenReportException
    {
        File packagesFile = new File( javadocOutputDirectory, PACKAGES_FILE_NAME );

        try
        {
            FileUtils.fileWrite( packagesFile.getAbsolutePath(),
                                 StringUtils.join( packageNames.toArray( new String[0] ),
                                                   SystemUtils.LINE_SEPARATOR ) );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to write '" + packagesFile.getName()
                + "' temporary file for command execution", e );
        }

        cmd.createArg().setValue( "@" + PACKAGES_FILE_NAME );
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
        if ( StringUtils.isNotEmpty( getEncoding() ) && !JavadocUtil.validateEncoding( getEncoding() ) )
        {
            throw new MavenReportException( "Unsupported option <encoding/> '" + getEncoding() + "'" );
        }

        // locale
        if ( StringUtils.isNotEmpty( this.locale ) )
        {
            StringTokenizer tokenizer = new StringTokenizer( this.locale, "_" );
            final int maxTokens = 3;
            if ( tokenizer.countTokens() > maxTokens )
            {
                throw new MavenReportException( "Unsupported option <locale/> '" + this.locale
                    + "', should be language_country_variant." );
            }

            Locale localeObject = null;
            if ( tokenizer.hasMoreTokens() )
            {
                String language = tokenizer.nextToken().toLowerCase( Locale.ENGLISH );
                if ( !Arrays.asList( Locale.getISOLanguages() ).contains( language ) )
                {
                    throw new MavenReportException( "Unsupported language '" + language
                        + "' in option <locale/> '" + this.locale + "'" );
                }
                localeObject = new Locale( language );

                if ( tokenizer.hasMoreTokens() )
                {
                    String country = tokenizer.nextToken().toUpperCase( Locale.ENGLISH );
                    if ( !Arrays.asList( Locale.getISOCountries() ).contains( country ) )
                    {
                        throw new MavenReportException( "Unsupported country '" + country
                            + "' in option <locale/> '" + this.locale + "'" );
                    }
                    localeObject = new Locale( language, country );

                    if ( tokenizer.hasMoreTokens() )
                    {
                        String variant = tokenizer.nextToken();
                        localeObject = new Locale( language, country, variant );
                    }
                }
            }

            if ( localeObject == null )
            {
                throw new MavenReportException( "Unsupported option <locale/> '" + this.locale
                    + "', should be language_country_variant." );
            }

            this.locale = localeObject.toString();
            final List<Locale> availableLocalesList = Arrays.asList( Locale.getAvailableLocales() );
            if ( StringUtils.isNotEmpty( localeObject.getVariant() )
                && !availableLocalesList.contains( localeObject ) )
            {
                StringBuffer sb = new StringBuffer();
                sb.append( "Unsupported option <locale/> with variant '" ).append( this.locale );
                sb.append( "'" );

                localeObject = new Locale( localeObject.getLanguage(), localeObject.getCountry() );
                this.locale = localeObject.toString();

                sb.append( ", trying to use <locale/> without variant, i.e. '" ).append( this.locale ).append( "'" );
                if ( getLog().isWarnEnabled() )
                {
                    getLog().warn( sb.toString() );
                }
            }

            if ( !availableLocalesList.contains( localeObject ) )
            {
                throw new MavenReportException( "Unsupported option <locale/> '" + this.locale + "'" );
            }
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
        if ( StringUtils.isNotEmpty( getDocencoding() ) && !JavadocUtil.validateEncoding( getDocencoding() ) )
        {
            throw new MavenReportException( "Unsupported option <docencoding/> '" + getDocencoding() + "'" );
        }

        // charset
        if ( StringUtils.isNotEmpty( getCharset() ) && !JavadocUtil.validateEncoding( getCharset() ) )
        {
            throw new MavenReportException( "Unsupported option <charset/> '" + getCharset() + "'" );
        }

        // helpfile
        if ( StringUtils.isNotEmpty( helpfile ) && nohelp )
        {
            throw new MavenReportException( "Option <nohelp/> conflicts with <helpfile/>" );
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

        // stylesheet
        if ( StringUtils.isNotEmpty( stylesheet )
            && !( stylesheet.equalsIgnoreCase( "maven" ) || stylesheet.equalsIgnoreCase( "java" ) ) )
        {
            throw new MavenReportException( "Option <stylesheet/> supports only \"maven\" or \"java\" value." );
        }

        // default java api links
        if ( javaApiLinks == null || javaApiLinks.size() == 0 )
        {
            javaApiLinks = DEFAULT_JAVA_API_LINKS;
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
    private boolean checkMissingArtifactsInReactor( Collection<Artifact> dependencyArtifacts,
                                                    Collection<Artifact> missing )
    {
        Set<MavenProject> foundInReactor = new HashSet<MavenProject>();
        for ( Artifact mArtifact : missing )
        {
            for ( MavenProject p : reactorProjects )
            {
                if ( p.getArtifactId().equals( mArtifact.getArtifactId() )
                    && p.getGroupId().equals( mArtifact.getGroupId() )
                    && p.getVersion().equals( mArtifact.getVersion() ) )
                {
                    getLog().warn(
                                   "The dependency: ["
                                       + p.getId()
                                       + "] can't be resolved but has been found in the reactor (probably snapshots).\n"
                                       + "This dependency has been excluded from the Javadoc classpath. "
                                       + "You should rerun javadoc after executing mvn install." );

                    // found it, move on.
                    foundInReactor.add( p );
                    break;
                }
            }
        }

        // if all of them have been found, we can continue.
        return foundInReactor.size() == missing.size();
    }

    /**
     * Add Standard Javadoc Options.
     * <br/>
     * The <a href="package-summary.html#Standard_Javadoc_Options">package documentation</a> details the
     * Standard Javadoc Options wrapped by this Plugin.
     *
     * @param arguments not null
     * @param sourcePaths not null
     * @throws MavenReportException if any
     * @see <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#javadocoptions">http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#javadocoptions</a>
     */
    private void addJavadocOptions( List<String> arguments, List<String> sourcePaths )
        throws MavenReportException
    {
        validateJavadocOptions();

        // see com.sun.tools.javadoc.Start#parseAndExecute(String argv[])
        addArgIfNotEmpty( arguments, "-locale", JavadocUtil.quotedArgument( this.locale ) );

        // all options in alphabetical order

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

        addArgIfNotEmpty( arguments, "-bootclasspath", JavadocUtil.quotedPathArgument( getBootclassPath() ) );

        if ( isJavaDocVersionAtLeast( SINCE_JAVADOC_1_5 ) )
        {
            addArgIf( arguments, breakiterator, "-breakiterator", SINCE_JAVADOC_1_5 );
        }

        addArgIfNotEmpty( arguments, "-classpath", JavadocUtil.quotedPathArgument( getClasspath() ) );

        if ( StringUtils.isNotEmpty( doclet ) )
        {
            addArgIfNotEmpty( arguments, "-doclet", JavadocUtil.quotedArgument( doclet ) );
            addArgIfNotEmpty( arguments, "-docletpath", JavadocUtil.quotedPathArgument( getDocletPath() ) );
        }

        if ( StringUtils.isEmpty( encoding ) )
        {
            getLog().warn(
                           "Source files encoding has not been set, using platform encoding "
                               + ReaderFactory.FILE_ENCODING + ", i.e. build is platform dependent!" );
        }
        addArgIfNotEmpty( arguments, "-encoding", JavadocUtil.quotedArgument( getEncoding() ) );

        addArgIfNotEmpty( arguments, "-exclude", getExcludedPackages( sourcePaths ), SINCE_JAVADOC_1_4 );

        addArgIfNotEmpty( arguments, "-extdirs", JavadocUtil.quotedPathArgument( JavadocUtil.unifyPathSeparator( extdirs ) ) );

        if ( ( getOverview() != null ) && ( getOverview().exists() ) )
        {
            addArgIfNotEmpty( arguments, "-overview",
                              JavadocUtil.quotedPathArgument( getOverview().getAbsolutePath() ) );
        }

        arguments.add( getAccessLevel() );

        if ( isJavaDocVersionAtLeast( SINCE_JAVADOC_1_5 ) )
        {
            addArgIf( arguments, quiet, "-quiet", SINCE_JAVADOC_1_5 );
        }

        addArgIfNotEmpty( arguments, "-source", JavadocUtil.quotedArgument( source ), SINCE_JAVADOC_1_4 );

        if ( ( StringUtils.isEmpty( sourcepath ) ) && ( StringUtils.isNotEmpty( subpackages ) ) )
        {
            sourcepath = StringUtils.join( sourcePaths.iterator(), File.pathSeparator );
        }
        addArgIfNotEmpty( arguments, "-sourcepath", JavadocUtil.quotedPathArgument( getSourcePath( sourcePaths ) ) );

        if ( StringUtils.isNotEmpty( sourcepath ) && isJavaDocVersionAtLeast( SINCE_JAVADOC_1_5 ) )
        {
            addArgIfNotEmpty( arguments, "-subpackages", subpackages, SINCE_JAVADOC_1_5 );
        }

        addArgIf( arguments, verbose, "-verbose" );

        addArgIfNotEmpty( arguments, null, additionalparam );
    }

    /**
     * Add Standard Doclet Options.
     * <br/>
     * The <a href="package-summary.html#Standard_Doclet_Options">package documentation</a> details the
     * Standard Doclet Options wrapped by this Plugin.
     *
     * @param javadocOutputDirectory not null
     * @param arguments not null
     * @throws MavenReportException if any
     * @see <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#standard">
     * http://download.oracle.com/javase/1.4.2/docs/tooldocs/windows/javadoc.html#standard</a>
     */
    private void addStandardDocletOptions( File javadocOutputDirectory, List<String> arguments )
        throws MavenReportException
    {
        validateStandardDocletOptions();

        // all options in alphabetical order

        addArgIf( arguments, author, "-author" );

        addArgIfNotEmpty( arguments, "-bottom", JavadocUtil.quotedArgument( getBottomText() ), false, false );

        if ( !isJavaDocVersionAtLeast( SINCE_JAVADOC_1_5 ) )
        {
            addArgIf( arguments, breakiterator, "-breakiterator", SINCE_JAVADOC_1_4 );
        }

        addArgIfNotEmpty( arguments, "-charset", JavadocUtil.quotedArgument( getCharset() ) );

        addArgIfNotEmpty( arguments, "-d", JavadocUtil.quotedPathArgument( javadocOutputDirectory.toString() ) );

        addArgIfNotEmpty( arguments, "-docencoding", JavadocUtil.quotedArgument( getDocencoding() ) );

        addArgIf( arguments, docfilessubdirs, "-docfilessubdirs", SINCE_JAVADOC_1_4 );

        addArgIfNotEmpty( arguments, "-doctitle", JavadocUtil.quotedArgument( getDoctitle() ), false, false );

        if ( docfilessubdirs )
        {
            addArgIfNotEmpty( arguments, "-excludedocfilessubdir",
                              JavadocUtil.quotedPathArgument( excludedocfilessubdir ), SINCE_JAVADOC_1_4 );
        }

        addArgIfNotEmpty( arguments, "-footer", JavadocUtil.quotedArgument( footer ), false, false );

        addGroups( arguments );

        addArgIfNotEmpty( arguments, "-header", JavadocUtil.quotedArgument( header ), false, false );

        addArgIfNotEmpty( arguments, "-helpfile",
                          JavadocUtil.quotedPathArgument( getHelpFile( javadocOutputDirectory ) ) );

        addArgIf( arguments, keywords, "-keywords", SINCE_JAVADOC_1_4_2 );

        if ( !isOffline )
        {
            addLinkArguments( arguments );
        }

        addLinkofflineArguments( arguments );

        addArgIf( arguments, linksource, "-linksource", SINCE_JAVADOC_1_4 );

        if ( sourcetab > 0 )
        {
            if ( fJavadocVersion == SINCE_JAVADOC_1_4_2 )
            {
                addArgIfNotEmpty( arguments, "-linksourcetab", String.valueOf( sourcetab ) );
            }
            addArgIfNotEmpty( arguments, "-sourcetab", String.valueOf( sourcetab ), SINCE_JAVADOC_1_5 );
        }

        addArgIf( arguments, nocomment, "-nocomment", SINCE_JAVADOC_1_4 );

        addArgIf( arguments, nodeprecated, "-nodeprecated" );

        addArgIf( arguments, nodeprecatedlist, "-nodeprecatedlist" );

        addArgIf( arguments, nohelp, "-nohelp" );

        addArgIf( arguments, noindex, "-noindex" );

        addArgIf( arguments, nonavbar, "-nonavbar" );

        addArgIf( arguments, nooverview, "-nooverview" );

        addArgIfNotEmpty( arguments, "-noqualifier", JavadocUtil.quotedArgument( noqualifier ), SINCE_JAVADOC_1_4 );

        addArgIf( arguments, nosince, "-nosince" );

        addArgIf( arguments, notimestamp, "-notimestamp", SINCE_JAVADOC_1_5 );

        addArgIf( arguments, notree, "-notree" );

        addArgIfNotEmpty( arguments, "-packagesheader", JavadocUtil.quotedArgument( packagesheader ),
                          SINCE_JAVADOC_1_4_2 );

        if ( !isJavaDocVersionAtLeast( SINCE_JAVADOC_1_5 ) ) // Sun bug: 4714350
        {
            addArgIf( arguments, quiet, "-quiet", SINCE_JAVADOC_1_4 );
        }

        addArgIf( arguments, serialwarn, "-serialwarn" );

        addArgIf( arguments, splitindex, "-splitindex" );

        addArgIfNotEmpty( arguments, "-stylesheetfile",
                          JavadocUtil.quotedPathArgument( getStylesheetFile( javadocOutputDirectory ) ) );

        if ( StringUtils.isNotEmpty( sourcepath ) && !isJavaDocVersionAtLeast( SINCE_JAVADOC_1_5 ) )
        {
            addArgIfNotEmpty( arguments, "-subpackages", subpackages, SINCE_JAVADOC_1_4 );
        }

        addArgIfNotEmpty( arguments, "-taglet", JavadocUtil.quotedArgument( taglet ), SINCE_JAVADOC_1_4 );
        addTaglets( arguments );
        addTagletsFromTagletArtifacts( arguments );
        addArgIfNotEmpty( arguments, "-tagletpath", JavadocUtil.quotedPathArgument( getTagletPath() ),
                          SINCE_JAVADOC_1_4 );

        addTags( arguments );

        addArgIfNotEmpty( arguments, "-top", JavadocUtil.quotedArgument( top ), false, false, SINCE_JAVADOC_1_6 );

        addArgIf( arguments, use, "-use" );

        addArgIf( arguments, version, "-version" );

        addArgIfNotEmpty( arguments, "-windowtitle", JavadocUtil.quotedArgument( getWindowtitle() ), false, false );
    }

    /**
     * Add <code>groups</code> parameter to arguments.
     *
     * @param arguments not null
     * @throws MavenReportException 
     */
    private void addGroups( List<String> arguments )
        throws MavenReportException
    {
        Set<Group> groups = collectGroups();
        if ( isEmpty( groups ) )
        {
            return;
        }

        for ( Group group : groups )
        {
            if ( group == null || StringUtils.isEmpty( group.getTitle() )
                || StringUtils.isEmpty( group.getPackages() ) )
            {
                if ( getLog().isWarnEnabled() )
                {
                    getLog().warn( "A group option is empty. Ignore this option." );
                }
            }
            else
            {
                String groupTitle = StringUtils.replace( group.getTitle(), ",", "&#44;" );
                addArgIfNotEmpty( arguments, "-group", JavadocUtil.quotedArgument( groupTitle ) + " "
                    + JavadocUtil.quotedArgument( group.getPackages() ), true );
            }
        }
    }

    /**
     * Add <code>tags</code> parameter to arguments.
     *
     * @param arguments not null
     * @throws MavenReportException 
     */
    private void addTags( List<String> arguments )
        throws MavenReportException
    {
        Set<Tag> tags = collectTags();
        
        if ( isEmpty( tags ) )
        {
            return;
        }

        for ( Tag tag : tags )
        {
            if ( StringUtils.isEmpty( tag.getName() ) )
            {
                if ( getLog().isWarnEnabled() )
                {
                    getLog().warn( "A tag name is empty. Ignore this option." );
                }
            }
            else
            {
                String value = "\"" + tag.getName();
                if ( StringUtils.isNotEmpty( tag.getPlacement() ) )
                {
                    value += ":" + tag.getPlacement();
                    if ( StringUtils.isNotEmpty( tag.getHead() ) )
                    {
                        value += ":" + tag.getHead();
                    }
                }
                value += "\"";
                addArgIfNotEmpty( arguments, "-tag", value, SINCE_JAVADOC_1_4 );
            }
        }
    }

    /**
     * Add <code>taglets</code> parameter to arguments.
     *
     * @param arguments not null
     */
    private void addTaglets( List<String> arguments )
    {
        if ( taglets == null )
        {
            return;
        }

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

    /**
     * Auto-detect taglets class name from <code>tagletArtifacts</code> and add them to arguments.
     *
     * @param arguments not null
     * @throws MavenReportException if any
     * @see JavadocUtil#getTagletClassNames(File)
     */
    private void addTagletsFromTagletArtifacts( List<String> arguments )
        throws MavenReportException
    {
        Set<TagletArtifact> tArtifacts = new LinkedHashSet<TagletArtifact>();
        if ( tagletArtifacts != null && tagletArtifacts.length > 0 )
        {
            tArtifacts.addAll( Arrays.asList( tagletArtifacts ) );
        }
        
        if ( includeDependencySources )
        {
            try
            {
                resolveDependencyBundles();
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Failed to resolve javadoc bundles from dependencies: " + e.getMessage(), e );
            }
            
            if ( isNotEmpty( dependencyJavadocBundles ) )
            {
                for ( JavadocBundle bundle : dependencyJavadocBundles )
                {
                    JavadocOptions options = bundle.getOptions();
                    if ( options != null && isNotEmpty( options.getTagletArtifacts() ) )
                    {
                        tArtifacts.addAll( options.getTagletArtifacts() );
                    }
                }
            }
        }
        
        if ( isEmpty( tArtifacts ) )
        {
            return;
        }

        List<String> tagletsPath = new ArrayList<String>();
        
        for ( TagletArtifact aTagletArtifact : tArtifacts )
        {
            if ( ( StringUtils.isNotEmpty( aTagletArtifact.getGroupId() ) )
                && ( StringUtils.isNotEmpty( aTagletArtifact.getArtifactId() ) )
                && ( StringUtils.isNotEmpty( aTagletArtifact.getVersion() ) ) )
            {
                Artifact artifact;
                try
                {
                    artifact = createAndResolveArtifact( aTagletArtifact );
                }
                catch ( ArtifactResolutionException e )
                {
                    throw new MavenReportException( "Unable to resolve artifact:" + aTagletArtifact, e );
                }
                catch ( ArtifactNotFoundException e )
                {
                    throw new MavenReportException( "Unable to find artifact:" + aTagletArtifact, e );
                }
                catch ( ProjectBuildingException e )
                {
                    throw new MavenReportException( "Unable to build the Maven project for the artifact:"
                        + aTagletArtifact, e );
                }

                tagletsPath.add( artifact.getFile().getAbsolutePath() );
            }
        }

        tagletsPath = JavadocUtil.pruneFiles( tagletsPath );

        for ( String tagletJar : tagletsPath )
        {
            if ( !tagletJar.toLowerCase( Locale.ENGLISH ).endsWith( ".jar" ) )
            {
                continue;
            }

            List<String> tagletClasses;
            try
            {
                tagletClasses = JavadocUtil.getTagletClassNames( new File( tagletJar ) );
            }
            catch ( IOException e )
            {
                if ( getLog().isWarnEnabled() )
                {
                    getLog().warn(
                                   "Unable to auto-detect Taglet class names from '" + tagletJar
                                       + "'. Try to specify them with <taglets/>." );
                }
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "IOException: " + e.getMessage(), e );
                }
                continue;
            }
            catch ( ClassNotFoundException e )
            {
                if ( getLog().isWarnEnabled() )
                {
                    getLog().warn(
                                   "Unable to auto-detect Taglet class names from '" + tagletJar
                                       + "'. Try to specify them with <taglets/>." );
                }
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "ClassNotFoundException: " + e.getMessage(), e );
                }
                continue;
            }
            catch ( NoClassDefFoundError e )
            {
                if ( getLog().isWarnEnabled() )
                {
                    getLog().warn(
                                   "Unable to auto-detect Taglet class names from '" + tagletJar
                                       + "'. Try to specify them with <taglets/>." );
                }
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "NoClassDefFoundError: " + e.getMessage(), e );
                }
                continue;
            }

            if ( tagletClasses != null && !tagletClasses.isEmpty() )
            {
                for ( String tagletClass : tagletClasses )
                {
                    addArgIfNotEmpty( arguments, "-taglet", JavadocUtil.quotedArgument( tagletClass ),
                                      SINCE_JAVADOC_1_4 );
                }
            }
        }
    }

    /**
     * Execute the Javadoc command line
     *
     * @param cmd not null
     * @param javadocOutputDirectory not null
     * @throws MavenReportException if any errors occur
     */
    private void executeJavadocCommandLine( Commandline cmd, File javadocOutputDirectory )
        throws MavenReportException
    {
        if ( getLog().isDebugEnabled() )
        {
            // no quoted arguments
            getLog().debug( CommandLineUtils.toString( cmd.getCommandline() ).replaceAll( "'", "" ) );
        }

        String cmdLine = null;
        if ( debug )
        {
            cmdLine = CommandLineUtils.toString( cmd.getCommandline() ).replaceAll( "'", "" );
            cmdLine = JavadocUtil.hideProxyPassword( cmdLine, settings );

            writeDebugJavadocScript( cmdLine, javadocOutputDirectory );
        }

        CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
        try
        {
            int exitCode = CommandLineUtils.executeCommandLine( cmd, out, err );

            String output = ( StringUtils.isEmpty( out.getOutput() ) ? null : '\n' + out.getOutput().trim() );

            if ( exitCode != 0 )
            {
                if ( cmdLine == null )
                {
                    cmdLine = CommandLineUtils.toString( cmd.getCommandline() ).replaceAll( "'", "" );
                    cmdLine = JavadocUtil.hideProxyPassword( cmdLine, settings );
                }
                writeDebugJavadocScript( cmdLine, javadocOutputDirectory );

                if ( StringUtils.isNotEmpty( output ) && StringUtils.isEmpty( err.getOutput() )
                    && isJavadocVMInitError( output ) )
                {
                    StringBuffer msg = new StringBuffer();
                    msg.append( output );
                    msg.append( '\n' ).append( '\n' );
                    msg.append( JavadocUtil.ERROR_INIT_VM ).append( '\n' );
                    msg.append( "Or, try to reduce the Java heap size for the Javadoc goal using " );
                    msg.append( "-Dminmemory=<size> and -Dmaxmemory=<size>." ).append( '\n' ).append( '\n' );

                    msg.append( "Command line was: " ).append( cmdLine ).append( '\n' ).append( '\n' );
                    msg.append( "Refer to the generated Javadoc files in '" ).append( javadocOutputDirectory )
                       .append( "' dir.\n" );

                    throw new MavenReportException( msg.toString() );
                }

                if ( StringUtils.isNotEmpty( output ) )
                {
                    getLog().info( output );
                }

                StringBuffer msg = new StringBuffer( "\nExit code: " );
                msg.append( exitCode );
                if ( StringUtils.isNotEmpty( err.getOutput() ) )
                {
                    msg.append( " - " ).append( err.getOutput() );
                }
                msg.append( '\n' );
                msg.append( "Command line was: " ).append( cmdLine ).append( '\n' ).append( '\n' );

                msg.append( "Refer to the generated Javadoc files in '" ).append( javadocOutputDirectory )
                   .append( "' dir.\n" );

                throw new MavenReportException( msg.toString() );
            }

            if ( StringUtils.isNotEmpty( output ) )
            {
                getLog().info( output );
            }
        }
        catch ( CommandLineException e )
        {
            throw new MavenReportException( "Unable to execute javadoc command: " + e.getMessage(), e );
        }

        // ----------------------------------------------------------------------
        // Handle Javadoc warnings
        // ----------------------------------------------------------------------

        if ( StringUtils.isNotEmpty( err.getOutput() ) && getLog().isWarnEnabled() )
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

    /**
     * @param outputFile not nul
     * @param inputResourceName a not null resource in <code>src/main/java</code>, <code>src/main/resources</code> or <code>src/main/javadoc</code>
     * or in the Javadoc plugin dependencies.
     * @return the resource file absolute path as String
     * @since 2.6
     */
    private String getResource( File outputFile, String inputResourceName )
    {
        if ( inputResourceName.startsWith( "/" ) )
        {
            inputResourceName = inputResourceName.replaceFirst( "//*", "" );
        }

        List<String> classPath = new ArrayList<String>();
        classPath.add( project.getBuild().getSourceDirectory() );

        URL resourceURL = getResource( classPath, inputResourceName );
        if ( resourceURL != null )
        {
            getLog().debug( inputResourceName + " found in the main src directory of the project." );
            return FileUtils.toFile( resourceURL ).getAbsolutePath();
        }

        classPath.clear();
        for ( Iterator<Resource> it = project.getBuild().getResources().iterator(); it.hasNext(); )
        {
            Resource resource = it.next();

            classPath.add( resource.getDirectory() );
        }
        resourceURL = getResource( classPath, inputResourceName );
        if ( resourceURL != null )
        {
            getLog().debug( inputResourceName + " found in the main resources directories of the project." );
            return FileUtils.toFile( resourceURL ).getAbsolutePath();
        }

        if ( javadocDirectory.exists() )
        {
            classPath.clear();
            classPath.add( javadocDirectory.getAbsolutePath() );
            resourceURL = getResource( classPath, inputResourceName );
            if ( resourceURL != null )
            {
                getLog().debug( inputResourceName + " found in the main javadoc directory of the project." );
                return FileUtils.toFile( resourceURL ).getAbsolutePath();
            }
        }

        classPath.clear();
        final String pluginId = "org.apache.maven.plugins:maven-javadoc-plugin";
        Plugin javadocPlugin = getPlugin( project, pluginId );
        if ( javadocPlugin != null && javadocPlugin.getDependencies() != null )
        {
            for ( Iterator<Dependency> it = javadocPlugin.getDependencies().iterator(); it.hasNext(); )
            {
                Dependency dependency = it.next();

                JavadocPathArtifact javadocPathArtifact = new JavadocPathArtifact();
                javadocPathArtifact.setGroupId( dependency.getGroupId() );
                javadocPathArtifact.setArtifactId( dependency.getArtifactId() );
                javadocPathArtifact.setVersion( dependency.getVersion() );
                Artifact artifact = null;
                try
                {
                    artifact = createAndResolveArtifact( javadocPathArtifact );
                }
                catch ( Exception e )
                {
                    if ( getLog().isDebugEnabled() )
                    {
                        getLog().error( "Unable to retrieve the dependency: " + dependency + ". Ignored.", e );
                    }
                    else
                    {
                        getLog().error( "Unable to retrieve the dependency: " + dependency + ". Ignored." );
                    }
                }

                if ( artifact != null && artifact.getFile().exists() )
                {
                    classPath.add( artifact.getFile().getAbsolutePath() );
                }
            }
            resourceURL = getResource( classPath, inputResourceName );
            if ( resourceURL != null )
            {
                getLog().debug( inputResourceName + " found in javadoc plugin dependencies." );
                try
                {
                    JavadocUtil.copyResource( resourceURL, outputFile );

                    return outputFile.getAbsolutePath();
                }
                catch ( IOException e )
                {
                    if ( getLog().isDebugEnabled() )
                    {
                        getLog().error( "IOException: " + e.getMessage(), e );
                    }
                    else
                    {
                        getLog().error( "IOException: " + e.getMessage() );
                    }
                }
            }
        }

        getLog()
                .warn( "Unable to find the resource '" + inputResourceName + "'. Using default Javadoc resources." );

        return null;
    }

    /**
     * @param classPath a not null String list of files where resource will be look up.
     * @param resource a not null ressource to find in the class path.
     * @return the resource from the given classpath or null if not found
     * @see ClassLoader#getResource(String)
     * @since 2.6
     */
    private URL getResource( final List<String> classPath, final String resource )
    {
        List<URL> urls = new ArrayList<URL>( classPath.size() );
        for ( String filename : classPath )
        {
            try
            {
                urls.add( new File( filename ).toURL() );
            }
            catch ( MalformedURLException e )
            {
                getLog().error( "MalformedURLException: " + e.getMessage() );
            }
        }

        ClassLoader javadocClassLoader = new URLClassLoader( (URL[]) urls.toArray( new URL[urls.size()] ), null );

        return javadocClassLoader.getResource( resource );
    }

    /**
     * Load the plugin pom.properties to get the current plugin version.
     *
     * @return <code>org.apache.maven.plugins:maven-javadoc-plugin:CURRENT_VERSION:javadoc</code>
     */
    private String getFullJavadocGoal()
    {
        String javadocPluginVersion = null;
        InputStream resourceAsStream = null;
        try
        {
            String resource =
                "META-INF/maven/org.apache.maven.plugins/maven-javadoc-plugin/pom.properties";
            resourceAsStream = AbstractJavadocMojo.class.getClassLoader().getResourceAsStream( resource );

            if ( resourceAsStream != null )
            {
                Properties properties = new Properties();
                properties.load( resourceAsStream );

                if ( StringUtils.isNotEmpty( properties.getProperty( "version" ) ) )
                {
                    javadocPluginVersion = properties.getProperty( "version" );
                }
            }
        }
        catch ( IOException e )
        {
            // nop
        }
        finally
        {
            IOUtil.close( resourceAsStream );
        }

        StringBuffer sb = new StringBuffer();

        sb.append( "org.apache.maven.plugins" ).append( ":" );
        sb.append( "maven-javadoc-plugin" ).append( ":" );
        if ( StringUtils.isNotEmpty( javadocPluginVersion ) )
        {
            sb.append( javadocPluginVersion ).append( ":" );
        }

        if ( TestJavadocReport.class.isAssignableFrom( getClass() ) )
        {
            sb.append( "test-javadoc" );
        }
        else
        {
            sb.append( "javadoc" );
        }

        return sb.toString();
    }

    /**
     * Using Maven, a Javadoc link is given by <code>${project.url}/apidocs</code>.
     *
     * @return the detected Javadoc links using the Maven conventions for all modules defined in the current project
     * or an empty list.
     * @throws MavenReportException if any
     * @see #detectOfflineLinks
     * @see #reactorProjects
     * @since 2.6
     */
    private List<OfflineLink> getModulesLinks()
        throws MavenReportException
    {
        if ( !( detectOfflineLinks && !isAggregator() && reactorProjects != null ) )
        {
            return Collections.emptyList();
        }

        getLog().debug( "Try to add links for modules..." );

        List<OfflineLink> modulesLinks = new ArrayList<OfflineLink>();
        String javadocDirRelative = PathUtils.toRelative( project.getBasedir(), getOutputDirectory() );
        for ( MavenProject p : reactorProjects )
        {
            if ( p.getPackaging().equals( "pom" ) )
            {
                continue;
            }

            if ( p.getId().equals( project.getId() ) )
            {
                continue;
            }

            File location = new File( p.getBasedir(), javadocDirRelative );
            if ( p.getUrl() != null )
            {
                if ( !location.exists() )
                {
                    String javadocGoal = getFullJavadocGoal();
                    getLog().info(
                                   "The goal '" + javadocGoal
                                       + "' has not be previously called for the project: '" + p.getId()
                                       + "'. Trying to invoke it..." );

                    File invokerDir = new File( project.getBuild().getDirectory(), "invoker" );
                    invokerDir.mkdirs();
                    File invokerLogFile = FileUtils.createTempFile( "maven-javadoc-plugin", ".txt", invokerDir );
                    try
                    {
                        JavadocUtil.invokeMaven( getLog(), new File( localRepository.getBasedir() ), p.getFile(),
                                                 Collections.singletonList( javadocGoal ), null, invokerLogFile );
                    }
                    catch ( MavenInvocationException e )
                    {
                        if ( getLog().isDebugEnabled() )
                        {
                            getLog().error( "MavenInvocationException: " + e.getMessage(), e );
                        }
                        else
                        {
                            getLog().error( "MavenInvocationException: " + e.getMessage() );
                        }

                        String invokerLogContent = JavadocUtil.readFile( invokerLogFile, "UTF-8" );
                        
                        // TODO: Why are we only interested in cases where the JVM won't start?
                        // [MJAVADOC-275][jdcasey] I changed the logic here to only throw an error WHEN 
                        //   the JVM won't start (opposite of what it was).
                        if ( invokerLogContent != null && invokerLogContent.indexOf( JavadocUtil.ERROR_INIT_VM ) > -1 )
                        {
                            throw new MavenReportException( e.getMessage(), e );
                        }
                    }
                    finally
                    {
                        // just create the directory to prevent repeated invokations..
                        if ( !location.exists() )
                        {
                            location.mkdirs();
                        }
                    }
                }

                if ( location.exists() )
                {
                    String url = getJavadocLink( p );

                    OfflineLink ol = new OfflineLink();
                    ol.setUrl( url );
                    ol.setLocation( location.getAbsolutePath() );

                    if ( getLog().isDebugEnabled() )
                    {
                        getLog().debug( "Added Javadoc link: " + url + " for the project: " + p.getId() );
                    }

                    modulesLinks.add( ol );
                }
            }
        }

        return modulesLinks;
    }

    /**
     * Using Maven, a Javadoc link is given by <code>${project.url}/apidocs</code>.
     *
     * @return the detected Javadoc links using the Maven conventions for all dependencies defined in the current
     * project or an empty list.
     * @see #detectLinks
     * @since 2.6
     */
    private List<String> getDependenciesLinks()
    {
        if ( !detectLinks )
        {
            return Collections.emptyList();
        }

        getLog().debug( "Try to add links for dependencies..." );

        List<String> dependenciesLinks = new ArrayList<String>();
        for ( Iterator<Artifact> it = project.getDependencyArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = it.next();

            if ( artifact != null && artifact.getFile() != null && artifact.getFile().exists() )
            {
                try
                {
                    MavenProject artifactProject =
                        mavenProjectBuilder.buildFromRepository( artifact, remoteRepositories, localRepository );

                    if ( StringUtils.isNotEmpty( artifactProject.getUrl() ) )
                    {
                        String url = getJavadocLink( artifactProject );

                        if ( getLog().isDebugEnabled() )
                        {
                            getLog().debug(
                                            "Added Javadoc link: " + url + " for the project: "
                                                + artifactProject.getId() );
                        }
                        dependenciesLinks.add( url );
                    }
                }
                catch ( ProjectBuildingException e )
                {
                    if ( getLog().isDebugEnabled() )
                    {
                        getLog().debug(
                                       "Error when building the artifact: " + artifact.toString()
                                           + ". Ignored to add Javadoc link." );
                        getLog().error( "ProjectBuildingException: " + e.getMessage(), e );
                    }
                    else
                    {
                        getLog().error( "ProjectBuildingException: " + e.getMessage() );
                    }
                }
            }
        }

        return dependenciesLinks;
    }

    /**
     * @return if {@link #detectJavaApiLink}, the Java API link based on the {@link #javaApiLinks} properties and the
     * value of the <code>source</code> parameter in the <code>org.apache.maven.plugins:maven-compiler-plugin</code>
     * defined in <code>${project.build.plugins}</code> or in <code>${project.build.pluginManagement}</code>,
     * or the {@link #fJavadocVersion}, or <code>null</code> if not defined.
     * @since 2.6
     * @see #detectJavaApiLink
     * @see #javaApiLinks
     * @see #DEFAULT_JAVA_API_LINKS
     * @see <a href="http://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#source">source parameter</a>
     */
    private String getDefaultJavadocApiLink()
    {
        if ( !detectJavaApiLink )
        {
            return null;
        }

        final String pluginId = "org.apache.maven.plugins:maven-compiler-plugin";
        float sourceVersion = fJavadocVersion;
        String sourceConfigured = getPluginParameter( project, pluginId, "source" );
        if ( sourceConfigured != null )
        {
            try
            {
                sourceVersion = Float.parseFloat( sourceConfigured );
            }
            catch ( NumberFormatException e )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug(
                                    "NumberFormatException for the source parameter in the maven-compiler-plugin. "
                                        + "Ignored it", e );
                }
            }
        }
        else
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug(
                                "No maven-compiler-plugin defined in ${build.plugins} or in "
                                    + "${project.build.pluginManagement} for the " + project.getId()
                                    + ". Added Javadoc API link according the javadoc executable version i.e.: "
                                    + fJavadocVersion );
            }
        }

        String javaApiLink = null;
        if ( sourceVersion >= 1.3f && sourceVersion < 1.4f && javaApiLinks.getProperty( "api_1.3" ) != null )
        {
            javaApiLink = javaApiLinks.getProperty( "api_1.3" ).toString();
        }
        else if ( sourceVersion >= 1.4f && sourceVersion < 1.5f && javaApiLinks.getProperty( "api_1.4" ) != null )
        {
            javaApiLink = javaApiLinks.getProperty( "api_1.4" ).toString();
        }
        else if ( sourceVersion >= 1.5f && sourceVersion < 1.6f && javaApiLinks.getProperty( "api_1.5" ) != null )
        {
            javaApiLink = javaApiLinks.getProperty( "api_1.5" ).toString();
        }
        else if ( sourceVersion >= 1.6f && javaApiLinks.getProperty( "api_1.6" ) != null )
        {
            javaApiLink = javaApiLinks.getProperty( "api_1.6" ).toString();
        }

        if ( StringUtils.isNotEmpty( javaApiLink ) )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "Found Java API link: " + javaApiLink );
            }
        }
        else
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "No Java API link found." );
            }
        }

        return javaApiLink;
    }

    /**
     * @param link not null
     * @return <code>true</code> if the link has a <code>/package-list</code>, <code>false</code> otherwise.
     * @since 2.6
     * @see <a href="http://download.oracle.com/javase/1.4.2/docs/tooldocs/solaris/javadoc.html#package-list">
     * package-list spec</a>
     */
    private boolean isValidJavadocLink( String link )
    {
        try
        {
            URI linkUri;
            if ( link.trim().toLowerCase( Locale.ENGLISH ).startsWith( "http" )
                || link.trim().toLowerCase( Locale.ENGLISH ).startsWith( "https" )
                || link.trim().toLowerCase( Locale.ENGLISH ).startsWith( "ftp" )
                || link.trim().toLowerCase( Locale.ENGLISH ).startsWith( "file" ) )
            {
                linkUri = new URI( link + "/package-list" );
            }
            else
            {
                // links can be relative paths or files
                File dir = new File( link );
                if ( !dir.isAbsolute() )
                {
                    dir = new File( getOutputDirectory(), link );
                }
                if ( !dir.isDirectory() )
                {
                    getLog().error( "The given File link: " + dir + " is not a dir." );
                }
                linkUri = new File( dir, "package-list" ).toURI();
            }

            JavadocUtil.fetchURL( settings, linkUri.toURL() );

            return true;
        }
        catch ( URISyntaxException e )
        {
            if ( getLog().isErrorEnabled() )
            {
                getLog().error( "Malformed link: " + link + "/package-list. Ignored it." );
            }
            return false;
        }
        catch ( IOException e )
        {
            if ( getLog().isErrorEnabled() )
            {
                getLog().error( "Error fetching link: " + link + "/package-list. Ignored it." );
            }
            return false;
        }
    }

    /**
     * Write a debug javadoc script in case of command line error or in debug mode.
     *
     * @param cmdLine the current command line as string, not null.
     * @param javadocOutputDirectory the output dir, not null.
     * @see #executeJavadocCommandLine(Commandline, File)
     * @since 2.6
     */
    private void writeDebugJavadocScript( String cmdLine, File javadocOutputDirectory )
    {
        File commandLineFile = new File( javadocOutputDirectory, DEBUG_JAVADOC_SCRIPT_NAME );
        commandLineFile.getParentFile().mkdirs();

        try
        {
            FileUtils.fileWrite( commandLineFile.getAbsolutePath(), "UTF-8", cmdLine );

            if ( !SystemUtils.IS_OS_WINDOWS )
            {
                Runtime.getRuntime().exec( new String[] { "chmod", "a+x", commandLineFile.getAbsolutePath() } );
            }
        }
        catch ( IOException e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().error( "Unable to write '" + commandLineFile.getName() + "' debug script file", e );
            }
            else
            {
                getLog().error( "Unable to write '" + commandLineFile.getName() + "' debug script file" );
            }
        }
    }

    /**
     * Check if the Javadoc JVM is correctly started or not.
     *
     * @param output the command line output, not null.
     * @return <code>true</code> if Javadoc output command line contains Javadoc word, <code>false</code> otherwise.
     * @see #executeJavadocCommandLine(Commandline, File)
     * @since 2.6.1
     */
    private boolean isJavadocVMInitError( String output )
    {
        /*
         * see main.usage and main.Building_tree keys from
         * com.sun.tools.javadoc.resources.javadoc bundle in tools.jar
         */
        if ( output.indexOf( "Javadoc" ) != -1 || output.indexOf( "javadoc" ) != -1 )
        {
            return false;
        }

        return true;
    }

    // ----------------------------------------------------------------------
    // Static methods
    // ----------------------------------------------------------------------

    /**
     * @param p not null
     * @return the javadoc link based on the project url i.e. <code>${project.url}/${destDir}</code> where
     * <code>destDir</code> is configued in the Javadoc plugin configuration (<code>apidocs</code> by default).
     * @since 2.6
     */
    private static String getJavadocLink( MavenProject p )
    {
        if ( p.getUrl() == null )
        {
            return null;
        }

        String url = cleanUrl( p.getUrl() );
        String destDir = "apidocs"; // see JavadocReport#destDir

        final String pluginId = "org.apache.maven.plugins:maven-javadoc-plugin";
        String destDirConfigured = getPluginParameter( p, pluginId, "destDir" );
        if ( destDirConfigured != null )
        {
            destDir = destDirConfigured;
        }

        return url + "/" + destDir;
    }

    /**
     * @param url could be null.
     * @return the url cleaned or empty if url was null.
     * @since 2.6
     */
    private static String cleanUrl( String url )
    {
        if ( url == null )
        {
            return "";
        }

        url = url.trim();
        while ( url.endsWith( "/" ) )
        {
            url = url.substring( 0, url.lastIndexOf( "/" ) );
        }

        return url;
    }

    /**
     * @param p not null
     * @param pluginId not null key of the plugin defined in {@link org.apache.maven.model.Build#getPluginsAsMap()}
     * or in {@link org.apache.maven.model.PluginManagement#getPluginsAsMap()}
     * @return the Maven plugin defined in <code>${project.build.plugins}</code> or in
     * <code>${project.build.pluginManagement}</code>, or <code>null</code> if not defined.
     * @since 2.6
     */
    private static Plugin getPlugin( MavenProject p, String pluginId )
    {
        Plugin plugin = null;
        if ( p.getBuild() != null && p.getBuild().getPluginsAsMap() != null )
        {
            plugin = (Plugin) p.getBuild().getPluginsAsMap().get( pluginId );
            if ( plugin == null )
            {
                if ( p.getBuild().getPluginManagement() != null
                    && p.getBuild().getPluginManagement().getPluginsAsMap() != null )
                {
                    plugin = (Plugin) p.getBuild().getPluginManagement().getPluginsAsMap().get( pluginId );
                }
            }
        }

        return plugin;
    }

    /**
     * @param p not null
     * @param pluginId not null
     * @param param not null
     * @return the simple parameter as String defined in the plugin configuration by <code>param</code> key
     * or <code>null</code> if not found.
     * @since 2.6
     */
    private static String getPluginParameter( MavenProject p, String pluginId, String param )
    {
//        p.getGoalConfiguration( pluginGroupId, pluginArtifactId, executionId, goalId );
        Plugin plugin = getPlugin( p, pluginId );
        if ( plugin != null )
        {
            Xpp3Dom xpp3Dom = (Xpp3Dom) plugin.getConfiguration();
            if ( xpp3Dom != null && xpp3Dom.getChild( param ) != null
                && StringUtils.isNotEmpty( xpp3Dom.getChild( param ).getValue() ) )
            {
                return xpp3Dom.getChild( param ).getValue();
            }
        }

        return null;
    }
    
    /**
     * Construct the output file for the generated javadoc-options XML file, after creating the 
     * javadocOptionsDir if necessary. This method does NOT write to the file in question.
     * 
     * @since 2.7
     */
    protected final File getJavadocOptionsFile()
    {
        if ( javadocOptionsDir != null && !javadocOptionsDir.exists() )
        {
            javadocOptionsDir.mkdirs();
        }
        
        return new File( javadocOptionsDir, "javadoc-options-" + getAttachmentClassifier() + ".xml" );
    }
    
    /**
     * Generate a javadoc-options XML file, for either bundling with a javadoc-resources artifact OR
     * supplying to a distro module in a includeDependencySources configuration, so the javadoc options
     * from this execution can be reconstructed and merged in the distro build.
     * 
     * @since 2.7
     */
    protected final JavadocOptions buildJavadocOptions()
        throws IOException
    {
        JavadocOptions options = new JavadocOptions();
        
        options.setBootclasspathArtifacts( toList( bootclasspathArtifacts ) );
        options.setDocfilesSubdirsUsed( docfilessubdirs );
        options.setDocletArtifacts( toList( docletArtifact, docletArtifacts ) );
        options.setExcludedDocfilesSubdirs( excludedocfilessubdir );
        options.setExcludePackageNames( toList( excludePackageNames ) );
        options.setGroups( toList( groups ) );
        options.setLinks( links );
        options.setOfflineLinks( toList( offlineLinks ) );
        options.setResourcesArtifacts( toList( resourcesArtifacts ) );
        options.setTagletArtifacts( toList( tagletArtifact, tagletArtifacts ) );
        options.setTaglets( toList( taglets ) );
        options.setTags( toList( tags ) );
        
        if ( getProject() != null && getJavadocDirectory() != null )
        {
            options.setJavadocResourcesDirectory( toRelative( getProject().getBasedir(), getJavadocDirectory().getAbsolutePath() ) );
        }
        
        File optionsFile = getJavadocOptionsFile();
        FileWriter writer = null;
        try
        {
            writer = new FileWriter( optionsFile );
            new JavadocOptionsXpp3Writer().write( writer, options );
        }
        finally
        {
            close( writer );
        }
        
        return options;
    }
    
    /**
     * Override this if you need to provide a bundle attachment classifier, as in the case of test 
     * javadocs.
     */
    protected String getAttachmentClassifier()
    {
        return JAVADOC_RESOURCES_ATTACHMENT_CLASSIFIER;
    }
    
}
