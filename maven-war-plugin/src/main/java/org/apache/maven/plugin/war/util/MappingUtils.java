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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;

/**
 * Utilities used to evaluate expression.
 * <p/>
 * TODO: this comes from the assembly plugin; refactor when it's shared.
 * <p/>
 * The expression might use any field of the {@link Artifact} interface. Some
 * examples might be:
 * <ul>
 * <li>@{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@</li>
 * <li>@{artifactId}@-@{version}@.@{extension}@</li>
 * <li>@{artifactId}@.@{extension}@</li>
 * </ul>
 *
 * @author Stephane Nicoll
 * @version $Id$
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
        throws InterpolationException
    {
        String value = expression;

        // FIXME: This is BAD! Accessors SHOULD NOT change the behavior of the object.
        artifact.isSnapshot();

        RegexBasedInterpolator interpolator = new RegexBasedInterpolator( "\\@\\{(", ")?([^}]+)\\}@" );
        interpolator.addValueSource( new ObjectBasedValueSource( artifact ) );
        interpolator.addValueSource( new ObjectBasedValueSource( artifact.getArtifactHandler() ) );

        Properties classifierMask = new Properties();
        classifierMask.setProperty( "classifier", "" );

        // Support for special expressions, like @{dashClassifier?}@, see MWAR-212
        String classifier = artifact.getClassifier();
        if ( classifier != null )
        {
            classifierMask.setProperty( "dashClassifier?", "-" + classifier );
            classifierMask.setProperty( "dashClassifier", "-" + classifier );
        }
        else
        {
            classifierMask.setProperty( "dashClassifier?", "" );
            classifierMask.setProperty( "dashClassifier", "" );
        }

        interpolator.addValueSource( new PropertiesBasedValueSource ( classifierMask ) );

        value = interpolator.interpolate( value, "__artifact" );

        return value;
    }


    /**
     * Internal implementation of {@link ValueSource}
     */
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

        public void clearFeedback()
        {
            // nothing here
            
        }

        public List getFeedback()
        {
            // nothing here just NPE free
            return Collections.EMPTY_LIST;
        }

    }

}
