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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.pmd.model.PmdErrorDetail;
import org.apache.maven.plugin.pmd.model.PmdFile;
import org.apache.maven.plugin.pmd.model.Violation;
import org.apache.maven.plugin.pmd.model.io.xpp3.PmdXpp3Reader;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Fail the build if there were any PMD violations in the source code.
 *
 * @since 2.0
 * @version $Id$
 * @goal check
 * @phase verify
 * @execute goal="pmd"
 * @threadSafe
 */
public class PmdViolationCheckMojo
    extends AbstractPmdViolationCheckMojo<Violation>
{
    /**
     * What priority level to fail the build on. Failures at or above this level
     * will stop the build. Anything below will be warnings and will be
     * displayed in the build output if verbose=true. Note: Minimum Priority = 5
     * Maximum Priority = 0
     *
     * @parameter expression="${pmd.failurePriority}" default-value="5"
     * @required
     */
    private int failurePriority;

    /**
     * Skip the PMD checks.  Most useful on the command line
     * via "-Dpmd.skip=true".
     *
     * @parameter expression="${pmd.skip}" default-value="false"
     */
    private boolean skip;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !skip )
        {
            executeCheck( "pmd.xml", "violation", "PMD violation", failurePriority );
        }
    }

    /** {@inheritDoc} */
    protected void printError( Violation item, String severity )
    {

        StringBuffer buff = new StringBuffer( 100 );
        buff.append( "PMD " + severity + ": " );
        if ( item.getViolationClass() != null )
        {
            if ( item.getViolationPackage() != null )
            {
                buff.append( item.getViolationPackage() );
                buff.append( "." );
            }
            buff.append( item.getViolationClass() );
        }
        else
        {
            buff.append( item.getFileName() );
        }
        buff.append( ":" );
        buff.append( item.getBeginline() );
        buff.append( " Rule:" ).append( item.getRule() );
        buff.append( " Priority:" ).append( item.getPriority() );
        buff.append( " " ).append( item.getText() ).append( "." );

        this.getLog().info( buff.toString() );
    }
    
    @Override
    protected List<Violation> getErrorDetails( File pmdFile )
        throws XmlPullParserException, IOException
    {
        PmdXpp3Reader reader = new PmdXpp3Reader();
        PmdErrorDetail details = reader.read( new FileReader( pmdFile ), false );

        List<Violation> violations = new ArrayList<Violation>();
        for( PmdFile file : details.getFiles() )
        {
            String fullPath = file.getName();
            
            for ( Violation violation : file.getViolations() )
            {
                violation.setFileName( getFilename( fullPath, violation.getViolationPackage() ) );
                violations.add( violation );
            }
        }
        return violations;
    }
    
    @Override
    protected int getPriority( Violation errorDetail )
    {
        return errorDetail.getPriority();
    }
    
    @Override
    protected ViolationDetails<Violation> newViolationDetailsInstance()
    {
        return new ViolationDetails<Violation>();
    }
    
    private String getFilename( String fullpath, String pkg )
    {
        int index = fullpath.lastIndexOf( File.separatorChar );

        while ( StringUtils.isNotEmpty( pkg ) )
        {
            index = fullpath.substring( 0, index ).lastIndexOf( File.separatorChar );

            int dot = pkg.indexOf( '.' );

            if ( dot < 0 )
            {
                break;
            }
            pkg = pkg.substring( dot + 1 );
        }

        return fullpath.substring( index + 1 );
    }
}