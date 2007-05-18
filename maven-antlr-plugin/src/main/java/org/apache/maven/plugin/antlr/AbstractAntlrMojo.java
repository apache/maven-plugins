package org.apache.maven.plugin.antlr;

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
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.antlr.options.Grammar;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.StringOutputStream;
import org.codehaus.plexus.util.StringUtils;

import antlr.Tool;

/**
 * Base class with majority of Antlr functionalities.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public abstract class AbstractAntlrMojo
    extends AbstractMojo
{
    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * Specifies the Antlr directory containing grammar files.
     *
     * @parameter expression="${basedir}/src/main/antlr"
     * @required
     */
    protected File sourceDirectory;

    /**
     * The Maven Project Object
     *
     * @parameter expression="${project}"
     * @required
     */
    protected MavenProject project;

    /**
     * The maven project's helper.
     *
     * @parameter expression="${component.org.apache.maven.project.MavenProjectHelper}"
     * @required
     * @readonly
     */
    private MavenProjectHelper projectHelper;

    // ----------------------------------------------------------------------
    // Antlr parameters
    // @see http://www.antlr.org/doc/options.html#Command%20Line%20Options
    // ----------------------------------------------------------------------

    /**
     * Specifies the destination directory where Antlr should generate files.
     * <br/>
     * See <a href="http://www.antlr.org/doc/options.html#Command%20Line%20Options">Command Line Options</a>
     *
     * @parameter expression="${project.build.directory}/generated-sources/antlr"
     * @required
     */
    protected File outputDirectory;

    /**
     * Comma separated grammar file names present in the <code>sourceDirectory</code> directory.
     * <br/>
     * See <a href="http://www.antlr.org/doc/options.html#Command%20Line%20Options">Command Line Options</a>
     *
     * @parameter expression="${grammars}"
     */
    protected String grammars;

    /**
     * Grammar list presents in the <code>sourceDirectory</code> directory.
     * <br/>
     * See <a href="http://www.antlr.org/doc/options.html#Command%20Line%20Options">Command Line Options</a>
     * <br/>
     * Example:
     * <pre>
     * &lt;grammarDefs&gt;<br/>
     *   &lt;grammar&gt;<br/>
     *     &lt;name&gt;myGrammar.g&lt;/name&gt;<br/>
     *     &lt;glib&gt;mySuperGrammar.g;myOtherSuperGrammar.g&lt;/glib&gt;<br/>
     *   &lt;/grammar&gt;<br/>
     * &lt;/grammarDefs&gt;
     * </pre>
     *
     * @parameter expression="${grammarDefs}"
     */
    protected Grammar[] grammarDefs;

    /**
     * Launch the ParseView debugger upon parser invocation.
     * <br/>
     * See <a href="http://www.antlr.org/doc/options.html#Command%20Line%20Options">Command Line Options</a>
     *
     * @parameter expression="${debug}" default-value="false"
     */
    private boolean debug;

    /**
     * Generate a text file from your grammar with a lot of debugging info.
     * <br/>
     * See <a href="http://www.antlr.org/doc/options.html#Command%20Line%20Options">Command Line Options</a>
     *
     * @parameter expression="${diagnostic}" default-value="false"
     */
    private boolean diagnostic;

    /**
     * Have all rules call traceIn/traceOut.
     * <br/>
     * See <a href="http://www.antlr.org/doc/options.html#Command%20Line%20Options">Command Line Options</a>
     *
     * @parameter expression="${trace}" default-value="false"
     */
    private boolean trace;

    /**
     * Have parser rules call traceIn/traceOut.
     * <br/>
     * See <a href="http://www.antlr.org/doc/options.html#Command%20Line%20Options">Command Line Options</a>
     *
     * @parameter expression="${traceParser}" default-value="false"
     */
    private boolean traceParser;

    /**
     * Have lexer rules call traceIn/traceOut.
     * <br/>
     * See <a href="http://www.antlr.org/doc/options.html#Command%20Line%20Options">Command Line Options</a>
     *
     * @parameter expression="${traceLexer}" default-value="false"
     */
    private boolean traceLexer;

    /**
     * Have tree rules call traceIn/traceOut.
     * <br/>
     * See <a href="http://www.antlr.org/doc/options.html#Command%20Line%20Options">Command Line Options</a>
     *
     * @parameter expression="${traceTreeParser}" default-value="false"
     */
    private boolean traceTreeParser;

    /**
     * @throws MojoExecutionException
     */
    protected void executeAntlr()
        throws MojoExecutionException
    {
        validateParameters();

        // ----------------------------------------------------------------------
        // Call Antlr for each grammar
        // ----------------------------------------------------------------------

        Grammar[] grammarsUsed = getGrammars();

        for ( int i = 0; i < grammarsUsed.length; i++ )
        {
            String grammarName = grammarsUsed[i].getName();

            if ( StringUtils.isEmpty( grammarName ) )
            {
                getLog().info( "Empty grammar in the configuration. Skipped." );
                continue;
            }

            File grammar = new File( sourceDirectory, grammarName.trim() );

            if ( !grammar.exists() )
            {
                throw new MojoExecutionException( "The grammar '" + grammar.getAbsolutePath() + "' doesnt exist." );
            }

            getLog().info( "Using Antlr grammar: " + grammar );

            File generated = null;
            try
            {
                generated = getGeneratedFile( grammar.getPath(), outputDirectory );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to get generated file: " + e.getMessage(), e );
            }

            if ( generated.exists() )
            {
                if ( generated.lastModified() > grammar.lastModified() )
                {
                    // it's more recent, skip.
                    getLog().info( "The grammar is already generated." );
                    continue;
                }
            }

            if ( !generated.getParentFile().exists() )
            {
                generated.getParentFile().mkdirs();
            }

            // ----------------------------------------------------------------------
            // Wrap arguments
            // Note: grammar file should be last
            // ----------------------------------------------------------------------

            List arguments = new LinkedList();
            addArgIf( arguments, debug, "-debug" );
            addArgIf( arguments, diagnostic, "-diagnostic" );
            addArgIf( arguments, trace, "-trace" );
            addArgIf( arguments, traceParser, "-traceParser" );
            addArgIf( arguments, traceLexer, "-traceLexer" );
            addArgIf( arguments, traceTreeParser, "-traceTreeParser" );
            addArgs( arguments );
            arguments.add( "-o" );
            arguments.add( generated.getParentFile().getPath() );
            if ( StringUtils.isNotEmpty( grammarsUsed[i].getGlib() ) )
            {
                StringBuffer glib = new StringBuffer();
                StringTokenizer st = new StringTokenizer( grammarsUsed[i].getGlib(), ",; " );
                while ( st.hasMoreTokens() )
                {
                    glib.append( new File( sourceDirectory, st.nextToken().trim() ) );
                    if ( st.hasMoreTokens() )
                    {
                        glib.append( ";" );
                    }
                }
                arguments.add( "-glib" );
                arguments.add( glib.toString() );
            }
            arguments.add( grammar.getPath() );

            String[] args = (String[]) arguments.toArray( new String[0] );

            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "antlr args=\n" + StringUtils.join( args, "\n" ) );
            }

            // ----------------------------------------------------------------------
            // Call Antlr
            // ----------------------------------------------------------------------
            boolean failedSetManager = false;
            SecurityManager oldSm = null;
            try 
            {
                oldSm = System.getSecurityManager();
                System.setSecurityManager( NoExitSecurityManager.INSTANCE );
            } 
            catch (SecurityException ex) 
            {
                //ANTLR-12
                oldSm = null;
                failedSetManager = true;
                //ignore, in embedded environment the security manager can already be set.
                // in such a case assume the exit call is handled properly..
                getLog().warn("Cannot set custom SecurityManager. " +
                        "Antlr's call to System.exit() can cause application shutdown " +
                        "if not handled by the current SecurityManager.");
            }
            
            PrintStream oldErr = System.err;

            OutputStream errOS = new StringOutputStream();
            PrintStream err = new PrintStream( errOS );
            System.setErr( err );

            try
            {
                Tool.main( (String[]) arguments.toArray( new String[0] ) );
            }
            catch ( SecurityException e )
            {
                if ( e.getMessage().equals( "exitVM-0" ) ||
                     e.getClass().getName().equals("org.netbeans.core.execution.ExitSecurityException" ) )  //netbeans IDE Sec Manager.
                {
                    //ANTLR-12
                    //now basically every secutiry manager could set different message, how to handle in generic way?
                    //probably only by external execution
                    /// in case of NetBeans SecurityManager, it's not possible to distinguish exit codes, rather swallow than fail.
                    getLog().debug(e);
                } 
                else 
                {
                    throw new MojoExecutionException( "Antlr execution failed: " + e.getMessage() + 
                                                      "\n Error output:\n" + errOS, e );
                }
            }
            finally
            {
                if ( ! failedSetManager ) {
                    System.setSecurityManager( oldSm );
                }
                System.setErr( oldErr );

                System.err.println( errOS.toString() );
            }
        }

        if ( project != null )
        {
            projectHelper.addResource( project, outputDirectory.getAbsolutePath(), Collections
                .singletonList( "**/**.txt" ), new ArrayList() );
            project.addCompileSourceRoot( outputDirectory.getAbsolutePath() );
        }
    }

    /**
     * Add arguments to be included in Antlr call
     *
     * @param arguments
     */
    protected abstract void addArgs( List arguments );

    /**
     * Convenience method to add an argument
     *
     * @param arguments
     * @param b
     * @param value
     */
    protected static void addArgIf( List arguments, boolean b, String value )
    {
        if ( b )
        {
            arguments.add( value );
        }
    }

    /**
     * @param grammar
     * @param outputDir
     * @return generated file
     * @throws IOException
     */
    private File getGeneratedFile( String grammar, File outputDir )
        throws IOException
    {
        String generatedFileName = null;

        String packageName = "";

        BufferedReader in = new BufferedReader( new FileReader( grammar ) );

        String line;

        while ( ( line = in.readLine() ) != null )
        {
            line = line.trim();

            int extendsIndex = line.indexOf( " extends " );

            if ( line.startsWith( "class " ) && extendsIndex > -1 )
            {
                generatedFileName = line.substring( 6, extendsIndex ).trim();

                break;
            }
            else if ( line.startsWith( "package" ) )
            {
                packageName = line.substring( 8 ).trim();
            }
        }

        in.close();

        if ( generatedFileName == null )
        {
            return null;
        }

        File genFile = null;

        if ( "".equals( packageName ) )
        {
            genFile = new File( outputDir, generatedFileName + ".java" );
        }
        else
        {
            String packagePath = packageName.replace( '.', File.separatorChar );

            packagePath = packagePath.replace( ';', File.separatorChar );

            genFile = new File( new File( outputDir, packagePath ), generatedFileName + ".java" );
        }

        return genFile;
    }

    /**
     * grammars or grammarDefs parameters is required
     *
     * @throws MojoExecutionException
     */
    private void validateParameters()
        throws MojoExecutionException
    {
        if ( ( StringUtils.isEmpty( grammars ) ) && ( ( grammarDefs == null ) || ( grammarDefs.length == 0 ) ) )
        {
            StringBuffer msg = new StringBuffer();
            msg.append( "Antlr plugin parameters are invalid/missing." ).append( '\n' );
            msg.append( "Inside the definition for plugin 'maven-antlr-plugin' specify the following:" ).append( '\n' );
            msg.append( '\n' );
            msg.append( "<configuration>" ).append( '\n' );
            msg.append( "  <grammars>VALUE</grammars>" ).append( '\n' );
            msg.append( "- OR - " ).append( '\n' );
            msg.append( "  <grammarDefs>VALUE</grammarDefs>" ).append( '\n' );
            msg.append( "</configuration>" ).append( '\n' );

            throw new MojoExecutionException( msg.toString() );
        }
    }

    /**
     * @return an array of grammar from <code>grammars</code> and <code>grammarDefs</code> variables
     */
    private Grammar[] getGrammars()
    {
        List grammarList = new LinkedList();

        if ( StringUtils.isNotEmpty( grammars ) )
        {
            StringTokenizer st = new StringTokenizer( grammars, ", " );
            while ( st.hasMoreTokens() )
            {
                String currentGrammar = st.nextToken().trim();

                Grammar grammar = new Grammar();
                grammar.setName( currentGrammar );

                grammarList.add( grammar );
            }
        }

        if ( grammarDefs != null )
        {
            grammarList.addAll( Arrays.asList( grammarDefs ) );
        }

        return (Grammar[]) grammarList.toArray( new Grammar[0] );
    }
}
