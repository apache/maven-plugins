package org.apache.maven.plugin.antlr;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
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
     * @required
     */
    protected String grammars;

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
        // ----------------------------------------------------------------------
        // Call Antlr for each grammar
        // ----------------------------------------------------------------------

        StringTokenizer st = new StringTokenizer( grammars, ", " );

        while ( st.hasMoreTokens() )
        {
            String eachGrammar = st.nextToken().trim();

            File grammar = new File( sourceDirectory, eachGrammar );

            getLog().info( "grammar: " + grammar );

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
                    getLog().info( "The grammar is already generated" );
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
            arguments.add( grammar.getPath() );

            String[] args = (String[]) arguments.toArray( new String[0] );

            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "antlr args=\n" + StringUtils.join( args, "\n" ) );
            }

            SecurityManager oldSm = System.getSecurityManager();

            System.setSecurityManager( NoExitSecurityManager.INSTANCE );

            // ----------------------------------------------------------------------
            // Call Antlr
            // ----------------------------------------------------------------------

            try
            {
                Tool.main( (String[]) arguments.toArray( new String[0] ) );
            }
            catch ( SecurityException e )
            {
                if ( !e.getMessage().equals( "exitVM-0" ) )
                {
                    throw new MojoExecutionException( "Execution failed", e );
                }
            }
            finally
            {
                System.setSecurityManager( oldSm );
            }
        }

        if ( project != null )
        {
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
}
