package org.apache.maven.plugin.assembly.utils;

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

import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.interpolation.ValueSource;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;

/**
 * @version $Id$
 */
public class PrefixedObjectBasedValueSource
    implements ValueSource
{

    private String prefix;
    private final Object root;
    private final Logger logger;

    public PrefixedObjectBasedValueSource( String prefix, Object root )
    {
        this.prefix = prefix;
        this.root = root;
        logger = null;
    }

    public PrefixedObjectBasedValueSource( String prefix, Object root, Logger logger )
    {
        this.prefix = prefix;
        this.root = root;
        this.logger = logger;
    }

    public Object getValue( String expression )
    {
        if ( ( expression == null ) || !expression.startsWith( prefix ) )
        {
            return null;
        }

        String realExpr = expression.substring( prefix.length() );
        if ( realExpr.startsWith( "." ) )
        {
            realExpr = realExpr.substring( 1 );
        }

        Object value = null;
        try
        {
            value = ReflectionValueExtractor.evaluate( realExpr, root, false );
        }
        catch ( Exception e )
        {
            if ( ( logger != null ) && logger.isDebugEnabled() )
            {
                logger.debug( "Failed to extract \'" + realExpr + "\' from: " + root + " (full expression was: \'" + expression + "\').", e );
            }
        }

        return value;
    }

}
