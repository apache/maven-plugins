package org.apache.maven.plugin.announcement;

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

import org.apache.maven.plugin.changes.AbstractChangesMojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Abstract superclass for announcement mojos.
 *
 * @version $Id$
 * @since 2.3
 */
public abstract class AbstractAnnouncementMojo
    extends AbstractChangesMojo
{
    /**
     * This will cause the execution to be run only at the top of a given module
     * tree. That is, run in the project contained in the same folder where the
     * mvn execution was launched.
     *
     * @since 2.3
     */
    @Parameter( property = "announcement.runOnlyAtExecutionRoot", defaultValue = "false" )
    protected boolean runOnlyAtExecutionRoot;
}
