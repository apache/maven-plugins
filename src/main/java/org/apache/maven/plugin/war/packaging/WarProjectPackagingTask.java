package org.apache.maven.plugin.war.packaging;

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
import java.util.Iterator;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.war.Overlay;
import org.apache.maven.plugin.war.util.PathSet;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

/**
 * Handles the project own resources, that is:
 * <ul
 * <li>The list of web resources, if any</li>
 * <li>The content of the webapp directory if it exists</li>
 * <li>The custom deployment descriptor(s), if any</li>
 * <li>The content of the classes directory if it exists</li>
 * <li>The dependencies of the project</li>
 * </ul>
 *
 * @author Stephane Nicoll
 */
public class WarProjectPackagingTask
    extends AbstractWarPackagingTask
{
    private final Resource[] webResources;

    private final File webXml;

    private final File containerConfigXML;

    private final String id;


    public WarProjectPackagingTask( Resource[] webResources, File webXml, File containerConfigXml )
    {
        if ( webResources != null )
        {
            this.webResources = webResources;
        }
        else
        {
            this.webResources = new Resource[0];
        }
        this.webXml = webXml;
        this.containerConfigXML = containerConfigXml;
        this.id = Overlay.currentProjectInstance().getId();
    }

    public void performPackaging( WarPackagingContext context )
        throws MojoExecutionException, MojoFailureException
    {

        context.getLog().info( "Processing war project" );
        // Prepare the INF directories
        File webinfDir = new File( context.getWebappDirectory(), WEB_INF_PATH );
        webinfDir.mkdirs();
        File metainfDir = new File( context.getWebappDirectory(), META_INF_PATH );
        metainfDir.mkdirs();

        handleWebResources( context );        
        
        handeWebAppSourceDirectory( context );
        PathSet pathSet = context.getWebappStructure().getStructure( "currentBuild" );
        context.getLog().debug( "currentBuild pathSet content dump" );
        for ( Iterator iterator = pathSet.iterator(); iterator.hasNext(); )
        {
            context.getLog().debug( "pathSet content " + iterator.next() );
        }
        handleDeploymentDescriptors( context, webinfDir, metainfDir );

        handleClassesDirectory( context );

        handleArtifacts( context );
    }


    /**
     * Handles the web resources.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if a resource could not be copied
     */
    protected void handleWebResources( WarPackagingContext context )
        throws MojoExecutionException
    {
        for ( int i = 0; i < webResources.length; i++ )
        {
            Resource resource = webResources[i];
            if ( !( new File( resource.getDirectory() ) ).isAbsolute() )
            {
                resource.setDirectory( context.getProject().getBasedir() + File.separator + resource.getDirectory() );
            }

            // Make sure that the resource directory is not the same as the webappDirectory
            if ( !resource.getDirectory().equals( context.getWebappDirectory().getPath() ) )
            {

                try
                {
                    copyResources( context, resource );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Could not copy resource[" + resource.getDirectory() + "]", e );
                }
            }
        }
    }

    /**
     * Handles the webapp sources.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if the sources could not be copied
     */
    protected void handeWebAppSourceDirectory( WarPackagingContext context )
        throws MojoExecutionException
    {
        if ( !context.getWebappSourceDirectory().exists() )
        {
            context.getLog().debug( "webapp sources directory does not exist - skipping." );
        }
        else
        if ( !context.getWebappSourceDirectory().getAbsolutePath().equals( context.getWebappDirectory().getPath() ) )
        {
            
            final PathSet sources = getFilesToIncludes( context.getWebappSourceDirectory(),
                                                        context.getWebappSourceIncludes(),
                                                        context.getWebappSourceExcludes() );

            try
            {
                copyFiles( id, context, context.getWebappSourceDirectory(), sources, false );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException(
                    "Could not copy webapp sources[" + context.getWebappDirectory().getAbsolutePath() + "]", e );
            }
        }
    }

    /**
     * Handles the webapp artifacts.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if the artifacts could not be packaged
     */
    protected void handleArtifacts( WarPackagingContext context )
        throws MojoExecutionException
    {
        ArtifactsPackagingTask task = new ArtifactsPackagingTask( context.getProject().getArtifacts() );
        task.performPackaging( context );
    }

    /**
     * Handles the webapp classes.
     *
     * @param context the packaging context
     * @throws MojoExecutionException if the classes could not be packaged
     */
    protected void handleClassesDirectory( WarPackagingContext context )
        throws MojoExecutionException
    {
        ClassesPackagingTask task = new ClassesPackagingTask();
        task.performPackaging( context );
    }

    /**
     * Handles the deployment descriptors, if specified. Note that the behavior
     * here is slightly different since the customized entry always win, even if
     * an overlay has already packaged a web.xml previously.
     *
     * @param context    the packaging context
     * @param webinfDir  the web-inf directory
     * @param metainfDir the meta-inf directory
     * @throws MojoFailureException   if the web.xml is specified but does not exist
     * @throws MojoExecutionException if an error occured while copying the descriptors
     */
    protected void handleDeploymentDescriptors( WarPackagingContext context, File webinfDir, File metainfDir )
        throws MojoFailureException, MojoExecutionException
    {
        try
        {
            if ( webXml != null && StringUtils.isNotEmpty( webXml.getName() ) )
            {
                if ( !webXml.exists() )
                {
                    throw new MojoFailureException( "The specified web.xml file '" + webXml + "' does not exist" );
                }
                if ( context.isFilteringDeploymentDescriptors() )
                {
                    context.getMavenFileFilter().copyFile( webXml, new File( webinfDir, "web.xml" ), true,
                                                           context.getFilterWrappers(), null );
                }
                else
                {
                    copyFile( context, webXml, new File( webinfDir, "web.xml" ), "WEB-INF/web.xml", true );
                }

                context.getWebappStructure().getFullStructure().add( WEB_INF_PATH + "/web.xml" );
            }
            else
            {
                // the webXml can be the default one
                File defaultWebXml = new File( context.getWebappSourceDirectory(), WEB_INF_PATH + "/web.xml" );
                // if exists we can filter it
                if ( defaultWebXml.exists() && context.isFilteringDeploymentDescriptors() )
                {
                    context.getMavenFileFilter().copyFile( defaultWebXml, new File( webinfDir, "web.xml" ), true,
                                                           context.getFilterWrappers(), null );
                    context.getWebappStructure().getFullStructure().add( WEB_INF_PATH + "/web.xml" );
                }
            }

            if ( containerConfigXML != null && StringUtils.isNotEmpty( containerConfigXML.getName() ) )
            {
                String xmlFileName = containerConfigXML.getName();
                if (context.isFilteringDeploymentDescriptors())
                {
                    context.getMavenFileFilter().copyFile( containerConfigXML, new File( metainfDir, xmlFileName ),
                                                           true, context.getFilterWrappers(), null );
                }
                else
                {
                copyFile( context, containerConfigXML, new File( metainfDir, xmlFileName ), "META-INF/" + xmlFileName,
                          true );
                }
                context.getWebappStructure().getFullStructure().add( META_INF_PATH + "/" + xmlFileName );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to copy deployment descriptor", e );
        }
        catch ( MavenFilteringException e )
        {
            throw new MojoExecutionException( "Failed to copy deployment descriptor", e );
        }
    }


    /**
     * Copies webapp webResources from the specified directory.
     *
     * @param context  the war packaging context to use
     * @param resource the resource to copy
     * @throws IOException            if an error occured while copying the resources
     * @throws MojoExecutionException if an error occured while retrieving the filter properties
     */
    public void copyResources( WarPackagingContext context, Resource resource )
        throws IOException, MojoExecutionException
    {
        if ( !context.getWebappDirectory().exists() )
        {
            context.getLog().warn(
                                   "Not copying webapp webResources[" + resource.getDirectory()
                                       + "]: webapp directory[" + context.getWebappDirectory().getAbsolutePath()
                                       + "] does not exist!" );
        }

        context.getLog().info(
                               "Copy webapp webResources[" + resource.getDirectory() + "] to["
                                   + context.getWebappDirectory().getAbsolutePath() + "]" );
        String[] fileNames = getFilesToCopy( resource );
        for ( int i = 0; i < fileNames.length; i++ )
        {
            String targetFileName = fileNames[i];
            context.getLog().debug( "copy targetFileName " + targetFileName );
            if ( resource.getTargetPath() != null )
            {
                //TODO make sure this thing is 100% safe
                // MWAR-129 if targetPath is only a dot <targetPath>.</targetPath> or ./
                // and the Resource is in a part of the warSourceDirectory the file from sources will override this
                // that's we don't have to add the targetPath yep not nice but works
                if ( !StringUtils.equals( ".", resource.getTargetPath() )
                    && !StringUtils.equals( "./", resource.getTargetPath() ) )
                {
                    targetFileName = resource.getTargetPath() + File.separator + targetFileName;
                }
            }
            context.getLog().debug( "copy targetFileName with targetPath " + targetFileName );
            if ( resource.isFiltering() && !context.isNonFilteredExtension( fileNames[i] ) )
            {
                copyFilteredFile( id, context, new File( resource.getDirectory(), fileNames[i] ), targetFileName );
            }
            else
            {
                copyFile( id, context, new File( resource.getDirectory(), fileNames[i] ), targetFileName );
            }
        }
    }


    /**
     * Returns a list of filenames that should be copied
     * over to the destination directory.
     *
     * @param resource the resource to be scanned
     * @return the array of filenames, relative to the sourceDir
     */
    private String[] getFilesToCopy( Resource resource )
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( resource.getDirectory() );
        if ( resource.getIncludes() != null && !resource.getIncludes().isEmpty() )
        {
            scanner.setIncludes(
                (String[]) resource.getIncludes().toArray( new String[resource.getIncludes().size()] ) );
        }
        else
        {
            scanner.setIncludes( DEFAULT_INCLUDES );
        }
        if ( resource.getExcludes() != null && !resource.getExcludes().isEmpty() )
        {
            scanner.setExcludes(
                (String[]) resource.getExcludes().toArray( new String[resource.getExcludes().size()] ) );
        }

        scanner.addDefaultExcludes();

        scanner.scan();

        return scanner.getIncludedFiles();
    }
}
