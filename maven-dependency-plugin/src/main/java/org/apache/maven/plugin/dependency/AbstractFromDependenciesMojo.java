package org.apache.maven.plugin.dependency;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

/**
 * Abstract Parent class used by mojos that get Artifact information from the
 * project dependencies.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 */
public abstract class AbstractFromDependenciesMojo
    extends AbstractDependencyFilterMojo
{

    /**
     * Strip artifact version during copy
     *
     * @optional
     * @parameter expression="${mdep.stripVersion}" default-value="false"
     * @parameter
     */
    protected boolean stripVersion = false;

    /**
     * Default location used for mojo unless overridden in ArtifactItem
     *
     * @parameter expression="${outputDirectory}"
     *            default-value="${project.build.directory}/dependency"
     * @optional
     * @since 1.0
     */
    protected File outputDirectory;

    /**
     * Place each artifact in the same directory layout as a default repository.
     * <br/>example: /outputDirectory/junit/junit/3.8.1/junit-3.8.1.jar
     * @since 2.0-alpha-2
     * @parameter expression="${mdep.useRepositoryLayout}" default-value="false"
     * @optional
     */
    protected boolean useRepositoryLayout;

    /**
     * Also copy the pom of each artifact.
     *
     * @since 2.0
     * @parameter expression="${mdep.copyPom}"
     *            default-value="false"
     * @optional
     */
    protected boolean copyPom = true;

    /**
     * Place each type of file in a separate subdirectory. (example
     * /outputDirectory/runtime /outputDirectory/provided etc)
     *
     * @parameter expression="${mdep.useSubDirectoryPerScope}" default-value="false"
     * @optional
     * @since 2.1-alpha-1
     */
    protected boolean useSubDirectoryPerScope;

    /**
     * Place each type of file in a separate subdirectory. (example
     * /outputDirectory/jars /outputDirectory/wars etc)
     *
     * @since 2.0-alpha-1
     * @parameter expression="${mdep.useSubDirectoryPerType}" default-value="false"
     * @optional
     */
    protected boolean useSubDirectoryPerType;

    /**
     * Place each file in a separate subdirectory. (example
     * /outputDirectory/junit-3.8.1-jar)
     *
     * @since 2.0-alpha-1
     * @parameter expression="${mdep.useSubDirectoryPerArtifact}"
     *            default-value="false"
     * @optional
     */
    protected boolean useSubDirectoryPerArtifact;

    /**
     * This only applies if the classifier parameter is used.
     *
     * @since 2.0-alpha-2
     * @parameter expression="${mdep.failOnMissingClassifierArtifact}"
     *            default-value="true"
     * @optional
     */
    protected boolean failOnMissingClassifierArtifact = true;

    /**
     * @return Returns the outputDirectory.
     */
    public File getOutputDirectory()
    {
        return this.outputDirectory;
    }

    /**
     * @param theOutputDirectory
     *            The outputDirectory to set.
     */
    public void setOutputDirectory( File theOutputDirectory )
    {
        this.outputDirectory = theOutputDirectory;
    }

    /**
     * @return Returns the useSubDirectoryPerArtifact.
     */
    public boolean isUseSubDirectoryPerArtifact()
    {
        return this.useSubDirectoryPerArtifact;
    }

    /**
     * @param theUseSubDirectoryPerArtifact
     *            The useSubDirectoryPerArtifact to set.
     */
    public void setUseSubDirectoryPerArtifact( boolean theUseSubDirectoryPerArtifact )
    {
        this.useSubDirectoryPerArtifact = theUseSubDirectoryPerArtifact;
    }

    /**
     * @return Returns the useSubDirectoryPerScope
     */
    public boolean isUseSubDirectoryPerScope()
    {
        return this.useSubDirectoryPerScope;
    }
    
    /**
     * @param theUseSubDirectoryPerScope
     *          The useSubDirectoryPerScope to set.
     */
    public void setUseSubDirectoryPerScope( boolean theUseSubDirectoryPerScope )
    {
        this.useSubDirectoryPerScope = theUseSubDirectoryPerScope;
    }

    /**
     * @return Returns the useSubDirectoryPerType.
     */
    public boolean isUseSubDirectoryPerType()
    {
        return this.useSubDirectoryPerType;
    }

    /**
     * @param theUseSubDirectoryPerType
     *            The useSubDirectoryPerType to set.
     */
    public void setUseSubDirectoryPerType( boolean theUseSubDirectoryPerType )
    {
        this.useSubDirectoryPerType = theUseSubDirectoryPerType;
    }

    public boolean isFailOnMissingClassifierArtifact()
    {
        return failOnMissingClassifierArtifact;
    }

    public void setFailOnMissingClassifierArtifact( boolean failOnMissingClassifierArtifact )
    {
        this.failOnMissingClassifierArtifact = failOnMissingClassifierArtifact;
    }

    public boolean isStripVersion()
    {
        return stripVersion;
    }

    public void setStripVersion( boolean stripVersion )
    {
        this.stripVersion = stripVersion;
    }

    /**
     *
     * @return true, if dependencies must be planted in a repository layout
     */
    public boolean isUseRepositoryLayout()
    {
        return useRepositoryLayout;
    }

    /**
     *
     * @param useRepositoryLayout -
     *            true if dependencies must be planted in a repository layout
     */
    public void setUseRepositoryLayout( boolean useRepositoryLayout )
    {
        this.useRepositoryLayout = useRepositoryLayout;
    }

    /**
     * @return true, if the pom of each artifact must be copied
     */
    public boolean isCopyPom()
    {
        return this.copyPom;
    }

    /**
     * @param copyPom - true if the pom of each artifact must be copied
     */
    public void setCopyPom( boolean copyPom )
    {
        this.copyPom = copyPom;
    }
}
