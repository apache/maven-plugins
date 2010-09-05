package org.apache.maven.plugin.ear;

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

import org.apache.maven.artifact.handler.ArtifactHandler;

/**
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public class ArtifactHandlerTestStub
    implements ArtifactHandler
{

    private final String extension;

    public ArtifactHandlerTestStub( String extension )
    {
        this.extension = extension;
    }

    public String getExtension()
    {
        return extension;
    }


    public String getDirectory()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public String getClassifier()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public String getPackaging()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public boolean isIncludesDependencies()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public String getLanguage()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }

    public boolean isAddedToClasspath()
    {
        throw new UnsupportedOperationException( "not implemented ; fake artifact stub" );
    }
}
