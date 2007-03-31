package org.apache.maven.plugin.idea.stubs;

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

import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

/**
 * @author Edwin Punzalan
 */
public class ReactorMavenProjectStub
    extends MavenProjectStub
{
    private static int reactorProjectCounter = 0;

    private String artifactId;

    public ReactorMavenProjectStub()
    {
        reactorProjectCounter++;

        setArtifactId( "plugin-reactor-project-" + reactorProjectCounter );
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getGroupId()
    {
        return "org.apache.maven.plugin.test";
    }

    public String getVersion()
    {
        return "1.0";
    }

    public static int getCurrentCount()
    {
        return reactorProjectCounter;
    }
}
