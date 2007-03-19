package org.apache.maven.plugin.enforcer;

import org.apache.maven.plugin.AbstractMojo;

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

/**
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * 
 */
public abstract class AbstractEnforcer
    extends AbstractMojo
{
    /**
     * Flag to fail the build if a version check fails.
     * 
     * @parameter expression="${enforcer.fail}" default-value="true"
     */
    protected boolean fail = true;

    /**
     * Flag to easily skip all checks
     * 
     * @parameter expression="${enforcer.skip}" default-value="false"
     */
    protected boolean skip = false;
}
