package org.apache.maven.ant.tasks.support;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.tools.ant.util.FileNameMapper;
import org.codehaus.plexus.util.StringUtils;

/**
 * Ant filename mapper to remove version info from filename when copying dependencies.
 *
 * @author <a href="mailto:hboutemy@apache.org">Herve Boutemy</a>
 * @version $Id$
 */
public class VersionMapper
    implements FileNameMapper, Comparator<String>
{
    private List<String> versions;

    private String to;

    public String[] mapFileName( String sourceFileName )
    {
        String originalFileName = new File( sourceFileName ).getName();
        for ( String version : versions )
        {
            int index = originalFileName.indexOf( version );
            if ( index >= 0 )
            {
                // remove version in artifactId-version(-classifier).type
                String baseFilename = originalFileName.substring( 0, index - 1 );
                String extension = originalFileName.substring( index + version.length() );
                String path = sourceFileName.substring( 0, sourceFileName.length() - originalFileName.length() );
                if ( "flatten".equals( to ) )
                {
                    path = "";
                }
                return new String[] { path + baseFilename + extension };
            }
        }
        return new String[] { sourceFileName };
    }

    /**
     * Set the versions identifiers that this mapper can remove from filenames. The separator value used is path
     * separator, as used by dependencies task when setting <code>versionsId</code> property value.
     */
    public void setFrom( String from )
    {
        String[] split = StringUtils.split( from, File.pathSeparator );
        // sort, from lengthiest to smallest
        Arrays.sort( split, this );
        versions = Arrays.asList( split );
    }

    /**
     * By default, only filename is changed, but if this attribute is set to <code>flatten</code>, directory is removed.
     */
    public void setTo( String to )
    {
        this.to = to;
    }

    public int compare( String s1, String s2 )
    {
        int lengthDiff = s2.length() - s1.length();
        return ( lengthDiff != 0 ) ? lengthDiff : s1.compareTo( s2 );
    }
}
