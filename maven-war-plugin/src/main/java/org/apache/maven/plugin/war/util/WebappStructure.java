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

import java.io.IOException;
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

    private Map registeredFiles;

    private transient PathSet allFiles = new PathSet();

    private transient WebappStructure cache;

    /**
     * Creates a new empty instance.
     */
    public WebappStructure()
    {
        this.registeredFiles = new HashMap();
        this.cache = null;
    }

    /**
     * Creates a new instance with the specified cache.
     *
     * @param cache the cache
     */
    public WebappStructure( WebappStructure cache )
    {
        this.registeredFiles = new HashMap();
        if ( cache == null )
        {
            this.cache = new WebappStructure();
        }
        else
        {
            this.cache = cache;
        }
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
            doRegister( id, path );
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Registers the specified path for the specified owner. Invokes
     * the <tt>callback</tt> with the result of the registration.
     *
     * @param id       the owner of the path
     * @param path     the relative path from the webapp root directory
     * @param callback the callback to invoke with the result of the registration
     * @throws IOException if the callback invocation throws an IOException
     */
    public void registerFile( String id, String path, RegistrationCallback callback )
        throws IOException
    {

        
        // If the file is already in the current structure, rejects it with the current owner
        if ( isRegistered( path ) )
        {
            callback.refused( id, path, getOwner( path ) );
        }
        else
        {
            doRegister( id, path );
            // This is a new file
            if ( cache.getOwner( path ) == null )
            {
                callback.registered( id, path );

            } // The file already belonged to this owner
            else if ( cache.getOwner( path ).equals( id ) )
            {
                callback.alreadyRegistered( id, path );
            } // The file belongs to another owner and it's known currently
            else if ( getOwners().contains( cache.getOwner( path ) ) )
            {
                callback.superseded( id, path, cache.getOwner( path ) );
            } // The file belongs to another owner and it's unknown
            else
            {
                callback.supersededUnknownOwner( id, path, cache.getOwner( path ) );
            }
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

    private void doRegister( String id, String path )
    {
        getFullStructure().add( path );
        getStructure( id ).add( path );
    }

    private Object readResolve()
    {
        // the full structure should be resolved so let's rebuild it
        this.allFiles = new PathSet();
        final Iterator it = registeredFiles.values().iterator();
        while ( it.hasNext() )
        {
            PathSet pathSet = (PathSet) it.next();
            this.allFiles.addAll( pathSet );
        }
        return this;
    }

    /**
     * Callback interfce to handle events related to filepath registration in
     * the webapp.
     */
    public interface RegistrationCallback
    {


        /**
         * Called if the <tt>targetFilename</tt> for the specified <tt>ownerId</tt>
         * has been registered successfully.
         * <p/>
         * This means that the <tt>targetFilename</tt> was unknown and has been
         * registered successfully.
         *
         * @param ownerId        the ownerId
         * @param targetFilename the relative path according to the root of the webapp
         * @throws IOException if an error occured while handling this event
         */
        void registered( String ownerId, String targetFilename )
            throws IOException;

        /**
         * Called if the <tt>targetFilename</tt> for the specified <tt>ownerId</tt>
         * has already been registered.
         * <p/>
         * This means that the <tt>targetFilename</tt> was known and belongs to the
         * specified owner.
         *
         * @param ownerId        the ownerId
         * @param targetFilename the relative path according to the root of the webapp
         * @throws IOException if an error occured while handling this event
         */
        void alreadyRegistered( String ownerId, String targetFilename )
            throws IOException;

        /**
         * Called if the registration of the <tt>targetFilename</tt> for the
         * specified <tt>ownerId</tt> has been refused since the path already
         * belongs to the <tt>actualOwnerId</tt>.
         * <p/>
         * This means that the <tt>targetFilename</tt> was known and does not
         * belong to the specified owner.
         *
         * @param ownerId        the ownerId
         * @param targetFilename the relative path according to the root of the webapp
         * @param actualOwnerId  the actual owner
         * @throws IOException if an error occured while handling this event
         */
        void refused( String ownerId, String targetFilename, String actualOwnerId )
            throws IOException;

        /**
         * Called if the <tt>targetFilename</tt> for the specified <tt>ownerId</tt>
         * has been registered successfully by superseding a <tt>deprecatedOwnerId</tt>,
         * that is the previous owner of the file.
         * <p/>
         * This means that the <tt>targetFilename</tt> was known but for another
         * owner. This usually happens after a project's configuration change. As a
         * result, the file has been registered successfully to the new owner.
         *
         * @param ownerId           the ownerId
         * @param targetFilename    the relative path according to the root of the webapp
         * @param deprecatedOwnerId the previous owner that does not exist anymore
         * @throws IOException if an error occured while handling this event
         */
        void superseded( String ownerId, String targetFilename, String deprecatedOwnerId )
            throws IOException;

        /**
         * Called if the <tt>targetFilename</tt> for the specified <tt>ownerId</tt>
         * has been registered successfully by superseding a <tt>unknownOwnerId</tt>,
         * that is an owner that does not exist anymore in the current project.
         * <p/>
         * This means that the <tt>targetFilename</tt> was known but for an owner that
         * does not exist anymore. Hence the file has been registered successfully to
         * the new owner.
         *
         * @param ownerId        the ownerId
         * @param targetFilename the relative path according to the root of the webapp
         * @param unknownOwnerId the previous owner that does not exist anymore
         * @throws IOException if an error occured while handling this event
         */
        void supersededUnknownOwner( String ownerId, String targetFilename, String unknownOwnerId )
            throws IOException;

    }
}
