package org.apache.maven.plugin.deploy.stubs;

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

public class ArtifactRepositoryStub2
    extends ArtifactRepositoryStub
{
    private String protocol;
    
    public String getUrl()
    {
        return "file://" + System.getProperty( "basedir" ) + "/target/remote-repo/basic-deploy-scp";
    }
    
    public String getBasedir()
    {
        return System.getProperty( "basedir" );
    }
    
    public String getProtocol()
    {
        if( this.protocol == null || this.protocol.equals("") )
        {
            this.protocol = "scp";
        }
        return this.protocol;
    }
    
    public void setProtocol( String protocol )
    {
        this.protocol = protocol;
    }
}
