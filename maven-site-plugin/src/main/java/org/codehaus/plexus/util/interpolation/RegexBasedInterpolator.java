package org.codehaus.plexus.util.interpolation;

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

import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RegexBasedInterpolator
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
