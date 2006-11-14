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
/**
 * 
 */
package org.apache.maven.plugin.dependency.utils.filters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.maven.plugin.dependency.utils.SilentLog;
import org.apache.maven.plugin.logging.Log;

/**
 * @author brianf
 * 
 */
public class TestFilterArtifacts
    extends TestCase
{
    Log log = new SilentLog();

    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    public void testArtifactFilter()
    {
        Set a = new HashSet();
        FilterArtifacts fa = new FilterArtifacts();
        ArtifactsFilter scope = new ScopeFilter( "compile", "system" );
        ArtifactsFilter type = new TypeFilter( "jar", "war" );
        ArtifactsFilter trans = new TransitivityFilter( a, true );

        assertEquals( 0, fa.getFilters().size() );
        fa.addFilter( scope );
        assertEquals( 1, fa.getFilters().size() );
        fa.addFilter( type );
        assertEquals( 2, fa.getFilters().size() );
        assertTrue( fa.getFilters().get( 0 ) instanceof ScopeFilter );
        assertTrue( fa.getFilters().get( 1 ) instanceof TypeFilter );
        fa.addFilter( 1, trans );
        assertEquals( 3, fa.getFilters().size() );
        assertTrue( fa.getFilters().get( 0 ) instanceof ScopeFilter );
        assertTrue( fa.getFilters().get( 1 ) instanceof TransitivityFilter );
        assertTrue( fa.getFilters().get( 2 ) instanceof TypeFilter );

        ArrayList list = new ArrayList();
        list.addAll( fa.getFilters() );

        fa.clearFilters();
        assertEquals( 0, fa.getFilters().size() );

        fa.setFilters( list );
        assertEquals( 3, fa.getFilters().size() );
        assertTrue( fa.getFilters().get( 0 ) instanceof ScopeFilter );
        assertTrue( fa.getFilters().get( 1 ) instanceof TransitivityFilter );
        assertTrue( fa.getFilters().get( 2 ) instanceof TypeFilter );

    }

}
