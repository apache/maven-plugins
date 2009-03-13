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

import org.apache.maven.project.MavenProject;

import java.util.Collections;
import java.util.List;

/**
 * This goal bundles all the test sources into a jar archive.  This goal functions the same
 * as the test-jar goal but does not fork the build, and is suitable for attaching 
 * to the build lifecycle.
 *
 * @goal test-jar-no-fork
 * @phase package
 * @since 2.1
 */
public class TestSourceJarNoForkMojo
    extends AbstractSourceJarMojo
{
    /** {@inheritDoc} */
    protected List getSources( MavenProject p )
    {
        return p.getTestCompileSourceRoots();
    }

    /** {@inheritDoc} */
    protected List getResources( MavenProject p )
    {
        if ( excludeResources )
        {
            return Collections.EMPTY_LIST;
        }

        return p.getTestResources();
    }

    /** {@inheritDoc} */
    protected String getClassifier()
    {
        return "test-sources";
    }
}
