package org.apache.maven.plugin.assembly.mojos;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;

/**
 * @goal attach-component-descriptor
 * @phase package
 */
public class AttachComponentDescriptorMojo
    extends AbstractMojo
{

    /**
     * @parameter default-value="src/main/resources/assembly-component.xml"
     * @required
     */
    private File componentDescriptor;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @component role-hint="assembly-component"
     */
    private ArtifactHandler handler;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * @component
     */
    private ArtifactFactory factory;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        Artifact artifact = factory.createProjectArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion() );
        artifact.setFile( project.getFile() );

        getLog().debug( "Replacing main project artifact with POM artifact: " + artifact.getId() );

        project.setArtifact( artifact );

        getLog().info( "Attaching assembly-component descriptor: " + componentDescriptor + " to the main project artifact under type: " + handler.getExtension() + " and classifier: " + handler.getClassifier() );

        projectHelper.attachArtifact( project, handler.getExtension(), handler.getClassifier(), componentDescriptor );
    }

}
