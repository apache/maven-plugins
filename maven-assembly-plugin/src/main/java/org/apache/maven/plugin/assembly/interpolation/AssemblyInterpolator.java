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
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.utils.AssemblyFileUtils;
import org.apache.maven.plugin.assembly.utils.InterpolationConstants;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.interpolation.object.FieldBasedObjectInterpolator;
import org.codehaus.plexus.interpolation.object.ObjectInterpolationWarning;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.cli.CommandLineUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @version $Id$
 */
public class AssemblyInterpolator
    extends AbstractLogEnabled
{
    private static final Set<String> INTERPOLATION_BLACKLIST;

    private static final Properties ENVIRONMENT_VARIABLES;

    static
    {
        final Set<String> blacklist = new HashSet<String>();

        blacklist.add( "outputFileNameMapping" );
        blacklist.add( "outputDirectoryMapping" );
        blacklist.add( "outputDirectory" );

        INTERPOLATION_BLACKLIST = blacklist;

        Properties environmentVariables;
        try
        {
            environmentVariables = CommandLineUtils.getSystemEnvVars( false );
        }
        catch ( final IOException e )
        {
            environmentVariables = new Properties();
        }

        ENVIRONMENT_VARIABLES = environmentVariables;
    }

    public AssemblyInterpolator()
        throws IOException
    {
    }

    public Assembly interpolate( final Assembly assembly, final MavenProject project,
                                 final AssemblerConfigurationSource configSource, FixedStringSearchInterpolator projectInterpolator )
        throws AssemblyInterpolationException
    {
        @SuppressWarnings( "unchecked" )
        final Set<String> blacklistFields =
            new HashSet<String>( FieldBasedObjectInterpolator.DEFAULT_BLACKLISTED_FIELD_NAMES );
        blacklistFields.addAll( INTERPOLATION_BLACKLIST );

        @SuppressWarnings( "unchecked" )
        final Set<String> blacklistPkgs = FieldBasedObjectInterpolator.DEFAULT_BLACKLISTED_PACKAGE_PREFIXES;

        final FieldBasedObjectInterpolator objectInterpolator =
            new FieldBasedObjectInterpolator( blacklistFields, blacklistPkgs );

        final FixedStringSearchInterpolator interpolator = buildInterpolator( project, projectInterpolator, configSource );

        // TODO: Will this adequately detect cycles between prefixed property references and prefixed project
        // references??
        final RecursionInterceptor interceptor =
            new PrefixAwareRecursionInterceptor( InterpolationConstants.PROJECT_PREFIXES, true );

        try
        {
            objectInterpolator.interpolate( assembly, interpolator.asBasicInterpolator(), interceptor );
        }
        catch ( final InterpolationException e )
        {
            throw new AssemblyInterpolationException( "Failed to interpolate assembly with ID: " + assembly.getId()
                + ". Reason: " + e.getMessage(), e );
        }

        if ( objectInterpolator.hasWarnings() && getLogger().isDebugEnabled() )
        {
            final StringBuilder sb = new StringBuilder();

            sb.append( "One or more minor errors occurred while interpolating the assembly with ID: " ).append( assembly.getId() ).append( ":\n" );

            @SuppressWarnings( "unchecked" )
            final List<ObjectInterpolationWarning> warnings = objectInterpolator.getWarnings();
            for ( final ObjectInterpolationWarning warning : warnings )
            {
                sb.append( '\n' ).append( warning );
            }

            sb.append( "\n\nThese values were SKIPPED, but the assembly process will continue.\n" );

            getLogger().debug( sb.toString() );
        }

        return assembly;
    }

    public static FixedStringSearchInterpolator buildInterpolator( final MavenProject project, @Nonnull FixedStringSearchInterpolator projectIp,
                                                  final AssemblerConfigurationSource configSource )
    {
        return FixedStringSearchInterpolator
            .create( configSource.getRepositoryInterpolator(), configSource.getCommandLinePropsInterpolator(), configSource.getEnvInterpolator(),  projectIp )
            .withPostProcessor( new PathTranslatingPostProcessor( project.getBasedir() ) );
    }

    @Override
    protected Logger getLogger()
    {
        Logger logger = super.getLogger();

        if ( logger == null )
        {
            logger = new ConsoleLogger( Logger.LEVEL_INFO, "interpolator-internal" );

            enableLogging( logger );
        }

        return logger;
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
