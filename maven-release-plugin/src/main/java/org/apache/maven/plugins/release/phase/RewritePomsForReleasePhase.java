package org.apache.maven.plugins.release.phase;

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

import org.apache.maven.model.Scm;
import org.apache.maven.plugins.release.config.ReleaseConfiguration;
import org.apache.maven.plugins.release.scm.ScmTranslator;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.repository.ScmRepository;
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.Map;

/**
 * Rewrite POMs for release.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RewritePomsForReleasePhase
    extends AbstractRewritePomsPhase
{
    /**
     * SCM URL translators mapped by provider name.
     */
    private Map scmTranslators;

    protected String getPomSuffix()
    {
        return "tag";
    }

    protected void transformScm( MavenProject project, Element rootElement, Namespace namespace,
                                 ReleaseConfiguration releaseConfiguration, String projectId,
                                 ScmRepository scmRepository )
    {
        // If SCM is null in original model, it is inherited, no mods needed
        if ( project.getScm() != null )
        {
            Element scmRoot = rootElement.getChild( "scm", namespace );
            if ( scmRoot != null )
            {
                releaseConfiguration.mapOriginalScmInfo( projectId, project.getScm() );

                ScmTranslator translator = (ScmTranslator) scmTranslators.get( scmRepository.getProvider() );
                if ( translator != null )
                {
                    Scm scm = project.getScm();
                    String tag = releaseConfiguration.getReleaseLabel();
                    scmRoot.getChild( "connection", namespace ).setText(
                        translator.translateTagUrl( scm.getConnection(), tag ) );

                    Element devConnection = scmRoot.getChild( "developerConnection", namespace );
                    if ( devConnection != null )
                    {
                        devConnection.setText( translator.translateTagUrl( scm.getDeveloperConnection(), tag ) );
                    }

                    Element url = scmRoot.getChild( "url", namespace );
                    if ( url != null )
                    {
                        url.setText( translator.translateTagUrl( scm.getUrl(), tag ) );
                    }
                }
                else
                {
                    getLogger().debug( "No SCM translator found - skipping rewrite" );
                }
            }
        }
    }

    protected Map getOriginalVersionMap( ReleaseConfiguration releaseConfiguration )
    {
        return releaseConfiguration.getOriginalVersions();
    }

    protected Map getNextVersionMap( ReleaseConfiguration releaseConfiguration )
    {
        return releaseConfiguration.getReleaseVersions();
    }
}
