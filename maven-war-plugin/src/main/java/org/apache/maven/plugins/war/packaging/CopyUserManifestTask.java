package org.apache.maven.plugins.war.packaging;

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
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

/**
 * @author Haikal Saadh
 *
 */
public class CopyUserManifestTask
    extends AbstractWarPackagingTask
{

    /** Instance logger */
    private Log log;

    public Log getLog()
    {
        if ( log == null )
        {
            log = new SystemStreamLog();
        }
        return log;
    }

    public void setLog( Log log )
    {
        this.log = log;
    }

    public void performPackaging( WarPackagingContext context )
        throws MojoExecutionException, MojoFailureException
    {
        File userManifest = context.getArchive().getManifestFile();
        if ( userManifest != null )
        {

            try
            {
                getLog().info( "Copying manifest..." );
                File metainfDir = new File( context.getWebappDirectory(), META_INF_PATH );
                copyFile( context, userManifest, new File( metainfDir, "MANIFEST.MF" ), "META-INF/MANIFEST.MF", true );

            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error copying user manifest", e );
            }
        }

    }

}
