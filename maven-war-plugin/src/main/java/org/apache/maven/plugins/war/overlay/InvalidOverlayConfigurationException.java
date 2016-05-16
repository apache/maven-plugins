package org.apache.maven.plugins.war.overlay;

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

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Thrown if the overlay configuration is invalid.
 *
 * @author Stephane Nicoll
 * @version $Id$
 */
public class InvalidOverlayConfigurationException
    extends MojoExecutionException
{

    /**
     * 
     */
    private static final long serialVersionUID = -9048144470408031414L;

    /**
     * @param string Set the message of the exception.
     */
    public InvalidOverlayConfigurationException( String string )
    {
        super( string );
    }

    /**
     * @param string Set the message of the exception.
     * @param throwable {@link Throwable}
     */
    public InvalidOverlayConfigurationException( String string, Throwable throwable )
    {
        super( string, throwable );
    }
}
