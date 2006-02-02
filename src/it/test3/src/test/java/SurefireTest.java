import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SurefireTest
    extends TestCase
{

    public SurefireTest( )
    {
        super( );
    }

    public SurefireTest( String name )
    {
        super( name );
    }


    public void testQuote()
    {
        fail( "\"" );
    }

    public void testLower()
    {
        fail( "<" );
    }

    public void testGreater()
    {
        fail( ">" );
    }

}
