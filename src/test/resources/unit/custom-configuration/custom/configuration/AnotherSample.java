package custom.configuration;

/**
 * @author Maria Odea Ching
 */
public class AnotherSample
{

    /**
     * The main method
     *
     * @param args  an array of strings that contains the arguments
     */
    public static void main( String[] args )
    {
        System.out.println( "Another Sample Application" );
    }

    /**
     * Unused method
     *
     * @return a blank String
     */
    private String unusedMethod( )
    {
        System.out.println( "This is just a test." );

        return "";
    }

    /**
     *
     * @param tst
     */
    public void sample( String tst )
    {
        if ( tst.equals("") )
            System.out.println( "Empty string." );
        else
            System.out.println( "String is not empty" );
    }

    /**
     * Sample duplicate method
     *
     * @param i
     */
    public void duplicateMethod( int i )
    {
        for( i = 0; i <= 5; i++ )
        {
            System.out.println( "The value of i is " + i );
        }

        i = i + 20;

        System.out.println( "The new value of i is " + i );
    }
    
}