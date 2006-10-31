package org.apache.maven.plugin.eclipse.writers.rad;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.eclipse.writers.AbstractEclipseWriter;
import org.apache.maven.plugin.eclipse.writers.AbstractWtpResourceWriter;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Create or adapt the manifest files for the RAD6 runtime dependencys.
 * attention these will not be used for the real ear these are just to get the
 * runtime enviorment using the maven dependencys.
 * 
 * @author <a href="mailto:nir@cfc.at">Richard van Nieuwenhoven </a>
 */
public class RadManifestWriter
    extends AbstractEclipseWriter
{

    private static final String MANIFEST_MF_FILENAME = "MANIFEST.MF";

    private static final String META_INF_DIRECTORY = "META-INF";

    private static final String WEBAPP_RESOURCE_DIR = "src" + File.separatorChar + "main" + File.separatorChar
        + "webapp";

    /**
     * Search the project for the existing META-INF directory where the manifest
     * should be located.
     * 
     * @return the apsolute path to the META-INF directory
     */
    public String getMetaInfBaseDirectory( MavenProject project )
    {
        String metaInfBaseDirectory = null;
        Iterator iterator = project.getResources().iterator();
        while ( iterator.hasNext() )
        {
            metaInfBaseDirectory = ( (Resource) iterator.next() ).getDirectory();
            
            File metaInfDirectoryFile = new File( metaInfBaseDirectory + File.separatorChar + META_INF_DIRECTORY );
            
            if ( !metaInfDirectoryFile.isDirectory() )
            {
                metaInfBaseDirectory = null;
            }
        }
        
        if ( metaInfBaseDirectory == null && config.getProject().getPackaging().equals( Constants.PROJECT_PACKAGING_WAR ) )
        {
            metaInfBaseDirectory = config.getProject().getBasedir().getAbsolutePath() + File.separatorChar + WEBAPP_RESOURCE_DIR;
            
            if ( !new File( metaInfBaseDirectory + File.separatorChar + META_INF_DIRECTORY ).exists() )
            {
                metaInfBaseDirectory = null;
            }
        }
        
        return metaInfBaseDirectory;
    }

    /**
     * Write the manifest files use an existing one it it exists (it will be
     * overwritten!! in a war use webapp/META-INF else use the generated rad6
     * sourcefolder
     * 
     * @see AbstractWtpResourceWriter#write(EclipseSourceDir[],
     *      ArtifactRepository, File)
     * @param sourceDirs
     *            all eclipse source directorys
     * @param localRepository
     *            the local reposetory
     * @param buildOutputDirectory
     *            build output directory (target)
     * @throws MojoExecutionException
     *             when writing the config files was not possible
     */
    public void write()
        throws MojoExecutionException
    {
        String metaInfBaseDirectory = getMetaInfBaseDirectory( config.getProject() );
        
        if ( metaInfBaseDirectory == null )
        {
            // TODO: if this really is an error, shouldn't we stop the build??
            log.error( Messages.getString( "EclipseCleanMojo.nofilefound", new Object[] { META_INF_DIRECTORY } ) );
            return;
        }
        
        Manifest manifest = createNewManifest();

        File manifestFile = new File( metaInfBaseDirectory + File.separatorChar + META_INF_DIRECTORY + File.separatorChar + MANIFEST_MF_FILENAME );

        if ( shouldNewManifestFileBeWritten( manifest, manifestFile ) )
        {
            try
            {
                FileOutputStream stream = new FileOutputStream( manifestFile );
                
                manifest.write( stream );
                
                stream.close();
                
                verifyManifestBasedirInSourceDirs( metaInfBaseDirectory );
                
            }
            catch ( Exception e )
            {
                log.error( Messages.getString( "EclipsePlugin.cantwritetofile", new Object[] { metaInfBaseDirectory
                    + File.separatorChar + MANIFEST_MF_FILENAME } ) );
            }

        }
    }

    // NOTE: This could change the config!
    private void verifyManifestBasedirInSourceDirs( String metaInfBaseDirectory )
    {
        EclipseSourceDir[] sourceDirs = config.getSourceDirs();
        
        if ( sourceDirs != null )
        {
            boolean foundMetaInfBaseDirectory = false;
            
            for ( int i = 0; i < sourceDirs.length; i++ )
            {
                EclipseSourceDir esd = sourceDirs[i];
                
                if ( esd.getPath().equals( metaInfBaseDirectory ) )
                {
                    foundMetaInfBaseDirectory = true;
                    break;
                }
            }
            
            if ( !foundMetaInfBaseDirectory )
            {
                EclipseSourceDir dir = new EclipseSourceDir( metaInfBaseDirectory, null, true, false, null, null );
                
                EclipseSourceDir[] newSourceDirs = new EclipseSourceDir[ sourceDirs.length + 1 ];
                newSourceDirs[ sourceDirs.length ] = dir;
                
                System.arraycopy( sourceDirs, 0, newSourceDirs, 0, sourceDirs.length );
                
                config.setSourceDirs( newSourceDirs );
            }
        }
    }

    /**
     * Add one dependency to the black seperated classpath stringbuffer. Wenn
     * the project is available in the reactor (current build) then the project
     * is used else the jar representing the artifact.
     * 
     * @param classpath
     *            existing classpath to append
     * @param dependency
     *            dependency to append as jar or as project
     */
    private void addDependencyToClassPath( StringBuffer classpath, IdeDependency dependency )
    {
        if ( !dependency.isTestDependency() && !dependency.isProvided() )
        {
            // blank is the separetor in manifest classpaths
            if ( classpath.length() != 0 )
            {
                classpath.append( ' ' );
            }
            // if the dependency is a workspace project add the project and not
            // the jar
            if ( !dependency.isReferencedProject() )
            {
                classpath.append( dependency.getFile().getName() );
            }
            else
            {
                classpath.append( dependency.getArtifactId() + ".jar" );
            }
        }
    }

    /**
     * Check if the two manifests are equal. Manifest.equal can not be used
     * because of the special case the Classpath entr, witch must be comaired
     * sorted so that a different oder in the classpath does not result in "not
     * equal". This not not realy correct but in this case it is more important
     * to reduce the number of version-controll files.
     * 
     * @param manifest
     *            the new manifest
     * @param existingManifest
     *            to compaire the new one with
     * @return are the manifests equal
     */
    private boolean areManifestsEqual( Manifest manifest, Manifest existingManifest )
    {
        Set keys = new HashSet();
        Attributes existingMap = existingManifest.getMainAttributes();
        Attributes newMap = manifest.getMainAttributes();
        keys.addAll( existingMap.keySet() );
        keys.addAll( newMap.keySet() );
        Iterator iterator = keys.iterator();
        while ( iterator.hasNext() )
        {
            Attributes.Name key = (Attributes.Name) iterator.next();
            String newValue = (String) newMap.get( key );
            String existingValue = (String) existingMap.get( key );
            // special case classpath... they are qual when there entries
            // are equal
            if ( Attributes.Name.CLASS_PATH.equals( key ) )
            {
                newValue = orderClasspath( newValue );
                existingValue = orderClasspath( existingValue );
            }
            if ( ( newValue == null || !newValue.equals( existingValue ) )
                && ( existingValue == null || !existingValue.equals( newValue ) ) )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Convert all dependencies in a blank seperated list of jars and projects
     * representing the classpath.
     * 
     * @return the blank separeted classpath string
     */
    private String constructManifestClasspath()
    {
        StringBuffer stringBuffer = new StringBuffer();
        IdeDependency[] deps = config.getDeps();
        
        for ( int index = 0; index < deps.length; index++ )
        {
            addDependencyToClassPath( stringBuffer, deps[index] );
        }
        
        return stringBuffer.toString();
    }

    /**
     * Create a manifest contaigning the required classpath.
     * 
     * @return the newly created manifest
     */
    private Manifest createNewManifest()
    {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put( Attributes.Name.MANIFEST_VERSION, "1.0" );
        manifest.getMainAttributes().put( Attributes.Name.CLASS_PATH, constructManifestClasspath() );
        return manifest;
    }

    /**
     * Aphabeticaly sort the classpath. Do this by splitting it up, sort the
     * entries and gleue them together again.
     * 
     * @param newValue
     *            classpath to sort
     * @return the sorted classpath
     */
    private String orderClasspath( String newValue )
    {
        String[] entries = newValue.split( " " );
        Arrays.sort( entries );
        StringBuffer buffer = new StringBuffer( newValue.length() );
        for ( int index = 0; index < entries.length; index++ )
        {
            buffer.append( entries[index] );
            buffer.append( ' ' );
        }
        return buffer.toString();
    }

    /**
     * Read and parse the existing manifest file.
     * 
     * @param manifestFile
     *            file
     * @return the read manifest
     * @throws IOException
     *             if the file could not be read
     */
    private Manifest readExistingManifest( File manifestFile )
        throws IOException
    {
        Manifest existingManifest = new Manifest();
        FileInputStream inputStream = new FileInputStream( manifestFile );
        existingManifest.read( inputStream );
        inputStream.close();
        return existingManifest;
    }

    /**
     * Verify is the manifest sould be overwritten this sould take in account
     * that the manifest should only be written if the contents of the classpath
     * was changed not the order. The classpath sorting oder should be ignored.
     * 
     * @param manifest
     *            the newly created classpath
     * @param manifestFile
     *            the file where the manifest
     * @return if the new manifest file must be written
     */
    private boolean shouldNewManifestFileBeWritten( Manifest manifest, File manifestFile )
    {
        try
        {
            Manifest existingManifest = readExistingManifest( manifestFile );
            if ( areManifestsEqual( manifest, existingManifest ) )
            {
                log.info( Messages.getString( "EclipseCleanMojo.unchanged", manifestFile.getAbsolutePath() ) );
                return false;
            }
        }
        catch ( Exception e )
        {
            log.debug( Messages.getString( "EclipseCleanMojo.nofilefound", manifestFile.getAbsolutePath() ) );
        }
        return true;
    }
}
