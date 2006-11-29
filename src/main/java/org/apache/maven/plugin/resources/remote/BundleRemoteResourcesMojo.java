package org.apache.maven.plugin.resources.remote;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.resources.remote.io.xpp3.RemoteResourcesBundleXpp3Writer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.util.List;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.io.FileWriter;

/**
 * Pull down artifacts containing remote resources and process the resources contained
 * inside the artifact.
 *
 * @goal bundle
 * @phase generate-resources
 */
public class BundleRemoteResourcesMojo
    extends AbstractMojo
{
    public static final String RESOURCES_MANIFEST = "META-INF/maven/remote-resources.xml";

    /**
     * The directory which contains the resources you want packaged up in this resource bundle.
     *
     * @parameter expression="${basedir}/src/main/resources"
     */
    private File resourcesDirectory;

    /**
     * The directory where you want the resource bundle manifest written to.
     * 
     * @parameter expression="${project.build.outputDirectory}"
     */
    private File outputDirectory;

    public void execute()
        throws MojoExecutionException
    {
        if ( !resourcesDirectory.exists() )
        {
            return;
        }        
        
        // Look at the content of ${basedir}/src/main/resources and create a manifest of the files
        // so that velocity can easily process any resources inside the JAR that need to be processed.

        RemoteResourcesBundle remoteResourcesBundle = new RemoteResourcesBundle();

        try
        {
            List resources = FileUtils.getFileNames( resourcesDirectory, "**/*.txt,**/*.vm", "**/.svn/**", false );

            for ( Iterator i = resources.iterator(); i.hasNext(); )
            {
                String resource = (String) i.next();

                remoteResourcesBundle.addRemoteResource( StringUtils.replace( resource, '\\', '/' ) );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error scanning resources.", e );
        }

        RemoteResourcesBundleXpp3Writer w = new RemoteResourcesBundleXpp3Writer();

        try
        {
            File f = new File( outputDirectory, RESOURCES_MANIFEST );

            FileUtils.mkdir( f.getParentFile().getAbsolutePath() );

            Writer writer = new FileWriter( f );

            w.write( writer, remoteResourcesBundle );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating remote resources manifest.", e );
        }
    }
}
