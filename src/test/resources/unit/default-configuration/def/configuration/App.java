package def.configuration;

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