package custom.configuration;

/**
 * @author Maria Odea Ching
 * @since 1.4
 * @version %I%, %G%
 */
public class AppSample
{
    /**
     * Contains the value sample
     */
    private String sample = "SAMPLE";

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
     * @deprecated This method is already deprecated. Already replaced with {@link java.lang.String#concat(String)}
     */
    public void concat( String str )
    {
        System.out.println( "Sample deprecated method." );        
    }
}