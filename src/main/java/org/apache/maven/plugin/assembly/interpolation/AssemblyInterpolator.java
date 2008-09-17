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

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.model.Assembly;
import org.apache.maven.plugin.assembly.utils.CommandLineUtils;
import org.apache.maven.plugin.assembly.utils.InterpolationConstants;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PrefixedPropertiesValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.object.FieldBasedObjectInterpolator;
import org.codehaus.plexus.interpolation.object.ObjectInterpolationWarning;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

/**
 * @version $Id$
 */
public class AssemblyInterpolator
    extends AbstractLogEnabled
{
    private static final Set INTERPOLATION_BLACKLIST;

    private static final Properties ENVIRONMENT_VARIABLES;

    static
    {
        Set blacklist = new HashSet();

        blacklist.add( "outputFileNameMapping" );
        blacklist.add( "outputDirectoryMapping" );
        blacklist.add( "outputDirectory" );

        INTERPOLATION_BLACKLIST = blacklist;
        
        Properties environmentVariables;
        try
        {
            environmentVariables = CommandLineUtils.getSystemEnvVars( false );
        }
        catch ( IOException e )
        {
            environmentVariables = new Properties();
        }
        
        ENVIRONMENT_VARIABLES = environmentVariables;
    }

    public AssemblyInterpolator()
        throws IOException
    {
    }

    public Assembly interpolate( Assembly assembly, MavenProject project, AssemblerConfigurationSource configSource )
        throws AssemblyInterpolationException
    {
        Set blacklistFields = new HashSet( FieldBasedObjectInterpolator.DEFAULT_BLACKLISTED_FIELD_NAMES );
        blacklistFields.addAll( INTERPOLATION_BLACKLIST );

        Set blacklistPkgs = FieldBasedObjectInterpolator.DEFAULT_BLACKLISTED_PACKAGE_PREFIXES;

        FieldBasedObjectInterpolator objectInterpolator =
            new FieldBasedObjectInterpolator( blacklistFields, blacklistPkgs );
        Interpolator interpolator = buildInterpolator( project, configSource );

        // TODO: Will this adequately detect cycles between prefixed property references and prefixed project
        // references??
        RecursionInterceptor interceptor =
            new PrefixAwareRecursionInterceptor( InterpolationConstants.PROJECT_PREFIXES, true );

        try
        {
            objectInterpolator.interpolate( assembly, interpolator, interceptor );
        }
        catch ( InterpolationException e )
        {
            throw new AssemblyInterpolationException( "Failed to interpolate assembly with ID: " + assembly.getId()
                + ". Reason: " + e.getMessage(), e );
        }
        finally
        {
            interpolator.clearAnswers();
        }

        if ( objectInterpolator.hasWarnings() && getLogger().isDebugEnabled() )
        {
            StringBuffer sb = new StringBuffer();

            sb.append( "One or more minor errors occurred while interpolating the assembly with ID: "
                + assembly.getId() + ":\n" );

            List warnings = objectInterpolator.getWarnings();
            for ( Iterator it = warnings.iterator(); it.hasNext(); )
            {
                ObjectInterpolationWarning warning = (ObjectInterpolationWarning) it.next();

                sb.append( '\n' ).append( warning );
            }

            sb.append( "\n\nThese values were SKIPPED, but the assembly process will continue.\n" );

            getLogger().debug( sb.toString() );
        }

        return assembly;
    }

    public static Interpolator buildInterpolator( MavenProject project, AssemblerConfigurationSource configSource )
    {
        StringSearchInterpolator interpolator = new StringSearchInterpolator();
        interpolator.setCacheAnswers( true );

        MavenSession session = configSource.getMavenSession();

        if ( session != null )
        {
            Properties userProperties = null;
            try
            {
                userProperties = session.getExecutionProperties();
            }
            catch ( NoSuchMethodError nsmer )
            {
                // OK, so user is using Maven <= 2.0.8. No big deal.
            }

            if ( userProperties != null )
            {
                // 4
                interpolator.addValueSource( new PropertiesBasedValueSource( userProperties ) );
            }
        }

        interpolator.addValueSource( new PrefixedPropertiesValueSource(
                                                                        InterpolationConstants.PROJECT_PROPERTIES_PREFIXES,
                                                                        project.getProperties(), true ) );
        interpolator.addValueSource( new PrefixedObjectValueSource( InterpolationConstants.PROJECT_PREFIXES, project,
                                                                    true ) );

        Properties commandLineProperties = System.getProperties();
        try
        {
            if ( session != null )
            {
                commandLineProperties = session.getExecutionProperties();
            }

        }
        catch ( NoSuchMethodError nsmer )
        {
            // OK, so user is using Maven <= 2.0.8. No big deal.
        }

        // 7
        interpolator.addValueSource( new PropertiesBasedValueSource( commandLineProperties ) );
        interpolator.addValueSource( new PrefixedPropertiesValueSource( Collections.singletonList( "env." ), ENVIRONMENT_VARIABLES,
                                                                        true ) );

        return interpolator;
    }

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
}
