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
import java.util.List;
import java.util.Locale;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

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
        new ModulesRenderer( getSink(), getProject().getModel(), getI18N( locale ), locale ).render();
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
        extends AbstractProjectInfoRenderer
    {
        private Model model;

        ModulesRenderer( Sink sink, Model model, I18N i18n, Locale locale )
        {
            super( sink, i18n, locale );

            this.model = model;
        }

        protected String getI18Nsection()
        {
            return "modules";
        }

        /** {@inheritDoc} */
        public void renderBody()
        {
            List<String> modules = model.getModules();

            startSection( getTitle() );

            paragraph( getI18nString( "intro" ) );

            startTable();

            String name = getI18nString( "header.name" );
            String description = getI18nString( "header.description" );
            tableHeader( new String[] {name, description} );

            for ( String module : modules )
            {
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
        Reader reader = null;

        try
        {
            reader = ReaderFactory.newXmlReader( pom );
            return xpp3.read( reader );
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
        }
    }
}
