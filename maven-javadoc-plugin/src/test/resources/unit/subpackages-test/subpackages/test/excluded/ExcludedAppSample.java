package subpackages.test.excluded;

import java.io.File;
import java.io.IOException;

/**
 * Sample class inside the package to be excluded in the javadoc
 *
 * @author Maria Odea Ching
 */
public class ExcludedAppSample
{

    /**
     * Test variable
     */
    protected String test;

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
     * Create new file
     *
     * @throws IOException  thrown if an I/O error occurred during file creation
     */
    public void createFile( File file )
        throws IOException
    {
        File f = new File( file.getAbsolutePath() );
        f.createNewFile();
    }

}