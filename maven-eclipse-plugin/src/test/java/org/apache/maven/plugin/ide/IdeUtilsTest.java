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
package org.apache.maven.plugin.ide;

import junit.framework.TestCase;

/**
 * Test for {@link IdeUtils}
 * 
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class IdeUtilsTest
    extends TestCase
{

    public void testGetProjectNameStringIdeDependency()
    {
        IdeDependency dependency = new IdeDependency();
        dependency.setGroupId( "g" );
        dependency.setArtifactId( "a" );
        dependency.setVersion( "v" );

        String name = IdeUtils.getProjectName( IdeUtils.PROJECT_NAME_DEFAULT_TEMPLATE, dependency );
        assertEquals( dependency.getArtifactId(), name );

        name = IdeUtils.getProjectName( IdeUtils.PROJECT_NAME_WITH_GROUP_AND_VERSION_TEMPLATE, dependency );
        assertEquals( dependency.getGroupId() + "." + dependency.getArtifactId() + "-" + dependency.getVersion(), name );

        name = IdeUtils.getProjectName( IdeUtils.PROJECT_NAME_WITH_GROUP_TEMPLATE, dependency );
        assertEquals( dependency.getGroupId() + "." + dependency.getArtifactId(), name );

        name = IdeUtils.getProjectName( IdeUtils.PROJECT_NAME_WITH_VERSION_TEMPLATE, dependency );
        assertEquals( dependency.getArtifactId() + "-" + dependency.getVersion(), name );
    }

}
