package org.apache.maven.plugin.invoker;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.scriptinterpreter.BeanShellScriptInterpreter;
import org.apache.maven.shared.scriptinterpreter.GroovyScriptInterpreter;
import org.apache.maven.shared.scriptinterpreter.ScriptEvaluationException;
import org.apache.maven.shared.scriptinterpreter.ScriptInterpreter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Runs pre-/post-build hook scripts.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
class ScriptRunner
{

    /**
     * The mojo logger to print diagnostic to, never <code>null</code>.
     */
    private Log log;

    /**
     * The supported script interpreters, indexed by the lower-case file extension of their associated script files,
     * never <code>null</code>.
     */
    private Map<String, ScriptInterpreter> scriptInterpreters;

    /**
     * The common set of global variables to pass into the script interpreter, never <code>null</code>.
     */
    private Map<String, Object> globalVariables;

    /**
     * The additional class path for the script interpreter, never <code>null</code>.
     */
    private List<String> classPath;

    /**
     * The file encoding of the hook scripts or <code>null</code> to use platform encoding.
     */
    private String encoding;

    /**
     * Creates a new script runner.
     * 
     * @param log The mojo logger to print diagnostic to, must not be <code>null</code>.
     */
    public ScriptRunner( Log log )
    {
        if ( log == null )
        {
            throw new IllegalArgumentException( "missing logger" );
        }
        this.log = log;
        scriptInterpreters = new LinkedHashMap<String, ScriptInterpreter>();
        scriptInterpreters.put( "bsh", new BeanShellScriptInterpreter() );
        scriptInterpreters.put( "groovy", new GroovyScriptInterpreter() );
        globalVariables = new HashMap<String, Object>();
        classPath = new ArrayList<String>();
    }

    /**
     * Gets the mojo logger.
     * 
     * @return The mojo logger, never <code>null</code>.
     */
    private Log getLog()
    {
        return log;
    }

    /**
     * Sets a global variable for the script interpeter.
     * 
     * @param name The name of the variable, must not be <code>null</code>.
     * @param value The value of the variable, may be <code>null</code>.
     */
    public void setGlobalVariable( String name, Object value )
    {
        this.globalVariables.put( name, value );
    }

    /**
     * Sets the additional class path for the hook scripts. Note that the provided list is copied, so any later changes
     * will not affect the scripts.
     * 
     * @param classPath The additional class path for the script interpreter, may be <code>null</code> or empty if only
     *            the plugin realm should be used for the script evaluation. If specified, this class path will precede
     *            the artifacts from the plugin class path.
     */
    public void setClassPath( List<String> classPath )
    {
        this.classPath = ( classPath != null ) ? new ArrayList<String>( classPath ) : new ArrayList<String>();
    }

    /**
     * Sets the file encoding of the hook scripts.
     * 
     * @param encoding The file encoding of the hook scripts, may be <code>null</code> or empty to use the platform's
     *            default encoding.
     */
    public void setScriptEncoding( String encoding )
    {
        this.encoding = StringUtils.isNotEmpty( encoding ) ? encoding : null;
    }

