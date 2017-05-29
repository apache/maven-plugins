package org.apache.maven.plugin.antrun;

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

/**
 * Encapsulates a Maven project with an unsupported clone operation. This makes sure that, when used as a reference in
 * an Ant project, it is passed by reference to sub-projects when <code>inheritRefs</code> is set to <code>true</code>
 * (which would otherwise pass a clone).
 * 
 * @author gboue
 */
public class MavenAntRunProject
{

    private MavenProject mavenProject;

    public MavenAntRunProject( MavenProject mavenProject )
    {
        this.mavenProject = mavenProject;
    }

    public MavenProject getMavenProject()
    {
        return mavenProject;
    }

}
