package org.apache.maven.plugin.assembly.interpolation;

import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.io.xpp3.AssemblyXpp3Reader;
import org.apache.maven.plugins.assembly.model.io.xpp3.AssemblyXpp3Writer;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssemblyInterpolator
    extends AbstractLogEnabled
{
    private static final Pattern ELEMENT_PATTERN = Pattern.compile( "\\<([^> ]+)[^>]*>([^<]+)" );
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile( "\\$\\{(pom\\.|project\\.|env\\.)?([^}]+)\\}" );
    
    private static final Map INTERPOLATION_BLACKLIST;
    
    static
    {
        Map blacklist = new HashMap();
        
        List ofnmBlacklistings = new ArrayList();
        
        ofnmBlacklistings.add( "groupId" );
        ofnmBlacklistings.add( "artifactId" );
        ofnmBlacklistings.add( "version" );
        
        blacklist.put( "outputFileNameMapping", ofnmBlacklistings );
        
        INTERPOLATION_BLACKLIST = blacklist;
    }

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
            throw new AssemblyInterpolationException( "Cannot serialize assembly descriptor for interpolation.", e );
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

    private String interpolateInternal( String src, Assembly assembly, Model model, Map context )
        throws AssemblyInterpolationException
    {
        String result = src;
        
        Matcher elementMatcher = ELEMENT_PATTERN.matcher( result );
        
        while( elementMatcher.find() )
        {
            String element = elementMatcher.group( 0 );
            
            String elementName = elementMatcher.group( 1 );
            String value = elementMatcher.group( 2 );
            
            // only attempt to interpolate if the following is met:
            // 1. the element is not in the interpolation blacklist.
            // 2. the value is not empty (otherwise there's nothing to interpolate)
            // 3. the value contains a "${" (a pretty good clue that there's an expression in it)
            if ( StringUtils.isNotEmpty( value ) && value.indexOf( "${" ) > -1 )
            {
                List blacklistedExpressions = (List) INTERPOLATION_BLACKLIST.get( elementName );
                if ( blacklistedExpressions == null )
                {
                    blacklistedExpressions = Collections.EMPTY_LIST;
                }
                
                String interpolatedValue = interpolateElementValue( value, assembly, model, context, blacklistedExpressions );
                
                String modifiedElement = StringUtils.replace( element, value, interpolatedValue );
                result = StringUtils.replace( result, element, modifiedElement );
            }
        }
        
        return result;
    }
    
    private String interpolateElementValue( String src, Assembly assembly, Model model, Map context, List blacklistedExpressions ) 
        throws AssemblyInterpolationException
    {
        String result = src;
        
        Matcher matcher = EXPRESSION_PATTERN.matcher( result );
        while ( matcher.find() )
        {
            String wholeExpr = matcher.group( 0 );
            String realExpr = matcher.group( 2 );
            
            if ( blacklistedExpressions.contains( realExpr ) )
            {
                continue;
            }

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
                        logger.debug( "Assembly descriptor interpolation cannot proceed with expression: " + wholeExpr +
                            ". Skipping...", e );
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
}
