package org.apache.maven.plugin.deploy.stubs;

import java.io.File;
import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.transform.ArtifactTransformationManager;
import org.codehaus.plexus.util.FileUtils;

public class ArtifactDeployerStub
    implements ArtifactDeployer
{
    private WagonManager wagonManager;

    private ArtifactTransformationManager transformationManager;

    private RepositoryMetadataManager repositoryMetadataManager;

    public void deploy( String basedir, String finalName, Artifact artifact, ArtifactRepository deploymentRepository,
                        ArtifactRepository localRepository )
        throws ArtifactDeploymentException
    {
        String extension = artifact.getArtifactHandler().getExtension();
        File source = new File( basedir, finalName + "." + extension );
        deploy( source, artifact, deploymentRepository, localRepository );
    }

    public void deploy( File source, Artifact artifact, ArtifactRepository deploymentRepository,
                        ArtifactRepository localRepository )
        throws ArtifactDeploymentException
    {

    }
}
