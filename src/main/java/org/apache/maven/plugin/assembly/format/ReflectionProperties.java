package org.apache.maven.plugin.assembly.format;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;

import java.util.Properties;

/**
 * @author Andreas Hoheneder (ahoh_at_inode.at)
 * @version $Id$
 * 
 * @depcrecated
 */
@Deprecated
public class ReflectionProperties
    extends Properties
{

    private static final long serialVersionUID = 1L;

    private final MavenProject project;

    boolean escapedBackslashesInFilePath;

    public ReflectionProperties( final MavenProject aProject, final boolean escapedBackslashesInFilePath )
    {
        super();

        project = aProject;

        this.escapedBackslashesInFilePath = escapedBackslashesInFilePath;
    }

    @Override
    public Object get( final Object key )
    {
        Object value = null;
        try
        {
            value = ReflectionValueExtractor.evaluate( "" + key, project );

            if ( escapedBackslashesInFilePath && value != null && "java.lang.String".equals( value.getClass()
                                                                                                  .getName() ) )
            {
                final String val = (String) value;

                // Check if it's a windows path
                if ( val.indexOf( ":\\" ) == 1 )
                {
                    value = StringUtils.replace( (String) value, "\\", "\\\\" );
                    value = StringUtils.replace( (String) value, ":", "\\:" );
                }
            }
        }
        catch ( final Exception e )
        {
            // TODO: remove the try-catch block when ReflectionValueExtractor.evaluate() throws no more exceptions
        }
        return value;
    }
}
