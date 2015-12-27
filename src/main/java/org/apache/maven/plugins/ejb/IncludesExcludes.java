package org.apache.maven.plugins.ejb;

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

import java.util.Collections;
import java.util.List;

/**
 * @author Karl Heinz Marbaise <khmarbaise@apache.org>
 */
public class IncludesExcludes
{
    private List<String> includes;

    private List<String> defaultIncludes;

    private List<String> excludes;

    private List<String> defaultExcludes;

    public IncludesExcludes( List<String> includes, List<String> excludes, List<String> defaultIncludes,
                             List<String> defaultExcludes )
    {
        this.includes = makeNonNullList( includes );
        this.excludes = makeNonNullList( excludes );
        this.defaultIncludes = makeNonNullList( defaultIncludes );
        this.defaultExcludes = makeNonNullList( defaultExcludes );
    }

    public String[] resultingIncludes()
    {
        String[] result = new String[0];
        if ( includes.isEmpty() )
        {
            result = defaultIncludes.toArray( new String[defaultIncludes.size()] );
        }
        else
        {
            result = includes.toArray( new String[includes.size()] );
        }

        return result;
    }

    public String[] resultingExcludes()
    {
        String[] result = new String[0];
        if ( excludes.isEmpty() )
        {
            result = defaultExcludes.toArray( new String[defaultExcludes.size()] );
        }
        else
        {
            result = excludes.toArray( new String[excludes.size()] );
        }

        return result;

    }

    private List<String> makeNonNullList( List<String> in )
    {
        if ( in == null )
        {
            return Collections.<String>emptyList();
        }
        else
        {
            return in;
        }
    }

}
