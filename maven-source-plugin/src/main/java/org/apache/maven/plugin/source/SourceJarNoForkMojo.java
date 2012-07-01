package org.apache.maven.plugin.source;

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

import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.Collections;
import java.util.List;

/**
 * This goal bundles all the sources into a jar archive.  This goal functions the same
 * as the jar goal but does not fork the build and is suitable for attaching
 * to the build lifecycle.
 *
 * @author pgier
 * @version $Id$
 * @since 2.1
 */
@Mojo( name = "jar-no-fork", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true )
public class SourceJarNoForkMojo
    extends AbstractSourceJarMojo
{
    /**
     * @since 2.2
     */
    @Parameter( property = "maven.source.classifier", defaultValue = "sources" )
    protected String classifier;

    /**
     * {@inheritDoc}
     */
    protected List<String> getSources( MavenProject p )
    {
        return p.getCompileSourceRoots();
    }

    /**
     * {@inheritDoc}
     */
    protected List<Resource> getResources( MavenProject p )
    {
        if ( excludeResources )
        {
            return Collections.emptyList();
        }

        return p.getResources();
    }

    /**
     * {@inheritDoc}
     */
    protected String getClassifier()
    {
        return classifier;
    }
}
