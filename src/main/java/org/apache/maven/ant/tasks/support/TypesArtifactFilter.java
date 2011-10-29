package org.apache.maven.ant.tasks.support;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Artifact Filter which filters on artifact types.
 * 
 */
public class TypesArtifactFilter
    implements ArtifactFilter
{
    private List<String> types = new ArrayList<String>();

    /** 
     * Accepts a comma separated list of types
     * 
     * @param types
     */
    public TypesArtifactFilter( String types )
    {
        if ( !types.trim().equals( "" ) )
        {
            for ( String type : types.split( "," ) )
            {
                this.types.add( type.trim() );
            }
        }
    }

    public boolean include( Artifact artifact )
    {
        String artifactType = artifact.getType();
        if ( artifactType == null || artifactType.equals( "" ) )
        {
            artifactType = "jar";
        }
        return types.contains( artifactType );
    }
}
