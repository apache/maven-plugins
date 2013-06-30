package org.apache.maven.plugin.war.util;

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

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Set of file's paths.
 * <p/>
 * The class extends functionality of a "normal" set of strings by a process of
 * the paths normalization. All paths are converted to unix form (slashes) and
 * they don't start with starting /.
 *
 * @author Piotr Tabor
 * @version $Id$
 */

public class PathSet implements Iterable<String>
{

    /**
     * Set of normalized paths
     */
    private Set<String> pathsSet = new LinkedHashSet<String>();

    /**
     * The method normalizes the path.
     * <p/>
     * <ul>
     * <li>changes directory separator to unix's separator(/)</li>
     * <li>deletes all trailing slashes</li>
     * </ul>
     *
     * @param path to normalization
     * @return normalized path
     */
    protected String normalizeFilePath( String path )
    {
        return normalizeFilePathStatic( path );
    }

    /*-------------------- Business interface ------------------------------*/

    /**
     * Creates an empty paths set
     */
    public PathSet()
    {
        /*Empty default constructor*/
    }

    /**
     * Creates paths set and normalizate and adds all 'paths'.
     * The source 'paths' will not be changed
     *
     * @param paths to be added
     */
    public PathSet( Collection<String> paths )
    {
        addAll( paths );
    }

    /**
     * Creates paths set and normalizate and adds all 'paths'.
     * The source 'paths' will not be changed
     *
     * @param paths to be added
     */
    public PathSet( String[] paths )
    {
        addAll( paths );
    }

    /**
     * Normalizes and adds given path to the set.
     *
     * @param path to be added
     */
    public void add( String path )
    {
        pathsSet.add( normalizeFilePath( path ) );
    }

    /**
     * Normalizes and adds given paths (collection of strings)
     * to the set. The source collection will not be changed
     *
     * @param paths  - collection of strings to be added
     * @param prefix added to all given paths
     */
    public void addAll( Collection<String> paths, String prefix )
    {
        for ( String val : paths )
        {
            add( prefix + val );
        }
    }

    /**
     * Normalizes and adds given paths to the set.
     * The source collection will not be changed
     *
     * @param paths  to be added
     * @param prefix added to all given paths
     */
    public void addAll( String[] paths, String prefix )
    {
        for ( String val : paths )
        {
            add( prefix + val );
        }
    }

    /**
     * Adds given paths to the set.
     * The source collection will not be changed
     *
     * @param paths  to be added
     * @param prefix added to all given paths
     */
    public void addAll( PathSet paths, String prefix )
    {
        for ( String path : paths )
        {
            add( prefix + path );
        }
    }

    /**
     * Normalizes and adds given paths (collection of strings)
     * to the set. The source collection will not be changed
     *
     * @param paths - collection of strings to be added
     */
    public void addAll( Collection<String> paths )
    {
        addAll( paths, "" );
    }

    /**
     * Normalizes and adds given paths to the set.
     * The source collection will not be changed
     *
     * @param paths to be added
     */
    public void addAll( String[] paths )
    {
        addAll( paths, "" );
    }

    /**
     * Adds given paths to the set.
     * The source collection will not be changed
     *
     * @param paths to be added
     */
    public void addAll( PathSet paths )
    {
        addAll( paths, "" );
    }

    /**
     * Checks if the set constains given path. The path is normalized
     * before check.
     *
     * @param path we are looking for in the set.
     * @return information if the set constains the path.
     */
    public boolean contains( String path )
    {
        return pathsSet.contains( normalizeFilePath( path ) );
    }

    /**
     * Removes the specified path if it exists.
     *
     * @param path the path to remove
     * @return true if the path was removed, false if it did not existed
     */
    boolean remove( String path )
    {
        final String normalizedPath = normalizeFilePath( path );
        return pathsSet.remove( normalizedPath );

    }

    /**
     * Returns iterator of normalized paths (strings)
     *
     * @return iterator of normalized paths (strings)
     */
    public Iterator<String> iterator()
    {
        return pathsSet.iterator();
    }

    public Collection<String> paths()
    {
        return pathsSet;
    }

    /**
     * Adds given prefix to all paths in the set.
     * <p/>
     * The prefix should be ended by '/'. The generated paths are normalized.
     *
     * @param prefix to be added to all items
     */
    public void addPrefix( String prefix )
    {
        final Set<String> newSet = new HashSet<String>();
        for ( String path : pathsSet )
        {
            newSet.add( normalizeFilePath( prefix + path ) );
        }
        pathsSet = newSet;
    }

    /**
     * Returns count of the paths in the set
     *
     * @return count of the paths in the set
     */
    public int size()
    {
        return pathsSet.size();
    }

    /**
     * Adds to the set all files in the given directory
     *
     * @param directory that will be searched for file's paths to add
     * @param prefix    to be added to all found files
     */
    public void addAllFilesInDirectory( File directory, String prefix )
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( directory );
        scanner.scan();
        addAll( scanner.getIncludedFiles(), prefix );
    }

    /*-------------------- Universal static mathods ------------------------*/
    /**
     * The method normalizes the path.
     * <p/>
     * <ul>
     * <li>changes directory separator to unix's separator(/)</li>
     * <li>deletes all trailing slashes</li>
     * </ul>
     *
     * @param path to normalization
     * @return normalized path
     */
    public static String normalizeFilePathStatic( String path )
    {
        return trimTrailingSlashes( StringUtils.replace( path, '\\', '/' ) );
    }

    /**
     * The method deletes all trailing slashes from the given string
     *
     * @param str a string
     * @return trimed string
     */
    public static String trimTrailingSlashes( String str )
    {
        int i;
        for ( i = 0; i < str.length() && str.charAt( i ) == '/'; i++ )
        {
            // just calculate i
        }
        return str.substring( i );
    }

}
