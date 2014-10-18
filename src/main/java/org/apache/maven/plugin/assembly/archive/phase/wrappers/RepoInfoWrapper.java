package org.apache.maven.plugin.assembly.archive.phase.wrappers;

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

import org.apache.maven.plugin.assembly.model.GroupVersionAlignment;
import org.apache.maven.plugin.assembly.model.Repository;
import org.apache.maven.shared.repository.model.RepositoryInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @version $Id$
 */
public class RepoInfoWrapper
    implements RepositoryInfo
{

    private final Repository repo;

    private List<GroupVersionAlignmentWrapper> convertedAlignments;

    public RepoInfoWrapper( final Repository repo )
    {
        this.repo = repo;
    }

    public List<String> getExcludes()
    {
        return repo.getExcludes();
    }

    public List<GroupVersionAlignmentWrapper> getGroupVersionAlignments()
    {
        final List<GroupVersionAlignment> alignments = repo.getGroupVersionAlignments();

        if ( convertedAlignments == null || alignments.size() != convertedAlignments.size() )
        {
            final List<GroupVersionAlignmentWrapper> l =
                new ArrayList<GroupVersionAlignmentWrapper>( alignments.size() );

            for ( final GroupVersionAlignment alignment : alignments )
            {
                l.add( new GroupVersionAlignmentWrapper( alignment ) );
            }

            convertedAlignments = l;
        }

        return convertedAlignments;
    }

    public List<String> getIncludes()
    {
        return repo.getIncludes();
    }

    public String getScope()
    {
        return repo.getScope();
    }

    public boolean isIncludeMetadata()
    {
        return repo.isIncludeMetadata();
    }

}
