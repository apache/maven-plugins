package org.apache.maven.plugin.javadoc;

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
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Fix Javadoc documentation and tags for the <code>Test Java code</code> for the project.
 * See <a href="http://java.sun.com/j2se/1.5.0/docs/tooldocs/windows/javadoc.html#wheretags">Where Tags Can Be Used</a>.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.6
 * @goal test-fix
 * @requiresDependencyResolution test
 * @execute phase="test-compile"
 */
public class TestFixJavadocMojo
    extends AbstractFixJavadocMojo
{
    /**
     * The classes of this project to compare the last release against.
     * Used by {@link AbstractFixJavadocMojo.ClirrMojoWrapper} class.
     *
     * @parameter default-value="${project.build.testOutputDirectory}
     */
    private File classesDirectory;

    /** {@inheritDoc} */
    protected File getClassesDirectory()
    {
        return classesDirectory;
    }

    /** {@inheritDoc} */
    protected List getProjectSourceRoots( MavenProject p )
    {
        if ( "pom".equals( p.getPackaging().toLowerCase() ) )
        {
            if ( getLog().isWarnEnabled() )
            {
                getLog().warn( "This project has 'pom' packaging, no test Java sources will be available." );
            }
            return Collections.EMPTY_LIST;
        }

        return p.getTestCompileSourceRoots();
    }

    /** {@inheritDoc} */
    protected List getCompileClasspathElements( MavenProject p )
        throws DependencyResolutionRequiredException
    {
        return p.getTestClasspathElements();
    }

    /** {@inheritDoc} */
    protected String getArtifactType( MavenProject p )
    {
        return "test-jar";
    }

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        // clirr doesn't analyze test code, so ignore it
        ignoreClirr = true;

        super.execute();
    }
}
