package org.apache.maven.plugins.release.scm;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

/**
 * Subversion tag translator.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CvsScmTranslator
    implements ScmTranslator
{
    public String translateTagUrl( String url, String tag, String tagBase )
    {
        return url;
    }

    public String resolveTag( String tag )
    {
        if ( !"HEAD".equals( tag ) )
        {
            return tag;
        }
        else
        {
            return null;
        }
    }
}
