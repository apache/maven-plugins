package def.configuration;

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

    public String dup( String str )
    {
        String tmp = "";

        for( int i = 0; i < str.length(); i++ )
        {
            if ( i != ( str.length() -1 ) )
            {
                tmp = tmp + str.substring( i, i + 1);
            }
            else
            {
                tmp = tmp + str.substring( i );
            }
        }

        if ( "".equals( tmp ) || tmp == null )
        {
                tmp = "EMPTY";
        }

        System.out.println( "The value of tmp is " + tmp );
        return tmp;
    }

}