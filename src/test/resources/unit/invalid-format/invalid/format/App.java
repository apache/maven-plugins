package invalid.format;

/**
 * This is a sample class used for testing
 *
 * @author Maria Odea Ching
 */
public class App
{
    protected String unusedVar1;

    private int unusedVar2;

    String unusedvar3;

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
        try
        {
            System.out.println( str );
        }
        catch ( Exception e )
        {

        }
    }

    /**
     * Test method
     *
     * @param unusedParam1
     * @param unusedParam2
     */
    public void testMethod( String unusedParam1, String unusedParam2)
    {
        System.out.println( "Test method" );
    }

    /**
     * Sample duplicate code
     *
     * @param str
     */
    public void duplicateMethod( String str )
    {
        System.out.println( "This is a sample duplicate method." );
        System.out.println( "The value of str is " + str );
    }

}