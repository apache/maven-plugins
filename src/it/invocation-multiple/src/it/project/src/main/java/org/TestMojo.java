package org;

import java.io.*;
import java.util.*;

import org.apache.maven.plugin.*;

/**
 * @goal test
 */
public class TestMojo extends AbstractMojo {

    /**
     * @parameter default-value="${project.build.directory}/test.txt"
     */
    private File outputFile;

    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        outputFile.getParentFile().mkdirs();
        Writer writer = null;
        try
        {
            writer = new OutputStreamWriter( new FileOutputStream( outputFile ), "UTF-8" );
            writer.write( System.getProperty( "file.encoding" ) );
        }
        catch (IOException e)
        {
            throw new MojoExecutionException( "Failed", e );
        }
        finally
        {
            if ( writer != null )
            {
                try
                {
                    writer.close();
                }
                catch (IOException e)
                {
                    // ignore
                }
            }
        }
    }

}
