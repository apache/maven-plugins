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
import org.apache.maven.plugins.release.ReleaseExecutionException;
import org.apache.maven.plugins.release.ReleaseResult;
import org.apache.maven.plugins.release.config.ReleaseDescriptor;
import org.apache.maven.plugins.release.scm.ScmTranslator;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.repository.ScmRepository;
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.List;
import java.util.Map;

/**
 * Rewrite POMs for future development
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class RewritePomsForDevelopmentPhase
    extends AbstractRewritePomsPhase
{
    /**
     * SCM URL translators mapped by provider name.
     */
    private Map scmTranslators;

    protected void transformScm( MavenProject project, Element rootElement, Namespace namespace,
                                 ReleaseDescriptor releaseDescriptor, String projectId, ScmRepository scmRepository,
                                 ReleaseResult result )
        throws ReleaseExecutionException
    {
        // If SCM is null in original model, it is inherited, no mods needed
        if ( project.getScm() != null )
        {
            Element scmRoot = rootElement.getChild( "scm", namespace );
            if ( scmRoot != null )
            {
                Map originalScmInfo = releaseDescriptor.getOriginalScmInfo();
                // check containsKey, not == null, as we store null as a value
                if ( !originalScmInfo.containsKey( projectId ) )
                {
                    throw new ReleaseExecutionException(
                        "Unable to find original SCM info for '" + project.getName() + "'" );
                }

                ScmTranslator translator = (ScmTranslator) scmTranslators.get( scmRepository.getProvider() );
                if ( translator != null )
                {
                    Scm scm = (Scm) originalScmInfo.get( projectId );

                    if ( scm != null )
                    {
                        rewriteElement( "connection", scm.getConnection(), scmRoot, namespace );
                        rewriteElement( "developerConnection", scm.getDeveloperConnection(), scmRoot, namespace );
                        rewriteElement( "url", scm.getUrl(), scmRoot, namespace );
                        rewriteElement( "tag", translator.resolveTag( scm.getTag() ), scmRoot, namespace );
                    }
                    else
                    {
                        // cleanly remove the SCM element
                        rewriteElement( "scm", null, rootElement, namespace );
                    }
                }
                else
                {
                    String message = "No SCM translator found - skipping rewrite";
                    result.appendDebug( message );
                    getLogger().debug( message );
                }
            }
        }
    }

    protected Map getOriginalVersionMap( ReleaseDescriptor releaseDescriptor, List reactorProjects )
    {
        return releaseDescriptor.getReleaseVersions();
    }

    protected Map getNextVersionMap( ReleaseDescriptor releaseDescriptor )
    {
        return releaseDescriptor.getDevelopmentVersions();
    }
}
