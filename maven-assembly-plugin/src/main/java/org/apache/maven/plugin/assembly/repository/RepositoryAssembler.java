package org.apache.maven.plugin.assembly.repository;

import org.apache.maven.plugins.assembly.model.Repository;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

/**
 * @author Jason van Zyl
 */
public interface RepositoryAssembler
{
    String ROLE = RepositoryAssembler.class.getName();

    public void assemble( File repositoryDirectory, Repository repository, MavenProject project )                          
        throws RepositoryAssemblyException;
}
