package org.apache.maven.plugin.javadoc;

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

import org.codehaus.plexus.util.StringUtils;

/**
 * Once the plugin requires Java9, this class can be replaced with java.lang.Runtime.Version
 * <p>
 * <strong>Note: </strong> Ensure the methods match, although parse+compareTo+toString should be enough.
 * </p>
 * 
 * 
 * @author Robert Scholte
 * @since 3.0.0
 */
public class JavadocVersion implements Comparable<JavadocVersion>
{
    private String rawVersion;

    private JavadocVersion( String rawVersion )
    {
        if ( StringUtils.isEmpty( rawVersion ) )
        {
            throw new IllegalArgumentException( "The rawVersion could not be null." );
        }
        this.rawVersion = rawVersion;
    }

    /**
     * Parser only the version-scheme.
     * 
     * @param s the version string
     * @return the version wrapped in a JavadocVersion
     */
    static JavadocVersion parse( String s ) 
    {
        return new JavadocVersion( s );
    }

    @Override
    public int compareTo( JavadocVersion other )
    {
        String[] thisSegments = this.rawVersion.split( "\\." );
        String[] otherSegments = other.rawVersion.split( "\\." );
        
        int minSegments = Math.min( thisSegments.length, otherSegments.length );
        
        for ( int index = 0; index < minSegments; index++ )
        {
            int thisValue = Integer.parseInt( thisSegments[index] );
            int otherValue = Integer.parseInt( otherSegments[index] );
            
            int compareValue = Integer.compare( thisValue, otherValue );
            
            if ( compareValue != 0 )
            {
                return compareValue;
            }
        }
        
        return ( thisSegments.length - otherSegments.length );
    }

    @Override
    public String toString()
    {
        return rawVersion;
    }
}
