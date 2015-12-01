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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.pmd.model.PmdErrorDetail;
import org.apache.maven.plugin.pmd.model.PmdFile;
import org.apache.maven.plugin.pmd.model.Violation;
import org.apache.maven.plugin.pmd.model.io.xpp3.PmdXpp3Reader;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 * Fail the build if there were any PMD violations in the source code.
 *
 * @version $Id$
 * @since 2.0
 */
@Mojo( name = "check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true )
@Execute( goal = "pmd" )
public class PmdViolationCheckMojo
    extends AbstractPmdViolationCheckMojo<Violation>
{
    /**
     * What priority level to fail the build on. Failures at or above this level will stop the build. Anything below
     * will be warnings and will be displayed in the build output if verbose=true. Note: Minimum Priority = 5 Maximum
     * Priority = 0
     */
    @Parameter( property = "pmd.failurePriority", defaultValue = "5", required = true )
    private int failurePriority;

    /**
     * Skip the PMD checks. Most useful on the command line via "-Dpmd.skip=true".
     */
    @Parameter( property = "pmd.skip", defaultValue = "false" )
    private boolean skip;

    private final Map<String, Set<String>> excludeFromFailureClasses = new HashMap<String, Set<String>>();

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( !skip )
        {
            executeCheck( "pmd.xml", "violation", "PMD violation", failurePriority );
        }
    }

    @Override
    protected void loadExcludeFromFailuresData( final String excludeFromFailureFile )
        throws MojoExecutionException
    {
        File file = new File( excludeFromFailureFile );
        if ( !file.exists() )
        {
            return;
        }
        final Properties props = new Properties();
        FileInputStream fileInputStream = null;
        try
        {
            fileInputStream = new FileInputStream( new File( excludeFromFailureFile ) );
            props.load( fileInputStream );
        }
        catch ( final IOException e )
        {
            throw new MojoExecutionException( "Cannot load properties file " + excludeFromFailureFile, e );
        }
        finally
        {
            IOUtil.close( fileInputStream );
        }
        for ( final Entry<Object, Object> propEntry : props.entrySet() )
        {
            final Set<String> excludedRuleSet = new HashSet<String>();
            final String className = propEntry.getKey().toString();
            final String[] excludedRules = propEntry.getValue().toString().split( "," );
            for ( final String excludedRule : excludedRules )
            {
                excludedRuleSet.add( excludedRule.trim() );
            }
            excludeFromFailureClasses.put( className, excludedRuleSet );
        }
    }

    @Override
    protected boolean isExcludedFromFailure( final Violation errorDetail )
    {
        final String className = extractClassName( errorDetail );
        final Set<String> excludedRuleSet = excludeFromFailureClasses.get( className );
        return excludedRuleSet != null && excludedRuleSet.contains( errorDetail.getRule() );
    }

    private String extractClassName( final Violation errorDetail )
    {
        // for some reason, some violations don't contain the package name, so we have to guess the full class name
        if ( errorDetail.getViolationPackage() != null && errorDetail.getViolationClass() != null )
        {
            return errorDetail.getViolationPackage() + "." + errorDetail.getViolationClass();
        }
        else
        {
            final String fileName = errorDetail.getFileName();
            final int javaIdx = fileName.indexOf( File.separator + "java" + File.separator );
            return fileName.substring( javaIdx >= 0 ? javaIdx + 6 : 0, fileName.length() - 5 ).replace(
                File.separatorChar, '.' );
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void printError( Violation item, String severity )
    {

        StringBuilder buff = new StringBuilder( 100 );
        buff.append( "PMD " ).append( severity ).append( ": " );
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
        final FileReader reader1 = new FileReader( pmdFile );
        try
        {
            PmdXpp3Reader reader = new PmdXpp3Reader();
            PmdErrorDetail details = reader.read( reader1, false );

            List<Violation> violations = new ArrayList<Violation>();
            for ( PmdFile file : details.getFiles() )
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
        finally
        {
            reader1.close();
        }
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
