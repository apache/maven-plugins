package org.apache.maven.plugins.assembly.io;

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

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.interpolation.AssemblyExpressionEvaluator;
import org.apache.maven.plugins.assembly.interpolation.AssemblyInterpolator;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.Component;
import org.apache.maven.plugins.assembly.model.ContainerDescriptorHandlerConfig;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.FileItem;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.plugins.assembly.model.Repository;
import org.apache.maven.plugins.assembly.model.io.xpp3.AssemblyXpp3Reader;
import org.apache.maven.plugins.assembly.model.io.xpp3.AssemblyXpp3Writer;
import org.apache.maven.plugins.assembly.model.io.xpp3.ComponentXpp3Reader;
import org.apache.maven.plugins.assembly.resolved.AssemblyId;
import org.apache.maven.plugins.assembly.utils.InterpolationConstants;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.io.location.ClasspathResourceLocatorStrategy;
import org.apache.maven.shared.io.location.FileLocatorStrategy;
import org.apache.maven.shared.io.location.Location;
import org.apache.maven.shared.io.location.Locator;
import org.apache.maven.shared.io.location.LocatorStrategy;
import org.apache.maven.shared.utils.ReaderFactory;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.interpolation.fixed.InterpolationState;
import org.codehaus.plexus.interpolation.fixed.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.fixed.PrefixedPropertiesValueSource;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @version $Id$
 */
