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
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

/**
 * Bundle up resources that should be considered as a remote-resource.
 */
@Mojo( name = "bundle", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true )
public class BundleRemoteResourcesMojo
    extends AbstractMojo
{
    public static final String RESOURCES_MANIFEST = "META-INF/maven/remote-resources.xml";

    private static final String[] DEFAULT_INCLUDES = new String[]{ "**/*.txt", "**/*.vm", };


    /**
     * The directory which contains the resources you want packaged up in this resource bundle.
     */
    @Parameter( defaultValue = "${basedir}/src/main/resources" )
    private File resourcesDirectory;

    /**
     * The directory where you want the resource bundle manifest written to.
     */
    @Parameter( defaultValue = "${project.build.outputDirectory}", required = true )
    private File outputDirectory;

    /**
     * A list of files to include. Can contain ant-style wildcards and double wildcards.
     * The default includes are
     * <code>**&#47;*.txt   **&#47;*.vm</code>
     *
     * @since 1.0-alpha-5
     */
    @Parameter
    private String[] includes;

    /**
     * A list of files to exclude. Can contain ant-style wildcards and double wildcards.
     *
     * @since 1.0-alpha-5
     */
    @Parameter
    private String[] excludes;

    /**
     * Encoding of the bundle.
     *
     * @since 1.1
     */
    @Parameter( defaultValue = "${project.build.sourceEncoding}" )
    private String sourceEncoding;

    public void execute()
        throws MojoExecutionException
    {
        if ( !resourcesDirectory.exists() )
        {
            getLog().info( "skip non existing resourceDirectory " + resourcesDirectory.getAbsolutePath() );
            return;
        }

        if ( StringUtils.isEmpty( sourceEncoding ) )
        {
            getLog().warn( "sourceEncoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                               + ", i.e. build is platform dependent!" );
            sourceEncoding = ReaderFactory.FILE_ENCODING;
        }

        // Look at the content of the resourcesDirectory and create a manifest of the files
        // so that velocity can easily process any resources inside the JAR that need to be processed.

        RemoteResourcesBundle remoteResourcesBundle = new RemoteResourcesBundle();
        remoteResourcesBundle.setSourceEncoding( sourceEncoding );

        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir( resourcesDirectory );
        if ( includes != null && includes.length != 0 )
        {
            scanner.setIncludes( includes );
        }
        else
        {
            scanner.setIncludes( DEFAULT_INCLUDES );
        }

        if ( excludes != null && excludes.length != 0 )
        {
            scanner.setExcludes( excludes );
        }

        scanner.addDefaultExcludes();
        scanner.scan();

        List<String> includedFiles = Arrays.asList( scanner.getIncludedFiles() );

        for ( String resource : includedFiles )
        {
            remoteResourcesBundle.addRemoteResource( StringUtils.replace( resource, '\\', '/' ) );
        }

        RemoteResourcesBundleXpp3Writer w = new RemoteResourcesBundleXpp3Writer();

        Writer writer = null;
        try
        {
            File f = new File( outputDirectory, RESOURCES_MANIFEST );

            FileUtils.mkdir( f.getParentFile()
                              .getAbsolutePath() );

            writer = new FileWriter( f );

            w.write( writer, remoteResourcesBundle );

            writer.close();
            writer = null;
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating remote resources manifest.", e );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }
}
