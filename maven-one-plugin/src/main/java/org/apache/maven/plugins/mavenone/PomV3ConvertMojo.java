package org.apache.maven.plugins.mavenone;

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

import org.apache.maven.model.converter.Maven1Converter;
import org.apache.maven.model.converter.ProjectConverterException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * Convert a Maven 1 project.xml (v3 pom) to a Maven 2 pom.xml (v4 pom).
 *
 * @author Fabrizio Giustina
 * @author Dennis Lundberg
 * @version $Id$
 * @goal convert
 * @requiresProject false
 * @since 1.1
 */
public class PomV3ConvertMojo extends AbstractMojo
{
    /**
     * Project basedir.
     *
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    private File basedir;

    /**
     * The converter to use.
     *
     * @component
     */
    private Maven1Converter converter;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        converter.setBasedir( basedir );

        try
        {
            converter.execute();
        }
        catch ( ProjectConverterException e )
        {
            throw new MojoExecutionException( "An exception occured while converting the Maven 1 project", e );
        }
    }
}
