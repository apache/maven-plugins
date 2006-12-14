package org.apache.maven.plugin.gpg;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

/** @author Jason van Zyl */
public class SigningBundle
{
    private String artifactType;

    private String classifier;

    private File signature;

    public SigningBundle( String artifactType,
                          File signature )
    {
        this.artifactType = artifactType;
       
        this.signature = signature;
    }


    public SigningBundle( String artifactType, String classifier,
                          File signature )
    {
        this.artifactType = artifactType;
        this.classifier = classifier;
        this.signature = signature;
    }

    public String getArtifactType()
    {
        return artifactType;
    }

    public File getSignature()
    {
        return signature;
    }

    public String getClassifier()
    {
        return classifier;
    }
}
