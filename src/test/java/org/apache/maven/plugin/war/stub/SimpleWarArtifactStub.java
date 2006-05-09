package org.apache.maven.plugin.war.stub;

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

import java.io.File;

/**
 * Stub
 */
public class SimpleWarArtifactStub
    extends AbstractArtifactStub
{

    private String artifactId;

    private File file;

    public SimpleWarArtifactStub( String _basedir )
    {
        super( _basedir );
    }

    public String getType()
    {
        return "war";
    }

    public String getArtifactId()
    {
        if ( artifactId == null )
        {
            return "simple";
        }
        else
        {
            return artifactId;
        }
    }

    public void setArtifactId( String _artifactId )
    {
        artifactId = _artifactId;
    }

    public File getFile()
    {
        if ( file == null )
        {
            return new File( basedir, "/target/test-classes/unit/sample_wars/simple.war" );
        }
        else
        {
            return file;
        }
    }

    public void setFile( File _file )
    {
        file = _file;
    }
}
