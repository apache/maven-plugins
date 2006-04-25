package aggregate.test.project2;

import java.io.File;
import java.io.IOException;

/**
 * Sample class inside the package to be included in the javadoc
 * 
 * @author Maria Odea Ching
 */
public class Project2Test
{
    /**
     * Contains the file to be set
     */
    protected File file;

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
     * Setter method for variable file
     *
     * @param file the value to be set
     */
    public void setFile( File file )
    {
        this.file = file;
    }

    /**
     * Getter method for variable file
     *
     * @return a File object
     */
    public File getFile()
    {
        return file;
    }

    /**
     * Create new file
     *
     * @throws java.io.IOException  thrown if an I/O error occurred during file creation
     */
    public void createFile()
        throws IOException
    {
        File f = new File( file.getAbsolutePath() );
        f.createNewFile();
    }

}