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

import java.util.Locale;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.plexus.i18n.I18N;

public abstract class AbstractProjectInfoRenderer
    extends AbstractMavenReportRenderer
{
    private I18N i18n;

    private Locale locale;

    public AbstractProjectInfoRenderer( Sink sink, I18N i18n, Locale locale )
    {
        super( sink );

        this.i18n = i18n;

        this.locale = locale;
    }

    /** {@inheritDoc} */
    public String getTitle()
    {
        return getI18nString( "title" );
    }

    protected String getI18nString( String key )
    {
        return getI18nString( getI18Nsection(), key );
    }

    protected String getI18nString( String section, String key )
    {
        return i18n.getString( "project-info-report", locale, "report." + section + '.' + key );
    }

    protected abstract String getI18Nsection();
}
