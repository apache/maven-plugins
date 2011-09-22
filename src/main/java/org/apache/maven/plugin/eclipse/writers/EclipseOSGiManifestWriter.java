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
package org.apache.maven.plugin.eclipse.writers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.ide.IdeDependency;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * The <code>EclipseOSGiManifestWriter</code> ensures that value of the "Bundle-Classpath" property in
 * META-INF/MANIFEST.MF is synchronized with the POM by adding all dependencies that don't have the scope provided.
 * 
 * @deprecated use <a href="http://cwiki.apache.org/FELIX/bundle-plugin-for-maven-bnd.html/">Maven Bundle plugin</a>
 *             from Felix
 */
public class EclipseOSGiManifestWriter
    extends AbstractEclipseWriter
{

    /**
     * Constant used for newline.
     * 
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

    /**
     * @see org.apache.maven.plugin.eclipse.writers.EclipseWriter#write()
     */
    public void write()
        throws MojoExecutionException
    {
        // check for existence
        if ( !config.getOSGIManifestFile().exists() )
        {
            log.warn( Messages.getString( "EclipseOSGiManifestWriter.nomanifestfile",
                                          config.getOSGIManifestFile().getAbsolutePath() ) );
            return;
        }

        StringBuffer manifestSb = rewriteManifest( config.getOSGIManifestFile() );
        Writer out = null;
        try
        {
            out = new OutputStreamWriter( new FileOutputStream( config.getOSGIManifestFile() ), "UTF-8" );
            out.write( manifestSb.toString() );
        }
        catch ( FileNotFoundException e )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.cantwritetofile",
                                                                  config.getOSGIManifestFile().getAbsolutePath() ) );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.cantwritetofile",
                                                                  config.getOSGIManifestFile().getAbsolutePath() ), e );
        }
        finally
        {
            IOUtil.close( out );
        }
    }

    protected StringBuffer rewriteManifest( File manifestFile )
        throws MojoExecutionException
    {

        // warning: we read and rewrite the file line by line in order to preserve formatting
        boolean inBundleClasspathEntry = false;
        StringBuffer manifestSb = new StringBuffer();
        try
        {
            BufferedReader in =
                new BufferedReader( new InputStreamReader( new FileInputStream( manifestFile ), "UTF-8" ) );
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

                // Note that this could be the empty string, if we encounter
                // a field that we weren't expecting to be multi-line.
                String name = line.substring( 0, line.indexOf( ":" ) + 1 );

                if ( name.equalsIgnoreCase( ENTRY_BUNDLE_CLASSPATH ) )
                {
                    inBundleClasspathEntry = true;
                }
                else if ( name.equalsIgnoreCase( ENTRY_BUNDLE_NAME ) )
                {
                    manifestSb.append( ENTRY_BUNDLE_NAME );
                    manifestSb.append( " " );
                    manifestSb.append( config.getProject().getName() );
                    manifestSb.append( NEWLINE );
                }
                else if ( name.equalsIgnoreCase( ENTRY_BUNDLE_SYMBOLICNAME ) )
                {
                    manifestSb.append( ENTRY_BUNDLE_SYMBOLICNAME );
                    manifestSb.append( " " );
                    manifestSb.append( config.getEclipseProjectName() );
                    manifestSb.append( ";singleton:=true" );
                    manifestSb.append( NEWLINE );
                }
                else if ( name.equalsIgnoreCase( ENTRY_BUNDLE_VERSION ) )
                {
                    manifestSb.append( ENTRY_BUNDLE_VERSION );
                    manifestSb.append( " " );
                    manifestSb.append( getNormalizedVersion( config.getProject().getVersion() ) );
                    manifestSb.append( NEWLINE );
                }
                else if ( name.equalsIgnoreCase( ENTRY_BUNDLE_VENDOR ) && config.getProject().getOrganization() != null )
                {
                    manifestSb.append( ENTRY_BUNDLE_VENDOR );
                    manifestSb.append( " " );
                    manifestSb.append( config.getProject().getOrganization().getName() );
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
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.cantreadfile",
                                                                  manifestFile.getAbsolutePath() ) );
        }
        manifestSb.append( addBundleClasspathEntries() );

        // OSGi manifest headers need to end with a line break
        manifestSb.append( NEWLINE );
        return manifestSb;
    }

    /**
     * Normalize a version number, by moving snapshot identifier to the 5th token (first 4 tokens must be numeric for
     * OSGI bundles)
     * 
     * @param version original version
     * @return a normalized version number
     */
    protected static String getNormalizedVersion( String version )
    {

        if ( version.endsWith( "-SNAPSHOT" ) )
        {
            String[] versionTokens = StringUtils.split( StringUtils.stripEnd( version, "-SNAPSHOT" ), "." );

            int j = 0;
            StringBuffer newVersion = new StringBuffer( 20 );
            for ( ; j < versionTokens.length; j++ )
            {
                newVersion.append( versionTokens[j] );
                newVersion.append( "." );
            }
            for ( ; j < 3; j++ )
            {
                newVersion.append( "0." );
            }

            newVersion.append( "SNAPSHOT" );
            version = newVersion.toString();

        }
        return version;
    }

    /**
     * Add all libraries that don't have the scope "provided" to the "Bundle-Classpath".
     * 
     * @return complete "Bundle-ClassPath:" entry for manifest
     */
    protected String addBundleClasspathEntries()
    {
        StringBuffer bundleClasspathSb = new StringBuffer( ENTRY_BUNDLE_CLASSPATH );

        // local classes, if the plugin is jarred
        // @todo handle expanded plugins
        bundleClasspathSb.append( " ." );

        IdeDependency[] deps = config.getDeps();

        for ( int j = 0; j < deps.length; j++ )
        {
            IdeDependency dep = deps[j];
            if ( !dep.isProvided() && !dep.isReferencedProject() && !dep.isTestDependency() && !dep.isOsgiBundle() )
            {
                bundleClasspathSb.append( "," + NEWLINE );

                log.debug( "Adding artifact to manifest: " + dep.getArtifactId() );

                bundleClasspathSb.append( " " + dep.getFile().getName() );
            }
        }
        // only insert the name of the property if there are local libraries
        return bundleClasspathSb.toString();
    }

}