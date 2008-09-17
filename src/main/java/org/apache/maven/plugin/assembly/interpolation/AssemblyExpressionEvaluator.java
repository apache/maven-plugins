package org.apache.maven.plugin.assembly.interpolation;

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
