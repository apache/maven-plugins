package invalid.format;

/**
 * @author Maria Odea Ching
 */
public class AppSample
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
     * @return a blank String
     */
    private String unusedMethod( String unusedParam )
    {
        System.out.println( "This is just a test." );

        return "";
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