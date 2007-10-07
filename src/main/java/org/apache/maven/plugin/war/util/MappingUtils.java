package org.apache.maven.plugin.war.util;

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
import org.codehaus.plexus.util.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.util.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.util.interpolation.ValueSource;

import java.util.Properties;

/**
 * Utilities used to evaluate expression.
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
public class MappingUtils
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

}
