package org.apache.maven.plugins.verifier;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.verifier.model.File;

/**
 * 
 */
public class VerificationResult
{
    private List<File> existenceFailures = new ArrayList<File>();

    private List<File> nonExistenceFailures = new ArrayList<File>();

    private List<File> contentFailures = new ArrayList<File>();

    /**
     * @param file {@link File}
     */
    public void addExistenceFailure( File file )
    {
        existenceFailures.add( file );
    }

    /**
     * Added non existence failure.
     * 
     * @param file {@linke File}
     */
    public void addNonExistenceFailure( File file )
    {
        nonExistenceFailures.add( file );
    }

    /**
     * Add content failure.
     * 
     * @param file {@link File}
     */
    public void addContentFailure( File file )
    {
        contentFailures.add( file );
    }

    /**
     * @return {@link #existenceFailures}
     */
    public List<File> getExistenceFailures()
    {
        return existenceFailures;
    }

    /**
     * @return {@link #nonExistenceFailures}
     */
    public List<File> getNonExistenceFailures()
    {
        return nonExistenceFailures;
    }

    /**
     * @return {@link #contentFailures}
     */
    public List<File> getContentFailures()
    {
        return contentFailures;
    }

    /**
     * @return true if a failures exists false otherwise.
     */
    public boolean hasFailures()
    {
        return !getExistenceFailures().isEmpty() || !getNonExistenceFailures().isEmpty()
            || !getContentFailures().isEmpty();
    }
}
