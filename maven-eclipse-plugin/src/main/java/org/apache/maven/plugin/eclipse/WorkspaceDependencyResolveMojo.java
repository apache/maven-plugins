package org.apache.maven.plugin.eclipse;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.eclipse.reader.ReadWorkspaceLocations;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

/**
 * For all projects currently part of the workspace, all references to the <code>M2_REPO</code> classpath variable are
 * resolved.
 * <p>
 * Note: not the projects of the <em>reactor</em> are inspected for unresolved artifacts, but the projects that are part
 * of the <em>workspace</em>.
 * 
 * @since 2.10
 * @author agudian
 */
@Mojo( name = "resolve-workspace-dependencies", aggregator = true, requiresProject = false )
public class WorkspaceDependencyResolveMojo
    extends AbstractMojo
{
    /**
     * The eclipse workspace directory.
     * <p>
     * If omitted, the parent directories of the working directory are checked. The first directory to contain a
     * <code>.metadata</code> subdirectory is chosen.
     */
    @Parameter( property = "eclipse.workspace" )
    private File workspace;

    @Component( role = ArtifactFactory.class )
    private ArtifactFactory artifactFactory;

    @Component( role = ArtifactResolver.class )
    private ArtifactResolver artifactResolver;

    @Parameter( property = "project.remoteArtifactRepositories", required = true, readonly = true )
    private List<ArtifactRepository> remoteArtifactRepositories;

    @Parameter( property = "localRepository", required = true, readonly = true )
    private ArtifactRepository localRepository;

    private List<File> findProjectLocations( File workspaceLocation )
    {
        return new ReadWorkspaceLocations().readProjectLocations( workspaceLocation, getLog() );
    }

    private void validateWorkspaceLocation()
        throws MojoExecutionException
    {
        if ( workspace != null && !isWorkspaceDirectory( workspace ) )
        {
            throw new MojoExecutionException( "Not a workspace directory: there is no subdirectory .metadata at "
                + workspace );
        }

        if ( workspace == null )
        {
            File currentWorkingDirectory = new File( "." ).getAbsoluteFile();
            while ( currentWorkingDirectory != null )
            {
                if ( isWorkspaceDirectory( currentWorkingDirectory ) )
                {
                    getLog().debug( "Detected workspace at " + currentWorkingDirectory );
                    workspace = currentWorkingDirectory;
                    return;
                }
                currentWorkingDirectory = currentWorkingDirectory.getParentFile();
            }
        }

        throw new MojoExecutionException( "No workspace location configured "
            + "and none can be detected in the parent directories." );
    }

    private boolean isWorkspaceDirectory( File currentWorkingDirectory )
    {
        return new File( currentWorkingDirectory, ".metadata" ).isDirectory();
    }

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        validateWorkspaceLocation();

        for ( File location : findProjectLocations( workspace ) )
        {
            File classpathFile = new File( location, ".classpath" );
            if ( classpathFile.exists() )
            {
                getLog().info( "Resolving M2_REPO dependencies in " + classpathFile );
                try
                {
                    Xpp3Dom classpath = Xpp3DomBuilder.build( new FileReader( classpathFile ) );

                    for ( Xpp3Dom entry : classpath.getChildren() )
                    {
                        if ( "var".equals( entry.getAttribute( "kind" ) ) )
                        {
                            resolveIfNecessary( entry.getAttribute( "path" ) );
                            resolveIfNecessary( entry.getAttribute( "sourcepath" ) );
                        }
                    }

                }
                catch ( Exception e )
                {
                    getLog().error( "Error parsing " + classpathFile, e );
                }

            }
        }
    }

    private void resolveIfNecessary( String path )
        throws ArtifactResolutionException
    {
        if ( null != path && path.startsWith( "M2_REPO" ) )
        {
            try
            {
                Artifact artifact = createArtifactFromPath( path );
                if ( artifact != null )
                {
                    artifactResolver.resolve( artifact, remoteArtifactRepositories, localRepository );
                }
            }
            catch ( ArtifactNotFoundException e )
            {
                getLog().info( e );
            }
        }
    }

    private Artifact createArtifactFromPath( String path )
    {
        String[] elements = path.split( "/" );
        if ( elements.length < 4 )
        {
            getLog().error( "Unexpected repository path structure: " + path );
            return null;
        }

        List<String> groupParts = new ArrayList<String>();
        for ( int i = 1; i < elements.length - 3; i++ )
        {
            groupParts.add( elements[i] );
        }
        String group = StringUtils.join( groupParts.iterator(), "." );
        String artifactId = elements[elements.length - 3];
        String version = elements[elements.length - 2];

        String classifier = null;
        String fileName = elements[elements.length - 1];
        String type = fileName.substring( fileName.lastIndexOf( '.' ) + 1 );
        String possibleClassifier =
            fileName.substring( artifactId.length() + version.length() + 1, fileName.length() - type.length() - 1 );
        if ( possibleClassifier.length() > 1 )
        {
            classifier = possibleClassifier.substring( 1 );
        }

        return artifactFactory.createArtifactWithClassifier( group, artifactId, version, type, classifier );
    }
}
