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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;

/**
 * Provides utility methods for selecting build jobs based on environmental conditions.
 *
 * @author Benjamin Bentmann
 */
class SelectorUtils
{

    static void parseList( String list, Collection<String> includes, Collection<String> excludes )
    {
        String[] tokens = ( list != null ) ? StringUtils.split( list, "," ) : new String[0];

        for ( String token1 : tokens )
        {
            String token = token1.trim();

            if ( token.startsWith( "!" ) )
            {
                excludes.add( token.substring( 1 ) );
            }
            else
            {
                includes.add( token );
            }
        }
    }

    static boolean isOsFamily( String osSpec )
    {
        List<String> includes = new ArrayList<String>();
        List<String> excludes = new ArrayList<String>();
        parseList( osSpec, includes, excludes );

        return isOsFamily( includes, true ) && !isOsFamily( excludes, false );
    }

    static boolean isOsFamily( List<String> families, boolean defaultMatch )
    {
        if ( families != null && !families.isEmpty() )
        {
            for ( String family : families )
            {
                if ( Os.isFamily( family ) )
                {
                    return true;
                }
            }

            return false;
        }
        else
        {
            return defaultMatch;
        }
    }

    /**
     * Retrieves the current Maven version.
     *
     * @return The current Maven version.
     */
    static String getMavenVersion()
    {
        try
        {
            // This relies on the fact that MavenProject is the in core classloader
            // and that the core classloader is for the maven-core artifact
            // and that should have a pom.properties file
            // if this ever changes, we will have to revisit this code.
            Properties properties = new Properties();
            // CHECKSTYLE_OFF: LineLength
            properties.load( MavenProject.class.getClassLoader().getResourceAsStream( "META-INF/maven/org.apache.maven/maven-core/pom.properties" ) );
            // CHECKSTYLE_ON: LineLength
            return StringUtils.trim( properties.getProperty( "version" ) );
        }
        catch ( Exception e )
        {
            return null;
        }
    }

    static String getMavenVersion( File mavenHome )
    {
        File mavenLib = new File( mavenHome, "lib" );
        File[] jarFiles = mavenLib.listFiles( new FilenameFilter()
        {
            public boolean accept( File dir, String name )
            {
                return name.endsWith( ".jar" );
            }
        } );

        for ( File file : jarFiles )
        {
            try
            {
                @SuppressWarnings( "deprecation" )
                URL url =
                    new URL( "jar:" + file.toURL().toExternalForm()
                        + "!/META-INF/maven/org.apache.maven/maven-core/pom.properties" );

                Properties properties = new Properties();
                properties.load( url.openStream() );
                String version = StringUtils.trim( properties.getProperty( "version" ) );
                if ( version != null )
                {
                    return version;
                }
            }
            catch ( MalformedURLException e )
            {
                // ignore
            }
            catch ( IOException e )
            {
                // ignore
            }
        }
        return null;
    }

    static boolean isMavenVersion( String mavenSpec )
    {
        return isMavenVersion( mavenSpec, getMavenVersion() );
    }

    static boolean isMavenVersion( String mavenSpec, String actualVersion )
    {
        List<String> includes = new ArrayList<String>();
        List<String> excludes = new ArrayList<String>();
        parseList( mavenSpec, includes, excludes );

        List<Integer> mavenVersionList = parseVersion( actualVersion );

        return isJreVersion( mavenVersionList, includes, true ) && !isJreVersion( mavenVersionList, excludes, false );
    }

    static String getJreVersion()
    {
        return System.getProperty( "java.version", "" );
    }

    static String getJreVersion( File javaHome )
    {
        // @todo detect actual version
        return null;
    }

    static boolean isJreVersion( String jreSpec )
    {
        return isJreVersion( jreSpec, getJreVersion() );
    }

    static boolean isJreVersion( String jreSpec, String actualJreVersion )
    {
        List<String> includes = new ArrayList<String>();
        List<String> excludes = new ArrayList<String>();
        parseList( jreSpec, includes, excludes );

        List<Integer> jreVersion = parseVersion( actualJreVersion );

        return isJreVersion( jreVersion, includes, true ) && !isJreVersion( jreVersion, excludes, false );
    }

    static boolean isJreVersion( List<Integer> jreVersion, List<String> versionPatterns, boolean defaultMatch )
    {
        if ( versionPatterns != null && !versionPatterns.isEmpty() )
        {
            for ( String versionPattern : versionPatterns )
            {
                if ( isJreVersion( jreVersion, versionPattern ) )
                {
                    return true;
                }
            }

            return false;
        }
        else
        {
            return defaultMatch;
        }
    }

    static boolean isJreVersion( List<Integer> jreVersion, String versionPattern )
    {
        List<Integer> checkVersion = parseVersion( versionPattern );

        if ( versionPattern.endsWith( "+" ) )
        {
            // 1.5+ <=> [1.5,)
            return compareVersions( jreVersion, checkVersion ) >= 0;
        }
        else if ( versionPattern.endsWith( "-" ) )
        {
            // 1.5- <=> (,1.5)
            return compareVersions( jreVersion, checkVersion ) < 0;
        }
        else
        {
            // 1.5 <=> [1.5,1.6)
            return checkVersion.size() <= jreVersion.size()
                && checkVersion.equals( jreVersion.subList( 0, checkVersion.size() ) );
        }
    }

    static List<Integer> parseVersion( String version )
    {
        version = version.replaceAll( "[^0-9]", "." );

        String[] tokens = StringUtils.split( version, "." );

        List<Integer> numbers = new ArrayList<Integer>();

        for ( String token : tokens )
        {
            numbers.add( Integer.valueOf( token ) );
        }

        return numbers;
    }

    static int compareVersions( List<Integer> version1, List<Integer> version2 )
    {
        for ( Iterator<Integer> it1 = version1.iterator(), it2 = version2.iterator();; )
        {
            if ( !it1.hasNext() )
            {
                return it2.hasNext() ? -1 : 0;
            }
            if ( !it2.hasNext() )
            {
                return it1.hasNext() ? 1 : 0;
            }

            Integer num1 = it1.next();
            Integer num2 = it2.next();

            int rel = num1.compareTo( num2 );
            if ( rel != 0 )
            {
                return rel;
            }
        }
    }

}
