package org.apache.maven.changes;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.List;

public class ReleaseTest extends TestCase
{
    Release release = new Release();

    public ReleaseTest( String testName )
    {
        super( testName );
    }

    protected void setUp() throws Exception 
    {
    }

    protected void tearDown() throws Exception 
    {
    }

    public static Test suite() 
    {
        TestSuite suite = new TestSuite( ReleaseTest.class );

        return suite;
    }

    public void testGetSetVersion() 
    {
        release.setVersion( "version" );

        assertEquals( "version", release.getVersion() );
    }

    public void testGetSetDateRelease() 
    {
        release.setDateRelease( "12-09-1979" );

        assertEquals( "12-09-1979", release.getDateRelease() );
    }

    public void testGetSetAction() 
    {
        List actionList = new ArrayList();

        release.setAction( actionList );

        assertEquals( actionList, release.getAction() );
    }
    
}
