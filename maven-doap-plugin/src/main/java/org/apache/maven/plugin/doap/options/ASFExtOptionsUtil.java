package org.apache.maven.plugin.doap.options;

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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Developer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * Utility class for {@link ASFExtOptions} class.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 1.1
 */
public class ASFExtOptionsUtil
{
    /** Apache domain name, i.e. apache.org */
    private static final String APACHE_DOMAIN_NAME = "apache.org";

    /**
     * The ASF category resource.
     *
     * @see <a href="http://projects.apache.org/guidelines.html">http://projects.apache.org/guidelines.html</a>
     */
    public static final String CATEGORY_RESOURCE = "http://projects.apache.org/category/";

    /** Projects related to building/maintaining source code/websites. */
    public static final String BUILD_MANAGEMENT_CATEGORY = "build-management";

    /** Projects related to databases. */
    public static final String DATABASE_CATEGORY = "database";

    /** Related to the HyperText Transfer Protocol. */
    public static final String HTTP_CATEGORY = "http";

    /** Modules designed for use by the Apache HTTP Server. */
    public static final String HTTP_MODULES_CATEGORY = "httpd-modules";

    /** A library meant to be used by other applications. */
    public static final String LIBRARY_CATEGORY = "library";

    /** Servers or applications related to internet mail protocols. */
    public static final String MAIL_CATEGORY = "mail";

    /** Anything that acts as a client across a network. */
    public static final String NETWORK_CLIENT_CATEGORY = "network-client";

    /** Anything that acts as a server across a network. */
    public static final String NETWORK_SERVER_CATEGORY = "network-server";

    /** Software designed to test or verify other software. */
    public static final String TESTING_CATEGORY = "testing";

    /** Unifying frameworks for website development. */
    public static final String WEB_FRAMEWORK_CATEGORY = "web-framework";

    /** Software based on XML technologies. */
    public static final String XML_CATEGORY = "xml";

    /** All categories supported by ASF */
    public static final String[] CATEGORIES = { BUILD_MANAGEMENT_CATEGORY, DATABASE_CATEGORY, HTTP_CATEGORY,
        HTTP_MODULES_CATEGORY, LIBRARY_CATEGORY, MAIL_CATEGORY, NETWORK_CLIENT_CATEGORY, NETWORK_SERVER_CATEGORY,
        TESTING_CATEGORY, WEB_FRAMEWORK_CATEGORY, XML_CATEGORY };

    /** C or C++ Programming Language. */
    public static final String C_PROGRAMMING_LANGUAGE = "C";

    /** Java Programming Language and all its components. */
    public static final String JAVA_PROGRAMMING_LANGUAGE = "Java";

    /** Perl Programming Language. */
    public static final String PERL_PROGRAMMING_LANGUAGE = "Perl";

    /** Python Programming Language. */
    public static final String PYTHON_PROGRAMMING_LANGUAGE = "Python";

    /** Scalable Vector Graphic Programming Language. */
    public static final String SVG_PROGRAMMING_LANGUAGE = "SVG";

    /** Tcl Programming Language. */
    public static final String TCL_PROGRAMMING_LANGUAGE = "Tcl";

    /** All Programming Languages supported by ASF */
    public static final String[] PROGRAMMING_LANGUAGES = { C_PROGRAMMING_LANGUAGE, JAVA_PROGRAMMING_LANGUAGE,
        PERL_PROGRAMMING_LANGUAGE, PYTHON_PROGRAMMING_LANGUAGE, SVG_PROGRAMMING_LANGUAGE, TCL_PROGRAMMING_LANGUAGE };

    /**
     * @param category not null
     * @return if the given category is supported by ASF (correctly formatted) or <code>null</code> if not found.
     * @see <a href="http://projects.apache.org/categories.html">http://projects.apache.org/categories.html</a>
     * @see #CATEGORIES
     */
    public static String getCategorySupportedByASF( String category )
    {
        for ( String category_ : CATEGORIES )
        {
            if ( category_.equalsIgnoreCase( category ) )
            {
                return category_;
            }
        }

        return null;
    }

    /**
     * @param programmingLanguage not null
     * @return the given programming language supported by ASF (correctly formatted) or <code>null</code> if not found.
     * @see <a href="http://projects.apache.org/languages.html">http://projects.apache.org/languages.html</a>
     * @see #PROGRAMMING_LANGUAGES
     */
    public static String getProgrammingLanguageSupportedByASF( String programmingLanguage )
    {
        for ( String programmingLanguage_ : PROGRAMMING_LANGUAGES )
        {
            if ( programmingLanguage_.equalsIgnoreCase( programmingLanguage ) )
            {
                return programmingLanguage_;
            }
        }

        return null;
    }

