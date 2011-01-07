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

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.changes.model.Body;
import org.apache.maven.plugins.changes.model.ChangesDocument;
import org.apache.maven.plugins.changes.model.Properties;
import org.apache.maven.plugins.changes.model.io.xpp3.ChangesXpp3Reader;

/**
 * A facade for a changes.xml file.
 *
 * @version $Id$
 */
public class ChangesXML
{

    private List releaseList;

    private String author;

    private String title;

    private String authorEmail;

    private ChangesDocument changesDocument;

    public ChangesXML( File xmlPath, Log log )
    {

        if ( xmlPath == null || !xmlPath.exists() )
        {
            log.error( "changes xml file is null or not exists " );
            return;
        }

        try
        {

            ChangesXpp3Reader reader = new ChangesXpp3Reader();

            changesDocument = reader.read( new FileInputStream( xmlPath ), false );

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
            // FIXME throw an Exception ?
            log.error( "An error occurred when parsing the changes.xml file: ", e );
        }
    }

    public void setAuthor( String author )
    {
        this.author = author;
    }

    public String getAuthor()
    {
        return author;
    }

    public void setReleaseList( List releaseList )
    {
        this.releaseList = releaseList;
    }

    public List getReleaseList()
    {
        return releaseList == null ? Collections.EMPTY_LIST : releaseList;
    }

    public void setTitle( String title )
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    public ChangesDocument getChangesDocument()
    {
        return changesDocument;
    }

    public String getAuthorEmail()
    {
        return authorEmail;
    }

    public void setAuthorEmail( String authorEmail )
    {
        this.authorEmail = authorEmail;
    }

}
