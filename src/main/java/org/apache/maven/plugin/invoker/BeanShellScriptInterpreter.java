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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import bsh.EvalError;
import bsh.Interpreter;

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
    public Object evaluateScript( String script, List classPath, Map globalVariables, PrintStream scriptOutput )
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

            if ( classPath != null && !classPath.isEmpty() )
            {
                for ( Iterator it = classPath.iterator(); it.hasNext(); )
                {
                    String path = (String) it.next();
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
                for ( Iterator it = globalVariables.keySet().iterator(); it.hasNext(); )
                {
                    String variable = (String) it.next();
                    Object value = globalVariables.get( variable );
                    try
                    {
                        engine.set( variable, value );
                    }
                    catch ( EvalError e )
                    {
                        throw new ScriptEvaluationException( "Illegal global variable: " + variable + " = " + value,
                                                             e );
                    }
                }
            }

            try
            {
                return engine.eval( script );
            }
            catch ( EvalError e )
            {
                throw new ScriptEvaluationException( "script evaluation error", e );
            }
        }
        finally
        {
            System.setErr( origErr );
            System.setOut( origOut );
        }
    }

}
