package org.apache.maven.plugin.assembly.repository;

import java.io.File;
import java.util.List;

/**
 * @author Jason van Zyl
 */
public interface RepositoryAssembler
{
    String ROLE = RepositoryAssembler.class.getName();

    public void assemble( File outputDirectory, List artifacts, List remoteRepositories )
        throws RepositoryAssemblyException;
}
