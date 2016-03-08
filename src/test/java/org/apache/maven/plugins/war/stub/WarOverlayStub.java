package org.apache.maven.plugins.war.stub;

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

/**
 * @author Stephane Nicoll
 */
public class WarOverlayStub
    extends AbstractArtifactStub
{


    private final String artifactId;

    private File file;

    public WarOverlayStub( String _basedir, String artifactId, File warFile )
    {
        super( _basedir );
        if ( artifactId == null )
        {
            throw new NullPointerException( "Id could not be null." );
        }
        if ( warFile == null )
        {
            throw new NullPointerException( "warFile could not be null." );

        }
        else if ( !warFile.exists() )
        {
            throw new IllegalStateException( "warFile[" + file.getAbsolutePath() + "] should exist." );
        }
        this.artifactId = artifactId;
        this.file = warFile;
    }

    public String getType()
    {
        return "war";
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getGroupId()
    {
        return "wartests";
    }

    public File getFile()
    {
        return file;
    }

}
