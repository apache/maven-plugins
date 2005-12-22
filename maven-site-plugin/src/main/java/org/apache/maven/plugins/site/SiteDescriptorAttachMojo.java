package org.apache.maven.plugins.site;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Adds the site descriptor to the list of files to be installed/deployed.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @goal attach-descriptor
 * @phase package
 */
public class SiteDescriptorAttachMojo
    extends AbstractSiteMojo
{
    /**
     * @parameter expression="${project.artifact}"
     * @required
     * @readonly
     */
    private Artifact artifact;

    /**
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    private File basedir;

    public void execute()
        throws MojoExecutionException
    {
        List localesList = initLocalesList();

        for ( Iterator iterator = localesList.iterator(); iterator.hasNext(); )
        {
            Locale locale = (Locale) iterator.next();

            File descriptorFile = getSiteDescriptorFile( basedir, locale );

            if ( descriptorFile.exists() )
            {
                artifact.addMetadata( new SiteDescriptorArtifactMetadata( artifact, descriptorFile ) );
            }
        }
    }
}
