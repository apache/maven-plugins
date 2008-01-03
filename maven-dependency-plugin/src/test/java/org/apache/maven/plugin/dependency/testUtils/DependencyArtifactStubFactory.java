package org.apache.maven.plugin.dependency.testUtils;

/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.dependency.fromConfiguration.ArtifactItem;
import org.apache.maven.plugin.testing.ArtifactStubFactory;

public class DependencyArtifactStubFactory
extends ArtifactStubFactory
{
 
    /**
     * @param theWorkingDir
     * @param theCreateFiles
     */
    public DependencyArtifactStubFactory( File theWorkingDir, boolean theCreateFiles )
    {
        super( theWorkingDir, theCreateFiles );
    }

    public ArtifactItem getArtifactItem( Artifact artifact )
    {
        ArtifactItem item = new ArtifactItem( artifact );
        return item;
    }

    public ArrayList getArtifactItems( Collection artifacts )
    {
        ArrayList list = new ArrayList();
        Iterator iter = artifacts.iterator();
        while ( iter.hasNext() )
        {
            list.add( getArtifactItem( (Artifact) iter.next() ) );
        }
        return list;
    }
 }
