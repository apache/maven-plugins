package org.apache.maven.plugin.war;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * Allows to map custom artifact type to standard type.
 *
 * @author <a href="p.tabor@students.mimuw.edu.pl">Piotr Tabor</a>
 */
public class Overlay
{
    /**
     * Resolved artifact that the overlay uses
     */
    private Artifact artifact;

    /* Those are set by configuration */

    private String groupId;

    private String artifactId;

    private String classifier;

    /**
     * The comma separated list of tokens to include in the WAR.
     * Default is '**'.
     */
    private String includes = "**";

    /**
     * The comma separated list of tokens to exclude from the WAR.
     * Default is ''.
     */
    private String excludes = "";

    /* ============== Implementation ======================== */

    public Overlay()
    {
    }

    /**
     * The constructor creates overlay that uses given artifact.
     *
     * @param artifact that will be used by the overlay.
     */
    public Overlay( Artifact artifact )
    {
        this.artifact = artifact;
        this.artifactId = artifact.getArtifactId();
        this.groupId = artifact.getGroupId();
        this.classifier = artifact.getClassifier();
    }

    /**
     * Returns the {@link Artifact} representing this module.
     * <p/>
     * Note that this might return <tt>null</tt> till the
     * module has been resolved or the overlay is using local project artifact.
     *
     * @return the artifact
     * @see #resolveArtifact(java.util.Set)
     */
    public Artifact getArtifact()
    {
        return artifact;
    }

    /**
     * Resolves the {@link Artifact} represented by the module.
     *
     * @param artifacts the project's artifacts with type WAR
     */

    public void resolveArtifact( Collection warArtifacts )
        throws MojoFailureException
    {
        /* If the artifact is already set no need to resolve it */
        if ( ( artifact == null ) && ( !isCurrentProjectOverlay() ) )
        {

            /* Make sure that at least the groupId and the artifactId are
             specified */
            if ( groupId == null || artifactId == null )
            {
                throw new MojoFailureException( "Could not resolve artifact[" + groupId + ":" + artifactId + ":war]" );
            }

            for ( Iterator iter = warArtifacts.iterator(); iter.hasNext(); )
            {
                Artifact candidat = (Artifact) iter.next();
                if ( candidat.getArtifactId().equals( artifactId ) && candidat.getGroupId().equals( groupId ) &&
                    candidat.getClassifier().equals( classifier ) )
                {
                    artifact = candidat;
                    return;
                }
            }

            /* Artifact has not been found */
            if ( artifact == null )
            {
                throw new MojoFailureException(
                    "Overlay[" + this.toString() + "] " + "is not a dependency of the project." );
            }
        }
    }

    /**
     * Return true if the overlay uses local project as artifact.
     *
     * @return information if the overlay uses local project as artifact.
     */
    public boolean isCurrentProjectOverlay()
    {
        return ( groupId == null ) && ( artifactId == null );
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( "war" ).append( ":" ).append( groupId ).append( ":" ).append( artifactId );
        if ( classifier != null )
        {
            sb.append( ":" ).append( classifier );
        }
        if ( artifact != null )
        {
            sb.append( ":" ).append( artifact.getVersion() );
        }
        return sb.toString();
    }

    /**
     * Returns a string array of the excludes to be used
     * when copying the war.
     *
     * @return an array of tokens to include
     */
    public String[] getParsedExcludes()
    {
        return StringUtils.split( StringUtils.defaultString( excludes ), "," );
    }

    /**
     * Returns a string array of the includes to be used
     * when copying the war.
     *
     * @return an array of tokens to include
     */
    public String[] getParsedIncludes()
    {
        return StringUtils.split( StringUtils.defaultString( includes ), "," );
    }

    /**
     * Sets a string array of the includes to be used
     * when copying the war.
     *
     * @param parsedIncludes an array of tokens to include
     */
    public void setParsedIncludes( String[] parsedIncludes )
    {
        if ( parsedIncludes != null )
        {
            includes = StringUtils.join( parsedIncludes, "," );
        }
        else
        {
            includes = null;
        }
    }

    /**
     * Sets a string array of the excludes to be used
     * when copying the war.
     *
     * @param parsedExcludes an array of tokens to include
     */
    public void setParsedExcludes( String[] parsedExcludes )
    {
        if ( parsedExcludes != null )
        {
            excludes = StringUtils.join( parsedExcludes, "," );
        }
        else
        {
            excludes = null;
        }
    }

