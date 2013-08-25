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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.pmd.model.CpdErrorDetail;
import org.apache.maven.plugin.pmd.model.CpdFile;
import org.apache.maven.plugin.pmd.model.Duplication;
import org.apache.maven.plugin.pmd.model.io.xpp3.CpdXpp3Reader;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fail the build if there were any CPD violations in the source code.
 *
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "cpd-check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true )
@Execute( goal = "cpd" )
public class CpdViolationCheckMojo
    extends AbstractPmdViolationCheckMojo<Duplication>
{

    /**
     * Skip the CPD violation checks.  Most useful on the command line
     * via "-Dcpd.skip=true".
     */
    @Parameter( property = "cpd.skip", defaultValue = "false" )
    private boolean skip;

    private final List<Set<String>> exclusionList = new ArrayList<Set<String>>();

    /**
     * Whether to fail the build if the validation check fails.
     *
     * @since 3.0
     */
    @Parameter( property = "cpd.failOnViolation", defaultValue = "true", required = true )
    protected boolean failOnViolation;


    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !skip )
        {
            executeCheck( "cpd.xml", "duplication", "CPD duplication", 10 );
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void printError( Duplication item, String severity )
    {
        int lines = item.getLines();

        StringBuilder buff = new StringBuilder( 100 );
        buff.append("CPD ").append(severity).append(": Found ");
        buff.append( lines ).append( " lines of duplicated code at locations:" );
        this.getLog().info( buff.toString() );

        for ( CpdFile file : item.getFiles() )
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

    /**
     * {@inheritDoc}
     */
    protected List<Duplication> getErrorDetails( File cpdFile )
        throws XmlPullParserException, IOException
    {
        CpdXpp3Reader reader = new CpdXpp3Reader();
        CpdErrorDetail details = reader.read( new FileReader( cpdFile ), false );
        return details.getDuplications();
    }

    @Override
    protected boolean isExcludedFromFailure( final Duplication errorDetail )
    {
        final Set<String> uniquePaths = new HashSet<String>();
        for ( final CpdFile cpdFile : errorDetail.getFiles() )
        {
            uniquePaths.add( cpdFile.getPath() );
        }
        for ( final Set<String> singleExclusionGroup : exclusionList )
        {
            if ( uniquePaths.size() == singleExclusionGroup.size() && duplicationExcludedByGroup( uniquePaths,
                                                                                                  singleExclusionGroup ) )
            {
                return true;
            }
        }
        return false;
    }

    private boolean duplicationExcludedByGroup( final Set<String> uniquePaths, final Set<String> singleExclusionGroup )
    {
        for ( final String path : uniquePaths )
        {
            if ( !fileExcludedByGroup( path, singleExclusionGroup ) )
            {
                return false;
            }
        }
        return true;
    }

    private boolean fileExcludedByGroup( final String path, final Set<String> singleExclusionGroup )
    {
        final String formattedPath = path.replace( '\\', '.' ).replace( '/', '.' );
        for ( final String className : singleExclusionGroup )
        {
            if ( formattedPath.contains( className ) )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void loadExcludeFromFailuresData( final String excludeFromFailureFile )
        throws MojoExecutionException
    {
        LineNumberReader reader = null;
        try
        {
            reader = new LineNumberReader( new FileReader( excludeFromFailureFile ) );
            String line;
            while ( ( line = reader.readLine() ) != null )
            {
                exclusionList.add( createSetFromExclusionLine( line ) );
            }
        }
        catch ( final IOException e )
        {
            throw new MojoExecutionException( "Cannot load file " + excludeFromFailureFile, e );
        }
        finally
        {
            if ( reader != null )
            {
                try
                {
                    reader.close();
                }
                catch ( final IOException e )
                {
                    getLog().warn( "Cannot close file " + excludeFromFailureFile, e );
                }
            }
        }

    }

    private Set<String> createSetFromExclusionLine( final String line )
    {
        final Set<String> result = new HashSet<String>();
        for ( final String className : line.split( "," ) )
        {
            result.add( className.trim() );
        }
        return result;
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

    @Override
    public boolean isFailOnViolation()
    {
        return failOnViolation;
    }
}