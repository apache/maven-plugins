package org.apache.maven.plugins.scripting;

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

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Evaluate the specified script
 * 
 * @author Robert Scholte
 * @since 3.0.0
 */
@Mojo( name = "eval" )
public class EvalMojo
    extends AbstractMojo
{
    @Parameter( required = true )
    private String engineName; // or map extension to engineName??

    /**
     * When used, also specify the engineName
     */
    @Parameter
    private String script;

    // script variables
    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;
    
    private ScriptEngineManager manager = new ScriptEngineManager();

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        ScriptEngine engine = null;
        
        if ( script != null )
        {
            engine = getScriptEngine( engineName );

            if ( engine == null )
            {
                throw new MojoFailureException( "Missing scriptEngine" );
            }
        }
        else
        {
            // from file
        }

        try
        {
            ScriptContext context = engine.getContext();
            context.setAttribute( "project", project, ScriptContext.GLOBAL_SCOPE );
            
            Object result = engine.eval( script );
            
            getLog().info( "Result:" );
            if ( result != null )
            {
                getLog().info( result.toString() );
            }
        }
        catch ( ScriptException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }
    
    private ScriptEngine getScriptEngine( String name )
    {
        if ( name == null ) 
        {
            return null;
        }
        else
        {
            return manager.getEngineByName( engineName );
        }
    }
}