    /**
     * Unpacks war artifacts into a temporary directory inside <tt>workDirectory</tt>
     * named with the name of the war.
     * <p/>
     * The war is unpacked in subdirectory of given 'workDirectory' named by result
     * of method: getTempDirectory().
     *
     * @param archiverManager Archiver manager used to unpack.
     * @param workDirectory   directory containing the unpacked war directory
     * @return directory containing the unpacked war (subdirectory of 'workDirectory')
     * @throws MojoExecutionException
     */
    public File unpackWarToTempDirectory( ArchiverManager archiverManager, File workDirectory )
        throws MojoExecutionException, NoSuchArchiverException
    {

        File tempLocation = getOverlayTempDirectory( workDirectory );

        boolean process = false;
        if ( !tempLocation.exists() )
        {
            tempLocation.mkdirs();
            process = true;
        }
        else if ( artifact.getFile().lastModified() > tempLocation.lastModified() )
        {
            process = true;
        }

        if ( process )
        {
            File file = artifact.getFile();
            unpack( archiverManager, file, tempLocation );
        }

        return tempLocation;
    }

    ;

    /**
     * The method generates the directory name in which
     * the overlayed war will be/was unpacked.
     *
     * @param workDirectory parent directory for the generated dir
     * @return generated directory (notice that it might not exist)
     */
    public File getOverlayTempDirectory( File workDirectory )
    {
        String name = artifact.getFile().getName();
        return new File( workDirectory, name.substring( 0, name.length() - 4 ) );
    }

    /**
     * Unpacks the archive file.
     *
     * @param file     File to be unpacked.
     * @param location Location where to put the unpacked files.
     */
    private void unpack( ArchiverManager archiverManager, File file, File location )
        throws MojoExecutionException, NoSuchArchiverException
    {
        String archiveExt = FileUtils.getExtension( file.getAbsolutePath() )
            .toLowerCase();

        try
        {
            UnArchiver unArchiver = archiverManager.getUnArchiver( archiveExt );
            unArchiver.setSourceFile( file );
            unArchiver.setDestDirectory( location );
            unArchiver.setOverwrite( true );
            unArchiver.extract();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error unpacking file: " + file + "to: " + location, e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error unpacking file: " + file + "to: " + location, e );
        }
    }

    /**
     * The method creats set of important (included and not excluded) file's
     * paths in the overlays directory.
     * <p/>
     * The method can be only used after unpacking the overlayed war
     * into overlayTempDirectory (for example by calling: unpackWarToTempDirectory).
     * <p/>
     * For current project's overlay the given directory is scanned.
     *
     * @param workDirectory - parent of the overlay temp directory
     * @return Set of file's paths included in the overlay
     */
    public PathsSet getFilesInOverlay( File workDirectory )
    {

        DirectoryScanner scanner = new DirectoryScanner();
        if ( isCurrentProjectOverlay() )
        {
            scanner.setBasedir( workDirectory );
        }
        else
        {
            scanner.setBasedir( getOverlayTempDirectory( workDirectory ) );
        }
        scanner.setExcludes( getParsedExcludes() );
        scanner.setIncludes( getParsedIncludes() );
        scanner.scan();
        return new PathsSet( scanner.getIncludedFiles() );
    }

    /* ====================== Mojo interface ================= */

    /**
     * Returns the artifact's Id.
     *
     * @return the artifact Id
     */
    public String getArtifactId()
    {
        return artifactId;
    }

    /**
     * Returns the artifact's groupId.
     *
     * @return the group Id
     */
    public String getGroupId()
    {
        return groupId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    /**
     * Returns the artifact's classifier.
     *
     * @return the artifact classifier
     */
    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    /**
     * Returns the overlay includes pattern
     *
     * @return the overlay includes pattern
     */
    public String getIncludes()
    {
        return includes;
    }

    public void setIncludes( String includes )
    {
        this.includes = includes;
    }

    /**
     * Returns the overlay excludes pattern
     *
     * @return the overlay excludes pattern
     */
    public String getExcludes()
    {
        return excludes;
    }

    public void setExcludes( String excludes )
    {
        this.excludes = excludes;
    }

    /* ============================================================ */

    /**
     * The methods creates overlay connected with the local project's war.
     */
    public static Overlay createLocalProjectInstance()
    {
        final Overlay resultOverlay = new Overlay();
        return resultOverlay;
    }

}
