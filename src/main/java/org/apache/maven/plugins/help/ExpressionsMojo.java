package org.apache.maven.plugins.help;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.usability.plugin.Expression;
import org.apache.maven.usability.plugin.ExpressionDocumentationException;
import org.apache.maven.usability.plugin.ExpressionDocumenter;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Displays the supported Plugin expressions used by Maven.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.1
 */
@Mojo( name = "expressions", requiresProject = false )
public class ExpressionsMojo
    extends AbstractHelpMojo
{
    /** English sentence when no description exists */
    private static final String NO_DESCRIPTION_AVAILABLE = "No description available.";

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        Map<String,Expression> m;
        try
        {
            m = ExpressionDocumenter.load();
        }
        catch ( ExpressionDocumentationException e )
        {
            throw new MojoExecutionException( "ExpressionDocumentationException: " + e.getMessage(), e );
        }

        StringBuilder sb = new StringBuilder();
        sb.append( "Maven supports the following Plugin expressions:\n\n" );
        for ( String expression : getExpressionsRoot() )
        {
            sb.append( "${" ).append( expression ).append( "}: " );
            sb.append( NO_DESCRIPTION_AVAILABLE );
            sb.append( "\n\n" );
        }

        for ( Map.Entry<String,Expression> entry : m.entrySet() )
        {
            String key = entry.getKey();
            Expression expression = entry.getValue();

            sb.append( "${" ).append( key ).append( "}: " );
            sb.append( trimCDATA( expression.getDescription() ) );
            sb.append( "\n\n" );
        }

        if ( output != null )
        {
            try
            {
                writeFile( output, sb );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot write plugins expressions description to output: "
                    + output, e );
            }

            if ( getLog().isInfoEnabled() )
            {
                getLog().info( "Wrote descriptions to: " + output );
            }
        }
        else
        {
            if ( getLog().isInfoEnabled() )
            {
                getLog().info( sb.toString() );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * @return the value of the private static field <code>ExpressionDocumenter#EXPRESSION_ROOTS</code>.
     * @throws MojoFailureException if any reflection exceptions occur
     * @throws MojoExecutionException if no value exists for <code>ExpressionDocumenter#EXPRESSION_ROOTS</code>
     */
    private static List<String> getExpressionsRoot()
        throws MojoFailureException, MojoExecutionException
    {
        try
        {
            Field f = ExpressionDocumenter.class.getDeclaredField( "EXPRESSION_ROOTS" );
            f.setAccessible( true );
            String[] value = (String[]) f.get( new ExpressionDocumenter() );
            if ( value == null )
            {
                throw new MojoExecutionException( "org.apache.maven.usability.plugin.ExpressionDocumenter#"
                    + "EXPRESSION_ROOTS has no value." );
            }

            return Arrays.asList( value );
        }
        catch ( SecurityException e )
        {
            throw new MojoFailureException( "SecurityException: " + e.getMessage() );
        }
        catch ( IllegalArgumentException e )
        {
            throw new MojoFailureException( "IllegalArgumentException: " + e.getMessage() );
        }
        catch ( NoSuchFieldException e )
        {
            throw new MojoFailureException( "NoSuchFieldException: " + e.getMessage() );
        }
        catch ( IllegalAccessException e )
        {
            throw new MojoFailureException( "IllegalAccessException: " + e.getMessage() );
        }
    }

    /**
     * @param description could be null
     * @return the given description without any new line, or <code>NO_DESCRIPTION_AVAILABLE</code> value if
     * <code>description</code> is null.
     * @see #NO_DESCRIPTION_AVAILABLE
     */
    private static String trimCDATA( String description )
    {
        if ( StringUtils.isEmpty( description ) )
        {
            return NO_DESCRIPTION_AVAILABLE;
        }

        StringBuilder sb = new StringBuilder();
        String[] lines = StringUtils.split( description, "\r\n" );
        for ( int i = 0; i < lines.length; i++ )
        {
            sb.append( StringUtils.trim( lines[i] ) ).append( " " );
        }

        return sb.toString();
    }
}
