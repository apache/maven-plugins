package org.apache.maven.plugin.eclipse;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Install plugins resolved from the Maven repository system into an Eclipse instance.
 * 
 * @goal install-plugins
 * @author jdcasey
 * @requiresDependencyResolution compile
 */
public class InstallPluginsMojo
    extends AbstractMojo
{

    /**
     * Set this property in a plugin POM's &lt;properties/&gt; section to determine whether that
     * plugin should be expanded during installation, or left as a jar file.
     */
    public static final String PROP_UNPACK_PLUGIN = "eclipse.unpack";

    /**
     * This is the installed base directory of the Eclipse instance you want to modify.
     * 
     * @parameter expression="${eclipseDir}"
     */
    private File eclipseDir;

    /**
     * Determines whether this mojo leaves existing installed plugins as-is, or overwrites them.
     * 
     * @parameter expression="${overwrite}" default-value="false"
     */
    private boolean overwrite;

    /**
     * The list of resolved dependencies from the current project. Since we're not resolving the
     * dependencies by hand here, the build will fail if some of these dependencies do not resolve.
     * 
     * @parameter default-value="${project.artifacts}"
     * @required
     * @readonly
     */
    private Collection artifacts;

    /**
     * Comma-delimited list of dependency &lt;type/&gt; values which will be installed in the eclipse
     * instance's plugins directory.
     * 
     * @parameter expression="${pluginDependencyTypes}" default-value="jar"
     */
    private String pluginDependencyTypes;

    /**
     * The location of the Maven local repository, from which to install resolved dependency plugins.
     * 
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * Used to retrieve the project metadata (POM) associated with each plugin dependency, to help
     * determine whether that plugin should be installed as a jar, or expanded into a directory.
     * 
     * @component
     */
    private MavenProjectBuilder projectBuilder;

    /**
     * Used to configure and retrieve an appropriate tool for extracting each resolved plugin 
     * dependency. It is conceivable that some resolved dependencies could be zip files, jar files,
     * or other types, so the manager approach is a convenient way to provide extensibility here.
     * 
     * @component
     */
    private ArchiverManager archiverManager;

    /**
     * Input handler, needed for comand line handling.
     * @component
     */
    private InputHandler inputHandler;

    // calculated below. Value will be ${eclipseDir}/plugins.
    private File pluginsDir;

    public InstallPluginsMojo()
    {
        //used for plexus init.
    }

    // used primarily for testing.
    protected InstallPluginsMojo( File eclipseDir, boolean overwrite, List dependencyArtifacts,
                                  String pluginDependencyTypes, ArtifactRepository localRepository,
                                  MavenProjectBuilder projectBuilder, ArchiverManager archiverManager,
                                  InputHandler inputHandler, Log log )
    {
        this.eclipseDir = eclipseDir;
        this.overwrite = overwrite;
        this.artifacts = dependencyArtifacts;
        this.pluginDependencyTypes = pluginDependencyTypes;
        this.localRepository = localRepository;
        this.projectBuilder = projectBuilder;
        this.archiverManager = archiverManager;
        this.inputHandler = inputHandler;
        setLog( log );
    }

    /**
     * Traverse the list of resolved dependency artifacts. For each one having a type that is listed
     * in the pluginDependencyTypes parameter value, resolve the associated project metadata (POM),
     * and perform install(..) on that artifact.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( eclipseDir == null )
        {
            getLog().info( "Eclipse directory? " );

            String eclipseDirString;
            try
            {
                eclipseDirString = inputHandler.readLine();
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Unable to read from standard input", e );
            }

            eclipseDir = new File( eclipseDirString );
        }

        if ( !eclipseDir.exists() || !eclipseDir.isDirectory() )
        {
            throw new MojoFailureException( "Invalid Eclipse directory: " + eclipseDir );
        }

        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            if ( pluginDependencyTypes.indexOf( artifact.getType() ) > -1 )
            {
                getLog().debug( "Processing Eclipse plugin dependency: " + artifact.getId() );

                MavenProject project;

                try
                {
                    project = projectBuilder.buildFromRepository( artifact, Collections.EMPTY_LIST, localRepository,
                                                                  true );
                }
                catch ( ProjectBuildingException e )
                {
                    throw new MojoExecutionException( "Failed to load project metadata (POM) for: " + artifact.getId(),
                                                      e );
                }

                install( artifact, project );
            }
            else
            {
                getLog()
                    .debug(
                            "Skipping dependency: " + artifact.getId()
                                + ". Set pluginDependencyTypes with a comma-separated list of types to change this." );
            }
        }
    }

    /**
     * <p>Install the plugin into the eclipse instance's /plugins directory</p>
     * 
     * <ol>
     * <li>Determine whether the plugin should be extracted into a directory or not</li>
     * <li>If the plugin's target location exists, or overwrite is set to true:
     * <ol type="a">
     *   <li>if extract, ensure the plugin target location exists (mkdirs), and extract there.</li>
     *   <li>copy the plugin file from the local repository to the target location</li>
     * </ol>
     * 
     * <p>
     * Warn whenever a plugin will overwrite an existing file or directory, and emit an INFO message
     * whenever a plugin installation is skipped because of an existing file and overwrite == false.
     * </p>
     * 
     * @param artifact The plugin dependency as it has been resolved.
     * @param project The project metadata for the accompanying plugin-dependency artifact, used to
     * determine whether to install as a jar or as a directory
     * 
     * @throws MojoExecutionException In the event the plugin should be extracted but cannot, or the
     * file copy fails (in the event it should not be extracted)
     * @throws MojoFailureException In the event that the plugins target directory (inside the 
     * Eclipse instance directory) does not exist, or is not a directory.
     */
    private void install( Artifact artifact, MavenProject project )
        throws MojoExecutionException, MojoFailureException
    {
        if ( pluginsDir == null )
        {
            pluginsDir = new File( eclipseDir, "plugins" );
        }

        if ( !pluginsDir.exists() || !pluginsDir.isDirectory() )
        {
            throw new MojoFailureException( "Invalid Eclipse directory: " + eclipseDir
                + " (plugins directory is missing or not a directory)." );
        }

        boolean installAsJar = true;

        Properties properties = project.getProperties();
        if ( properties != null )
        {
            installAsJar = !Boolean.valueOf( properties.getProperty( PROP_UNPACK_PLUGIN, "false" ) ).booleanValue();
        }

        Attributes attributes = null;
        try
        {
            JarFile jar = new JarFile( artifact.getFile() );
            Manifest manifest = jar.getManifest();
            attributes = manifest.getMainAttributes();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to read manifest of plugin "
                + artifact.getFile().getAbsolutePath(), e );
        }

        String bundleVersion = attributes.getValue( "Bundle-Version" );
        String pluginName = formatEclipsePluginName( artifact, bundleVersion );

        File pluginFile = new File( pluginsDir, pluginName + ".jar" );
        File pluginDir = new File( pluginsDir, pluginName );

        boolean skipped = true;

        /* check if artifact is an OSGi bundle and ignore if not */
        Object bundleName = attributes.getValue( "Bundle-Name" );
        if ( bundleName == null )
        {
            getLog().debug(
                            "Ignoring " + artifact.getArtifactId()
                                + " as it is not an OSGi bundle (no Bundle-Name in manifest)" );
            return;
        }

        if ( overwrite )
        {
            if ( pluginFile.exists() || pluginDir.exists() )
            {
                getLog().warn( "Overwriting old plugin with contents of: " + artifact.getId() );

                getLog().debug( "Removing old plugin from both: " + pluginFile + " and: " + pluginDir );

                try
                {
                    FileUtils.forceDelete( pluginDir );
                    FileUtils.forceDelete( pluginFile );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Failed to remove old plugin from: " + pluginFile + " or: "
                        + pluginDir, e );
                }

                getLog().debug( "Removal of old plugin is complete; proceeding with plugin installation." );
            }

            performFileOperations( installAsJar, artifact, pluginFile, pluginDir );

            skipped = false;
        }
        else if ( installAsJar && !pluginFile.exists() )
        {
            performFileOperations( installAsJar, artifact, pluginFile, pluginDir );

            skipped = false;
        }
        else if ( !installAsJar && !pluginDir.exists() )
        {
            performFileOperations( installAsJar, artifact, pluginFile, pluginDir );

            skipped = false;
        }

        if ( skipped )
        {
            if ( installAsJar )
            {
                getLog().info(
                               "Skipping plugin installation for: " + artifact.getId() + "; file: " + pluginFile
                                   + " already exists. Set overwrite = true to override this." );
            }
            else if ( !installAsJar )
            {
                getLog().info(
                               "Skipping plugin installation for: " + artifact.getId() + "; directory: " + pluginDir
                                   + " already exists. Set overwrite = true to override this." );
            }
        }
    }

    private void performFileOperations( boolean installAsJar, Artifact artifact, File pluginFile, File pluginDir )
        throws MojoExecutionException
    {
        File artifactFile = artifact.getFile();

        if ( installAsJar )
        {
            try
            {
                getLog().debug( "Copying: " + artifact.getId() + " to: " + pluginFile );

                FileUtils.copyFile( artifactFile, pluginFile );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Failed to copy Eclipse plugin: " + artifact.getId() + "\nfrom: "
                    + artifact.getFile() + "\nto: " + pluginFile, e );
            }
        }
        else
        {
            try
            {
                getLog().debug( "Expanding: " + artifact.getId() + " into: " + pluginDir );

                pluginDir.mkdirs();

                UnArchiver unarchiver = archiverManager.getUnArchiver( artifactFile );

                unarchiver.setSourceFile( artifactFile );
                unarchiver.setDestDirectory( pluginDir );
                unarchiver.extract();
            }
            catch ( NoSuchArchiverException e )
            {
                throw new MojoExecutionException( "Could not find unarchiver for: " + artifactFile, e );
            }
            catch ( ArchiverException e )
            {
                throw new MojoExecutionException( "Could not extract: " + artifactFile, e );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Could not extract: " + artifactFile, e );
            }
        }
    }

    /**
     * <p>
     * Format the artifact information into an Eclipse-friendly plugin name. Currently, this is just:
     * <code>artifactId + "_" + bundle version</code> if bundle version is not null.
     * </p>
     */
    private String formatEclipsePluginName( Artifact artifact, String bundleVersion )
    {
        if ( bundleVersion == null )
        {
            return artifact.getArtifactId() + StringUtils.replace( artifact.getVersion(), "-", "." );
        }
        else
        {
            return artifact.getArtifactId() + "_" + bundleVersion;
        }
    }

}
