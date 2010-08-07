package org.apache.maven.plugins.shade.relocation;

import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Test for {@link SimpleRelocator}.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class SimpleRelocatorTest
    extends TestCase
{

    public void testCanRelocatePath()
    {
        SimpleRelocator relocator;

        relocator = new SimpleRelocator( "org.foo", null, null, null );
        assertEquals( true, relocator.canRelocatePath( "org/foo/Class" ) );
        assertEquals( true, relocator.canRelocatePath( "org/foo/Class.class" ) );
        assertEquals( true, relocator.canRelocatePath( "org/foo/bar/Class" ) );
        assertEquals( true, relocator.canRelocatePath( "org/foo/bar/Class.class" ) );
        assertEquals( false, relocator.canRelocatePath( "com/foo/bar/Class" ) );
        assertEquals( false, relocator.canRelocatePath( "com/foo/bar/Class.class" ) );
        assertEquals( false, relocator.canRelocatePath( "org/Foo/Class" ) );
        assertEquals( false, relocator.canRelocatePath( "org/Foo/Class.class" ) );

        relocator =
            new SimpleRelocator( "org.foo", null, null, Arrays.asList( new String[] { "org.foo.Excluded", "org.foo.public.*",
                "org.foo.Public*Stuff" } ) );
        assertEquals( true, relocator.canRelocatePath( "org/foo/Class" ) );
        assertEquals( true, relocator.canRelocatePath( "org/foo/Class.class" ) );
        assertEquals( true, relocator.canRelocatePath( "org/foo/excluded" ) );
        assertEquals( false, relocator.canRelocatePath( "org/foo/Excluded" ) );
        assertEquals( false, relocator.canRelocatePath( "org/foo/Excluded.class" ) );
        assertEquals( false, relocator.canRelocatePath( "org/foo/public" ) );
        assertEquals( false, relocator.canRelocatePath( "org/foo/public/Class" ) );
        assertEquals( false, relocator.canRelocatePath( "org/foo/public/Class.class" ) );
        assertEquals( true, relocator.canRelocatePath( "org/foo/publicRELOC/Class" ) );
        assertEquals( true, relocator.canRelocatePath( "org/foo/PrivateStuff" ) );
        assertEquals( true, relocator.canRelocatePath( "org/foo/PrivateStuff.class" ) );
        assertEquals( false, relocator.canRelocatePath( "org/foo/PublicStuff" ) );
        assertEquals( false, relocator.canRelocatePath( "org/foo/PublicStuff.class" ) );
        assertEquals( false, relocator.canRelocatePath( "org/foo/PublicUtilStuff" ) );
        assertEquals( false, relocator.canRelocatePath( "org/foo/PublicUtilStuff.class" ) );
    }

    public void testCanRelocateClass()
    {
        SimpleRelocator relocator;

        relocator = new SimpleRelocator( "org.foo", null, null, null );
        assertEquals( true, relocator.canRelocateClass( "org.foo.Class" ) );
        assertEquals( true, relocator.canRelocateClass( "org.foo.bar.Class" ) );
        assertEquals( false, relocator.canRelocateClass( "com.foo.bar.Class" ) );
        assertEquals( false, relocator.canRelocateClass( "org.Foo.Class" ) );

        relocator =
            new SimpleRelocator( "org.foo", null, null, Arrays.asList( new String[] { "org.foo.Excluded", "org.foo.public.*",
                "org.foo.Public*Stuff" } ) );
        assertEquals( true, relocator.canRelocateClass( "org.foo.Class" ) );
        assertEquals( true, relocator.canRelocateClass( "org.foo.excluded" ) );
        assertEquals( false, relocator.canRelocateClass( "org.foo.Excluded" ) );
        assertEquals( false, relocator.canRelocateClass( "org.foo.public" ) );
        assertEquals( false, relocator.canRelocateClass( "org.foo.public.Class" ) );
        assertEquals( true, relocator.canRelocateClass( "org.foo.publicRELOC.Class" ) );
        assertEquals( true, relocator.canRelocateClass( "org.foo.PrivateStuff" ) );
        assertEquals( false, relocator.canRelocateClass( "org.foo.PublicStuff" ) );
        assertEquals( false, relocator.canRelocateClass( "org.foo.PublicUtilStuff" ) );
    }

    public void testRelocatePath()
    {
        SimpleRelocator relocator;

        relocator = new SimpleRelocator( "org.foo", null, null, null );
        assertEquals( "hidden/org/foo/bar/Class.class", relocator.relocatePath( "org/foo/bar/Class.class" ) );

        relocator = new SimpleRelocator( "org.foo", "private.stuff", null, null );
        assertEquals( "private/stuff/bar/Class.class", relocator.relocatePath( "org/foo/bar/Class.class" ) );
    }

    public void testRelocateClass()
    {
        SimpleRelocator relocator;

        relocator = new SimpleRelocator( "org.foo", null, null, null );
        assertEquals( "hidden.org.foo.bar.Class", relocator.relocateClass( "org.foo.bar.Class" ) );

        relocator = new SimpleRelocator( "org.foo", "private.stuff", null, null );
        assertEquals( "private.stuff.bar.Class", relocator.relocateClass( "org.foo.bar.Class" ) );
    }

}
