package org.apache.maven.plugin.jdeps;

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

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.StringUtils;

/**
 * Check if test sources depends on internal JDK classes
 * 
 * @author Robert Scholte
 *
 */
@Mojo( name = "test-jdkinternal", 
       requiresDependencyResolution = ResolutionScope.TEST,
       defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES )
public class TestJDKInternalsMojo
    extends AbstractJDepsMojo
{

    @Override
    protected String getClassesDirectory()
    {
        return getProject().getBuild().getTestOutputDirectory();
    }
    
    @Override
    protected String getClassPath() throws DependencyResolutionRequiredException
    {
        return StringUtils.join( getProject().getTestClasspathElements().iterator(), File.pathSeparator );
    }
}
