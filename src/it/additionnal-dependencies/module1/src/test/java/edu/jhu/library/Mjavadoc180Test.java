package edu.jhu.library;

import junit.framework.TestCase;
import org.dbunit.database.IDatabaseConnection;

/**
 * Created by IntelliJ IDEA.
 * User: esm
 * Date: May 17, 2008
 * Time: 11:28:04 AM
 * To change this template use File | Settings | File Templates.
 */
public class Mjavadoc180Test
    extends TestCase
{
    /**
     * This is some test javadoc.  This test method has a phony dependency on DB Unit.
     */
    public void testMJAVADOC180()
    {
        IDatabaseConnection phony = null;
        final HelloWorld hw = new HelloWorld();
        assertTrue( "Hello World".equals( hw.hello( "Hello World" ) ) );
    }
}
