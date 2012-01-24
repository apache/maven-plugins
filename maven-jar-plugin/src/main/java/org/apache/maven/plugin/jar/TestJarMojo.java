package org.apache.maven.plugin.jar;

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

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Build a JAR of the test classes for the current project.
 *
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 * @goal test-jar
 * @phase package
 * @requiresProject
 * @threadSafe
 * @requiresDependencyResolution test
 */
public class TestJarMojo
    extends AbstractJarMojo
{

    /**
     * Set this to <code>true</code> to bypass unit tests entirely.
     * Its use is <b>NOT RECOMMENDED</b>, but quite convenient on occasion.
     *
     * @parameter expression="${maven.test.skip}"
     */
    private boolean skip;

    /**
     * Directory containing the test classes and resource files that should be packaged into the JAR.
     *
     * @parameter default-value="${project.build.testOutputDirectory}"
     * @required
     */
    private File testClassesDirectory;

    protected String getClassifier()
    {
        return "tests";
    }

    /**
     * @return type of the generated artifact
     */
    protected String getType()
    {
        return "test-jar";
    }

    /**
     * Return the test-classes directory, to serve as the root of the tests jar.
     */
    protected File getClassesDirectory()
    {
        return testClassesDirectory;
    }

	public void execute()
        throws MojoExecutionException
    {
        if ( skip )
        {
            getLog().info( "Skipping packaging of the test-jar" );
        }
        else
        {
            super.execute();
        }
    }
}
