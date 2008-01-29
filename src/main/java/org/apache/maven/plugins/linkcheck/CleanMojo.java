package org.apache.maven.plugins.linkcheck;

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

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Clean <code>Linkcheck</code> generated files by the report.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 1.0
 * @goal clean
 */
public class CleanMojo
    extends LinkcheckReport
{
    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        File cache = new File( linkcheckCache );
        if ( cache.isFile() && cache.exists() )
        {
            cache.delete();

            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "The file '" + cache.getAbsolutePath() + "' has been deleted." );
            }
        }

        File output = new File( linkcheckOutput );
        if ( output.isFile() && output.exists() )
        {
            output.delete();

            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "The file '" + output.getAbsolutePath() + "' has been deleted." );
            }
        }
    }
}
