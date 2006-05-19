/*
 * Copyright 2006 The Apache Software Foundation.
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
package org.apache.maven.plugin.clover;

import org.jmock.MockObjectTestCase;
import org.jmock.Mock;
import org.apache.maven.artifact.Artifact;

import java.util.Collections;

/**
 * Unit tests for {@link org.apache.maven.plugin.clover.CloverInstrumentInternalMojo}.
 *
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @version $Id$
 */
public class CloverInstrumentInternalMojoTest extends MockObjectTestCase
{
    public void testFindCloverArtifactWithCorrectArtifactIdButWrongGroupId()
    {
        Mock mockArtifact = mock(Artifact.class);
        mockArtifact.stubs().method( "getArtifactId" ).will( returnValue( "clover" ) );
        mockArtifact.stubs().method( "getGroupId" ).will( returnValue( "notcenquaid" ) );

        CloverInstrumentInternalMojo mojo = new CloverInstrumentInternalMojo();
        Artifact clover = mojo.findCloverArtifact( Collections.singletonList( mockArtifact.proxy() ) );

        assertNull( "Clover artifact should not have been found!", clover );
    }

    public void testFindCloverArtifactWhenCorrectIds()
    {
        Mock mockArtifact = mock(Artifact.class);
        mockArtifact.stubs().method( "getArtifactId" ).will( returnValue( "clover" ) );
        mockArtifact.stubs().method( "getGroupId" ).will( returnValue( "com.cenqua.clover" ) );

        CloverInstrumentInternalMojo mojo = new CloverInstrumentInternalMojo();
        Artifact clover = mojo.findCloverArtifact( Collections.singletonList( mockArtifact.proxy() ) );

        assertNotNull( "Clover artifact should have been found!", clover );
    }
}
