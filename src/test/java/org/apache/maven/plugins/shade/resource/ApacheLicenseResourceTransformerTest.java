package org.apache.maven.plugins.shade.resource;

import java.util.Locale;

import junit.framework.TestCase;

/**
 * Test for {@link ApacheLicenseResourceTransformer}.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class ApacheLicenseResourceTransformerTest
    extends TestCase
{

    private ApacheLicenseResourceTransformer transformer;

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
        this.transformer = new ApacheLicenseResourceTransformer();
    }

    public void testCanTransformResource()
    {
        assertTrue( this.transformer.canTransformResource( "META-INF/LICENSE" ) );
        assertTrue( this.transformer.canTransformResource( "META-INF/LICENSE.TXT" ) );
        assertTrue( this.transformer.canTransformResource( "META-INF/License.txt" ) );
        assertFalse( this.transformer.canTransformResource( "META-INF/MANIFEST.MF" ) );
    }

}
