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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.reporting.AbstractMavenReportRenderer;

import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Generates the Project Modules report.
 *
 * @author ltheussl
 * @version $Id$
 * @since 2.2
 * @goal modules
 */
public class ModulesReport
    extends AbstractProjectInfoReport
{
    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void executeReport( Locale locale )
    {
        new ModulesRenderer( getSink(), getProject().getModel(), i18n, locale ).render();
    }

    /** {@inheritDoc} */
    public String getOutputName()
    {
        return "modules";
    }

    protected String getI18Nsection()
    {
        return "modules";
    }

    /** {@inheritDoc} */
    public boolean canGenerateReport()
    {
        return ( getProject().getModel().getModules() != null && !getProject().getModel().getModules().isEmpty() );
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    /**
     * Internal renderer class
     */
    private class ModulesRenderer
        extends AbstractMavenReportRenderer
    {
        private Model model;

        private I18N i18n;

        private Locale locale;

        ModulesRenderer( Sink sink, Model model, I18N i18n, Locale locale )
        {
            super( sink );

            this.model = model;

            this.i18n = i18n;

            this.locale = locale;
        }

        /** {@inheritDoc} */
        public String getTitle()
        {
            return i18n.getString( "project-info-report", locale, "report.modules.title" );
        }

        /** {@inheritDoc} */
        public void renderBody()
        {
            List modules = model.getModules();

            startSection( getTitle() );

            paragraph( i18n.getString( "project-info-report", locale, "report.modules.intro" ) );

            startTable();

            String name = i18n.getString( "project-info-report", locale, "report.modules.header.name" );
            String description = i18n.getString( "project-info-report", locale, "report.modules.header.description" );
            tableHeader( new String[] {name, description} );

            for ( Iterator it = modules.iterator(); it.hasNext(); )
            {
                String module = (String) it.next();

                Model moduleModel;
                File f = new File( project.getBasedir(), module + "/pom.xml" );

                if ( f.exists() )
                {
                    moduleModel = readModel( f );

                    if ( moduleModel == null )
                    {
                        getLog().warn( "Unable to read filesystem POM for module " + module );

                        moduleModel = new Model();
                        moduleModel.setName( module );
                    }
                }
                else
                {
                    getLog().warn( "No filesystem POM found for module " + module );

                    moduleModel = new Model();
                    moduleModel.setName( module );
                }

                tableRow( new String[] {linkedName( moduleModel.getName(), module ), moduleModel.getDescription()} );
            }

            endTable();

            endSection();
        }
    }

    private String linkedName( String name, String link )
    {
        return "{" + name + ", ./" + link + "/}";
    }

    /**
     * Gets the pom model for this file.
     *
     * @param pom the pom
     *
     * @return the model
     */
    private Model readModel ( File pom )
    {
        MavenXpp3Reader xpp3 = new MavenXpp3Reader();
        Model model = null;
        Reader reader = null;

        try
        {
            reader = ReaderFactory.newXmlReader( pom );
            model = xpp3.read( reader );
        }
        catch ( IOException io )
        {
            getLog().debug( io );
            return null;
        }
        catch ( XmlPullParserException xe )
        {
            getLog().debug( xe );
            return null;
        }
        finally
        {
            IOUtil.close( reader );
            reader = null;
        }

        return model;
    }
}
