/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
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
import org.apache.maven.plugin.eclipse.Constants;
import org.apache.maven.plugin.eclipse.EclipseSourceDir;
import org.apache.maven.plugin.eclipse.Messages;
import org.apache.maven.plugin.eclipse.writers.wtp.AbstractWtpResourceWriter;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * Create or adapt the manifest files for the RAD6 runtime dependencys. attention these will not be used for the real
 * ear these are just to get the runtime enviorment using the maven dependencies. WARNING: The manifest resources added
 * here will not have the benefit of the dependencies of the project, since that's not provided in the setup() apis, one
 * of the locations from which this writer is used in the RadPlugin.
 * 
 * @author <a href="mailto:nir@cfc.at">Richard van Nieuwenhoven </a>
 */
public class EclipseManifestWriter
    extends AbstractEclipseWriter
{

    private static final String MANIFEST_MF_FILENAME = "MANIFEST.MF";

    private static final String META_INF_DIRECTORY = "META-INF";

    private static final String GENERATED_RESOURCE_DIRNAME =
        "target" + File.separatorChar + "generated-resources" + File.separatorChar + "eclipse";

    private static final String WEBAPP_RESOURCE_DIR =
        "src" + File.separatorChar + "main" + File.separatorChar + "webapp";

    /**
     * Search the project for the existing META-INF directory where the manifest should be located.
     * 
     * @return the apsolute path to the META-INF directory
     */
    public String getMetaInfBaseDirectory( MavenProject project )
    {
        String metaInfBaseDirectory = null;

        if ( this.config.getProject().getPackaging().equals( Constants.PROJECT_PACKAGING_WAR ) )
        {
            metaInfBaseDirectory =
                this.config.getProject().getBasedir().getAbsolutePath() + File.separatorChar +
                    EclipseManifestWriter.WEBAPP_RESOURCE_DIR;

            this.log.debug( "Attempting to use: " + metaInfBaseDirectory + " for location of META-INF in war project." );

            File metaInfDirectoryFile =
                new File( metaInfBaseDirectory + File.separatorChar + EclipseManifestWriter.META_INF_DIRECTORY );

            if ( metaInfDirectoryFile.exists() && !metaInfDirectoryFile.isDirectory() )
            {
                metaInfBaseDirectory = null;
            }
        }

        for ( int index = this.config.getSourceDirs().length - 1; metaInfBaseDirectory == null && index >= 0; index-- )
        {

            File manifestFile =
                new File( this.config.getEclipseProjectDirectory(), this.config.getSourceDirs()[index].getPath() +
                    File.separatorChar + EclipseManifestWriter.META_INF_DIRECTORY + File.separatorChar +
                    EclipseManifestWriter.MANIFEST_MF_FILENAME );

            this.log.debug( "Checking for existence of META-INF/MANIFEST.MF file: " + manifestFile );

            if ( manifestFile.exists() )
            {
                metaInfBaseDirectory = manifestFile.getParentFile().getParent();
            }
        }

        return metaInfBaseDirectory;
    }

    /**
     * Write the manifest files use an existing one it it exists (it will be overwritten!! in a war use webapp/META-INF
     * else use the generated rad6 sourcefolder
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
        // if
        // (this.config.getEclipseProjectName().equals(IdeUtils.getProjectName(IdeUtils.PROJECT_NAME_WITH_VERSION_TEMPLATE,
        // this.config.getProject()))) {
        // MavenArchiver mavenArchiver = new MavenArchiver();
        // ManifestConfiguration configuration = new ManifestConfiguration() {
        //
        // public boolean isAddClasspath() {
        // return true;
        // }
        // };
        //
        // File manifestFile = new File(metaInfBaseDirectory + File.separatorChar +
        // EclipseManifestWriter.META_INF_DIRECTORY + File.separatorChar + EclipseManifestWriter.MANIFEST_MF_FILENAME);
        // manifestFile.getParentFile().mkdirs();
        //
        // try {
        // PrintWriter printwriter = new PrintWriter(manifestFile);
        // mavenArchiver.getManifest(this.config.getProject(), configuration).write(printwriter);
        // printwriter.close();
        // } catch (Exception e) {
        // this.log.error(Messages.getString("EclipsePlugin.cantwritetofile", new Object[]{
        // metaInfBaseDirectory + File.separatorChar + EclipseManifestWriter.MANIFEST_MF_FILENAME
        // }));
        // }
        // } else {
        Manifest manifest = createNewManifest();

        File manifestFile =
            new File( metaInfBaseDirectory + File.separatorChar + EclipseManifestWriter.META_INF_DIRECTORY +
                File.separatorChar + EclipseManifestWriter.MANIFEST_MF_FILENAME );

        System.out.println( "MANIFEST LOCATION: " + manifestFile );

        if ( shouldNewManifestFileBeWritten( manifest, manifestFile ) )
        {
            System.out.println( "Writing manifest..." );

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
                                                    new Object[] { metaInfBaseDirectory + File.separatorChar +
                                                        EclipseManifestWriter.MANIFEST_MF_FILENAME } ) );
            }

            // }
        }
    }

    /**
     * make room for a Manifest file. use a generated resource for JARS and for WARS use the manifest in the
     * webapp/META-INF directory.
     * 
     * @throws MojoExecutionException
     */
    public static void addManifestResource( Log log, EclipseWriterConfig config )
        throws MojoExecutionException
    {

        EclipseManifestWriter manifestWriter = new EclipseManifestWriter();
        manifestWriter.init( log, config );

        String packaging = config.getProject().getPackaging();

        String manifestDirectory = manifestWriter.getMetaInfBaseDirectory( config.getProject() );

        if ( !Constants.PROJECT_PACKAGING_EAR.equals( packaging ) &&
            !Constants.PROJECT_PACKAGING_WAR.equals( packaging ) && manifestDirectory == null )
        {

            String generatedResourceDir =
                config.getProject().getBasedir().getAbsolutePath() + File.separatorChar +
                    EclipseManifestWriter.GENERATED_RESOURCE_DIRNAME;

            manifestDirectory = generatedResourceDir + File.separatorChar + "META-INF";

            try
            {
                new File( manifestDirectory ).mkdirs();
                File manifestFile = new File( manifestDirectory + File.separatorChar + "MANIFEST.MF" );
                if ( manifestFile.exists() )
                {
                    manifestFile.delete();
                }
                manifestFile.createNewFile();
            }
            catch ( IOException e )
            {
                log.error( Messages.getString( "EclipsePlugin.cantwritetofile", new Object[] { manifestDirectory +
                    File.separatorChar + "META-INF" + File.separatorChar + "MANIFEST.MF" } ) );
            }

            log.debug( "Adding " + EclipseManifestWriter.GENERATED_RESOURCE_DIRNAME + " to eclipse sources " );

            EclipseSourceDir[] sourceDirs = config.getSourceDirs();
            EclipseSourceDir[] newSourceDirs = new EclipseSourceDir[sourceDirs.length + 1];
            System.arraycopy( sourceDirs, 0, newSourceDirs, 0, sourceDirs.length );
            newSourceDirs[sourceDirs.length] =
                new EclipseSourceDir( EclipseManifestWriter.GENERATED_RESOURCE_DIRNAME, null, true, false, null, null,
                                      false );
            config.setSourceDirs( newSourceDirs );
        }

        if ( Constants.PROJECT_PACKAGING_WAR.equals( packaging ) )
        {
            new File( config.getProject().getBasedir().getAbsolutePath() + File.separatorChar + "src" +
                File.separatorChar + "main" + File.separatorChar + "webapp" + File.separatorChar + "META-INF" ).mkdirs();
        }

        // special case must be done first because it can add stuff to the
        // classpath that will be
        // written by the superclass
        manifestWriter.write();
    }

    /**
     * Add one dependency to the black separated classpath stringbuffer. When the project is available in the reactor
     * (current build) then the project is used else the jar representing the artifact. System dependencies will only be
     * included if they are in this project.
     * 
     * @param classpath existing classpath to append
     * @param dependency dependency to append as jar or as project
     */
    private void addDependencyToClassPath( StringBuffer classpath, IdeDependency dependency )
    {
        if ( !dependency.isTestDependency() && !dependency.isProvided() &&
            !dependency.isSystemScopedOutsideProject( this.config.getProject() ) )
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
    private boolean areManifestsEqual( Manifest manifest, Manifest existingManifest )
    {
        if ( existingManifest == null )
        {
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
            // special case classpath... they are qual when there entries
            // are equal
            if ( Attributes.Name.CLASS_PATH.equals( key ) )
            {
                newValue = orderClasspath( newValue );
                existingValue = orderClasspath( existingValue );
            }
            if ( ( newValue == null || !newValue.equals( existingValue ) ) &&
                ( existingValue == null || !existingValue.equals( newValue ) ) )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Convert all dependencies in a blank seperated list of jars and projects representing the classpath.
     * 
     * @return the blank separeted classpath string
     */
    private String constructManifestClasspath()
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
    private Manifest createNewManifest()
    {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put( Attributes.Name.MANIFEST_VERSION, "1.0" );
        manifest.getMainAttributes().put( Attributes.Name.CLASS_PATH, constructManifestClasspath() );
        return manifest;
    }

    /**
     * Aphabeticaly sort the classpath. Do this by splitting it up, sort the entries and gleue them together again.
     * 
     * @param newValue classpath to sort
     * @return the sorted classpath
     */
    private String orderClasspath( String newValue )
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
    private Manifest readExistingManifest( File manifestFile )
        throws IOException
    {
        if ( !manifestFile.exists() )
        {
            return null;
        }

        Manifest existingManifest = new Manifest();
        FileInputStream inputStream = new FileInputStream( manifestFile );
        existingManifest.read( inputStream );
        inputStream.close();
        return existingManifest;
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
    private boolean shouldNewManifestFileBeWritten( Manifest manifest, File manifestFile )
        throws MojoExecutionException
    {
        try
        {
            Manifest existingManifest = readExistingManifest( manifestFile );
            if ( areManifestsEqual( manifest, existingManifest ) )
            {
                this.log.info( Messages.getString( "EclipseCleanMojo.unchanged", manifestFile.getAbsolutePath() ) );
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
}
