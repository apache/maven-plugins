package org.apache.maven.plugins.release.config;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Configuration used for the release.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ReleaseConfiguration
{
    /**
     * The last completed phase.
     */
    private String completedPhase;

    /**
     * The Maven settings.
     */
    private Settings settings;

    /**
     * Tag base for an SVN repository.
     */
    private String tagBase;

    /**
     * Username for the SCM repository.
     */
    private String username;

    /**
     * Password for the SCM repository.
     */
    private String password;

    /**
     * URL for the SCM repository.
     */
    private String url;

    /**
     * Private key for an SSH based SCM repository.
     */
    private String privateKey;

    /**
     * Passphrase for the private key.
     */
    private String passphrase;

    /**
     * Where the release is executed.
     */
    private File workingDirectory;

    /**
     * The projects being operated on.
     */
    private List reactorProjects;

    /**
     * Whether to use edit mode when making SCM modifications. This setting is disregarded if the SCM does not support
     * edit mode, or if edit mode is compulsory for the given SCM.
     */
    private boolean useEditMode;

    /**
     * Whether to add the model schema to the top of the rewritten POM if it wasn't there already. If <code>false</code>
     * then the root element will remain untouched.
     */
    private boolean addSchema;

    /**
     * Whether to generate release POMs.
     */
    private boolean generateReleasePoms;

    /**
     * Whether the release process is interactive and the release manager should be prompted to confirm values, or
     * whether the defaults are used regardless.
     */
    private boolean interactive = true;

    /**
     * A map of projects to versions to use when releasing the given projects.
     */
    private Map releaseVersions = new HashMap();

    /**
     * A map of projects to versions to use when moving the given projects back into devlopment after release.
     */
    private Map developmentVersions = new HashMap();

    /**
     * A map of projects to original versions before any transformation.
     */
    private Map originalVersions;

    public boolean isInteractive()
    {
        return interactive;
    }

    public void setInteractive( boolean interactive )
    {
        this.interactive = interactive;
    }

    public boolean isGenerateReleasePoms()
    {
        return generateReleasePoms;
    }

    public String getCompletedPhase()
    {
        return completedPhase;
    }

    public void setCompletedPhase( String completedPhase )
    {
        this.completedPhase = completedPhase;
    }

    public Settings getSettings()
    {
        return settings;
    }

    public String getTagBase()
    {
        return tagBase;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public String getUrl()
    {
        return url;
    }

    public String getPrivateKey()
    {
        return privateKey;
    }

    public String getPassphrase()
    {
        return passphrase;
    }

    public File getWorkingDirectory()
    {
        return workingDirectory;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public void setSettings( Settings settings )
    {
        this.settings = settings;
    }

    public void setTagBase( String tagBase )
    {
        this.tagBase = tagBase;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public void setPrivateKey( String privateKey )
    {
        this.privateKey = privateKey;
    }

    public void setPassphrase( String passphrase )
    {
        this.passphrase = passphrase;
    }

    public void setWorkingDirectory( File workingDirectory )
    {
        this.workingDirectory = workingDirectory;
    }

    public List getReactorProjects()
    {
        return reactorProjects;
    }

    public void setReactorProjects( List reactorProjects )
    {
        this.reactorProjects = reactorProjects;
        this.originalVersions = null;
    }

    public boolean isUseEditMode()
    {
        return useEditMode;
    }

    public boolean isAddSchema()
    {
        return addSchema;
    }

    public void setUseEditMode( boolean useEditMode )
    {
        this.useEditMode = useEditMode;
    }

    public void setAddSchema( boolean addSchema )
    {
        this.addSchema = addSchema;
    }

    public void setGenerateReleasePoms( boolean generateReleasePoms )
    {
        this.generateReleasePoms = generateReleasePoms;
    }

    public Map getReleaseVersions()
    {
        return Collections.unmodifiableMap( releaseVersions );
    }

    public Map getDevelopmentVersions()
    {
        return Collections.unmodifiableMap( developmentVersions );
    }

    /**
     * Merge two configurations together. All SCM settings are overridden by the merge configuration, as are the
     * <code>settings</code> and <code>workingDirectory</code> fields. The <code>completedPhase</code> field is used as
     * a default from the merge configuration, but not overridden if it exists.
     *
     * @param mergeConfiguration the configuration to merge into this configuration
     * @todo double check if these are the expected behaviours
     */
    public void merge( ReleaseConfiguration mergeConfiguration )
    {
        // Overridden if configured from the caller
        this.url = mergeOverride( this.url, mergeConfiguration.url );
        this.tagBase = mergeOverride( this.tagBase, mergeConfiguration.tagBase );
        this.username = mergeOverride( this.username, mergeConfiguration.username );
        this.password = mergeOverride( this.password, mergeConfiguration.password );
        this.privateKey = mergeOverride( this.privateKey, mergeConfiguration.privateKey );
        this.passphrase = mergeOverride( this.passphrase, mergeConfiguration.passphrase );
        this.useEditMode = mergeConfiguration.useEditMode;
        this.addSchema = mergeConfiguration.addSchema;
        this.generateReleasePoms = mergeConfiguration.generateReleasePoms;
        this.interactive = mergeConfiguration.interactive;

        // These must be overridden, as they are not stored
        this.settings = mergeOverride( this.settings, mergeConfiguration.settings );
        this.workingDirectory = mergeOverride( this.workingDirectory, mergeConfiguration.workingDirectory );
        this.reactorProjects = mergeOverride( this.reactorProjects, mergeConfiguration.reactorProjects );

        // Not overridden - not configured from caller
        this.completedPhase = mergeDefault( this.completedPhase, mergeConfiguration.completedPhase );

        // The version maps are never merged
    }

    private List mergeOverride( List thisValue, List mergeValue )
    {
        return mergeValue != null ? mergeValue : thisValue;
    }

    private static File mergeOverride( File thisValue, File mergeValue )
    {
        return mergeValue != null ? mergeValue : thisValue;
    }

    private static String mergeOverride( String thisValue, String mergeValue )
    {
        return mergeValue != null ? mergeValue : thisValue;
    }

    private static Settings mergeOverride( Settings thisValue, Settings mergeValue )
    {
        return mergeValue != null ? mergeValue : thisValue;
    }

    private static String mergeDefault( String thisValue, String mergeValue )
    {
        return thisValue != null ? thisValue : mergeValue;
    }

    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null || getClass() != obj.getClass() )
        {
            return false;
        }

        ReleaseConfiguration that = (ReleaseConfiguration) obj;

        if ( addSchema != that.addSchema )
        {
            return false;
        }
        if ( generateReleasePoms != that.generateReleasePoms )
        {
            return false;
        }
        if ( interactive != that.interactive )
        {
            return false;
        }
        if ( useEditMode != that.useEditMode )
        {
            return false;
        }
        if ( completedPhase != null ? !completedPhase.equals( that.completedPhase ) : that.completedPhase != null )
        {
            return false;
        }
        if ( developmentVersions != null ? !developmentVersions.equals( that.developmentVersions )
            : that.developmentVersions != null )
        {
            return false;
        }
        if ( passphrase != null ? !passphrase.equals( that.passphrase ) : that.passphrase != null )
        {
            return false;
        }
        if ( password != null ? !password.equals( that.password ) : that.password != null )
        {
            return false;
        }
        if ( privateKey != null ? !privateKey.equals( that.privateKey ) : that.privateKey != null )
        {
            return false;
        }
        if ( reactorProjects != null ? !reactorProjects.equals( that.reactorProjects ) : that.reactorProjects != null )
        {
            return false;
        }
        if ( releaseVersions != null ? !releaseVersions.equals( that.releaseVersions ) : that.releaseVersions != null )
        {
            return false;
        }
        if ( settings != null ? !settings.equals( that.settings ) : that.settings != null )
        {
            return false;
        }
        if ( tagBase != null ? !tagBase.equals( that.tagBase ) : that.tagBase != null )
        {
            return false;
        }
        if ( url != null ? !url.equals( that.url ) : that.url != null )
        {
            return false;
        }
        if ( username != null ? !username.equals( that.username ) : that.username != null )
        {
            return false;
        }
        //noinspection RedundantIfStatement
        if ( workingDirectory != null ? !workingDirectory.equals( that.workingDirectory )
            : that.workingDirectory != null )
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result = completedPhase != null ? completedPhase.hashCode() : 0;
        result = 29 * result + ( settings != null ? settings.hashCode() : 0 );
        result = 29 * result + ( tagBase != null ? tagBase.hashCode() : 0 );
        result = 29 * result + ( username != null ? username.hashCode() : 0 );
        result = 29 * result + ( password != null ? password.hashCode() : 0 );
        result = 29 * result + ( url != null ? url.hashCode() : 0 );
        result = 29 * result + ( privateKey != null ? privateKey.hashCode() : 0 );
        result = 29 * result + ( passphrase != null ? passphrase.hashCode() : 0 );
        result = 29 * result + ( workingDirectory != null ? workingDirectory.hashCode() : 0 );
        result = 29 * result + ( reactorProjects != null ? reactorProjects.hashCode() : 0 );
        result = 29 * result + ( useEditMode ? 1 : 0 );
        result = 29 * result + ( addSchema ? 1 : 0 );
        result = 29 * result + ( generateReleasePoms ? 1 : 0 );
        result = 29 * result + ( interactive ? 1 : 0 );
        result = 29 * result + ( releaseVersions != null ? releaseVersions.hashCode() : 0 );
        result = 29 * result + ( developmentVersions != null ? developmentVersions.hashCode() : 0 );
        return result;
    }

    /**
     * Map a given project to a specified version from when it is released.
     *
     * @param projectId   the project's group and artifact ID
     * @param nextVersion the version to map to
     */
    public void mapReleaseVersion( String projectId, String nextVersion )
    {
        assert !releaseVersions.containsKey( projectId );

        releaseVersions.put( projectId, nextVersion );
    }

    /**
     * Map a given project to a specified version from when it is incremented and placed back into development.
     *
     * @param projectId   the project's group and artifact ID
     * @param nextVersion the version to map to
     */
    public void mapDevelopmentVersion( String projectId, String nextVersion )
    {
        assert !developmentVersions.containsKey( projectId );

        developmentVersions.put( projectId, nextVersion );
    }

    /**
     * Retrieve the original version map, before transformation, keyed by project's versionless identifier.
     * @return the map of project IDs to versions.
     */
    public synchronized Map getOriginalVersions()
    {
        if ( originalVersions == null )
        {
            originalVersions = new HashMap();
            for ( Iterator i = reactorProjects.iterator(); i.hasNext(); )
            {
                MavenProject project = (MavenProject) i.next();
                originalVersions.put( ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() ),
                                      project.getVersion() );
            }
        }
        return originalVersions;
    }
}
