package org.apache.maven.plugin.assembly.io;

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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.interpolation.AssemblyInterpolationException;
import org.apache.maven.plugin.assembly.interpolation.AssemblyInterpolator;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.model.Component;
import org.apache.maven.plugin.assembly.model.ContainerDescriptorHandlerConfig;
import org.apache.maven.plugin.assembly.model.DependencySet;
import org.apache.maven.plugin.assembly.model.FileItem;
import org.apache.maven.plugin.assembly.model.FileSet;
import org.apache.maven.plugin.assembly.model.Repository;
import org.apache.maven.plugin.assembly.model.io.xpp3.AssemblyXpp3Reader;
import org.apache.maven.plugin.assembly.model.io.xpp3.AssemblyXpp3Writer;
import org.apache.maven.plugin.assembly.model.io.xpp3.ComponentXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.io.location.ArtifactLocatorStrategy;
import org.apache.maven.shared.io.location.ClasspathResourceLocatorStrategy;
import org.apache.maven.shared.io.location.FileLocatorStrategy;
import org.apache.maven.shared.io.location.Location;
import org.apache.maven.shared.io.location.Locator;
import org.apache.maven.shared.io.location.LocatorStrategy;
import org.apache.maven.shared.io.location.URLLocatorStrategy;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @version $Id$
 * @plexus.component role="org.apache.maven.plugin.assembly.io.AssemblyReader" role-hint="default"
 */
