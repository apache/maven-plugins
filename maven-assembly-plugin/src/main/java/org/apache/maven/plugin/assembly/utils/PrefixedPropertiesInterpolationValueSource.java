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

import java.util.Properties;

/**
 * @version $Id$
 */
public class PrefixedPropertiesInterpolationValueSource
    extends PropertiesInterpolationValueSource
{

    private final String prefix;

    public PrefixedPropertiesInterpolationValueSource( String prefix, Properties properties )
    {
        super( properties );
        this.prefix = prefix;
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

        return super.getValue( realExpr );
    }

}
