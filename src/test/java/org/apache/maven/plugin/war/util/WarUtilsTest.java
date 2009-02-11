package org.apache.maven.plugin.war.util;

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

import junit.framework.TestCase;
import org.apache.maven.model.Dependency;

/**
 * Test the WarUtils.
 *
 * @author Dennis Lundberg
 * @version $Id$
 */
public class WarUtilsTest
    extends TestCase
{
    /**
     * Test for MWAR-160.
     */
    public void testDependencyEquals()
    {
        Dependency firstDependency = new Dependency();
        firstDependency.setGroupId( "1" );
        firstDependency.setArtifactId( "a" );
        Dependency secondDependency = new Dependency();
        secondDependency.setGroupId( "2" );
        secondDependency.setArtifactId( "b" );
        Dependency thirdDependency = new Dependency();
        thirdDependency.setGroupId( "1" );
        thirdDependency.setArtifactId( "c" );
        Dependency fourthDependency = new Dependency();
        fourthDependency.setGroupId( "4" );
        fourthDependency.setArtifactId( "a" );

        assertFalse( "dependencies 1:a and 2:b should not be equal", WarUtils.dependencyEquals( firstDependency,
                                                                                                secondDependency ) );
        assertFalse( "dependencies 1:a and 1:c should not be equal", WarUtils.dependencyEquals( firstDependency,
                                                                                                thirdDependency ) );
        assertFalse( "dependencies 1:a and 4:a should not be equal", WarUtils.dependencyEquals( firstDependency,
                                                                                                fourthDependency ) );
        assertFalse( "dependencies 2:b and 1:c should not be equal", WarUtils.dependencyEquals( secondDependency,
                                                                                                thirdDependency ) );
        assertFalse( "dependencies 2:b and 4:a should not be equal", WarUtils.dependencyEquals( secondDependency,
                                                                                                fourthDependency ) );
        assertFalse( "dependencies 1:c and 4:a should not be equal", WarUtils.dependencyEquals( thirdDependency,
                                                                                                fourthDependency ) );
    }
}