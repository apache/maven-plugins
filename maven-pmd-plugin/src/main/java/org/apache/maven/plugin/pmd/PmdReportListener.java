package org.apache.maven.plugin.pmd;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.ReportListener;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.stat.Metric;

/**
 * Handle events from PMD and collect violations.
 */
public class PmdReportListener
    implements ReportListener
{
    private HashSet<RuleViolation> violations = new HashSet<RuleViolation>();

//    private List<Metric> metrics = new ArrayList<Metric>();

    /**
     * {@inheritDoc}
     */
    public void ruleViolationAdded( RuleViolation ruleViolation )
    {
        violations.add( ruleViolation );
    }

    public List<RuleViolation> getViolations()
    {
        return new ArrayList<RuleViolation>( violations );
    }

    public boolean hasViolations()
    {
        return !violations.isEmpty();
    }

    /**
     * Create a new single report with all violations for further rendering into other formats than HTML.
     */
    public Report asReport() {
        Report report = new Report();
        for ( RuleViolation v : violations )
        {
            report.addRuleViolation( v );
        }
        return report;
    }

    /**
     * {@inheritDoc}
     */
    public void metricAdded( Metric metric )
    {
//        if ( metric.getCount() != 0 )
//        {
//            // Skip metrics which have no data
//            metrics.add( metric );
//        }
    }
}