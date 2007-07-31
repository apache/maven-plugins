package org.apache.maven.plugin.assembly.mojos;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;

/**
 * @goal attach-assembly-descriptor
 * @phase package
 */
public class AttachAssemblyDescriptorMojo
    extends AbstractMojo
{

    /**
     * @parameter default-value="src/main/resources/assembly-descriptor.xml"
     * @required
     */
    private File assemblyDescriptor;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @component role-hint="assembly-descriptor"
     */
    private ArtifactHandler handler;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        File pomFile = project.getFile();
        project.getArtifact().setFile( pomFile );
        projectHelper.attachArtifact( project, assemblyDescriptor, handler.getClassifier() );
    }

}
