package org.apache.maven.plugins.ear;

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
 * The base exception of the EAR plugin.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id: EarPluginException.java 1645331 2014-12-13 17:31:09Z khmarbaise $
 */
public class EarPluginException
    extends Exception
{

    /**
     * 
     */
    private static final long serialVersionUID = -5540929953103327928L;

    /**
     * Create an instance.
     */
    public EarPluginException()
    {
    }

    /**
     * @param message The message for the exception.
     */
    public EarPluginException( String message )
    {
        super( message );
    }

    /**
     * @param cause {@link Throwable}
     */
    public EarPluginException( Throwable cause )
    {
        super( cause );
    }

    /**
     * @param message The message to emit.
     * @param cause {@link Throwable}
     */
    public EarPluginException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
