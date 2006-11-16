/*
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

package org.apache.maven.plugin.eclipse;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Tests the BuildCommand class.
 *
 * @author <a href="mailto:kenneyw@neonics.com">Kenney Westerhof</a>
 */
public class BuildCommandTest
    extends TestCase
{
    /**
     * Tests various equalities for buildCommands, needed to remove duplicate build commands from <code>.project</code>.
     */
    public void testEquals()
    {
        BuildCommand b1 = new BuildCommand( "foobuilder", null, (Map) null );
        BuildCommand b2 = new BuildCommand( "foobuilder", "", (Map) null );
        assertEquals( false, b1.equals( b2 ) );
        assertEquals( false, b2.equals( b1 ) );

        b2 = new BuildCommand( "foobuilder", null, (Map) null );
        assertEquals( true, b1.equals( b2 ) );
        assertEquals( true, b2.equals( b1 ) );

        b2 = new BuildCommand( "foobuilder", null, new HashMap() );
        assertEquals( true, b1.equals( b2 ) );
        assertEquals( true, b2.equals( b1 ) );

        Map m1 = new HashMap();
        Map m2 = new HashMap();

        b1 = new BuildCommand( "foobuilder", null, m1 );
        b2 = new BuildCommand( "foobuilder", null, m2 );
        assertEquals( true, b1.equals( b2 ) );
        assertEquals( true, b2.equals( b1 ) );

        m1.put( "arg1", "value1" );
        m2.put( "arg1", "value1" );
        b1 = new BuildCommand( "foobuilder", null, m1 );
        b2 = new BuildCommand( "foobuilder", null, m2 );
        assertEquals( true, b1.equals( b2 ) );
        assertEquals( true, b2.equals( b1 ) );

        m2.put( "arg1", "foo" );
        b1 = new BuildCommand( "foobuilder", null, m1 );
        b2 = new BuildCommand( "foobuilder", null, m2 );
        assertEquals( false, b1.equals( b2 ) );
        assertEquals( false, b2.equals( b1 ) );
    }
}
