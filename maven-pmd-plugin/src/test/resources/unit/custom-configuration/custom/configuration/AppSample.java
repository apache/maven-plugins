package custom.configuration;

/**
 * @author Maria Odea Ching
 */
public class AppSample
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
     * @param unusedParam
     * @return a blank String
     */
    private String unusedMethod( String unusedParam )
    {
        try
        {
            System.out.println( "This is just a test." );
        }
        catch ( Exception e )
        {
            
        }

        return "";
    }
}