@org.codehaus.plexus.component.annotations.Component( role = AssemblyReader.class )
public class DefaultAssemblyReader
    extends AbstractLogEnabled
    implements AssemblyReader
{

    public static FixedStringSearchInterpolator createProjectInterpolator( MavenProject project )
    {
        // CHECKSTYLE_OFF: LineLength
        return FixedStringSearchInterpolator.create( new PrefixedPropertiesValueSource( InterpolationConstants.PROJECT_PROPERTIES_PREFIXES,
                                                                                        project.getProperties(), true ),
                                                     new PrefixedObjectValueSource( InterpolationConstants.PROJECT_PREFIXES,
                                                                                    project, true ) );
        // CHECKSTYLE_ON: LineLength
    }

    @Override
    public List<Assembly> readAssemblies( final AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        final Locator locator = new Locator();

        final List<LocatorStrategy> strategies = new ArrayList<LocatorStrategy>();
        strategies.add( new RelativeFileLocatorStrategy( configSource.getBasedir() ) );
        strategies.add( new FileLocatorStrategy() );

        final List<LocatorStrategy> refStrategies = new ArrayList<LocatorStrategy>();
        refStrategies.add( new PrefixedClasspathLocatorStrategy( "/assemblies/" ) );

        final List<Assembly> assemblies = new ArrayList<Assembly>();

        final String[] descriptors = configSource.getDescriptors();
        final String[] descriptorRefs = configSource.getDescriptorReferences();
        final File descriptorSourceDirectory = configSource.getDescriptorSourceDirectory();

        if ( ( descriptors != null ) && ( descriptors.length > 0 ) )
        {
            locator.setStrategies( strategies );
            for ( String descriptor1 : descriptors )
            {
                getLogger().info( "Reading assembly descriptor: " + descriptor1 );
                addAssemblyFromDescriptor( descriptor1, locator, configSource, assemblies );
            }
        }

        if ( ( descriptorRefs != null ) && ( descriptorRefs.length > 0 ) )
        {
            locator.setStrategies( refStrategies );
            for ( String descriptorRef : descriptorRefs )
            {
                addAssemblyForDescriptorReference( descriptorRef, configSource, assemblies );
            }
        }

        if ( ( descriptorSourceDirectory != null ) && descriptorSourceDirectory.isDirectory() )
        {
            // CHECKSTYLE_OFF: LineLength
            locator.setStrategies( Collections.<LocatorStrategy>singletonList( new RelativeFileLocatorStrategy( descriptorSourceDirectory ) ) );
            // CHECKSTYLE_ON: LineLength

            final DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( descriptorSourceDirectory );
            scanner.setIncludes( new String[] { "**/*.xml" } );
            scanner.addDefaultExcludes();

            scanner.scan();

            final String[] paths = scanner.getIncludedFiles();

            for ( String path : paths )
            {
                addAssemblyFromDescriptor( path, locator, configSource, assemblies );
            }
        }

        if ( assemblies.isEmpty() )
        {
            if ( configSource.isIgnoreMissingDescriptor() )
            {
                getLogger().debug( "Ignoring missing assembly descriptors per configuration. "
                    + "See messages above for specifics." );
            }
            else
            {
                throw new AssemblyReadException( "No assembly descriptors found." );
            }
        }

        // check unique IDs
        final Set<String> ids = new HashSet<String>();
        for ( final Assembly assembly : assemblies )
        {
            if ( !ids.add( assembly.getId() ) )
            {
                getLogger().warn( "The assembly id " + assembly.getId() + " is used more than once." );
            }

        }
        return assemblies;
    }

    @Override
    public Assembly getAssemblyForDescriptorReference( final String ref,
                                                       final AssemblerConfigurationSource configSource )
                                                           throws AssemblyReadException,
                                                           InvalidAssemblerConfigurationException
    {
        return addAssemblyForDescriptorReference( ref, configSource, new ArrayList<Assembly>( 1 ) );
    }

    @Override
    public Assembly getAssemblyFromDescriptorFile( final File file, final AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        return addAssemblyFromDescriptorFile( file, configSource, new ArrayList<Assembly>( 1 ) );
    }

    private Assembly addAssemblyForDescriptorReference( final String ref,
                                                        final AssemblerConfigurationSource configSource,
                                                        final List<Assembly> assemblies )
                                                            throws AssemblyReadException,
                                                            InvalidAssemblerConfigurationException
    {
        final InputStream resourceAsStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream( "assemblies/" + ref + ".xml" );

        if ( resourceAsStream == null )
        {
            if ( configSource.isIgnoreMissingDescriptor() )
            {
                getLogger().debug( "Ignoring missing assembly descriptor with ID '" + ref + "' per configuration." );
                return null;
            }
            else
            {
                throw new AssemblyReadException( "Descriptor with ID '" + ref + "' not found" );
            }
        }

        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( resourceAsStream );
            final Assembly assembly = readAssembly( reader, ref, null, configSource );
            reader.close();
            reader = null;
            assemblies.add( assembly );
            return assembly;
        }
        catch ( final IOException e )
        {
            throw new AssemblyReadException( "Problem with descriptor with ID '" + ref + "'", e );
        }
        finally
        {
            IOUtils.closeQuietly( reader );
        }
    }

    private Assembly addAssemblyFromDescriptorFile( final File descriptor,
                                                    final AssemblerConfigurationSource configSource,
                                                    final List<Assembly> assemblies )
                                                        throws AssemblyReadException,
                                                        InvalidAssemblerConfigurationException
    {
        if ( !descriptor.exists() )
        {
            if ( configSource.isIgnoreMissingDescriptor() )
            {
                getLogger().debug( "Ignoring missing assembly descriptor: '" + descriptor + "' per configuration." );
                return null;
            }
            else
            {
                throw new AssemblyReadException( "Descriptor: '" + descriptor + "' not found" );
            }
        }

        Reader r = null;
        try
        {
            r = ReaderFactory.newXmlReader( descriptor );

            final Assembly assembly =
                readAssembly( r, descriptor.getAbsolutePath(), descriptor.getParentFile(), configSource );

            r.close();
            r = null;

            assemblies.add( assembly );

            return assembly;
        }
        catch ( final IOException e )
        {
            throw new AssemblyReadException( "Error reading assembly descriptor: " + descriptor, e );
        }
        finally
        {
            IOUtil.close( r );
        }
    }

    private Assembly addAssemblyFromDescriptor( final String spec, final Locator locator,
                                                final AssemblerConfigurationSource configSource,
                                                final List<Assembly> assemblies )
                                                    throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        final Location location = locator.resolve( spec );

        if ( location == null )
        {
            if ( configSource.isIgnoreMissingDescriptor() )
            {
                getLogger().debug( "Ignoring missing assembly descriptor with ID '" + spec
                    + "' per configuration.\nLocator output was:\n\n" + locator.getMessageHolder().render() );
                return null;
            }
            else
            {
                throw new AssemblyReadException( "Error locating assembly descriptor: " + spec + "\n\n"
                    + locator.getMessageHolder().render() );
            }
        }

        Reader r = null;
        try
        {
            r = ReaderFactory.newXmlReader( location.getInputStream() );

            File dir = null;
            if ( location.getFile() != null )
            {
                dir = location.getFile().getParentFile();
            }

            final Assembly assembly = readAssembly( r, spec, dir, configSource );

            r.close();
            r = null;

            assemblies.add( assembly );

            return assembly;
        }
        catch ( final IOException e )
        {
            throw new AssemblyReadException( "Error reading assembly descriptor: " + spec, e );
        }
        finally
        {
            IOUtil.close( r );
        }

    }

    public Assembly readAssembly( Reader reader, final String locationDescription, final File assemblyDir,
                                  final AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        Assembly assembly;

        final MavenProject project = configSource.getProject();
        try
        {

            InterpolationState is = new InterpolationState();
            final RecursionInterceptor interceptor =
                new PrefixAwareRecursionInterceptor( InterpolationConstants.PROJECT_PREFIXES, true );
            is.setRecursionInterceptor( interceptor );

            FixedStringSearchInterpolator interpolator =
                AssemblyInterpolator.fullInterpolator( project, createProjectInterpolator( project ), configSource );
            AssemblyXpp3Reader.ContentTransformer transformer =
                AssemblyInterpolator.assemblyInterpolator( interpolator, is, getLogger() );

            final AssemblyXpp3Reader r = new AssemblyXpp3Reader( transformer );
            assembly = r.read( reader );

            ComponentXpp3Reader.ContentTransformer ctrans =
                AssemblyInterpolator.componentInterpolator( interpolator, is, getLogger() );
            mergeComponentsWithMainAssembly( assembly, assemblyDir, configSource, ctrans );
            debugPrintAssembly( "After assembly is interpolated:", assembly );

            AssemblyInterpolator.checkErrors( AssemblyId.createAssemblyId( assembly ), is, getLogger() );

            reader.close();
            reader = null;
        }
        catch ( final IOException e )
        {
            throw new AssemblyReadException( "Error reading descriptor: " + locationDescription + ": " + e.getMessage(),
                                             e );
        }
        catch ( final XmlPullParserException e )
        {
            throw new AssemblyReadException( "Error reading descriptor: " + locationDescription + ": " + e.getMessage(),
                                             e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        if ( assembly.isIncludeSiteDirectory() )
        {
            includeSiteInAssembly( assembly, configSource );
        }

        return assembly;
    }

    private void debugPrintAssembly( final String message, final Assembly assembly )
    {
        final StringWriter sWriter = new StringWriter();
        try
        {
            new AssemblyXpp3Writer().write( sWriter, assembly );
        }
        catch ( final IOException e )
        {
            getLogger().debug( "Failed to print debug message with assembly descriptor listing, and message: "
                + message, e );
        }

        getLogger().debug( message + "\n\n" + sWriter.toString() + "\n\n" );
    }

    /**
     * Add the contents of all included components to main assembly
     *
     * @param assembly The assembly
     * @param assemblyDir The assembly directory
     * @param transformer The component interpolator
     * @throws AssemblyReadException .
     */
    protected void mergeComponentsWithMainAssembly( final Assembly assembly, final File assemblyDir,
                                                    final AssemblerConfigurationSource configSource,
                                                    ComponentXpp3Reader.ContentTransformer transformer )
                                                        throws AssemblyReadException
    {
        final Locator locator = new Locator();

        if ( assemblyDir != null && assemblyDir.exists() && assemblyDir.isDirectory() )
        {
            locator.addStrategy( new RelativeFileLocatorStrategy( assemblyDir ) );
        }

        // allow absolute paths in componentDescriptor... MASSEMBLY-486
        locator.addStrategy( new RelativeFileLocatorStrategy( configSource.getBasedir() ) );
        locator.addStrategy( new FileLocatorStrategy() );
        locator.addStrategy( new ClasspathResourceLocatorStrategy() );

        final AssemblyExpressionEvaluator aee = new AssemblyExpressionEvaluator( configSource );

        final List<String> componentLocations = assembly.getComponentDescriptors();

        for ( String location : componentLocations )
        {
            // allow expressions in path to component descriptor... MASSEMBLY-486
            try
            {
                location = aee.evaluate( location ).toString();
            }
            catch ( final Exception eee )
            {
                getLogger().error( "Error interpolating componentDescriptor: " + location, eee );
            }

            final Location resolvedLocation = locator.resolve( location );

            if ( resolvedLocation == null )
            {
                throw new AssemblyReadException( "Failed to locate component descriptor: " + location );
            }

            Component component = null;
            Reader reader = null;
            try
            {
                reader = new InputStreamReader( resolvedLocation.getInputStream() );
                component = new ComponentXpp3Reader( transformer ).read( reader );
            }
            catch ( final IOException e )
            {
                throw new AssemblyReadException( "Error reading component descriptor: " + location + " (resolved to: "
                    + resolvedLocation.getSpecification() + ")", e );
            }
            catch ( final XmlPullParserException e )
            {
                throw new AssemblyReadException( "Error reading component descriptor: " + location + " (resolved to: "
                    + resolvedLocation.getSpecification() + ")", e );
            }
            finally
            {
                IOUtil.close( reader );
            }

            mergeComponentWithAssembly( component, assembly );
        }
    }

    /**
     * Add the content of a single Component to main assembly
     *
     * @param component The component
     * @param assembly The assembly
     */
    protected void mergeComponentWithAssembly( final Component component, final Assembly assembly )
    {
        final List<ContainerDescriptorHandlerConfig> containerHandlerDescriptors =
            component.getContainerDescriptorHandlers();

        for ( final ContainerDescriptorHandlerConfig cfg : containerHandlerDescriptors )
        {
            assembly.addContainerDescriptorHandler( cfg );
        }

        final List<DependencySet> dependencySetList = component.getDependencySets();

        for ( final DependencySet dependencySet : dependencySetList )
        {
            assembly.addDependencySet( dependencySet );
        }

        final List<FileSet> fileSetList = component.getFileSets();

        for ( final FileSet fileSet : fileSetList )
        {
            assembly.addFileSet( fileSet );
        }

        final List<FileItem> fileList = component.getFiles();

        for ( final FileItem fileItem : fileList )
        {
            assembly.addFile( fileItem );
        }

        final List<Repository> repositoriesList = component.getRepositories();

        for ( final Repository repository : repositoriesList )
        {
            assembly.addRepository( repository );
        }

        final List<ModuleSet> moduleSets = component.getModuleSets();
        for ( final ModuleSet moduleSet : moduleSets )
        {
            assembly.addModuleSet( moduleSet );
        }
    }

    @Override
    public void includeSiteInAssembly( final Assembly assembly, final AssemblerConfigurationSource configSource )
        throws InvalidAssemblerConfigurationException
    {
        final File siteDirectory = configSource.getSiteDirectory();

        if ( !siteDirectory.exists() )
        {
            throw new InvalidAssemblerConfigurationException( "site did not exist in the target directory - "
                + "please run site:site before creating the assembly" );
        }

        getLogger().info( "Adding site directory to assembly : " + siteDirectory );

        final FileSet siteFileSet = new FileSet();

        siteFileSet.setDirectory( siteDirectory.getPath() );

        siteFileSet.setOutputDirectory( "/site" );

        assembly.addFileSet( siteFileSet );
    }

    @Override
    protected Logger getLogger()
    {
        Logger logger = super.getLogger();

        if ( logger == null )
        {
            logger = new ConsoleLogger( Logger.LEVEL_INFO, "assemblyReader-internal" );
            enableLogging( logger );
        }

        return logger;
    }

}
