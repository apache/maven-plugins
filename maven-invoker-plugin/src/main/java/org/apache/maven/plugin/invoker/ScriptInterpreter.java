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

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * Defines a simple abstraction used to plug-in several script interpreters for the pre-/post-build-hooks. Each
 * interpretator implementation should be stateless and support reuse.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
interface ScriptInterpreter
{

    /**
     * Evaluates the specified script.
     * 
     * @param script The script contents to evalute, must not be <code>null</code>.
     * @param classPath The additional class path for the script interpreter, may be <code>null</code> or empty if only
     *            the plugin realm should be used for the script evaluation. If specified, this class path will precede
     *            the artifacts from the plugin class path.
     * @param globalVariables The global variables (as a mapping from variable name to value) to define for the script,
     *            may be <code>null</code> if not used.
     * @param scriptOutput A print stream to redirect any output from the script to, may be <code>null</code> to use
     *            stdout/stderr.
     * @return The return value from the script, can be <code>null</code>
     * @throws ScriptEvaluationException If the script evaluation produced an error.
     */
    Object evaluateScript( String script, List<String> classPath, Map<String, ? extends Object> globalVariables, PrintStream scriptOutput )
        throws ScriptEvaluationException;

}
