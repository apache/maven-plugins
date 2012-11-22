package org.apache.maven.plugin.jira;

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

import org.apache.maven.plugin.logging.Log;

import java.util.List;

/**
 * An interface for building a search query for JIRA.
 *
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.8
 */
public interface JiraQueryBuilder
{
    String build();

    JiraQueryBuilder components( String components );

    JiraQueryBuilder components( List<String> components );

    JiraQueryBuilder filter( String filter );

    JiraQueryBuilder fixVersion( String fixVersion );

    JiraQueryBuilder fixVersionIds( String fixVersionIds );

    JiraQueryBuilder fixVersionIds( List<String> fixVersionIds );

    Log getLog();

    JiraQueryBuilder priorityIds( String priorityIds );

    JiraQueryBuilder priorityIds( List<String> priorityIds );

    JiraQueryBuilder project( String project );

    JiraQueryBuilder resolutionIds( String resolutionIds );

    JiraQueryBuilder resolutionIds( List<String> resolutionIds );

    JiraQueryBuilder sortColumnNames( String sortColumnNames );

    JiraQueryBuilder statusIds( String statusIds );

    JiraQueryBuilder statusIds( List<String> statusIds );

    JiraQueryBuilder typeIds( String typeIds );

    JiraQueryBuilder typeIds( List<String> typeIds );
}
