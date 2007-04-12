package org.apache.maven.plugin.war;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

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

/**
 * Set of file's paths.
 * <p/>
 * The class extends functionality of a "normal" set of strings by a process of
 * the paths normalization. All paths are converted to unix form (slashes) and
 * they don't start with starting /.
 *
 * @author Piotr Tabor
 */

public class PathsSet
{

    /**
     * Set of normalized paths
     */
    private Set/* <String> */pathsSet = new LinkedHashSet();

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
    public PathsSet()
    {
        /*Empty default constructor*/
    }

    /**
     * Creates paths set and normalizate and adds all 'paths'.
     * The source 'paths' will not be changed
     *
     * @param paths to be added
     */
    public PathsSet( Collection/*String>*/ paths )
    {
        addAll( paths );
    }

    ;

    /**
     * Creates paths set and normalizate and adds all 'paths'.
     * The source 'paths' will not be changed
     *
     * @param paths to be added
     */
    public PathsSet( String[] paths )
    {
        addAll( paths );
    }

    ;

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
    public void addAll( Collection/*<String>*/ paths, String prefix )
    {
        for ( Iterator iter = paths.iterator(); iter.hasNext(); )
        {
            add( prefix + (String) iter.next() );
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
        for ( int i = 0; i < paths.length; i++ )
        {
            add( prefix + paths[i] );
        }
    }

    /**
     * Adds given paths to the set.
     * The source collection will not be changed
     *
     * @param paths  to be added
     * @param prefix added to all given paths
     */
    public void addAll( PathsSet paths, String prefix )
    {
        for ( Iterator iter = paths.iterator(); iter.hasNext(); )
        {
            add( prefix + (String) iter.next() );
        }
    }

    /**
     * Normalizes and adds given paths (collection of strings)
     * to the set. The source collection will not be changed
     *
     * @param paths - collection of strings to be added
     */
    public void addAll( Collection/*<String>*/ paths )
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
    public void addAll( PathsSet paths )
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
     * Returns iterator of normalized paths (strings)
     *
     * @return iterator of normalized paths (strings)
     */
    public Iterator iterator()
    {
        return pathsSet.iterator();
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
        final Set/*<String>*/ newSet = new HashSet();
        for ( Iterator iter = pathsSet.iterator(); iter.hasNext(); )
        {
            String path = (String) iter.next();
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
     * @param path
     * @return trimed string
     */
	public static String trimTrailingSlashes(String str) {
		int i;
        for ( i = 0; i < str.length() && str.charAt( i ) == '/'; i++ )
            /* just calculate i */
        {
            ;
        }
        return str.substring(i);
	}

}
