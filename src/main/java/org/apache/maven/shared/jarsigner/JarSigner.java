package org.apache.maven.shared.jarsigner;

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
 * Provides a facade to invoke JarSigner tool.
 *
 * @author tchemit <chemit@codelutin.com>
 * @version $Id$
 * @since 1.0
 */
public interface JarSigner
{

    /**
     * Executes JarSigner tool using the parameters specified by the given invocation request.
     *
     * @param request The invocation request to execute, must not be <code>null</code>.
     * @return The result of the JarSigner invocation, never <code>null</code>.
     * @throws JarSignerException if something fails while init the command
     */
    JarSignerResult execute( JarSignerRequest request )
        throws JarSignerException;

}
