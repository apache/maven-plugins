package org.apache.maven.plugin.war.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Represents the structure of a web application composed of multiple
 * overlays. Each overlay is registered within this structure with the
 * set of files it holds.
 * <p/>
 * Note that this structure is persisted to disk at each invocation to
 * store wich owner holds which path (file).
 *
 * @author Stephane Nicoll
 */
public class WebappStructure
{

    private final Map registeredFiles;

    private final transient PathSet allFiles;

    /**
     * Creates a new instance.
     */
    public WebappStructure()
    {
        this.registeredFiles = new HashMap();
        this.allFiles = new PathSet();
    }

    /**
     * Specify if the specified <tt>path</tt> is registered or not.
     *
     * @param path the relative path from the webapp root directory
     * @return true if the path is registered, false otherwise
     */
    public boolean isRegistered( String path )
    {
        return getFullStructure().contains( path );

    }

    /**
     * Registers the specified path for the specified owner. Returns <tt>true</tt>
     * if the path is not already registered, <tt>false</tt> otherwise.
     *
     * @param id   the owner of the path
     * @param path the relative path from the webapp root directory
     * @return true if the file was registered successfully
     */
    public boolean registerFile( String id, String path )
    {
        if ( !isRegistered( path ) )
        {
            getFullStructure().add( path );
            getStructure( id ).add( path );
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Returns the owner of the specified <tt>path</tt>. If the file is not
     * registered, returns <tt>null</tt>
     *
     * @param path the relative path from the webapp root directory
     * @return the owner or <tt>null</tt>.
     */
    public String getOwner( String path )
    {
        if ( !isRegistered( path ) )
        {
            return null;
        }
        else
        {
            final Iterator it = registeredFiles.keySet().iterator();
            while ( it.hasNext() )
            {
                final String owner = (String) it.next();
                final PathSet structure = getStructure( owner );
                if ( structure.contains( path ) )
                {
                    return owner;
                }

            }
            throw new IllegalStateException(
                "Should not happen, path[" + path + "] is flagged as being registered but was not found." );
        }

    }

    /**
     * Returns the owners. Note that this the returned {@link Set} may be
     * inconsistent since it represents a persistent cache accross multiple
     * invocations.
     * <p/>
     * For instance, if an overlay was removed in this execution, it will be
     * still be there till the cache is cleaned. This happens when the clean
     * mojo is invoked.
     *
     * @return the list of owners
     */
    public Set getOwners()
    {
        return registeredFiles.keySet();
    }

    /**
     * Returns all paths that have been registered so far.
     *
     * @return all registered path
     */
    public PathSet getFullStructure()
    {
        return allFiles;
    }

    /**
     * Returns the list of registered files for the specified owner.
     *
     * @param id the owner
     * @return the list of files registered for that owner
     */
    public PathSet getStructure( String id )
    {
        PathSet pathSet = (PathSet) registeredFiles.get( id );
        if ( pathSet == null )
        {
            pathSet = new PathSet();
            registeredFiles.put( id, pathSet );
        }
        return pathSet;
    }
}
