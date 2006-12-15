package org.apache.maven.plugins.site;

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

import org.apache.maven.reporting.MavenReport;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * Sorts reports.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @todo move to reporting API?
 * @todo allow reports to define their order in some other way?
 */
public class ReportComparator
    implements Comparator
{
    private final Locale locale;

    public ReportComparator( Locale locale )
    {
        this.locale = locale;
    }

    public int compare( Object o1, Object o2 )
    {
        MavenReport r1 = (MavenReport) o1;
        MavenReport r2 = (MavenReport) o2;

        Collator collator = Collator.getInstance( locale );
        return collator.compare( r1.getName( locale ), r2.getName( locale ) );
    }
}
