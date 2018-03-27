package org.apache.maven.plugins.pdf;

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

import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.doxia.document.DocumentTOC;
import org.apache.maven.doxia.document.DocumentTOCItem;
import org.apache.maven.model.Reporting;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Aggregates reports from all projects in a reactor.
 *
 * @author anthony-beurive
 * @since 1.4
 */
@Mojo( name = "stage", aggregator = true, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true )
@Execute( goal = "pdf" )
public class PdfStageMojo
    extends PdfMojo
{
    /**
     * The reactor projects.
     */
    @Parameter( defaultValue = "${reactorProjects}", required = true, readonly = true )
    private List<MavenProject> reactorProjects;

    @Override
    protected void appendGeneratedReports( DocumentModel model, Locale locale )
    {
        super.appendGeneratedReports( model, locale );

        getLog().info( "Appending staged reports." );

        DocumentTOC toc = model.getToc();

        File dstSiteTmp = getSiteDirectoryTmp( project );
        if ( !dstSiteTmp.exists() )
        {
            getLog().error( "Top-level project does not have src.tmp directory" );
            return;
        }

        for ( MavenProject reactorProject : reactorProjects )
        {
            if ( reactorProject == this.project )
            {
                continue;
            }

            getLog().info( "Appending " + reactorProject.getArtifactId() + " reports." );

            copySiteDirectoryTmp( reactorProject, dstSiteTmp );

            addTOCItems( toc, reactorProject );
        }
    }

    private void copySiteDirectoryTmp( MavenProject project, File dstSiteTmp )
    {
        Reporting reporting = project.getReporting();
        if ( reporting == null )
        {
            getLog().info( "Skipping reactor project " + project + ": no reporting" );
            return;
        }

        File srcSiteTmp = getSiteDirectoryTmp( project );
        if ( !srcSiteTmp.exists() )
        {
            getLog().info( "Skipping reactor project " + project + ": no site.tmp directory" );
            return;
        }

        String stagedId = getStagedId( project );

        try
        {
            String defaultExcludes = FileUtils.getDefaultExcludesAsString();
            List<String> srcDirNames = FileUtils.getDirectoryNames( srcSiteTmp, "*", defaultExcludes, false );
            for ( String srcDirName : srcDirNames )
            {
                File srcDir = new File( srcSiteTmp, srcDirName );
                File dstDir = new File( new File( dstSiteTmp, srcDirName ), stagedId );
                if ( !dstDir.exists() && !dstDir.mkdirs() )
                {
                    getLog().error( "Could not create directory: " + dstDir );
                    return;
                }

                FileUtils.copyDirectoryStructure( srcDir, dstDir );
            }
        }
        catch ( IOException e )
        {
            getLog().error( "Error while copying sub-project " + project.getArtifactId()
                                    + " site.tmp: " + e.getMessage(), e );
        }
    }

    private void addTOCItems( DocumentTOC topLevelToc, MavenProject project )
    {
        String stagedId = getStagedId( project );
        File tocFile = new File( getWorkingDirectory( project ), "toc.json" );
        Reader reader = null;
        JSONObject toc;

        try
        {
            reader = ReaderFactory.newReader( tocFile, "UTF-8" );
            JSONTokener tokener = new JSONTokener( reader );
            toc = new JSONObject( tokener );
        }
        catch ( IOException e )
        {
            getLog().error( "Error while reading table of contents of project " + project.getArtifactId(), e );
            return;
        }
        finally
        {
            IOUtil.close( reader );
        }

        JSONArray items = toc.getJSONArray( "items" );

        DocumentTOCItem tocItem = new DocumentTOCItem();
        tocItem.setName( project.getName() );
        tocItem.setRef( stagedId );

        if ( items.length() == 1 && "project-info".equals( items.getJSONObject( 0 ).getString( "ref" ) ) )
        {
            // Special case where a sub-project only contains generated reports.
            items = items.getJSONObject( 0 ).getJSONArray( "items" );
        }

        for ( int i = 0; i < items.length(); i++ )
        {
            JSONObject item = items.getJSONObject( i );
            addTOCItems( tocItem, item, stagedId );
        }

        topLevelToc.addItem( tocItem );
    }

    private void addTOCItems( DocumentTOCItem parent, JSONObject item, String stagedId )
    {
        DocumentTOCItem tocItem = new DocumentTOCItem();
        tocItem.setName( item.getString( "name" ) );
        tocItem.setRef( stagedId + "/" + item.getString( "ref" ) );

        JSONArray items = item.getJSONArray( "items" );

        for ( int i = 0; i < items.length(); i++ )
        {
            JSONObject it = items.getJSONObject( i );
            addTOCItems( tocItem, it, stagedId );
        }

        parent.addItem( tocItem );
    }

    private MavenProject[] getProjectPath( MavenProject project )
    {
        MavenProject p = project;
        List<MavenProject> projectPath = new ArrayList<MavenProject>();
        projectPath.add( 0, p );
        while ( p.getParent() != null )
        {
            p = p.getParent();
            projectPath.add( 0, p );
        }
        return projectPath.toArray( new MavenProject[0] );
    }

    private MavenProject getTopLevelProject()
    {
        return getProjectPath( project )[0];
    }

    private String getStagedId( MavenProject project )
    {
        StringBuilder stagedId = new StringBuilder();
        MavenProject[] projectPath = getProjectPath( project );
        for ( int i = 1; i < projectPath.length; i++ )
        {
            if ( i > 1 )
            {
                stagedId.append( "/" );
            }
            stagedId.append( projectPath[i].getArtifactId() );
        }
        return stagedId.toString();
    }

    private File getWorkingDirectory( MavenProject project )
    {
        return new File( project.getBuild().getDirectory(), "pdf" );
    }

    private File getSiteDirectoryTmp( MavenProject project )
    {
        return new File( getWorkingDirectory( project ), "site.tmp" );
    }
}
