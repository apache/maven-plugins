package org.apache.maven.plugin;

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

import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compiles application test sources.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @since 2.0
 * @goal testCompile
 * @phase test-compile
 * @requiresDependencyResolution test
 */
public class TestCompilerMojo
    extends AbstractCompilerMojo
{
    /**
     * Set this to 'true' to bypass unit tests entirely.
     * Its use is NOT RECOMMENDED, but quite convenient on occasion.
     *
     * @parameter expression="${maven.test.skip}"
     */
    private boolean skip;

    /**
     * The source directories containing the test-source to be compiled.
     *
     * @parameter expression="${project.testCompileSourceRoots}"
     * @required
     * @readonly
     */
    private List compileSourceRoots;

    /**
     * Project test classpath.
     *
     * @parameter expression="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    private List classpathElements;

    /**
     * The directory where compiled test classes go.
     *
     * @parameter expression="${project.build.testOutputDirectory}"
     * @required
     * @readonly
     */
    private File outputDirectory;

    /**
     * A list of inclusion filters for the compiler.
     *
     * @parameter
     */
    private Set testIncludes = new HashSet();

    /**
     * A list of exclusion filters for the compiler.
     *
     * @parameter
     */
    private Set testExcludes = new HashSet();

    /**
     * The -source argument for the test Java compiler.
     *
     * @parameter expression="${maven.compiler.testSource}"
     */
    private String testSource;

    /**
     * The -target argument for the test Java compiler.
     *
     * @parameter expression="${maven.compiler.testTarget}"
     */
    private String testTarget;


    /**
     * <p>
     * Sets the arguments to be passed to test compiler (prepending a dash) if fork is set to true.
     * </p>
     * <p>
     * This is because the list of valid arguments passed to a Java compiler
     * varies based on the compiler version.
     * </p>
     *
     * @parameter
     * @since 2.0.1
     */
    private Map testCompilerArguments;

    /**
     * <p>
     * Sets the unformatted argument string to be passed to test compiler if fork is set to true.
     * </p>
     * <p>
     * This is because the list of valid arguments passed to a Java compiler
     * varies based on the compiler version.
     * </p>
     *
     * @parameter
     */
    private String testCompilerArgument;

    public void execute()
        throws MojoExecutionException, CompilationFailureException
    {
        if ( skip )
        {
            getLog().info( "Not compiling test sources" );
        }
        else
        {
            super.execute();
        }
    }

    protected List getCompileSourceRoots()
    {
        return compileSourceRoots;
    }

    protected List getClasspathElements()
    {
        return classpathElements;
    }

    protected File getOutputDirectory()
    {
        return outputDirectory;
    }

    protected SourceInclusionScanner getSourceInclusionScanner( int staleMillis )
    {
        SourceInclusionScanner scanner = null;

        if ( testIncludes.isEmpty() && testExcludes.isEmpty() )
        {
            scanner = new StaleSourceScanner( staleMillis );
        }
        else
        {
            if ( testIncludes.isEmpty() )
            {
                testIncludes.add( "**/*.java" );
            }
            scanner = new StaleSourceScanner( staleMillis, testIncludes, testExcludes );
        }

        return scanner;
    }

    protected SourceInclusionScanner getSourceInclusionScanner( String inputFileEnding )
    {
        SourceInclusionScanner scanner = null;

        if ( testIncludes.isEmpty() && testExcludes.isEmpty() )
        {
            testIncludes = Collections.singleton( "**/*." + inputFileEnding );
            scanner = new SimpleSourceInclusionScanner( testIncludes, Collections.EMPTY_SET );
        }
        else
        {
            if ( testIncludes.isEmpty() )
            {
                testIncludes.add( "**/*." + inputFileEnding );
            }
            scanner = new SimpleSourceInclusionScanner( testIncludes, testExcludes );
        }

        return scanner;
    }

    protected String getSource()
    {
      return testSource == null ? source : testSource;
    }

    protected String getTarget()
    {
      return testTarget == null ? target : testTarget;
    }

    protected String getCompilerArgument()
    {
      return testCompilerArgument == null ? compilerArgument : testCompilerArgument;
    }

    protected Map getCompilerArguments()
    {
      return testCompilerArguments == null ? compilerArguments : testCompilerArguments;
    }

}
