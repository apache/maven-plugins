package org.apache.maven.plugins.stage;

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

import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.repository.Repository;

/**
 * @author Jason van Zyl
 * @requiresProject false
 * @goal copy
 */
public class CopyRepositoryMojo
    extends AbstractMojo
{
    /** @parameter expression="${source}" */
    private String source;

    /** @parameter expression="${target}" */
    private String target;

    /** @parameter expression="${repositoryId}" */
    private String repositoryId = "target";

    /**
     * @parameter expression="${version}"
     * @required
     */
    private String version;

    /** @component */
    private RepositoryCopier copier;

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            Repository targetRepository = new Repository( repositoryId, target );
            copier.copy( source, targetRepository, version );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException(
                "Error copying repository from " + source + " to " + target, e );
        }
        catch ( WagonException e )
        {
            throw new MojoExecutionException(
                "Error copying repository from " + source + " to " + target, e );
        }
    }
}

