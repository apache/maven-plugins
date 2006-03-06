package org.apache.maven.plugin.assembly.interpolation;

import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.io.xpp3.AssemblyXpp3Reader;
import org.apache.maven.plugins.assembly.model.io.xpp3.AssemblyXpp3Writer;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssemblyInterpolator
    extends AbstractLogEnabled
{
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile( "\\$\\{(pom\\.|project\\.|env\\.)?([^}]+)\\}" );
    
    private Properties envars;
    
    public AssemblyInterpolator( Properties envars )
    {
        this.envars = envars;
    }

    public AssemblyInterpolator()
        throws IOException
    {
        envars = CommandLineUtils.getSystemEnvVars();
    }
      
    public Assembly interpolate( Assembly assembly, Model model, Map context )
        throws AssemblyInterpolationException
    {
        return interpolate( assembly, model, context, true );
    }    
    
    public Assembly interpolate( Assembly assembly, Model model, Map context, boolean strict )
        throws AssemblyInterpolationException
    {
        StringWriter sWriter = new StringWriter();
        
        AssemblyXpp3Writer writer = new AssemblyXpp3Writer();
        
        try
        {
            writer.write( sWriter, assembly );
        }
        catch ( IOException e )
        {
            throw new AssemblyInterpolationException ( "Cannot serialize assembly descriptor for interpolation.", e );
        }
        
        String serializedAssembly = sWriter.toString();
        
        serializedAssembly = interpolateInternal( serializedAssembly, assembly, model, context );

        StringReader sReader = new StringReader( serializedAssembly );

        AssemblyXpp3Reader assemblyReader = new AssemblyXpp3Reader();
        try
        {
            assembly = assemblyReader.read( sReader );
        }
        catch ( IOException e )
        {
            throw new AssemblyInterpolationException(
                "Cannot read assembly descriptor from interpolating filter of serialized version.", e );
        }
        catch ( XmlPullParserException e )
        {
            throw new AssemblyInterpolationException(
                "Cannot read assembly descriptor from interpolating filter of serialized version.", e );
        }
        return assembly;        
    }
    
    private String interpolateInternal( String result, Assembly assembly, Model model, Map context )
        throws AssemblyInterpolationException
    {
        Matcher matcher = EXPRESSION_PATTERN.matcher( result );
        while ( matcher.find() )
        {
            String wholeExpr = matcher.group( 0 );
            String realExpr = parseExpression( wholeExpr );
            
            Object value = context.get( realExpr );

            if ( value == null )
            {
                value = model.getProperties().getProperty( realExpr );
            }
            
            if ( value == null )
            {
                try
                {
                    value = ReflectionValueExtractor.evaluate( realExpr, model );
                }
                catch ( Exception e )
                {
                    Logger logger = getLogger();
                    if ( logger != null )
                    {
                        logger.debug( "Assembly descriptor interpolation cannot proceed with expression: " + wholeExpr + ". Skipping...", e );
                    }
                }
            }
    
            if ( value == null )
            {
                value = envars.getProperty( realExpr );
            }
    
            // if the expression refers to itself, skip it.
            if ( wholeExpr.equals( value ) )
            {
                throw new AssemblyInterpolationException( wholeExpr, assembly.getId() + " references itself." );
            }
    
            if ( value != null )
            {
                result = StringUtils.replace( result, wholeExpr, String.valueOf( value ) );
                // could use:
                // result = matcher.replaceFirst( stringValue );
                // but this could result in multiple lookups of stringValue, and replaceAll is not correct behaviour
                matcher.reset( result );
            }
        }
        return result;
    }
    
    private String parseExpression( String expression )
    {
        int startIndex = expression.indexOf( "{" );
        int endIndex = expression.indexOf( "}" );
        
        expression = expression.substring( startIndex + 1, endIndex );
        
        return expression;
    }
}
