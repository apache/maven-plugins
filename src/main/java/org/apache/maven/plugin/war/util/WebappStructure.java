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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the structure of a web application composed of multiple
 * overlays. Each overlay is registered within this structure with the
 * set of files it holds.
 * <p/>
 * Note that this structure is persisted to disk at each invocation to
 * store which owner holds which path (file).
 *
 * @author Stephane Nicoll
 * @version $Id$
 */
public class WebappStructure
{

    private Map registeredFiles;

    private List dependenciesInfo;

    private transient PathSet allFiles = new PathSet();

    private transient WebappStructure cache;

    /**
     * Creates a new empty instance.
     *
     * @param dependencies the dependencies of the project
     */
    public WebappStructure( List dependencies )
    {
        this.dependenciesInfo = createDependenciesInfoList( dependencies );
        this.registeredFiles = new HashMap();
        this.cache = null;

    }

    /**
     * Creates a new instance with the specified cache.
     *
     * @param dependencies the dependencies of the project
     * @param cache        the cache
     */
    public WebappStructure( List dependencies, WebappStructure cache )
    {
        this.dependenciesInfo = createDependenciesInfoList( dependencies );
        this.registeredFiles = new HashMap();
        if ( cache == null )
        {
            this.cache = new WebappStructure( dependencies );

        }
        else
        {
            this.cache = cache;
        }
    }

    /**
     * Returns the list of {@link DependencyInfo} for the project.
     *
     * @return the dependencies information of the project
     */
    public List getDependenciesInfo()
    {
        return dependenciesInfo;
    }

