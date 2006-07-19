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
import org.apache.maven.plugin.ide.IdeUtils;
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

    /**
     * Constant used for newline.
     * @todo check if we should use system-dependent newlines or if eclipse prefers a common format
     */
    private static final String NEWLINE = "\n";

    /**
     * Bundle classpath: updated with the list of dependencies.
     */
    public final static String ENTRY_BUNDLE_CLASSPATH = "Bundle-ClassPath:";

    /**
     * Bundle name: updated with the project name.
     */
    public final static String ENTRY_BUNDLE_NAME = "Bundle-Name:";

    /**
     * Bundle symbolic name: updated with the artifact id.
     */
    public final static String ENTRY_BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName:";

    /**
     * Bundle version: updated with the project version.
     */
    public final static String ENTRY_BUNDLE_VERSION = "Bundle-Version:";

    /**
     * Bundle vendor: updated with the organization name (if set in the POM).
     */
    public final static String ENTRY_BUNDLE_VENDOR = "Bundle-Vendor:";

    public EclipseOSGiManifestWriter( Log log, File eclipseProjectDir, MavenProject project, IdeDependency[] deps )
    {
        super( log, eclipseProjectDir, project, deps );
    }

    public void write( File manifestFile, File libdir )
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

    protected StringBuffer rewriteManifest( File manifestFile, File libdir )
        throws MojoExecutionException
    {

        // warning: we read and rewrite the file line by line in order to preserve formatting
        boolean inBundleClasspathEntry = false;
        StringBuffer manifestSb = new StringBuffer();
        try
        {
            BufferedReader in = new BufferedReader( new FileReader( manifestFile ) );
            String line;
            while ( ( line = in.readLine() ) != null )
            {
                if ( inBundleClasspathEntry && line.indexOf( ":" ) > -1 )
                {
                    inBundleClasspathEntry = false;
                }
                else if ( inBundleClasspathEntry )
                {
                    // skip it
                    continue;
                }

                if ( line.startsWith( ENTRY_BUNDLE_CLASSPATH ) )
                {
                    inBundleClasspathEntry = true;
                }
                else if ( line.startsWith( ENTRY_BUNDLE_NAME ) )
                {
                    manifestSb.append( ENTRY_BUNDLE_NAME );
                    manifestSb.append( " " );
                    manifestSb.append( getProject().getName() );
                    manifestSb.append( NEWLINE );
                }
                else if ( line.startsWith( ENTRY_BUNDLE_SYMBOLICNAME ) )
                {
                    manifestSb.append( ENTRY_BUNDLE_SYMBOLICNAME );
                    manifestSb.append( " " );
                    manifestSb.append( getProject().getArtifactId() );
                    manifestSb.append( ";singleton:=true" );
                    manifestSb.append( NEWLINE );
                }
                else if ( line.startsWith( ENTRY_BUNDLE_VERSION ) )
                {
                    manifestSb.append( ENTRY_BUNDLE_VERSION );
                    manifestSb.append( " " );
                    manifestSb.append( getProject().getVersion() );
                    manifestSb.append( NEWLINE );
                }
                else if ( line.startsWith( ENTRY_BUNDLE_VENDOR ) && getProject().getOrganization() != null )
                {
                    manifestSb.append( ENTRY_BUNDLE_VENDOR );
                    manifestSb.append( " " );
                    manifestSb.append( getProject().getOrganization().getName() );
                    manifestSb.append( NEWLINE );
                }
                else
                {
                    manifestSb.append( line + NEWLINE );
                }
            }

            IOUtil.close( in );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( Messages.getString( "cantreadfile", manifestFile.getAbsolutePath() ) );
        }
        manifestSb.append( addBundleClasspathEntries( libdir ) );

        // OSGi manifest headers need to end with a line break
        manifestSb.append( NEWLINE );
        return manifestSb;
    }

    /**
     * Add all libraries that don't have the scope "provided" to the "Bundle-Classpath".
     * @throws MojoExecutionException 
     */
    protected String addBundleClasspathEntries( File libdir )
        throws MojoExecutionException
    {
        StringBuffer bundleClasspathSb = new StringBuffer( ENTRY_BUNDLE_CLASSPATH );

        // local classes, if the plugin is jarred
        // @todo handle expanded plugins
        bundleClasspathSb.append( " ." );

        for ( int j = 0; j < this.deps.length; j++ )
        {
            IdeDependency dep = this.deps[j];
            if ( !dep.isProvided() && !dep.isReferencedProject() && !dep.isTestDependency() )
            {
                bundleClasspathSb.append( "," + NEWLINE );

                getLog().debug( "Adding artifact to manifest: " + dep.getArtifactId() );

                File artifactFile = new File( libdir, dep.getFile().getName() );

                bundleClasspathSb.append( " "
                    + IdeUtils.toRelativeAndFixSeparator( getEclipseProjectDirectory(), artifactFile, false ) );
            }
        }
        // only insert the name of the property if there are local libraries
        return bundleClasspathSb.toString();
    }

}