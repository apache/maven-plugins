package custom.configuration;

/**
 * @deprecated This class is already deprecated. Replaced by {@link java.lang.String}
 * @author Maria Odea Ching
 */
public class App
{

    /**
     * Contains the sample value
     */
    private String sample = "SAMPLE";

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
     * Sample method that prints out the parameter string.
     *
     * @param str   The string value to be printed.
     */
    protected void sampleMethod( String str )
    {
        System.out.println( str );
    }

}