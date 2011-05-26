package org.apache.maven.plugins.shade;

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
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Interface that defines the process of shading. 
 *
 */
public interface Shader
{
    String ROLE = Shader.class.getName();

    /**
     * Perform a shading operation.
     * @param jars which jars
     * @param uberJar output jar
     * @param filters the filters
     * @param relocators the relocators
     * @param resourceTransformers the transformers
     * @throws IOException for IO errors reading the thing
     * @throws MojoExecutionException for anything else that goes wrong.
     */
    void shade( Set jars, File uberJar, List filters, List relocators, List resourceTransformers )
        throws IOException, MojoExecutionException;
}
