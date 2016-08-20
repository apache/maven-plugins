package org.apache.maven.plugins.war;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * An overlay is a skeleton WAR added to another WAR project in order to inject a functionality, resources or any other
 * shared component.</p>
 * 
 * <p>Note that a particular WAR dependency can be added multiple times as an overlay with different includes/excludes
 * filter; this allows building a fine grained overwriting policy.</p>
 * 
 * <p>The current project can also be described as an overlay and can not be specified twice. An overlay with no groupId
 * and no artifactId represents the current project.</p>
 *
 * @author Stephane Nicoll
 * @version $Id$
 */
public class Overlay
{

    /**
     * The list of default includes.
     */
    public static final String[] DEFAULT_INCLUDES = new String[] { "**/**" };

    /**
     * The list of default excludes.
     */
    public static final String[] DEFAULT_EXCLUDES = new String[] { "META-INF/MANIFEST.MF" };

    private String id;

    private String groupId;

    private String artifactId;

    private String classifier = null;

    private String[] includes = DEFAULT_INCLUDES;

    private String[] excludes = DEFAULT_EXCLUDES;

    private boolean filtered = false;

    private boolean skip = false;

    private Artifact artifact;

    private String targetPath;

    /** default overlay type is war */
    private String type = "war";

    /**
     * Create instance.
     */
    public Overlay()
    {
        super();
    }

    /**
     * @param groupId {@link #groupId}
     * @param artifactId {@link #artifactId}
     */
    public Overlay( String groupId, String artifactId )
    {
        this();
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    /**
     * Specify whether this overlay represents the current project or not.
     *
     * @return true if the overlay represents the current project, false otherwise
     */
    public boolean isCurrentProject()
    {
        return ( groupId == null && artifactId == null );
    }

    /**
     * @return {@link Overlay} instance.
     */
    public static Overlay createInstance()
    {
        Overlay overlay = new Overlay();
        overlay.setId( "currentBuild" );
        return overlay;
    }

    // Getters and Setters

    /**
     * @return The id.
     */
    public String getId()
    {
        if ( id == null )
        {
            final StringBuilder sb = new StringBuilder();
            sb.append( getGroupId() ).append( ":" ).append( getArtifactId() );
            if ( getClassifier() != null )
            {
                sb.append( ":" ).append( getClassifier() );
            }
            id = sb.toString();
        }
        return id;
    }

    /**
     * @param id The id.
     */
    public void setId( String id )
    {
        this.id = id;
    }

    /**
     * @return {@link #groupId}
     */
    public String getGroupId()
    {
        return groupId;
    }

    /**
     * @param groupId {@link #groupId}
     */
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    /**
     * @return {@link #artifactId}
     */
    public String getArtifactId()
    {
        return artifactId;
    }

    /**
     * @param artifactId {@link #artifactId}
     */
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    /**
     * @return {@link #classifier}
     */
    public String getClassifier()
    {
        return classifier;
    }

    /**
     * @param classifier {@link #classifier}
     */
    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    /**
     * @return {@link #includes}
     */
    public String[] getIncludes()
    {
        return includes;
    }

    /**
     * @param includes {@link #includes}
     */
    public void setIncludes( String includes )
    {
        this.includes = parse( includes );
    }

    /**
     * @param includes {@link #includes}
     */
    public void setIncludes( String[] includes )
    {
        this.includes = includes;
    }

    /**
     * @return {@link #excludes}
     */
    public String[] getExcludes()
    {
        return excludes;
    }

    /**
     * @param excludes {@link #excludes}
     */
    public void setExcludes( String excludes )
    {
        this.excludes = parse( excludes );
    }

    /**
     * @param excludes {@link #excludes}
     */
    public void setExcludes( String[] excludes )
    {
        this.excludes = excludes;
    }

    /**
     * @return {@link #filtered}
     */
    public boolean isFiltered()
    {
        return filtered;
    }

    /**
     * @param filtered {@link #filtered}
     */
    public void setFiltered( boolean filtered )
    {
        this.filtered = filtered;
    }

    /**
     * @return {@link #skip}
     */
    public boolean shouldSkip()
    {
        return skip;
    }

    /**
     * @param skip {@link #skip}
     */
    public void setSkip( boolean skip )
    {
        this.skip = skip;
    }

    /**
     * @return {@link #artifact}
     */
    public Artifact getArtifact()
    {
        return artifact;
    }

    /**
     * @param artifact {@link #artifact}
     */
    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }

    /**
     * @return {@link #targetPath}
     */
    public String getTargetPath()
    {
        return targetPath;
    }

    /**
     * @param targetPath {@link #targetPath}
     */
    public void setTargetPath( String targetPath )
    {
        this.targetPath = targetPath;
    }

    /**
     * @return {@link #type}
     */
    public String getType()
    {
        return type;
    }

    /**
     * @param type {@link #type}
     */
    public void setType( String type )
    {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return " id " + getId();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        Overlay overlay = (Overlay) o;

        if ( excludes != null ? !Arrays.equals( excludes, overlay.excludes ) : overlay.excludes != null )
        {
            return false;
        }
        if ( getId() != null ? !getId().equals( overlay.getId() ) : overlay.getId() != null )
        {
            return false;
        }
        if ( includes != null ? !Arrays.equals( includes, overlay.includes ) : overlay.includes != null )
        {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        int result;
        result = ( getId() != null ? getId().hashCode() : 0 );
        result = 31 * result + ( includes != null ? includes.hashCode() : 0 );
        result = 31 * result + ( excludes != null ? excludes.hashCode() : 0 );
        return result;
    }

    private String[] parse( String s )
    {
        final List<String> result = new ArrayList<String>();
        if ( s == null )
        {
            return result.toArray( new String[result.size()] );
        }
        else
        {
            String[] tokens = s.split( "," );
            for ( String token : tokens )
            {
                result.add( token.trim() );
            }
            return result.toArray( new String[result.size()] );
        }
    }

}
