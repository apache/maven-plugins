package org.apache.maven.plugins.pmd;

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

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @param <D> type of violation to exclude, e.g. {@link org.apache.maven.plugins.pmd.model.Violation}
 * or {@link org.apache.maven.plugins.pmd.model.Duplication}.
 * @author Andreas Dangel
 */
public interface ExcludeFromFile<D>
{
    /**
     * Loads the exclude definitions from the given file.
     *
     * @param excludeFromFailureFile the path to the properties file
     * @throws MojoExecutionException if the properties file couldn't be loaded
     */
    void loadExcludeFromFailuresData( String excludeFromFailureFile ) throws MojoExecutionException;

    /**
     * Determines how many exclusions are considered.
     * @return the number of active exclusions
     */
    int countExclusions();

    /**
     * Checks whether the given violation is excluded. Note: the exclusions must have been
     * loaded before via {@link #loadExcludeFromFailuresData(String)}.
     *
     * @param errorDetail the violation to check
     * @return <code>true</code> if the violation should be excluded, <code>false</code> otherwise.
     */
    boolean isExcludedFromFailure( D errorDetail );

}
