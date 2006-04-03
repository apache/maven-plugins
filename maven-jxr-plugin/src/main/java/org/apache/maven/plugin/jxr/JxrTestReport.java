package org.apache.maven.plugin.jxr;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Locale;

/**
 * Creates an html-based, cross referenced version of Java source code
 * for a project's test sources.
 *
 * @author <a href="mailto:bellingard.NO-SPAM@gmail.com">Fabrice Bellingard</a>
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @goal test-jxr
 */
public class JxrTestReport
    extends AbstractJxrReport
{
    /**
     * Test directories of the project.
     *
     * @parameter expression="${project.testCompileSourceRoots}"
     * @required
     * @readonly
     */
    private List sourceDirs;

    /**
     * Folder where the Xref files will be copied to.
     *
     * @parameter expression="${project.build.directory}/site/xref-test"
     */
    private String destDir;

    protected List getSourceRoots()
    {
        return this.sourceDirs;
    }

    protected List getSourceRoots( MavenProject project )
    {
        return project.getTestCompileSourceRoots();
    }

    protected String getDestinationDirectory()
    {
        return destDir;
    }

    /**
     * Cf. overriden method documentation.
     *
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.xref.test.description" );
    }

    /**
     * Cf. overriden method documentation.
     *
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.xref.test.name" );
    }

    /**
     * Cf. overriden method documentation.
     *
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "xref-test/index";
    }
}
