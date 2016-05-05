package org.apache.maven.plugin.checkstyle.exec;

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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.resource.ResourceManager;
import org.codehaus.plexus.resource.loader.FileResourceCreationException;
import org.codehaus.plexus.resource.loader.FileResourceLoader;
import org.codehaus.plexus.resource.loader.ResourceNotFoundException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.PackageNamesLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.FilterSet;
import com.puppycrawl.tools.checkstyle.filters.SuppressionsLoader;

/**
 * @author Olivier Lamy
 * @since 2.5
 * @version $Id$
 */
@Component( role = CheckstyleExecutor.class, hint = "default", instantiationStrategy = "per-lookup" )
public class DefaultCheckstyleExecutor
    extends AbstractLogEnabled
    implements CheckstyleExecutor
{
    @Requirement( hint = "default" )
    private ResourceManager locator;
    
    @Requirement( hint = "license" )
    private ResourceManager licenseLocator;

    public CheckstyleResults executeCheckstyle( CheckstyleExecutorRequest request )
        throws CheckstyleExecutorException, CheckstyleException
    {
        // Checkstyle will always use the context classloader in order
        // to load resources (dtds),
        // so we have to fix it
        // olamy this hack is not anymore needed in Maven 3.x
        ClassLoader checkstyleClassLoader = PackageNamesLoader.class.getClassLoader();
        Thread.currentThread().setContextClassLoader( checkstyleClassLoader );

        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "executeCheckstyle start headerLocation : " + request.getHeaderLocation() );
        }

        MavenProject project = request.getProject();

        configureResourceLocator( locator, request, null );
        
        configureResourceLocator( licenseLocator, request, request.getLicenseArtifacts() );

        // Config is less critical than License, locator can still be used.
        // configureResourceLocator( configurationLocator, request, request.getConfigurationArtifacts() );

        List<File> files;
        try
        {
            files = getFilesToProcess( request );
        }
        catch ( IOException e )
        {
            throw new CheckstyleExecutorException( "Error getting files to process", e );
        }

        final String suppressionsFilePath = getSuppressionsFilePath( request );
        FilterSet filterSet = getSuppressionsFilterSet( suppressionsFilePath );

        Checker checker = new Checker();

        // setup classloader, needed to avoid "Unable to get class information for ..." errors
        List<String> classPathStrings = new ArrayList<>();
        List<String> outputDirectories = new ArrayList<>();
        
        // stand-alone
        Collection<File> sourceDirectories = null;
        Collection<File> testSourceDirectories = request.getTestSourceDirectories();
        
        // aggregator
        Map<MavenProject, Collection<File>> sourceDirectoriesByProject = new HashMap<>();
        Map<MavenProject, Collection<File>> testSourceDirectoriesByProject = new HashMap<>();
        
        if ( request.isAggregate() )
        {
            for ( MavenProject childProject : request.getReactorProjects() )
            {
                sourceDirectories = new ArrayList<>( childProject.getCompileSourceRoots().size() );
                List<String> compileSourceRoots = childProject.getCompileSourceRoots();
                for ( String compileSourceRoot : compileSourceRoots )
                {
                    sourceDirectories.add( new File( compileSourceRoot ) );
                }
                sourceDirectoriesByProject.put( childProject, sourceDirectories );
                
                testSourceDirectories = new ArrayList<>( childProject.getTestCompileSourceRoots().size() );
                List<String> testCompileSourceRoots = childProject.getTestCompileSourceRoots();
                for ( String testCompileSourceRoot : testCompileSourceRoots )
                {
                    testSourceDirectories.add( new File( testCompileSourceRoot ) );
                }
                testSourceDirectoriesByProject.put( childProject, testSourceDirectories );
                
                prepareCheckstylePaths( request, childProject, classPathStrings, outputDirectories,
                                        sourceDirectories, testSourceDirectories );
            }
        }
        else
        {
            sourceDirectories = request.getSourceDirectories();
            prepareCheckstylePaths( request, project, classPathStrings, outputDirectories, sourceDirectories,
                                    testSourceDirectories );
        }

        final List<URL> urls = new ArrayList<>( classPathStrings.size() );

        for ( String path : classPathStrings )
        {
            try
            {
                urls.add( new File( path ).toURL() );
            }
            catch ( MalformedURLException e )
            {
                throw new CheckstyleExecutorException( e.getMessage(), e );
            }
        }

        for ( String outputDirectoryString : outputDirectories )
        {
            try
            {
                if ( outputDirectoryString != null )
                {
                    File outputDirectoryFile = new File( outputDirectoryString );
                    if ( outputDirectoryFile.exists() )
                    {
                        URL outputDirectoryUrl = outputDirectoryFile.toURL();
                        getLogger().debug( "Adding the outputDirectory " + outputDirectoryUrl.toString()
                                               + " to the Checkstyle class path" );
                        urls.add( outputDirectoryUrl );
                    }
                }
            }
            catch ( MalformedURLException e )
            {
                throw new CheckstyleExecutorException( e.getMessage(), e );
            }
        }

        URLClassLoader projectClassLoader = AccessController.doPrivileged( new PrivilegedAction<URLClassLoader>()
        {
            public URLClassLoader run()
            {
                return new URLClassLoader( urls.toArray( new URL[urls.size()] ), null );
            }
        } );

        checker.setClassLoader( projectClassLoader );

        checker.setModuleClassLoader( Thread.currentThread().getContextClassLoader() );

        if ( filterSet != null )
        {
            checker.addFilter( filterSet );
        }
        Configuration configuration = getConfiguration( request );
        checker.configure( configuration );

        AuditListener listener = request.getListener();

        if ( listener != null )
        {
            checker.addListener( listener );
        }

        if ( request.isConsoleOutput() )
        {
            checker.addListener( request.getConsoleListener() );
        }

        CheckstyleCheckerListener checkerListener = new CheckstyleCheckerListener( configuration );
        if ( request.isAggregate() )
        {
            for ( MavenProject childProject : request.getReactorProjects() )
            {
                sourceDirectories = sourceDirectoriesByProject.get( childProject );
                testSourceDirectories = testSourceDirectoriesByProject.get( childProject );
                addSourceDirectory( checkerListener, sourceDirectories,
                                    testSourceDirectories,
                                    childProject.getResources(), request );
            }
        }
        else
        {
            addSourceDirectory( checkerListener, sourceDirectories, testSourceDirectories, request.getResources(),
                                request );
        }

        checker.addListener( checkerListener );

        int nbErrors = checker.process( files );

        checker.destroy();

        if ( projectClassLoader instanceof Closeable )
        {
            try
            {
                ( ( Closeable ) projectClassLoader ).close();
            }
            catch ( IOException ex ) 
            {
                // Nothing we can do - and not detrimental to the build (save running out of file handles).
                getLogger().info( "Failed to close custom Classloader - this indicated a bug in the code.", ex );
            }
        }

        if ( request.getStringOutputStream() != null )
        {
            String message = request.getStringOutputStream().toString().trim();

            if ( message.length() > 0 )
            {
                getLogger().info( message );
            }
        }

        if ( nbErrors > 0 )
        {
            StringBuilder message = new StringBuilder( "There " );
            if ( nbErrors == 1 )
            {
                message.append( "is" );
            }
            else
            {
                message.append( "are" );
            }
            message.append( " " );
            message.append( nbErrors );
            message.append( " error" );
            if ( nbErrors != 1 )
            {
                message.append( "s" );
            }
            message.append( " reported by Checkstyle" );
            String version = getCheckstyleVersion();
            if ( version != null )
            {
                message.append( " " );
                message.append( version );
            }
            message.append( " with " );
            message.append( request.getConfigLocation() );
            message.append( " ruleset." );

            if ( request.isFailsOnError() )
            {
                // TODO: should be a failure, not an error. Report is not meant to
                // throw an exception here (so site would
                // work regardless of config), but should record this information
                throw new CheckstyleExecutorException( message.toString() );
            }
            else
            {
                getLogger().info( message.toString() );
            }
        }

        return checkerListener.getResults();
    }

    protected void addSourceDirectory( CheckstyleCheckerListener sinkListener, Collection<File> sourceDirectories,
                                       Collection<File> testSourceDirectories, List<Resource> resources,
                                       CheckstyleExecutorRequest request )
    {
        if ( sourceDirectories != null )
        {
            for ( File sourceDirectory : sourceDirectories )
            {
                if ( sourceDirectory.exists() )
                {
                    sinkListener.addSourceDirectory( sourceDirectory );
                }
            }
        }

        if ( request.isIncludeTestSourceDirectory() && ( testSourceDirectories != null ) )
        {
            for ( File testSourceDirectory : testSourceDirectories )
            {
                if ( testSourceDirectory.isDirectory() )
                {
                    sinkListener.addSourceDirectory( testSourceDirectory );
                }
            }
        }

        if ( resources != null )
        {
            for ( Resource resource : resources )
            {
                if ( resource.getDirectory() != null )
                {
                    File resourcesDirectory = new File( resource.getDirectory() );
                    if ( resourcesDirectory.exists() && resourcesDirectory.isDirectory() )
                    {
                        sinkListener.addSourceDirectory( resourcesDirectory );
                        getLogger().debug( "Added '" + resourcesDirectory.getAbsolutePath()
                                + "' as a source directory." );
                    }
                }
            }
        }
    }

    public Configuration getConfiguration( CheckstyleExecutorRequest request )
        throws CheckstyleExecutorException
    {
        try
        {
            // Checkstyle will always use the context classloader in order
            // to load resources (dtds),
            // so we have to fix it
            ClassLoader checkstyleClassLoader = PackageNamesLoader.class.getClassLoader();
            Thread.currentThread().setContextClassLoader( checkstyleClassLoader );
            String configFile = getConfigFile( request );
            Properties overridingProperties = getOverridingProperties( request );
            Configuration config = ConfigurationLoader
                .loadConfiguration( configFile, new PropertiesExpander( overridingProperties ) );
            String effectiveEncoding = StringUtils.isNotEmpty( request.getEncoding() ) ? request.getEncoding() : System
                .getProperty( "file.encoding", "UTF-8" );
            
            if ( StringUtils.isEmpty( request.getEncoding() ) )
            {
                getLogger().warn( "File encoding has not been set, using platform encoding " + effectiveEncoding
                                      + ", i.e. build is platform dependent!" );
            }

            if ( "Checker".equals( config.getName() )
                    || "com.puppycrawl.tools.checkstyle.Checker".equals( config.getName() ) )
            {
                if ( config instanceof DefaultConfiguration )
                {
                    // MCHECKSTYLE-173 Only add the "charset" attribute if it has not been set
                    try
                    {
                        if ( config.getAttribute( "charset" ) == null )
                        {
                            ( (DefaultConfiguration) config ).addAttribute( "charset", effectiveEncoding );
                        }
                    }
                    catch ( CheckstyleException ex )
                    {
                        // Checkstyle 5.4+ throws an exception when trying to access an attribute that doesn't exist
                        ( (DefaultConfiguration) config ).addAttribute( "charset", effectiveEncoding );
                    }
                }
                else
                {
                    getLogger().warn( "Failed to configure file encoding on module " + config );
                }
            }
            Configuration[] modules = config.getChildren();
            for ( Configuration module : modules )
            {
                if ( "TreeWalker".equals( module.getName() )
                    || "com.puppycrawl.tools.checkstyle.TreeWalker".equals( module.getName() ) )
                {
                    if ( module instanceof DefaultConfiguration )
                    {
                        // MCHECKSTYLE-132 DefaultConfiguration addAttribute has changed in checkstyle 5.3
                        try
                        {
                            if ( module.getAttribute( "cacheFile" ) == null )
                            {
                                ( (DefaultConfiguration) module ).addAttribute( "cacheFile", request.getCacheFile() );
                            }
                        }
                        catch ( CheckstyleException ex )
                        {
                            // MCHECKSTYLE-159 - checkstyle 5.4 throws an exception instead of return null if
                            // "cacheFile"
                            // doesn't exist
                            ( (DefaultConfiguration) module ).addAttribute( "cacheFile", request.getCacheFile() );
                        }
                    }
                    else
                    {
                        getLogger().warn( "Failed to configure cache file on module " + module );
                    }
                }
            }
            return config;
        }
        catch ( CheckstyleException e )
        {
            throw new CheckstyleExecutorException( "Failed during checkstyle configuration", e );
        }
    }

    private void prepareCheckstylePaths( CheckstyleExecutorRequest request, MavenProject project,
                                         List<String> classPathStrings, List<String> outputDirectories,
                                         Collection<File> sourceDirectories, Collection<File> testSourceDirectories )
        throws CheckstyleExecutorException
    {
        try
        {
            outputDirectories.add( project.getBuild().getOutputDirectory() );

            if ( request.isIncludeTestSourceDirectory() && ( testSourceDirectories != null )
                && anyDirectoryExists( testSourceDirectories ) )
            {
                classPathStrings.addAll( project.getTestClasspathElements() );
                outputDirectories.add( project.getBuild().getTestOutputDirectory() );
            }
            else
            {
                classPathStrings.addAll( project.getCompileClasspathElements() );
            }
        }
        catch ( DependencyResolutionRequiredException e )
        {
            throw new CheckstyleExecutorException( e.getMessage(), e );
        }
    }
    
    private boolean anyDirectoryExists( Collection<File> files )
    {
        for ( File file : files )
        {
            if ( file.isDirectory() )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the effective Checkstyle version at runtime.
     * @return the MANIFEST implementation version of Checkstyle API package (can be <code>null</code>)
     *
     *@todo Copied from CheckstyleReportGenerator - move to a utility class
     */
    private String getCheckstyleVersion()
    {
        Package checkstyleApiPackage = Configuration.class.getPackage();

        return ( checkstyleApiPackage == null ) ? null : checkstyleApiPackage.getImplementationVersion();
    }

    private Properties getOverridingProperties( CheckstyleExecutorRequest request )
        throws CheckstyleExecutorException
    {
        Properties p = new Properties();
        InputStream in = null;
        try
        {
            if ( request.getPropertiesLocation() != null )
            {
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "request.getPropertiesLocation() " + request.getPropertiesLocation() );
                }

                File propertiesFile = locator.getResourceAsFile( request.getPropertiesLocation(),
                                                                 "checkstyle-checker.properties" );

                if ( propertiesFile != null )
                {
                    in = new FileInputStream( propertiesFile );
                    p.load( in );
                    in.close();
                    in = null;
                }
            }

            if ( StringUtils.isNotEmpty( request.getPropertyExpansion() ) )
            {
                String propertyExpansion = request.getPropertyExpansion();
                // Convert \ to \\, so that p.load will convert it back properly
                propertyExpansion = StringUtils.replace( propertyExpansion, "\\", "\\\\" );
                p.load( new ByteArrayInputStream( propertyExpansion.getBytes() ) );
            }

            // Workaround for MCHECKSTYLE-48
            // Make sure that "config/maven-header.txt" is the default value
            // for headerLocation, if configLocation="config/maven_checks.xml"
            String headerLocation = request.getHeaderLocation();
            if ( "config/maven_checks.xml".equals( request.getConfigLocation() ) )
            {

                if ( "LICENSE.txt".equals( request.getHeaderLocation() ) )
                {
                    headerLocation = "config/maven-header.txt";
                }
            }
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "headerLocation " + headerLocation );
            }

            if ( StringUtils.isNotEmpty( headerLocation ) )
            {
                try
                {
                    File headerFile = licenseLocator.getResourceAsFile( headerLocation, "checkstyle-header.txt" );

                    if ( headerFile != null )
                    {
                        p.setProperty( "checkstyle.header.file", headerFile.getAbsolutePath() );
                    }
                }
                catch ( FileResourceCreationException | ResourceNotFoundException e )
                {
                    getLogger().debug( "Unable to process header location: " + headerLocation );
                    getLogger().debug( "Checkstyle will throw exception if ${checkstyle.header.file} is used" );
                }
            }

            if ( request.getCacheFile() != null )
            {
                p.setProperty( "checkstyle.cache.file", request.getCacheFile() );
            }
        }
        catch ( IOException | ResourceNotFoundException | FileResourceCreationException e )
        {
            throw new CheckstyleExecutorException( "Failed to get overriding properties", e );
        }
        finally
        {
            IOUtils.closeQuietly( in );
        }
        if ( request.getSuppressionsFileExpression() != null )
        {
            String suppressionsFilePath = getSuppressionsFilePath( request );

            if ( suppressionsFilePath != null )
            {
                p.setProperty( request.getSuppressionsFileExpression(), suppressionsFilePath );
            }
        }

        return p;
    }

    private List<File> getFilesToProcess( CheckstyleExecutorRequest request )
        throws IOException
    {
        StringBuilder excludesStr = new StringBuilder();

        if ( StringUtils.isNotEmpty( request.getExcludes() ) )
        {
            excludesStr.append( request.getExcludes() );
        }

        String[] defaultExcludes = FileUtils.getDefaultExcludes();
        for ( String defaultExclude : defaultExcludes )
        {
            if ( excludesStr.length() > 0 )
            {
                excludesStr.append( "," );
            }

            excludesStr.append( defaultExclude );
        }

        Set<File> files = new LinkedHashSet<>();
        if ( request.isAggregate() )
        {
            for ( MavenProject project : request.getReactorProjects() )
            {
                Set<File> sourceDirectories = new LinkedHashSet<>();
                
                // CompileSourceRoots are absolute paths
                List<String> compileSourceRoots = project.getCompileSourceRoots(); 
                for ( String compileSourceRoot : compileSourceRoots )
                {
                    sourceDirectories.add( new File( compileSourceRoot ) );
                }

                Set<File> testSourceDirectories = new LinkedHashSet<>();
                // CompileSourceRoots are absolute paths
                List<String> testCompileSourceRoots = project.getTestCompileSourceRoots(); 
                for ( String testCompileSourceRoot : testCompileSourceRoots )
                {
                    testSourceDirectories.add( new File( testCompileSourceRoot ) );
                }

                addFilesToProcess( request, sourceDirectories, project.getResources(), project.getTestResources(),
                                   files, testSourceDirectories );
            }
        }
        else
        {
            Collection<File> sourceDirectories = request.getSourceDirectories();
            addFilesToProcess( request, sourceDirectories, request.getResources(),
                request.getTestResources(), files, request.getTestSourceDirectories() );
        }

        getLogger().debug( "Added " + files.size() + " files to process." );

        return new ArrayList<>( files );
    }

    private void addFilesToProcess( CheckstyleExecutorRequest request, Collection<File> sourceDirectories,
                                    List<Resource> resources, List<Resource> testResources, Collection<File> files,
                                    Collection<File> testSourceDirectories )
        throws IOException
    {
        if ( sourceDirectories != null )
        {
            for ( File sourceDirectory : sourceDirectories )
            {
                if ( sourceDirectory.isDirectory() )
                {
                    final List<File> sourceFiles =
                        FileUtils.getFiles( sourceDirectory, request.getIncludes(), request.getExcludes() );
                    files.addAll( sourceFiles );
                    getLogger().debug( "Added " + sourceFiles.size() + " source files found in '"
                                           + sourceDirectory.getAbsolutePath() + "'." );
                }
            }
        }

        if ( request.isIncludeTestSourceDirectory() && testSourceDirectories != null )
        {
            for ( File testSourceDirectory : testSourceDirectories )
            {
                if ( testSourceDirectory.isDirectory() )
                {
                    final List<File> testSourceFiles =
                        FileUtils.getFiles( testSourceDirectory, request.getIncludes(), request.getExcludes() );
                    
                    files.addAll( testSourceFiles );
                    getLogger().debug( "Added " + testSourceFiles.size() + " test source files found in '"
                            + testSourceDirectory.getAbsolutePath() + "'." );
                }
            }
        }

        if ( resources != null && request.isIncludeResources() )
        {
            addResourceFilesToProcess( request, resources, files );
        }
        else
        {
            getLogger().debug( "No resources found in this project." );
        }

        if ( testResources != null && request.isIncludeTestResources() )
        {
            addResourceFilesToProcess( request, testResources, files );
        }
        else
        {
            getLogger().debug( "No test resources found in this project." );
        }
    }

    private void addResourceFilesToProcess( CheckstyleExecutorRequest request, List<Resource> resources,
                                            Collection<File> files )
        throws IOException
    {
        for ( Resource resource : resources )
        {
            if ( resource.getDirectory() != null )
            {
                File resourcesDirectory = new File( resource.getDirectory() );
                if ( resourcesDirectory.isDirectory() )
                {
                    String includes = request.getResourceIncludes();
                    String excludes = request.getResourceExcludes();

                    // MCHECKSTYLE-214: Only with project-root respect in/excludes, otherwise you'll get every file
                    if ( resourcesDirectory.equals( request.getProject().getBasedir() ) )
                    {
                        String resourceIncludes = StringUtils.join( resource.getIncludes().iterator(), "," );
                        if ( StringUtils.isEmpty( includes ) )
                        {
                            includes = resourceIncludes;
                        }
                        else
                        {
                            includes += "," + resourceIncludes;
                        }
                        
                        String resourceExcludes = StringUtils.join( resource.getExcludes().iterator(), "," );
                        if ( StringUtils.isEmpty( excludes ) )
                        {
                            excludes = resourceExcludes;
                        }
                        else
                        {
                            excludes += "," + resourceExcludes;
                        }
                    }
                    
                    List<File> resourceFiles =
                        FileUtils.getFiles( resourcesDirectory, includes, excludes );
                    files.addAll( resourceFiles );
                    getLogger().debug( "Added " + resourceFiles.size() + " resource files found in '"
                            + resourcesDirectory.getAbsolutePath() + "'." );
                }
                else
                {
                    getLogger().debug( "The resources directory '" + resourcesDirectory.getAbsolutePath()
                            + "' does not exist or is not a directory." );
                }
            }
        }
    }

    private FilterSet getSuppressionsFilterSet( final String suppressionsFilePath )
        throws CheckstyleExecutorException
    {
        if ( suppressionsFilePath == null )
        {
            return null;
        }

        try
        {
            return SuppressionsLoader.loadSuppressions( suppressionsFilePath );
        }
        catch ( CheckstyleException ce )
        {
            throw new CheckstyleExecutorException( "Failed to load suppressions file from: "
                + suppressionsFilePath, ce );
        }
    }

    private String getSuppressionsFilePath( final CheckstyleExecutorRequest request )
        throws CheckstyleExecutorException
    {
        final String suppressionsLocation = request.getSuppressionsLocation();
        if ( StringUtils.isEmpty( suppressionsLocation ) )
        {
            return null;
        }
        
        try
        {
            File suppressionsFile = locator.getResourceAsFile( suppressionsLocation, "checkstyle-suppressions.xml" );
            return suppressionsFile == null ? null : suppressionsFile.getAbsolutePath();
        }
        catch ( ResourceNotFoundException e )
        {
            throw new CheckstyleExecutorException( "Unable to find suppressions file at location: "
                + suppressionsLocation, e );
        }
        catch ( FileResourceCreationException e )
        {
            throw new CheckstyleExecutorException( "Unable to process suppressions file location: "
                + suppressionsLocation, e );
        }
    }

    private String getConfigFile( CheckstyleExecutorRequest request )
        throws CheckstyleExecutorException
    {
        try
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "request.getConfigLocation() " + request.getConfigLocation() );
            }

            File configFile = locator.getResourceAsFile( request.getConfigLocation(), "checkstyle-checker.xml" );
            if ( configFile == null )
            {
                throw new CheckstyleExecutorException( "Unable to process config location: "
                    + request.getConfigLocation() );
            }
            return configFile.getAbsolutePath();
        }
        catch ( ResourceNotFoundException e )
        {
            throw new CheckstyleExecutorException( "Unable to find configuration file at location: "
                + request.getConfigLocation(), e );
        }
        catch ( FileResourceCreationException e )
        {
            throw new CheckstyleExecutorException( "Unable to process configuration file at location: "
                + request.getConfigLocation(), e );
        }

    }

    /**
     * Configures search paths in the resource locator.
     * This method should only be called once per execution.
     *
     * @param request executor request data.
     */
    private void configureResourceLocator( final ResourceManager resourceManager,
                                           final CheckstyleExecutorRequest request,
                                           final List<Artifact> additionalArtifacts )
    {
        final MavenProject project = request.getProject();
        resourceManager.setOutputDirectory( new File( project.getBuild().getDirectory() ) );

        // Recurse up the parent hierarchy and add project directories to the search roots
        MavenProject parent = project;
        while ( parent != null && parent.getFile() != null )
        {
            // MCHECKSTYLE-131 ( olamy ) I don't like this hack.
            // (dkulp) Me either.   It really pollutes the location stuff
            // by allowing searches of stuff outside the current module.
            File dir = parent.getFile().getParentFile();
            resourceManager.addSearchPath( FileResourceLoader.ID, dir.getAbsolutePath() );
            parent = parent.getParent();
        }
        resourceManager.addSearchPath( "url", "" );
        
        // MCHECKSTYLE-225: load licenses from additional artifacts, not from classpath
        if ( additionalArtifacts != null )
        {
            for ( Artifact licenseArtifact : additionalArtifacts )
            {
                try
                {
                    resourceManager.addSearchPath( "jar", "jar:" + licenseArtifact.getFile().toURI().toURL() );
                }
                catch ( MalformedURLException e )
                {
                    // noop
                }
            }
        }
    }
}
