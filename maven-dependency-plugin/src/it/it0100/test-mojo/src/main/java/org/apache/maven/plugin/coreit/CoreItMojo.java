package org.apache.maven.plugin.coreit;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import org.apache.maven.coreit.testdep.QuotableSolutions;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * test goal.
 *
 * @goal test
 * @phase test
 */
public class CoreItMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    public void execute()
        throws MojoExecutionException
    {
        String quote;
        boolean pass = true;

        QuotableSolutions quotes = new QuotableSolutions();
        quote = quotes.getQuoteViaThread( "teller" );
        if ( quote == null )
        {
            getLog().error( "Error using Thread.currentThread().getContextClassLoader()." );
            pass = false;
        }
        else
        {
            getLog().info( quote );
        }

        quote = quotes.getQuoteViaThis( "fuller" );
        if ( quote == null )
        {
            getLog().error( "Error using this.getClass().getClassLoader()" );
            pass = false;
        }
        else
        {
            getLog().info( quote );
        }

        if ( pass )
        {
            touch( outputDirectory, "mojo.success" );
        }
        else
        {
            throw new MojoExecutionException( "ClassLoader tests failed!" );
        }
    }

    private static void touch( File dir, String file )
        throws MojoExecutionException
    {
        try
        {
            if ( !dir.exists() )
            {
                dir.mkdirs();
            }

            File touch = new File( dir, file );

            FileWriter w = new FileWriter( touch );

            w.write( file );

            w.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error touching file", e );
        }
    }
}
