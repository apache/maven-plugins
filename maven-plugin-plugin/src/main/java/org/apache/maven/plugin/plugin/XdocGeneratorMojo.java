package org.apache.maven.plugin.plugin;

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

import org.apache.maven.tools.plugin.generator.Generator;
import org.apache.maven.tools.plugin.generator.PluginXdocGenerator;

import java.io.File;

/**
 * Generate Xdoc files for the project mojos or goals
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @goal xdoc
 */
public class XdocGeneratorMojo
    extends AbstractGeneratorMojo
{
    /**
     * The directory where the generated Xdoc files will be put.
     *
     * @parameter expression="${project.build.directory}/generated-site/xdoc"
     * @required
     */
    protected File outputDirectory;

    protected File getOutputDirectory()
    {
        return outputDirectory;
    }

    protected Generator createGenerator()
    {
        return new PluginXdocGenerator();
    }
}
