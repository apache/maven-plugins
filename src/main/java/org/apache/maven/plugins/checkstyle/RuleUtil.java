package org.apache.maven.plugins.checkstyle;

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

import com.puppycrawl.tools.checkstyle.api.AuditEvent;

/**
 * Tooling for Checkstyle rules conventions: names, categories.
 *
 * @author Hervé Boutemy
 * @since 2.13
 */
public final class RuleUtil
{
    private RuleUtil()
    {
        // hide utility class constructor
    }
    
    private static final String CHECKSTYLE_PACKAGE = "com.puppycrawl.tools.checkstyle.checks";

    /**
     * Get the rule name from an audit event.
     *
     * @param event the audit event
     * @return the rule name, which is the class name without package and removed eventual "Check" suffix
     */
    public static String getName( AuditEvent event )
    {
        return getName( event.getSourceName() );
    }
    /**
     * Get the rule name from an audit event source name.
     *
     * @param eventSrcName the audit event source name
     * @return the rule name, which is the class name without package and removed eventual "Check" suffix
     */
    public static String getName( String eventSrcName )
    {
        if ( eventSrcName == null )
        {
            return null;
        }

        if ( eventSrcName.endsWith( "Check" ) )
        {
            eventSrcName = eventSrcName.substring( 0, eventSrcName.length() - 5 );
        }

        return eventSrcName.substring( eventSrcName.lastIndexOf( '.' ) + 1 );
    }

    /**
     * Get the rule category from an audit event.
     *
     * @param event the audit event
     * @return the rule category, which is the last package name or "misc" or "extension"
     */
    public static String getCategory( AuditEvent event )
    {
        return getCategory( event.getSourceName() );
    }

    /**
     * Get the rule category from an audit event source name.
     *
     * @param eventSrcName the audit event source name
     * @return the rule category, which is the last package name or "misc" or "extension"
     */
    public static String getCategory( String eventSrcName )
    {
        if ( eventSrcName == null )
        {
            return null;
        }

        int end = eventSrcName.lastIndexOf( '.' );
        eventSrcName = eventSrcName.substring( 0,  end );

        if ( CHECKSTYLE_PACKAGE.equals( eventSrcName ) )
        {
            return "misc";
        }
        else if ( !eventSrcName.startsWith( CHECKSTYLE_PACKAGE ) )
        {
            return "extension";
        }

        return eventSrcName.substring( eventSrcName.lastIndexOf( '.' ) + 1 );
    }

    public static Matcher[] parseMatchers( String[] specs )
    {
        Matcher[] matchers = new Matcher[specs.length];
        int i = 0;
        for ( String spec: specs )
        {
            spec = spec.trim();
            Matcher matcher;
            if ( Character.isUpperCase( spec.charAt( 0 ) ) )
            {
                // spec starting with uppercase is a rule name
                matcher = new RuleMatcher( spec );
            }
            else if ( "misc".equals( spec ) )
            {
                // "misc" is a special case
                matcher = new PackageMatcher( CHECKSTYLE_PACKAGE );
            }
            else if ( "extension".equals( spec ) )
            {
                // "extension" is a special case
                matcher = new ExtensionMatcher();
            }
            else if ( !spec.contains( "." ) )
            {
                matcher = new PackageMatcher( CHECKSTYLE_PACKAGE + '.' + spec );
            }
            else
            {
                // by default, spec is a package name
                matcher = new PackageMatcher( spec );
            }
            matchers[i++] = matcher;
        }
        return matchers;
    }

    /**
     * Audit event source name matcher.
     */
    public interface Matcher
    {
        /**
         * Does the event source name match?
         * @param eventSrcName the event source name
         * @return boolean
         */
        boolean match( String eventSrcName );
    }

    private static class RuleMatcher
        implements Matcher
    {
        private final String rule;

        public RuleMatcher( String rule )
        {
            this.rule = rule;
        }

        public boolean match( String eventSrcName )
        {
            return rule.equals( getName( eventSrcName ) );
        }
    }

    private static class PackageMatcher
        implements Matcher
    {
        private final String packageName;

        public PackageMatcher( String packageName )
        {
            this.packageName = packageName;
        }

        public boolean match( String eventSrcName )
        {
            return eventSrcName.startsWith( packageName )
                && !eventSrcName.substring( packageName.length() + 1 ).contains( "." );
        }
    }

    /**
     * An extension does not start with Checkstyle package.
     */
    private static class ExtensionMatcher
        implements Matcher
    {
        public boolean match( String eventSrcName )
        {
            return !eventSrcName.startsWith( CHECKSTYLE_PACKAGE );
        }
    }
}
