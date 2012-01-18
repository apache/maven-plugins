package test;

public class MyClass
{

    /**
     * 
     * @param args
     */
    public static void main( String[] args )
    {
        int nullArgs = 0;
        int emptyArgs = 0;
        int notEmptyArgs = 0;
        for ( int i = 0; i < args.length; i++ )
        {
            if( args[i] == null )
            {
                nullArgs++;
                System.out.println( "arg[" + i + "] is null, weird" );
            }
            else if( args[i] == "" )
            {
                emptyArgs++;
                System.out.println( "arg[" + i + "] is empty" );
            }
            else
            {
                notEmptyArgs++;
                System.out.println( "arg[" + i + "] is not empty" );
            }
            System.out.print( "Number of null args: " + nullArgs );
            System.out.print( "Number of empty args: " + emptyArgs );
            System.out.print( "Number of not empty args: " + notEmptyArgs );
        }
    }
}