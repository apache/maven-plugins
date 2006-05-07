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

import org.apache.maven.artifact.ArtifactUtils;
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

                translateScm( project, releaseConfiguration, scmRoot, namespace, scmRepository );
            }
            else
            {
                releaseConfiguration.mapOriginalScmInfo( projectId, null );

                MavenProject parent = project.getParent();
                if ( parent != null )
                {
                    // If the SCM element is not present, only add it if the parent was not mapped (ie, it's external to
                    // the release process and so has not been modified, so the values will not be correct on the tag),
                    String parentId = ArtifactUtils.versionlessKey( parent.getGroupId(), parent.getArtifactId() );
                    if ( !releaseConfiguration.getOriginalScmInfo().containsKey( parentId ) )
                    {
                        // we need to add it, since it has changed from the inherited value
                        scmRoot = new Element( "scm" );
                        scmRoot.addContent( "\n  " );

                        if ( translateScm( project, releaseConfiguration, scmRoot, namespace, scmRepository ) )
                        {
                            rootElement.addContent( "\n  " ).addContent( scmRoot ).addContent( "\n" );
                        }
                    }
                }
            }
        }
    }

    private boolean translateScm( MavenProject project, ReleaseConfiguration releaseConfiguration, Element scmRoot,
                                  Namespace namespace, ScmRepository scmRepository )
    {
        ScmTranslator translator = (ScmTranslator) scmTranslators.get( scmRepository.getProvider() );
        boolean result = false;
        if ( translator != null )
        {
            Scm scm = project.getScm();
            String tag = releaseConfiguration.getReleaseLabel();
            String tagBase = releaseConfiguration.getTagBase();

            // TODO: svn utils should take care of prepending this
            if ( tagBase != null )
            {
                tagBase = "scm:svn:" + tagBase;
            }
            String value = translator.translateTagUrl( scm.getConnection(), tag, tagBase );
            if ( !value.equals( scm.getConnection() ) )
            {
                rewriteElement( "connection", value, scmRoot, namespace );
                result = true;
            }

            value = translator.translateTagUrl( scm.getDeveloperConnection(), tag, tagBase );
            if ( !value.equals( scm.getDeveloperConnection() ) )
            {
                rewriteElement( "developerConnection", value, scmRoot, namespace );
                result = true;
            }

            // use original tag base without protocol
            value = translator.translateTagUrl( scm.getUrl(), tag, releaseConfiguration.getTagBase() );
            if ( !value.equals( scm.getUrl() ) )
            {
                rewriteElement( "url", value, scmRoot, namespace );
                result = true;
            }

            value = translator.resolveTag( tag );
            if ( value != null && !value.equals( scm.getTag() ) )
            {
                rewriteElement( "tag", value, scmRoot, namespace );
                result = true;
            }
        }
        else
        {
            getLogger().debug( "No SCM translator found - skipping rewrite" );
        }
        return result;
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
