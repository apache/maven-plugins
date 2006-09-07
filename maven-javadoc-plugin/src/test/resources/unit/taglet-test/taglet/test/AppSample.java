package taglet.test;

/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;

/**
 * Sample class inside the package to be included in the javadoc
 *
 * @todo To test!
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class AppSample
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