public class DefaultAssemblyReader
    extends AbstractLogEnabled
    implements AssemblyReader
{

    /**
     * @plexus.requirement
     */
    private ArtifactFactory factory;

    /**
     * @plexus.requirement
     */
    private ArtifactResolver resolver;

    public List readAssemblies( AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        Locator locator = new Locator();

        LocatorStrategy prefixedClasspathStrategy = new PrefixedClasspathLocatorStrategy( "/assemblies/" );
        LocatorStrategy classpathStrategy = new ClasspathResourceLocatorStrategy();

        List strategies = new ArrayList();
        strategies.add( new RelativeFileLocatorStrategy( configSource.getBasedir() ) );

        strategies.add( new ArtifactLocatorStrategy( factory, resolver, configSource.getLocalRepository(),
                                                     configSource.getRemoteRepositories(), "xml", "assembly-descriptor" ) );

        strategies.add( prefixedClasspathStrategy );
        strategies.add( classpathStrategy );
        strategies.add( new FileLocatorStrategy() );
        strategies.add( new URLLocatorStrategy() );

        List refStrategies = new ArrayList();
        refStrategies.add( prefixedClasspathStrategy );
        refStrategies.add( classpathStrategy );

        List assemblies = new ArrayList();

        String descriptor = configSource.getDescriptor();
        String descriptorId = configSource.getDescriptorId();
        String[] descriptors = configSource.getDescriptors();
        String[] descriptorRefs = configSource.getDescriptorReferences();
        File descriptorSourceDirectory = configSource.getDescriptorSourceDirectory();

        if ( descriptor != null )
        {
            locator.setStrategies( strategies );
            assemblies.add( getAssemblyFromDescriptor( descriptor, locator, configSource ) );
        }

        if ( descriptorId != null )
        {
            locator.setStrategies( refStrategies );
            assemblies.add( getAssemblyForDescriptorReference( descriptorId, configSource ) );
        }

        if ( ( descriptors != null ) && ( descriptors.length > 0 ) )
        {
            locator.setStrategies( strategies );
            for ( int i = 0; i < descriptors.length; i++ )
            {
                getLogger().info( "Reading assembly descriptor: " + descriptors[i] );
                assemblies.add( getAssemblyFromDescriptor( descriptors[i], locator, configSource ) );
            }
        }

        if ( ( descriptorRefs != null ) && ( descriptorRefs.length > 0 ) )
        {
            locator.setStrategies( refStrategies );
            for ( int i = 0; i < descriptorRefs.length; i++ )
            {
                assemblies.add( getAssemblyForDescriptorReference( descriptorRefs[i], configSource ) );
            }
        }

        if ( ( descriptorSourceDirectory != null ) && descriptorSourceDirectory.isDirectory() )
        {
            locator.setStrategies( Collections.singletonList( new RelativeFileLocatorStrategy( descriptorSourceDirectory ) ) );

            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( descriptorSourceDirectory );
            scanner.setIncludes( new String[]{ "**/*.xml" } );
            scanner.addDefaultExcludes();

            try
            {
                scanner.scan();
            }
            // FIXME: plexus-utils >= 1.3-SNAPSHOT should fix this.
            catch ( NullPointerException e )
            {
                StackTraceElement frameZero = e.getStackTrace()[0];

                if ( "org.codehaus.plexus.util.DirectoryScanner".equals( frameZero.getClassName() )
                                && "scandir".equals( frameZero.getMethodName() ) )
                {
                    if ( getLogger().isDebugEnabled() )
                    {
                        getLogger().debug(
                                           "Caught filesystem error while scanning directories..."
                                                           + "using zero-length list as the result.", e );
                    }
                }
                else
                {
                    throw e;
                }
            }

            String[] paths = scanner.getIncludedFiles();

            if ( paths != null )
            {
                for ( int i = 0; i < paths.length; i++ )
                {
                    assemblies.add( getAssemblyFromDescriptor( paths[i], locator, configSource ) );
                }
            }
        }

        if ( assemblies.isEmpty() )
        {
            throw new AssemblyReadException( "No assembly descriptors found." );
        }

        // check unique IDs
        Set ids = new HashSet();
        for ( Iterator i = assemblies.iterator(); i.hasNext(); )
        {
            Assembly assembly = (Assembly) i.next();
            if ( !ids.add( assembly.getId() ) )
            {
                getLogger().warn( "The assembly id " + assembly.getId() + " is used more than once." );
            }

        }
        return assemblies;
    }

    public Assembly getAssemblyForDescriptorReference( String ref, AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        InputStream resourceAsStream = getClass().getResourceAsStream( "/assemblies/" + ref + ".xml" );

        if ( resourceAsStream == null )
        {
            throw new AssemblyReadException( "Descriptor with ID '" + ref + "' not found" );
        }

        try
        {
            // TODO use ReaderFactory.newXmlReader() when plexus-utils is upgraded to 1.4.5+
            return readAssembly( new InputStreamReader( resourceAsStream, "UTF-8" ), ref, configSource );
        }
        catch ( UnsupportedEncodingException e )
        {
            // should not occur since UTF-8 support is mandatory
            throw new AssemblyReadException( "Encoding not supported for descriptor with ID '" + ref + "'" );
        }
    }

    public Assembly getAssemblyFromDescriptorFile( File descriptor, AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        Reader r = null;
        try
        {
            // TODO use ReaderFactory.newXmlReader() when plexus-utils is upgraded to 1.4.5+
            r = new InputStreamReader( new FileInputStream( descriptor ), "UTF-8" );
            return readAssembly( r, descriptor.getAbsolutePath(), configSource );
        }
        catch ( IOException e )
        {
            throw new AssemblyReadException( "Error reading assembly descriptor: " + descriptor, e );
        }
        finally
        {
            IOUtil.close( r );
        }
    }

    public Assembly getAssemblyFromDescriptor( String spec, Locator locator, AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        Location location = locator.resolve( spec );

        if ( location == null )
        {
            throw new AssemblyReadException( "Error locating assembly descriptor: " + spec + "\n\n" + locator.getMessageHolder().render() );
        }

        Reader r = null;
        try
        {
         // TODO use ReaderFactory.newXmlReader() when plexus-utils is upgraded to 1.4.5+
            r = new InputStreamReader( location.getInputStream(), "UTF-8" );
            return readAssembly( r, spec, configSource );
        }
        catch ( IOException e )
        {
            throw new AssemblyReadException( "Error reading assembly descriptor: " + spec, e );
        }
        finally
        {
            IOUtil.close( r );
        }

    }

    public Assembly readAssembly( Reader reader, String locationDescription, AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        Assembly assembly;

        File basedir = configSource.getBasedir();
        MavenProject project = configSource.getProject();

        try
        {
            Map context = new HashMap( System.getProperties() );

            context.put( "basedir", basedir.getAbsolutePath() );

            AssemblyXpp3Reader r = new AssemblyXpp3Reader();
            assembly = r.read( reader );

            mergeComponentsWithMainAssembly( assembly, configSource );

            debugPrintAssembly( "Before assembly is interpolated:", assembly );

            assembly = new AssemblyInterpolator().interpolate( assembly, project, context );

            debugPrintAssembly( "After assembly is interpolated:", assembly );
        }
        catch ( IOException e )
        {
            throw new AssemblyReadException( "Error reading descriptor at: " + locationDescription + ": " + e.getMessage(), e );
        }
        catch ( XmlPullParserException e )
        {
            throw new AssemblyReadException( "Error reading descriptor at: " + locationDescription + ": " + e.getMessage(), e );
        }
        catch ( AssemblyInterpolationException e )
        {
            throw new AssemblyReadException( "Error reading descriptor at: " + locationDescription + ": " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        if ( configSource.isSiteIncluded() || assembly.isIncludeSiteDirectory() )
        {
            includeSiteInAssembly( assembly, configSource );
        }

        return assembly;
    }

    private void debugPrintAssembly( String message, Assembly assembly )
    {
        StringWriter sWriter = new StringWriter();
        try
        {
            new AssemblyXpp3Writer().write( sWriter, assembly );
        }
        catch ( IOException e )
        {
            getLogger().debug( "Failed to print debug message with assembly descriptor listing, and message: " + message, e );
        }

        getLogger().debug( message + "\n\n" + sWriter.toString() + "\n\n" );
    }

    /**
     * Add the contents of all included components to main assembly
     *
     * @param assembly
     * @throws AssemblyReadException
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    protected void mergeComponentsWithMainAssembly( Assembly assembly, AssemblerConfigurationSource configSource )
        throws AssemblyReadException
    {
        RelativeFileLocatorStrategy fls = new RelativeFileLocatorStrategy( configSource.getBasedir() );

        ClasspathResourceLocatorStrategy crls = new ClasspathResourceLocatorStrategy();

        ArtifactLocatorStrategy als = new ArtifactLocatorStrategy( factory, resolver,
                                                                   configSource.getLocalRepository(),
                                                                   configSource.getRemoteRepositories(),
                                                                   "assembly-component" );

        URLLocatorStrategy uls = new URLLocatorStrategy();

        Locator locator = new Locator();
        locator.addStrategy( fls );
        locator.addStrategy( als );
        locator.addStrategy( crls );
        locator.addStrategy( uls );

        List componentLocations = assembly.getComponentDescriptors();

        for ( Iterator it = componentLocations.iterator(); it.hasNext(); )
        {
            String location = (String) it.next();

            Location resolvedLocation = locator.resolve( location );

            Component component = null;
            Reader reader = null;
            try
            {
                reader = new InputStreamReader( resolvedLocation.getInputStream() );
                component = new ComponentXpp3Reader().read( reader );
            }
            catch ( IOException e )
            {
                throw new AssemblyReadException( "Error reading component descriptor", e );
            }
            catch ( XmlPullParserException e )
            {
                throw new AssemblyReadException( "Error reading component descriptor", e );
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
     * @param component
     * @param assembly
     */
    protected void mergeComponentWithAssembly( Component component, Assembly assembly )
    {
        List containerHandlerDescriptors = component.getContainerDescriptorHandlers();

        for ( Iterator it = containerHandlerDescriptors.iterator(); it.hasNext(); )
        {
            ContainerDescriptorHandlerConfig cfg = (ContainerDescriptorHandlerConfig) it.next();
            assembly.addContainerDescriptorHandler( cfg );
        }

        List dependencySetList = component.getDependencySets();

        for ( Iterator it = dependencySetList.iterator(); it.hasNext(); )
        {
            DependencySet dependencySet = ( DependencySet ) it.next();
            assembly.addDependencySet( dependencySet );
        }

        List fileSetList = component.getFileSets();

        for ( Iterator it = fileSetList.iterator(); it.hasNext(); )
        {
            FileSet fileSet = ( FileSet ) it.next();

            assembly.addFileSet( fileSet );
        }

        List fileList = component.getFiles();

        for ( Iterator it = fileList.iterator(); it.hasNext(); )
        {
            FileItem fileItem = ( FileItem ) it.next();

            assembly.addFile( fileItem );
        }

        List repositoriesList = component.getRepositories();

        for ( Iterator it = repositoriesList.iterator(); it.hasNext(); )
        {
            Repository repository = ( Repository ) it.next();

            assembly.addRepository( repository );
        }
    }

    public void includeSiteInAssembly( Assembly assembly, AssemblerConfigurationSource configSource )
        throws InvalidAssemblerConfigurationException
    {
        File siteDirectory = configSource.getSiteDirectory();

        if ( !siteDirectory.exists() )
        {
            throw new InvalidAssemblerConfigurationException(
                "site did not exist in the target directory - please run site:site before creating the assembly" );
        }

        getLogger().info( "Adding site directory to assembly : " + siteDirectory );

        FileSet siteFileSet = new FileSet();

        siteFileSet.setDirectory( siteDirectory.getPath() );

        siteFileSet.setOutputDirectory( "/site" );

        assembly.addFileSet( siteFileSet );
    }

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
