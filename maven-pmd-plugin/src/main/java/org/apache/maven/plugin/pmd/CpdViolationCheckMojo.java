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
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.pmd.model.CpdErrorDetail;
import org.apache.maven.plugin.pmd.model.CpdFile;
import org.apache.maven.plugin.pmd.model.Duplication;
import org.apache.maven.plugin.pmd.model.io.xpp3.CpdXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Fail the build if there were any CPD violations in the source code.
 *
 * @since 2.0
 * @version $Id$
 * @goal cpd-check
 * @phase verify
 * @execute goal="cpd"
 * @threadSafe
 */
public class CpdViolationCheckMojo
    extends AbstractPmdViolationCheckMojo<Duplication>
{

    /**
     * Skip the CPD violation checks.  Most useful on the command line
     * via "-Dcpd.skip=true".
     *
     * @parameter expression="${cpd.skip}" default-value="false"
     */
    private boolean skip;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !skip )
        {
            executeCheck( "cpd.xml", "duplication", "CPD duplication", 10 );
        }
    }

    /** {@inheritDoc} */
    protected void printError( Duplication item, String severity )
    {
        int lines = item.getLines();


        StringBuffer buff = new StringBuffer( 100 );
        buff.append( "CPD " + severity + ": Found " );
        buff.append( lines ).append( " lines of duplicated code at locations:" );
        this.getLog().info( buff.toString() );

        
        for( CpdFile file : item.getFiles() )
        {
            buff.setLength( 0 );
            buff.append( "    " );
            buff.append( file.getPath() );
            buff.append( " line " ).append( file.getLine() );
            this.getLog().info( buff.toString() );
        }

        this.getLog().debug( "CPD " + severity + ": Code Fragment " );
        this.getLog().debug( item.getCodefragment() );
    }

    /** {@inheritDoc} */
    protected List<Duplication> getErrorDetails( File cpdFile )
        throws XmlPullParserException, IOException
    {
        CpdXpp3Reader reader = new CpdXpp3Reader();
        CpdErrorDetail details = reader.read( new FileReader( cpdFile ), false );
        return details.getDuplications();
    }
    
    @Override
    protected int getPriority( Duplication errorDetail )
    {
        return 0;
    }
    
    @Override
    protected ViolationDetails<Duplication> newViolationDetailsInstance()
    {
        return new ViolationDetails<Duplication>();
    }
}