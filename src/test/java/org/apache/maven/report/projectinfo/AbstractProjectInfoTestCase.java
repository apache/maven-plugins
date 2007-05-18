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

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.reporting.MavenReport;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class AbstractProjectInfoTestCase
    extends AbstractMojoTestCase
{
    protected File generateReport( String goal, String pluginXml )
        throws Exception
    {
        File pluginXmlFile = new File( getBasedir(), "src/test/plugin-configs/" + pluginXml );

        Mojo mojo = lookupMojo( goal, pluginXmlFile );

        assertNotNull( "Mojo found.", mojo );

        mojo.execute();

        MavenReport reportMojo = (MavenReport) mojo;

        File outputDir = reportMojo.getReportOutputDirectory();

        String filename = reportMojo.getOutputName() + ".html";

        File outputHtml = new File( outputDir, filename );

//        assertTrue( "Test html generated", outputHtml.exists() );

        return outputHtml;
    }
}
