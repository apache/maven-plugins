package org.apache.maven.plugin.assembly.archive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

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

public class ManifestCreationFinalizer
    extends AbstractArchiveFinalizer
{
    
    private final MavenProject project;
    private final MavenArchiveConfiguration archiveConfiguration;

    public ManifestCreationFinalizer( MavenProject project, MavenArchiveConfiguration archiveConfiguration )
    {
        this.project = project;
        this.archiveConfiguration = archiveConfiguration;
    }

    public void finalizeArchiveCreation( Archiver archiver )
        throws ArchiverException
    {
        // TODO: I'd really prefer to rewrite MavenArchiver as either a
        // separate manifest creation utility (and to
        // create an include pom.properties etc into another archiver), or
        // an implementation of an archiver
        // (the first is preferable).
        MavenArchiver mavenArchiver = new MavenArchiver();

        if ( archiveConfiguration != null )
        {
            try
            {
                Manifest manifest;
                File manifestFile = archiveConfiguration.getManifestFile();

                if ( manifestFile != null )
                {
                    try
                    {
                        manifest = new Manifest( new FileReader( manifestFile ) );
                    }
                    catch ( FileNotFoundException e )
                    {
                        throw new ArchiverException( "Manifest not found: " + e.getMessage() );
                    }
                    catch ( IOException e )
                    {
                        throw new ArchiverException( "Error processing manifest: " + e.getMessage(), e );
                    }
                }
                else
                {
                    manifest = mavenArchiver.getManifest( project, archiveConfiguration.getManifest() );
                }

                if ( manifest != null && ( archiver instanceof JarArchiver ) )
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

}
