package custom.configuration;

/**
 * @author Maria Odea Ching
 */
public class Sample
{       
    private String unusedVar = "UNUSED";

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
     * @param unusedParam
     * @param unusedParam2
     * @return a blank String
     */
    private String unusedMethod( String unusedParam, String unusedParam2 )
    {
        System.out.println( "This is just a test." );

        return "";
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