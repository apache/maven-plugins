package org.apache.maven.plugin.war;

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
 * An overlay is a skeleton war added to another war project in order to inject a
 * functionnality, resources or any other shared component.
 * <p/>
 * Note that a particlar war dependency can be added multiple times as an overlay
 * with different includes/excludes filter; this allows building a fine grained
 * overwriting policy.
 * <p/>
 * The current project can also be described as an overlay and could not be specified
 * twice. An overlay with no groupId and no artifactId is detected as defining the
 * current project.
 *
 * @author Stephane Nicoll
 * @version $Id$
 */
public class Overlay
{

    public static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};

    public static final String[] DEFAULT_EXCLUDES = new String[]{"META-INF/MANIFEST.MF"};

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

    public Overlay()
    {
        super();
    }


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

    public static Overlay createInstance()
    {
        Overlay overlay = new Overlay();
        overlay.setId( "currentBuild" );
        return overlay;
    }

    // Getters and Setters

    public String getId()
    {
        if ( id == null )
        {
            final StringBuffer sb = new StringBuffer();
            sb.append( getGroupId() ).append( ":" ).append( getArtifactId() );
            if ( getClassifier() != null )
            {
                sb.append( ":" ).append( getClassifier() );
            }
            id = sb.toString();
        }
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public String[] getIncludes()
    {
        return includes;
    }

    public void setIncludes( String includes )
    {
        this.includes = parse( includes );
    }

    public void setIncludes( String[] includes )
    {
        this.includes = includes;
    }

    public String[] getExcludes()
    {
        return excludes;
    }

    public void setExcludes( String excludes )
    {
        this.excludes = parse( excludes );
    }

    public void setExcludes( String[] excludes )
    {
        this.excludes = excludes;
    }

    public boolean isFiltered()
    {
        return filtered;
    }

    public void setFiltered( boolean filtered )
    {
        this.filtered = filtered;
    }

    public boolean shouldSkip()
    {
        return skip;
    }

    public void setSkip( boolean skip )
    {
        this.skip = skip;
    }

    public Artifact getArtifact()
    {
        return artifact;
    }

    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }

    public String getTargetPath()
    {
        return targetPath;
    }


    public void setTargetPath( String targetPath )
    {
        this.targetPath = targetPath;
    }

    public String getType()
    {
        return type;
    }


    public void setType( String type )
    {
        this.type = type;
    }
    
    public String toString()
    {
        return " id " + getId();
    }


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
        final List result = new ArrayList();
        if ( s == null )
        {
            return (String[]) result.toArray( new String[result.size()] );
        }
        else
        {
            String[] tokens = s.split( "," );
            for ( int i = 0; i < tokens.length; i++ )
            {
                String token = tokens[i];
                result.add( token.trim() );
            }
            return (String[]) result.toArray( new String[result.size()] );
        }
    }

}
