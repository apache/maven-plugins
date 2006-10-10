package org.apache.maven.plugin.dependency.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Utility class with static helper methods
 * 
 * @author brianf
 * 
 */
public class DependencyUtil
{
    /**
     * Does the actual copy of the file and logging.
     * 
     * @param artifact
     *            represents the file to copy.
     * @param destFile
     *            file name of destination file.
     * @param log
     *            to use for output.
     * @param overWriteSnapshots
     *            force Over Writing of the file
     * 
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     */
    public static boolean copyFile( File artifact, File destFile, Log log, boolean overWrite )
        throws MojoExecutionException
    {
        boolean result = false;
        if ( !destFile.exists() || overWrite )
        {
            try
            {
                log.info( "Copying " + artifact.getAbsolutePath() + " to " + destFile );
                FileUtils.copyFile( artifact, destFile );
                result = true;
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( "Error copying artifact from " + artifact + " to " + destFile, e );
            }
        }
        else
        {
            log.info( artifact.getName() + " already exists." );
        }

        return result;
    }

    /**
     * Unpacks the archive file, checking for a marker file to see if it should
     * unpack again. Creates the marker file after unpacking.
     * 
     * @param Artifact
     *            File to be unpacked.
     * @param unpackDirectory
     *            Location where to put the unpacked files.
     */
    public static void unpackFile( Artifact artifact, File unpackDirectory, File markersDirectory,
                                  ArchiverManager archiverManager, Log log, boolean overWrite )
        throws MojoExecutionException
    {
        markersDirectory.mkdirs();

        File markerFile = new File( markersDirectory, artifact.getId().replace( ':', '-' ) + ".unpacked" );

        if ( !markerFile.exists() || overWrite )
        {
            try
            {
                unpackDirectory.mkdirs();

                unpack( artifact.getFile(), unpackDirectory, archiverManager, log );

                // create marker file
                markerFile.getParentFile().mkdirs();
                markerFile.createNewFile();
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error creating marker file: " + markerFile );
            }
        }
        else
        {
            log.info( artifact.getFile().getName() + " already unpacked." );
        }
    }

