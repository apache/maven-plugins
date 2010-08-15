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

import java.util.Collections;
import java.util.LinkedList;
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
    /** {@inheritDoc} */
    protected List<String> getProjectSourceRoots( MavenProject p )
    {
        return ( p.getTestCompileSourceRoots() == null ? Collections.EMPTY_LIST
                        : new LinkedList<String>( p.getTestCompileSourceRoots() ) );
    }

    /** {@inheritDoc} */
    protected List<String> getCompileClasspathElements( MavenProject p )
        throws DependencyResolutionRequiredException
    {
        return ( p.getTestClasspathElements() == null ? Collections.EMPTY_LIST
                        : new LinkedList<String>( p.getTestClasspathElements() ) );
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
