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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.repository.Repository;

import java.io.IOException;

/**
 * Copies artifacts from one repository to another repository.
 * 
 * @author Jason van Zyl
 * @requiresProject false
 * @goal copy
 */
public class CopyRepositoryMojo
    extends AbstractMojo
{
    /**
     * The URL to the source repository.
     *
     * @parameter expression="${source}"
     */
    private String source;

    /**
     * The URL to the target repository.
     * 
     * <p>
     * <strong>Note:</strong> currently only <code>scp:</code> URLs are allowed
     * as a target URL.
     * </p>
     * 
     * @parameter expression="${target}"
     */
    private String target;

    /**
     * The id of the source repository, required if you need the configuration from the user settings.
     * 
     * @parameter expression="${sourceRepositoryId}" default-value="source"
     */
    private String sourceRepositoryId;

    /**
     * The id of the target repository, required if you need the configuration from the user settings.
     * 
     * @parameter expression="${targetRepositoryId}" default-value="target"
     */
    private String targetRepositoryId;

    /**
     * The version of the artifact that is to be copied.
     * <p>
     * <b>Note:</b> This is currently only used for naming temporary files.
     * <i>All</i> versions of the artifacts will be copied.
     * </p>
     *
     * @parameter expression="${version}"
     * @required
     */
    private String version;

    /**
     * The repository copier to use.
     *
     * @component
     */
    private RepositoryCopier copier;

    public void execute()
        throws MojoExecutionException
    {
        try
        {
            Repository sourceRepository = new Repository( sourceRepositoryId, source );
            Repository targetRepository = new Repository( targetRepositoryId, target );
            copier.copy( sourceRepository, targetRepository, version );
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

