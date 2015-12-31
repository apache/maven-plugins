package org.apache.maven.plugins.assembly.archive.phase.wrappers;

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

import org.apache.maven.plugins.assembly.model.Repository;
import org.apache.maven.plugins.assembly.repository.model.GroupVersionAlignment;
import org.apache.maven.plugins.assembly.repository.model.RepositoryInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @version $Id$
 */
public class RepoInfoWrapper
    implements RepositoryInfo
{

    private final Repository repo;

    private List<GroupVersionAlignment> convertedAlignments;

    /**
     * @param repo The {@link Repository}
     */
    public RepoInfoWrapper( final Repository repo )
    {
        this.repo = repo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getExcludes()
    {
        return repo.getExcludes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GroupVersionAlignment> getGroupVersionAlignments()
    {
        final List<org.apache.maven.plugins.assembly.model.GroupVersionAlignment> alignments =
            repo.getGroupVersionAlignments();

        if ( convertedAlignments == null || alignments.size() != convertedAlignments.size() )
        {
            final List<GroupVersionAlignment> l = new ArrayList<GroupVersionAlignment>( alignments.size() );

            for ( final org.apache.maven.plugins.assembly.model.GroupVersionAlignment alignment : alignments )
            {
                l.add( new GroupVersionAlignmentWrapper( alignment ) );
            }

            convertedAlignments = l;
        }

        return convertedAlignments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getIncludes()
    {
        return repo.getIncludes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScope()
    {
        return repo.getScope();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIncludeMetadata()
    {
        return repo.isIncludeMetadata();
    }

}
