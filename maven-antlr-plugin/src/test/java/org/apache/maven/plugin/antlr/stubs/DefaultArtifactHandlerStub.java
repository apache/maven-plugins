package org.apache.maven.plugin.antlr.stubs;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import org.apache.maven.artifact.handler.DefaultArtifactHandler;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class DefaultArtifactHandlerStub
    extends DefaultArtifactHandler
{
    private String language;

    /**
     * @see org.apache.maven.artifact.handler.ArtifactHandler#getLanguage()
     */
    public String getLanguage()
    {
        if ( language == null )
        {
            language = "java";
        }

        return language;
    }

    /**
     * @param language
     */
    public void setLanguage( String language )
    {
        this.language = language;
    }
}
