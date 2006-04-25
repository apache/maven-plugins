package javadocjar.invalid.destdir;

/**
 * This is a sample app class with javadoc
 *
 * @author Maria Odea Ching
 */
public class App
{

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
     * @param str   the string to be displayed
     */
    protected void sampleMethod( String str )
    {
        System.out.println( str );
    }

}