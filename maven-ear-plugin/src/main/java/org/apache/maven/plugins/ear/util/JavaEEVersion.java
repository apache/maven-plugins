package org.apache.maven.plugins.ear.util;

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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the supported JavaEE version.
 * 
 * @author Stephane Nicoll
 */
public class JavaEEVersion
    implements Comparable<JavaEEVersion>
{

    private static final String VERSION_1_3 = "1.3";

    private static final String VERSION_1_4 = "1.4";

    private static final String VERSION_5 = "5";

    private static final String VERSION_6 = "6";

    private static final String VERSION_7 = "7";
    
    private static final String VERSION_8 = "8";

    private static final Map<String, JavaEEVersion> VERSION_MAP = new HashMap<String, JavaEEVersion>();

    /**
     * Represents the J2EE 1.3 version.
     */
    public static final JavaEEVersion ONE_DOT_THREE = new JavaEEVersion( Integer.valueOf( 0 ), VERSION_1_3 );

    /**
     * Represents the J2EE 1.4 version.
     */
    public static final JavaEEVersion ONE_DOT_FOUR = new JavaEEVersion( Integer.valueOf( 1 ), VERSION_1_4 );

    /**
     * Represents the JavaEE 5 version.
     */
    public static final JavaEEVersion FIVE = new JavaEEVersion( Integer.valueOf( 2 ), VERSION_5 );

    /**
     * Represents the JavaEE 6 version.
     */
    public static final JavaEEVersion SIX = new JavaEEVersion( Integer.valueOf( 3 ), VERSION_6 );

    /**
     * Represents the JavaEE 7 version.
     */
    public static final JavaEEVersion SEVEN = new JavaEEVersion( Integer.valueOf( 4 ), VERSION_7 );

    /**
     * Represents the JavaEE 8 version.
     */
    public static final JavaEEVersion EIGHT = new JavaEEVersion( Integer.valueOf( 5 ), VERSION_8 );

    private final Integer index;

    private final String version;

    private JavaEEVersion( Integer index, String version )
    {
        this.index = index;
        this.version = version;
        VERSION_MAP.put( version, this );
    }

    /**
     * @param paramVersion The version.
     * @return {@link JavaEEVersion}
     * @throws InvalidJavaEEVersion in case of a wrong version.
     */
    public static JavaEEVersion getJavaEEVersion( String paramVersion )
        throws InvalidJavaEEVersion
    {
        if ( !isValid( paramVersion ) )
        {
            throw new InvalidJavaEEVersion( "Invalid version [" + paramVersion + "]", paramVersion );
        }
        return VERSION_MAP.get( paramVersion );
    }

    /**
     * Returns the version as a string.
     * 
     * @return the version string
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * Specifies if this version is greater or equal to the specified version.
     * 
     * @param parmVersion the version to check
     * @return true if this version is greater or equal to <tt>version</tt>
     */
    public boolean ge( JavaEEVersion parmVersion )
    {
        return this.compareTo( parmVersion ) >= 0;
    }

    /**
     * Specifies if this version is greater than the specified version.
     * 
     * @param paramVersion the version to check
     * @return true if this version is greater to <tt>version</tt>
     */
    public boolean gt( JavaEEVersion paramVersion )
    {
        return this.compareTo( paramVersion ) > 0;
    }

    /**
     * Specifies if this version is equal to the specified version.
     * 
     * @param paramVersion the version to check
     * @return true if this version is equal to <tt>version</tt>
     */
    public boolean eq( JavaEEVersion paramVersion )
    {
        return this.compareTo( paramVersion ) == 0;
    }

    /**
     * Specifies if this version is less or equal to the specified version.
     * 
     * @param paramVersion the version to check
     * @return true if this version is less or equal to <tt>version</tt>
     */
    public boolean le( JavaEEVersion paramVersion )
    {
        return this.compareTo( paramVersion ) <= 0;
    }

    /**
     * Specifies if this version is less than the specified version.
     * 
     * @param paramVersion the version to check
     * @return true if this version is less or equal to <tt>version</tt>
     */
    public boolean lt( JavaEEVersion paramVersion )
    {
        return this.compareTo( paramVersion ) < 0;
    }

    /**
     * Checks if the specified version string is valid.
     * 
     * @param paramVersion the version string to check
     * @return <tt>true</tt> if the version is valid
     */
    private static boolean isValid( String paramVersion )
    {
        if ( paramVersion == null )
        {
            throw new IllegalArgumentException( "version could not be null." );
        }
        // @formatter:off
        return VERSION_1_3.equals( paramVersion ) 
            || VERSION_1_4.equals( paramVersion )
            || VERSION_5.equals( paramVersion ) 
            || VERSION_6.equals( paramVersion ) 
            || VERSION_7.equals( paramVersion )
            || VERSION_8.equals( paramVersion );
        // @formatter:on
    }

    /** {@inheritDoc} */
    public int compareTo( JavaEEVersion otherVersion )
    {
        if ( otherVersion == null )
        {
            throw new IllegalArgumentException( "other object to compare to could not be null." );
        }
        return index.compareTo( otherVersion.index );
    }
}