package org.apache.maven.plugin.assembly.archive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.AbstractArchiveFinalizer;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.util.IOUtil;

public class ManifestCreationFinalizer
    extends AbstractArchiveFinalizer
{

    private final MavenProject project;
    private final MavenArchiveConfiguration archiveConfiguration;

    // TODO: I'd really prefer to rewrite MavenArchiver as either a
    // separate manifest creation utility (and to
    // create an include pom.properties etc into another archiver), or
    // an implementation of an archiver
    // (the first is preferable).
    private MavenArchiver mavenArchiver = new MavenArchiver();

    public ManifestCreationFinalizer( MavenProject project, MavenArchiveConfiguration archiveConfiguration )
    {
        this.project = project;
        this.archiveConfiguration = archiveConfiguration;
    }

    public void finalizeArchiveCreation( Archiver archiver )
        throws ArchiverException
    {
        if ( archiveConfiguration != null )
        {
            try
            {
                Manifest manifest;
                File manifestFile = archiveConfiguration.getManifestFile();

                if ( manifestFile != null )
                {
                    FileReader manifestFileReader = null;
                    try
                    {
                        manifestFileReader = new FileReader( manifestFile );
                        manifest = new Manifest( manifestFileReader );
                    }
                    catch ( FileNotFoundException e )
                    {
                        throw new ArchiverException( "Manifest not found: " + e.getMessage(), e );
                    }
                    catch ( IOException e )
                    {
                        throw new ArchiverException( "Error processing manifest: " + e.getMessage(), e );
                    }
                    finally
                    {
                        IOUtil.close( manifestFileReader );
                    }
                }
                else
                {
                    manifest = mavenArchiver.getManifest( project, archiveConfiguration );
                }

                if ( ( manifest != null ) && ( archiver instanceof JarArchiver ) )
                {
                    JarArchiver jarArchiver = (JarArchiver) archiver;
                    jarArchiver.addConfiguredManifest( manifest );
                }
            }
            catch ( ManifestException e )
            {
                throw new ArchiverException( "Error creating manifest: " + e.getMessage(), e );
            }
            catch ( DependencyResolutionRequiredException e )
            {
                throw new ArchiverException( "Dependencies were not resolved: " + e.getMessage(), e );
            }
        }
    }

    public List getVirtualFiles()
    {
        if ( archiveConfiguration != null )
        {
            try
            {
                if ( mavenArchiver.getManifest( project, archiveConfiguration.getManifest() ) != null )
                {
                    return Collections.singletonList( "META-INF/MANIFEST.MF" );
                }
            }
            catch ( ManifestException e )
            {
            }
            catch ( DependencyResolutionRequiredException e )
            {
            }
        }

        return null;
    }

}
