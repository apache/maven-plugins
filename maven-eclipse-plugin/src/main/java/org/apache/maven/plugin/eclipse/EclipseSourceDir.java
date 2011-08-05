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
package org.apache.maven.plugin.eclipse;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.ide.IdeUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * Represent an eclipse source dir. Eclipse has no "main", "test" or "resource" concepts, so two source dirs with the
 * same path are equal.
 * <p>
 * source directories should always have a null output value.
 * 
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipseSourceDir
    implements Comparable
{
    private static final String PATTERN_SEPARATOR = "|";

    private String path;

    /**
     * source directories should always have a null output value.
     */
    private String output;

    /**
     * List<String>
     */
    private List include;

    /**
     * List<String>
     */
    private List exclude;

    private boolean isResource;

    private boolean test;

    private boolean filtering;

    /**
     * @param path the eclipse source directory
     * @param output path output directory
     * @param isResource true if the directory only contains resources, false if a compilation directory
     * @param test true if is a test directory, false otherwise
     * @param include a string in the eclipse pattern format for the include filter
     * @param exclude a string in the eclipse pattern format for the exclude filter
     * @param filtering true if filtering should be applied, false otherwise. Note: Filtering will only be applied if
     *            this become a "special directory" by being nested within the default output directory.
     */
    public EclipseSourceDir( String path, String output, boolean isResource, boolean test, List include, List exclude,
                             boolean filtering )
    {
        setPath( path );
        this.output = output;
        this.isResource = isResource;
        this.test = test;
        setInclude( include );
        setExclude( exclude );
        this.filtering = filtering;
    }

    /**
     * Getter for <code>exclude</code>.
     * 
     * @return Returns the exclude. Never null.
     */
    public List getExclude()
    {
        return this.exclude;
    }

    /**
     * Setter for <code>exclude</code>.
     * 
     * @param exclude The exclude to set.
     */
    public void setExclude( List exclude )
    {
        this.exclude = new ArrayList();
        if ( exclude != null )
        {
            this.exclude.addAll( exclude );
        }
    }

    /**
     * Getter for <code>include</code>.
     * 
     * @return Returns the include. Never null.
     */
    public List getInclude()
    {
        return this.include;
    }

    /**
     * Setter for <code>include</code>.
     * 
     * @param include The include to set.
     */
    public void setInclude( List include )
    {
        this.include = new ArrayList();
        if ( include != null )
        {
            this.include.addAll( include );
        }
    }

    /**
     * @return Returns the exclude as a string pattern suitable for eclipse
     */
    public String getExcludeAsString()
    {
        return StringUtils.join( exclude.iterator(), PATTERN_SEPARATOR );
    }

    /**
     * @return Returns the include as a string pattern suitable for eclipse
     */
    public String getIncludeAsString()
    {
        return StringUtils.join( include.iterator(), PATTERN_SEPARATOR );
    }

    /**
     * Getter for <code>output</code>.
     * <p>
     * source directories should always have a null output value.
     * 
     * @return Returns the output.
     */
    public String getOutput()
    {
        return this.output;
    }

    /**
     * Setter for <code>output</code>.
     * 
     * @param output The output to set.
     */
    public void setOutput( String output )
    {
        this.output = output;
    }

    /**
     * Getter for <code>path</code>.
     * 
     * @return Returns the path.
     */
    public String getPath()
    {
        return this.path;
    }

    /**
     * Setter for <code>path</code>. Converts \\ to / in path.
     * 
     * @param path The path to set.
     */
    public void setPath( String path )
    {
        this.path = IdeUtils.fixSeparator( path );
    }

    /**
     * Getter for <code>test</code>.
     * 
     * @return Returns the test.
     */
    public boolean isTest()
    {
        return this.test;
    }

    /**
     * Setter for <code>test</code>.
     * 
     * @param test The test to set.
     */
    public void setTest( boolean test )
    {
        this.test = test;
    }

    /**
     * Getter for <code>isResource</code>.
     * 
     * @return Returns the isResource.
     */
    public boolean isResource()
    {
        return this.isResource;
    }

    /**
     * Wheter this resource should be copied with filtering.
     */
    public boolean isFiltering()
    {
        return filtering;
    }

    /**
     * Wheter this resource should be copied with filtering.
     * @param filtering filter resources
     */
    public void setFiltering(boolean filtering)
    {
        this.filtering = filtering;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object obj )
    {
        return ( obj != null ) && ( obj instanceof EclipseSourceDir )
            && this.path.equals( ( (EclipseSourceDir) obj ).path );
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return this.path.hashCode();
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Object obj )
    {
        return this.path.compareTo( ( (EclipseSourceDir) obj ).path );
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( isResource ? "resource " : "source " );
        buffer.append( path );
        buffer.append( ": " );
        buffer.append( "output=" ).append( output ).append( ", " );
        buffer.append( "include=[" ).append( getIncludeAsString() ).append( "], " );
        buffer.append( "exclude=[" ).append( getExcludeAsString() ).append( "], " );
        buffer.append( "test=" ).append( test ).append( ", " );
        buffer.append( "filtering=" ).append( filtering );
        return buffer.toString();
    }

    /**
     * Merge with the provided directory.
     * <p>
     * If one directory is a source and the other is a resource directory then the result will be a source directory and
     * any includes or excludes will be removed since Eclipse has no "main", "test" or "resource" concepts. The output
     * directory will be the source directories value.
     * <p>
     * If the two directories are the same resources type (i.e isResource is equal) then the result will be the same
     * resource type with the includes from each merged together (duplicates will be removed), similarly for the
     * excludes. No effort is made to ensure that the includes and excludes are disjointed sets. Please fix your pom
     * instead.
     * <p>
     * No support for cases where the test, or filtering values are not identical.
     * 
     * @param mergeWith the directory to merge with
     * @throws MojoExecutionException test or filtering values are not identical, or isResource true and output are not
     *             identical
     */
    public boolean merge( EclipseSourceDir mergeWith )
        throws MojoExecutionException
    {

        if ( isResource != mergeWith.isResource )
        {
            if ( isResource )
            {
                // the output directory is set to the source directory's value
                output = mergeWith.output;
            }
            isResource = false;
            setInclude( null );
            setExclude( null );

        }
        else
        {

            LinkedHashSet includesAsSet = new LinkedHashSet();

            // if the orginal or merged dir have an empty "include" this means all is included,
            // so merge includes only if both are not empty
            if (!include.isEmpty() && !mergeWith.include.isEmpty())
            {
                includesAsSet.addAll(include);
                includesAsSet.addAll(mergeWith.include);
            }

            include = new ArrayList( includesAsSet );

            LinkedHashSet excludesAsSet = new LinkedHashSet();
            excludesAsSet.addAll( exclude );
            excludesAsSet.addAll( mergeWith.exclude );
            exclude = new ArrayList( excludesAsSet );
        }

        if (!StringUtils.equals(output, mergeWith.output))
        {
            // Request to merge when 'output' is not identical
            return false;
        }

        if (test != mergeWith.test)
        {
            // Request to merge when 'test' is not identical
            return false;
        }

        if (filtering != mergeWith.filtering)
        {
            // Request to merge when 'filtering' is not identical
            return false;
        }
        return true;
    }
}
