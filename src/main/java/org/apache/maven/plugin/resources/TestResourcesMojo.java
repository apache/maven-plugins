package org.apache.maven.plugin.resources;

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
import java.util.List;

/**
 * Copy resources for the test source code to the test output directory.
 * Always uses the project.build.testResources element to specify the resources to copy.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @goal testResources
 * @phase process-test-resources
 * @threadSafe
 * 
 */
public class TestResourcesMojo
    extends ResourcesMojo
{
    /**
     * The output directory into which to copy the resources.
     *
     * @parameter expression="${project.build.testOutputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * The list of resources we want to transfer.
     *
     * @parameter expression="${project.testResources}"
     * @required
     * @readonly
     */
    private List resources;

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public List getResources()
    {
        return resources;
    }

    public void setResources( List resources )
    {
        this.resources = resources;
    }

}
