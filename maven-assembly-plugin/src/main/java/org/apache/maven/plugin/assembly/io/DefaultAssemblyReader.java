package org.apache.maven.plugin.assembly.io;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.interpolation.AssemblyInterpolationException;
import org.apache.maven.plugin.assembly.interpolation.AssemblyInterpolator;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.Component;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.FileItem;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.plugins.assembly.model.Repository;
import org.apache.maven.plugins.assembly.model.io.xpp3.AssemblyXpp3Reader;
import org.apache.maven.plugins.assembly.model.io.xpp3.ComponentXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @plexus.component role="org.apache.maven.plugin.assembly.io.AssemblyReader" role-hint="default"
 */
public class DefaultAssemblyReader
    extends AbstractLogEnabled
    implements AssemblyReader
{

    public List readAssemblies( AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        List assemblies = new ArrayList();

        File descriptor = configSource.getDescriptor();
        String descriptorId = configSource.getDescriptorId();
        File[] descriptors = configSource.getDescriptors();
        String[] descriptorRefs = configSource.getDescriptorReferences();
        File descriptorSourceDirectory = configSource.getDescriptorSourceDirectory();

        if ( descriptor != null )
        {
            assemblies.add( getAssemblyFromDescriptorFile( descriptor, configSource ) );
        }

        if ( descriptorId != null )
        {
            assemblies.add( getAssemblyForDescriptorReference( descriptorId, configSource ) );
        }

        if ( descriptors != null && descriptors.length > 0 )
        {
            for ( int i = 0; i < descriptors.length; i++ )
            {
                assemblies.add( getAssemblyFromDescriptorFile( descriptors[i], configSource ) );
            }
        }

        if ( descriptorRefs != null && descriptorRefs.length > 0 )
        {
            for ( int i = 0; i < descriptorRefs.length; i++ )
            {
                assemblies.add( getAssemblyForDescriptorReference( descriptorRefs[i], configSource ) );
            }
        }

        if ( descriptorSourceDirectory != null && descriptorSourceDirectory.isDirectory() )
        {
            try
            {
                List descriptorList;

                try
                {
                    descriptorList = FileUtils.getFiles( descriptorSourceDirectory, "**/*.xml", null );
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

                        descriptorList = Collections.EMPTY_LIST;
                    }
                    else
                    {
                        throw e;
                    }
                }

                for ( Iterator iter = descriptorList.iterator(); iter.hasNext(); )
                {
                    assemblies.add( getAssemblyFromDescriptorFile( (File) iter.next(), configSource ) );
                }
            }
            catch ( IOException e )
            {
                throw new AssemblyReadException( "error discovering descriptor files: " + e.getMessage() );
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
        
        return readAssembly( new InputStreamReader( resourceAsStream ), ref, configSource );
    }

    public Assembly getAssemblyFromDescriptorFile( File file, AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        Reader r = null;
        try
        {
            r = new FileReader( file );
            return readAssembly( r, file.getAbsolutePath(), configSource );
        }
        catch ( FileNotFoundException e )
        {
            throw new AssemblyReadException( "Error locating assembly descriptor file: " + file, e );
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

            assembly = new AssemblyInterpolator().interpolate( assembly, project.getModel(), context );
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

        mergeComponentsWithMainAssembly( assembly, configSource );
        
        return assembly;
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
        List componentDescriptorFiles = assembly.getComponentDescriptors();

        for ( int i = 0; i < componentDescriptorFiles.size(); ++i )
        {
            Component component = getComponentFromFile( componentDescriptorFiles.get( i ).toString(), configSource );

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

    /**
     * Load the Component via a given file path relative to ${basedir}
     * 
     * @param filePath
     * @return
     * @throws AssemblyReadException 
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    protected Component getComponentFromFile( String filePath, AssemblerConfigurationSource configSource ) throws AssemblyReadException
    {
        File basedir = configSource.getBasedir();

        File componentDescriptor = new File( basedir, filePath );

        Reader r;
        try
        {
            r = new FileReader( componentDescriptor );
        }
        catch ( FileNotFoundException e )
        {
            throw new AssemblyReadException( "Unable to find descriptor: " + e.getMessage() );
        }

        return readComponent( r );

    }

    /**
     * Load the Component via a Reader
     * 
     * @param reader
     * @throws AssemblyReadException 
     */
    protected Component readComponent( Reader reader )
        throws AssemblyReadException
    {
        Component component;
        try
        {
            ComponentXpp3Reader r = new ComponentXpp3Reader();
            component = r.read( reader );
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

        return component;
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
