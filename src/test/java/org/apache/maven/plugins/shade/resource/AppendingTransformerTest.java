package org.apache.maven.plugins.shade.resource;

import java.util.Locale;

import junit.framework.TestCase;

/**
 * Test for {@link AppendingTransformer}.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class AppendingTransformerTest
    extends TestCase
{

    private AppendingTransformer transformer;

    static
    {
        /*
         * NOTE: The Turkish locale has an usual case transformation for the letters "I" and "i", making it a prime
         * choice to test for improper case-less string comparisions.
         */
        Locale.setDefault( new Locale( "tr" ) );
    }

    public void setUp()
    {
        this.transformer = new AppendingTransformer();
    }

    public void testCanTransformResource()
    {
        this.transformer.resource = "abcdefghijklmnopqrstuvwxyz";

        assertTrue( this.transformer.canTransformResource( "abcdefghijklmnopqrstuvwxyz" ) );
        assertTrue( this.transformer.canTransformResource( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" ) );
        assertFalse( this.transformer.canTransformResource( "META-INF/MANIFEST.MF" ) );
    }

}
