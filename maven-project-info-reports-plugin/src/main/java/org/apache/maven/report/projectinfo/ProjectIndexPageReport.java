package org.apache.maven.report.projectinfo;

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

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.reporting.AbstractMavenReportRenderer;

import java.util.Locale;

/**
 * Generates the project index page.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter </a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 * @since 2.0
 * @goal index
 */
public class ProjectIndexPageReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public String getName( Locale locale )
    {
        return i18n.getString( "project-info-report", locale, "report.index.title" );
    }

    /** {@inheritDoc} */
    public String getDescription( Locale locale )
    {
        String desc;
        if ( project.getDescription() != null )
        {
            // TODO How to handle i18n?
            desc = project.getDescription();
        }
        else
        {
            desc = i18n.getString( "project-info-report", locale, "report.index.nodescription" );
        }
        return desc;
    }

    /** {@inheritDoc} */
    public void executeReport( Locale locale )
    {
        ProjectIndexRenderer r =
            new ProjectIndexRenderer( getName( locale ), project.getName(), getDescription( locale ), getSink() );

        r.render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "index";
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private static class ProjectIndexRenderer
        extends AbstractMavenReportRenderer
    {
        private final String title;

        private final String description;

        private final String name;

        ProjectIndexRenderer( String title, String name, String description, Sink sink )
        {
            super( sink );

            this.title = title;

            this.description = description;

            this.name = name;
        }

        /** {@inheritDoc} */
        public String getTitle()
        {
            return title;
        }

        /** {@inheritDoc} */
        public void renderBody()
        {
            startSection( title.trim() + " " + name );

            paragraph( description );

            endSection();
        }
    }
}
