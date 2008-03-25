package ${package};

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit ${package} for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the ${package} case
     *
     * @param ${package}Name name of the ${package} case
     */
    public AppTest( String ${package}Name )
    {
        super( ${package}Name );
    }

    /**
     * @return the suite of ${package}s being ${package}ed
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void ${package}App()
    {
        assertTrue( true );
    }
}
