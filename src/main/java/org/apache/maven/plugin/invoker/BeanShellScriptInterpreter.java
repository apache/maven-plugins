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
import java.util.List;
import java.util.Map;

import bsh.Capabilities;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.TargetError;

/**
 * Provides a facade to evaluate BeanShell scripts.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
class BeanShellScriptInterpreter
    implements ScriptInterpreter
{

    /**
     * {@inheritDoc}
     */
    public Object evaluateScript( String script, List<String> classPath, Map<String, ? extends Object> globalVariables, PrintStream scriptOutput )
        throws ScriptEvaluationException
    {
        PrintStream origOut = System.out;
        PrintStream origErr = System.err;

        try
        {
            Interpreter engine = new Interpreter();

            if ( scriptOutput != null )
            {
                System.setErr( scriptOutput );
                System.setOut( scriptOutput );
                engine.setErr( scriptOutput );
                engine.setOut( scriptOutput );
            }

            if ( !Capabilities.haveAccessibility() )
            {
                try
                {
                    Capabilities.setAccessibility( true );
                }
                catch ( Exception e )
                {
                    if ( scriptOutput != null )
                    {
                        e.printStackTrace( scriptOutput );
                    }
                }
            }

            if ( classPath != null && !classPath.isEmpty() )
            {
                for ( String path : classPath)
                {
                    try
                    {
                        engine.getClassManager().addClassPath( new File( path ).toURI().toURL() );
                    }
                    catch ( IOException e )
                    {
                        throw new RuntimeException( "bad class path: " + path, e );
                    }
                }
            }

            if ( globalVariables != null )
            {
                for ( String variable : globalVariables.keySet() )
                {
                    Object value = globalVariables.get( variable );
                    try
                    {
                        engine.set( variable, value );
                    }
                    catch ( EvalError e )
                    {
                        throw new RuntimeException( e );
                    }
                }
            }

            try
            {
                return engine.eval( script );
            }
            catch ( TargetError e )
            {
                throw new ScriptEvaluationException( e.getTarget() );
            }
            catch ( ThreadDeath e )
            {
                throw e;
            }
            catch ( Throwable e )
            {
                throw new ScriptEvaluationException( e );
            }
        }
        finally
        {
            System.setErr( origErr );
            System.setOut( origOut );
        }
    }

}
