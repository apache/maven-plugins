package org.apache.maven.plugin.install.stubs;

import java.io.File;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

public class AttachedArtifactStub0
    extends InstallArtifactStub
{
    public String getArtifactId()
    {
        return "attached-artifact-test-0";
    }
    
    public File getFile()
    {
        return new File( System.getProperty( "basedir" ), 
            "target/test-classes/unit/basic-install-test-with-attached-artifacts/" +
            "target/maven-install-test-1.0-SNAPSHOT.jar" ); 
    }
}
