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
package org.apache.maven.plugin.eclipse;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractWorkspaceMojo
    extends AbstractMojo
{

    /**
     * Directory location of the <code>Eclipse</code> workspace.
     */
    @Parameter( property = "eclipse.workspace", required = true )
    private String workspace;

    @Parameter( property = "localRepository", required = true, readonly = true )
    private ArtifactRepository localRepository;

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    public String getWorkspace()
    {
        return workspace;
    }

    public void setWorkspace( String workspace )
    {
        this.workspace = workspace;
    }
}
