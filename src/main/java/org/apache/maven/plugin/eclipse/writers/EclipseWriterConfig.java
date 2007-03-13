/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugin.eclipse.writers;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.project.MavenProject;

/**
 * @author Fabrizio Giustina
 * @version $Id$
 */
public class EclipseWriterConfig
{
    /**
     * The maven project.
     */
    private MavenProject project;

    /**
     * Eclipse project dir.
     */
    private File eclipseProjectDirectory;

    /**
     * The name of the project in eclipse.
     */
    private String eclipseProjectName;

    /**
     * Base project dir.
     */
    private File projectBaseDir;

    /**
     * List of IDE dependencies.
     */
    private IdeDependency[] deps;

    /**
     * Source directories.
     */
    private EclipseSourceDir[] sourceDirs;

    /**
     * Local maven repo.
     */
    private ArtifactRepository localRepository;

    /**
     * Build output directory for eclipse.
     */
    private File buildOutputDirectory;

    /**
     * Manifest file.
     */
    private File manifestFile;

    /**
     * PDE mode.
     */
    private boolean pde;

    /**
     * Project natures.
     */
    private List projectnatures;
    
    /**
     * Project facets.
     */
    private Map projectFacets;

    /**
     * Build commands.
     *
     * List&lt;BuildCommand&gt;
     */
    private List buildCommands;

    /**
     * Classpath containers.
     */
    private List classpathContainers;

    /**
     * Getter for <code>deps</code>.
     * @return Returns the deps.
     */
    public IdeDependency[] getDeps()
    {
        return this.deps;
    }

    /**
     * Setter for <code>deps</code>.
     * @param deps The deps to set.
     */
    public void setDeps( IdeDependency[] deps )
    {
        this.deps = deps;
    }

    /**
     * Getter for <code>eclipseProjectDir</code>.
     * @return Returns the eclipseProjectDir.
     */
    public File getEclipseProjectDirectory()
    {
        return this.eclipseProjectDirectory;
    }

    /**
     * Setter for <code>eclipseProjectDir</code>.
     * @param eclipseProjectDir The eclipseProjectDir to set.
     */
    public void setEclipseProjectDirectory( File eclipseProjectDir )
    {
        this.eclipseProjectDirectory = eclipseProjectDir;
    }

    /**
     * Getter for <code>eclipseProjectName</code>.
     * @return Returns the project name used in eclipse.
     */
    public String getEclipseProjectName()
    {
        return eclipseProjectName;
    }

    /**
     * Setter for <code>eclipseProjectName</code>.
     * @param eclipseProjectDir the project name used in eclipse.
     */
    public void setEclipseProjectName( String eclipseProjectName )
    {
        this.eclipseProjectName = eclipseProjectName;
    }

    /**
     * Getter for <code>project</code>.
     * @return Returns the project.
     */
    public MavenProject getProject()
    {
        return this.project;
    }

    /**
     * Setter for <code>project</code>.
     * @param project The project to set.
     */
    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    /**
     * Getter for <code>sourceDirs</code>.
     * @return Returns the sourceDirs.
     */
    public EclipseSourceDir[] getSourceDirs()
    {
        return this.sourceDirs;
    }

    /**
     * Setter for <code>sourceDirs</code>.
     * @param sourceDirs The sourceDirs to set.
     */
    public void setSourceDirs( EclipseSourceDir[] sourceDirs )
    {
        this.sourceDirs = sourceDirs;
    }

    /**
     * Getter for <code>buildOutputDirectory</code>.
     * @return Returns the buildOutputDirectory.
     */
    public File getBuildOutputDirectory()
    {
        return this.buildOutputDirectory;
    }

    /**
     * Setter for <code>buildOutputDirectory</code>.
     * @param buildOutputDirectory The buildOutputDirectory to set.
     */
    public void setBuildOutputDirectory( File buildOutputDirectory )
    {
        this.buildOutputDirectory = buildOutputDirectory;
    }

    /**
     * Getter for <code>localRepository</code>.
     * @return Returns the localRepository.
     */
    public ArtifactRepository getLocalRepository()
    {
        return this.localRepository;
    }

    /**
     * Setter for <code>localRepository</code>.
     * @param localRepository The localRepository to set.
     */
    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    /**
     * Getter for <code>manifestFile</code>.
     * @return Returns the manifestFile.
     */
    public File getManifestFile()
    {
        return this.manifestFile;
    }

    /**
     * Setter for <code>manifestFile</code>.
     * @param manifestFile The manifestFile to set.
     */
    public void setManifestFile( File manifestFile )
    {
        this.manifestFile = manifestFile;
    }

    /**
     * Getter for <code>classpathContainers</code>.
     * @return Returns the classpathContainers.
     */
    public List getClasspathContainers()
    {
        return this.classpathContainers;
    }

    /**
     * Setter for <code>classpathContainers</code>.
     * @param classpathContainers The classpathContainers to set.
     */
    public void setClasspathContainers( List classpathContainers )
    {
        this.classpathContainers = classpathContainers;
    }

    /**
     * Getter for <code>pde</code>.
     * @return Returns the pde.
     */
    public boolean isPde()
    {
        return this.pde;
    }

    /**
     * Setter for <code>pde</code>.
     * @param pde The pde to set.
     */
    public void setPde( boolean pde )
    {
        this.pde = pde;
    }

    /**
     * Getter for <code>buildCommands</code>.
     * @return Returns the buildCommands.
     */
    public List getBuildCommands()
    {
        return this.buildCommands;
    }

    /**
     * Setter for <code>buildCommands</code>.
     * @param buildCommands The buildCommands to set.
     */
    public void setBuildCommands( List buildCommands )
    {
        this.buildCommands = buildCommands;
    }

    /**
     * Getter for <code>projectnatures</code>.
     * @return Returns the projectnatures.
     */
    public List getProjectnatures()
    {
        return this.projectnatures;
    }

    /**
     * Setter for <code>projectnatures</code>.
     * @param projectnatures The projectnatures to set.
     */
    public void setProjectnatures( List projectnatures )
    {
        this.projectnatures = projectnatures;
    }

    /**
     * Getter for <code>projectFacets</code>.
     * @return Returns the projectFacets
     */
    public Map getProjectFacets()
    {
        return projectFacets;
    }

    /**
     * Setter for <code>projectFacets</code>
     * @param projectFacets The projectFacets to set.
     */
    public void setProjectFacets( Map projectFacets )
    {
        this.projectFacets = projectFacets;
    }

    /**
     * Getter for <code>projectBaseDir</code>.
     * @return Returns the projectBaseDir.
     */
    public File getProjectBaseDir()
    {
        return this.projectBaseDir;
    }

    /**
     * Setter for <code>projectBaseDir</code>.
     * @param projectBaseDir The projectBaseDir to set.
     */
    public void setProjectBaseDir( File projectBaseDir )
    {
        this.projectBaseDir = projectBaseDir;
    }
}
