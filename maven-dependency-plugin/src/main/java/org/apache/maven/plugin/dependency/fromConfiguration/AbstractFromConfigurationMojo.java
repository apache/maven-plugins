package org.apache.maven.plugin.dependency.fromConfiguration;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.AbstractDependencyMojo;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.dependency.utils.filters.ArtifactItemFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Abstract Parent class used by mojos that get Artifact information from the
 * plugin configuration as an ArrayList of ArtifactItems
 *
 * @see ArtifactItem
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 * @version $Id$
 *
 */
public abstract class AbstractFromConfigurationMojo
    extends AbstractDependencyMojo
{

    /**
     * Default location used for mojo unless overridden in ArtifactItem
     *
     * @parameter expression="${outputDirectory}"
     *            default-value="${project.build.directory}/dependency"
     * @optional
     * @since 1.0
     */
    private File outputDirectory;

    /**
     * Overwrite release artifacts
     *
     * @optional
     * @since 1.0
     * @parameter expression="${mdep.overWriteReleases}" default-value="false"
     */
    private boolean overWriteReleases;

    /**
     * Overwrite snapshot artifacts
     *
     * @optional
     * @since 1.0
     * @parameter expression="${mdep.overWriteSnapshots}" default-value="false"
     */
    private boolean overWriteSnapshots;

    /**
     * Overwrite if newer
     *
     * @optional
     * @since 2.0
     * @parameter expression="${mdep.overIfNewer}" default-value="true"
     */
    private boolean overWriteIfNewer;

    /**
     * Collection of ArtifactItems to work on. (ArtifactItem contains groupId,
     * artifactId, version, type, classifier, location, destFileName, markerFile and overwrite.)
     * See <a href="./usage.html">Usage</a> for details.
     *
     * @parameter
     * @required
     * @since 1.0
     */
    private ArrayList artifactItems;

    /**
     * To look up ArtifactRepository implementation
     * @component
     * @readonly
     */
    private ArtifactRepositoryFactory artifactRepositoryManager;

    /**
     * Path to override default local repository during plugin's execution.
     * To remove all downloaded artifacts as part of the build, set this value to a location under your project's target directory
     * @parameter
     * @since 2.2
     */
    private  File localRepositoryDirectory;

    /**
     * To host and cache localRepositoryDirectory
     */
    private ArtifactRepository overrideLocalRepository;

    abstract ArtifactItemFilter getMarkedArtifactFilter( ArtifactItem item );

    /**
     * Preprocesses the list of ArtifactItems. This method defaults the
     * outputDirectory if not set and creates the output Directory if it doesn't
     * exist.
     *
     * @param removeVersion
     *            remove the version from the filename.
     * @return An ArrayList of preprocessed ArtifactItems
     *
     * @throws MojoExecutionException
     *             with a message if an error occurs.
     *
     * @see ArtifactItem
     */
    protected ArrayList getProcessedArtifactItems( boolean removeVersion )
        throws MojoExecutionException
    {
        if ( artifactItems == null || artifactItems.size() < 1 )
        {
            throw new MojoExecutionException( "There are no artifactItems configured." );
        }

        Iterator iter = artifactItems.iterator();
        while ( iter.hasNext() )
        {
            ArtifactItem artifactItem = (ArtifactItem) iter.next();
            this.getLog().info( "Configured Artifact: " + artifactItem.toString() );

            if ( artifactItem.getOutputDirectory() == null )
            {
                artifactItem.setOutputDirectory( this.outputDirectory );
            }
            artifactItem.getOutputDirectory().mkdirs();

            // make sure we have a version.
            if ( StringUtils.isEmpty( artifactItem.getVersion() ) )
            {
                fillMissingArtifactVersion( artifactItem );
            }

            artifactItem.setArtifact( this.getArtifact( artifactItem ) );

            if ( StringUtils.isEmpty( artifactItem.getDestFileName() ) )
            {
                artifactItem.setDestFileName( DependencyUtil.getFormattedFileName( artifactItem.getArtifact(),
                                                                                   removeVersion ) );
            }

            try
            {
                artifactItem.setNeedsProcessing( checkIfProcessingNeeded( artifactItem ) );
            }
            catch ( ArtifactFilterException e )
            {
                throw new MojoExecutionException( e.getMessage(), e );
            }
        }
        return artifactItems;
    }

    private boolean checkIfProcessingNeeded( ArtifactItem item )
        throws MojoExecutionException, ArtifactFilterException
    {
        boolean result = false;
        if ( StringUtils.equalsIgnoreCase( item.getOverWrite(), "true" ) )
        {
            result = true;
        }
        else
        {
            ArtifactItemFilter filter = getMarkedArtifactFilter( item );
            result = filter.isArtifactIncluded( item );
        }
        return result;
    }

    /**
     * Resolves the Artifact from the remote repository if nessessary. If no
     * version is specified, it will be retrieved from the dependency list or
     * from the DependencyManagement section of the pom.
     *
     * @param artifactItem
     *            containing information about artifact from plugin
     *            configuration.
     * @return Artifact object representing the specified file.
     *
     * @throws MojoExecutionException
     *             with a message if the version can't be found in
     *             DependencyManagement.
     */
    protected Artifact getArtifact( ArtifactItem artifactItem )
        throws MojoExecutionException
    {
        Artifact artifact;

        // Map managedVersions = createManagedVersionMap( factory,
        // project.getId(), project.getDependencyManagement() );
        VersionRange vr;
        try
        {
            vr = VersionRange.createFromVersionSpec( artifactItem.getVersion() );
        }
        catch ( InvalidVersionSpecificationException e1 )
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            vr = VersionRange.createFromVersion( artifactItem.getVersion() );
        }

        if ( StringUtils.isEmpty( artifactItem.getClassifier() ) )
        {
            artifact = factory.createDependencyArtifact( artifactItem.getGroupId(), artifactItem.getArtifactId(), vr,
                                                         artifactItem.getType(), null, Artifact.SCOPE_COMPILE );
        }
        else
        {
            artifact = factory.createDependencyArtifact( artifactItem.getGroupId(), artifactItem.getArtifactId(), vr,
                                                         artifactItem.getType(), artifactItem.getClassifier(),
                                                         Artifact.SCOPE_COMPILE );
        }

        try
        {
            // mdep-50 - rolledback for now because it's breaking some
            // functionality.
            /*
             * List listeners = new ArrayList();
             *
             * Set theSet = new HashSet(); theSet.add( artifact );
             * ArtifactResolutionResult artifactResolutionResult =
             * artifactCollector.collect( theSet, project .getArtifact(),
             * managedVersions, this.local,
             * project.getRemoteArtifactRepositories(), artifactMetadataSource,
             * null, listeners ); Iterator iter =
             * artifactResolutionResult.getArtifactResolutionNodes().iterator();
             * while ( iter.hasNext() ) { ResolutionNode node = (ResolutionNode)
             * iter.next(); artifact = node.getArtifact(); }
             */

            resolver.resolve( artifact, remoteRepos, getLocal() );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Unable to resolve artifact.", e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MojoExecutionException( "Unable to find artifact.", e );
        }

        return artifact;
    }

    /**
     * Tries to find missing version from dependancy list and dependency
     * management. If found, the artifact is updated with the correct version.
     *
     * It will first look for an exact match on artifactId/groupId/classifier/type and if it doesn't find
     * a match, it will try again looking for artifactId and groupId only.
     * @param artifact
     *            representing configured file.
     * @throws MojoExecutionException
     */
    private void fillMissingArtifactVersion( ArtifactItem artifact )
        throws MojoExecutionException
    {
        if ( !findDependencyVersion( artifact, project.getDependencies(), false )
            && ( project.getDependencyManagement() == null || !findDependencyVersion( artifact, project
                .getDependencyManagement().getDependencies(), false ) )
            && !findDependencyVersion( artifact, project.getDependencies(), true )
            && ( project.getDependencyManagement() == null || !findDependencyVersion( artifact, project
                .getDependencyManagement().getDependencies(), true ) ) )
        {
            throw new MojoExecutionException( "Unable to find artifact version of " + artifact.getGroupId() + ":"
                + artifact.getArtifactId() + " in either dependency list or in project's dependency management." );
        }
    }

    /**
     * Tries to find missing version from a list of dependencies. If found, the
     * artifact is updated with the correct version.
     *
     * @param artifact
     *            representing configured file.
     * @param list
     *            list of dependencies to search.
     * @param looseMatch
     *            only look at artifactId and groupId
     * @return the found dependency
     */
    private boolean findDependencyVersion( ArtifactItem artifact, List list, boolean looseMatch )
    {
        boolean result = false;

        for ( int i = 0; i < list.size(); i++ )
        {
            Dependency dependency = (Dependency) list.get( i );
            if ( StringUtils.equals( dependency.getArtifactId(), artifact.getArtifactId() )
                && StringUtils.equals( dependency.getGroupId(), artifact.getGroupId() )
                && ( looseMatch || StringUtils.equals( dependency.getClassifier(), artifact.getClassifier() ) )
                && ( looseMatch || StringUtils.equals( dependency.getType(), artifact.getType() ) ) )
            {

                artifact.setVersion( dependency.getVersion() );

                result = true;
                break;
            }
        }

        return result;
    }

   /* private Map createManagedVersionMap( ArtifactFactory artifactFactory, String projectId,
                                         DependencyManagement dependencyManagement )
        throws MojoExecutionException
    {
        Map map;
        if ( dependencyManagement != null && dependencyManagement.getDependencies() != null )
        {
            map = new HashMap();
            for ( Iterator i = dependencyManagement.getDependencies().iterator(); i.hasNext(); )
            {
                Dependency d = (Dependency) i.next();

                try
                {
                    VersionRange versionRange = VersionRange.createFromVersionSpec( d.getVersion() );
                    Artifact artifact = artifactFactory.createDependencyArtifact( d.getGroupId(), d.getArtifactId(),
                                                                                  versionRange, d.getType(), d
                                                                                      .getClassifier(), d.getScope(), d
                                                                                      .isOptional() );
                    map.put( d.getManagementKey(), artifact );
                }
                catch ( InvalidVersionSpecificationException e )
                {
                    throw new MojoExecutionException( "Unable to parse version", e );
                }
            }
        }
        else
        {
            map = Collections.EMPTY_MAP;
        }
        return map;
    }*/


    /**
     * Override the base to
     * @return Returns the local.
     */
    protected ArtifactRepository getLocal ()
    {
        if ( this.overrideLocalRepository != null )
        {
            return this.overrideLocalRepository;
        }

        if ( this.localRepositoryDirectory != null )
        {
            //create a new local repo using existing layout, snapshots, and releases policy
            this.overrideLocalRepository = artifactRepositoryManager.createArtifactRepository(
                super.getLocal().getId(), "file://"
                + this.localRepositoryDirectory.getAbsolutePath(), super.getLocal().getLayout(), super.getLocal()
                .getSnapshots(), super.getLocal().getReleases() );

            this.getLog().debug( "Execution local repository is at: " + this.overrideLocalRepository.getBasedir() );

            return this.overrideLocalRepository;
        }

        this.overrideLocalRepository = super.getLocal();

        return this.overrideLocalRepository;
    }

    /**
     * @return Returns the artifactItems.
     */
    public ArrayList getArtifactItems()
    {
        return this.artifactItems;
    }

    /**
     * @param theArtifactItems
     *            The artifactItems to set.
     */
    public void setArtifactItems( ArrayList theArtifactItems )
    {
        this.artifactItems = theArtifactItems;
    }

    /**
     * @return Returns the outputDirectory.
     */
    public File getOutputDirectory()
    {
        return this.outputDirectory;
    }

    /**
     * @param theOutputDirectory
     *            The outputDirectory to set.
     */
    public void setOutputDirectory( File theOutputDirectory )
    {
        this.outputDirectory = theOutputDirectory;
    }

    /**
     * @return Returns the overWriteIfNewer.
     */
    public boolean isOverWriteIfNewer()
    {
        return this.overWriteIfNewer;
    }

    /**
     * @param theOverWriteIfNewer
     *            The overWriteIfNewer to set.
     */
    public void setOverWriteIfNewer( boolean theOverWriteIfNewer )
    {
        this.overWriteIfNewer = theOverWriteIfNewer;
    }

    /**
     * @return Returns the overWriteReleases.
     */
    public boolean isOverWriteReleases()
    {
        return this.overWriteReleases;
    }

    /**
     * @param theOverWriteReleases
     *            The overWriteReleases to set.
     */
    public void setOverWriteReleases( boolean theOverWriteReleases )
    {
        this.overWriteReleases = theOverWriteReleases;
    }

    /**
     * @return Returns the overWriteSnapshots.
     */
    public boolean isOverWriteSnapshots()
    {
        return this.overWriteSnapshots;
    }

    /**
     * @param theOverWriteSnapshots
     *            The overWriteSnapshots to set.
     */
    public void setOverWriteSnapshots( boolean theOverWriteSnapshots )
    {
        this.overWriteSnapshots = theOverWriteSnapshots;
    }

    public void setLocalRepositoryDirectory( File localRepositoryDirectory )
    {
        this.localRepositoryDirectory = localRepositoryDirectory;
    }
}
