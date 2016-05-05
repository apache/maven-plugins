package org.apache.maven.plugin.changes;

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
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;

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

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.changes.model.Body;
import org.apache.maven.plugins.changes.model.ChangesDocument;
import org.apache.maven.plugins.changes.model.Properties;
import org.apache.maven.plugins.changes.model.Release;
import org.apache.maven.plugins.changes.model.io.xpp3.ChangesXpp3Reader;
import org.codehaus.plexus.util.IOUtil;

/**
 * A facade for a changes.xml file.
 *
 * @version $Id$
 */
public class ChangesXML
{

    /** The list of releases in the changes.xml file. */
    private List<Release> releaseList;

    /** The author in the changes.xml file. */
    private String author;

    /** The title of the changes.xml file. */
    private String title;

    /** The e-mail address of the author in the changes.xml file. */
    private String authorEmail;

    /** The changes.xml document. */
    private ChangesDocument changesDocument;

    /**
     * Constructor that sets the changes.xml file and the logger.
     * 
     * @param xmlPath the changes.xml file
     * @param log the logger
     * @throws ChangesXMLRuntimeException if there was a fatal error while parsing the changes.xml file
     */
    public ChangesXML( File xmlPath, Log log )
        throws ChangesXMLRuntimeException
    {

        if ( xmlPath == null || !xmlPath.exists() )
        {
            log.error( "changes xml file is null or not exists " );
            return;
        }

        FileInputStream fileInputStream = null;

        try
        {

            ChangesXpp3Reader reader = new ChangesXpp3Reader();

            fileInputStream = new FileInputStream( xmlPath );
            changesDocument = reader.read( fileInputStream, false );
            fileInputStream.close();
            fileInputStream = null;

            if ( changesDocument == null )
            {
                log.error( "Cannot build Changes Report from file: " + xmlPath.getPath() );
                return;
            }

            Properties properties = changesDocument.getProperties();

            if ( properties != null )
            {
                if ( properties.getAuthor() != null )
                {
                    this.author = properties.getAuthor().getName();
                    this.authorEmail = properties.getAuthor().getName();
                }
                this.title = properties.getTitle();
            }

            Body body = changesDocument.getBody();

            if ( body != null )
            {
                this.releaseList = body.getReleases();
            }

        }
        catch ( Throwable e )
        {
            log.error( "An error occurred when parsing the changes.xml file: ", e );
            throw new ChangesXMLRuntimeException( "An error occurred when parsing the changes.xml file", e );
        }
        finally
        {
            IOUtil.close( fileInputStream );
        }
    }

    /**
     * Sets the {@link ChangesXML#author} attribute.
     * 
     * @param author the new value of the {@link ChangesXML#author} attribute
     */
    public void setAuthor( String author )
    {
        this.author = author;
    }

    /**
     * Returns the current value of the author attribute.
     * 
     * @return the current value of the author attribute
     */
    public String getAuthor()
    {
        return author;
    }

    /**
     * Sets the {@link ChangesXML#releaseList} attribute.
     * 
     * @param releaseList the new value of the {@link ChangesXML#releaseList} attribute
     */
    public void setReleaseList( List<Release> releaseList )
    {
        this.releaseList = releaseList;
    }

    /**
     * Returns the current value of the {@link ChangesXML#releaseList} attribute.
     * 
     * @return the current value of the {@link ChangesXML#releaseList} attribute
     */
    public List<Release> getReleaseList()
    {
        return releaseList == null ? Collections.<Release>emptyList() : releaseList;
    }

    /**
     * Sets the {@link ChangesXML#title} attribute.
     * 
     * @param title the new value of the {@link ChangesXML#title} attribute
     */
    public void setTitle( String title )
    {
        this.title = title;
    }

    /**
     * Returns the current value of the {@link ChangesXML#title} attribute.
     * 
     * @return the current value of the {@link ChangesXML#title} attribute
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Returns the current value of the {@link ChangesXML#changesDocument} attribute.
     * 
     * @return the current value of the {@link ChangesXML#changesDocument} attribute
     */
    public ChangesDocument getChangesDocument()
    {
        return changesDocument;
    }

    /**
     * Returns the current value of the {@link ChangesXML#authorEmail} attribute.
     * 
     * @return the current value of the {@link ChangesXML#authorEmail} attribute
     */
    public String getAuthorEmail()
    {
        return authorEmail;
    }

    /**
     * Sets the {@link ChangesXML#authorEmail} attribute.
     * 
     * @param authorEmail the new value of the {@link ChangesXML#authorEmail} attribute
     */
    public void setAuthorEmail( String authorEmail )
    {
        this.authorEmail = authorEmail;
    }

}
