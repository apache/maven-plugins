package org.apache.maven.report.projectinfo.dependencies.renderer;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * GraphColors 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class GraphColors
{
    private ArtifactColors rootProject;

    private ArtifactColors directDependencies;

    private ArtifactColors transitiveDependencies;

    private ArtifactColors testDependencies;

    public GraphColors()
    {
        rootProject = new ArtifactColors( "#00ee00", "#aaeeaa", null );
        directDependencies = new ArtifactColors( "#ddeedd", "#ddeedd", null );
        transitiveDependencies = new ArtifactColors( "#ddeedd", "#eeffee", "#005500" );
        testDependencies = new ArtifactColors( "#eeeeff", "#eeeeff", "#0000ff" );
    }

    public ArtifactColors getDirectDependencies()
    {
        return directDependencies;
    }

    public void setDirectDependencies( ArtifactColors directDependencies )
    {
        this.directDependencies = directDependencies;
    }

    public ArtifactColors getRootProject()
    {
        return rootProject;
    }

    public void setRootProject( ArtifactColors rootProject )
    {
        this.rootProject = rootProject;
    }

    public ArtifactColors getTestDependencies()
    {
        return testDependencies;
    }

    public void setTestDependencies( ArtifactColors testDependencies )
    {
        this.testDependencies = testDependencies;
    }

    public ArtifactColors getTransitiveDependencies()
    {
        return transitiveDependencies;
    }

    public void setTransitiveDependencies( ArtifactColors transitiveDependencies )
    {
        this.transitiveDependencies = transitiveDependencies;
    }
}
