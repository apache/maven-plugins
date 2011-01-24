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


import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.doxia.document.DocumentAuthor;
import org.apache.maven.doxia.document.DocumentCover;
import org.apache.maven.doxia.document.DocumentMeta;
import org.apache.maven.doxia.document.DocumentModel;
import org.apache.maven.doxia.document.DocumentTOC;
import org.apache.maven.doxia.document.DocumentTOCItem;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.site.decoration.Menu;
import org.apache.maven.doxia.site.decoration.MenuItem;
import org.apache.maven.model.Developer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;

/**
 * Construct a DocumentModel from a MavenProject and related information.
 *
 * @author ltheussl
 * @version $Id$
 */
public class DocumentModelBuilder
{
    /** A MavenProject to extract the information. */
    private final MavenProject project;

    /** A DecorationModel to extract additional information. */
    private final DecorationModel decorationModel;

    /**
     * Constructor. Initialize a MavenProject to extract information from.
     *
     * @param project a MavenProject. May be null.
     */
    public DocumentModelBuilder( MavenProject project )
    {
        this( project, null );
    }

    /**
     * Constructor. Initialize a MavenProject and a DecorationModel to extract information from.
     *
     * @param project a MavenProject. May be null.
     * @param decorationModel a DecorationModel. May be null.
     */
    public DocumentModelBuilder( MavenProject project, DecorationModel decorationModel )
    {
        this.project = project;
        this.decorationModel = decorationModel;
    }

    /**
     * Get a DocumentModel.
     *
     * @return a DocumentModel. Always non-null.
     */
    public DocumentModel getDocumentModel()
    {
        return getDocumentModel( project, decorationModel, null );
    }

