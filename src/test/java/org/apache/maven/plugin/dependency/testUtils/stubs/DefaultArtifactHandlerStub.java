package org.apache.maven.plugin.dependency.testUtils.stubs;

/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import org.apache.maven.artifact.handler.ArtifactHandler;

public class DefaultArtifactHandlerStub
    implements ArtifactHandler
{
    private String extension;

    private String type;

    private String classifier;

    private String directory;

    private String packaging;

    private boolean includesDependencies;

    private String language;

    private boolean addedToClasspath;

    public DefaultArtifactHandlerStub( String t, String c )
    {
        type = t;
        classifier = c;
        if ( t.equals( "test-jar" ) )
        {
            extension = "jar";
        }

    }

    public DefaultArtifactHandlerStub( String type )
    {
        this.type = type;
    }

    public String getExtension()
    {
        if ( extension == null )
        {
            extension = type;
        }
        return extension;
    }

    public String getType()
    {
        return type;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public String getDirectory()
    {
        if ( directory == null )
        {
            directory = getPackaging() + "s";
        }
        return directory;
    }

    public String getPackaging()
    {
        if ( packaging == null )
        {
            packaging = type;
        }
        return packaging;
    }

    public boolean isIncludesDependencies()
    {
        return includesDependencies;
    }

    public String getLanguage()
    {
        if ( language == null )
        {
            language = "none";
        }

        return language;
    }

    public boolean isAddedToClasspath()
    {
        return addedToClasspath;
    }

    /**
     * @param theAddedToClasspath
     *            The addedToClasspath to set.
     */
    public void setAddedToClasspath( boolean theAddedToClasspath )
    {
        this.addedToClasspath = theAddedToClasspath;
    }

    /**
     * @param theClassifier
     *            The classifier to set.
     */
    public void setClassifier( String theClassifier )
    {
        this.classifier = theClassifier;
    }

    /**
     * @param theDirectory
     *            The directory to set.
     */
    public void setDirectory( String theDirectory )
    {
        this.directory = theDirectory;
    }

    /**
     * @param theExtension
     *            The extension to set.
     */
    public void setExtension( String theExtension )
    {
        this.extension = theExtension;
    }

    /**
     * @param theIncludesDependencies
     *            The includesDependencies to set.
     */
    public void setIncludesDependencies( boolean theIncludesDependencies )
    {
        this.includesDependencies = theIncludesDependencies;
    }

    /**
     * @param theLanguage
     *            The language to set.
     */
    public void setLanguage( String theLanguage )
    {
        this.language = theLanguage;
    }

    /**
     * @param thePackaging
     *            The packaging to set.
     */
    public void setPackaging( String thePackaging )
    {
        this.packaging = thePackaging;
    }

    /**
     * @param theType
     *            The type to set.
     */
    public void setType( String theType )
    {
        this.type = theType;
    }
}
