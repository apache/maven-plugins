package org.apache.maven.plugin.war;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.util.interpolation.ValueSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utiities used to eveluate expression.
 * <p/>
 * TODO: this comes from the assembly plugin; refactor when it's shared.
 * <p/>
 * The expression might use any fied of the {@link Artifact} interface. Some
 * examples might be:
 * <ul>
 * <li>${artifactId}-${version}.${extension}</li>
 * <li>${artifactId}.${extension}</li>
 * </ul>
 *
 * @author Stephane Nicoll
 */
class MappingUtils
{

    /**
     * Evaluates the specified expression for the given artifact.
     *
     * @param expression the expression to evaluate
     * @param artifact   the artifact to use as value object for tokens
     * @return expression the evaluated expression
     */
    public static String evaluateFileNameMapping( String expression, Artifact artifact )

    {
        String value = expression;

        // FIXME: This is BAD! Accessors SHOULD NOT change the behavior of the object.
        artifact.isSnapshot();

        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

        interpolator.addValueSource( new ObjectBasedValueSource( artifact ) );
        interpolator.addValueSource( new ObjectBasedValueSource( artifact.getArtifactHandler() ) );

        Properties classifierMask = new Properties();
        classifierMask.setProperty( "classifier", "" );

        interpolator.addValueSource( new PropertiesInterpolationValueSource( classifierMask ) );

        value = interpolator.interpolate( value, "__artifact" );

        return value;
    }


    static class PropertiesInterpolationValueSource
        implements ValueSource
    {

        private final Properties properties;

        public PropertiesInterpolationValueSource( Properties properties )
        {
            this.properties = properties;
        }

        public Object getValue( String key )
        {
            return properties.getProperty( key );
        }

    }


    /**
     * This a copy of the class in plexus-utils 1.4
     * <p/>
     * TODO: remove this once the plugin can depend on plexus-utils 1.4
     */
    static class RegexBasedInterpolator
    {

        private List valueSources;

        public RegexBasedInterpolator()
        {
            valueSources = new ArrayList();
        }

        public RegexBasedInterpolator( List valueSources )
        {
            this.valueSources = new ArrayList( valueSources );
        }

        public void addValueSource( ValueSource valueSource )
        {
            this.valueSources.add( valueSource );
        }

        public void removeValuesSource( ValueSource valueSource )
        {
            this.valueSources.remove( valueSource );
        }

        public String interpolate( String input, String thisPrefixPattern )
        {
            String result = input;

            Pattern expressionPattern = Pattern.compile( "\\$\\{(" + thisPrefixPattern + ")?([^}]+)\\}" );
            Matcher matcher = expressionPattern.matcher( result );

            while ( matcher.find() )
            {
                String wholeExpr = matcher.group( 0 );
                String realExpr = matcher.group( 2 );

                if ( realExpr.startsWith( "." ) )
                {
                    realExpr = realExpr.substring( 1 );
                }

                Object value = null;
                for ( Iterator it = valueSources.iterator(); it.hasNext() && value == null; )
                {
                    ValueSource vs = (ValueSource) it.next();

                    value = vs.getValue( realExpr );
                }

                // if the expression refers to itself, die.
                if ( wholeExpr.equals( value ) )
                {
                    throw new IllegalArgumentException( "Expression: \'" + wholeExpr + "\' references itself." );
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

}
