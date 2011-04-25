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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.lang.ClassUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.AbstractInheritableJavaEntity;
import com.thoughtworks.qdox.model.AbstractJavaEntity;
import com.thoughtworks.qdox.model.Annotation;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.Type;
import com.thoughtworks.qdox.model.TypeVariable;
import com.thoughtworks.qdox.parser.ParseException;

/**
 * Abstract class to fix Javadoc documentation and tags in source files.
 * <br/>
 * See <a href="http://download.oracle.com/javase/1.5.0/docs/tooldocs/windows/javadoc.html#wheretags">Where Tags Can Be Used</a>.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.6
 */
public abstract class AbstractFixJavadocMojo
    extends AbstractMojo
{
    /** The vm line separator */
    private static final String EOL = System.getProperty( "line.separator" );

    /** Tag name for &#64;author **/
    private static final String AUTHOR_TAG = "author";

    /** Tag name for &#64;version **/
    private static final String VERSION_TAG = "version";

    /** Tag name for &#64;since **/
    private static final String SINCE_TAG = "since";

    /** Tag name for &#64;param **/
    private static final String PARAM_TAG = "param";

    /** Tag name for &#64;return **/
    private static final String RETURN_TAG = "return";

    /** Tag name for &#64;throws **/
    private static final String THROWS_TAG = "throws";

    /** Tag name for {&#64;inheritDoc} **/
    private static final String INHERITED_TAG = "{@inheritDoc}";

    /** Start Javadoc String i.e. <code>&#47;&#42;&#42;</code> **/
    private static final String START_JAVADOC = "/**";

    /** End Javadoc String i.e. <code>&#42;&#47;</code> **/
    private static final String END_JAVADOC = "*/";

    /** Javadoc Separator i.e. <code> &#42; </code> **/
    private static final String SEPARATOR_JAVADOC = " * ";

    /** Inherited Javadoc i.e. <code>&#47;&#42;&#42; {&#64;inheritDoc} &#42;&#47;</code> **/
    private static final String INHERITED_JAVADOC = START_JAVADOC + " " + INHERITED_TAG + " " + END_JAVADOC;

    /** <code>all</code> parameter used by {@link #fixTags} **/
    private static final String FIX_TAGS_ALL = "all";

    /** <code>public</code> parameter used by {@link #level} **/
    private static final String LEVEL_PUBLIC = "public";

    /** <code>protected</code> parameter used by {@link #level} **/
    private static final String LEVEL_PROTECTED = "protected";

    /** <code>package</code> parameter used by {@link #level} **/
    private static final String LEVEL_PACKAGE = "package";

    /** <code>private</code> parameter used by {@link #level} **/
    private static final String LEVEL_PRIVATE = "private";

    /** The Clirr Maven plugin groupId <code>org.codehaus.mojo</code> **/
    private static final String CLIRR_MAVEN_PLUGIN_GROUPID = "org.codehaus.mojo";

    /** The Clirr Maven plugin artifactId <code>clirr-maven-plugin</code> **/
    private static final String CLIRR_MAVEN_PLUGIN_ARTIFACTID = "clirr-maven-plugin";

    /** The latest Clirr Maven plugin version <code>2.2.2</code> **/
    private static final String CLIRR_MAVEN_PLUGIN_VERSION = "2.2.2";

    /** The Clirr Maven plugin goal <code>check</code> **/
    private static final String CLIRR_MAVEN_PLUGIN_GOAL = "check";

    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Input handler, needed for command line handling.
     *
     * @component
     */
    private InputHandler inputHandler;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * Version to compare the current code against using the
     * <a href="http://mojo.codehaus.org/clirr-maven-plugin/">Clirr Maven Plugin</a>.
     * <br/>
     * See <a href="#defaultSince">defaultSince</a>.
     *
     * @parameter expression="${comparisonVersion}" default-value="(,${project.version})"
     */
    private String comparisonVersion;

    /**
     * Default value for the Javadoc tag <code>&#64;author</code>.
     * <br/>
     * If not specified, the <code>user.name</code> defined in the System properties will be used.
     *
     * @parameter expression="${defaultAuthor}"
     */
    private String defaultAuthor;

    /**
     * Default value for the Javadoc tag <code>&#64;since</code>.
     * <br/>
     *
     * @parameter expression="${defaultSince}" default-value="${project.version}"
     */
    private String defaultSince;

    /**
     * Default value for the Javadoc tag <code>&#64;version</code>.
     * <br/>
     * By default, it is <code>&#36;Id:&#36;</code>, corresponding to a
     * <a href="http://svnbook.red-bean.com/en/1.1/ch07s02.html#svn-ch-7-sect-2.3.4">SVN keyword</a>.
     * Refer to your SCM to use an other SCM keyword.
     *
     * @parameter expression="${defaultVersion}"
     */
    private String defaultVersion = "\u0024Id: \u0024"; // can't use default-value="\u0024Id: \u0024"

    /**
     * The file encoding to use when reading the source files. If the property
     * <code>project.build.sourceEncoding</code> is not set, the platform default encoding is used.
     *
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     */
    private String encoding;

    /**
     * Comma separated excludes Java files, i.e. <code>&#42;&#42;/&#42;Test.java</code>.
     *
     * @parameter expression="${excludes}"
     */
    private String excludes;

    /**
     * Comma separated tags to fix in classes, interfaces or methods Javadoc comments.
     * Possible values are:
     * <ul>
     * <li>all (fix all Javadoc tags)</li>
     * <li>author (fix only &#64;author tag)</li>
     * <li>version (fix only &#64;version tag)</li>
     * <li>since (fix only &#64;since tag)</li>
     * <li>param (fix only &#64;param tag)</li>
     * <li>return (fix only &#64;return tag)</li>
     * <li>throws (fix only &#64;throws tag)</li>
     * </ul>
     *
     * @parameter expression="${fixTags}" default-value="all"
     */
    private String fixTags;

    /**
     * Flag to fix the classes or interfaces Javadoc comments according the <code>level</code>.
     *
     * @parameter expression="${fixClassComment}" default-value="true"
     */
    private boolean fixClassComment;

    /**
     * Flag to fix the fields Javadoc comments according the <code>level</code>.
     *
     * @parameter expression="${fixFieldComment}" default-value="true"
     */
    private boolean fixFieldComment;

    /**
     * Flag to fix the methods Javadoc comments according the <code>level</code>.
     *
     * @parameter expression="${fixMethodComment}" default-value="true"
     */
    private boolean fixMethodComment;

    /**
     * Forcing the goal execution i.e. skip warranty messages (not recommended).
     *
     * @parameter expression="${force}"
     */
    private boolean force;

    /**
     * Flag to ignore or not Clirr.
     *
     * @parameter expression="${ignoreClirr}" default-value="false"
     */
    protected boolean ignoreClirr;

    /**
     * Comma separated includes Java files, i.e. <code>&#42;&#42;/&#42;Test.java</code>.
     *
     * @parameter expression="${includes}" default-value="**\/*.java"
     */
    private String includes;

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
     * @parameter expression="${level}" default-value="protected"
     */
    private String level;

    /**
     * The local repository where the artifacts are located, used by the tests.
     *
     * @parameter expression="${localRepository}"
     */
    private ArtifactRepository localRepository;

    /**
     * Output directory where Java classes will be rewritten.
     *
     * @parameter expression="${outputDirectory}" default-value="${project.build.sourceDirectory}"
     */
    private File outputDirectory;

    /**
     * The Maven Project Object.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The current user system settings for use in Maven.
     *
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    // ----------------------------------------------------------------------
    // Internal fields
    // ----------------------------------------------------------------------

    /** The current project class loader. */
    private ClassLoader projectClassLoader;

    /**
     * Split {@link #fixTags} by comma.
     * @see {@link #init()}
     */
    private String[] fixTagsSplitted;

    /** New classes found by Clirr. */
    private List<String> clirrNewClasses;

    /** New Methods in a Class (the key) found by Clirr. */
    private Map<String, List<String>> clirrNewMethods;

    /** List of classes where <code>&#42;since</code> is added. Will be used to add or not this tag in the methods. */
    private List<String> sinceClasses;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !fixClassComment && !fixFieldComment && !fixMethodComment )
        {
            getLog().info( "Specified to NOT fix classes, fields and methods. Nothing to do." );
            return;
        }

        // verify goal params
        init();

        if ( fixTagsSplitted.length == 0 )
        {
            getLog().info( "No fix tag specified. Nothing to do." );
            return;
        }

        // add warranty msg
        if ( !preCheck() )
        {
            return;
        }

        // run clirr
        try
        {
            executeClirr();
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
            getLog().info( "Clirr is ignored." );
        }

        // run qdox and process
        try
        {
            JavaClass[] javaClasses = getQdoxClasses();

            if ( javaClasses != null )
            {
                for ( int i = 0; i < javaClasses.length; i++ )
                {
                    JavaClass javaClass = javaClasses[i];

                    processFix( javaClass );
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "IOException: " + e.getMessage(), e );
        }
    }

    // ----------------------------------------------------------------------
    // protected methods
    // ----------------------------------------------------------------------

    /**
     * @param p not null maven project.
     * @return the artifact type.
     */
    protected String getArtifactType( MavenProject p )
    {
        return p.getArtifact().getType();
    }

    /**
     * @param p not null maven project.
     * @return the list of source paths for the given project.
     */
    protected List<String> getProjectSourceRoots( MavenProject p )
    {
        return ( p.getCompileSourceRoots() == null ? Collections.<String>emptyList()
                        : new LinkedList<String>( p.getCompileSourceRoots() ) );
    }

    /**
     * @param p not null
     * @return the compile classpath elements
     * @throws DependencyResolutionRequiredException if any
     */
    protected List<String> getCompileClasspathElements( MavenProject p )
        throws DependencyResolutionRequiredException
    {
        return ( p.getCompileClasspathElements() == null ? Collections.<String>emptyList()
                        : new LinkedList<String>( p.getCompileClasspathElements() ) );
    }

    /**
     * @param javaMethod not null
     * @return the fully qualify name of javaMethod with signature
     */
    protected static String getJavaMethodAsString( JavaMethod javaMethod )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( javaMethod.getParentClass().getFullyQualifiedName() );
        sb.append( "#" ).append( javaMethod.getCallSignature() );

        return sb.toString();
    }

    // ----------------------------------------------------------------------
    // private methods
    // ----------------------------------------------------------------------

    /**
     * Init goal parameters.
     */
    private void init()
    {
        // defaultAuthor
        if ( StringUtils.isEmpty( defaultAuthor ) )
        {
            defaultAuthor = System.getProperty( "user.name" );
        }

        // defaultSince
        int i = defaultSince.indexOf( "-" + Artifact.SNAPSHOT_VERSION );
        if ( i != -1 )
        {
            defaultSince = defaultSince.substring( 0, i );
        }

        // fixTags
        if ( !FIX_TAGS_ALL.equalsIgnoreCase( fixTags.trim() ) )
        {
            String[] split = StringUtils.split( fixTags, "," );
            List<String> filtered = new LinkedList<String>();
            for ( int j = 0; j < split.length; j++ )
            {
                String s = split[j].trim();
                if ( FIX_TAGS_ALL.equalsIgnoreCase( s.trim() ) || AUTHOR_TAG.equalsIgnoreCase( s.trim() )
                    || VERSION_TAG.equalsIgnoreCase( s.trim() ) || SINCE_TAG.equalsIgnoreCase( s.trim() )
                    || PARAM_TAG.equalsIgnoreCase( s.trim() ) || RETURN_TAG.equalsIgnoreCase( s.trim() )
                    || THROWS_TAG.equalsIgnoreCase( s.trim() ) )
                {
                    filtered.add( s );
                }
                else
                {
                    if ( getLog().isWarnEnabled() )
                    {
                        getLog().warn( "Unrecognized '" + s + "' for fixTags parameter. Ignored it!" );
                    }
                }
            }
            fixTags = StringUtils.join( filtered.iterator(), "," );
        }
        fixTagsSplitted = StringUtils.split( fixTags, "," );

        // encoding
        if ( StringUtils.isEmpty( encoding ) )
        {
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn(
                               "File encoding has not been set, using platform encoding "
                                   + ReaderFactory.FILE_ENCODING + ", i.e. build is platform dependent!" );
            }
            encoding = ReaderFactory.FILE_ENCODING;
        }

        // level
        if ( !( LEVEL_PUBLIC.equalsIgnoreCase( level.trim() ) || LEVEL_PROTECTED.equalsIgnoreCase( level.trim() )
            || LEVEL_PACKAGE.equalsIgnoreCase( level.trim() ) || LEVEL_PRIVATE.equalsIgnoreCase( level.trim() ) ) )
        {
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn( "Unrecognized '" + level + "' for level parameter, using 'protected' level." );
            }
            level = "protected";
        }
    }

    /**
     * @return <code>true</code> if the user wants to proceed, <code>false</code> otherwise.
     * @throws MojoExecutionException if any
     */
    private boolean preCheck()
        throws MojoExecutionException
    {
        if ( force )
        {
            return true;
        }

        if ( outputDirectory != null
            && !outputDirectory.getAbsolutePath().equals( getProjectSourceDirectory().getAbsolutePath() ) )
        {
            return true;
        }

        if ( !settings.isInteractiveMode() )
        {
            getLog().error(
                            "Maven is not attempt to interact with the user for input. "
                                + "Verify the <interactiveMode/> configuration in your settings." );
            return false;
        }

        getLog().warn( "" );
        getLog().warn( "    WARRANTY DISCLAIMER" );
        getLog().warn( "" );
        getLog().warn( "All warranties with regard to this Maven goal are disclaimed!" );
        getLog().warn( "The changes will be done directly in the source code." );
        getLog().warn(
                       "The Maven Team strongly recommends the use of a SCM software BEFORE executing this "
                           + "goal." );
        getLog().warn( "" );

        while ( true )
        {
            getLog().info( "Are you sure to proceed? [Y]es [N]o" );

            try
            {
                String userExpression = inputHandler.readLine();
                if ( userExpression == null || userExpression.toLowerCase( Locale.ENGLISH ).equalsIgnoreCase( "Y" )
                    || userExpression.toLowerCase( Locale.ENGLISH ).equalsIgnoreCase( "Yes" ) )
                {
                    getLog().info( "OK, let's proceed..." );
                    break;
                }
                if ( userExpression == null || userExpression.toLowerCase( Locale.ENGLISH ).equalsIgnoreCase( "N" )
                    || userExpression.toLowerCase( Locale.ENGLISH ).equalsIgnoreCase( "No" ) )
                {
                    getLog().info( "No changes in your sources occur." );
                    return false;
                }
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Unable to read from standard input.", e );
            }
        }

        return true;
    }

    /**
     * @return the source dir as File for the given project
     */
    private File getProjectSourceDirectory()
    {
        return new File( project.getBuild().getSourceDirectory() );
    }

    /**
     * Invoke Maven to run clirr-maven-plugin to find API differences.
     *
     * @throws MavenInvocationException if any
     */
    private void executeClirr()
        throws MavenInvocationException
    {
        if ( ignoreClirr )
        {
            getLog().info( "Clirr is ignored." );
            return;
        }

        String clirrGoal = getFullClirrGoal();

        // http://mojo.codehaus.org/clirr-maven-plugin/check-mojo.html
        File clirrTextOutputFile =
            FileUtils.createTempFile( "clirr", ".txt", new File( project.getBuild().getDirectory() ) );
        Properties properties = new Properties();
        properties.put( "textOutputFile", clirrTextOutputFile.getAbsolutePath() );
        properties.put( "comparisonVersion", comparisonVersion );
        properties.put( "failOnError", "false" );

        File invokerDir = new File( project.getBuild().getDirectory(), "invoker" );
        invokerDir.mkdirs();
        File invokerLogFile = FileUtils.createTempFile( "clirr-maven-plugin", ".txt", invokerDir );
        new File( project.getBuild().getDirectory(), "invoker-clirr-maven-plugin.txt" );
        JavadocUtil.invokeMaven( getLog(), new File( localRepository.getBasedir() ), project.getFile(),
                                 Collections.singletonList( clirrGoal ), properties, invokerLogFile );

        try
        {
            if ( invokerLogFile.exists() )
            {
                String invokerLogContent = StringUtils.unifyLineSeparators( FileUtils.fileRead( invokerLogFile, "UTF-8" ) );
                // see org.codehaus.mojo.clirr.AbstractClirrMojo#getComparisonArtifact()
                final String artifactNotFoundMsg =
                    "Unable to find a previous version of the project in the repository";
                if ( invokerLogContent.indexOf( artifactNotFoundMsg ) != -1 )
                {
                    getLog().warn( "No previous artifact has been deployed, Clirr is ignored." );
                    return;
                }
            }
        }
        catch ( IOException e )
        {
            getLog().debug( "IOException: " + e.getMessage() );
        }

        try
        {
            parseClirrTextOutputFile( clirrTextOutputFile );
        }
        catch ( IOException e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "IOException: " + e.getMessage(), e );
            }
            getLog().info(
                           "IOException when parsing Clirr output '" + clirrTextOutputFile.getAbsolutePath()
                               + "', Clirr is ignored." );
        }
    }

    /**
     * @param clirrTextOutputFile not null
     * @throws IOException if any
     */
    private void parseClirrTextOutputFile( File clirrTextOutputFile )
        throws IOException
    {
        if ( !clirrTextOutputFile.exists() )
        {
            if ( getLog().isInfoEnabled() )
            {
                getLog().info(
                               "No Clirr output file '" + clirrTextOutputFile.getAbsolutePath()
                                   + "' exists, Clirr is ignored." );
            }
            return;
        }

        if ( getLog().isInfoEnabled() )
        {
            getLog().info( "Clirr output file was created: " + clirrTextOutputFile.getAbsolutePath() );
        }

        clirrNewClasses = new LinkedList<String>();
        clirrNewMethods = new LinkedHashMap<String, List<String>>();

        BufferedReader input = new BufferedReader( ReaderFactory.newReader( clirrTextOutputFile, "UTF-8" ) );
        String line = null;
        while ( ( line = input.readLine() ) != null )
        {
            String[] split = StringUtils.split( line, ":" );
            if ( split.length != 4 )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "Unable to parse the clirr line: " + line );
                }
                continue;
            }

            int code;
            try
            {
                code = Integer.parseInt( split[1].trim() );
            }
            catch ( NumberFormatException e )
            {
                if ( getLog().isDebugEnabled() )
                {
                    getLog().debug( "Unable to parse the clirr line: " + line );
                }
                continue;
            }

            // http://clirr.sourceforge.net/clirr-core/exegesis.html
            // 7011 - Method Added
            // 7012 - Method Added to Interface
            // 8000 - Class Added
            List<String> list;
            String[] splits2;
            switch ( code )
            {
                case 7011:
                    list = clirrNewMethods.get( split[2].trim() );
                    if ( list == null )
                    {
                        list = new ArrayList<String>();
                    }
                    splits2 = StringUtils.split( split[3].trim(), "'" );
                    if ( splits2.length != 3 )
                    {
                        continue;
                    }
                    list.add( splits2[1].trim() );
                    clirrNewMethods.put( split[2].trim(), list );
                    break;

                case 7012:
                    list = clirrNewMethods.get( split[2].trim() );
                    if ( list == null )
                    {
                        list = new ArrayList<String>();
                    }
                    splits2 = StringUtils.split( split[3].trim(), "'" );
                    if ( splits2.length != 3 )
                    {
                        continue;
                    }
                    list.add( splits2[1].trim() );
                    clirrNewMethods.put( split[2].trim(), list );
                    break;

                case 8000:
                    clirrNewClasses.add( split[2].trim() );
                    break;
                default:
                    break;
            }
        }

        if ( clirrNewClasses.isEmpty() && clirrNewMethods.isEmpty() )
        {
            getLog().info( "Clirr NOT found API differences." );
        }
        else
        {
            getLog().info( "Clirr found API differences, i.e. new classes/interfaces or methods." );
        }
    }

    /**
     * @param tag not null
     * @return <code>true</code> if <code>tag</code> is defined in {@link #fixTags}.
     */
    private boolean fixTag( String tag )
    {
        if ( fixTagsSplitted.length == 1 && fixTagsSplitted[0].equals( FIX_TAGS_ALL ) )
        {
            return true;
        }

        for ( int i = 0; i < fixTagsSplitted.length; i++ )
        {
            if ( fixTagsSplitted[i].trim().equals( tag ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Calling Qdox to find {@link JavaClass} objects from the Maven project sources.
     * Ignore java class if Qdox has parsing errors.
     *
     * @return an array of {@link JavaClass} found by QDox
     * @throws IOException if any
     * @throws MojoExecutionException if any
     */
    private JavaClass[] getQdoxClasses()
        throws IOException, MojoExecutionException
    {
        if ( "pom".equals( project.getPackaging().toLowerCase() ) )
        {
            getLog().warn( "This project has 'pom' packaging, no Java sources is available." );
            return null;
        }

        List<File> javaFiles = new LinkedList<File>();
        for ( String sourceRoot : getProjectSourceRoots( project ) )
        {
            File f = new File( sourceRoot );
            if ( f.isDirectory() )
            {
                javaFiles.addAll( FileUtils.getFiles( f, includes, excludes, true ) );
            }
            else
            {
                if ( getLog().isWarnEnabled() )
                {
                    getLog().warn( f + " doesn't exist. Ignored it." );
                }
            }
        }

        JavaDocBuilder builder = new JavaDocBuilder();
        builder.getClassLibrary().addClassLoader( getProjectClassLoader() );
        builder.setEncoding( encoding );
        for ( File f : javaFiles )
        {
            if ( !f.getAbsolutePath().toLowerCase( Locale.ENGLISH ).endsWith( ".java" )
                && getLog().isWarnEnabled() )
            {
                getLog().warn( "'" + f + "' is not a Java file. Ignored it." );
                continue;
            }

            try
            {
                builder.addSource( f );
            }
            catch ( ParseException e )
            {
                if ( getLog().isWarnEnabled() )
                {
                    getLog().warn( "QDOX ParseException: " + e.getMessage() + ". Can't fix it." );
                }
            }
        }

        return builder.getClasses();
    }

    /**
     * @return the classLoader for the given project using lazy instantiation.
     * @throws MojoExecutionException if any
     */
    private ClassLoader getProjectClassLoader()
        throws MojoExecutionException
    {
        if ( projectClassLoader == null )
        {
            List<String> classPath;
            try
            {
                classPath = getCompileClasspathElements( project );
            }
            catch ( DependencyResolutionRequiredException e )
            {
                throw new MojoExecutionException( "DependencyResolutionRequiredException: " + e.getMessage(), e );
            }

            List<URL> urls = new ArrayList<URL>( classPath.size() );
            for ( String filename : classPath )
            {
                try
                {
                    urls.add( new File( filename ).toURL() );
                }
                catch ( MalformedURLException e )
                {
                    throw new MojoExecutionException( "MalformedURLException: " + e.getMessage(), e );
                }
            }

            projectClassLoader = new URLClassLoader( (URL[]) urls.toArray( new URL[urls.size()] ), null );
        }

        return projectClassLoader;
    }

    /**
     * Process the given {@link JavaClass}, ie add missing javadoc tags depending user parameters.
     *
     * @param javaClass not null
     * @throws IOException if any
     * @throws MojoExecutionException if any
     */
    private void processFix( JavaClass javaClass )
        throws IOException, MojoExecutionException
    {
        // Skipping inner classes
        if ( javaClass.isInner() )
        {
            return;
        }

        File javaFile = new File( javaClass.getSource().getURL().getFile() );
        // the original java content in memory
        final String originalContent = StringUtils.unifyLineSeparators( FileUtils.fileRead( javaFile, encoding ) );

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Fixing " + javaClass.getFullyQualifiedName() );
        }

        final StringWriter stringWriter = new StringWriter();
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader( new StringReader( originalContent ) );

            String line;
            int lineNumber = 0;
            while ( ( line = reader.readLine() ) != null )
            {
                lineNumber++;
                final String indent = autodetectIndentation( line );

                // fixing classes
                if ( javaClass.getComment() == null && javaClass.getAnnotations() != null
                    && javaClass.getAnnotations().length != 0 )
                {
                    if ( lineNumber == javaClass.getAnnotations()[0].getLineNumber() )
                    {
                        fixClassComment( stringWriter, originalContent, javaClass, indent );

                        takeCareSingleComment( stringWriter, originalContent, javaClass );
                    }
                }
                else
                {
                    if ( lineNumber == javaClass.getLineNumber() )
                    {
                        fixClassComment( stringWriter, originalContent, javaClass, indent );

                        takeCareSingleComment( stringWriter, originalContent, javaClass );
                    }
                }

                // fixing fields
                if ( javaClass.getFields() != null )
                {
                    for ( int i = 0; i < javaClass.getFields().length; i++ )
                    {
                        JavaField field = javaClass.getFields()[i];

                        if ( lineNumber == field.getLineNumber() )
                        {
                            fixFieldComment( stringWriter, javaClass, field, indent );
                        }
                    }
                }

                // fixing methods
                if ( javaClass.getMethods() != null )
                {
                    for ( int i = 0; i < javaClass.getMethods().length; i++ )
                    {
                        JavaMethod method = javaClass.getMethods()[i];

                        if ( lineNumber == method.getLineNumber() )
                        {
                            fixMethodComment( stringWriter, originalContent, method, indent );

                            takeCareSingleComment( stringWriter, originalContent, method );
                        }
                    }
                }

                stringWriter.write( line );
                stringWriter.write( EOL );
            }
        }
        finally
        {
            IOUtil.close( reader );
        }

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Saving " + javaClass.getFullyQualifiedName() );
        }

        if ( outputDirectory != null
            && !outputDirectory.getAbsolutePath().equals( getProjectSourceDirectory().getAbsolutePath() ) )
        {
            String path =
                StringUtils.replace( javaFile.getAbsolutePath().replaceAll( "\\\\", "/" ),
                                     project.getBuild().getSourceDirectory().replaceAll( "\\\\", "/" ), "" );
            javaFile = new File( outputDirectory, path );
            javaFile.getParentFile().mkdirs();
        }
        writeFile( javaFile, encoding, stringWriter.toString() );
    }

    /**
     * Take care of block or single comments between Javadoc comment and entity declaration ie:
     * <br/>
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff">&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;</font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;{Javadoc&nbsp;Comment}</font><br />
     * <font color="#808080">3</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&#47;</font><br />
     * <font color="#808080">4</font>&nbsp;<font color="#ffffff">&nbsp;</font>
     * <font color="#3f7f5f">&#47;&#42;</font><br />
     * <font color="#808080">5</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f7f5f">&#42;&nbsp;{Block&nbsp;Comment}</font><br />
     * <font color="#808080">6</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f7f5f">&#42;&#47;</font><br />
     * <font color="#808080">7</font>&nbsp;<font color="#ffffff">&nbsp;</font>
     * <font color="#3f7f5f">&#47;&#47;&nbsp;{Single&nbsp;comment}</font><br />
     * <font color="#808080">8</font>&nbsp;<font color="#ffffff">&nbsp;</font>
     * <font color="#7f0055"><b>public&nbsp;</b></font><font color="#7f0055"><b>void&nbsp;</b></font>
     * <font color="#000000">dummyMethod</font><font color="#000000">(&nbsp;</font>
     * <font color="#000000">String&nbsp;s&nbsp;</font><font color="#000000">){}</font>
     * </code>
     *
     * @param stringWriter not null
     * @param originalContent not null
     * @param entity not null
     * @throws IOException if any
     * @see #extractOriginalJavadoc(String, AbstractJavaEntity)
     */
    private void takeCareSingleComment( final StringWriter stringWriter, final String originalContent,
                                        final AbstractInheritableJavaEntity entity )
        throws IOException
    {
        if ( entity.getComment() == null )
        {
            return;
        }

        String javadocComment = trimRight( extractOriginalJavadoc( originalContent, entity ) );
        String extraComment =
            javadocComment.substring( javadocComment.indexOf( END_JAVADOC ) + END_JAVADOC.length() );
        if ( StringUtils.isNotEmpty( extraComment ) )
        {
            if ( extraComment.indexOf( EOL ) != -1 )
            {
                stringWriter.write( extraComment.substring( extraComment.indexOf( EOL ) + EOL.length() ) );
            }
            else
            {
                stringWriter.write( extraComment );
            }
            stringWriter.write( EOL );
        }
    }

    /**
     * Add/update Javadoc class comment.
     *
     * @param stringWriter
     * @param originalContent
     * @param javaClass
     * @param indent
     * @throws MojoExecutionException
     * @throws IOException
     */
    private void fixClassComment( final StringWriter stringWriter, final String originalContent,
                                  final JavaClass javaClass, final String indent )
        throws MojoExecutionException, IOException
    {
        if ( !fixClassComment )
        {
            return;
        }

        if ( !isInLevel( javaClass.getModifiers() ) )
        {
            return;
        }

        // add
        if ( javaClass.getComment() == null )
        {
            addDefaultClassComment( stringWriter, javaClass, indent );
            return;
        }

        // update
        updateEntityComment( stringWriter, originalContent, javaClass, indent );
    }

    /**
     * @param modifiers list of modifiers (public, private, protected, package)
     * @return <code>true</code> if modifier is align with <code>level</code>.
     */
    private boolean isInLevel( String[] modifiers )
    {
        List<String> modifiersAsList = Arrays.asList( modifiers );

        if ( LEVEL_PUBLIC.equalsIgnoreCase( level.trim() ) )
        {
            if ( modifiersAsList.contains( LEVEL_PUBLIC ) )
            {
                return true;
            }

            return false;
        }

        if ( LEVEL_PROTECTED.equalsIgnoreCase( level.trim() ) )
        {
            if ( modifiersAsList.contains( LEVEL_PUBLIC ) || modifiersAsList.contains( LEVEL_PROTECTED ) )
            {
                return true;
            }

            return false;
        }

        if ( LEVEL_PACKAGE.equalsIgnoreCase( level.trim() ) )
        {
            if ( !modifiersAsList.contains( LEVEL_PRIVATE ) )
            {
                return true;
            }

            return false;
        }

        // should be private (shows all classes and members)
        return true;
    }

    /**
     * Add a default Javadoc for the given class, i.e.:
     * <br/>
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff">&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;</font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;{Comment&nbsp;based&nbsp;on&nbsp;the&nbsp;class&nbsp;name}</font><br />
     * <font color="#808080">3</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;</font><br />
     * <font color="#808080">4</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@author&nbsp;</font>
     * <font color="#3f5fbf">X&nbsp;{added&nbsp;if&nbsp;addMissingAuthor}</font><br />
     * <font color="#808080">5</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@version&nbsp;</font>
     * <font color="#3f5fbf">X&nbsp;{added&nbsp;if&nbsp;addMissingVersion}</font><br />
     * <font color="#808080">6</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@since&nbsp;</font>
     * <font color="#3f5fbf">X&nbsp;{added&nbsp;if&nbsp;addMissingSince&nbsp;and&nbsp;new&nbsp;classes
     * from&nbsp;previous&nbsp;version}</font><br />
     * <font color="#808080">7</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&#47;</font><br />
     * <font color="#808080">8</font>&nbsp;<font color="#7f0055"><b>public&nbsp;class&nbsp;</b></font>
     * <font color="#000000">DummyClass&nbsp;</font><font color="#000000">{}</font></code>
     * </code>
     *
     * @param buffer not null
     * @param javaClass not null
     * @param indent not null
     * @see #getDefaultClassJavadocComment(JavaClass)
     * @see #appendDefaultAuthorTag(StringBuffer, String)
     * @see #appendDefaultSinceTag(StringBuffer, String)
     * @see #appendDefaultVersionTag(StringBuffer, String)
     */
    private void addDefaultClassComment( final StringWriter stringWriter, final JavaClass javaClass,
                                         final String indent )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( indent ).append( START_JAVADOC );
        sb.append( EOL );
        sb.append( indent ).append( SEPARATOR_JAVADOC );
        sb.append( getDefaultClassJavadocComment( javaClass ) );
        sb.append( EOL );

        appendSeparator( sb, indent );

        appendDefaultAuthorTag( sb, indent );

        appendDefaultVersionTag( sb, indent );

        if ( fixTag( SINCE_TAG ) )
        {
            if ( !ignoreClirr )
            {
                if ( isNewClassFromLastVersion( javaClass ) )
                {
                    appendDefaultSinceTag( sb, indent );
                }
            }
            else
            {
                appendDefaultSinceTag( sb, indent );
                addSinceClasses( javaClass );
            }
        }

        sb.append( indent ).append( " " ).append( END_JAVADOC );
        sb.append( EOL );

        stringWriter.write( sb.toString() );
    }

    /**
     * Add Javadoc field comment, only for static fields or interface fields.
     *
     * @param stringWriter not null
     * @param javaClass not null
     * @param field not null
     * @param indent not null
     * @throws IOException if any
     */
    private void fixFieldComment( final StringWriter stringWriter, final JavaClass javaClass,
                                  final JavaField field, final String indent )
        throws IOException
    {
        if ( !fixFieldComment )
        {
            return;
        }

        if ( !javaClass.isInterface() )
        {
            if ( !isInLevel( field.getModifiers() ) )
            {
                return;
            }

            if ( !field.isStatic() )
            {
                return;
            }
        }

        // add
        if ( field.getComment() == null )
        {
            addDefaultFieldComment( stringWriter, field, indent );
            return;
        }

        // no update
    }

    /**
     * Add a default Javadoc for the given field, i.e.:
     * <br/>
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;&nbsp;Constant&nbsp;</font><font color="#7f7f9f">&lt;code&gt;</font>
     * <font color="#3f5fbf">MY_STRING_CONSTANT=&#34;value&#34;</font>
     * <font color="#7f7f9f">&lt;/code&gt;&nbsp;</font><font color="#3f5fbf">&#42;&#47;</font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#7f0055"><b>public&nbsp;static&nbsp;final&nbsp;</b></font>
     * <font color="#000000">String&nbsp;MY_STRING_CONSTANT&nbsp;=&nbsp;</font>
     * <font color="#2a00ff">&#34;value&#34;</font><font color="#000000">;</font>
     * </code>
     *
     * @param stringWriter not null
     * @param field not null
     * @param indent not null
     * @throws IOException if any
     */
    private void addDefaultFieldComment( final StringWriter stringWriter, final JavaField field,
                                         final String indent )
        throws IOException
    {
        StringBuffer sb = new StringBuffer();

        sb.append( indent ).append( START_JAVADOC ).append( " " );
        sb.append( "Constant <code>" ).append( field.getName() );

        if ( StringUtils.isNotEmpty( field.getInitializationExpression() ) )
        {
            String qualifiedName = field.getType().getJavaClass().getFullyQualifiedName();

            if ( qualifiedName.equals( Byte.TYPE.toString() ) || qualifiedName.equals( Short.TYPE.toString() )
                || qualifiedName.equals( Integer.TYPE.toString() ) || qualifiedName.equals( Long.TYPE.toString() )
                || qualifiedName.equals( Float.TYPE.toString() ) || qualifiedName.equals( Double.TYPE.toString() )
                || qualifiedName.equals( Boolean.TYPE.toString() )
                || qualifiedName.equals( Character.TYPE.toString() ) )
            {
                sb.append( "=" );
                sb.append( field.getInitializationExpression().trim() );
            }

            if ( qualifiedName.equals( String.class.getName() ) )
            {
                StringBuffer value = new StringBuffer();
                String[] lines = getLines( field.getInitializationExpression() );
                for ( int i = 0; i < lines.length; i++ )
                {
                    String line = lines[i];

                    StringTokenizer token = new StringTokenizer( line.trim(), "\"\n\r" );
                    while ( token.hasMoreTokens() )
                    {
                        String s = token.nextToken();

                        if ( s.trim().equals( "+" ) )
                        {
                            continue;
                        }
                        if ( s.trim().endsWith( "\\" ) )
                        {
                            s += "\"";
                        }
                        value.append( s );
                    }
                }

                sb.append( "=\"" );
                // reduce the size
                if ( value.length() < 40 )
                {
                    sb.append( value.toString() ).append( "\"" );
                }
                else
                {
                    sb.append( value.toString().substring( 0, 39 ) ).append( "\"{trunked}" );
                }
            }
        }

        sb.append( "</code> " ).append( END_JAVADOC );
        sb.append( EOL );

        stringWriter.write( sb.toString() );
    }

    /**
     * Add/update Javadoc method comment.
     *
     * @param stringWriter not null
     * @param originalContent not null
     * @param javaMethod not null
     * @param indent not null
     * @throws MojoExecutionException if any
     * @throws IOException if any
     */
    private void fixMethodComment( final StringWriter stringWriter, final String originalContent,
                                   final JavaMethod javaMethod, final String indent )
        throws MojoExecutionException, IOException
    {
        if ( !fixMethodComment )
        {
            return;
        }

        if ( !javaMethod.getParentClass().isInterface() && !isInLevel( javaMethod.getModifiers() ) )
        {
            return;
        }

        // add
        if ( javaMethod.getComment() == null )
        {
            addDefaultMethodComment( stringWriter, javaMethod, indent );
            return;
        }

        // update
        updateEntityComment( stringWriter, originalContent, javaMethod, indent );
    }

    /**
     * Add in the buffer a default Javadoc for the given class:
     * <br/>
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff">&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;</font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;{Comment&nbsp;based&nbsp;on&nbsp;the&nbsp;method&nbsp;name}</font><br />
     * <font color="#808080">3</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;</font><br />
     * <font color="#808080">4</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@param&nbsp;</font>
     * <font color="#3f5fbf">X&nbsp;{added&nbsp;if&nbsp;addMissingParam}</font><br />
     * <font color="#808080">5</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@return&nbsp;</font>
     * <font color="#3f5fbf">X&nbsp;{added&nbsp;if&nbsp;addMissingReturn}</font><br />
     * <font color="#808080">6</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@throws&nbsp;</font>
     * <font color="#3f5fbf">X&nbsp;{added&nbsp;if&nbsp;addMissingThrows}</font><br />
     * <font color="#808080">7</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@since&nbsp;</font>
     * <font color="#3f5fbf">X&nbsp;{added&nbsp;if&nbsp;addMissingSince&nbsp;and&nbsp;new&nbsp;classes
     * from&nbsp;previous&nbsp;version}</font><br />
     * <font color="#808080">8</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&#47;</font><br />
     * <font color="#808080">9</font>&nbsp;<font color="#7f0055"><b>public&nbsp;</b></font>
     * <font color="#7f0055"><b>void&nbsp;</b></font><font color="#000000">dummyMethod</font>
     * <font color="#000000">(&nbsp;</font><font color="#000000">String&nbsp;s&nbsp;</font>
     * <font color="#000000">){}</font>
     * </code>
     *
     * @param buffer not null
     * @param javaMethod not null
     * @param indent not null
     * @throws MojoExecutionException if any
     * @see #getDefaultMethodJavadocComment(JavaMethod)
     * @see #appendDefaultSinceTag(StringBuffer, String)
     */
    private void addDefaultMethodComment( final StringWriter stringWriter, final JavaMethod javaMethod,
                                          final String indent )
        throws MojoExecutionException
    {
        StringBuffer sb = new StringBuffer();

        // special case
        if ( isInherited( javaMethod ) )
        {
            sb.append( indent ).append( INHERITED_JAVADOC );
            sb.append( EOL );

            stringWriter.write( sb.toString() );
            return;
        }

        sb.append( indent ).append( START_JAVADOC );
        sb.append( EOL );
        sb.append( indent ).append( SEPARATOR_JAVADOC );
        sb.append( getDefaultMethodJavadocComment( javaMethod ) );
        sb.append( EOL );

        boolean separatorAdded = false;
        if ( fixTag( PARAM_TAG ) )
        {
            if ( javaMethod.getParameters() != null )
            {
                for ( int i = 0; i < javaMethod.getParameters().length; i++ )
                {
                    JavaParameter javaParameter = javaMethod.getParameters()[i];

                    separatorAdded = appendDefaultParamTag( sb, indent, separatorAdded, javaParameter );
                }
            }
            // is generic?
            if ( javaMethod.getTypeParameters() != null )
            {
                for ( int i = 0; i < javaMethod.getTypeParameters().length; i++ )
                {
                    TypeVariable typeParam = javaMethod.getTypeParameters()[i];

                    separatorAdded = appendDefaultParamTag( sb, indent, separatorAdded, typeParam );
                }
            }
        }
        if ( fixTag( RETURN_TAG ) && javaMethod.getReturns() != null && !javaMethod.getReturns().isVoid() )
        {
            separatorAdded = appendDefaultReturnTag( sb, indent, separatorAdded, javaMethod );
        }
        if ( fixTag( THROWS_TAG ) && javaMethod.getExceptions() != null && javaMethod.getExceptions().length > 0 )
        {
            for ( int i = 0; i < javaMethod.getExceptions().length; i++ )
            {
                Type exception = javaMethod.getExceptions()[i];

                separatorAdded = appendDefaultThrowsTag( sb, indent, separatorAdded, exception );
            }
        }
        if ( fixTag( SINCE_TAG ) && isNewMethodFromLastRevision( javaMethod ) )
        {
            separatorAdded = appendDefaultSinceTag( sb, indent, separatorAdded );
        }

        sb.append( indent ).append( " " ).append( END_JAVADOC );
        sb.append( EOL );

        stringWriter.write( sb.toString() );
    }

    /**
     * @param stringWriter not null
     * @param originalContent not null
     * @param entity not null
     * @param indent not null
     * @throws MojoExecutionException if any
     * @throws IOException if any
     */
    private void updateEntityComment( final StringWriter stringWriter, final String originalContent,
                                      final AbstractInheritableJavaEntity entity, final String indent )
        throws MojoExecutionException, IOException
    {
        String s = stringWriter.toString();
        int i = s.lastIndexOf( START_JAVADOC );
        if ( i != -1 )
        {
            String tmp = s.substring( 0, i );
            if ( tmp.lastIndexOf( EOL ) != -1 )
            {
                tmp = tmp.substring( 0, tmp.lastIndexOf( EOL ) );
            }
            stringWriter.getBuffer().delete( 0, stringWriter.getBuffer().length() );
            stringWriter.write( tmp );
            stringWriter.write( EOL );
        }

        updateJavadocComment( stringWriter, originalContent, entity, indent );
    }

    /**
     * @param stringWriter not null
     * @param originalContent not null
     * @param entity not null
     * @param indent not null
     * @throws MojoExecutionException if any
     * @throws IOException if any
     */
    private void updateJavadocComment( final StringWriter stringWriter, final String originalContent,
                                       final AbstractInheritableJavaEntity entity, final String indent )
        throws MojoExecutionException, IOException
    {
        if ( entity.getComment() == null && ( entity.getTags() == null || entity.getTags().length == 0 ) )
        {
            return;
        }

        boolean isJavaMethod = false;
        if ( entity instanceof JavaMethod )
        {
            isJavaMethod = true;
        }

        StringBuffer sb = new StringBuffer();

        // special case for inherited method
        if ( isJavaMethod )
        {
            JavaMethod javaMethod = (JavaMethod) entity;

            if ( isInherited( javaMethod ) )
            {
                // QDOX-154 could be empty
                if ( StringUtils.isEmpty( javaMethod.getComment() ) )
                {
                    sb.append( indent ).append( INHERITED_JAVADOC );
                    sb.append( EOL );
                    stringWriter.write( sb.toString() );
                    return;
                }

                String javadoc = getJavadocComment( originalContent, javaMethod );

                // case: /** {@inheritDoc} */ or no tags
                if ( hasInheritedTag( javadoc )
                    && ( javaMethod.getTags() == null || javaMethod.getTags().length == 0 ) )
                {
                    sb.append( indent ).append( INHERITED_JAVADOC );
                    sb.append( EOL );
                    stringWriter.write( sb.toString() );
                    return;
                }

                if ( javadoc.indexOf( START_JAVADOC ) != -1 )
                {
                    javadoc = javadoc.substring( javadoc.indexOf( START_JAVADOC ) + START_JAVADOC.length() );
                }
                if ( javadoc.indexOf( END_JAVADOC ) != -1 )
                {
                    javadoc = javadoc.substring( 0, javadoc.indexOf( END_JAVADOC ) );
                }

                sb.append( indent ).append( START_JAVADOC );
                sb.append( EOL );
                if ( javadoc.indexOf( INHERITED_TAG ) == -1 )
                {
                    sb.append( indent ).append( SEPARATOR_JAVADOC ).append( INHERITED_TAG );
                    sb.append( EOL );
                    appendSeparator( sb, indent );
                }
                javadoc = removeLastEmptyJavadocLines( javadoc );
                javadoc = alignIndentationJavadocLines( javadoc, indent );
                sb.append( javadoc );
                sb.append( EOL );
                if ( javaMethod.getTags() != null )
                {
                    for ( int i = 0; i < javaMethod.getTags().length; i++ )
                    {
                        DocletTag docletTag = javaMethod.getTags()[i];

                        // Voluntary ignore these tags
                        if ( docletTag.getName().equals( PARAM_TAG )
                            || docletTag.getName().equals( RETURN_TAG )
                            || docletTag.getName().equals( THROWS_TAG ) )
                        {
                            continue;
                        }

                        String s = getJavadocComment( originalContent, entity, docletTag );
                        s = removeLastEmptyJavadocLines( s );
                        s = alignIndentationJavadocLines( s, indent );
                        sb.append( s );
                        sb.append( EOL );
                    }
                }
                sb.append( indent ).append( " " ).append( END_JAVADOC );
                sb.append( EOL );

                if ( hasInheritedTag( sb.toString().trim() ) )
                {
                    sb = new StringBuffer();
                    sb.append( indent ).append( INHERITED_JAVADOC );
                    sb.append( EOL );
                    stringWriter.write( sb.toString() );
                    return;
                }

                stringWriter.write( sb.toString() );
                return;
            }
        }

        sb.append( indent ).append( START_JAVADOC );
        sb.append( EOL );

        // comment
        if ( StringUtils.isNotEmpty( entity.getComment() ) )
        {
            updateJavadocComment( sb, originalContent, entity, indent );
        }
        else
        {
            addDefaultJavadocComment( sb, entity, indent, isJavaMethod );
        }

        // tags
        if ( entity.getTags() != null && entity.getTags().length > 0 )
        {
            updateJavadocTags( sb, originalContent, entity, indent, isJavaMethod );
        }
        else
        {
            addDefaultJavadocTags( sb, entity, indent, isJavaMethod );
        }

        sb = new StringBuffer( removeLastEmptyJavadocLines( sb.toString() ) ).append( EOL );

        sb.append( indent ).append( " " ).append( END_JAVADOC );
        sb.append( EOL );

        stringWriter.write( sb.toString() );
    }

    /**
     * @param sb not null
     * @param originalContent not null
     * @param entity not null
     * @param indent not null
     * @throws IOException if any
     */
    private void updateJavadocComment( final StringBuffer sb, final String originalContent,
                                       final AbstractInheritableJavaEntity entity, final String indent )
        throws IOException
    {
        String comment = getJavadocComment( originalContent, entity );
        comment = removeLastEmptyJavadocLines( comment );
        comment = alignIndentationJavadocLines( comment, indent );

        if ( comment.indexOf( START_JAVADOC ) != -1 )
        {
            comment = comment.substring( comment.indexOf( START_JAVADOC ) + START_JAVADOC.length() );
            comment = indent + SEPARATOR_JAVADOC + comment.trim();
        }
        if ( comment.indexOf( END_JAVADOC ) != -1 )
        {
            comment = comment.substring( 0, comment.indexOf( END_JAVADOC ) );
        }

        String[] lines = getLines( comment );
        for ( int i = 0; i < lines.length; i++ )
        {
            sb.append( indent ).append( " " ).append( lines[i].trim() );
            sb.append( EOL );
        }
    }

    /**
     * @param sb not null
     * @param entity not null
     * @param indent not null
     * @param isJavaMethod
     */
    private void addDefaultJavadocComment( final StringBuffer sb, final AbstractInheritableJavaEntity entity,
                                           final String indent, final boolean isJavaMethod )
    {
        sb.append( indent ).append( SEPARATOR_JAVADOC );
        if ( isJavaMethod )
        {
            sb.append( getDefaultMethodJavadocComment( (JavaMethod) entity ) );
        }
        else
        {
            sb.append( getDefaultClassJavadocComment( (JavaClass) entity ) );
        }
        sb.append( EOL );
    }

    /**
     * @param sb not null
     * @param originalContent not null
     * @param entity not null
     * @param indent not null
     * @param isJavaMethod
     * @throws IOException if any
     * @throws MojoExecutionException if any
     */
    private void updateJavadocTags( final StringBuffer sb, final String originalContent,
                                    final AbstractInheritableJavaEntity entity, final String indent,
                                    final boolean isJavaMethod )
        throws IOException, MojoExecutionException
    {
        appendSeparator( sb, indent );

        // parse tags
        JavaEntityTags javaEntityTags = parseJavadocTags( originalContent, entity, indent, isJavaMethod );

        // update and write tags
        updateJavadocTags( sb, entity, isJavaMethod, javaEntityTags );

        // add missing tags...
        addMissingJavadocTags( sb, entity, indent, isJavaMethod, javaEntityTags );
    }

    /**
     * Parse entity tags
     *
     * @param originalContent not null
     * @param entity not null
     * @param indent not null
     * @param isJavaMethod
     * @return an instance of {@link JavaEntityTags}
     * @throws IOException if any
     */
    private JavaEntityTags parseJavadocTags( final String originalContent,
                                             final AbstractInheritableJavaEntity entity, final String indent,
                                             final boolean isJavaMethod )
        throws IOException
    {
        JavaEntityTags javaEntityTags = new JavaEntityTags( entity, isJavaMethod );
        for ( int i = 0; i < entity.getTags().length; i++ )
        {
            DocletTag docletTag = entity.getTags()[i];

            String originalJavadocTag = getJavadocComment( originalContent, entity, docletTag );
            originalJavadocTag = removeLastEmptyJavadocLines( originalJavadocTag );
            originalJavadocTag = alignIndentationJavadocLines( originalJavadocTag, indent );

            javaEntityTags.getNamesTags().add( docletTag.getName() );

            if ( isJavaMethod )
            {
                String[] params = docletTag.getParameters();
                if ( params.length < 1 )
                {
                    continue;
                }

                params = fixQdox173( params );
                String paramName = params[0];
                if ( docletTag.getName().equals( PARAM_TAG ) )
                {
                    javaEntityTags.putJavadocParamTag( paramName, originalJavadocTag );
                }
                else if ( docletTag.getName().equals( RETURN_TAG ) )
                {
                    javaEntityTags.setJavadocReturnTag( originalJavadocTag );
                }
                else if ( docletTag.getName().equals( THROWS_TAG ) )
                {
                    javaEntityTags.putJavadocThrowsTag( paramName, originalJavadocTag );
                }
                else
                {
                    javaEntityTags.getUnknownTags().add( originalJavadocTag );
                }
            }
            else
            {
                javaEntityTags.getUnknownTags().add( originalJavadocTag );
            }
        }

        return javaEntityTags;
    }

    /**
     * Write tags according javaEntityTags.
     *
     * @param sb not null
     * @param entity not null
     * @param isJavaMethod
     * @param javaEntityTags not null
     */
    private void updateJavadocTags( final StringBuffer sb, final AbstractInheritableJavaEntity entity,
                                    final boolean isJavaMethod, final JavaEntityTags javaEntityTags )
    {
        for ( int i = 0; i < entity.getTags().length; i++ )
        {
            DocletTag docletTag = entity.getTags()[i];

            if ( isJavaMethod )
            {
                JavaMethod javaMethod = (JavaMethod) entity;

                String[] params = docletTag.getParameters();
                if ( params.length < 1 )
                {
                    continue;
                }

                if ( docletTag.getName().equals( PARAM_TAG ) )
                {
                    writeParamTag( sb, javaMethod, javaEntityTags, params );
                }
                else if ( docletTag.getName().equals( RETURN_TAG ) )
                {
                    writeReturnTag( sb, javaMethod, javaEntityTags );
                }
                else if ( docletTag.getName().equals( THROWS_TAG ) )
                {
                    writeThrowsTag( sb, javaMethod, javaEntityTags, params );
                }
                else
                {
                    // write unknown tags
                    for ( Iterator<String> it = javaEntityTags.getUnknownTags().iterator(); it.hasNext(); )
                    {
                        String originalJavadocTag = it.next();

                        if ( StringUtils.removeDuplicateWhitespace( originalJavadocTag ).trim()
                                        .indexOf( "@" + docletTag.getName() ) != -1 )
                        {
                            it.remove();
                            sb.append( originalJavadocTag );
                            sb.append( EOL );
                        }
                    }
                }
            }
            else
            {
                for ( Iterator<String> it = javaEntityTags.getUnknownTags().iterator(); it.hasNext(); )
                {
                    String originalJavadocTag = it.next();

                    if ( StringUtils.removeDuplicateWhitespace( originalJavadocTag ).trim()
                                    .indexOf( "@" + docletTag.getName() ) != -1 )
                    {
                        it.remove();
                        sb.append( originalJavadocTag );
                        sb.append( EOL );
                    }
                }
            }

            if ( sb.toString().endsWith( EOL ) )
            {
                sb.delete( sb.toString().lastIndexOf( EOL ), sb.toString().length() );
            }

            sb.append( EOL );
        }
    }

    private void writeParamTag( final StringBuffer sb, final JavaMethod javaMethod,
                                final JavaEntityTags javaEntityTags, String[] params )
    {
        params = fixQdox173( params );

        String paramName = params[0];

        if ( !fixTag( PARAM_TAG ) )
        {
            // write original param tag if found
            String originalJavadocTag = javaEntityTags.getJavadocParamTag( paramName );
            if ( originalJavadocTag != null )
            {
                sb.append( originalJavadocTag );
            }
            return;
        }

        boolean found = false;
        JavaParameter javaParam = javaMethod.getParameterByName( paramName );
        if ( javaParam == null )
        {
            // is generic?
            TypeVariable[] typeParams = javaMethod.getTypeParameters();
            for ( int i = 0; i < typeParams.length; i++ )
            {
                if ( typeParams[i].getGenericValue().equals( paramName ) )
                {
                    found = true;
                }
            }
        }
        else
        {
            found = true;
        }

        if ( !found )
        {
            if ( getLog().isWarnEnabled() )
            {
                StringBuffer warn = new StringBuffer();

                warn.append( "Fixed unknown param '" ).append( paramName ).append( "' defined in " );
                warn.append( getJavaMethodAsString( javaMethod ) );

                getLog().warn( warn.toString() );
            }

            if ( sb.toString().endsWith( EOL ) )
            {
                sb.delete( sb.toString().lastIndexOf( EOL ), sb.toString().length() );
            }
        }
        else
        {
            String originalJavadocTag = javaEntityTags.getJavadocParamTag( paramName );
            if ( originalJavadocTag != null )
            {
                sb.append( originalJavadocTag );
                String s = "@" + PARAM_TAG + " " + paramName;
                if ( StringUtils.removeDuplicateWhitespace( originalJavadocTag ).trim().endsWith( s ) )
                {
                    sb.append( " " );
                    sb.append( getDefaultJavadocForType( javaParam.getType() ) );
                }
            }
        }
    }

    private void writeReturnTag( final StringBuffer sb, final JavaMethod javaMethod,
                                 final JavaEntityTags javaEntityTags )
    {
        String originalJavadocTag = javaEntityTags.getJavadocReturnTag();
        if ( originalJavadocTag == null )
        {
            return;
        }

        if ( !fixTag( RETURN_TAG ) )
        {
            // write original param tag if found
            sb.append( originalJavadocTag );
            return;
        }

        if ( StringUtils.isNotEmpty( originalJavadocTag ) && javaMethod.getReturns() != null
            && !javaMethod.getReturns().isVoid() )
        {
            sb.append( originalJavadocTag );
            if ( originalJavadocTag.trim().endsWith( "@" + RETURN_TAG ) )
            {
                sb.append( " " );
                sb.append( getDefaultJavadocForType( javaMethod.getReturns() ) );
            }
        }
    }

    private void writeThrowsTag( final StringBuffer sb, final JavaMethod javaMethod,
                                 final JavaEntityTags javaEntityTags, final String[] params )
    {
        String exceptionClassName = params[0];

        String originalJavadocTag = javaEntityTags.getJavadocThrowsTag( exceptionClassName );
        if ( originalJavadocTag == null )
        {
            return;
        }

        if ( !fixTag( THROWS_TAG ) )
        {
            // write original param tag if found
            sb.append( originalJavadocTag );
            return;
        }

        if ( javaMethod.getExceptions() != null )
        {
            for ( int j = 0; j < javaMethod.getExceptions().length; j++ )
            {
                Type exception = javaMethod.getExceptions()[j];

                if ( exception.getValue().endsWith( exceptionClassName ) )
                {
                    originalJavadocTag =
                        StringUtils.replace( originalJavadocTag, exceptionClassName, exception.getValue() );
                    if ( StringUtils.removeDuplicateWhitespace( originalJavadocTag ).trim()
                                    .endsWith( "@" + THROWS_TAG + " " + exception.getValue() ) )
                    {
                        originalJavadocTag += " if any.";
                    }

                    sb.append( originalJavadocTag );

                    // added qualified name
                    javaEntityTags.putJavadocThrowsTag( exception.getValue(), originalJavadocTag );

                    return;
                }
            }
        }

        // Maybe a RuntimeException
        Class<?> clazz = getRuntimeExceptionClass( javaMethod.getParentClass(), exceptionClassName );
        if ( clazz != null )
        {
            sb.append( StringUtils.replace( originalJavadocTag, exceptionClassName, clazz.getName() ) );

            // added qualified name
            javaEntityTags.putJavadocThrowsTag( clazz.getName(), originalJavadocTag );

            return;
        }

        if ( getLog().isWarnEnabled() )
        {
            StringBuffer warn = new StringBuffer();

            warn.append( "Unknown throws exception '" ).append( exceptionClassName ).append( "' defined in " );
            warn.append( getJavaMethodAsString( javaMethod ) );

            getLog().warn( warn.toString() );
        }

        sb.append( originalJavadocTag );
        if ( params.length == 1 )
        {
            sb.append( " if any." );
        }
    }

    /**
     * Add missing tags not already written.
     *
     * @param sb not null
     * @param entity not null
     * @param indent not null
     * @param isJavaMethod
     * @param javaEntityTags not null
     * @throws MojoExecutionException if any
     */
    private void addMissingJavadocTags( final StringBuffer sb, final AbstractInheritableJavaEntity entity,
                                        final String indent, final boolean isJavaMethod,
                                        final JavaEntityTags javaEntityTags )
        throws MojoExecutionException
    {
        if ( isJavaMethod )
        {
            JavaMethod javaMethod = (JavaMethod) entity;

            if ( fixTag( PARAM_TAG ) )
            {
                if ( javaMethod.getParameters() != null )
                {
                    for ( int i = 0; i < javaMethod.getParameters().length; i++ )
                    {
                        JavaParameter javaParameter = javaMethod.getParameters()[i];

                        if ( javaEntityTags.getJavadocParamTag( javaParameter.getName(), true ) == null )
                        {
                            appendDefaultParamTag( sb, indent, javaParameter );
                        }
                    }
                }
                // is generic?
                if ( javaMethod.getTypeParameters() != null )
                {
                    for ( int i = 0; i < javaMethod.getTypeParameters().length; i++ )
                    {
                        TypeVariable typeParam = javaMethod.getTypeParameters()[i];

                        if ( javaEntityTags.getJavadocParamTag( "<" + typeParam.getName() + ">", true ) == null )
                        {
                            appendDefaultParamTag( sb, indent, typeParam );
                        }
                    }
                }
            }

            if ( fixTag( RETURN_TAG ) && StringUtils.isEmpty( javaEntityTags.getJavadocReturnTag() )
                && javaMethod.getReturns() != null && !javaMethod.getReturns().isVoid() )
            {
                appendDefaultReturnTag( sb, indent, javaMethod );
            }

            if ( fixTag( THROWS_TAG ) && javaMethod.getExceptions() != null )
            {
                for ( int i = 0; i < javaMethod.getExceptions().length; i++ )
                {
                    Type exception = javaMethod.getExceptions()[i];

                    if ( javaEntityTags.getJavadocThrowsTag( exception.getValue(), true ) == null )
                    {
                        appendDefaultThrowsTag( sb, indent, exception );
                    }
                }
            }
        }
        else
        {
            if ( !javaEntityTags.getNamesTags().contains( AUTHOR_TAG ) )
            {
                appendDefaultAuthorTag( sb, indent );
            }
            if ( !javaEntityTags.getNamesTags().contains( VERSION_TAG ) )
            {
                appendDefaultVersionTag( sb, indent );
            }
        }
        if ( fixTag( SINCE_TAG ) && !javaEntityTags.getNamesTags().contains( SINCE_TAG ) )
        {
            if ( !isJavaMethod )
            {
                if ( !ignoreClirr )
                {
                    if ( isNewClassFromLastVersion( (JavaClass) entity ) )
                    {
                        appendDefaultSinceTag( sb, indent );
                    }
                }
                else
                {
                    appendDefaultSinceTag( sb, indent );
                    addSinceClasses( (JavaClass) entity );
                }
            }
            else
            {
                if ( !ignoreClirr )
                {
                    if ( isNewMethodFromLastRevision( (JavaMethod) entity ) )
                    {
                        appendDefaultSinceTag( sb, indent );
                    }
                }
                else
                {
                    if ( sinceClasses != null && !sinceClassesContains( ( (JavaMethod) entity ).getParentClass() ) )
                    {
                        appendDefaultSinceTag( sb, indent );
                    }
                }
            }
        }
    }

    /**
     * @param sb not null
     * @param entity not null
     * @param indent not null
     * @param isJavaMethod
     * @throws MojoExecutionException if any
     */
    private void addDefaultJavadocTags( final StringBuffer sb, final AbstractInheritableJavaEntity entity,
                                        final String indent, final boolean isJavaMethod )
        throws MojoExecutionException
    {
        boolean separatorAdded = false;
        if ( isJavaMethod )
        {
            JavaMethod javaMethod = (JavaMethod) entity;

            if ( fixTag( PARAM_TAG ) && javaMethod.getParameters() != null )
            {
                for ( int i = 0; i < javaMethod.getParameters().length; i++ )
                {
                    JavaParameter javaParameter = javaMethod.getParameters()[i];

                    separatorAdded = appendDefaultParamTag( sb, indent, separatorAdded, javaParameter );
                }
            }

            if ( fixTag( RETURN_TAG ) )
            {
                if ( javaMethod.getReturns() != null && !javaMethod.getReturns().isVoid() )
                {
                    separatorAdded = appendDefaultReturnTag( sb, indent, separatorAdded, javaMethod );
                }
            }

            if ( fixTag( THROWS_TAG ) && javaMethod.getExceptions() != null )
            {
                for ( int i = 0; i < javaMethod.getExceptions().length; i++ )
                {
                    Type exception = javaMethod.getExceptions()[i];

                    separatorAdded = appendDefaultThrowsTag( sb, indent, separatorAdded, exception );
                }
            }
        }
        else
        {
            separatorAdded = appendDefaultAuthorTag( sb, indent, separatorAdded );

            separatorAdded = appendDefaultVersionTag( sb, indent, separatorAdded );
        }

        if ( fixTag( SINCE_TAG ) )
        {
            if ( !isJavaMethod )
            {
                JavaClass javaClass = (JavaClass) entity;

                if ( !ignoreClirr )
                {
                    if ( isNewClassFromLastVersion( javaClass ) )
                    {
                        separatorAdded = appendDefaultSinceTag( sb, indent, separatorAdded );
                    }
                }
                else
                {
                    separatorAdded = appendDefaultSinceTag( sb, indent, separatorAdded );

                    addSinceClasses( javaClass );
                }
            }
            else
            {
                JavaMethod javaMethod = (JavaMethod) entity;

                if ( !ignoreClirr )
                {
                    if ( isNewMethodFromLastRevision( javaMethod ) )
                    {
                        separatorAdded = appendDefaultSinceTag( sb, indent, separatorAdded );
                    }
                }
                else
                {
                    if ( sinceClasses != null && !sinceClassesContains( javaMethod.getParentClass() ) )
                    {
                        separatorAdded = appendDefaultSinceTag( sb, indent, separatorAdded );
                    }
                }
            }
        }
    }

    /**
     * @param sb not null
     * @param indent not null
     * @param separatorAdded
     * @return true if separator has been added.
     */
    private boolean appendDefaultAuthorTag( final StringBuffer sb, final String indent, boolean separatorAdded )
    {
        if ( !fixTag( AUTHOR_TAG ) )
        {
            return separatorAdded;
        }

        if ( !separatorAdded )
        {
            appendSeparator( sb, indent );
            separatorAdded = true;
        }

        appendDefaultAuthorTag( sb, indent );
        return separatorAdded;
    }

    /**
     * @param sb not null
     * @param indent not null
     */
    private void appendDefaultAuthorTag( final StringBuffer sb, final String indent )
    {
        if ( !fixTag( AUTHOR_TAG ) )
        {
            return;
        }

        sb.append( indent ).append( " * @" ).append( AUTHOR_TAG ).append( " " );
        sb.append( defaultAuthor );
        sb.append( EOL );
    }

    /**
     * @param sb not null
     * @param indent not null
     * @param separatorAdded
     * @return true if separator has been added.
     */
    private boolean appendDefaultSinceTag( final StringBuffer sb, final String indent, boolean separatorAdded )
    {
        if ( !fixTag( SINCE_TAG ) )
        {
            return separatorAdded;
        }

        if ( !separatorAdded )
        {
            appendSeparator( sb, indent );
            separatorAdded = true;
        }

        appendDefaultSinceTag( sb, indent );
        return separatorAdded;
    }

    /**
     * @param sb not null
     * @param indent not null
     */
    private void appendDefaultSinceTag( final StringBuffer sb, final String indent )
    {
        if ( !fixTag( SINCE_TAG ) )
        {
            return;
        }

        sb.append( indent ).append( " * @" ).append( SINCE_TAG ).append( " " );
        sb.append( defaultSince );
        sb.append( EOL );
    }

    /**
     * @param sb not null
     * @param indent not null
     * @param separatorAdded
     * @return true if separator has been added.
     */
    private boolean appendDefaultVersionTag( final StringBuffer sb, final String indent, boolean separatorAdded )
    {
        if ( !fixTag( VERSION_TAG ) )
        {
            return separatorAdded;
        }

        if ( !separatorAdded )
        {
            appendSeparator( sb, indent );
            separatorAdded = true;
        }

        appendDefaultVersionTag( sb, indent );
        return separatorAdded;
    }

    /**
     * @param sb not null
     * @param indent not null
     */
    private void appendDefaultVersionTag( final StringBuffer sb, final String indent )
    {
        if ( !fixTag( VERSION_TAG ) )
        {
            return;
        }

        sb.append( indent ).append( " * @" ).append( VERSION_TAG ).append( " " );
        sb.append( defaultVersion );
        sb.append( EOL );
    }

    /**
     * @param sb not null
     * @param indent not null
     * @param separatorAdded
     * @param javaParameter not null
     * @return true if separator has been added.
     */
    private boolean appendDefaultParamTag( final StringBuffer sb, final String indent, boolean separatorAdded,
                                           final JavaParameter javaParameter )
    {
        if ( !fixTag( PARAM_TAG ) )
        {
            return separatorAdded;
        }

        if ( !separatorAdded )
        {
            appendSeparator( sb, indent );
            separatorAdded = true;
        }

        appendDefaultParamTag( sb, indent, javaParameter );
        return separatorAdded;
    }

    /**
     * @param sb not null
     * @param indent not null
     * @param separatorAdded
     * @param typeParameter not null
     * @return true if separator has been added.
     */
    private boolean appendDefaultParamTag( final StringBuffer sb, final String indent, boolean separatorAdded,
                                           final TypeVariable typeParameter )
    {
        if ( !fixTag( PARAM_TAG ) )
        {
            return separatorAdded;
        }

        if ( !separatorAdded )
        {
            appendSeparator( sb, indent );
            separatorAdded = true;
        }

        appendDefaultParamTag( sb, indent, typeParameter );
        return separatorAdded;
    }

    /**
     * @param sb not null
     * @param indent not null
     * @param javaParameter not null
     */
    private void appendDefaultParamTag( final StringBuffer sb, final String indent,
                                        final JavaParameter javaParameter )
    {
        if ( !fixTag( PARAM_TAG ) )
        {
            return;
        }

        sb.append( indent ).append( " * @" ).append( PARAM_TAG ).append( " " );
        sb.append( javaParameter.getName() );
        sb.append( " " );
        sb.append( getDefaultJavadocForType( javaParameter.getType() ) );
        sb.append( EOL );
    }

    /**
     * @param sb not null
     * @param indent not null
     * @param typeParameter not null
     */
    private void appendDefaultParamTag( final StringBuffer sb, final String indent,
                                        final TypeVariable typeParameter )
    {
        if ( !fixTag( PARAM_TAG ) )
        {
            return;
        }

        sb.append( indent ).append( " * @" ).append( PARAM_TAG ).append( " " );
        sb.append( "<" + typeParameter.getName() + ">" );
        sb.append( " " );
        sb.append( getDefaultJavadocForType( typeParameter ) );
        sb.append( EOL );
    }

    /**
     * @param sb not null
     * @param indent not null
     * @param separatorAdded
     * @param javaMethod not null
     * @return true if separator has been added.
     */
    private boolean appendDefaultReturnTag( final StringBuffer sb, final String indent, boolean separatorAdded,
                                            final JavaMethod javaMethod )
    {
        if ( !fixTag( RETURN_TAG ) )
        {
            return separatorAdded;
        }

        if ( !separatorAdded )
        {
            appendSeparator( sb, indent );
            separatorAdded = true;
        }

        appendDefaultReturnTag( sb, indent, javaMethod );
        return separatorAdded;
    }

    /**
     * @param sb not null
     * @param indent not null
     * @param javaMethod not null
     */
    private void appendDefaultReturnTag( final StringBuffer sb, final String indent, final JavaMethod javaMethod )
    {
        if ( !fixTag( RETURN_TAG ) )
        {
            return;
        }

        sb.append( indent ).append( " * @" ).append( RETURN_TAG ).append( " " );
        sb.append( getDefaultJavadocForType( javaMethod.getReturns() ) );
        sb.append( EOL );
    }

    /**
     * @param sb not null
     * @param indent not null
     * @param separatorAdded
     * @param exception not null
     * @return true if separator has been added.
     */
    private boolean appendDefaultThrowsTag( final StringBuffer sb, final String indent, boolean separatorAdded,
                                            final Type exception )
    {
        if ( !fixTag( THROWS_TAG ) )
        {
            return separatorAdded;
        }

        if ( !separatorAdded )
        {
            appendSeparator( sb, indent );
            separatorAdded = true;
        }

        appendDefaultThrowsTag( sb, indent, exception );
        return separatorAdded;
    }

    /**
     * @param sb not null
     * @param indent not null
     * @param exception not null
     */
    private void appendDefaultThrowsTag( final StringBuffer sb, final String indent, final Type exception )
    {
        if ( !fixTag( THROWS_TAG ) )
        {
            return;
        }

        sb.append( indent ).append( " * @" ).append( THROWS_TAG ).append( " " );
        sb.append( exception.getJavaClass().getFullyQualifiedName() );
        sb.append( " if any." );
        sb.append( EOL );
    }

    /**
     * @param sb not null
     * @param indent not null
     */
    private void appendSeparator( final StringBuffer sb, final String indent )
    {
        sb.append( indent ).append( " *" );
        sb.append( EOL );
    }

    /**
     * Verify if a method has <code>&#64;java.lang.Override()</code> annotation or if it is an inherited method
     * from an interface or a super class. The goal is to handle <code>&#123;&#64;inheritDoc&#125;</code> tag.
     *
     * @param javaMethod not null
     * @return <code>true</code> if the method is inherited, <code>false</code> otherwise.
     * @throws MojoExecutionException if any
     */
    private boolean isInherited( JavaMethod javaMethod )
        throws MojoExecutionException, SecurityException
    {
        if ( javaMethod.getAnnotations() != null )
        {
            for ( int i = 0; i < javaMethod.getAnnotations().length; i++ )
            {
                Annotation annotation = javaMethod.getAnnotations()[i];

                if ( annotation.toString().equals( "@java.lang.Override()" ) )
                {
                    return true;
                }
            }
        }

        Class<?> clazz = getClass( javaMethod.getParentClass().getFullyQualifiedName() );

        List<Class<?>>interfaces = ClassUtils.getAllInterfaces( clazz );
        for ( Class<?> intface : interfaces )
        {
            if ( isInherited( intface, javaMethod ) )
            {
                return true;
            }
        }

        List<Class<?>> classes = ClassUtils.getAllSuperclasses( clazz );
        for ( Class<?> superClass : classes )
        {
            if ( isInherited( superClass, javaMethod ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * @param clazz the Java class object, not null
     * @param javaMethod the QDox JavaMethod object not null
     * @return <code>true</code> if <code>javaMethod</code> exists in the given <code>clazz</code>,
     * <code>false</code> otherwise.
     * @see #isInherited(JavaMethod)
     */
    private boolean isInherited( Class<?> clazz, JavaMethod javaMethod )
    {
        Method[] methods = clazz.getDeclaredMethods();
        for ( int i = 0; i < methods.length; i++ )
        {
            if ( !methods[i].getName().equals( javaMethod.getName() ) )
            {
                continue;
            }

            if ( methods[i].getParameterTypes().length != javaMethod.getParameters().length )
            {
                continue;
            }

            boolean found = false;
            for ( int j = 0; j < methods[i].getParameterTypes().length; j++ )
            {
                String name1 = methods[i].getParameterTypes()[j].getName();
                String name2 = javaMethod.getParameters()[j].getType().getFullQualifiedName();
                if ( name1.equals( name2 ) )
                {
                    found = true;
                }
                else
                {
                    found = found && false;
                }
            }

            return found;
        }

        return false;
    }

    /**
     * @param type
     * @return
     */
    private String getDefaultJavadocForType( Type type )
    {
        StringBuffer sb = new StringBuffer();

        if ( !TypeVariable.class.isAssignableFrom( type.getClass() ) && type.isPrimitive() )
        {
            if ( type.isArray() )
            {
                sb.append( "an array of " );
            }
            else
            {
                sb.append( "a " );
            }
            sb.append( type.getJavaClass().getFullyQualifiedName() );
            sb.append( "." );
            return sb.toString();
        }

        StringBuffer javadocLink = new StringBuffer();
        try
        {
            getClass( type.getJavaClass().getFullyQualifiedName() );

            javadocLink.append( "{@link " );
            String s = type.getJavaClass().getFullyQualifiedName();
            s = StringUtils.replace( s, "$", "." );
            javadocLink.append( s );
            javadocLink.append( "}" );
        }
        catch ( Exception e )
        {
            javadocLink.append( type.getJavaClass().getFullyQualifiedName() );
        }

        if ( type.isArray() )
        {
            sb.append( "an array of " );
            sb.append( javadocLink.toString() );
            sb.append( " objects." );
        }
        else
        {
            sb.append( "a " ).append( javadocLink.toString() ).append( " object." );
        }

        return sb.toString();
    }

    /**
     * Check under Clirr if this given class is newer from the last version.
     *
     * @param javaClass a given class not null
     * @return <code>true</code> if Clirr said that this class is added from the last version,
     * <code>false</code> otherwise or if {@link #clirrNewClasses} is null.
     */
    private boolean isNewClassFromLastVersion( JavaClass javaClass )
    {
        if ( clirrNewClasses == null )
        {
            return false;
        }

        return clirrNewClasses.contains( javaClass.getFullyQualifiedName() );
    }

    /**
     * Check under Clirr if this given method is newer from the last version.
     *
     * @param javaMethod a given method not null
     * @return <code>true</code> if Clirr said that this method is added from the last version,
     * <code>false</code> otherwise or if {@link #clirrNewMethods} is null.
     * @throws MojoExecutionException  if any
     */
    private boolean isNewMethodFromLastRevision( JavaMethod javaMethod )
        throws MojoExecutionException
    {
        if ( clirrNewMethods == null )
        {
            return false;
        }

        List<String> clirrMethods = clirrNewMethods.get( javaMethod.getParentClass().getFullyQualifiedName() );
        if ( clirrMethods == null )
        {
            return false;
        }

        for ( String clirrMethod : clirrMethods )
        {
            // see net.sf.clirr.core.internal.checks.MethodSetCheck#getMethodId(JavaType clazz, Method method)
            String retrn = "";
            if ( javaMethod.getReturns() != null )
            {
                retrn = javaMethod.getReturns().getFullQualifiedName();
            }
            StringBuffer params = new StringBuffer();
            JavaParameter[] parameters = javaMethod.getParameters();
            for ( int i = 0; i < parameters.length; i++ )
            {
                params.append( parameters[i].getResolvedValue() );
                if ( i < parameters.length - 1 )
                {
                    params.append( ", " );
                }
            }
            if ( ( clirrMethod.indexOf( retrn + " " ) != -1 )
                && ( clirrMethod.indexOf( javaMethod.getName() + "(" ) != -1 )
                && ( clirrMethod.indexOf( "(" + params.toString() + ")" ) != -1 ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * @param className not null
     * @return the Class corresponding to the given class name using the project classloader.
     * @throws MojoExecutionException if class not found
     * @see {@link ClassUtils#getClass(ClassLoader, String, boolean)}
     * @see {@link #getProjectClassLoader()}
     */
    private Class<?> getClass( String className )
        throws MojoExecutionException
    {
        try
        {
            return ClassUtils.getClass( getProjectClassLoader(), className, false );
        }
        catch ( ClassNotFoundException e )
        {
            throw new MojoExecutionException( "ClassNotFoundException: " + e.getMessage(), e );
        }
    }

    /**
     * Returns the Class object assignable for {@link RuntimeException} class and associated with the given
     * exception class name.
     *
     * @param currentClass not null
     * @param exceptionClassName not null, an exception class name defined as:
     * <ul>
     * <li>exception class fully qualified</li>
     * <li>exception class in the same package</li>
     * <li>exception inner class</li>
     * <li>exception class in java.lang package</li>
     * </ul>
     * @return a RuntimeException assignable class.
     * @see #getClass(String)
     */
    private Class<?> getRuntimeExceptionClass( JavaClass currentClass, String exceptionClassName )
    {
        String[] potentialClassNames =
            new String[] { exceptionClassName, currentClass.getPackage().getName() + "." + exceptionClassName,
                currentClass.getPackage().getName() + "." + currentClass.getName() + "$" + exceptionClassName,
                "java.lang." + exceptionClassName };

        Class<?> clazz = null;
        for ( int i = 0; i < potentialClassNames.length; i++ )
        {
            try
            {
                clazz = getClass( potentialClassNames[i] );
            }
            catch ( MojoExecutionException e )
            {
                // nop
            }
            if ( clazz != null && ClassUtils.isAssignable( clazz, RuntimeException.class ) )
            {
                return clazz;
            }
        }

        return null;
    }

    /**
     * @param javaClass not null
     */
    private void addSinceClasses( JavaClass javaClass )
    {
        if ( sinceClasses == null )
        {
            sinceClasses = new ArrayList<String>();
        }
        sinceClasses.add( javaClass.getFullyQualifiedName() );
    }

    private boolean sinceClassesContains( JavaClass javaClass )
    {
        return sinceClasses.contains( javaClass.getFullyQualifiedName() );
    }

    // ----------------------------------------------------------------------
    // Static methods
    // ----------------------------------------------------------------------

    /**
     * Write content into the given javaFile and using the given encoding.
     * All line separators will be unified.
     *
     * @param javaFile not null
     * @param encoding not null
     * @param content not null
     * @throws IOException if any
     */
    private static void writeFile( final File javaFile, final String encoding, final String content )
        throws IOException
    {
        Writer writer = null;
        try
        {
            writer = WriterFactory.newWriter( javaFile, encoding );
            writer.write( StringUtils.unifyLineSeparators( content ) );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    /**
     * @return the full clirr goal, i.e. <code>groupId:artifactId:version:goal</code>. The clirr-plugin version
     * could be load from the pom.properties in the clirr-maven-plugin dependency.
     */
    private static String getFullClirrGoal()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( CLIRR_MAVEN_PLUGIN_GROUPID ).append( ":" );
        sb.append( CLIRR_MAVEN_PLUGIN_ARTIFACTID ).append( ":" );
        String clirrVersion = CLIRR_MAVEN_PLUGIN_VERSION;
        InputStream resourceAsStream = null;
        try
        {
            String resource =
                "META-INF/maven/" + CLIRR_MAVEN_PLUGIN_GROUPID + "/" + CLIRR_MAVEN_PLUGIN_ARTIFACTID
                    + "/pom.properties";
            resourceAsStream = AbstractFixJavadocMojo.class.getClassLoader().getResourceAsStream( resource );

            if ( resourceAsStream != null )
            {
                Properties properties = new Properties();
                properties.load( resourceAsStream );

                if ( StringUtils.isNotEmpty( properties.getProperty( "version" ) ) )
                {
                    clirrVersion = properties.getProperty( "version" );
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

        sb.append( clirrVersion ).append( ":" );
        sb.append( CLIRR_MAVEN_PLUGIN_GOAL );

        return sb.toString();
    }

    /**
     * Default comment for class.
     *
     * @param javaClass not null
     * @return a default comment for class.
     */
    private static String getDefaultClassJavadocComment( final JavaClass javaClass )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "<p>" );
        if ( Arrays.asList( javaClass.getModifiers() ).contains( "abstract" ) )
        {
            sb.append( "Abstract " );
        }

        sb.append( javaClass.getName() );

        if ( !javaClass.isInterface() )
        {
            sb.append( " class." );
        }
        else
        {
            sb.append( " interface." );
        }

        sb.append( "</p>" );

        return sb.toString();
    }

    /**
     * Default comment for method with taking care of getter/setter in the javaMethod name.
     *
     * @param javaMethod not null
     * @return a default comment for method.
     */
    private static String getDefaultMethodJavadocComment( final JavaMethod javaMethod )
    {
        StringBuffer sb = new StringBuffer();

        if ( javaMethod.isConstructor() )
        {
            sb.append( "<p>Constructor for " );
            sb.append( javaMethod.getName() ).append( ".</p>" );
            return sb.toString();
        }

        if ( javaMethod.getName().length() > 3
            && ( javaMethod.getName().startsWith( "get" ) || javaMethod.getName().startsWith( "set" ) ) )
        {
            String field = StringUtils.lowercaseFirstLetter( javaMethod.getName().substring( 3 ) );

            JavaClass clazz = javaMethod.getParentClass();

            if ( clazz.getFieldByName( field ) == null )
            {
                sb.append( "<p>" ).append( javaMethod.getName() ).append( "</p>" );
                return sb.toString();
            }

            sb.append( "<p>" );
            if ( javaMethod.getName().startsWith( "get" ) )
            {
                sb.append( "Getter " );
            }
            if ( javaMethod.getName().startsWith( "set" ) )
            {
                sb.append( "Setter " );
            }
            sb.append( "for the field <code>" ).append( field ).append( "</code>." );
            sb.append( "</p>" );

            return sb.toString();
        }

        sb.append( "<p>" ).append( javaMethod.getName() ).append( "</p>" );

        return sb.toString();
    }

    /**
     * Try to find if a Javadoc comment has an {@link #INHERITED_TAG} for instance:
     * <pre>
     * &#47;&#42;&#42; {&#64;inheritDoc} &#42;&#47;
     * </pre>
     * or
     * <pre>
     * &#47;&#42;&#42;
     * &#32;&#42; {&#64;inheritDoc}
     * &#32;&#42;&#47;
     * </pre>
     *
     * @param content not null
     * @return <code>true</code> if the content has an inherited tag, <code>false</code> otherwise.
     */
    private static boolean hasInheritedTag( final String content )
    {
        final String inheritedTagPattern =
            "^\\s*(\\/\\*\\*)?(\\s*(\\*)?)*(\\{)@inheritDoc\\s*(\\})(\\s*(\\*)?)*(\\*\\/)?$";
        return Pattern.matches( inheritedTagPattern, StringUtils.removeDuplicateWhitespace( content ) );
    }

    /**
     * Workaround for QDOX-146 about whitespace.
     * Ideally we want to use <code>entity.getComment()</code>
     * <br/>
     * For instance, with the following snippet:
     * <br/>
     *
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff"></font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;</font><br />
     * <font color="#808080">3</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;Dummy&nbsp;Javadoc&nbsp;comment.</font><br />
     * <font color="#808080">4</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@param&nbsp;</font>
     * <font color="#3f5fbf">s&nbsp;a&nbsp;String</font><br />
     * <font color="#808080">5</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&#47;</font><br />
     * <font color="#808080">6</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#7f0055"><b>public&nbsp;</b></font><font color="#7f0055"><b>void&nbsp;</b></font>
     * <font color="#000000">dummyMethod</font><font color="#000000">(&nbsp;</font>
     * <font color="#000000">String&nbsp;s&nbsp;</font><font color="#000000">){}</font><br />
     * </code>
     *
     * <br/>
     * The return will be:
     * <br/>
     *
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;Dummy&nbsp;Javadoc&nbsp;comment.</font><br />
     * </code>
     *
     * @param javaClassContent original class content not null
     * @param entity not null
     * @return the javadoc comment for the entity without any tags.
     * @throws IOException if any
     */
    private static String getJavadocComment( final String javaClassContent, final AbstractJavaEntity entity )
        throws IOException
    {
        if ( entity.getComment() == null )
        {
            return "";
        }

        String originalJavadoc = extractOriginalJavadocContent( javaClassContent, entity );

        StringBuffer sb = new StringBuffer();
        BufferedReader lr = new BufferedReader( new StringReader( originalJavadoc ) );
        String line;
        while ( ( line = lr.readLine() ) != null )
        {
            if ( StringUtils.removeDuplicateWhitespace( line.trim() ).startsWith( "* @" )
                || StringUtils.removeDuplicateWhitespace( line.trim() ).startsWith( "*@" ) )
            {
                break;
            }
            sb.append( line ).append( EOL );
        }

        return trimRight( sb.toString() );
    }

    /**
     * Work around for QDOX-146 about whitespace.
     * Ideally we want to use <code>docletTag.getValue()</code>
     * <br/>
     * For instance, with the following snippet:
     * <br/>
     *
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff"></font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;</font><br />
     * <font color="#808080">3</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;Dummy&nbsp;Javadoc&nbsp;comment.</font><br />
     * <font color="#808080">4</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@param&nbsp;</font>
     * <font color="#3f5fbf">s&nbsp;a&nbsp;String</font><br />
     * <font color="#808080">5</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&#47;</font><br />
     * <font color="#808080">6</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#7f0055"><b>public&nbsp;</b></font><font color="#7f0055"><b>void&nbsp;</b></font>
     * <font color="#000000">dummyMethod</font><font color="#000000">(&nbsp;</font>
     * <font color="#000000">String&nbsp;s&nbsp;</font><font color="#000000">){}</font><br />
     * </code>
     *
     * <br/>
     * The return will be:
     * <br/>
     *
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@param&nbsp;</font>
     * <font color="#3f5fbf">s&nbsp;a&nbsp;String</font><br />
     * </code>
     *
     * @param javaClassContent original class content not null
     * @param entity not null
     * @param docletTag not null
     * @return the javadoc comment for the entity without Javadoc tags.
     * @throws IOException if any
     */
    private static String getJavadocComment( final String javaClassContent,
                                             final AbstractInheritableJavaEntity entity, final DocletTag docletTag )
        throws IOException
    {
        if ( docletTag.getValue() == null )
        {
            return "";
        }
        if ( docletTag.getParameters().length == 0 )
        {
            return "";
        }

        String originalJavadoc = extractOriginalJavadocContent( javaClassContent, entity );

        String[] params = fixQdox173( docletTag.getParameters() );
        String paramValue = params[0];

        StringBuffer sb = new StringBuffer();
        BufferedReader lr = new BufferedReader( new StringReader( originalJavadoc ) );
        String line;
        boolean found = false;
        while ( ( line = lr.readLine() ) != null )
        {
            if ( StringUtils.removeDuplicateWhitespace( line.trim() ).startsWith(
                                                                                  "* @" + docletTag.getName()
                                                                                      + " " + paramValue )
                || StringUtils.removeDuplicateWhitespace( line.trim() ).startsWith(
                                                                                    "*@" + docletTag.getName()
                                                                                        + " " + paramValue ) )
            {
                sb.append( line ).append( EOL );
                found = true;
            }
            else
            {
                if ( StringUtils.removeDuplicateWhitespace( line.trim() ).startsWith( "* @" )
                    || StringUtils.removeDuplicateWhitespace( line.trim() ).startsWith( "*@" ) )
                {
                    found = false;
                }
                if ( found )
                {
                    sb.append( line ).append( EOL );
                }
            }
        }

        return trimRight( sb.toString() );
    }

    /**
     * Extract the original Javadoc and others comments up to {@link #START_JAVADOC} form the entity. This method
     * takes care of the Javadoc indentation. All javadoc lines will be trimmed on right.
     * <br/>
     * For instance, with the following snippet:
     * <br/>
     *
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff"></font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;</font><br />
     * <font color="#808080">3</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;Dummy&nbsp;Javadoc&nbsp;comment.</font><br />
     * <font color="#808080">4</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@param&nbsp;</font>
     * <font color="#3f5fbf">s&nbsp;a&nbsp;String</font><br />
     * <font color="#808080">5</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&#47;</font><br />
     * <font color="#808080">6</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#7f0055"><b>public&nbsp;</b></font><font color="#7f0055"><b>void&nbsp;</b></font>
     * <font color="#000000">dummyMethod</font><font color="#000000">(&nbsp;</font>
     * <font color="#000000">String&nbsp;s&nbsp;</font><font color="#000000">){}</font><br />
     * </code>
     *
     * <br/>
     * The return will be:
     * <br/>
     *
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;</font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;Dummy&nbsp;Javadoc&nbsp;comment.</font><br />
     * <font color="#808080">3</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@param&nbsp;</font>
     * <font color="#3f5fbf">s&nbsp;a&nbsp;String</font><br />
     * <font color="#808080">4</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&#47;</font><br />
     * </code>
     *
     * @param javaClassContent not null
     * @param entity not null
     * @return return the original javadoc as String for the current entity
     * @throws IOException if any
     */
    private static String extractOriginalJavadoc( final String javaClassContent, final AbstractJavaEntity entity )
        throws IOException
    {
        if ( entity.getComment() == null )
        {
            return "";
        }

        String[] javaClassContentLines = getLines( javaClassContent );
        List<String> list = new LinkedList<String>();
        for ( int i = entity.getLineNumber() - 2; i >= 0; i-- )
        {
            String line = javaClassContentLines[i];

            list.add( trimRight( line ) );
            if ( line.trim().startsWith( START_JAVADOC ) )
            {
                break;
            }
        }

        Collections.reverse( list );

        return StringUtils.join( list.iterator(), EOL );
    }

    /**
     * Extract the Javadoc comment between {@link #START_JAVADOC} and {@link #END_JAVADOC} form the entity. This method
     * takes care of the Javadoc indentation. All javadoc lines will be trimmed on right.
     * <br/>
     * For instance, with the following snippet:
     * <br/>
     *
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff"></font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#47;&#42;&#42;</font><br />
     * <font color="#808080">3</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;Dummy&nbsp;Javadoc&nbsp;comment.</font><br />
     * <font color="#808080">4</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@param&nbsp;</font>
     * <font color="#3f5fbf">s&nbsp;a&nbsp;String</font><br />
     * <font color="#808080">5</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&#47;</font><br />
     * <font color="#808080">6</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#7f0055"><b>public&nbsp;</b></font><font color="#7f0055"><b>void&nbsp;</b></font>
     * <font color="#000000">dummyMethod</font><font color="#000000">(&nbsp;</font>
     * <font color="#000000">String&nbsp;s&nbsp;</font><font color="#000000">){}</font><br />
     * </code>
     *
     * <br/>
     * The return will be:
     * <br/>
     *
     * <code>
     * <font color="#808080">1</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;Dummy&nbsp;Javadoc&nbsp;comment.</font><br />
     * <font color="#808080">2</font>&nbsp;<font color="#ffffff">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
     * <font color="#3f5fbf">&#42;&nbsp;</font><font color="#7f9fbf">@param&nbsp;</font>
     * <font color="#3f5fbf">s&nbsp;a&nbsp;String</font><br />
     * </code>
     *
     * @param javaClassContent not null
     * @param entity not null
     * @return return the original javadoc as String for the current entity
     * @throws IOException if any
     */
    private static String extractOriginalJavadocContent( final String javaClassContent,
                                                         final AbstractJavaEntity entity )
        throws IOException
    {
        if ( entity.getComment() == null )
        {
            return "";
        }

        String originalJavadoc = extractOriginalJavadoc( javaClassContent, entity );
        if ( originalJavadoc.indexOf( START_JAVADOC ) != -1 )
        {
            originalJavadoc =
                originalJavadoc.substring( originalJavadoc.indexOf( START_JAVADOC ) + START_JAVADOC.length() );
        }
        if ( originalJavadoc.indexOf( END_JAVADOC ) != -1 )
        {
            originalJavadoc = originalJavadoc.substring( 0, originalJavadoc.indexOf( END_JAVADOC ) );
        }
        if ( originalJavadoc.startsWith( "\r\n" ) )
        {
            originalJavadoc = originalJavadoc.substring( 2 );
        }
        else if ( originalJavadoc.startsWith( "\n" ) || originalJavadoc.startsWith( "\r" ) )
        {
            originalJavadoc = originalJavadoc.substring( 1 );
        }

        return trimRight( originalJavadoc );
    }

    /**
     * @param content not null
     * @return the content without last lines containing javadoc separator (ie <code> * </code>)
     * @throws IOException if any
     * @see #getJavadocComment(String, AbstractInheritableJavaEntity, DocletTag)
     */
    private static String removeLastEmptyJavadocLines( final String content )
        throws IOException
    {
        if ( content.indexOf( EOL ) == -1 )
        {
            return content;
        }

        String[] lines = getLines( content );
        if ( lines.length == 1 )
        {
            return content;
        }

        List<String> linesList = new LinkedList<String>();
        linesList.addAll( Arrays.asList( lines ) );

        Collections.reverse( linesList );

        for ( Iterator<String> it = linesList.iterator(); it.hasNext(); )
        {
            String line = it.next();

            if ( line.trim().equals( "*" ) )
            {
                it.remove();
            }
            else
            {
                break;
            }
        }

        Collections.reverse( linesList );

        return StringUtils.join( linesList.iterator(), EOL );
    }

    /**
     * @param content not null
     * @return the javadoc comment with the given indentation
     * @throws IOException if any
     * @see #getJavadocComment(String, AbstractInheritableJavaEntity, DocletTag)
     */
    private static String alignIndentationJavadocLines( final String content, final String indent )
        throws IOException
    {
        String[] lines = getLines( content );

        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < lines.length; i++ )
        {
            String line = lines[i];
            if ( !line.trim().startsWith( "*" ) )
            {
                line = "*" + line;
            }
            sb.append( indent ).append( " " ).append( trimLeft( line ) );
            if ( i < lines.length - 1 )
            {
                sb.append( EOL );
            }
        }

        return sb.toString();
    }

    /**
     * Autodetect the indentation of a given line:
     * <pre>
     * autodetectIndentation( null ) = "";
     * autodetectIndentation( "a" ) = "";
     * autodetectIndentation( "    a" ) = "    ";
     * autodetectIndentation( "\ta" ) = "\t";
     * </pre>
     *
     * @param line not null
     * @return the indentation for the given line.
     */
    private static String autodetectIndentation( final String line )
    {
        if ( StringUtils.isEmpty( line ) )
        {
            return "";
        }

        return line.substring( 0, line.indexOf( trimLeft( line ) ) );
    }

    /**
     * @param content not null
     * @return an array of all content lines
     * @throws IOException if any
     */
    private static String[] getLines( final String content )
        throws IOException
    {
        List<String> lines = new LinkedList<String>();

        BufferedReader reader = new BufferedReader( new StringReader( content ) );
        String line = reader.readLine();
        while ( line != null )
        {
            lines.add( line );
            line = reader.readLine();
        }

        return (String[]) lines.toArray( new String[0] );
    }

    /**
     * Trim a given line on the left:
     * <pre>
     * trimLeft( null ) = "";
     * trimLeft( "  " ) = "";
     * trimLeft( "a" ) = "a";
     * trimLeft( "    a" ) = "a";
     * trimLeft( "\ta" ) = "a";
     * trimLeft( "    a    " ) = "a    ";
     * </pre>
     *
     * @param text
     * @return the text trimmed on left side or empty if text is null.
     */
    private static String trimLeft( final String text )
    {
        if ( StringUtils.isEmpty( text ) || StringUtils.isEmpty( text.trim() ) )
        {
            return "";
        }

        String textTrimmed = text.trim();
        return text.substring( text.indexOf( textTrimmed ), text.length() );
    }

    /**
     * Trim a given line on the right:
     * <pre>
     * trimRight( null ) = "";
     * trimRight( "  " ) = "";
     * trimRight( "a" ) = "a";
     * trimRight( "a\t" ) = "a";
     * trimRight( "    a    " ) = "    a";
     * </pre>
     *
     * @param text
     * @return the text trimmed on tight side or empty if text is null.
     */
    private static String trimRight( final String text )
    {
        if ( StringUtils.isEmpty( text ) || StringUtils.isEmpty( text.trim() ) )
        {
            return "";
        }

        String textTrimmed = text.trim();
        return text.substring( 0, text.indexOf( textTrimmed ) + textTrimmed.length() );
    }

    /**
     * Workaroung for QDOX-173 about generic.
     *
     * @param params not null
     * @return the wanted params.
     */
    private static String[] fixQdox173( String[] params )
    {
        if ( params == null || params.length == 0 )
        {
            return params;
        }
        if ( params.length < 3 )
        {
            return params;
        }

        if ( params[0].trim().equals( "<" ) && params[2].trim().equals( ">" ) )
        {
            String param = params[1];
            List<String> l = new ArrayList<String>( Arrays.asList( params ) );
            l.set( 1, "<" + param + ">" );
            l.remove( 0 );
            l.remove( 1 );

            return (String[]) l.toArray( new String[0] );
        }

        return params;
    }

    /**
     * Wrapper class for the entity's tags.
     */
    private class JavaEntityTags
    {
        private final AbstractInheritableJavaEntity entity;

        private final boolean isJavaMethod;

        /** List of tag names. */
        private List<String> namesTags;

        /** Map with java parameter as key and original Javadoc lines as values. */
        private Map<String, String> tagParams;

        /** Original javadoc lines. */
        private String tagReturn;

        /** Map with java throw as key and original Javadoc lines as values. */
        private Map<String, String> tagThrows;

        /** Original javadoc lines for unknown tags. */
        private List<String> unknownsTags;

        public JavaEntityTags( AbstractInheritableJavaEntity entity, boolean isJavaMethod )
        {
            this.entity = entity;
            this.isJavaMethod = isJavaMethod;
            this.namesTags = new LinkedList<String>();
            this.tagParams = new LinkedHashMap<String, String>();
            this.tagThrows = new LinkedHashMap<String, String>();
            this.unknownsTags = new LinkedList<String>();
        }

        public List<String> getNamesTags()
        {
            return namesTags;
        }

        public String getJavadocReturnTag()
        {
            return tagReturn;
        }

        public void setJavadocReturnTag( String s )
        {
            tagReturn = s;
        }

        public List<String> getUnknownTags()
        {
            return unknownsTags;
        }

        public void putJavadocParamTag( String paramName, String originalJavadocTag )
        {
            tagParams.put( paramName, originalJavadocTag );
        }

        public String getJavadocParamTag( String paramName )
        {
            return getJavadocParamTag( paramName, false );
        }

        public String getJavadocParamTag( String paramName, boolean nullable )
        {
            String originalJavadocTag = (String) tagParams.get( paramName );
            if ( !nullable && originalJavadocTag == null && getLog().isWarnEnabled() )
            {
                getLog().warn( getMessage( paramName, "javaEntityTags.tagParams" ) );
            }

            return originalJavadocTag;
        }

        public void putJavadocThrowsTag( String paramName, String originalJavadocTag )
        {
            tagThrows.put( paramName, originalJavadocTag );
        }

        public String getJavadocThrowsTag( String paramName )
        {
            return getJavadocThrowsTag( paramName, false );
        }

        public String getJavadocThrowsTag( String paramName, boolean nullable )
        {
            String originalJavadocTag = (String) tagThrows.get( paramName );
            if ( !nullable && originalJavadocTag == null && getLog().isWarnEnabled() )
            {
                getLog().warn( getMessage( paramName, "javaEntityTags.tagThrows" ) );
            }

            return originalJavadocTag;
        }

        private String getMessage( String paramName, String mapName )
        {
            StringBuffer msg = new StringBuffer();
            msg.append( "No param '" ).append( paramName );
            msg.append( "' key found in " + mapName + " for the entity: " );
            if ( isJavaMethod )
            {
                JavaMethod javaMethod = (JavaMethod) entity;
                msg.append( getJavaMethodAsString( javaMethod ) );
            }
            else
            {
                JavaClass javaClass = (JavaClass) entity;
                msg.append( javaClass.getFullyQualifiedName() );
            }

            return msg.toString();
        }

        /** {@inheritDoc} */
        public String toString()
        {
            StringBuffer sb = new StringBuffer();

            sb.append( "namesTags=" ).append( namesTags ).append( "\n" );
            sb.append( "tagParams=" ).append( tagParams ).append( "\n" );
            sb.append( "tagReturn=" ).append( tagReturn ).append( "\n" );
            sb.append( "tagThrows=" ).append( tagThrows ).append( "\n" );
            sb.append( "unknownsTags=" ).append( unknownsTags ).append( "\n" );

            return sb.toString();
        }
    }
}
