package org.apache.maven.plugins.it;

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

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkAdapter;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

import java.util.Locale;

/**
 * Goal which creates a sink in a report.
 *
 * @goal test
 * @phase site
 */
public class MyReport extends AbstractMavenReport {

    public String getOutputName() {
        return "MSITE-627";
    }

    public String getName(Locale locale) {
        return "MSITE-627";
    }

    public String getDescription(Locale locale) {
        return "Test Report for MSITE-672";
    }

    @Override
    protected Renderer getSiteRenderer() {
        return null;
    }

    @Override
    protected String getOutputDirectory() {
        return null;
    }

    @Override
    protected MavenProject getProject() {
        return null;
    }

    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        final Sink s = getSink();
        final Sink sa = new SinkAdapter() {
            @Override
            public void text(String text) {                
                s.text(text.replace("OK", "passed"));
            }            
        };
        sa.text("Test OK");
    }
}