    /**
     * Unpacks the archive file.
     * 
     * @param file
     *            File to be unpacked.
     * @param location
     *            Location where to put the unpacked files.
     */
    private static void unpack( File file, File location, ArchiverManager archiverManager, Log log )
        throws MojoExecutionException
    {

        try
        {
            UnArchiver unArchiver;

            unArchiver = archiverManager.getUnArchiver( file );

            unArchiver.setSourceFile( file );

            unArchiver.setDestDirectory( location );

            unArchiver.extract();
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "Unknown archiver type", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error unpacking file: " + file + "to: " + location, e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error unpacking file: " + file + "to: " + location, e );
        }
    }

    /**
     * Builds the file name. If removeVersion is set, then the file name must be
     * reconstructed from the artifactId, Classifier (if used) and Type.
     * Otherwise, this method returns the artifact file name.
     * 
     * @param artifact
     *            File to be formatted.
     * @param removeVersion
     *            Specifies if the version should be removed from the file name.
     * @return Formatted file name in the format
     *         artifactId-[classifier-][version].[type]
     */
    public static String getFormattedFileName( Artifact artifact, boolean removeVersion )
    {
        String destFileName = null;
        if ( !removeVersion )
        {
            File file = artifact.getFile();
            if ( file != null )
            {
                destFileName = file.getName();
            }
            // so it can be used offline
            else
            {
                if ( StringUtils.isNotEmpty( artifact.getClassifier() ) )
                {
                    destFileName = artifact.getArtifactId() + "-" + artifact.getClassifier() + "-"
                        + artifact.getVersion() + "." + artifact.getType();
                }
                else
                {
                    destFileName = artifact.getArtifactId() + "-" + artifact.getVersion() + "." + artifact.getType();
                }
            }

        }
        else
        {
            if ( artifact.getClassifier() != null )
            {
                destFileName = artifact.getArtifactId() + "-" + artifact.getClassifier() + "." + artifact.getType();
            }
            else
            {
                destFileName = artifact.getArtifactId() + "." + artifact.getType();
            }
        }
        return destFileName;
    }

    /**
     * Formats the outputDirectory based on type.
     * 
     * @param useSubdirsPerType
     *            if a new sub directory should be used for each type.
     * @param useSubdirsPerArtifact
     *            if a new sub directory should be used for each artifact.
     * @param outputDirectory
     *            base outputDirectory.
     * @param artifact
     *            information about the artifact.
     * 
     * @return a formatted File object to use for output.
     */
    public static File getFormattedOutputDirectory( boolean useSubdirsPerType, boolean useSubdirPerArtifact,
                                                   File outputDirectory, Artifact artifact )
    {
        File result = null;

        // get id but convert the chars so it's safe as a folder name.
        String artifactId = artifact.getId().replace( ':', '-' );
        if ( !useSubdirsPerType )
        {
            if ( useSubdirPerArtifact )
            {

                result = new File( outputDirectory.getAbsolutePath() + File.separatorChar + artifactId
                    + File.separatorChar );
            }
            else
            {
                result = outputDirectory;
            }
        }
        else
        {
            if ( useSubdirPerArtifact )
            {
                result = new File( outputDirectory.getAbsolutePath() + File.separatorChar + artifact.getType() + "s"
                    + File.separatorChar + artifactId + File.separatorChar );
            }
            else
            {
                result = new File( outputDirectory.getAbsolutePath() + File.separatorChar + artifact.getType() + "s"
                    + File.separatorChar );
            }
        }

        return result;

    }

    /**
     * This method resolves the plugin artifacts from the project.
     * 
     * @param project
     *            The POM.
     * @param artifactFactory
     *            component to build artifact objects.
     * @param localRepository
     *            where to resolve artifacts.
     * @param remotePluginRepositories
     *            list of remote repositories used to resolve plugins.
     * @param artifactResolver
     *            component used to resolve artifacts.
     * 
     * @return set of resolved plugin artifacts.
     * 
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     */
    public static Set resolvePluginArtifacts( MavenProject project, ArtifactFactory artifactFactory,
                                             ArtifactRepository localRepository, List remotePluginRepositories,
                                             ArtifactResolver artifactResolver )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        Set plugins = project.getPluginArtifacts();
        Set reports = project.getReportArtifacts();

        Set artifacts = new HashSet();
        artifacts.addAll( reports );
        artifacts.addAll( plugins );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            // resolve the new artifact
            artifactResolver.resolve( artifact, remotePluginRepositories, localRepository );
        }
        return artifacts;
    }

    /**
     * This method resolves the dependency artifacts from the project.
     * 
     * @param project
     *            The POM.
     * @param artifactFactory
     *            component to build artifact objects.
     * @param localRepository
     *            where to resolve artifacts.
     * @param remoteArtifactRepositories
     *            list of remote repositories used to resolve artifacts.
     * @param artifactResolver
     *            component used to resolve artifacts.
     * @return resolved set of dependency artifacts.
     * 
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     * @throws InvalidDependencyVersionException
     */
    public static Set resolveDependencyArtifacts( MavenProject project, ArtifactFactory artifactFactory,
                                                 ArtifactRepository localRepository, List remoteArtifactRepositories,
                                                 ArtifactResolver artifactResolver )
        throws ArtifactResolutionException, ArtifactNotFoundException, InvalidDependencyVersionException
    {
        Set artifacts = project.createArtifacts( artifactFactory, Artifact.SCOPE_TEST,
                                                 new ScopeArtifactFilter( Artifact.SCOPE_TEST ) );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            // resolve the new artifact
            artifactResolver.resolve( artifact, remoteArtifactRepositories, localRepository );
        }
        return artifacts;
    }

    /**
     * This method resolves all transitive dependencies of an artifact.
     * 
     * @param artifact
     *            the artifact used to retrieve dependencies
     * @param artifactFactory
     *            component to build artifact objects.
     * @param localRepository
     *            where to resolve artifacts.
     * @param remoteArtifactRepositories
     *            list of remote repositories used to resolve artifacts.
     * @param artifactResolver
     *            component used to resolve artifacts.
     * @param mavenProjectBuilder
     *            component used to build a pom artifact.
     * 
     * @return resolved set of dependencies
     * 
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     * @throws ProjectBuildingException
     * @throws InvalidDependencyVersionException
     */
    public static Set resolveArtifactDependencies( Artifact artifact, ArtifactFactory artifactFactory,
                                                  ArtifactRepository localRepository, List remoteArtifactRepositories,
                                                  ArtifactResolver artifactResolver,
                                                  MavenProjectBuilder mavenProjectBuilder )
        throws ArtifactResolutionException, ArtifactNotFoundException, ProjectBuildingException,
        InvalidDependencyVersionException
    {
        Artifact pomArtifact = artifactFactory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                               artifact.getVersion(), "", "pom" );

        MavenProject pomProject = mavenProjectBuilder.buildFromRepository( pomArtifact, remoteArtifactRepositories,
                                                                           localRepository );

        return resolveDependencyArtifacts( pomProject, artifactFactory, localRepository, remoteArtifactRepositories,
                                           artifactResolver );
    }

}
