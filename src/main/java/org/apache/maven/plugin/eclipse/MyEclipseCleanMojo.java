package org.apache.maven.plugin.eclipse;

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


import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Deletes configuration files used by MyEclipse
 *
 * @author Olivier Jacob
 * @since 2.5
 */
@Mojo( name = "myeclipse-clean", defaultPhase = LifecyclePhase.NONE )
public class MyEclipseCleanMojo
    extends EclipseCleanMojo
{
    /**
     * @throws MojoExecutionException
     */
    protected void cleanExtras()
        throws MojoExecutionException
    {
        delete( new File( getBasedir(), ".mymetadata" ) );
        delete( new File( getBasedir(), ".mystrutsdata" ) );
        delete( new File( getBasedir(), ".myhibernatedata" ) );
        delete( new File( getBasedir(), ".springBeans" ) );
    }
}
