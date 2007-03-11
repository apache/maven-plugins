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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Collections;

import net.sourceforge.pmd.cpd.CPD;
import net.sourceforge.pmd.cpd.CSVRenderer;
import net.sourceforge.pmd.cpd.JavaLanguage;
import net.sourceforge.pmd.cpd.Renderer;
import net.sourceforge.pmd.cpd.XMLRenderer;

import org.apache.maven.reporting.MavenReportException;

/**
 * Report for PMD's CPD tool.  See <a href="http://pmd.sourceforge.net/cpd.html">http://pmd.sourceforge.net/cpd.html</a>
 * for more detail.
 *
 * @author Mike Perham
 * @version $Id: PmdReport.java,v 1.3 2005/02/23 00:08:53 brett Exp $
 * @goal cpd
 * @todo needs to support the multiple source roots
 */
public class CpdReport
    extends AbstractPmdReport
{
    /**
     * The minimum number of tokens that need to be duplicated before it causes a violation.
     *
     * @parameter expression="${minimumTokens}" default-value="100"
     */
    private int minimumTokens;

    /**
     * Skip the PMD report generation.  Most useful on the command line
     * via "-Dmaven.cpd.skip=true".
     *
     * @parameter expression="${maven.cpd.skip}" default-value="false"
     */
    private boolean skip;

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.cpd.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.cpd.description" );
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        if ( !skip && canGenerateReport() )
        {         
            CPD cpd = new CPD(minimumTokens, new JavaLanguage());
            Map files = null;
            try
            {
                files = getFilesToProcess( );
                for ( Iterator it = files.keySet().iterator(); it.hasNext(); ) 
                {
                    cpd.add( (File) it.next() );
                }
            }
            catch (IOException e)
            {
                throw new MavenReportException(e.getMessage(), e);
            }
            cpd.go();

            CpdReportGenerator gen =
                new CpdReportGenerator( getSink(), files, getBundle( locale ) );
            gen.generate( cpd.getMatches() );

            if ( !isHtml() )
            {
                Renderer r = createRenderer();
                String buffer = r.render( cpd.getMatches() );
                try
                {
                    targetDirectory.mkdirs();
                    Writer writer = new FileWriter( new File( targetDirectory, "cpd." + format ) );
                    writer.write( buffer, 0, buffer.length() );
                    writer.close();
                    
                    
                    File siteDir = new File(targetDirectory, "site");
                    siteDir.mkdirs();
                    writer = new FileWriter( new File( siteDir,
                                                         "cpd." + format ) );
                    writer.write( buffer, 0, buffer.length() );
                    writer.close();
                    
                }
                catch ( IOException ioe )
                {
                    throw new MavenReportException( ioe.getMessage(), ioe );
                }
            }
        }
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "cpd";
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "cpd-report", locale, CpdReport.class.getClassLoader() );
    }

    /**
     * Create and return the correct renderer for the output type.
     *
     * @return the renderer based on the configured output
     * @throws org.apache.maven.reporting.MavenReportException
     *          if no renderer found for the output type
     */
    public Renderer createRenderer()
        throws MavenReportException
    {
        Renderer renderer = null;
        if ( "xml".equals( format ) )
        {
            renderer = new XMLRenderer();
        }
        else if ( "csv".equals( format ) )
        {
            renderer = new CSVRenderer();
        }
        else if ( !"".equals( format ) && !"none".equals( format ) )
        {
            try
            {
                renderer = (Renderer) Class.forName( format ).newInstance();
            }
            catch ( Exception e )
            {
                throw new MavenReportException(
                    "Can't find the custom format " + format + ": " + e.getClass().getName() );
            }
        }

        if ( renderer == null )
        {
            throw new MavenReportException( "Can't create report with format of " + format );
        }

        return renderer;
    }
}
