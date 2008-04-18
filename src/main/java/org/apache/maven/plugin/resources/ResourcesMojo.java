package org.apache.maven.plugin.resources;

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

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.InterpolationFilterReader;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Copy resources for the main source code to the main output directory.
 *
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author Andreas Hoheneder
 * @author William Ferguson
 * @version $Id$
 * @goal resources
 * @phase process-resources
 */
public class ResourcesMojo
    extends AbstractMojo
{

    /**
     * The character encoding scheme to be applied.
     *
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     */
    private String encoding;

    /**
     * The output directory into which to copy the resources.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * The list of resources we want to transfer.
     *
     * @parameter expression="${project.resources}"
     * @required
     */
    private List resources;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    private Properties filterProperties;

    private static final String[] EMPTY_STRING_ARRAY = {};

    private static final String[] DEFAULT_INCLUDES = {"**/**"};

    /**
     * The list of additional key-value pairs aside from that of the System,
     * and that of the project, which would be used for the filtering.
     *
     * @parameter expression="${project.build.filters}"
     */
    private List filters;

    /**
     * Gets the source file encoding.
     *
     * @return The source file encoding, never <code>null</code>.
     */
    protected String getEncoding()
    {
        return ( encoding == null ) ? ReaderFactory.ISO_8859_1 : encoding;
    }

    public void execute()
        throws MojoExecutionException
    {
        copyResources( resources, outputDirectory );
    }

    protected void copyResources( List resources, File outputDirectory )
        throws MojoExecutionException
    {
        initializeFiltering();

        getLog().info( "Using " + getEncoding() + " encoding to copy filtered resources." );

        for ( Iterator i = resources.iterator(); i.hasNext(); )
        {
            Resource resource = (Resource) i.next();

            String targetPath = resource.getTargetPath();

            File resourceDirectory = new File( resource.getDirectory() );
            if ( !resourceDirectory.isAbsolute() )
            {
                resourceDirectory = new File( project.getBasedir(), resourceDirectory.getPath() );
            }

            if ( !resourceDirectory.exists() )
            {
                getLog().info( "Resource directory does not exist: " + resourceDirectory );
                continue;
            }

            // this part is required in case the user specified "../something" as destination
            // see MNG-1345
            if ( !outputDirectory.exists() )
            {
                if ( !outputDirectory.mkdirs() )
                {
                    throw new MojoExecutionException( "Cannot create resource output directory: " + outputDirectory );
                }
            }

            DirectoryScanner scanner = new DirectoryScanner();

            scanner.setBasedir( resourceDirectory );
            if ( resource.getIncludes() != null && !resource.getIncludes().isEmpty() )
            {
                scanner.setIncludes( (String[]) resource.getIncludes().toArray( EMPTY_STRING_ARRAY ) );
            }
            else
            {
                scanner.setIncludes( DEFAULT_INCLUDES );
            }

            if ( resource.getExcludes() != null && !resource.getExcludes().isEmpty() )
            {
                scanner.setExcludes( (String[]) resource.getExcludes().toArray( EMPTY_STRING_ARRAY ) );
            }

            scanner.addDefaultExcludes();
            scanner.scan();

            List includedFiles = Arrays.asList( scanner.getIncludedFiles() );

            getLog().info( "Copying " + includedFiles.size() + " resource"
                + ( includedFiles.size() > 1 ? "s" : "" )
                + ( targetPath == null ? "" : " to " + targetPath ) );

            for ( Iterator j = includedFiles.iterator(); j.hasNext(); )
            {
                String name = (String) j.next();

                String destination = name;

                if ( targetPath != null )
                {
                    destination = targetPath + "/" + name;
                }

                File source = new File( resourceDirectory, name );

                File destinationFile = new File( outputDirectory, destination );

                if ( !destinationFile.getParentFile().exists() )
                {
                    destinationFile.getParentFile().mkdirs();
                }

                try
                {
                    copyFile( source, destinationFile, resource.isFiltering() );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Error copying resource " + source, e );
                }
            }
        }
    }

    private void initializeFiltering()
        throws MojoExecutionException
    {
        filterProperties = new Properties();

        // System properties
        filterProperties.putAll( System.getProperties() );

        // Project properties
        filterProperties.putAll( project.getProperties() );

        // Take a copy of filterProperties to ensure that evaluated filterTokens are not propagated
        // to subsequent filter files. NB this replicates current behaviour and seems to make sense.
        final Properties baseProps = new Properties();
        baseProps.putAll( this.filterProperties );

        for ( Iterator i = filters.iterator(); i.hasNext(); )
        {
            String filtersfile = (String) i.next();

            try
            {
                Properties properties = PropertyUtils.loadPropertyFile( new File( filtersfile ), baseProps );

                filterProperties.putAll( properties );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error loading property file '" + filtersfile + "'", e );
            }
        }
    }

    private void copyFile( File from, final File to, boolean filtering )
        throws IOException
    {
        FileUtils.FilterWrapper[] wrappers = null;
        if (filtering) {
            wrappers = new FileUtils.FilterWrapper[]{
                    // support ${token}
                    new FileUtils.FilterWrapper() {
                        public Reader getReader(Reader reader) {
                            return new InterpolationFilterReader(reader, filterProperties, "${", "}");
                        }
                    },
                    // support @token@
                    new FileUtils.FilterWrapper() {
                        public Reader getReader(Reader reader) {
                            return new InterpolationFilterReader(reader, filterProperties, "@", "@");
                        }
                    },

                    new FileUtils.FilterWrapper() {
                        public Reader getReader(Reader reader) {
                            boolean isPropertiesFile = false;

                            if (to.isFile() && to.getName().endsWith(".properties")) {
                                isPropertiesFile = true;
                            }

                            return new InterpolationFilterReader(reader, new ReflectionProperties(project, isPropertiesFile), "${", "}");
                        }
                    }
            };
        }
        FileUtils.copyFile(from, to, getEncoding(), wrappers);
    }
}
