package org.apache.maven.plugin.invoker;

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

import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.plugin.invoker.model.BuildJob;

import junit.framework.TestCase;

public class BuildJobComparatorTest
    extends TestCase
{

    private BuildJobComparator comparator = new BuildJobComparator( Collections.singletonList( "setup*/pom.xml" ) );

    public void testComparator()
    {
        BuildJob setup = new BuildJob( "setup/pom.xml", BuildJob.Type.DIRECT );
        BuildJob setup2 = new BuildJob( "setup2/pom.xml", BuildJob.Type.DIRECT );
        BuildJob normal = new BuildJob( "normal/pom.xml", BuildJob.Type.DIRECT );
        BuildJob normal2 = new BuildJob( "normal2/pom.xml", BuildJob.Type.DIRECT );

        assertEquals( 0, comparator.compare( setup, setup2 ) );
        assertEquals( 0, comparator.compare( normal, normal2 ) );

        assertEquals( -1, comparator.compare( setup, normal ) );
        assertEquals( 1, comparator.compare( normal, setup ) );

        BuildJob[] jobs = new BuildJob[] { normal, setup, normal2, setup2 };
        Arrays.sort( jobs, comparator );

        assertEquals( setup, jobs[0] );
        assertEquals( setup2, jobs[1] );
        assertEquals( normal, jobs[2] );
        assertEquals( normal2, jobs[3] );
    }

}
