package org.apache.maven.plugin.invoker;

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

import java.util.Comparator;
import java.util.List;

import org.apache.maven.plugin.invoker.model.BuildJob;
import org.codehaus.plexus.util.MatchPatterns;

/**
 * Ensures that setupProjects are always the first
 * 
 * @author Robert Scholte
 * @since 3.0
 */
public class BuildJobComparator
    implements Comparator<BuildJob>
{
    private List<String> setupIncludes;

    public BuildJobComparator( List<String> setupIncludes )
    {
        this.setupIncludes = setupIncludes;
    }

    @Override
    public int compare( BuildJob job1, BuildJob job2 )
    {
        MatchPatterns setupPatterns = MatchPatterns.from( setupIncludes );

        if ( setupPatterns.matches( job1.getProject(), true ) )
        {
            if ( setupPatterns.matches( job2.getProject(), true ) )
            {
                return 0;
            }
            else
            {
                return -1;
            }
        }
        else
        {
            if ( setupPatterns.matches( job2.getProject(), true ) )
            {
                return 1;
            }
            else
            {
                return 0;
            }
        }
    }

}