    /**
     * Returns the dependencies of the project.
     *
     * @return the dependencies of the project
     */
    public List getDependencies()
    {
        final List result = new ArrayList();
        if ( dependenciesInfo == null )
        {
            return result;
        }
        final Iterator it = dependenciesInfo.iterator();
        while ( it.hasNext() )
        {
            DependencyInfo dependencyInfo = (DependencyInfo) it.next();
            result.add( dependencyInfo.getDependency() );
        }
        return result;
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
     * Forces the registration of the specified path for the specified owner. If
     * the file is not registered yet, a simple registration is performed. If the
     * file already exists, the owner changes to the specified one.
     * <p/>
     * Beware that the semantic of the return boolean is different than the one
     * from {@link #registerFile(String, String)}; returns <tt>true</tt> if an
     * owner replacement was made and <tt>false</tt> if the file was simply registered
     * for the first time.
     *
     * @param id   the owner of the path
     * @param path the relative path from the webapp root directory
     * @return false if the file did not exist, true if the owner was replaced
     */
    public boolean registerFileForced( String id, String path )
    {
        if ( !isRegistered( path ) )
        {
            doRegister( id, path );
            return false;
        }
        else
        {
            // Force the switch to the new owner
            getStructure( getOwner( path ) ).remove( path );
            getStructure( id ).add( path );
            return true;
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
                "Should not happen, path [" + path + "] is flagged as being registered but was not found." );
        }

    }

    /**
     * Returns the owners. Note that this the returned {@link Set} may be
     * inconsistent since it represents a persistent cache across multiple
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


    /**
     * Analyze the dependencies of the project using the specified callback.
     *
     * @param callback the callback to use to report the result of the analysis
     */
    public void analyseDependencies( DependenciesAnalysisCallback callback )
    {
        if ( callback == null )
        {
            throw new NullPointerException( "Callback could not be null." );
        }
        if ( cache == null )
        {
            // Could not analyze dependencies without a cache
            return;
        }

        final List currentDependencies = new ArrayList( getDependencies() );
        final List previousDependencies = new ArrayList( cache.getDependencies() );
        final Iterator it = currentDependencies.listIterator();
        while ( it.hasNext() )
        {
            Dependency dependency = (Dependency) it.next();
            // Check if the dependency is there "as is"

            final Dependency matchingDependency = matchDependency( previousDependencies, dependency );
            if ( matchingDependency != null )
            {
                callback.unchangedDependency( dependency );
                // Handled so let's remove
                it.remove();
                previousDependencies.remove( matchingDependency );
            }
            else
            {
                // Try to get the dependency
                final Dependency previousDep = findDependency( dependency, previousDependencies );
                if ( previousDep == null )
                {
                    callback.newDependency( dependency );
                    it.remove();
                }
                else if ( !dependency.getVersion().equals( previousDep.getVersion() ) )
                {
                    callback.updatedVersion( dependency, previousDep.getVersion() );
                    it.remove();
                    previousDependencies.remove( previousDep );
                }
                else if ( !dependency.getScope().equals( previousDep.getScope() ) )
                {
                    callback.updatedScope( dependency, previousDep.getScope() );
                    it.remove();
                    previousDependencies.remove( previousDep );
                }
                else if ( dependency.isOptional() != previousDep.isOptional() )
                {
                    callback.updatedOptionalFlag( dependency, previousDep.isOptional() );
                    it.remove();
                    previousDependencies.remove( previousDep );
                }
                else
                {
                    callback.updatedUnknown( dependency, previousDep );
                    it.remove();
                    previousDependencies.remove( previousDep );
                }
            }
        }
        final Iterator previousDepIt = previousDependencies.iterator();
        while ( previousDepIt.hasNext() )
        {
            Dependency dependency = (Dependency) previousDepIt.next();
            callback.removedDependency( dependency );
        }
    }

    /**
     * Registers the target file name for the specified artifact.
     *
     * @param artifact       the artifact
     * @param targetFileName the target file name
     */
    public void registerTargetFileName( Artifact artifact, String targetFileName )
    {
        final Iterator it = dependenciesInfo.iterator();
        while ( it.hasNext() )
        {
            DependencyInfo dependencyInfo = (DependencyInfo) it.next();
            if ( WarUtils.isRelated( artifact, dependencyInfo.getDependency() ) )
            {
                dependencyInfo.setTargetFileName( targetFileName );
            }
        }
    }

    /**
     * Returns the cached target file name that matches the specified
     * dependency, that is the target file name of the previous run.
     * <p/>
     * The dependency object may have changed so the comparison is
     * based on basic attributes of the dependency.
     *
     * @param dependency a dependency
     * @return the target file name of the last run for this dependency
     */
    public String getCachedTargetFileName( Dependency dependency )
    {
        if ( cache == null )
        {
            return null;
        }
        final Iterator it = cache.getDependenciesInfo().iterator();
        while ( it.hasNext() )
        {
            DependencyInfo dependencyInfo = (DependencyInfo) it.next();
            final Dependency dependency2 = dependencyInfo.getDependency();
            if ( StringUtils.equals( dependency.getGroupId(), dependency2.getGroupId() )
                && StringUtils.equals( dependency.getArtifactId(), dependency2.getArtifactId() )
                && StringUtils.equals( dependency.getType(), dependency2.getType() )
                && StringUtils.equals( dependency.getClassifier(), dependency2.getClassifier() ) )
            {

                return dependencyInfo.getTargetFileName();

            }
        }
        return null;
    }

    // Private helpers

    private void doRegister( String id, String path )
    {
        getFullStructure().add( path );
        getStructure( id ).add( path );
    }

    /**
     * Find a dependency that is similar from the specified dependency.
     *
     * @param dependency   the dependency to find
     * @param dependencies a list of dependencies
     * @return a similar dependency or <tt>null</tt> if no similar dependency is found
     */
    private Dependency findDependency( Dependency dependency, List dependencies )
    {
        final Iterator it = dependencies.iterator();
        while ( it.hasNext() )
        {
            Dependency dep = (Dependency) it.next();
            if ( dependency.getGroupId().equals( dep.getGroupId() )
                && dependency.getArtifactId().equals( dep.getArtifactId() )
                && dependency.getType().equals( dep.getType() )
                && ( ( dependency.getClassifier() == null && dep.getClassifier() == null )
                    || ( dependency.getClassifier() != null
                        && dependency.getClassifier().equals( dep.getClassifier() ) ) ) )
            {
                return dep;
            }
        }
        return null;
    }

    private Dependency matchDependency( List dependencies, Dependency dependency )
    {
        final Iterator it = dependencies.iterator();
        while ( it.hasNext() )
        {
            Dependency dep = (Dependency) it.next();
            if ( WarUtils.dependencyEquals( dep, dependency ) )
            {
                return dep;
            }

        }
        return null;
    }


    private List createDependenciesInfoList( List dependencies )
    {
        if ( dependencies == null )
        {
            return Collections.EMPTY_LIST;
        }
        final List result = new ArrayList();
        final Iterator it = dependencies.iterator();
        while ( it.hasNext() )
        {
            Dependency dependency = (Dependency) it.next();
            result.add( new DependencyInfo( dependency ) );
        }
        return result;
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
     * Callback interface to handle events related to filepath registration in
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
         * @throws IOException if an error occurred while handling this event
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
         * @throws IOException if an error occurred while handling this event
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
         * @throws IOException if an error occurred while handling this event
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
         * @throws IOException if an error occurred while handling this event
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
         * @throws IOException if an error occurred while handling this event
         */
        void supersededUnknownOwner( String ownerId, String targetFilename, String unknownOwnerId )
            throws IOException;
    }

    /**
     * Callback interface to handle events related to dependencies analysis.
     */
    public interface DependenciesAnalysisCallback
    {

        /**
         * Called if the dependency has not changed since the last build.
         *
         * @param dependency the dependency that hasn't changed
         */
        void unchangedDependency( Dependency dependency );

        /**
         * Called if a new dependency has been added since the last build.
         *
         * @param dependency the new dependency
         */
        void newDependency( Dependency dependency );

        /**
         * Called if the dependency has been removed since the last build.
         *
         * @param dependency the dependency that has been removed
         */
        void removedDependency( Dependency dependency );

        /**
         * Called if the version of the dependency has changed since the last build.
         *
         * @param dependency      the dependency
         * @param previousVersion the previous version of the dependency
         */
        void updatedVersion( Dependency dependency, String previousVersion );

        /**
         * Called if the scope of the dependency has changed since the last build.
         *
         * @param dependency    the dependency
         * @param previousScope the previous scope
         */
        void updatedScope( Dependency dependency, String previousScope );

        /**
         * Called if the optional flag of the dependency has changed since the
         * last build.
         *
         * @param dependency       the dependency
         * @param previousOptional the previous optional flag
         */
        void updatedOptionalFlag( Dependency dependency, boolean previousOptional );

        /**
         * Called if the dependency has been updated for unknown reason.
         *
         * @param dependency  the dependency
         * @param previousDep the previous dependency
         */
        void updatedUnknown( Dependency dependency, Dependency previousDep );

    }
}
