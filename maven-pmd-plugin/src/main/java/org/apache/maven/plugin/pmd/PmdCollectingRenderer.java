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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.Report.ProcessingError;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.renderers.AbstractRenderer;
import net.sourceforge.pmd.util.datasource.DataSource;

import org.codehaus.plexus.util.StringUtils;


/**
 * A PMD renderer, that collects all violations and processing errors
 * from a pmd execution.
 * 
 * @author Andreas Dangel
 */
public class PmdCollectingRenderer extends AbstractRenderer
{
    private List<ProcessingError> errors = Collections.synchronizedList( new ArrayList<ProcessingError>() );
    private List<RuleViolation> violations = Collections.synchronizedList( new ArrayList<RuleViolation>() );

    /**
     * Collects all reports from all threads.
     */
    public PmdCollectingRenderer()
    {
        super( PmdCollectingRenderer.class.getSimpleName(), "Collects all reports from all threads" );
    }

    @Override
    public void renderFileReport( Report report ) throws IOException
    {
        for ( RuleViolation v : report )
        {
            violations.add( v );
        }
        for ( Iterator<ProcessingError> it = report.errors(); it.hasNext(); )
        {
            errors.add( it.next() );
        }
    }

    /**
     * Checks whether any violations have been found.
     * @return <code>true</code> if at least one violations has been found
     */
    public boolean hasViolations()
    {
        return !violations.isEmpty();
    }

    /**
     * Gets the list of all found violations.
     * @return the violations
     */
    public List<RuleViolation> getViolations()
    {
        return violations;
    }

    /**
     * Checks whether any processing errors have been found.
     * @return <code>true</code> if any errors have been found
     */
    public boolean hasErrors()
    {
        return !errors.isEmpty();
    }

    /**
     * Gets all the processing errors.
     * @return the errors
     */
    public List<ProcessingError> getErrors()
    {
        return errors;
    }

    /**
     * Gets the errors as a single string. Each error is in its own line.
     * @return the errors as string
     */
    public String getErrorsAsString()
    {
        List<String> errorsAsString = new ArrayList<>( errors.size() );
        for ( ProcessingError error : errors )
        {
            errorsAsString.add( error.getFile() + ": " + error.getMsg() );
        }
        return StringUtils.join( errorsAsString.toArray(), System.getProperty( "line.separator" ) );
    }

    /**
     * Create a new single report with all violations for further rendering into other formats than HTML.
     * @return the report
     */
    public Report asReport()
    {
        Report report = new Report();
        for ( RuleViolation v : violations )
        {
            report.addRuleViolation( v );
        }
        return report;
    }


    // stubs need to fulfill the Renderer interface
    @Override
    public String defaultFileExtension()
    {
        return null;
    }
    @Override
    public void start() throws IOException
    {
    }
    @Override
    public void startFileAnalysis( DataSource dataSource )
    {
    }
    @Override
    public void end() throws IOException
    {
    }
}
