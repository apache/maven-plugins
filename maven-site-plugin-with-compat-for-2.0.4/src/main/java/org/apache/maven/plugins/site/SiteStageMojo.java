package org.apache.maven.plugins.site;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.model.Site;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.util.PathTool;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.Iterator;

/**
 * Staging a site in specific directory.
 * <p>Useful to test the generated site.</p>
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal stage
 * @requiresDependencyResolution test
 */
public class SiteStageMojo
    extends SiteMojo
{
    /**
     * Staging directory location.
     *
     * @parameter expression="${stagingDirectory}" default-value="${project.build.directory}/staging"
     * @required
     */
    protected File stagingDirectory;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        String structureProject = getStructure( project, false );

        if ( structureProject == null )
        {
            throw new MojoExecutionException( "Missing site information." );
        }

        outputDirectory = new File( stagingDirectory, structureProject );

        // Safety
        if ( !outputDirectory.exists() )
        {
            outputDirectory.mkdirs();
        }

        String outputRelativePath = PathTool.getRelativePath( stagingDirectory.getAbsolutePath(), new File(
            outputDirectory, "dummy.html" ).getAbsolutePath() );
        project.setUrl( outputRelativePath + "/" + structureProject );

        MavenProject parent = getParentProject( project );
        if ( parent != null )
        {
            String structureParentProject = getStructure( parent, true );
            if ( structureParentProject != null )
            {
                parent.setUrl( outputRelativePath + "/" + structureParentProject );
            }
        }

        if ( reactorProjects != null && reactorProjects.size() > 1 )
        {
            Iterator reactorItr = reactorProjects.iterator();

            while ( reactorItr.hasNext() )
            {
                MavenProject reactorProject = (MavenProject) reactorItr.next();

                if ( reactorProject != null && reactorProject.getParent() != null &&
                    project.getArtifactId().equals( reactorProject.getParent().getArtifactId() ) )
                {
                    String structureReactorProject = getStructure( reactorProject, false );
                    reactorProject.setUrl( outputRelativePath + "/" + structureReactorProject );
                }
            }
        }

        super.execute();
    }

    /**
     * Generates the site structure using the project hiearchy (project and its modules) or using the
     * distributionManagement elements from the pom.xml.
     *
     * @param project
     * @param ignoreMissingSiteUrl
     * @return the structure relative path
     * @throws MojoFailureException if any
     */
    protected static String getStructure( MavenProject project, boolean ignoreMissingSiteUrl )
        throws MojoFailureException
    {
        if ( project.getDistributionManagement() == null )
        {
            String hierarchy = project.getName();

            MavenProject parent = project.getParent();
            while ( parent != null )
            {
                hierarchy = parent.getName() + "/" + hierarchy;
                parent = parent.getParent();
            }

            return hierarchy;
        }

        Site site = project.getDistributionManagement().getSite();
        if ( site == null )
        {
            if ( !ignoreMissingSiteUrl )
            {
                throw new MojoFailureException(
                    "Missing site information in the distribution management element in the project: '" +
                        project.getName() + "'." );
            }

            return null;
        }

        if ( StringUtils.isEmpty( site.getUrl() ) )
        {
            if ( !ignoreMissingSiteUrl )
            {
                throw new MojoFailureException( "The URL in the site is missing in the project descriptor." );
            }

            return null;
        }

        Repository repository = new Repository( site.getId(), site.getUrl() );
        if ( StringUtils.isEmpty( repository.getBasedir() ) )
        {
            return repository.getHost();
        }

        if ( repository.getBasedir().startsWith( "/" ) )
        {
            return repository.getHost() + repository.getBasedir();
        }

        return repository.getHost() + "/" + repository.getBasedir();
    }
}
