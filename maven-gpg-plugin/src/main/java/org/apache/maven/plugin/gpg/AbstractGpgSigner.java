package org.apache.maven.plugin.gpg;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * A base class for all classes that implements signing of files.
 *
 * @author Dennis Lundberg
 * @since 1.5
 */
public abstract class AbstractGpgSigner
{
    public static final String SIGNATURE_EXTENSION = ".asc";

    protected boolean useAgent;

    protected boolean isInteractive = true;

    protected boolean defaultKeyring = true;

    protected String keyname;

    private Log log;

    protected String passphrase;

    private File outputDir;

    private File buildDir;

    private File baseDir;

    protected File homeDir;

    protected String secretKeyring;

    protected String publicKeyring;

    protected String lockMode;

    protected List<String> args;

    public Log getLog()
    {
        return log;
    }

    public void setArgs( List<String> args )
    {
        this.args = args;
    }

    public void setInteractive( boolean b )
    {
        isInteractive = b;
    }

    public void setLockMode( String lockMode )
    {
        this.lockMode = lockMode;
    }

    public void setUseAgent( boolean b )
    {
        useAgent = b;
    }

    public void setDefaultKeyring( boolean enabled )
    {
        defaultKeyring = enabled;
    }

    public void setKeyName( String s )
    {
        keyname = s;
    }

    public void setLog( Log log )
    {
        this.log = log;
    }

    public void setPassPhrase( String s )
    {
        passphrase = s;
    }

    public void setOutputDirectory( File out )
    {
        outputDir = out;
    }

    public void setBuildDirectory( File out )
    {
        buildDir = out;
    }

    public void setBaseDirectory( File out )
    {
        baseDir = out;
    }

    public void setHomeDirectory( File homeDirectory )
    {
        homeDir = homeDirectory;
    }

    public void setSecretKeyring( String path )
    {
        secretKeyring = path;
    }

    public void setPublicKeyring( String path )
    {
        publicKeyring = path;
    }

    /**
     * Create a detached signature file for the provided file.
     *
     * @param file The file to sign
     * @return A reference to the generated signature file
     * @throws org.apache.maven.plugin.MojoExecutionException
     */
    public File generateSignatureForArtifact( File file )
        throws MojoExecutionException
    {
        // ----------------------------------------------------------------------------
        // Set up the file and directory for the signature file
        // ----------------------------------------------------------------------------

        File signature = new File( file + SIGNATURE_EXTENSION );

        boolean isInBuildDir = false;
        if ( buildDir != null )
        {
            File parent = signature.getParentFile();
            if ( buildDir.equals( parent ) )
            {
                isInBuildDir = true;
            }
        }
        if ( !isInBuildDir && outputDir != null )
        {
            String fileDirectory = "";
            File signatureDirectory = signature;

            while ( ( signatureDirectory = signatureDirectory.getParentFile() ) != null )
            {
                if ( !signatureDirectory.equals( baseDir ) )
                {
                    fileDirectory = signatureDirectory.getName() + File.separatorChar + fileDirectory;
                }
                else
                {
                    break;
                }
            }
            signatureDirectory = new File( outputDir, fileDirectory );
            if ( !signatureDirectory.exists() )
            {
                signatureDirectory.mkdirs();
            }
            signature = new File( signatureDirectory, file.getName() + SIGNATURE_EXTENSION );
        }

        if ( signature.exists() )
        {
            signature.delete();
        }

        // ----------------------------------------------------------------------------
        // Generate the signature file
        // ----------------------------------------------------------------------------

        generateSignatureForFile( file, signature );

        return signature;
    }

    /**
     * Generate the detached signature file for the provided file.
     *
     * @param file The file to sign
     * @param signature The file in which the generate signature will be put
     * @throws MojoExecutionException
     */
    protected abstract void generateSignatureForFile( File file, File signature )
        throws MojoExecutionException;

    private MavenProject findReactorProject( MavenProject prj )
    {
        if ( prj.getParent() != null && prj.getParent().getBasedir() != null && prj.getParent().getBasedir().exists() )
        {
            return findReactorProject( prj.getParent() );
        }
        return prj;
    }

    public String getPassphrase( MavenProject project )
        throws IOException
    {
        String pass = null;

        if ( project != null )
        {
            pass = project.getProperties().getProperty( "gpg.passphrase" );
            if ( pass == null )
            {
                MavenProject prj2 = findReactorProject( project );
                pass = prj2.getProperties().getProperty( "gpg.passphrase" );
            }
        }
        if ( pass == null )
        {
            pass = readPassword( "GPG Passphrase: " );
        }
        if ( project != null )
        {
            findReactorProject( project ).getProperties().setProperty( "gpg.passphrase", pass );
        }
        return pass;
    }

    private String readPassword( String prompt )
        throws IOException
    {
        try {
            return readPasswordJava16( prompt );
        } catch ( IOException e ) {
            throw e;
        }
        catch ( NoSuchMethodException e )
        {
            return readPasswordJava15( prompt );
        }
        catch ( IllegalAccessException e )
        {
            return readPasswordJava15( prompt );
        }
        catch ( InvocationTargetException e )
        {
            return readPasswordJava15( prompt );
        }
    }

    private String readPasswordJava16( String prompt )
        throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Method consoleMethod = System.class.getMethod( "console" );
        Object console = consoleMethod.invoke( null );
        if ( console == null )
        {
            throw new IllegalAccessException( "console was null" );
        }
        Method readPasswordMethod = console.getClass().getMethod( "readPassword", String.class, Object[].class );
        return new String( (char[]) readPasswordMethod.invoke( console, prompt, null ) );
    }

    private String readPasswordJava15( String prompt )
        throws IOException
    {
        BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );
        while ( System.in.available() != 0 )
        {
            // there's some junk already on the input stream, consume it
            // so we can get the real passphrase
            System.in.read();
        }

        System.out.print( prompt );
        System.out.print( ' ' );
        MaskingThread thread = new MaskingThread();
        thread.start();
        try
        {

            return in.readLine();
        }
        finally
        {
            // stop masking
            thread.stopMasking();

        }
    }

    // based on ideas from http://java.sun.com/developer/technicalArticles/Security/pwordmask/
    class MaskingThread
        extends Thread
    {
        private volatile boolean stop;

        /**
         * Begin masking until asked to stop.
         */
        public void run()
        {
            // this needs to be high priority to make sure the characters don't
            // really get to the screen.

            int priority = Thread.currentThread().getPriority();
            Thread.currentThread().setPriority( Thread.MAX_PRIORITY );

            try
            {
                stop = false;
                while ( !stop )
                {
                    // print a backspace + * to overwrite anything they type
                    System.out.print( "\010*" );
                    try
                    {
                        // attempt masking at this rate
                        Thread.sleep( 1 );
                    }
                    catch ( InterruptedException iex )
                    {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            finally
            {
                // restore the original priority
                Thread.currentThread().setPriority( priority );
            }
        }

        /**
         * Instruct the thread to stop masking.
         */
        public void stopMasking()
        {
            this.stop = true;
        }
    }
}
