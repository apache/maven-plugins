package org.apache.maven.plugin.compiler;

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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.java.DefaultJavaToolChain;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsRequest;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsResult;

/**
 * Compiles application test sources.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "testCompile", defaultPhase = LifecyclePhase.TEST_COMPILE, threadSafe = true,
                requiresDependencyResolution = ResolutionScope.TEST )
public class TestCompilerMojo
    extends AbstractCompilerMojo
{
    /**
     * Set this to 'true' to bypass compilation of test sources.
     * Its use is NOT RECOMMENDED, but quite convenient on occasion.
     */
    @Parameter ( property = "maven.test.skip" )
    private boolean skip;

    /**
     * The source directories containing the test-source to be compiled.
     */
    @Parameter ( defaultValue = "${project.testCompileSourceRoots}", readonly = true, required = true )
    private List<String> compileSourceRoots;

    /**
     * The directory where compiled test classes go.
     */
    @Parameter ( defaultValue = "${project.build.testOutputDirectory}", required = true, readonly = true )
    private File outputDirectory;

    /**
     * A list of inclusion filters for the compiler.
     */
    @Parameter
    private Set<String> testIncludes = new HashSet<String>();

    /**
     * A list of exclusion filters for the compiler.
     */
    @Parameter
    private Set<String> testExcludes = new HashSet<String>();

    /**
     * The -source argument for the test Java compiler.
     *
     * @since 2.1
     */
    @Parameter ( property = "maven.compiler.testSource" )
    private String testSource;

    /**
     * The -target argument for the test Java compiler.
     *
     * @since 2.1
     */
    @Parameter ( property = "maven.compiler.testTarget" )
    private String testTarget;

    /**
     * the -release argument for the test Java compiler
     * 
     * @since 3.6
     */
    @Parameter ( property = "maven.compiler.testRelease" )
    private String testRelease;

    /**
     * <p>
     * Sets the arguments to be passed to test compiler (prepending a dash) if fork is set to true.
     * </p>
     * <p>
     * This is because the list of valid arguments passed to a Java compiler
     * varies based on the compiler version.
     * </p>
     *
     * @since 2.1
     */
    @Parameter
    private Map<String, String> testCompilerArguments;

    /**
     * <p>
     * Sets the unformatted argument string to be passed to test compiler if fork is set to true.
     * </p>
     * <p>
     * This is because the list of valid arguments passed to a Java compiler
     * varies based on the compiler version.
     * </p>
     *
     * @since 2.1
     */
    @Parameter
    private String testCompilerArgument;

    /**
     * <p>
     * Specify where to place generated source files created by annotation processing.
     * Only applies to JDK 1.6+
     * </p>
     *
     * @since 2.2
     */
    @Parameter ( defaultValue = "${project.build.directory}/generated-test-sources/test-annotations" )
    private File generatedTestSourcesDirectory;

    @Parameter( defaultValue = "${project.compileClasspathElements}", readonly = true )
    private List<String> compilePath;

    @Parameter( defaultValue = "${project.testClasspathElements}", readonly = true )
    private List<String> testPath;

    private LocationManager locationManager = new LocationManager();

    private Map<String, JavaModuleDescriptor> pathElements;
    
    private Collection<String> classpathElements;

    private Collection<String> modulepathElements;

    public void execute()
        throws MojoExecutionException, CompilationFailureException
    {
        if ( skip )
        {
            getLog().info( "Not compiling test sources" );
            return;
        }
        super.execute();
    }

    protected List<String> getCompileSourceRoots()
    {
        return compileSourceRoots;
    }

    @Override
    protected Map<String, JavaModuleDescriptor> getPathElements()
    {
        return pathElements;
    }

    protected List<String> getClasspathElements()
    {
        return new ArrayList<>( classpathElements );
    }

    @Override
    protected List<String> getModulepathElements()
    {
        return new ArrayList<>( modulepathElements );
    }

    protected File getOutputDirectory()
    {
        return outputDirectory;
    }

    @Override
    protected void preparePaths( Set<File> sourceFiles )
    {
        File mainOutputDirectory = new File( getProject().getBuild().getOutputDirectory() );
        
        File mainModuleDescriptorClassFile = new File( mainOutputDirectory, "module-info.class" );
        JavaModuleDescriptor mainModuleDescriptor = null;

        File testModuleDescriptorJavaFile = new File( "module-info.java" );
        ModuleInfo testModuleInfo = null;

        // Go through the source files to respect includes/excludes 
        for ( File sourceFile : sourceFiles )
        {
            // @todo verify if it is the root of a sourcedirectory?
            if ( "module-info.java".equals( sourceFile.getName() ) ) 
            {
                testModuleDescriptorJavaFile = sourceFile;
                break;
            }
        }

        // Get additional information from the main module descriptors, if available
        if ( mainModuleDescriptorClassFile.exists() )
        {
            ResolvePathsResult<String> result;

            try
            {
                ResolvePathsRequest<String> request =
                        ResolvePathsRequest.withStrings( testPath )
                                .setMainModuleDescriptor( mainModuleDescriptorClassFile.getAbsolutePath() );

                Toolchain toolchain = getToolchain();
                if ( toolchain != null && toolchain instanceof DefaultJavaToolChain )
                {
                    request.setJdkHome( ( (DefaultJavaToolChain) toolchain ).getJavaHome() );
                }

                result = locationManager.resolvePaths( request );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }

            mainModuleDescriptor = result.getMainModuleDescriptor();

            pathElements = new LinkedHashMap<String, JavaModuleDescriptor>( result.getPathElements().size() );
            pathElements.putAll( result.getPathElements() );

            modulepathElements = result.getModulepathElements().keySet();
            classpathElements = result.getClasspathElements();
        }

        // Get additional information from the test module descriptors, if available
        if ( testModuleDescriptorJavaFile.exists() )
        {
            testModuleInfo = ModuleInfo.of( testModuleDescriptorJavaFile.toPath() );
        }
        
        if ( release != null )
        {
            if ( Integer.valueOf( release ) < 9 )
            {
                pathElements = Collections.emptyMap();
                modulepathElements = Collections.emptyList();
                classpathElements = testPath;
                return;
            }
        }
        else if ( Double.valueOf( getTarget() ) < Double.valueOf( MODULE_INFO_TARGET ) )
        {
            pathElements = Collections.emptyMap();
            modulepathElements = Collections.emptyList();
            classpathElements = testPath;
            return;
        }
            
        if ( testModuleInfo != null )
        {
            modulepathElements = testPath;
            classpathElements = Collections.emptyList();

            if ( mainModuleDescriptor != null )
            {
                System.out.println( "test and main module descriptor exist" );

                System.out.println( "main = " + mainModuleDescriptor.name() + " - " + mainModuleDescriptor );
                System.out.println( "test = " + testModuleInfo.name() + " - " + testModuleInfo );

                if ( testModuleInfo.name().equals( mainModuleDescriptor.name() ) )
                {
                    if ( compilerArgs == null )
                    {
                        compilerArgs = new ArrayList<>();
                    }
                    compilerArgs.add( "--patch-module" );

                    StringBuilder patchModuleValue = new StringBuilder( testModuleInfo.name() )
                            .append( '=' )
                            .append( mainOutputDirectory )
                            .append( PS );
                    for ( String root : compileSourceRoots )
                    {
                        patchModuleValue.append( root ).append( PS );
                    }

                    compilerArgs.add( patchModuleValue.toString() );
                }
                else
                {
                    // Black-box testing - all is ready to compile.
                }

            }
            else
            {
                // very odd
                // Means that main sources must be compiled with -modulesource and -Xmodule:<moduleName>
                // However, this has a huge impact since you can't simply use it as a classpathEntry 
                // due to extra folder in between
                throw new UnsupportedOperationException( "Can't compile test sources "
                    + "when main sources are missing a module descriptor" );
            }
        }
        else
        {
            if ( mainModuleDescriptor != null )
            {

                if ( compilerArgs == null )
                {
                    compilerArgs = new ArrayList<String>();
                }
                compilerArgs.add( "--patch-module" );
                
                StringBuilder patchModuleValue = new StringBuilder( mainModuleDescriptor.name() )
                                .append( '=' )
                                .append( mainOutputDirectory )
                                .append( PS );
                for ( String root : compileSourceRoots )
                {
                    patchModuleValue.append( root ).append( PS );
                }
                
                compilerArgs.add( patchModuleValue.toString() );
                
                compilerArgs.add( "--add-reads" );
                compilerArgs.add( mainModuleDescriptor.name() + "=ALL-UNNAMED" );
            }
            else
            {
                modulepathElements = Collections.emptyList();
                classpathElements = testPath;
            }
        }
    }

    protected SourceInclusionScanner getSourceInclusionScanner( int staleMillis )
    {
        SourceInclusionScanner scanner;

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
        SourceInclusionScanner scanner;

        // it's not defined if we get the ending with or without the dot '.'
        String defaultIncludePattern = "**/*" + ( inputFileEnding.startsWith( "." ) ? "" : "." ) + inputFileEnding;

        if ( testIncludes.isEmpty() && testExcludes.isEmpty() )
        {
            testIncludes = Collections.singleton( defaultIncludePattern );
            scanner = new SimpleSourceInclusionScanner( testIncludes, Collections.<String>emptySet() );
        }
        else
        {
            if ( testIncludes.isEmpty() )
            {
                testIncludes.add( defaultIncludePattern );
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
    
    @Override
    protected String getRelease()
    {
        return testRelease == null ? release : testRelease;
    }

    protected String getCompilerArgument()
    {
        return testCompilerArgument == null ? compilerArgument : testCompilerArgument;
    }

    protected Map<String, String> getCompilerArguments()
    {
        return testCompilerArguments == null ? compilerArguments : testCompilerArguments;
    }

    protected File getGeneratedSourcesDirectory()
    {
        return generatedTestSourcesDirectory;
    }

    @Override
    protected boolean isTestCompile()
    {
        return true;
    }

    static class ModuleInfo
    {

        static Pattern namePattern = Pattern.compile( "module (.+)\\{", Pattern.DOTALL );

        static ModuleInfo of( Path path )
        {
            if ( Files.isDirectory( path ) )
            {
                path = path.resolve( "module-info.java" );
            }
            if ( Files.notExists( path ) )
            {
                throw new IllegalArgumentException( "expected module-info.java file, but got: " + path );
            }
            try
            {
                return of( new String( Files.readAllBytes( path ), StandardCharsets.UTF_8 ) );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "reading '" + path + "' failed", e );
            }
        }

        static ModuleInfo of( String source )
        {
            // extract module name
            Matcher nameMatcher = namePattern.matcher( source );
            if ( !nameMatcher.find() )
            {
                throw new AssertionError( "expected java module descriptor unit, but got: " + source );
            }
            String name = nameMatcher.group( 1 ).trim();
            return new ModuleInfo( name );
        }

        final String name;

        ModuleInfo( String name )
        {
            this.name = name;
        }

        String name()
        {
            return name;
        }
    }

}
