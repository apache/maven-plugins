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
 * Translate the SCM information after tagging/reverting to trunk.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo move this API into SCM?
 */
public interface ScmTranslator
{
    /**
     * Plexus Role.
     */
    String ROLE = ScmTranslator.class.getName();

    /**
     * Take an URL and find the correct replacement URL for a given tag.
     *
     * @param url the source URL
     * @param tag the tag
     * @return the replacement URL
     */
    String translateTagUrl( String url, String tag );

    /**
     * Determine what tag should be added to the POM given the original tag and the new one.
     * @param sourceTag the original tag
     * @param tag the new tag
     * @return the tag to use, or <code>null</code> if the provider does not use tags
     */
    String resolveTag( String sourceTag, String tag );
}
