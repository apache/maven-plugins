package org.apache.maven.plugins.assembly.functions;

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
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class MavenProjects
{
    public static void without( Iterable<MavenProject> source, String packagingType, MavenProjectConsumer consumer )
    {
        for ( MavenProject project : source )
        {
            if ( !packagingType.equals( project.getPackaging() ) )
            {
                consumer.accept( project );
            }
        }
    }

    public static void select( Iterable<MavenProject> source, String packagingType, MavenProjectConsumer consumer )
    {
        for ( MavenProject project : source )
        {
            if ( packagingType.equals( project.getPackaging() ) )
            {
                consumer.accept( project );
            }
        }
    }

    public static void select( Iterable<MavenProject> source, String packagingType, MavenProjectConsumer include,
                               MavenProjectConsumer excluded )
    {
        for ( MavenProject project : source )
        {
            if ( packagingType.equals( project.getPackaging() ) )
            {
                include.accept( project );
            }
            else
            {
                excluded.accept( project );
            }
        }
    }

    @Nullable
    public static Artifact findArtifactByClassifier( MavenProject mavenProject, String classifier )
    {
        final List<Artifact> attachments = mavenProject.getAttachedArtifacts();
        if ( ( attachments != null ) && !attachments.isEmpty() )
        {
            for ( final Artifact attachment : attachments )
            {
                if ( classifier.equals( attachment.getClassifier() ) )
                {
                    return attachment;
                }
            }
        }
        return null;
    }


    public static MavenProjectConsumer log( final Logger logger )
    {
        return new MavenProjectConsumer()
        {
            @Override
            public void accept( MavenProject project )
            {
                final String projectId = ArtifactUtils.versionlessKey( project.getGroupId(), project.getArtifactId() );

                logger.debug( "Excluding POM-packaging module: " + projectId );
            }
        };
    }

    public static MavenProjectConsumer addTo( final Set<MavenProject> set )
    {
        return new MavenProjectConsumer()
        {
            @Override
            public void accept( MavenProject project )
            {
                set.add( project );
            }
        };
    }

}