    /**
     * Runs the specified hook script of the specified project (if any).
     * 
     * @param scriptDescription The description of the script to use for logging, must not be <code>null</code>.
     * @param basedir The base directory of the project, must not be <code>null</code>.
     * @param relativeScriptPath The path to the script relative to the project base directory, may be <code>null</code>
     *            to skip the script execution.
     * @param context The key-value storage used to share information between hook scripts, may be <code>null</code>.
     * @param logger The logger to redirect the script output to, may be <code>null</code> to use stdout/stderr.
     * @param stage The stage of the build job the script is invoked in, must not be <code>null</code>.
     * @param failOnException If <code>true</code> and the script throws an exception, then a {@link BuildFailureException}
     *            will be thrown, otherwise a {@link BuildErrorException} will be thrown on script exception.
     * @throws MojoExecutionException If an I/O error occurred while reading the script file.
     * @throws BuildFailureException If the script did not return <code>true</code> of threw an exception.
     */
    public void run( final String scriptDescription, final File basedir, final String relativeScriptPath,
                     final Map<String, ? extends Object> context, final FileLogger logger, String stage, boolean failOnException )
        throws MojoExecutionException, BuildFailureException
    {
        if ( relativeScriptPath == null )
        {
            return;
        }

        final File scriptFile = resolveScript( new File( basedir, relativeScriptPath ) );

        if ( !scriptFile.exists() )
        {
            return;
        }

        Map<String, Object> globalVariables = new HashMap<String, Object>( this.globalVariables );
        globalVariables.put( "basedir", basedir );
        globalVariables.put( "context", context );

        PrintStream out = ( logger != null ) ? logger.getPrintStream() : null;

        ScriptInterpreter interpreter = getInterpreter( scriptFile );
        if ( getLog().isDebugEnabled() )
        {
            String name = interpreter.getClass().getName();
            name = name.substring( name.lastIndexOf( '.' ) + 1 );
            getLog().debug( "Running script with " + name + ": " + scriptFile );
        }

        String script;
        try
        {
            script = FileUtils.fileRead( scriptFile, encoding );
        }
        catch ( IOException e )
        {
            String errorMessage =
                "error reading " + scriptDescription + " " + scriptFile.getPath() + ", " + e.getMessage();
            throw new MojoExecutionException( errorMessage, e );
        }

        Object result;
        try
        {
            if ( logger != null )
            {
                logger.consumeLine( "Running " + scriptDescription + ": " + scriptFile );
            }
            result = interpreter.evaluateScript( script, classPath, globalVariables, out );
            if ( logger != null )
            {
                logger.consumeLine( "Finished " + scriptDescription + ": " + scriptFile );
            }
        }
        catch ( ScriptEvaluationException e )
        {
            Throwable t = ( e.getCause() != null ) ? e.getCause() : e;
            String msg = ( t.getMessage() != null ) ? t.getMessage() : t.toString();
            if ( getLog().isDebugEnabled() )
            {
                String errorMessage = "Error evaluating " + scriptDescription + " " + scriptFile.getPath() + ", " + t;
                getLog().debug( errorMessage, t );
            }
            if ( logger != null )
            {
                t.printStackTrace( logger.getPrintStream() );
            }
            if ( failOnException )
            {
                throw new BuildFailureException( "The " + scriptDescription + " did not succeed. " + msg, stage );
            }
            else
            {
                throw new BuildErrorException( "The " + scriptDescription + " did not succeed. " + msg, stage, t );
            }
        }

        if ( !( result == null || Boolean.TRUE.equals( result ) || "true".equals( result ) ) )
        {
            throw new BuildFailureException( "The " + scriptDescription + " returned " + result + ".", stage );
        }
    }

    /**
     * Gets the effective path to the specified script. For convenience, we allow to specify a script path as "verify"
     * and have the plugin auto-append the file extension to search for "verify.bsh" and "verify.groovy".
     * 
     * @param scriptFile The script file to resolve, may be <code>null</code>.
     * @return The effective path to the script file or <code>null</code> if the input was <code>null</code>.
     */
    private File resolveScript( File scriptFile )
    {
        if ( scriptFile != null && !scriptFile.exists() )
        {
            for ( String ext : this.scriptInterpreters.keySet() )
            {
                File candidateFile = new File( scriptFile.getPath() + '.' + ext );
                if ( candidateFile.exists() )
                {
                    scriptFile = candidateFile;
                    break;
                }
            }
        }
        return scriptFile;
    }

    /**
     * Determines the script interpreter for the specified script file by looking at its file extension. In this
     * context, file extensions are considered case-insensitive. For backward compatibility with plugin versions 1.2-,
     * the BeanShell interpreter will be used for any unrecognized extension.
     * 
     * @param scriptFile The script file for which to determine an interpreter, must not be <code>null</code>.
     * @return The script interpreter for the file, never <code>null</code>.
     */
    private ScriptInterpreter getInterpreter( File scriptFile )
    {
        String ext = FileUtils.extension( scriptFile.getName() ).toLowerCase( Locale.ENGLISH );
        ScriptInterpreter interpreter = scriptInterpreters.get( ext );
        if ( interpreter == null )
        {
            interpreter = scriptInterpreters.get( "bsh" );
        }
        return interpreter;
    }

}
