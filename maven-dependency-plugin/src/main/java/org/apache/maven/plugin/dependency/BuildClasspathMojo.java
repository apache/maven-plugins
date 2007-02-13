package org.apache.maven.plugin.dependency;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.dependency.utils.filters.ArtifactsFilter;

/**
 * This goal will output a classpath string of dependencies from the local
 * repository to a file or log.
 * 
 * @goal build-classpath
 * @requiresDependencyResolution compile
 * @phase generate-sources
 * @author ankostis
 * @since 2.0-alpha-2
 */
public class BuildClasspathMojo
    extends AbstractDependencyFilterMojo
    implements Comparator
{

    /**
     * Strip artifact version during copy (only works if prefix is set)
     * 
     * @parameter expression="${stripVersion}" default-value="false"
     * @parameter
     */
    private boolean stripVersion = false;

    /**
     * The prefix to preppend on each dependent artifact. If undefined, the
     * paths refer to the actual files store in the local repository (the
     * stipVersion parameter does nothing then).
     * 
     * @parameter expression="${maven.dep.prefix}"
     */
    private String prefix;

    /**
     * The file to write the classpath string. If undefined, it just prints the
     * classpath as [INFO].
     * 
     * @parameter expression="${maven.dep.cpFile}"
     */
    private File cpFile;

    /**
     * If 'true', it skips the up-to-date-check, and always regenerates the
     * classpath file.
     * 
     * @parameter default-value="false" expression="${maven.dep.regenerateFile}"
     */
    private boolean regenerateFile;

    /**
     * Main entry into mojo. Gets the list of dependencies and iterates through
     * calling copyArtifact.
     * 
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     * 
     * @see #getDependencies
     * @see #copyArtifact(Artifact, boolean)
     */
    public void execute()
        throws MojoExecutionException
    {
        Set artifacts = getResolvedDependencies( true );

        if ( artifacts == null || artifacts.isEmpty() )
        {
            getLog().info( "No dependencies found." );
        }

        List artList = new ArrayList( artifacts );

        StringBuffer sb = new StringBuffer();
        Iterator i = artList.iterator();

        if ( i.hasNext() )
        {
            appendArtifactPath( (Artifact) i.next(), sb );

            while ( i.hasNext() )
            {
                sb.append( ":" );
                appendArtifactPath( (Artifact) i.next(), sb );
            }
        }

        String cpString = sb.toString();

        if ( cpFile == null )
        {
            getLog().info( "Dependencies classpath:\n" + cpString );
        }
        else
        {
            if ( regenerateFile || !isUpdToDate( cpString ) )
            {
                storeClasspathFile( cpString );
            }
            else
            {
                this.getLog().info( "Skipped writting classpath file '" + cpFile + "'.  No changes found." );
            }
        }
    }

    /**
     * Appends the artifact path into the specified stringBuffer.
     * 
     * @param art
     * @param sb
     */
    protected void appendArtifactPath( Artifact art, StringBuffer sb )
    {
        if ( prefix == null )
        {
            sb.append( art.getFile() );
        }
        else
        {
            // TODO: add param for prepending groupId and version.
            sb.append( prefix );
            sb.append( File.separatorChar );
            sb.append( DependencyUtil.getFormattedFileName( art, this.stripVersion ) );
        }
    }

    /**
     * Checks that new classpath differs from that found inside the old
     * classpathFile.
     * 
     * @param cpString
     * @return true if the specified classpath equals to that found inside the
     *         file, false otherwise (including when file does not exists but
     *         new classpath does).
     */
    private boolean isUpdToDate( String cpString )
    {
        try
        {
            String oldCp = readClasspathFile();
            return ( cpString == oldCp || ( cpString != null && cpString.equals( oldCp ) ) );
        }
        catch ( Exception ex )
        {
            this.getLog().warn( "Error while reading old classpath file '" + cpFile + "' for up-to-date check: " + ex );

            return false;
        }
    }

    /**
     * It stores the specified string into that file.
     * 
     * @param cpString
     *            the string to be written into the file.
     * @throws MojoExecutionException
     */
    private void storeClasspathFile( String cpString )
        throws MojoExecutionException
    {
        try
        {
            Writer w = new BufferedWriter( new FileWriter( cpFile ) );

            try
            {
                w.write( cpString );

                getLog().info( "Written classpath file '" + cpFile + "'." );
            }
            catch ( IOException ex )
            {
                throw new MojoExecutionException( "Error while writting to classpath file '" + cpFile + "': "
                    + ex.toString(), ex );
            }
            finally
            {
                w.close();
            }
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( "Error while opening/closing classpath file '" + cpFile + "': "
                + ex.toString(), ex );
        }
    }

    /**
     * Reads into a string the file specified by the mojo param 'cpFile'.
     * Assumes, the instance variable 'cpFile' is not null.
     * 
     * @return the string contained in the classpathFile, if exists, or null
     *         ortherwise.
     * @throws MojoExecutionException
     */
    private String readClasspathFile()
        throws IOException
    {
        if ( !cpFile.isFile() )
        {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        BufferedReader r = new BufferedReader( new FileReader( cpFile ) );

        try
        {
            String l;
            while ( ( l = r.readLine() ) != null )
            {
                sb.append( l );
            }

            return sb.toString();
        }
        finally
        {
            r.close();
        }
    }

    /**
     * Compares artifacts lexicographically, using pattern
     * [group_id][artifact_id][version].
     * 
     * @param arg1
     *            first object
     * @param arg2
     *            second object
     * @return the value <code>0</code> if the argument string is equal to
     *         this string; a value less than <code>0</code> if this string is
     *         lexicographically less than the string argument; and a value
     *         greater than <code>0</code> if this string is lexicographically
     *         greater than the string argument.
     */
    public int compare( Object arg1, Object arg2 )
    {
        if ( arg1 instanceof Artifact && arg2 instanceof Artifact )
        {
            if ( arg1 == arg2 )
            {
                return 0;
            }
            else if ( arg1 == null )
            {
                return -1;
            }
            else if ( arg2 == null )
            {
                return +1;
            }

            Artifact art1 = (Artifact) arg1;
            Artifact art2 = (Artifact) arg2;

            String s1 = art1.getGroupId() + art1.getArtifactId() + art1.getVersion();
            String s2 = art2.getGroupId() + art2.getArtifactId() + art2.getVersion();

            return s1.compareTo( s2 );
        }
        else
        {
            return 0;
        }
    }

    protected ArtifactsFilter getMarkedArtifactFilter()
    {
        return null;
    }
}
