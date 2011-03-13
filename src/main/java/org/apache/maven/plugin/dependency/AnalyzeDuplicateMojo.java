package org.apache.maven.plugin.dependency;

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

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Analyzes the <code>&lt;dependencies/&gt;</code> and <code>&lt;dependencyManagement/&gt;</code> tags in the
 * <code>pom.xml</code> and determines the duplicate declared dependencies.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @goal analyze-duplicate
 * @aggregator false
 */
public class AnalyzeDuplicateMojo
    extends AbstractMojo
{
    /**
     * The Maven project to analyze.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model = null;
        try
        {
            model = pomReader.read( new FileReader( project.getFile() ) );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "IOException: " + e.getMessage(), e );
        }

        Set<String> duplicateDependencies = new HashSet<String>();
        if ( model.getDependencies() != null )
        {
            duplicateDependencies = findDuplicateDependencies( model.getDependencies() );
        }

        Set<String> duplicateDependenciesManagement = new HashSet<String>();
        if ( model.getDependencyManagement() != null && model.getDependencyManagement().getDependencies() != null )
        {
            duplicateDependenciesManagement =
                findDuplicateDependencies( model.getDependencyManagement().getDependencies() );
        }

        if ( getLog().isInfoEnabled() )
        {
            StringBuffer sb = new StringBuffer();

            if ( !duplicateDependencies.isEmpty() )
            {
                sb.append( "List of duplicate dependencies defined in <dependencies/> in your pom.xml:\n" );
                for ( Iterator<String> it = duplicateDependencies.iterator(); it.hasNext(); )
                {
                    String dup = it.next();

                    sb.append( "\to " + dup );
                    if ( it.hasNext() )
                    {
                        sb.append( "\n" );
                    }
                }
            }

            if ( !duplicateDependenciesManagement.isEmpty() )
            {
                if ( sb.length() > 0 )
                {
                    sb.append( "\n" );
                }
                sb.append( "List of duplicate dependencies defined in <dependencyManagement/> in "
                    + "your pom.xml:\n" );
                for ( Iterator<String> it = duplicateDependenciesManagement.iterator(); it.hasNext(); )
                {
                    String dup = it.next();

                    sb.append( "\to " + dup );
                    if ( it.hasNext() )
                    {
                        sb.append( "\n" );
                    }
                }
            }

            if ( sb.length() > 0 )
            {
                getLog().info( sb.toString() );
            }
            else
            {
                getLog().info( "No duplicate dependencies found in <dependencies/> or in <dependencyManagement/>" );
            }
        }
    }

    private Set<String> findDuplicateDependencies( List<Dependency> modelDependencies )
    {
        List<String> modelDependencies2 = new ArrayList<String>();
        for ( Dependency dep : modelDependencies )
        {
            modelDependencies2.add( dep.getManagementKey() );
        }

        return new HashSet<String>( CollectionUtils.disjunction( modelDependencies2,
                                                                 new HashSet<String>( modelDependencies2 ) ) );
    }
}
