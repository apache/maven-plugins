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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.eclipse.writers.wtp.AbstractWtpResourceWriter;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;

/**
 * Common behaviours for creating or adapting the manifest files for eclipse environments.
 * 
 * @author <a href="mailto:nir@cfc.at">Richard van Nieuwenhoven </a>
 */
public abstract class AbstractEclipseManifestWriter
    extends AbstractEclipseWriter
{

    protected static final String MANIFEST_MF_FILENAME = "MANIFEST.MF";

    protected static final String META_INF_DIRECTORY = "META-INF";

    public AbstractEclipseManifestWriter()
    {
        super();
    }

    /**
     * Aphabeticaly sort the classpath. Do this by splitting it up, sort the entries and gleue them together again.
     * 
     * @param newValue classpath to sort
     * @return the sorted classpath
     */
    protected String orderClasspath( String newValue )
    {
        if ( newValue == null )
        {
            return null;
        }
        String[] entries = newValue.split( " " );
        Arrays.sort( entries );
        StringBuffer buffer = new StringBuffer( newValue.length() );
        for ( int index = 0; index < entries.length; index++ )
        {
            buffer.append( entries[index] );
            buffer.append( ' ' );
        }
        return buffer.toString();
    }

    /**
     * Read and parse the existing manifest file.
     * 
     * @param manifestFile file
     * @return the read manifest
     * @throws IOException if the file could not be read
     */
    protected Manifest readExistingManifest( File manifestFile )
        throws IOException
    {
        if ( !manifestFile.exists() )
        {
            return null;
        }

        Manifest existingManifest = new Manifest();
        FileInputStream inputStream = new FileInputStream( manifestFile );
        try
        {
            existingManifest.read( inputStream );
        }
        finally
        {
            IOUtil.close( inputStream );
        }
        return existingManifest;
    }

    /**
     * Add one dependency to the blank separated classpath stringbuffer. When the project is available in the reactor
     * (current build) then the project is used else the jar representing the artifact. System dependencies will only be
     * included if they are in this project.
     * 
     * @param classpath existing classpath to append
     * @param dependency dependency to append as jar or as project
     */
    protected void addDependencyToClassPath( StringBuffer classpath, IdeDependency dependency )
    {
        if ( !dependency.isTestDependency() && !dependency.isProvided()
            && !dependency.isSystemScopedOutsideProject( this.config.getProject() ) )
        {

            // blank is the separator in manifest classpath's
            if ( classpath.length() != 0 )
            {
                classpath.append( ' ' );
            }
            // if the dependency is a workspace project add the project and not
            // the jar
            if ( !dependency.isReferencedProject() )
            {
                classpath.append( dependency.getFile().getName() );
            }
            else
            {
                classpath.append( dependency.getEclipseProjectName() + ".jar" );
            }
        }
    }

    /**
     * Check if the two manifests are equal. Manifest.equal can not be used because of the special case the Classpath
     * entr, witch must be comaired sorted so that a different oder in the classpath does not result in "not equal".
     * This not not realy correct but in this case it is more important to reduce the number of version-controll files.
     * 
     * @param manifest the new manifest
     * @param existingManifest to compaire the new one with
     * @return are the manifests equal
     */
    protected boolean areManifestsEqual( Manifest manifest, Manifest existingManifest )
    {
        if ( existingManifest == null )
        {
            log.info( "@@@ FALSE - Manifest are not equal because existingManifest is null" );
            return false;
        }

        Set keys = new HashSet();
        Attributes existingMap = existingManifest.getMainAttributes();
        Attributes newMap = manifest.getMainAttributes();
        keys.addAll( existingMap.keySet() );
        keys.addAll( newMap.keySet() );
        Iterator iterator = keys.iterator();
        while ( iterator.hasNext() )
        {
            Attributes.Name key = (Attributes.Name) iterator.next();
            String newValue = (String) newMap.get( key );
            String existingValue = (String) existingMap.get( key );
            // special case classpath... they are equal when there entries
            // are equal
            if ( Attributes.Name.CLASS_PATH.equals( key ) )
            {
                newValue = orderClasspath( newValue );
                existingValue = orderClasspath( existingValue );
            }
            if ( ( newValue == null || !newValue.equals( existingValue ) )
                && ( existingValue == null || !existingValue.equals( newValue ) ) )
            {
                log.info( "@@@ FALSE - Manifest are not equal because key = " + key + " has existing value = "
                    + existingValue + " and new value = " + newValue + " are different" );
                return false;
            }
        }
        log.info( "@@@ TRUE - Manifests are equal" );
        return true;
    }

    /**
     * Convert all dependencies in a blank seperated list of jars and projects representing the classpath.
     * 
     * @return the blank separeted classpath string
     */
    protected String constructManifestClasspath()
    {
        StringBuffer stringBuffer = new StringBuffer();
        IdeDependency[] deps = this.config.getDepsOrdered();

        for ( int index = 0; index < deps.length; index++ )
        {
            addDependencyToClassPath( stringBuffer, deps[index] );
        }

        return stringBuffer.toString();
    }

    /**
     * Create a manifest contaigning the required classpath.
     * 
     * @return the newly created manifest
     */
    protected Manifest createNewManifest()
    {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put( Attributes.Name.MANIFEST_VERSION, "1.0" );
        manifest.getMainAttributes().put( Attributes.Name.CLASS_PATH, constructManifestClasspath() );
        return manifest;
    }

    /**
     * Verify is the manifest sould be overwritten this sould take in account that the manifest should only be written
     * if the contents of the classpath was changed not the order. The classpath sorting oder should be ignored.
     * 
     * @param manifest the newly created classpath
     * @param manifestFile the file where the manifest
     * @return if the new manifest file must be written
     * @throws MojoExecutionException
     */
    protected boolean shouldNewManifestFileBeWritten( Manifest manifest, File manifestFile )
        throws MojoExecutionException
    {
        try
        {
            Manifest existingManifest = readExistingManifest( manifestFile );
            if ( areManifestsEqual( manifest, existingManifest ) )
            {
                this.log.info( Messages.getString( "EclipsePlugin.unchangedmanifest", manifestFile.getAbsolutePath() ) );
                return false;
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( Messages.getString( "EclipseCleanMojo.nofilefound",
                                                                  manifestFile.getAbsolutePath() ), e );
        }
        return true;
    }

    /**
     * Search the project for the existing META-INF directory where the manifest should be located.
     * 
     * @return the absolute path to the META-INF directory
     * @throws MojoExecutionException
     */
    protected abstract String getMetaInfBaseDirectory( MavenProject project )
        throws MojoExecutionException;

    /**
     * If the existing manifest file located in <code>getMetaInfBaseDirectory()</code> already has a correct
     * MANIFEST_VERSION and CLASS_PATH value then do nothing.
     * <p>
     * Otherwise generate a <b>NEW</b> (i.e the old one is overwritten) which only contains values for MANIFEST_VERSION
     * and CLASS_PATH, all other previous entries are not kept.
     * 
     * @see AbstractWtpResourceWriter#write(EclipseSourceDir[], ArtifactRepository, File)
     * @param sourceDirs all eclipse source directorys
     * @param localRepository the local reposetory
     * @param buildOutputDirectory build output directory (target)
     * @throws MojoExecutionException when writing the config files was not possible
     */
    public void write()
        throws MojoExecutionException
    {
        String metaInfBaseDirectory = getMetaInfBaseDirectory( this.config.getProject() );

        if ( metaInfBaseDirectory == null )
        {
            // TODO: if this really is an error, shouldn't we stop the build??
            throw new MojoExecutionException(
                                              Messages.getString(
                                                                  "EclipseCleanMojo.nofilefound",
                                                                  new Object[] { EclipseManifestWriter.META_INF_DIRECTORY } ) );
        }
        File manifestFile =
            new File( metaInfBaseDirectory + File.separatorChar + EclipseManifestWriter.META_INF_DIRECTORY
                + File.separatorChar + EclipseManifestWriter.MANIFEST_MF_FILENAME );
        Manifest manifest = createNewManifest();

        if ( shouldNewManifestFileBeWritten( manifest, manifestFile ) )
        {
            log.info( "Writing manifest..." );

            manifestFile.getParentFile().mkdirs();

            try
            {
                FileOutputStream stream = new FileOutputStream( manifestFile );

                manifest.write( stream );

                stream.close();

            }
            catch ( Exception e )
            {
                this.log.error( Messages.getString( "EclipsePlugin.cantwritetofile",
                                                    new Object[] { manifestFile.getAbsolutePath() } ) );
            }
        }
    }

}