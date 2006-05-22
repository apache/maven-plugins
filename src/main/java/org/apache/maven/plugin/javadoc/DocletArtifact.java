package org.apache.maven.plugin.javadoc;

/*
 * Copyright 2006 The Apache Software Foundation.
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
 * This class represents the doclet artifact parameter specified in the plugin configuration.
 */
public class DocletArtifact
{
    private String groupId;

    private String artifactId;

    private String version;

    /**
     * Method to get the groupId of the artifact
     *
     * @return a String that contains the groupId
     */
    public String getGroupId()
    {
        return groupId;
    }

    /**
     * Method to set the groupId of the artifact
     *
     * @param groupId the value to be set
     */
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    /**
     * Method to get the artifactId of the artifact
     *
     * @return a String that contains the artifactId
     */
    public String getArtifactId()
    {
        return artifactId;
    }

    /**
     * Method to set the artifactId of the artifact
     *
     * @param artifactId the value to be set
     */
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    /**
     * Method to get the version of the artifact
     *
     * @return a String that contains the version
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * Method to set the version of the artifact
     *
     * @param version the value to be set
     */
    public void setVersion( String version )
    {
        this.version = version;
    }

}
