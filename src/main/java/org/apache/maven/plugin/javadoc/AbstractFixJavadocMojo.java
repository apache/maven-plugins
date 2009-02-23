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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.clirr.core.ApiDifference;
import net.sf.clirr.core.MessageTranslator;

import org.apache.commons.lang.ClassUtils;
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
 * @since 2.5.1
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

    /** Inherited Javadoc i.e. <code>&#47;&#42;&#42;{&#64;inheritDoc}&#42;&#47;</code> **/
    private static final String INHERITED_JAVADOC = START_JAVADOC + " " + INHERITED_TAG + " " + END_JAVADOC;

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
     * By default, it is a <a href="http://svnbook.red-bean.com/en/1.1/ch07s02.html#svn-ch-7-sect-2.3.4">SVN keyword</a>.
     * Refer to your SCM to use an other SCM keyword.
     *
     * @parameter expression="${defaultVersion}" default-value="$Id$"
     */
    private String defaultVersion;

    /**
     * The file encoding to use when reading the source files. If the property <code>project.build.sourceEncoding</code>
     * is not set, the platform default encoding is used.
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

    /**
     * API differences found by Clirr.
     */
    private List clirrApiDifferences;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !fixClassComment && !fixFieldComment && !fixMethodComment )
        {
            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "NOT fix classes, fields and methods specified. Nothing to do." );
                return;
            }
        }

        // add warranty msg
        if ( !preCheck() )
        {
            return;
        }

        // verify goal params
        init();

        // run clirr
        executeClirr();

        // run qdox and processing
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
     * Calling Clirr to find API differences via clirr-maven-plugin.
     *
     * @throws MojoExecutionException if any
     */
    protected void executeClirr()
        throws MojoExecutionException, MojoFailureException
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
            getLog().error( "Clirr is ignored" );
            return;
        }

        this.clirrApiDifferences = wrapper.getClirrDiffListener().getApiDifferences();

        if ( getLog().isInfoEnabled() )
        {
            if ( clirrApiDifferences.isEmpty() )
            {
                getLog().info( "Clirr NOT found Api differences." );
            }
            else
            {
                getLog().info( "Clirr found Api differences." );

                if ( getLog().isDebugEnabled() )
                {
                    StringBuffer sb = new StringBuffer();

                    MessageTranslator translator = new MessageTranslator();
                    translator.setLocale( Locale.ENGLISH );
                    for ( Iterator it = clirrApiDifferences.iterator(); it.hasNext(); )
                    {
                        ApiDifference diff = (ApiDifference) it.next();

                        sb.append( diff.getReport( translator ) );
                        if ( it.hasNext() )
                        {
                            sb.append( EOL );
                        }
                    }

                    getLog().debug( sb.toString() );
                }
            }
        }
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
        int i = defaultSince.indexOf( "-SNAPSHOT" );
        if ( i != -1 )
        {
            defaultSince = defaultSince.substring( 0, i );
        }

        // fixTags
        if ( StringUtils.isEmpty( fixTags ) )
        {
            // default
            fixTags = "all";
        }
        if ( !fixTags.trim().equals( "all" ) )
        {
            String[] split = StringUtils.split( fixTags, "," );
            List filtered = new LinkedList();
            for ( int j = 0; j < split.length; j++ )
            {
                String s = split[j];
                if ( !( s.equals( "all" ) || s.equals( AUTHOR_TAG ) || s.equals( VERSION_TAG )
                    || s.equals( SINCE_TAG ) || s.equals( PARAM_TAG ) || s.equals( RETURN_TAG ) || s
                                                                                                    .equals( THROWS_TAG ) ) )
                {
                    if ( getLog().isWarnEnabled() )
                    {
                        getLog().warn( "Unrecognized '" + s + "' for fixTags parameter. Ignored it!" );
                    }
                }
                else
                {
                    filtered.add( s );
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
        if ( !( "public".equalsIgnoreCase( level.trim() ) || "protected".equalsIgnoreCase( level.trim() )
            || "package".equalsIgnoreCase( level.trim() ) || "private".equalsIgnoreCase( level.trim() ) ) )
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
            getLog().warn( EOL );
            getLog().warn( "WARRANTY DISCLAIMER" );
            getLog().warn( EOL );
            getLog().warn( "All warranties with regard to this Maven goal are disclaimed!" );
            getLog().warn(
                           "The Maven Team strongly recommends the use of a SCM software BEFORE executing "
                               + "this goal." );
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
     * @param tag not null
     * @return <code>true</code> if <code>tag</code> is defined in {@link #fixTags}.
     */
    private boolean fixTag( String tag )
    {
        if ( fixTagsSplitted.length == 1 && fixTagsSplitted[0].equals( "all" ) )
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
            javaFiles.addAll( FileUtils.getFiles( new File( (String) i.next() ), includes, excludes, true ) );
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
            getLog().debug( "Reading " + javaClass.getFullyQualifiedName() );
        }
        final File javaFile = javaClass.getSource().getFile();
        Reader fileReader = null;
        // the original java content in memory
        String originalContent;
        try
        {
            fileReader = ReaderFactory.newReader( javaFile, encoding );
            originalContent = StringUtils.unifyLineSeparators( IOUtil.toString( fileReader ) );
        }
        finally
        {
            IOUtil.close( fileReader );
        }

        final StringWriter stringWriter = new StringWriter();

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Fixing " + javaClass.getFullyQualifiedName() );
        }
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

                if ( lineNumber == javaClass.getLineNumber() )
                {
                    fixClassComment( stringWriter, originalContent, javaClass, indent );

                    takeCareSingleComment( stringWriter, originalContent, javaClass );
                }

                if ( javaClass.getFields() != null )
                {
                    for ( int i = 0; i < javaClass.getFields().length; i++ )
                    {
                        JavaField field = javaClass.getFields()[i];

                        if ( lineNumber == field.getLineNumber() )
                        {
                            fixFieldComment( stringWriter, field, indent );
                        }
                    }
                }

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
        final Writer writer = WriterFactory.newWriter( javaFile, encoding );
        try
        {
            writer.write( stringWriter.toString() );
        }
        finally
        {
            IOUtil.close( writer );
        }
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

        String javadocComment = extractOriginalJavadoc( originalContent, entity );
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

        if ( "public".equalsIgnoreCase( level.trim() ) )
        {
            if ( modifiersAsList.contains( "public" ) )
            {
                return true;
            }

            return false;
        }

        if ( "protected".equalsIgnoreCase( level.trim() ) )
        {
            if ( modifiersAsList.contains( "public" ) || modifiersAsList.contains( "protected" ) )
            {
                return true;
            }

            return false;
        }

        if ( "package".equalsIgnoreCase( level.trim() ) )
        {
            if ( !modifiersAsList.contains( "private" ) )
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
     * @see #addDefaultAuthor(StringBuffer, String)
     * @see #addDefaultSince(StringBuffer, String)
     * @see #addDefaultVersion(StringBuffer, String)
     */
    private void addDefaultClassComment( final StringWriter stringWriter, final JavaClass javaClass,
                                         final String indent )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( indent ).append( START_JAVADOC );
        sb.append( EOL );
        sb.append( indent ).append( " * " );
        sb.append( getDefaultClassJavadocComment( javaClass ) );
        sb.append( EOL );
        addSeparator( sb, indent );
        if ( fixTag( AUTHOR_TAG ) )
        {
            addDefaultAuthor( sb, indent );
        }
        if ( fixTag( VERSION_TAG ) )
        {
            addDefaultVersion( sb, indent );
        }
        if ( fixTag( SINCE_TAG ) )
        {
            if ( !ignoreClirr )
            {
                if ( isNewClassFromLastVersion( javaClass ) )
                {
                    addDefaultSince( sb, indent );
                }
            }
            else
            {
                addDefaultSince( sb, indent );
            }
        }
        sb.append( indent ).append( " " ).append( END_JAVADOC );
        sb.append( EOL );

        stringWriter.write( sb.toString() );
    }

    /**
     * Add Javadoc field comment.
     * Acutally, only for static fields.
     *
     * @param stringWriter not null
     * @param field not null
     * @param indent not null
     * @throws IOException if any
     */
    private void fixFieldComment( final StringWriter stringWriter, final JavaField field, final String indent )
        throws IOException
    {
        if ( !fixFieldComment )
        {
            return;
        }

        if ( !isInLevel( field.getModifiers() ) )
        {
            return;
        }

        if ( !field.isStatic() )
        {
            return;
        }

        // add
        if ( field.getComment() == null )
        {
            addDefaultFieldComment( stringWriter, field, indent );
            return;
        }
    }

    /**
     * Add a default Javadoc for the given field, i.e.:
     * <pre>
     * &#47;&#42;&#42; Field name &#42;&#47;
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
                sb.append( field.getInitializationExpression().trim() );
            }

            if ( qualifiedName.equals( "java.lang.String" ) )
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
     * @see #addDefaultSince(StringBuffer, String)
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
        sb.append( indent ).append( " * " );
        sb.append( getDefaultMethodJavadocComment( javaMethod ) );
        sb.append( EOL );

        boolean addSeparator = false;
        if ( fixTag( PARAM_TAG ) && javaMethod.getParameters() != null )
        {
            for ( int i = 0; i < javaMethod.getParameters().length; i++ )
            {
                JavaParameter javaParameter = javaMethod.getParameters()[i];

                if ( !addSeparator )
                {
                    addSeparator( sb, indent );
                    addSeparator = true;
                }

                sb.append( indent ).append( " * @" ).append( PARAM_TAG ).append( " " );
                sb.append( javaParameter.getName() );
                sb.append( " " );
                sb.append( getDefaultJavadocForType( javaParameter.getType() ) );
                sb.append( EOL );
            }
        }
        if ( fixTag( RETURN_TAG ) && javaMethod.getReturns() != null && !javaMethod.getReturns().isVoid() )
        {
            if ( !addSeparator )
            {
                addSeparator( sb, indent );
                addSeparator = true;
            }
            sb.append( indent ).append( " * @" ).append( RETURN_TAG ).append( " " );
            sb.append( getDefaultJavadocForType( javaMethod.getReturns() ) );
            sb.append( EOL );
        }
        if ( fixTag( THROWS_TAG ) && javaMethod.getExceptions() != null && javaMethod.getExceptions().length > 0 )
        {
            for ( int i = 0; i < javaMethod.getExceptions().length; i++ )
            {
                Type exception = javaMethod.getExceptions()[i];

                if ( !addSeparator )
                {
                    addSeparator( sb, indent );
                    addSeparator = true;
                }

                sb.append( indent ).append( " * @" ).append( THROWS_TAG ).append( " " );
                sb.append( exception.getJavaClass().getFullyQualifiedName() );
                sb.append( " if any." );
                sb.append( EOL );
            }
        }
        if ( fixTag( SINCE_TAG ) && isNewMethodFromLastRevision( javaMethod ) )
        {
            if ( !addSeparator )
            {
                addSeparator( sb, indent );
                addSeparator = true;
            }
            addDefaultSince( sb, indent );
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
            int eol = 0;
            for ( int j = i - 1; j > 0; j-- )
            {
                if ( !Character.isWhitespace( s.charAt( j ) ) )
                {
                    eol = StringUtils.countMatches( s.substring( j, i + 1 ), EOL );
                    break;
                }
            }

            String tmp = trimRight( s.substring( 0, i ) );
            stringWriter.getBuffer().delete( 0, stringWriter.getBuffer().length() );
            stringWriter.write( tmp );
            if ( eol > 0 )
            {
                for ( int j = 0; j < eol; j++ )
                {
                    stringWriter.write( EOL );
                }
            }
            updateJavadocComment( stringWriter, originalContent, entity, indent );
        }
        else
        {
            updateJavadocComment( stringWriter, originalContent, entity, indent );
        }
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

        StringBuffer sb = new StringBuffer();

        // special case for inherited method
        if ( entity instanceof JavaMethod )
        {
            JavaMethod javaMethod = (JavaMethod) entity;

            if ( isInherited( javaMethod ) )
            {
                if ( StringUtils.isEmpty( javaMethod.getComment() ) )
                {
                    sb.append( indent ).append( INHERITED_JAVADOC );
                    sb.append( EOL );
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

                if ( javadoc.trim().equals( INHERITED_TAG )
                    && ( javaMethod.getTags() == null || javaMethod.getTags().length == 0 ) )
                {
                    sb.append( indent ).append( START_JAVADOC ).append( javadoc ).append( END_JAVADOC );
                    sb.append( EOL );
                }
                else
                {
                    sb.append( indent ).append( START_JAVADOC );
                    sb.append( EOL );
                    if ( javadoc.indexOf( INHERITED_TAG ) == -1 )
                    {
                        sb.append( indent ).append( " * " ).append( INHERITED_TAG );
                        sb.append( EOL );
                        addSeparator( sb, indent );
                    }
                    String leftTrimmed = trimLeft( javadoc );
                    if ( leftTrimmed.startsWith( "* " ) )
                    {
                        sb.append( indent ).append( " " ).append( leftTrimmed );
                    }
                    else
                    {
                        sb.append( indent ).append( " * " ).append( leftTrimmed );
                    }
                    sb.append( EOL );
                    if ( javaMethod.getTags() != null )
                    {
                        for ( int i = 0; i < javaMethod.getTags().length; i++ )
                        {
                            DocletTag docletTag = javaMethod.getTags()[i];

                            // volontary ignore these tags
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
            addDefaultJavadocComment( sb, entity, indent );
        }

        // tags
        if ( entity.getTags() != null && entity.getTags().length > 0 )
        {
            updateJavadocTags( sb, originalContent, entity, indent );
        }
        else
        {
            addDefaultJavadocTags( sb, entity, indent );
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
            comment = indent + " * " + comment.trim();
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
     */
    private void addDefaultJavadocComment( final StringBuffer sb, final AbstractInheritableJavaEntity entity,
                                           final String indent )
    {
        sb.append( indent ).append( " * " );
        if ( entity instanceof JavaMethod )
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
     * @throws IOException if any
     * @throws MojoExecutionException if any
     */
    private void updateJavadocTags( final StringBuffer sb, final String originalContent,
                                    final AbstractInheritableJavaEntity entity, final String indent )
        throws IOException, MojoExecutionException
    {
        boolean isJavaMethod = false;
        if ( entity instanceof JavaMethod )
        {
            isJavaMethod = true;
        }

        addSeparator( sb, indent );

        List tagNames = new LinkedList();
        List tagParams = new LinkedList();
        boolean hasTagReturn = false;
        List tagThrows = new LinkedList();

        for ( int i = 0; i < entity.getTags().length; i++ )
        {
            DocletTag docletTag = entity.getTags()[i];

            tagNames.add( docletTag.getName() );

            if ( docletTag.getName().equals( RETURN_TAG ) )
            {
                hasTagReturn = true;
            }

            if ( docletTag.getName().equals( THROWS_TAG ) )
            {
                String originalTag = getJavadocComment( originalContent, entity, docletTag );
                originalTag = removeLastEmptyJavadocLines( originalTag );

                StringTokenizer token =
                    new StringTokenizer( originalTag.substring( originalTag.indexOf( "@" + THROWS_TAG ) + 7 ), EOL
                        + " " );
                if ( token.countTokens() > 0 )
                {
                    tagThrows.add( token.nextToken() );
                }
            }

            if ( docletTag.getValue().length() > 0 )
            {
                String originalTag = getJavadocComment( originalContent, entity, docletTag );
                originalTag = removeLastEmptyJavadocLines( originalTag );
                originalTag = trimRight( originalTag );

                String param = null;

                if ( docletTag.getName().equals( PARAM_TAG ) )
                {
                    StringTokenizer token =
                        new StringTokenizer( originalTag.substring( originalTag.indexOf( "@" + PARAM_TAG ) + 6 ),
                                             EOL + " " );
                    if ( token.countTokens() > 0 )
                    {
                        param = token.nextToken();
                        tagParams.add( param );
                    }
                }

                if ( isJavaMethod && param != null )
                {
                    JavaMethod javaMethod = (JavaMethod) entity;

                    JavaParameter javaParam = javaMethod.getParameterByName( param );
                    if ( javaParam == null )
                    {
                        if ( getLog().isWarnEnabled() )
                        {
                            StringBuffer warn = new StringBuffer();

                            warn.append( "Fixed unknown param '" ).append( param ).append( "' defined in " );
                            warn.append( javaMethod.getParentClass().getFullyQualifiedName() );
                            warn.append( "#" ).append( javaMethod.getCallSignature() );

                            getLog().warn( warn.toString() );
                        }

                        if ( sb.toString().endsWith( EOL ) )
                        {
                            sb.delete( sb.toString().lastIndexOf( EOL ), sb.toString().length() );
                        }
                    }
                    else
                    {
                        sb.append( originalTag );
                        if ( StringUtils.removeDuplicateWhitespace( originalContent ).indexOf( "param " + param ) == -1 )
                        {
                            sb.append( " " );
                            sb.append( getDefaultJavadocForType( javaParam.getType() ) );
                        }
                    }
                }
                else
                {
                    if ( isJavaMethod && docletTag.getName().equals( THROWS_TAG ) && tagThrows.size() > 0 )
                    {
                        JavaMethod javaMethod = (JavaMethod) entity;

                        if ( javaMethod.getExceptions() != null )
                        {
                            for ( int j = 0; j < javaMethod.getExceptions().length; j++ )
                            {
                                Type exception = javaMethod.getExceptions()[j];

                                String throwException = tagThrows.get( tagThrows.size() - 1 ).toString();
                                if ( exception.getValue().endsWith( throwException ) )
                                {
                                    originalTag =
                                        StringUtils.replace( originalTag, throwException, exception.getValue() );
                                    tagThrows.add( tagThrows.size() - 1, exception.getValue() );
                                    if ( originalTag.endsWith( exception.getValue() ) )
                                    {
                                        originalTag += " if any";
                                    }

                                    break;
                                }
                            }
                        }
                        if ( !originalTag.trim().startsWith( "*" ) )
                        {
                            sb.append( indent ).append( " *" );
                        }
                        sb.append( originalTag );
                    }
                    else
                    {
                        if ( !originalTag.trim().startsWith( "*" ) )
                        {
                            sb.append( indent ).append( " *" );
                        }
                        sb.append( originalTag );
                    }
                }
            }
            else
            {
                if ( docletTag.getName().equals( RETURN_TAG ) )
                {
                    if ( isJavaMethod )
                    {
                        JavaMethod javaMethod = (JavaMethod) entity;

                        if ( javaMethod.getReturns() != null && !javaMethod.getReturns().isVoid() )
                        {
                            sb.append( indent ).append( " * @" ).append( RETURN_TAG ).append( " " );
                            sb.append( getDefaultJavadocForType( javaMethod.getReturns() ) );
                        }
                    }
                    else
                    {
                        sb.append( indent ).append( " * @" );
                        sb.append( docletTag.getName() );
                    }
                }
                else if ( !docletTag.getName().equals( PARAM_TAG ) )
                {
                    sb.append( indent ).append( " * @" );
                    sb.append( docletTag.getName() );
                }
            }
            sb.append( EOL );
        }

        // add missing tags...
        if ( isJavaMethod )
        {
            JavaMethod javaMethod = (JavaMethod) entity;

            for ( int i = 0; i < javaMethod.getParameters().length; i++ )
            {
                JavaParameter param = javaMethod.getParameters()[i];

                if ( !tagParams.contains( param.getName() ) )
                {
                    sb.append( indent ).append( " * @" ).append( PARAM_TAG ).append( " " );
                    sb.append( param.getName() );
                    sb.append( " " );
                    sb.append( getDefaultJavadocForType( param.getType() ) );
                    sb.append( EOL );
                }
            }

            if ( !hasTagReturn && javaMethod.getReturns() != null && !javaMethod.getReturns().isVoid() )
            {
                sb.append( indent ).append( " * @" ).append( RETURN_TAG ).append( " " );
                sb.append( getDefaultJavadocForType( javaMethod.getReturns() ) );
                sb.append( EOL );
            }

            if ( javaMethod.getExceptions() != null )
            {
                for ( int i = 0; i < javaMethod.getExceptions().length; i++ )
                {
                    Type exception = javaMethod.getExceptions()[i];

                    if ( !tagThrows.contains( exception.getValue() ) )
                    {
                        sb.append( indent ).append( " * @" ).append( THROWS_TAG ).append( " " );
                        sb.append( getDefaultJavadocForType( exception ) );
                        sb.append( " if any" );
                        sb.append( EOL );
                    }
                }
            }
        }
        if ( !isJavaMethod )
        {
            if ( fixTag( AUTHOR_TAG ) && !tagNames.contains( AUTHOR_TAG ) )
            {
                addDefaultAuthor( sb, indent );
            }
            if ( fixTag( VERSION_TAG ) && !tagNames.contains( VERSION_TAG ) )
            {
                addDefaultVersion( sb, indent );
            }
        }
        if ( fixTag( SINCE_TAG ) && !tagNames.contains( SINCE_TAG ) )
        {
            if ( !isJavaMethod )
            {
                if ( !ignoreClirr )
                {
                    if ( isNewClassFromLastVersion( (JavaClass) entity ) )
                    {
                        addDefaultSince( sb, indent );
                    }
                }
                else
                {
                    addDefaultSince( sb, indent );
                }
            }
            else
            {
                if ( isNewMethodFromLastRevision( (JavaMethod) entity ) )
                {
                    addDefaultSince( sb, indent );
                }
            }
        }
    }

    /**
     * @param sb not null
     * @param entity not null
     * @param indent not null
     */
    private void addDefaultJavadocTags( final StringBuffer sb, final AbstractInheritableJavaEntity entity,
                                        final String indent )
    {
        boolean isJavaMethod = false;
        if ( entity instanceof JavaMethod )
        {
            isJavaMethod = true;
        }
        boolean addSeparator = false;
        if ( isJavaMethod )
        {
            JavaMethod javaMethod = (JavaMethod) entity;

            if ( fixTag( PARAM_TAG ) && javaMethod.getParameters() != null )
            {
                for ( int i = 0; i < javaMethod.getParameters().length; i++ )
                {
                    JavaParameter javaParameter = javaMethod.getParameters()[i];

                    if ( !addSeparator )
                    {
                        addSeparator( sb, indent );
                        addSeparator = true;
                    }

                    sb.append( indent ).append( " * @" ).append( PARAM_TAG ).append( " " );
                    sb.append( javaParameter.getName() );
                    sb.append( " " );
                    sb.append( getDefaultJavadocForType( javaParameter.getType() ) );
                    sb.append( EOL );
                }
            }

            if ( fixTag( RETURN_TAG ) )
            {
                if ( javaMethod.getReturns() != null && !javaMethod.getReturns().isVoid() )
                {
                    if ( !addSeparator )
                    {
                        addSeparator( sb, indent );
                        addSeparator = true;
                    }

                    sb.append( indent ).append( " * @" ).append( RETURN_TAG ).append( " " );
                    sb.append( getDefaultJavadocForType( javaMethod.getReturns() ) );
                    sb.append( EOL );
                }
            }

            if ( fixTag( THROWS_TAG ) && javaMethod.getExceptions() != null )
            {
                for ( int i = 0; i < javaMethod.getExceptions().length; i++ )
                {
                    Type exception = javaMethod.getExceptions()[i];

                    if ( !addSeparator )
                    {
                        addSeparator( sb, indent );
                        addSeparator = true;
                    }

                    sb.append( indent ).append( " * @" ).append( THROWS_TAG ).append( " " );
                    sb.append( getDefaultJavadocForType( exception ) );
                    sb.append( " if any" );
                    sb.append( EOL );
                }
            }
        }

        if ( !isJavaMethod )
        {
            if ( fixTag( AUTHOR_TAG ) )
            {
                if ( !addSeparator )
                {
                    addSeparator( sb, indent );
                    addSeparator = true;
                }

                addDefaultAuthor( sb, indent );
            }
            if ( fixTag( VERSION_TAG ) )
            {
                if ( !addSeparator )
                {
                    addSeparator( sb, indent );
                    addSeparator = true;
                }

                addDefaultVersion( sb, indent );
            }
        }

        if ( fixTag( SINCE_TAG ) )
        {
            if ( !isJavaMethod )
            {
                JavaClass javaClass = (JavaClass) entity;

                if ( !addSeparator )
                {
                    addSeparator( sb, indent );
                    addSeparator = true;
                }

                if ( !ignoreClirr )
                {
                    if ( isNewClassFromLastVersion( javaClass ) )
                    {
                        addDefaultSince( sb, indent );
                    }
                }
                else
                {
                    addDefaultSince( sb, indent );
                }
            }
        }
    }

    /**
     * @param sb not null
     * @param indent not null
     */
    private void addDefaultAuthor( final StringBuffer sb, final String indent )
    {
        sb.append( indent ).append( " * @" ).append( AUTHOR_TAG ).append( " " );
        sb.append( defaultAuthor );
        sb.append( EOL );
    }

    /**
     * @param sb not null
     * @param indent not null
     */
    private void addDefaultSince( final StringBuffer sb, final String indent )
    {
        sb.append( indent ).append( " * @" ).append( SINCE_TAG ).append( " " );
        sb.append( defaultSince );
        sb.append( EOL );
    }

    /**
     * @param sb not null
     * @param indent not null
     */
    private void addDefaultVersion( final StringBuffer sb, final String indent )
    {
        sb.append( indent ).append( " * @" ).append( VERSION_TAG ).append( " " );
        sb.append( defaultVersion );
        sb.append( EOL );
    }

    /**
     * @param sb not null
     * @param indent not null
     */
    private void addSeparator( final StringBuffer sb, final String indent )
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
            sb.append( "a " );
            sb.append( type.getJavaClass().getFullyQualifiedName() );
            sb.append( "." );
        }
        else if ( type.isArray() )
        {
            sb.append( "an array of " );
            sb.append( type.getJavaClass().getFullyQualifiedName() );
            sb.append( "." );
        }
        else
        {
            try
            {
                Class clazz = getClass( type.getJavaClass(), project );

                if ( Exception.class.isAssignableFrom( clazz ) )
                {
                    sb.append( type.getJavaClass().getFullyQualifiedName() );
                }
                else
                {
                    sb.append( "a {@link " );
                    String s = type.getJavaClass().getFullyQualifiedName();
                    s = StringUtils.replace( s, "$", "." );
                    sb.append( s );
                    sb.append( "} object." );
                }
            }
            catch ( Exception e )
            {
                sb.append( type.getJavaClass().getFullyQualifiedName() );
            }
        }

        return sb.toString();
    }

    /**
     * Check under Clirr if this given class is newer from the last version.
     *
     * @param javaClass a given class not null
     * @return <code>true</code> if Clirr said that this class is added from the last version,
     * <code>false</code> otherwise or if clirrApiDifferences is null.
     */
    private boolean isNewClassFromLastVersion( JavaClass javaClass )
    {
        if ( clirrApiDifferences == null )
        {
            return false;
        }

        MessageTranslator translator = new MessageTranslator();
        translator.setLocale( Locale.ENGLISH );

        for ( Iterator it = clirrApiDifferences.iterator(); it.hasNext(); )
        {
            ApiDifference diff = (ApiDifference) it.next();
            String msg = diff.getReport( translator );

            if ( msg.indexOf( "added" ) == -1 )
            {
                continue;
            }

            if ( msg.indexOf( javaClass.getFullyQualifiedName() ) != -1 )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Check under Clirr if this given method is newer from the last version.
     *
     * @param javaMethod a given method not null
     * @return <code>true</code> if Clirr said that this method is added from the last version,
     * <code>false</code> otherwise or if clirrApiDifferences is null.
     * @throws MojoExecutionException  if any
     */
    private boolean isNewMethodFromLastRevision( JavaMethod javaMethod )
        throws MojoExecutionException
    {
        if ( clirrApiDifferences == null )
        {
            return false;
        }

        MessageTranslator translator = new MessageTranslator();
        translator.setLocale( Locale.ENGLISH );

        boolean notFound = false;
        for ( Iterator it = clirrApiDifferences.iterator(); it.hasNext(); )
        {
            ApiDifference diff = (ApiDifference) it.next();
            String msg = diff.getReport( translator );

            if ( msg.indexOf( "added" ) == -1 )
            {
                continue;
            }

            for ( int i = 0; i < javaMethod.getParentClass().getMethods().length; i++ )
            {
                JavaMethod method = javaMethod.getParentClass().getMethods()[i];

                if ( javaMethod.getDeclarationSignature( true ).equals( method.getDeclarationSignature( true ) ) )
                {
                    // Align to Clirr msg
                    StringBuffer sb = new StringBuffer();
                    sb.append( method.getName() );
                    sb.append( "(" );
                    for ( int j = 0; j < method.getParameters().length; j++ )
                    {
                        Class clazz = null;
                        try
                        {
                            clazz = getClass( method.getParameters()[j].getType().getJavaClass(), project );
                            sb.append( clazz.getName() );
                        }
                        catch ( Exception e )
                        {
                            // maybe generics
                            notFound = true;
                        }
                    }
                    sb.append( ")" );

                    if ( msg.indexOf( sb.toString() ) != -1 )
                    {
                        return true;
                    }
                }
            }
        }

        if ( notFound )
        {
            getLog().warn(
                           "Ignore difference in " + javaMethod.getParentClass().getFullyQualifiedName() + "#"
                               + javaMethod.getCallSignature() + " maybe due to generics" );
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

    // ----------------------------------------------------------------------
    // Static methods
    // ----------------------------------------------------------------------

    /**
     * @param javaClass
     * @return
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
     * Take care of getter/setter in the javaMethod.getName()
     *
     * @param javaMethod
     * @return
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
     * @return the javadoc comment for the entity without Javadoc tags.
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
        String[] docletTagLines = getLines( docletTag.getValue() );

        StringBuffer sb = new StringBuffer();

        boolean intag = false;
        for ( int i = 0; i < originalJavadocLines.length; i++ )
        {
            String line = originalJavadocLines[i];

            if ( intag )
            {
                Matcher matcher = JAVADOC_TAG_LINE_PATTERN.matcher( line );
                if ( matcher.find() || line.indexOf( END_JAVADOC ) != -1 )
                {
                    break;
                }
                sb.append( line );
                sb.append( EOL );
            }

            if ( !intag
                && line.indexOf( "@" + docletTag.getName() ) != -1
                && StringUtils.removeDuplicateWhitespace( line )
                              .endsWith( StringUtils.removeDuplicateWhitespace( docletTagLines[0] ) ) )
            {
                intag = true;

                sb.append( line );
                sb.append( EOL );
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
        private ClirrDiffListener clirrDiffListener;

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
            clirrDiffListener = executeClirr();
        }

        /**
         * @return the API differences found by Clirr.
         * @throws MojoExecutionException if any
         * @throws MojoFailureException if any
         */
        public ClirrDiffListener getClirrDiffListener()
            throws MojoExecutionException, MojoFailureException
        {
            if ( clirrDiffListener == null )
            {
                clirrDiffListener = executeClirr();
            }

            return clirrDiffListener;
        }
    }
}
