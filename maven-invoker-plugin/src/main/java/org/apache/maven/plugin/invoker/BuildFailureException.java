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
 * Signals a failure of a sub build run by the Invoker Plugin. This can be caused by an unsuccessful pre-/post-build
 * script or a failure of the forked Maven build itself.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
class BuildFailureException
    extends Exception
{

    /**
     * The serial version identifier for this class.
     */
    private static final long serialVersionUID = 236131530635863815L;

    /**
     * The type of the build failure.
     */
    private final String type;

    /**
     * Creates a new exception with the specified detail message.
     *
     * @param message The detail message, may be <code>null</code>.
     * @param type The type of build failure, may not be <code>null</code>.
     */
    public BuildFailureException( String message, String type )
    {
        super( message );
        if ( type == null )
        {
            throw new IllegalArgumentException( "missing failure type" );
        }
        this.type = type;
    }

    /**
     * Returns the type of build failure.
     * 
     * @return The type of build failure, never <code>null</code>.
     */
    public String getType()
    {
        return type;
    }

}
