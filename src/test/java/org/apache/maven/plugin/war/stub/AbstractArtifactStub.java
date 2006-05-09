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

import org.apache.maven.plugin.testing.stubs.ArtifactStub;

public abstract class AbstractArtifactStub
    extends ArtifactStub
{
    protected String basedir;

    public AbstractArtifactStub( String _basedir )
    {
        basedir = _basedir;
    }

    public String getVersion()
    {
        return "0.0-Test";
    }

    public String getScope()
    {
        return ArtifactStub.SCOPE_RUNTIME;
    }

    public boolean isOptional()
    {
        return false;
    }
}
