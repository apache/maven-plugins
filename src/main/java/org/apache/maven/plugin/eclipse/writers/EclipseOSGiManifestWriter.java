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
package org.apache.maven.plugin.eclipse.writers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;

/**
 * The <code>EclipseOSGiManifestWriter</code> ensures that value of the "Bundle-Classpath" property 
 * in META-INF/MANIFEST.MF is synchronized with the POM by adding all dependencies that don't have the
 * scope provided.
 */
public class EclipseOSGiManifestWriter
    extends AbstractEclipseResourceWriter
{

    public final static String ENTRY_BUNDLE_CLASSPATH = "Bundle-ClassPath:";

    public EclipseOSGiManifestWriter( Log log, File eclipseProjectDir, MavenProject project, IdeDependency[] deps )
    {
        super( log, eclipseProjectDir, project, deps );
    }

    public void write( File manifestFile, String libdir )
        throws MojoExecutionException
    {
        // check for existence
        if ( !manifestFile.exists() )
        {
            getLog().warn(
                           Messages.getString( "EclipseOSGiManifestWriter.nomanifestfile", manifestFile
                               .getAbsolutePath() ) );
            return;
        }

        StringBuffer manifestSb = rewriteManifest( manifestFile, libdir );
        FileWriter fos = null;
        try
        {
            fos = new FileWriter( manifestFile );
            fos.write( manifestSb.toString() );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( Messages.getString( "cantwritetofile", manifestFile.getAbsolutePath() ) );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( Messages.getString( "cantwritetofile", manifestFile.getAbsolutePath() ),
                                              e );
        }
        finally
        {
            IOUtil.close( fos );
        }
    }

    protected StringBuffer rewriteManifest( File manifestFile, String libdir )
        throws MojoExecutionException
    {
        boolean inBundleClasspathEntry = false;
        StringBuffer manifestSb = new StringBuffer();
        try
        {
            BufferedReader in = new BufferedReader( new FileReader( manifestFile ) );
            String str;
            while ( ( str = in.readLine() ) != null )
            {
                if ( inBundleClasspathEntry && str.indexOf( ":" ) > -1 )
                {
                    inBundleClasspathEntry = false;
                    if ( str.length() > 0 )
                    {
                        manifestSb.append( str + "\n" );
                    }
                }
                else if ( str.indexOf( ENTRY_BUNDLE_CLASSPATH ) > -1 )
                {
                    inBundleClasspathEntry = true;
                }
                else if ( inBundleClasspathEntry )
                {
                    // skip it
                }
                else
                {
                    manifestSb.append( str + "\n" );
                }
            }
            in.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( Messages.getString( "cantreadfile", manifestFile.getAbsolutePath() ) );
        }
        manifestSb.append( addBundleClasspathEntries( libdir ) );
        // OSGi manifest headers need to end with a line break
        manifestSb.append( "\n" );
        return manifestSb;
    }

    /**
     * Add all libraries that don't have the scope "provided" to the "Bundle-Classpath".
     */
    protected String addBundleClasspathEntries( String libdir )
    {
        StringBuffer bundleClasspathSb = new StringBuffer( ENTRY_BUNDLE_CLASSPATH );
        int countAddedLibs = 0;
        for ( int i = 0; i < this.deps.length; i++ )
        {
            if ( !this.deps[i].isProvided() && !this.deps[i].isReferencedProject() )
            {
                if ( countAddedLibs != 0 )
                {
                    // TODO problems with line endings might appear
                    bundleClasspathSb.append( ",\n" );
                }
                System.out.println( "artifact: " + this.deps[i].getArtifactId() );
                bundleClasspathSb.append( " " + libdir + "/" + this.deps[i].getFile().getName() + "" );
                countAddedLibs++;
            }
        }
        // only insert the name of the property if there are local libraries
        if ( countAddedLibs > 0 )
        {
            return bundleClasspathSb.toString();
        }
        return "";
    }

}