    /**
     * Find the chair man of the project. The role of the developer should contain <code>chair</code>.
     *
     * @param developers list of <code>{@link Developer}</code>
     * @return a Developer or null if not found.
     */
    public static Developer findChair( List<Developer> developers )
    {
        if ( developers == null || developers.isEmpty() )
        {
            return null;
        }

        for ( Developer developer : developers )
        {
            List<String> roles = developer.getRoles();

            for ( String role : roles )
            {
                if ( role.toLowerCase().contains( "chair" ) )
                {
                    return developer;
                }
            }
        }

        return null;
    }

    /**
     * Find the list of PMC members of the project. The role of each developer should contain <code>pmc</code>.
     *
     * @param developers list of <code>{@link Developer}</code>
     * @return a not null list of Developer.
     */
    public static List<Developer> findPMCMembers( List<Developer> developers )
    {
        if ( developers == null || developers.isEmpty() )
        {
            return null;
        }

        List<Developer> pmcs = new ArrayList<Developer>();
        for ( Developer developer : developers )
        {
            List<String> roles = developer.getRoles();

            for ( String role : roles )
            {
                if ( role.toLowerCase().contains( "pmc" ) )
                {
                    pmcs.add( developer );
                }
            }
        }

        return pmcs;
    }

    /**
     * Try to find if the given project is hosted at Apache.
     *
     * @param project not null
     * @return <code>true</code> if the SCM url, distribution management url, project url or organization url is hosted
     *         in the Apache domain name, <code>false</code> otherwise.
     * @see #APACHE_DOMAIN_NAME
     * @since 1.1
     */
    public static boolean isASFProject( MavenProject project )
    {
        if ( project == null )
        {
            throw new IllegalArgumentException( "project is required" );
        }

        // check organization name
        if ( project.getOrganization() != null && StringUtils.isNotEmpty( project.getOrganization().getName() )
            && project.getOrganization().getName().trim().equals( "The Apache Software Foundation" ) ) // see
                                                                                                       // org.apache:apache
                                                                                                       // artifact
        {
            return true;
        }

        // check domain name
        if ( project.getOrganization() != null && isHostedAtASF( project.getOrganization().getUrl() ) )
        {
            return true;
        }

        if ( isHostedAtASF( project.getUrl() ) )
        {
            return true;
        }

        if ( project.getScm() != null )
        {
            if ( StringUtils.isNotEmpty( project.getScm().getUrl() )
                && project.getScm().getUrl().contains( APACHE_DOMAIN_NAME ) )
            {
                return true;
            }

            if ( StringUtils.isNotEmpty( project.getScm().getConnection() )
                && project.getScm().getConnection().contains( APACHE_DOMAIN_NAME ) )
            {
                return true;
            }

            if ( StringUtils.isNotEmpty( project.getScm().getDeveloperConnection() )
                && project.getScm().getDeveloperConnection().contains( APACHE_DOMAIN_NAME ) )
            {
                return true;
            }
        }

        if ( project.getDistributionManagement() != null )
        {
            if ( isHostedAtASF( project.getDistributionManagement().getDownloadUrl() ) )
            {
                return true;
            }

            if ( project.getDistributionManagement().getRepository() != null
                && isHostedAtASF( project.getDistributionManagement().getRepository().getUrl() ) )
            {
                return true;
            }

            if ( project.getDistributionManagement().getSnapshotRepository() != null
                && isHostedAtASF( project.getDistributionManagement().getSnapshotRepository().getUrl() ) )
            {
                return true;
            }

            if ( project.getDistributionManagement().getSite() != null
                && isHostedAtASF( project.getDistributionManagement().getSite().getUrl() ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * @param str an url could be null
     * @return <code>true</code> if the str is hosted by ASF.
     * @see #APACHE_DOMAIN_NAME
     */
    private static boolean isHostedAtASF( String str )
    {
        if ( StringUtils.isEmpty( str ) )
        {
            return false;
        }

        str = str.trim();
        try
        {
            URL url = new URL( str );
            if ( url.getHost().endsWith( APACHE_DOMAIN_NAME ) )
            {
                return true;
            }
        }
        catch ( MalformedURLException e )
        {
        }

        return false;
    }
}
