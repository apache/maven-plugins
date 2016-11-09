package org.apache.maven.plugins.assembly.interpolation;

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

import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.io.DefaultAssemblyReader;
import org.apache.maven.plugins.assembly.utils.InterpolationConstants;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.interpolation.fixed.InterpolationState;

import java.io.File;

/**
 *
 */
public class AssemblyExpressionEvaluator
    implements ExpressionEvaluator
{

    private final AssemblerConfigurationSource configSource;

    private final FixedStringSearchInterpolator interpolator;

    private final PrefixAwareRecursionInterceptor interceptor;

    public AssemblyExpressionEvaluator( AssemblerConfigurationSource configSource )
    {
        this.configSource = configSource;

        final MavenProject project = configSource.getProject();
        final FixedStringSearchInterpolator projectInterpolator =
            DefaultAssemblyReader.createProjectInterpolator( project );
        interpolator = AssemblyInterpolator.fullInterpolator( project, projectInterpolator, configSource );
        interceptor = new PrefixAwareRecursionInterceptor( InterpolationConstants.PROJECT_PREFIXES, true );
    }

    @Override
    public File alignToBaseDirectory( File f )
    {
        String basePath = configSource.getBasedir().getAbsolutePath();
        String path = f.getPath();

        if ( !f.isAbsolute() && !path.startsWith( basePath ) )
        {
            return new File( configSource.getBasedir(), path );
        }
        else
        {
            return f;
        }
    }

    @Override
    public Object evaluate( String expression )
        throws ExpressionEvaluationException
    {
        InterpolationState is = new InterpolationState();
        is.setRecursionInterceptor( interceptor );
        return interpolator.interpolate( expression, is );
    }

}
