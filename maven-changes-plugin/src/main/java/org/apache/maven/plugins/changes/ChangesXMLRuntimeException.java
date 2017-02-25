package org.apache.maven.plugins.changes;

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

/**
 * This is a runtime exception class that is thrown by the
 * {@link ChangesXML#ChangesXML(java.io.File, org.apache.maven.plugin.logging.Log)} constructor if the given
 * changes.xml file cannot be parsed, for example it is not well-formed or valid.
 *
 * @author <a href="mailto:szgabsz91@gmail.com">Gabor Szabo</a>
 */
public class ChangesXMLRuntimeException
    extends RuntimeException
{
    /** The serialVersionUID **/
    private static final long serialVersionUID = -8059557047280992301L;

    /**
     * Default constructor that sets the message.
     *
     * @param msg the exception message.
     */
    public ChangesXMLRuntimeException( String msg )
    {
        super( msg );
    }

    /**
     * Constructor that sets the message and the cause of the exception.
     *
     * @param msg the exception message.
     * @param cause the cause.
     */
    public ChangesXMLRuntimeException( String msg, Throwable cause )
    {
        super( msg, cause );
    }
}
