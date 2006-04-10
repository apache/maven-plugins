/*
 *  Copyright 2005-2006 Brian Fox (brianefox@gmail.com)
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
/**
 * 
 */
package org.apache.maven.plugin.dependency.utils.filters;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.dependency.utils.SilentLog;

/**
 * @author brianf
 *
 */
public class TestTransitivityFilter
    extends TestCase
{
    Set artifacts = new HashSet();

    Set directArtifacts = new HashSet();

    Log log = new SilentLog();

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArtifactHandler ah = new DefaultArtifactHandler();
        VersionRange vr = VersionRange.createFromVersion( "1.1" );
        Artifact artifact = new DefaultArtifact( "test", "1", vr, Artifact.SCOPE_COMPILE, "jar", "", ah, false );
        artifacts.add( artifact );
        artifact = new DefaultArtifact( "test", "2", vr, Artifact.SCOPE_COMPILE, "war", "", ah, false );
        artifacts.add( artifact );
        directArtifacts.add( artifact );
        artifact = new DefaultArtifact( "test", "3", vr, Artifact.SCOPE_COMPILE, "sources", "", ah, false );
        artifacts.add( artifact );
        directArtifacts.add( artifact );
        artifact = new DefaultArtifact( "test", "4", vr, Artifact.SCOPE_COMPILE, "zip", "", ah, false );
        artifacts.add( artifact );
    }

    public void testAll()
    {
        TransitivityFilter filter = new TransitivityFilter( directArtifacts, false );

        Set result = filter.filter( artifacts, log );

        assertEquals( result.size(), 4 );
    }

    public void testExclude()
    {
        TransitivityFilter filter = new TransitivityFilter( directArtifacts, true );

        Set result = filter.filter( artifacts, log );

        assertEquals( result.size(), 2 );

        Iterator iter = result.iterator();
        while ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertTrue( artifact.getArtifactId().equals( "2" ) || artifact.getArtifactId().equals( "3" ) );
        }
    }

}
