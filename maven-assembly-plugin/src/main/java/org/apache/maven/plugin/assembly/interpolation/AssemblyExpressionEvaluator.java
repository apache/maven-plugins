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

import java.io.File;

import org.apache.maven.plugin.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugin.assembly.utils.InterpolationConstants;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;

public class AssemblyExpressionEvaluator
    implements ExpressionEvaluator
{
    
    private final AssemblerConfigurationSource configSource;
    private Interpolator interpolator;
    private PrefixAwareRecursionInterceptor interceptor;

    public AssemblyExpressionEvaluator( AssemblerConfigurationSource configSource )
    {
        this.configSource = configSource;
        
        interpolator = AssemblyInterpolator.buildInterpolator( configSource.getProject(), configSource );
        interceptor = new PrefixAwareRecursionInterceptor( InterpolationConstants.PROJECT_PREFIXES, true );
    }

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

    public Object evaluate( String expression )
        throws ExpressionEvaluationException
    {
        try
        {
            return interpolator.interpolate( expression, interceptor );
        }
        catch ( InterpolationException e )
        {
            throw new ExpressionEvaluationException( "Interpolation failed for archiver expression: " + expression, e );
        }
    }

}
