package org.codehaus.mojo.shade.mojo;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.model.Model;
import org.apache.maven.model.Dependency;
import org.codehaus.mojo.shade.Shader;
import org.codehaus.mojo.shade.pom.PomWriter;
import org.codehaus.mojo.shade.relocation.SimpleRelocator;
import org.codehaus.mojo.shade.resource.ResourceTransformer;

/**
 * @author Jason van Zyl
 * @author Mauro Talevi
 * @goal shade
 * @phase package
 */
public class ShadeMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;

    /** @component */
    private MavenProjectHelper projectHelper;

    /** @component */
    private Shader shader;

    /**
     * Artifacts to include/exclude from the final artifact.
     *
     * @parameter
     */
    private ArtifactSet artifactSet;

    /**
     * Packages to be relocated.
     *
     * @parameter
     */
    private PackageRelocation[] relocations;

    /**
     * Resource transformers to be used.
     *
     * @parameter
     */
    private ResourceTransformer[] transformers;

    /** @parameter expression="${project.build.directory}" */
    private File outputDirectory;

    /**
     * The name of the shaded artifactId
     *
     * @parameter expression="${shadedArtifactId}" default-value="${project.artifactId}"
     */
    private String shadedArtifactId;

    /**
     * If specified, this will include only artifacts which have groupIds which
     * start with this.
     *
     * @parameter expression="${shadedGroupFilter}"
     */
    private String shadedGroupFilter;

    /**
     * Defines whether the shaded artifact should be attached as classifier to
     * the original artifact.  If false, the shaded jar will be the main artifact
     * of the project
     *
     * @parameter expression="${shadedArtifactAttached}" default-value="false"
     */
    private boolean shadedArtifactAttached;

    /**
     * @parameter expression="${createDependencyReducedPom}" default-value="true"
     */
    private boolean createDependencyReducedPom;

    /**
     * The name of the classifier used in case the shaded artifact is attached.
     *
     * @parameter expression="${shadedClassifierName}" default-value="shaded"
     */
    private String shadedClassifierName;

    /** @throws MojoExecutionException  */
    public void execute()
        throws MojoExecutionException
    {
        Set artifacts = new HashSet();

        Set artifactIds = new HashSet();

        for ( Iterator it = project.getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
   
            if ( excludeArtifact( artifact ) )
            {
                getLog().info( "Excluding " + artifact.getId() + " from the shaded jar." );

                continue;
            }

            getLog().debug( "Including " + artifact.getId() + " in the shaded jar." );

            artifacts.add( artifact.getFile() );

            artifactIds.add( getId( artifact ) );
        }

        artifacts.add( project.getArtifact().getFile() );


        File outputJar = shadedArtifactFileWithClassifier();

        // Now add our extra resources
        try
        {
            List relocators = getRelocators();

            List resourceTransformers = getResourceTrasformers();

            shader.shade( artifacts, outputJar, relocators, resourceTransformers );

            if ( shadedArtifactAttached )
            {
                getLog().info( "Attaching shaded artifact." );
                projectHelper.attachArtifact( getProject(), "jar", shadedClassifierName, outputJar );
            }

            else
            {
                getLog().info( "Replacing original artifact with shaded artifact." );
                File file = shadedArtifactFile();
                file.renameTo( new File( outputDirectory, "original-" + file.getName() ) );

                if ( !outputJar.renameTo( file ) )
                {
                    getLog().warn( "Could not replace original artifact with shaded artifact!" );
                }

                if ( createDependencyReducedPom )
                {
                    createDependencyReducedPom( artifactIds );
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating shaded jar.", e );
        }
    }

    private boolean excludeArtifact( Artifact artifact )
    {
        String id = getId( artifact );

        // This is the case where we have only stated artifacts to include and no exclusions
        // have been listed. We just want what we have asked to include.
        if ( ( artifactSet.getExcludes() == null && artifactSet.getIncludes() != null ) && !includedArtifacts().contains( id ) )
        {
            return true;
        }

        if ( excludedArtifacts().contains( id ) )
        {
            return true;
        }

        if ( shadedGroupFilter != null && !artifact.getGroupId().startsWith( shadedGroupFilter ) )
        {
            return true;
        }

        return false;
    }

    private Set excludedArtifacts()
    {
        if ( artifactSet != null && artifactSet.getExcludes() != null )
        {
            return artifactSet.getExcludes();
        }
        
        return Collections.EMPTY_SET;
    }

    private Set includedArtifacts()
    {
        if ( artifactSet != null && artifactSet.getIncludes() != null )
        {
            return artifactSet.getIncludes();
        }

        return Collections.EMPTY_SET;
    }

    private List getRelocators()
    {
        List relocators = new ArrayList();

        if ( relocations == null )
        {
            return relocators;
        }

        for ( int i = 0; i < relocations.length; i++ )
        {
            PackageRelocation r = relocations[i];

            if ( r.getExcludes() != null )
            {
                relocators.add( new SimpleRelocator( r.getPattern(), r.getExcludes() ) );
            }
            else
            {
                relocators.add( new SimpleRelocator( r.getPattern(), null ) );
            }
        }
        return relocators;
    }

    private List getResourceTrasformers()
    {
        if ( transformers == null )
        {
            return Collections.EMPTY_LIST;
        }

        return Arrays.asList( transformers );
    }

    private File shadedArtifactFileWithClassifier()
    {
        Artifact artifact = project.getArtifact();
        final String shadedName =
            shadedArtifactId + "-" + artifact.getVersion() + "-" + shadedClassifierName + "." + artifact.getType();
        return new File( outputDirectory, shadedName );
    }

    private File shadedArtifactFile()
    {
        Artifact artifact = project.getArtifact();
        final String shadedName = shadedArtifactId + "-" + artifact.getVersion() + "." + artifact.getType();
        return new File( outputDirectory, shadedName );
    }

    protected MavenProject getProject()
    {
        if ( project.getExecutionProject() != null )
        {
            return project.getExecutionProject();
        }
        else
        {
            return project;
        }
    }

    // We need to find the direct dependencies that have been included in the uber JAR so that we can modify the
    // POM accordingly.
    private void createDependencyReducedPom( Set artifactsToRemove )
        throws IOException
    {
        Model model = getProject().getOriginalModel();

        List dependencies = new ArrayList();

        for ( Iterator i = model.getDependencies().iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            String id = d.getGroupId() + ":" + d.getArtifactId();

            if ( !artifactsToRemove.contains( id ) )
            {
                dependencies.add( d );
            }
        }

        // Check to see if we have a reduction and if so rewrite the POM.
        if ( dependencies.size() < model.getDependencies().size() )
        {
            model.setDependencies( dependencies );

            File f = new File( getProject().getFile().getParentFile(), "dependency-reduced-pom.xml" );

            f.deleteOnExit();

            Writer w = new FileWriter( f );

            PomWriter.write( w, model, true );

            w.close();

            getProject().setFile( f );
        }
    }

    private String getId( Artifact artifact )
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }
}
