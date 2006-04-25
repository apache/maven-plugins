package subpackages.test.included.exclude;

/**
 * Sample class in the package to be excluded in the included package.
 *
 * @author Maria Odea Ching
 */
public class ExcludedSubApp
{
    /**
     * The main method
     *
     * @param args  an array of strings that contains the arguments
     */
    public static void main( String[] args )
    {
        System.out.println( "Sample Excluded Application." );
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