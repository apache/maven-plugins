package org.apache.maven.plugins.release.scm;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.apache.maven.plugins.release.ReleaseFailureException;

import java.util.Iterator;
import java.util.List;

/**
 * Exception occurring during an SCM repository operation.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ReleaseScmRepositoryException
    extends ReleaseFailureException
{
    public ReleaseScmRepositoryException( String message, List validationMessages )
    {
        super( message + listValidationMessages( validationMessages ) );
    }

    private static String listValidationMessages( List messages )
    {
        StringBuffer buffer = new StringBuffer();

        if ( messages != null )
        {
            Iterator iter = messages.iterator();

            while ( iter.hasNext() )
            {
                buffer.append( iter.next().toString() );
                buffer.append( "\n" );
            }
        }

        return buffer.toString();
    }
}
