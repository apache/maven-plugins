package org.apache.maven.plugin.dependency.utils;
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

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.dependency.AbstractDependencyMojoTestCase;
import org.apache.maven.plugin.logging.Log;

public class TestDependencyStatusSets
    extends AbstractDependencyMojoTestCase
{

    protected void setUp()
        throws Exception
    {
        // required for mojo lookups to work
        super.setUp( "dss", true );
    }

    public void testDependencyStatusSettersGetters()
    {
        /*
         * DependencyStatusSets dss = new DependencyStatusSets();
         * 
         * Set set = new HashSet();
         * 
         * dss.setResolvedDependencies( set ); assertSame( set,
         * dss.getResolvedDependencies() );
         * 
         * set = new HashSet();
         * 
         * dss.setUnResolvedDependencies( set ); assertSame( set,
         * dss.getUnResolvedDependencies() );
         * 
         * set = new HashSet();
         * 
         * dss.setSkippedDependencies( set ); assertSame( set,
         * dss.getSkippedDependencies() );
         * 
         * assertNotSame( dss.getResolvedDependencies(),
         * dss.getSkippedDependencies() ); assertNotSame(
         * dss.getResolvedDependencies(), dss.getUnResolvedDependencies() );
         * assertNotSame( dss.getSkippedDependencies(),
         * dss.getUnResolvedDependencies() );
         */
    }

    public void testDependencyStatusConstructor()
    {
        /*
         * Set r = new HashSet(); Set u = new HashSet(); Set s = new HashSet();
         * 
         * DependencyStatusSets dss = new DependencyStatusSets( r, u, s );
         * 
         * assertSame( r, dss.getResolvedDependencies() ); assertSame( u,
         * dss.getUnResolvedDependencies() ); assertSame( s,
         * dss.getSkippedDependencies() );
         */
    }

    public void testDependencyStatusLog()
        throws IOException
    {
        Set artifacts = this.stubFactory.getMixedArtifacts();
        doTestDependencyStatusLog( artifacts );
    }

    public void testDependencyStatusLogNullFiles()
        throws IOException
    {
        this.stubFactory.setCreateFiles(false);
        Set artifacts = this.stubFactory.getMixedArtifacts();
        doTestDependencyStatusLog( artifacts );
    }

    public void testDependencyStatusEmptySet()
    {
        doTestDependencyStatusLog( new HashSet() );
    }

    public void doTestDependencyStatusLog( Set artifacts )
    {
        // TODO: implement logger to check correct output
        // this test is just looking for unexpected exceptions.
        Log log = new SilentLog();

        DependencyStatusSets dss = new DependencyStatusSets();
        dss.logStatus( log, false );
        dss.logStatus( log, true );

        dss = new DependencyStatusSets( artifacts, null, null );
        dss.logStatus( log, false );
        dss.logStatus( log, true );

        dss = new DependencyStatusSets( null, artifacts, null );
        dss.logStatus( log, false );
        dss.logStatus( log, true );

        dss = new DependencyStatusSets( null, null, artifacts );
        dss.logStatus( log, false );
        dss.logStatus( log, true );

        dss = new DependencyStatusSets( artifacts, artifacts, null );
        dss.logStatus( log, false );
        dss.logStatus( log, true );

        dss = new DependencyStatusSets( null, artifacts, artifacts );
        dss.logStatus( log, false );
        dss.logStatus( log, true );

        dss = new DependencyStatusSets( artifacts, null, artifacts );
        dss.logStatus( log, false );
        dss.logStatus( log, true );

        dss = new DependencyStatusSets( artifacts, artifacts, artifacts );
        dss.logStatus( log, false );
        dss.logStatus( log, true );

    }
}
