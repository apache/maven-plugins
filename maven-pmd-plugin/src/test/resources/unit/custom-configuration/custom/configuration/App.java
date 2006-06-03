package custom.configuration;

/**
 * This is a sample class used for testing
 *
 * @author Maria Odea Ching
 */
public class App
{

    /**
     * trigger Unnecessary constructor rule 
     */
    public App() {}
    
    /**
     * The main method
     *
     * @param args  an array of strings that contains the arguments
     */
    public static void main( String[] args )
    {
        System.out.println( "Sample Application." );
    }

    /**
     * Sample method
     *
     * @param str   the value to be displayed
     */
    protected void sampleMethod( String str )
    {
        if ( str.equals( "RED" ) )
            System.out.println( "The color is red." );
        else if ( str.equals( "BLLUE" ) )
            System.out.println( "The color is blue." );

    }

    /**
     * Test method
     *
     * @param unusedParam1
     * @param unusedParam2
     */
    public void testMethod( String unusedParam1, String unusedParam2 )
    {
        System.out.println( "Test method" );
    }

}
