package org.apache.maven.plugins.pmd;

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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.pmd.model.Violation;

import net.sourceforge.pmd.RuleViolation;

/**
 * This class contains utility for loading property files, which define which PMD violations
 * from which classes should be ignored and not cause a failure.
 * See property <code>pmd.excludeFromFailureFile</code>.
 *
 * @author Andreas Dangel
 */
public class ExcludeViolationsFromFile implements ExcludeFromFile<Violation>
{
    private Map<String, Set<String>> excludeFromFailureClasses = new HashMap<>();

    @Override
    public void loadExcludeFromFailuresData( final String excludeFromFailureFile )
        throws MojoExecutionException
    {
        if ( StringUtils.isEmpty( excludeFromFailureFile ) )
        {
            return;
        }

        File file = new File( excludeFromFailureFile );
        if ( !file.exists() )
        {
            return;
        }
        final Properties props = new Properties();
        try ( FileInputStream fileInputStream = new FileInputStream( new File( excludeFromFailureFile ) ) )
        {
            props.load( fileInputStream );
        }
        catch ( final IOException e )
        {
            throw new MojoExecutionException( "Cannot load properties file " + excludeFromFailureFile, e );
        }
        for ( final Entry<Object, Object> propEntry : props.entrySet() )
        {
            final Set<String> excludedRuleSet = new HashSet<>();
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
    public boolean isExcludedFromFailure( final Violation errorDetail )
    {
        final String className = extractClassName( errorDetail.getViolationPackage(), errorDetail.getViolationClass(),
                errorDetail.getFileName() );
        return isExcludedFromFailure( className, errorDetail.getRule() );
    }

    /**
     * Checks whether the given {@link RuleViolation} is excluded. Note: the exclusions must have been
     * loaded before via {@link #loadExcludeFromFailuresData(String)}.
     *
     * @param errorDetail the violation to check
     * @return <code>true</code> if the violation should be excluded, <code>false</code> otherwise.
     */
    public boolean isExcludedFromFailure( final RuleViolation errorDetail )
    {
        final String className = extractClassName( errorDetail.getPackageName(), errorDetail.getClassName(),
                errorDetail.getFilename() );
        return isExcludedFromFailure( className, errorDetail.getRule().getName() );
    }

    @Override
    public int countExclusions()
    {
        int result = 0;
        for ( Set<String> rules : excludeFromFailureClasses.values() )
        {
            result += rules.size();
        }
        return result;
    }

    private boolean isExcludedFromFailure( String className, String ruleName )
    {
        final Set<String> excludedRuleSet = excludeFromFailureClasses.get( className );
        return excludedRuleSet != null && excludedRuleSet.contains( ruleName );
    }

    private String extractClassName( String packageName, String className, String fullPath )
    {
        // for some reason, some violations don't contain the package name, so we have to guess the full class name
        // this looks like a bug in PMD - at least for UnusedImport rule.
        if ( StringUtils.isNotEmpty( packageName ) && StringUtils.isNotEmpty( className ) )
        {
            return packageName + "." + className;
        }
        else if ( StringUtils.isNotEmpty( packageName ) )
        {
            String fileName = fullPath;
            fileName = fileName.substring( fileName.lastIndexOf( File.separatorChar ) + 1 );
            fileName = fileName.substring( 0, fileName.length() - 5 );
            return packageName + "." + fileName;
        }
        else
        {
            final String fileName = fullPath;
            final int javaIdx = fileName.indexOf( File.separator + "java" + File.separator );
            return fileName.substring( javaIdx >= 0 ? javaIdx + 6 : 0, fileName.length() - 5 ).replace(
                File.separatorChar, '.' );
        }
    }
}
