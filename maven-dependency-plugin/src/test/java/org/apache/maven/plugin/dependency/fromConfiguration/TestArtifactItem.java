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
package org.apache.maven.plugin.dependency.fromConfiguration;

import java.io.IOException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.dependency.AbstractDependencyMojoTestCase;

public class TestArtifactItem
    extends AbstractDependencyMojoTestCase
{

    protected void setUp()
        throws Exception
    {
        setUp("artifactItems",false);
    }

    public void testArtifactItemConstructor() throws IOException
    {
        Artifact artifact = stubFactory.createArtifact( "g", "a", "1.0", Artifact.SCOPE_COMPILE, "jar", "one" );
       
        ArtifactItem item = new ArtifactItem(artifact);
        
        assertEquals(item.getArtifact(),artifact);
        assertEquals(item.getArtifactId(),artifact.getArtifactId());
        assertEquals(item.getGroupId(),artifact.getGroupId());
        assertEquals(item.getVersion(),artifact.getVersion());
        assertEquals(item.getClassifier(),artifact.getClassifier());
        assertEquals(item.getType(),artifact.getType());
    }
    
    public void testArtifactItemDefaultType()
    {
        ArtifactItem item = new ArtifactItem();
        //check type default
        assertEquals( "jar", item.getType() );
    }

}
