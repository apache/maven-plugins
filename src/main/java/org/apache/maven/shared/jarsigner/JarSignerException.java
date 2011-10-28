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
 * Signals an error during the construction of the command line used to invoke jar signer, e.g. illegal invocation arguments.
 * This should not be confused with a failure of the invoked JarSigner build itself which will be reported by means of a
 * non-zero exit code.
 *
 * @author tchemit <chemit@codelutin.com>
 * @version $Id$
 * @see JarSignerResult#getExitCode()
 * @since 1.0
 */
public class JarSignerException
    extends Exception
{

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exception using the specified detail message and cause.
     *
     * @param message The detail message for this exception, may be <code>null</code>.
     * @param cause   The nested exception, may be <code>null</code>.
     */
    public JarSignerException( String message, Throwable cause )
    {
        super( message, cause );
    }

    /**
     * Creates a new exception using the specified detail message.
     *
     * @param message The detail message for this exception, may be <code>null</code>.
     */
    public JarSignerException( String message )
    {
        super( message );
    }

}
