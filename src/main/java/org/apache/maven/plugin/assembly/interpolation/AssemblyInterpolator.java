package org.apache.maven.plugin.assembly.interpolation;

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

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.model.io.xpp3.AssemblyXpp3Reader;
import org.apache.maven.plugin.assembly.model.io.xpp3.ComponentXpp3Reader;
import org.apache.maven.plugin.assembly.resolved.AssemblyId;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.interpolation.fixed.InterpolationState;
import org.codehaus.plexus.interpolation.object.FieldBasedObjectInterpolator;
import org.codehaus.plexus.logging.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @version $Id$
 */
public class AssemblyInterpolator
{
    private static final Set<String> INTERPOLATION_BLACKLIST;

    static
    {
        final Set<String> blacklist = new HashSet<String>();

        blacklist.add( "outputFileNameMapping" );
        blacklist.add( "outputDirectoryMapping" );
        blacklist.add( "outputDirectory" );

        INTERPOLATION_BLACKLIST = blacklist;
    }

    public AssemblyInterpolator()
        throws IOException
    {
    }

    public static AssemblyXpp3Reader.ContentTransformer assemblyInterpolator(
        final FixedStringSearchInterpolator interpolator, final InterpolationState is, final Logger logger )
    {
        @SuppressWarnings( "unchecked" ) final Set<String> blacklistFields =
            new HashSet<String>( FieldBasedObjectInterpolator.DEFAULT_BLACKLISTED_FIELD_NAMES );
        blacklistFields.addAll( INTERPOLATION_BLACKLIST );

        return new AssemblyXpp3Reader.ContentTransformer()
        {
            public String transform( String source, String contextDescription )
            {
                if ( blacklistFields.contains( contextDescription ) )
                {
                    return source;
                }

                String interpolated = interpolator.interpolate( source, is );
                if ( !source.equals( interpolated ) && logger.isDebugEnabled())
                {
                   logger.debug(
                       "Field" + contextDescription + " value: " + source + " interpolated to: " + interpolated );
                }
                return interpolated;
            }
        };
    }

    public static ComponentXpp3Reader.ContentTransformer componentInterpolator(
        final FixedStringSearchInterpolator interpolator, final InterpolationState is, final Logger logger )
    {
        @SuppressWarnings( "unchecked" ) final Set<String> blacklistFields =
            new HashSet<String>( FieldBasedObjectInterpolator.DEFAULT_BLACKLISTED_FIELD_NAMES );
        blacklistFields.addAll( INTERPOLATION_BLACKLIST );

        return new ComponentXpp3Reader.ContentTransformer()
        {
            public String transform( String source, String contextDescription )
            {
                if ( blacklistFields.contains( contextDescription ) )
                {
                    return source;
                }

                String interpolated = interpolator.interpolate( source, is );
                if ( !source.equals( interpolated ) )
                {
                    logger.debug(
                        "Field" + contextDescription + " value: " + source + " interpolated to: " + interpolated );
                }
                return interpolated;
            }
        };
    }


    public static void checkErrors( AssemblyId assemblyId, InterpolationState interpolationState, Logger logger )
    {
        if ( interpolationState.asList() != null && interpolationState.asList().size() > 0 && logger.isDebugEnabled() )
        {
            final StringBuilder sb = new StringBuilder();

            sb.append( "One or more minor errors occurred while interpolating the assembly with ID: " ).append(
                assemblyId ).append( ":\n" );

            @SuppressWarnings( "unchecked" ) final List<Object> warnings = interpolationState.asList();
            for ( final Object warning : warnings )
            {
                sb.append( '\n' ).append( warning );
            }

            sb.append( "\n\nThese values were SKIPPED, but the assembly process will continue.\n" );

            logger.debug( sb.toString() );
        }
    }

    public static FixedStringSearchInterpolator fullInterpolator( final MavenProject project,
                                                                  @Nonnull FixedStringSearchInterpolator projectIp,
                                                                  final AssemblerConfigurationSource configSource )
    {
        FixedStringSearchInterpolator fixedStringSearchInterpolator =
            FixedStringSearchInterpolator.create( configSource.getRepositoryInterpolator(),
                                                  configSource.getCommandLinePropsInterpolator(),
                                                  configSource.getEnvInterpolator(), projectIp );
        return fixedStringSearchInterpolator.withPostProcessor(
            new PathTranslatingPostProcessor( project.getBasedir() ) );

    }

    private static final class PathTranslatingPostProcessor
        implements InterpolationPostProcessor
    {

        private final File basedir;

        public PathTranslatingPostProcessor( final File basedir )
        {
            this.basedir = basedir;
        }

        public Object execute( final String expression, final Object value )
        {
            final String path = String.valueOf( value );
            return AssemblyFileUtils.makePathRelativeTo( path, basedir );
        }

    }
}
