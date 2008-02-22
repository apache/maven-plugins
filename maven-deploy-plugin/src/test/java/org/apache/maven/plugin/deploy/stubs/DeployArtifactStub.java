package org.apache.maven.plugin.deploy.stubs;

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

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;

public class DeployArtifactStub
    extends ArtifactStub
{
    private Map metadataMap;
    
    private File file;
    
    private boolean release;
    
    private String extension;
    
    public String getArtifactId()
    {
        return "maven-deploy-test";
    }

    public String getGroupId()
    {
        return "org.apache.maven.test";
    }

    public String getVersion()
    {
        return "1.0-SNAPSHOT";
    }
    
    public String getBaseVersion()
    {
        return getVersion();
    }
    
    public void setFile( File file )
    {
        this.file = file;
    }
    
    public File getFile()
    {
        return file;
    }
    
    public ArtifactHandler getArtifactHandler()
    {
        return new DefaultArtifactHandler()
        {
            public String getExtension()
            {
                if( extension == null )
                {
                    extension = "jar";
                }
                return extension;
            }
        };
    }
    
    public void setArtifactHandlerExtension( String extension )
    {
        this.extension = extension;
    }
    
    public void addMetadata( ArtifactMetadata metadata )
    {
        if ( metadataMap == null )
        {
            metadataMap = new HashMap();
        }

        ArtifactMetadata m = (ArtifactMetadata) metadataMap.get( metadata.getKey() );
        if ( m != null )
        {
            m.merge( metadata );
        }
        else
        {
            metadataMap.put( metadata.getKey(), metadata );
        }
    }
    
    public Collection getMetadataList()
    {
        return metadataMap == null ? Collections.EMPTY_LIST : metadataMap.values();
    }

    public boolean isRelease()
    {
        return release;
    }

    public void setRelease( boolean release )
    {
        this.release = release;
    }

    /*
    public boolean isSnapshot()
    {
        return false;
    }
    */
}