    /**
     * Get a DocumentModel.
     *
     * @param date overrides the default date in meta- and cover information.
     * @return a DocumentModel. Always non-null.
     */
    public DocumentModel getDocumentModel( Date date )
    {
        return getDocumentModel( project, decorationModel, date );
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Extract a DocumentModel from a MavenProject.
     *
     * @param project a MavenProject. May be null.
     * @param decorationModel a DecorationModel. May be null.
     * @param date the date of the TOC. May be null in which case the build date will be used.
     *
     * @return a DocumentModel. Always non-null.
     */
    private static DocumentModel getDocumentModel( MavenProject project,
            DecorationModel decorationModel, Date date )
    {
        final Date now = ( date == null ? new Date() : date );

        final DocumentModel docModel = new DocumentModel();

        docModel.setModelEncoding( getProjectModelEncoding( project ) );
        docModel.setOutputName( project == null || project.getArtifactId() == null
                ? "unnamed" : project.getArtifactId() );
        docModel.setMeta( getDocumentMeta( project, now ) );
        docModel.setCover( getDocumentCover( project, now ) );
        docModel.setToc( getDocumentTOC( decorationModel ) );

        return docModel;
    }

    /**
     * Extract a DocumentTOC from a DecorationModel.
     *
     * @param decorationModel a DecorationModel. May be null.
     * @return a DocumentTOC, always non-null.
     */
    private static DocumentTOC getDocumentTOC( DecorationModel decorationModel )
    {
        final DocumentTOC toc = new DocumentTOC();

        if ( decorationModel != null && decorationModel.getMenus() != null )
        {
            for ( final Iterator it = decorationModel.getMenus().iterator(); it.hasNext(); )
            {
                final Menu menu = (Menu) it.next();

                for ( final Iterator it2 = menu.getItems().iterator(); it2.hasNext(); )
                {
                    final MenuItem item = (MenuItem) it2.next();

                    final DocumentTOCItem documentTOCItem = new DocumentTOCItem();
                    documentTOCItem.setName( item.getName() );
                    documentTOCItem.setRef( item.getHref() );
                    toc.addItem( documentTOCItem );
                }
            }
        }

        return toc;
    }

    /**
     * Extract meta information from a MavenProject.
     *
     * @param project a MavenProject. May be null.
     * @param date the date to use in meta. May be null.
     *
     * @return a DocumentMeta object. Always non-null.
     */
    private static DocumentMeta getDocumentMeta( MavenProject project, Date date )
    {
        final DocumentMeta meta = new DocumentMeta();

        meta.setAuthors( getAuthors( project ) );
        meta.setCreationDate( date );
        meta.setCreator( System.getProperty( "user.name" ) );
        meta.setDate( date );
        meta.setDescription( project == null ? null : project.getDescription() );
        //meta.setGenerator( generator );
        meta.setInitialCreator( System.getProperty( "user.name" ) );
        //meta.setLanguage( locale == null ? null : locale.getLanguage() );
        //meta.setPageSize( pageSize );
        meta.setSubject( getProjectName( project ) );
        meta.setTitle( getProjectName( project ) );

        return meta;
    }

    /**
     * Extract information for a DocumentCover from a MavenProject.
     *
     * @param project a MavenProject. May be null.
     * @param date the cover date. May be null.
     *
     * @return a DocumentCover object. Always non-null.
     */
    private static DocumentCover getDocumentCover( MavenProject project, Date date )
    {
        final DocumentCover cover = new DocumentCover();

        cover.setAuthors( getAuthors( project ) );
        //cover.setCompanyLogo( companyLogo );
        cover.setCompanyName( getProjectOrganizationName( project ) );
        cover.setCoverDate( date );
        cover.setCoverSubTitle( project == null ? null : "v. " + project.getVersion() );
        cover.setCoverTitle( getProjectName( project ) );
        //cover.setCoverType( type );
        cover.setCoverVersion( project == null ? null : project.getVersion() );
        //cover.setProjectLogo( projectLogo );
        cover.setProjectName( getProjectName( project ) );

        return cover;
    }

    /**
     * Wrap the list of project {@link Developer} to a list of {@link DocumentAuthor}.
     *
     * @param project the MavenProject to extract the authors from.
     * @return a list of DocumentAuthors from the project developers.
     * Returns null if project is null or contains no developers.
     */
    private static List getAuthors( MavenProject project )
    {
        if ( project == null || project.getDevelopers() == null )
        {
            return null;
        }

        final List ret = new ArrayList( 4 );

        for ( final Iterator it = project.getDevelopers().iterator(); it.hasNext(); )
        {
            final Developer developer = (Developer) it.next();

            final DocumentAuthor author = new DocumentAuthor();
            author.setName( developer.getName() );
            author.setEmail( developer.getEmail() );
            author.setCompanyName( developer.getOrganization() );
            StringBuffer roles = null;

            for ( final Iterator it2 = developer.getRoles().iterator(); it2.hasNext(); )
            {
                final String role = (String) it2.next();

                if ( roles == null )
                {
                    roles = new StringBuffer( 32 );
                }

                roles.append( role );

                if ( it2.hasNext() )
                {
                    roles.append( ',' ).append( ' ' );
                }
            }
            if ( roles != null )
            {
                author.setPosition( roles.toString() );
            }

            ret.add( author );
        }

        return ret;
    }

    /**
     * @param project the MavenProject to extract the project organization name from.
     * @return the project organization name if not empty, or the current System user name otherwise.
     */
    private static String getProjectOrganizationName( MavenProject project )
    {
        if ( project != null && project.getOrganization() != null
                && StringUtils.isNotEmpty( project.getOrganization().getName() ) )
        {
            return project.getOrganization().getName();
        }

        return System.getProperty( "user.name" );
    }

    /**
     * Extract the name of the project.
     *
     * @param project the MavenProject to extract the project name from.
     * @return the project name, or the project groupId and artifactId if
     * the project name is empty, or null if project is null.
     */
    private static String getProjectName( MavenProject project )
    {
        if ( project == null )
        {
            return null;
        }

        if ( StringUtils.isEmpty( project.getName() ) )
        {
            return project.getGroupId() + ":" + project.getArtifactId();
        }

        return project.getName();
    }

    /**
     * Extract the encoding.
     *
     * @param project the MavenProject to extract the encoding name from.
     * @return the project encoding if defined, or UTF-8 otherwise, or null if project is null.
     */
    private static String getProjectModelEncoding( MavenProject project )
    {
        if ( project == null )
        {
            return null;
        }

        String encoding = project.getModel().getModelEncoding();
        // Workaround for MNG-4289
        XmlStreamReader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( project.getFile() );
            encoding = reader.getEncoding();
        }
        catch ( IOException e )
        {
            // nop
        }
        finally
        {
            IOUtil.close( reader );
        }

        if ( StringUtils.isEmpty( encoding ) )
        {
            return "UTF-8";
        }

        return encoding;
    }
}
