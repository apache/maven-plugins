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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.clirr.core.ApiDifference;
import net.sf.clirr.core.MessageTranslator;

import org.apache.commons.lang.ClassUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.settings.Settings;
import org.codehaus.mojo.clirr.AbstractClirrMojo;
import org.codehaus.mojo.clirr.ArtifactSpecification;
import org.codehaus.mojo.clirr.ClirrDiffListener;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.ReflectionUtils;
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
import com.thoughtworks.qdox.parser.ParseException;

/**
 * Abstract class to fix Javadoc documentation and tags in source files.
 * <br/>
 * See <a href="http://java.sun.com/j2se/1.5.0/docs/tooldocs/windows/javadoc.html#wheretags">Where Tags Can Be Used</a>.
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

    /**
     * Pattern to find if a Javadoc line contains Javadoc tag for instance
     * <pre>
     * &#32;&#42; &#64;param X
     * </pre>
     */
    private static final Pattern JAVADOC_TAG_LINE_PATTERN =
        Pattern.compile( "\\s+\\*\\s+@\\w\\s*.*\\z", Pattern.DOTALL );

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

    /** Inherited Javadoc i.e. <code>&#47;&#42;&#42;{&#64;inheritDoc}&#42;&#47;</code> **/
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

    // ----------------------------------------------------------------------
    // Mojo components
    // ----------------------------------------------------------------------

    /**
     * Used by {@link ClirrMojoWrapper} class.
     *
     * @component
     */
    private ArtifactFactory artifactFactory;

    /**
     * Used by {@link ClirrMojoWrapper} class.
     *
     * @component
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * Used by {@link ClirrMojoWrapper} class.
     *
     * @component
     */
    private ArtifactResolver artifactResolver;

    /**
     * Input handler, needed for command line handling.
     *
     * @component
     */
    private InputHandler inputHandler;

    /**
     * Used by {@link ClirrMojoWrapper} class.
     *
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * Used by {@link ClirrMojoWrapper} class.
     *
     * @component
     */
    private MavenProjectBuilder mavenProjectBuilder;

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * Version to compare the current code against.
     * Used by {@link ClirrMojoWrapper} class.
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
     * <li><a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#public">public</a>
     * (shows only public classes and members)</li>
     * <li><a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#protected">protected</a>
     * (shows only public and protected classes and members)</li>
     * <li><a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#package">package</a>
     * (shows all classes and members not marked private)</li>
     * <li><a href="http://java.sun.com/j2se/1.4.2/docs/tooldocs/windows/javadoc.html#private">private</a>
     * (shows all classes and members)</li>
     * </ul>
     * <br/>
     *
     * @parameter expression="${level}" default-value="protected"
     */
    private String level;

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

    /**
     * Split {@link #fixTags} by comma.
     * @see {@link #init()}
     */
    private String[] fixTagsSplitted;

    /** New classes found by Clirr. */
    private List clirrNewClasses;

    /** New Methods in a Class (the key) found by Clirr. */
    private Map clirrNewMethods;

    /** List of classes where <code>&#42;since</code> is added. Will be used to add or not this tag in the methods. */
    private List sinceClasses;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !fixClassComment && !fixFieldComment && !fixMethodComment )
        {
            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "Specified to NOT fix classes, fields and methods. Nothing to do." );
            }
            return;
        }

        // verify goal params
        init();

        if ( fixTagsSplitted.length == 0 )
        {
            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "No fix tag specified. Nothing to do." );
            }
            return;
        }

        // add warranty msg
        if ( !preCheck() )
        {
            return;
        }

        // run clirr
        executeClirr();

        // run qdox and process
        try
        {
            JavaClass[] javaClasses = getQdoxClasses();

            for ( int i = 0; i < javaClasses.length; i++ )
            {
                JavaClass javaClass = javaClasses[i];

                processFix( javaClass );
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
     * @return the current classes directory.
     */
    protected abstract File getClassesDirectory();

    /**
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
    protected List getProjectSourceRoots( MavenProject p )
    {
        if ( "pom".equals( p.getPackaging().toLowerCase() ) )
        {
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn( "This project has 'pom' packaging, no Java sources will be available." );
            }
            return Collections.EMPTY_LIST;
        }

        return p.getCompileSourceRoots();
    }

    /**
     * @param p not null
     * @return the compile classpath elements
     * @throws DependencyResolutionRequiredException
     */
    protected List getCompileClasspathElements( MavenProject p )
        throws DependencyResolutionRequiredException
    {
        return p.getCompileClasspathElements();
    }

    /**
     * @param p not null
     * @return the project classLoader
     * @throws MojoExecutionException if any
     */
    protected ClassLoader getProjectClassLoader( MavenProject p )
        throws MojoExecutionException
    {
        List classPath;
        try
        {
            classPath = getCompileClasspathElements( p );
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new MojoExecutionException( "DependencyResolutionRequiredException: " + e.getMessage(), e );
        }

        List urls = new ArrayList( classPath.size() );
        Iterator iter = classPath.iterator();
        while ( iter.hasNext() )
        {
            try
            {
                urls.add( new File( ( (String) iter.next() ) ).toURL() );
            }
            catch ( MalformedURLException e )
            {
                throw new MojoExecutionException( "MalformedURLException: " + e.getMessage(), e );
            }
        }

        return new URLClassLoader( (URL[]) urls.toArray( new URL[urls.size()] ), null );
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
            List filtered = new LinkedList();
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
                level = "protected";
            }
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

        if ( !settings.isInteractiveMode() )
        {
            if ( getLog().isErrorEnabled() )
            {
                getLog().error(
                                "Maven is not attempt to interact with the user for input. "
                                    + "Verify the <interactiveMode/> configuration in your settings." );
            }
            return false;
        }

        if ( getLog().isWarnEnabled() )
        {
            getLog().warn( "" );
            getLog().warn( "    WARRANTY DISCLAIMER" );
            getLog().warn( "" );
            getLog().warn( "All warranties with regard to this Maven goal are disclaimed!" );
            getLog().warn( "The changes will be done directly in the source code." );
            getLog().warn(
                           "The Maven Team strongly recommends the use of a SCM software BEFORE executing this "
                               + "goal." );
            getLog().warn( "" );
        }

        while ( true )
        {
            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "Are you sure to proceed? [Y]es [N]o" );
            }

            try
            {
                String userExpression = inputHandler.readLine();
                if ( userExpression == null || userExpression.toLowerCase( Locale.ENGLISH ).equalsIgnoreCase( "Y" )
                    || userExpression.toLowerCase( Locale.ENGLISH ).equalsIgnoreCase( "Yes" ) )
                {
                    if ( getLog().isInfoEnabled() )
                    {
                        getLog().info( "OK, let's proceed..." );
                    }
                    break;
                }
                if ( userExpression == null || userExpression.toLowerCase( Locale.ENGLISH ).equalsIgnoreCase( "N" )
                    || userExpression.toLowerCase( Locale.ENGLISH ).equalsIgnoreCase( "No" ) )
                {
                    if ( getLog().isInfoEnabled() )
                    {
                        getLog().info( "No changes in your sources occur." );
                    }
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
     * Calling Clirr to find API differences via clirr-maven-plugin.
     *
     * @throws MojoExecutionException if any
     */
    private void executeClirr()
        throws MojoExecutionException
    {
        if ( ignoreClirr )
        {
            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "Clirr is ignored." );
            }
            return;
        }

        ClirrMojoWrapper wrapper = null;
        try
        {
            wrapper =
                new ClirrMojoWrapper( getClassesDirectory(), comparisonVersion, getArtifactType( project ),
                                      artifactFactory, localRepository, mavenProjectBuilder,
                                      artifactMetadataSource, project, artifactResolver, includes, excludes );

            wrapper.execute();
        }
        catch ( Exception e )
        {
            if ( getLog().isDebugEnabled() )
            {
                getLog().error( "Error when executing Clirr: " + e.getMessage(), e );
            }
            else
            {
                getLog().error( "Error when executing Clirr: " + e.getMessage() );
            }
            getLog().error( "Clirr is ignored due to the error above." );
            return;
        }

        clirrNewClasses = wrapper.getNewClasses();
        clirrNewMethods = wrapper.getNewMethods();

        if ( getLog().isInfoEnabled() )
        {
            if ( clirrNewClasses.isEmpty() && clirrNewMethods.isEmpty() )
            {
                getLog().info( "Clirr NOT found API differences." );
            }
            else
            {
                getLog().info( "Clirr found API differences, i.e. new classes/interfaces or methods." );
                try
                {
                    writeClirr();
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
    }

    /**
     * In debug mode, write {@link #clirrNewClasses} and {@link #clirrNewMethods} in the file
     * <code>project.getBuild().getDirectory()/clirr.diff</code>.
     * @throws IOException
     */
    private void writeClirr()
        throws IOException
    {
        if ( !getLog().isDebugEnabled() )
        {
            return;
        }

        StringBuffer sb = new StringBuffer();

        for ( Iterator it = clirrNewClasses.iterator(); it.hasNext(); )
        {
            String newClass = (String) it.next();

            sb.append( "Added Class " );
            sb.append( "'" ).append( newClass ).append( "'" );
            sb.append( EOL );
        }

        for ( Iterator it = clirrNewMethods.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();

            sb.append( "In the Class " );
            sb.append( "'" ).append( entry.getKey() ).append( "'" );
            sb.append( EOL );

            for ( Iterator it2 = ( (List) entry.getValue() ).iterator(); it2.hasNext(); )
            {
                String newMethod = (String) it2.next();

                sb.append( "\tAdded Method " );
                sb.append( "'" ).append( newMethod ).append( "'" );
                if ( it2.hasNext() )
                {
                    sb.append( EOL );
                }
            }
            sb.append( EOL );
        }

        File f = new File( project.getBuild().getDirectory(), "clirr.diff" );
        writeFile( f, WriterFactory.UTF_8, sb.toString() );

        getLog().debug( "Writing Clirr difference to: " + f.getAbsolutePath() );
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
     * @throws FileNotFoundException if any
     * @throws MojoExecutionException if any
     */
    private JavaClass[] getQdoxClasses()
        throws FileNotFoundException, IOException, MojoExecutionException
    {
        List javaFiles = new LinkedList();
        for ( Iterator i = getProjectSourceRoots( project ).iterator(); i.hasNext(); )
        {
            File f = new File( (String) i.next() );
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
        builder.getClassLibrary().addClassLoader( getProjectClassLoader( project ) );
        builder.setEncoding( encoding );
        for ( Iterator i = javaFiles.iterator(); i.hasNext(); )
        {
            File f = (File) i.next();
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
                // QDOX-118
                if ( getLog().isWarnEnabled() )
                {
                    getLog().warn( "QDOX ParseException: " + e.getMessage() );
                }
            }
        }

        return builder.getClasses();
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

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Reading '" + javaClass.getFullyQualifiedName() + "' class." );
        }

        final File javaFile = javaClass.getSource().getFile();
        // the original java content in memory
        final String originalContent = readFile( javaFile, encoding );

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
        writeFile( javaFile, encoding, stringWriter.toString() );
    }

    /**
     * Take care of block or single comments between Javadoc comment and entity declaration ie:
     * <pre>
     * &#47;&#42;&#42;
     * &#32;&#42; {Javadoc Comment}
     * &#32;&#42;&#47;
     * &#47;&#42;
     * &#32;&#42; {Block Comment}
     * &#32;&#42;&#47;
     * &#47;&#47; {Single comment}
     * entity
     * </pre>
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
        List modifiersAsList = Arrays.asList( modifiers );

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
     * <pre>
     * &#47;&#42;&#42;
     * &#32;&#42; {<i>Comment based on the class name</i>}
     * &#32;&#42;
     * &#32;&#42; &#64;author X {<i>added if <code>addMissingAuthor</code></i>}
     * &#32;&#42; &#64;version X {<i>added if <code>addMissingVersion</code></i>}
     * &#32;&#42; &#64;since X {<i>added if <code>addMissingSince</code> and new classes from previous version</i>}
     * &#32;&#42;&#47;
     * </pre>
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
     * <pre>
     * &#47;&#42;&#42; Constant &lt;code&gt;Field name&lt;/code&gt; &#42;&#47;
     * </pre>
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
                // QDOX-155 for char
                sb.append( field.getInitializationExpression().trim() );
            }

            if ( qualifiedName.equals( String.class.getName() ) )
            {
                StringBuffer value = new StringBuffer();
                // QDOX-156 and QDOX-157
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
     * <pre>
     * &#47;&#42;&#42;
     * &#32;&#42; {<i>Comment based on the class name</i>}
     * &#32;&#42;
     * &#32;&#42; &#64;param X {<i>added if <code>addMissingParam</code></i>}
     * &#32;&#42; &#64;return X {<i>added if <code>addMissingReturn</code></i>}
     * &#32;&#42; &#64;throws X {<i>added if <code>addMissingThrows</code>}
     * &#32;&#42; &#64;since X {<i>added if <code>addMissingSince</code> and new classes from previous version</i>}
     * &#32;&#42;&#47;
     * </pre>
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
        if ( fixTag( PARAM_TAG ) && javaMethod.getParameters() != null )
        {
            for ( int i = 0; i < javaMethod.getParameters().length; i++ )
            {
                JavaParameter javaParameter = javaMethod.getParameters()[i];

                separatorAdded = appendDefaultParamTag( sb, indent, separatorAdded, javaParameter );
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
                if ( StringUtils.removeDuplicateWhitespace( javadoc.trim() ).equals( INHERITED_JAVADOC ) )
                {
                    sb.append( indent ).append( INHERITED_JAVADOC );
                    sb.append( EOL );
                    stringWriter.write( sb.toString() );
                    return;
                }

                javadoc = removeLastEmptyJavadocLines( javadoc );
                if ( javadoc.indexOf( START_JAVADOC ) != -1 )
                {
                    javadoc = javadoc.substring( javadoc.indexOf( START_JAVADOC ) + START_JAVADOC.length() );
                }
                if ( javadoc.indexOf( END_JAVADOC ) != -1 )
                {
                    javadoc = javadoc.substring( 0, javadoc.indexOf( END_JAVADOC ) );
                }
                if ( StringUtils.removeDuplicateWhitespace( javadoc.trim() ).equals( "* " + INHERITED_TAG )
                    && ( javaMethod.getTags() == null || javaMethod.getTags().length == 0 ) )
                {
                    sb.append( indent ).append( START_JAVADOC ).append( INHERITED_TAG ).append( END_JAVADOC );
                    sb.append( EOL );
                }
                else
                {
                    sb.append( indent ).append( START_JAVADOC );
                    sb.append( EOL );
                    if ( javadoc.indexOf( INHERITED_TAG ) == -1 )
                    {
                        sb.append( indent ).append( SEPARATOR_JAVADOC ).append( INHERITED_TAG );
                        sb.append( EOL );
                        appendSeparator( sb, indent );
                    }
                    String leftTrimmed = trimLeft( javadoc );
                    if ( leftTrimmed.startsWith( "* " ) )
                    {
                        sb.append( indent ).append( " " ).append( leftTrimmed );
                    }
                    else
                    {
                        sb.append( indent ).append( SEPARATOR_JAVADOC ).append( leftTrimmed );
                    }
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
                            sb.append( trimRight( s ) );
                            sb.append( EOL );
                        }
                    }
                    sb.append( indent ).append( " " ).append( END_JAVADOC );
                    sb.append( EOL );
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
        JavaEntityTags javaEntityTags = parseJavadocTags( originalContent, entity, isJavaMethod );

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
     * @param isJavaMethod
     * @return an instance of {@link JavaEntityTags}
     * @throws IOException if any
     */
    private JavaEntityTags parseJavadocTags( final String originalContent,
                                             final AbstractInheritableJavaEntity entity, final boolean isJavaMethod )
        throws IOException
    {
        JavaEntityTags javaEntityTags = new JavaEntityTags( entity, isJavaMethod );
        for ( int i = 0; i < entity.getTags().length; i++ )
        {
            DocletTag docletTag = entity.getTags()[i];

            String originalJavadocTag = getJavadocComment( originalContent, entity, docletTag );
            originalJavadocTag = removeLastEmptyJavadocLines( originalJavadocTag );

            javaEntityTags.getNamesTags().add( docletTag.getName() );

            if ( isJavaMethod )
            {
                String[] params = docletTag.getParameters();
                if ( params.length < 1 )
                {
                    continue;
                }

                String paramName = params[0];
                if ( docletTag.getName().equals( PARAM_TAG ) )
                {
                    if ( paramName.equals( "<" ) )
                    {
                        paramName = params[1];
                    }
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
                    for ( Iterator it = javaEntityTags.getUnknownTags().iterator(); it.hasNext(); )
                    {
                        String originalJavadocTag = it.next().toString();

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
                for ( Iterator it = javaEntityTags.getUnknownTags().iterator(); it.hasNext(); )
                {
                    String originalJavadocTag = it.next().toString();

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
        String paramName = params[0];

        boolean genericParam = false;
        if ( paramName.equals( "<" ) )
        {
            paramName = params[1];
            genericParam = true;
        }

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

        JavaParameter javaParam = javaMethod.getParameterByName( paramName );
        if ( javaParam == null )
        {
            if ( genericParam )
            {
                String originalJavadocTag = javaEntityTags.getJavadocParamTag( paramName );
                if ( originalJavadocTag != null )
                {
                    sb.append( originalJavadocTag );
                }
            }
            else
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
        if ( !fixTag( RETURN_TAG ) )
        {
            // write original tag if found
            if ( StringUtils.isNotEmpty( javaEntityTags.getJavadocReturnTag() ) )
            {
                sb.append( javaEntityTags.getJavadocReturnTag() );
            }
            return;
        }

        if ( StringUtils.isNotEmpty( javaEntityTags.getJavadocReturnTag() ) && javaMethod.getReturns() != null
            && !javaMethod.getReturns().isVoid() )
        {
            sb.append( javaEntityTags.getJavadocReturnTag() );
            if ( javaEntityTags.getJavadocReturnTag().trim().endsWith( "@" + RETURN_TAG ) )
            {
                sb.append( " " );
                sb.append( getDefaultJavadocForType( javaMethod.getReturns() ) );
            }
        }
    }

    private void writeThrowsTag( final StringBuffer sb, final JavaMethod javaMethod,
                                 final JavaEntityTags javaEntityTags, final String[] params )
    {
        String paramName = params[0];

        String originalJavadocTag = javaEntityTags.getJavadocThrowsTag( paramName );
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

                if ( exception.getValue().endsWith( paramName ) )
                {
                    originalJavadocTag = StringUtils.replace( originalJavadocTag, paramName, exception.getValue() );
                    if ( StringUtils.removeDuplicateWhitespace( originalJavadocTag ).trim()
                                    .endsWith( "@" + THROWS_TAG + " " + exception.getValue() ) )
                    {
                        originalJavadocTag += " if any.";
                    }

                    sb.append( originalJavadocTag );

                    // added qualified name
                    javaEntityTags.putJavadocThrowsTag( exception.getValue(), originalJavadocTag );
                }
            }
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

            if ( fixTag( PARAM_TAG ) && javaMethod.getParameters() != null )
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
     * Verify if a method has <code>&#64;Override</code> annotation or if it is an inherited method from an interface
     * or a super class. The goal is to handle <code>&#123;&#64;inheritDoc&#125;</code> tag.
     *
     * @param javaMethod not null
     * @return <code>true</code> if the method is inherited.
     * @throws MojoExecutionException if any
     * @throws SecurityException if any
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

        Class clazz = getClass( javaMethod.getParentClass(), project );

        List interfaces = ClassUtils.getAllInterfaces( clazz );
        for ( Iterator it = interfaces.iterator(); it.hasNext(); )
        {
            Class intface = (Class) it.next();

            if ( intface.getDeclaredMethods() != null )
            {
                if ( isInherited( intface.getDeclaredMethods(), javaMethod ) )
                {
                    return true;
                }
            }
        }

        List classes = ClassUtils.getAllSuperclasses( clazz );
        for ( Iterator it = classes.iterator(); it.hasNext(); )
        {
            Class superClass = (Class) it.next();

            if ( superClass.getDeclaredMethods() != null )
            {
                if ( isInherited( superClass.getDeclaredMethods(), javaMethod ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @param methods not null
     * @param javaMethod not null
     * @return <code>true</code> if the javaMethod exists in methods.
     */
    private boolean isInherited( Method[] methods, JavaMethod javaMethod )
    {
        AccessibleObject.setAccessible( methods, true );
        for ( int i = 0; i < methods.length; i++ )
        {
            Method method = methods[i];

            if ( !method.getName().equals( javaMethod.getName() ) )
            {
                continue;
            }

            if ( javaMethod.getParameters() != null )
            {
                boolean isMaybeGeneric = false;
                List javaMethodParams = new LinkedList();

                for ( int j = 0; j < javaMethod.getParameters().length; j++ )
                {
                    Type type = javaMethod.getParameters()[j].getType();

                    // workaround for generics i.e. type.getValue() = E instead of real class
                    try
                    {
                        getClass( type.getJavaClass(), project );
                        if ( type.isArray() )
                        {
                            javaMethodParams.add( type.getValue() + "[]" );
                        }
                        else
                        {
                            javaMethodParams.add( type.getValue() );
                        }
                    }
                    catch ( MojoExecutionException e )
                    {
                        isMaybeGeneric = true;
                        break;
                    }
                }
                if ( !isMaybeGeneric )
                {
                    List methodParams = new LinkedList();
                    for ( int j = 0; j < method.getParameterTypes().length; j++ )
                    {
                        if ( method.getParameterTypes()[j].isArray() )
                        {
                            methodParams.add( method.getParameterTypes()[j].getComponentType().getName() + "[]" );
                        }
                        else
                        {
                            methodParams.add( method.getParameterTypes()[j].getName() );
                        }
                    }
                    if ( !methodParams.equals( javaMethodParams ) )
                    {
                        continue;
                    }
                }
            }

            return true;
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

        if ( type.isPrimitive() )
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
            getClass( type.getJavaClass(), project );

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

        if ( clirrNewClasses.contains( javaClass.getFullyQualifiedName() ) )
        {
            return true;
        }

        return false;
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

        List clirrMethods = (List) clirrNewMethods.get( javaMethod.getParentClass().getFullyQualifiedName() );
        if ( clirrMethods == null )
        {
            return false;
        }

        for ( Iterator it = clirrMethods.iterator(); it.hasNext(); )
        {
            String clirrMethod = (String) it.next(); // see java.lang.reflect.Method#toString()

            String javaMethodSignature = javaMethod.getDeclarationSignature( false );
            if ( clirrMethod.indexOf( javaMethodSignature ) != -1 )
            {
                return true;
            }

            String smallSignature = javaMethod.getName();
            if ( javaMethod.getReturns() != null )
            {
                smallSignature = javaMethod.getReturns().getValue() + " " + javaMethod.getName();
            }
            if ( clirrMethod.indexOf( smallSignature ) == -1 )
            {
                continue;
            }
            // Workaround to take care of generics
            if ( javaMethod.getParameters() != null )
            {
                boolean isMaybeGeneric = false;
                List javaMethodParams = new LinkedList();
                for ( int i = 0; i < javaMethod.getParameters().length; i++ )
                {
                    Type type = javaMethod.getParameters()[i].getType();

                    // QDOX-150: type.getValue() = E instead of real class...
                    try
                    {
                        getClass( type.getJavaClass(), project );
                        if ( type.isArray() )
                        {
                            javaMethodParams.add( type.getValue() + "[]" );
                        }
                        else
                        {
                            javaMethodParams.add( type.getValue() );
                        }
                    }
                    catch ( MojoExecutionException e )
                    {
                        isMaybeGeneric = true;
                        break;
                    }
                }
                if ( !isMaybeGeneric )
                {
                    if ( clirrMethod.indexOf( "(" + StringUtils.join( javaMethodParams.iterator(), ", " ) + ")" ) != -1 )
                    {
                        return true;
                    }
                }
                else
                {
                    if ( getLog().isWarnEnabled() )
                    {
                        StringBuffer warn = new StringBuffer();
                        warn.append( "Not sure if " );
                        warn.append( getJavaMethodAsString( javaMethod ) );
                        warn.append( " is newer or not, it is maybe due to generics. " );
                        warn.append( "You need to manually review it." );

                        getLog().warn( warn.toString() );
                    }
                }
            }
        }

        return false;
    }

    /**
     * Note: JavaClass doesn't handle generic class i.e. E
     *
     * @param javaClass not null
     * @param project not null
     * @return the class corresponding to the javaClass parameter.
     * @throws MojoExecutionException if any
     * @see {@link Class#forName(String, boolean, ClassLoader)}
     */
    private Class getClass( JavaClass javaClass, MavenProject project )
        throws MojoExecutionException
    {
        // primitive
        String qualifiedName = javaClass.getFullyQualifiedName();
        if ( qualifiedName.equals( Byte.TYPE.toString() ) )
        {
            return Byte.TYPE;
        }
        else if ( qualifiedName.equals( Short.TYPE.toString() ) )
        {
            return Short.TYPE;
        }
        else if ( qualifiedName.equals( Integer.TYPE.toString() ) )
        {
            return Integer.TYPE;
        }
        else if ( qualifiedName.equals( Long.TYPE.toString() ) )
        {
            return Long.TYPE;
        }
        else if ( qualifiedName.equals( Float.TYPE.toString() ) )
        {
            return Float.TYPE;
        }
        else if ( qualifiedName.equals( Double.TYPE.toString() ) )
        {
            return Double.TYPE;
        }
        else if ( qualifiedName.equals( Boolean.TYPE.toString() ) )
        {
            return Boolean.TYPE;
        }
        else if ( qualifiedName.equals( Character.TYPE.toString() ) )
        {
            return Character.TYPE;
        }

        try
        {
            return Class.forName( javaClass.getFullyQualifiedName(), false, getProjectClassLoader( project ) );
        }
        catch ( ClassNotFoundException e )
        {
            throw new MojoExecutionException( "ClassNotFoundException: " + e.getMessage(), e );
        }
    }

    /**
     * @param javaClass not null
     */
    private void addSinceClasses( JavaClass javaClass )
    {
        if ( sinceClasses == null )
        {
            sinceClasses = new ArrayList();
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
     * @param javaFile not null
     * @param encoding not null
     * @return the content of javaFile using the wanted encoding.
     * @throws IOException if any
     */
    private static String readFile( File javaFile, String encoding )
        throws IOException
    {
        Reader fileReader = null;
        try
        {
            fileReader = ReaderFactory.newReader( javaFile, encoding );
            return StringUtils.unifyLineSeparators( IOUtil.toString( fileReader ) );
        }
        finally
        {
            IOUtil.close( fileReader );
        }
    }

    /**
     * @param javaFile not null
     * @param encoding not null
     * @param content not null
     * @throws IOException if any
     */
    private static void writeFile( File javaFile, String encoding, String content )
        throws IOException
    {
        Writer writer = null;
        try
        {
            writer = WriterFactory.newWriter( javaFile, encoding );
            writer.write( content );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    /**
     * Default comment for class.
     *
     * @param javaClass not null
     * @return a default comment for class.
     */
    private static String getDefaultClassJavadocComment( JavaClass javaClass )
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
    private static String getDefaultMethodJavadocComment( JavaMethod javaMethod )
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
     * Work around for QDOX-146 about whitespace.
     * Ideally we want to use <code>entity.getComment()</code>
     *
     * @param javaClassContent original class content not null
     * @param entity not null
     * @return the javadoc comment for the entity without any tags.
     * @throws IOException if any
     */
    private static String getJavadocComment( String javaClassContent, AbstractJavaEntity entity )
        throws IOException
    {
        if ( entity.getComment() == null )
        {
            return "";
        }

        String originalJavadoc = extractOriginalJavadoc( javaClassContent, entity );
        String[] originalJavadocLines = getLines( originalJavadoc );

        if ( originalJavadocLines.length == 1 )
        {
            return originalJavadocLines[0];
        }

        List originalJavadocLinesAsList = new LinkedList();
        originalJavadocLinesAsList.addAll( Arrays.asList( originalJavadocLines ) );

        Collections.reverse( originalJavadocLinesAsList );

        for ( Iterator it = originalJavadocLinesAsList.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();

            if ( line.endsWith( END_JAVADOC ) )
            {
                break;
            }

            it.remove();
        }

        Collections.reverse( originalJavadocLinesAsList );

        boolean toremove = false;
        for ( Iterator it = originalJavadocLinesAsList.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();

            if ( toremove )
            {
                it.remove();
                continue;
            }

            if ( line.trim().equals( START_JAVADOC ) )
            {
                it.remove();
                continue;
            }
            if ( line.trim().equals( END_JAVADOC ) )
            {
                it.remove();
                break;
            }

            if ( line.indexOf( START_JAVADOC ) != -1 )
            {
                line = line.substring( line.indexOf( START_JAVADOC ) + START_JAVADOC.length() );
            }
            if ( line.indexOf( END_JAVADOC ) != -1 )
            {
                line = line.substring( 0, line.indexOf( END_JAVADOC ) );
            }

            Matcher matcher = JAVADOC_TAG_LINE_PATTERN.matcher( line );
            if ( matcher.find() )
            {
                it.remove();
                toremove = true;
                continue;
            }
        }

        return StringUtils.join( originalJavadocLinesAsList.iterator(), EOL );
    }

    /**
     * Work around for QDOX-146 about whitespace.
     * Ideally we want to use <code>docletTag.getValue()</code>
     *
     * @param javaClassContent original class content not null
     * @param entity not null
     * @param docletTag not null
     * @return the javadoc comment for the entity without Javadoc tags.
     * @throws IOException if any
     */
    private static String getJavadocComment( String javaClassContent, AbstractInheritableJavaEntity entity,
                                             DocletTag docletTag )
        throws IOException
    {
        if ( docletTag.getValue() == null )
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
        String[] originalJavadocLines = getLines( originalJavadoc );

        if ( originalJavadocLines.length == 1 )
        {
            return originalJavadocLines[0];
        }

        // Note: docletTag.getValue() removes duplicate whitespace
        String[] docletTagLines = getLines( docletTag.getValue() );

        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < docletTagLines.length; i++ )
        {
            boolean found = false;
            for ( int j = 0; j < originalJavadocLines.length; j++ )
            {
                String line = originalJavadocLines[j];

                if ( line.indexOf( "@" + docletTag.getName() ) != -1 )
                {
                    found = true;
                }
                if ( found
                    && StringUtils.removeDuplicateWhitespace( line ).trim()
                                  .endsWith( StringUtils.removeDuplicateWhitespace( docletTagLines[i] ).trim() ) )
                {
                    sb.append( line );
                    sb.append( EOL );
                    if ( docletTag.getParameters().length == 1 )
                    {
                        break;
                    }
                }
            }
        }

        if ( sb.toString().lastIndexOf( EOL ) != -1 )
        {
            return sb.toString().substring( 0, sb.toString().lastIndexOf( EOL ) );
        }

        return sb.toString();
    }

    /**
     * Extract the original Javadoc and others comments up to {@link #START_JAVADOC} form the entity.
     *
     * @param javaClassContent not null
     * @param entity not null
     * @return return the original javadoc as String for the current entity
     * @throws IOException if any
     */
    private static String extractOriginalJavadoc( String javaClassContent, AbstractJavaEntity entity )
        throws IOException
    {
        if ( entity.getComment() == null )
        {
            return "";
        }

        List list = new LinkedList();
        String[] javaClassContentLines = getLines( javaClassContent );
        for ( int i = entity.getLineNumber() - 2; i >= 0; i-- )
        {
            String line = javaClassContentLines[i];

            list.add( line );
            if ( line.trim().startsWith( START_JAVADOC ) )
            {
                break;
            }
        }

        Collections.reverse( list );

        return trimLeft( StringUtils.join( list.iterator(), EOL ) );
    }

    /**
     * @param content not null
     * @return the content without javadoc separator (ie <code> * </code>)
     * @throws IOException if any
     */
    private static String removeLastEmptyJavadocLines( String content )
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

        List linesList = new LinkedList();
        linesList.addAll( Arrays.asList( lines ) );

        Collections.reverse( linesList );

        for ( Iterator it = linesList.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();

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
     * @param line not null
     * @return the indentation for the given line.
     */
    private static String autodetectIndentation( String line )
    {
        int i = line.indexOf( line.trim() );
        if ( i == -1 )
        {
            return "";
        }

        return line.substring( 0, i );
    }

    /**
     * @param content not null
     * @return an array of all content lines
     * @throws IOException if any
     */
    private static String[] getLines( String content )
        throws IOException
    {
        List lines = new LinkedList();

        BufferedReader reader = new BufferedReader( new StringReader( content ) );
        String line = reader.readLine();
        while ( line != null )
        {
            lines.add( line );
            line = reader.readLine();
        }

        try
        {
            return (String[]) lines.toArray( new String[0] );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    /**
     * @param text
     * @return the text trimmed on left side or empty if text is null.
     */
    private static String trimLeft( String text )
    {
        if ( StringUtils.isEmpty( text ) || StringUtils.isEmpty( text.trim() ) )
        {
            return "";
        }

        for ( int i = 0; i < text.length(); i++ )
        {
            if ( !Character.isWhitespace( text.charAt( i ) ) )
            {
                return text.substring( i );
            }
        }

        return text;
    }

    /**
     * @param text
     * @return the text trimmed on tight side or empty if text is null.
     */
    private static String trimRight( String text )
    {
        if ( StringUtils.isEmpty( text ) || StringUtils.isEmpty( text.trim() ) )
        {
            return "";
        }

        for ( int i = text.length() - 1; i > 0; i-- )
        {
            if ( !Character.isWhitespace( text.charAt( i ) ) )
            {
                return text.substring( 0, i + 1 );
            }
        }

        return text;
    }

    /**
     * Wrapper implementation of the {@link AbstractClirrMojo}.
     */
    private static class ClirrMojoWrapper
        extends AbstractClirrMojo
    {
        private List clirrNewClasses;

        private Map clirrNewMethods;

        public ClirrMojoWrapper( File classesDirectory, String comparisonVersion, String artifactType,
                                 ArtifactFactory factory, ArtifactRepository localRepository,
                                 MavenProjectBuilder mavenProjectBuilder, ArtifactMetadataSource metadataSource,
                                 MavenProject project, ArtifactResolver resolver, String includes, String excludes )
            throws MojoFailureException
        {
            super();

            try
            {
                super.classesDirectory = classesDirectory;
                ArtifactSpecification artifactSpec = new ArtifactSpecification();
                artifactSpec.setGroupId( project.getGroupId() );
                artifactSpec.setArtifactId( project.getArtifactId() );
                artifactSpec.setVersion( comparisonVersion );
                artifactSpec.setType( artifactType );
                artifactSpec.setClassifier( null );
                super.comparisonArtifacts = new ArtifactSpecification[] { artifactSpec };
                super.factory = factory;
                super.localRepository = localRepository;
                ReflectionUtils.setVariableValueInObject( this, "mavenProjectBuilder", mavenProjectBuilder );
                ReflectionUtils.setVariableValueInObject( this, "metadataSource", metadataSource );
                super.project = project;
                super.resolver = resolver;
                // TODO align includes/excludes with org.codehaus.mojo.clirr.ClirrClassFilter
                if ( includes != null )
                {
                    super.includes = StringUtils.split( "**", "," );
                }
                if ( excludes != null )
                {
                    super.excludes = null;
                }
            }
            catch ( IllegalArgumentException e )
            {
                throw new MojoFailureException( "IllegalArgumentException: " + e.getMessage() );
            }
            catch ( SecurityException e )
            {
                throw new MojoFailureException( "SecurityException: " + e.getMessage() );
            }
            catch ( IllegalAccessException e )
            {
                throw new MojoFailureException( "IllegalAccessException: " + e.getMessage() );
            }
        }

        /** {@inheritDoc} */
        public void execute()
            throws MojoExecutionException, MojoFailureException
        {
            ClirrDiffListener clirrDiffListener = executeClirr();

            clirrNewClasses = new ArrayList();
            clirrNewMethods = new HashMap();

            MessageTranslator translator = new MessageTranslator();
            translator.setLocale( Locale.ENGLISH );

            for ( Iterator it = clirrDiffListener.getApiDifferences().iterator(); it.hasNext(); )
            {
                ApiDifference diff = (ApiDifference) it.next();
                String msg = diff.getReport( translator );

                // Align to Clirr messages
                if ( msg.startsWith( "Class" ) && msg.endsWith( "added" ) )
                {
                    clirrNewClasses.add( diff.getAffectedClass() );
                }

                if ( msg.startsWith( "Method" ) && msg.endsWith( "added" ) )
                {
                    List list = (List) clirrNewMethods.get( diff.getAffectedClass() );
                    if ( list == null )
                    {
                        list = new ArrayList();
                    }
                    list.add( diff.getAffectedMethod() );
                    clirrNewMethods.put( diff.getAffectedClass(), list );
                }

                if ( msg.startsWith( "Method" ) && msg.endsWith( "added to an interface" ) )
                {
                    List list = (List) clirrNewMethods.get( diff.getAffectedClass() );
                    if ( list == null )
                    {
                        list = new ArrayList();
                    }
                    list.add( diff.getAffectedMethod() );
                    clirrNewMethods.put( diff.getAffectedClass(), list );
                }
            }
        }

        /**
         * @return a list of added classes.
         */
        public List getNewClasses()
        {
            return clirrNewClasses;
        }

        /**
         * @return a map with the String of affected class (as key) and a list of the String of added methods
         * (as value).
         */
        public Map getNewMethods()
        {
            return clirrNewMethods;
        }
    }

    /**
     * Wrapper class for the entity's tags.
     */
    private class JavaEntityTags
    {
        private final AbstractInheritableJavaEntity entity;

        private final boolean isJavaMethod;

        /** List of tag names. */
        private List namesTags;

        /** Map with java parameter as key and original Javadoc lines as values. */
        private Map tagParams;

        /** Original javadoc lines. */
        private String tagReturn;

        /** Map with java throw as key and original Javadoc lines as values. */
        private Map tagThrows;

        /** Original javadoc lines for unknown tags. */
        private List unknownsTags;

        public JavaEntityTags( AbstractInheritableJavaEntity entity, boolean isJavaMethod )
        {
            this.entity = entity;
            this.isJavaMethod = isJavaMethod;
            this.namesTags = new LinkedList();
            this.tagParams = new LinkedHashMap();
            this.tagThrows = new LinkedHashMap();
            this.unknownsTags = new LinkedList();
        }

        public List getNamesTags()
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

        public List getUnknownTags()
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
