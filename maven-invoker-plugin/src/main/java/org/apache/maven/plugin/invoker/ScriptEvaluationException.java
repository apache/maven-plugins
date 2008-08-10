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

/**
 * Signals an error during parsing/evaluation of a script. This can either be a syntax error in the script itself or an
 * exception triggered by the methods it invoked.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
class ScriptEvaluationException
    extends Exception
{

    /**
     * The serial version identifier for this class.
     */
    private static final long serialVersionUID = 199336743291078393L;

    /**
     * Creates a new exception with the specified cause.
     * 
     * @param cause The cause, may be <code>null</code>.
     */
    public ScriptEvaluationException( Throwable cause )
    {
        super( cause );
    }

}
