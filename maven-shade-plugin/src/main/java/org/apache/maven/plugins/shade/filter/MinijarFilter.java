package org.apache.maven.plugins.shade.filter;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.vafer.jdependency.Clazz;
import org.vafer.jdependency.Clazzpath;
import org.vafer.jdependency.ClazzpathUnit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A filter that prevents the inclusion of classes not required in the final jar.
 *
 * @author Torsten Curdt
 */
public class MinijarFilter
    implements Filter
{

    private Log log;

    private Set removable;

    private int classesKept;

    private int classesRemoved;

    public MinijarFilter( MavenProject project, Log log )
        throws IOException
    {
        this( project, log, Collections.<SimpleFilter>emptyList() );
    }

    /**
     *
     * @since 1.6
     */
    public MinijarFilter( MavenProject project, Log log, List<SimpleFilter> simpleFilters )
        throws IOException
    {

        this.log = log;

        Clazzpath cp = new Clazzpath();

        ClazzpathUnit artifactUnit =
            cp.addClazzpathUnit( new FileInputStream( project.getArtifact().getFile() ), project.toString() );

        for ( Iterator it = project.getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact dependency = (Artifact) it.next();

            InputStream is = null;
            try
            {
                is = new FileInputStream( dependency.getFile() );
                cp.addClazzpathUnit( is, dependency.toString() );
            }
            finally
            {
                IOUtil.close( is );
            }
        }

        removable = cp.getClazzes();
        removePackages( artifactUnit );
        removable.removeAll( artifactUnit.getClazzes() );
        removable.removeAll( artifactUnit.getTransitiveDependencies() );
        removeSpecificallyIncludedClasses( project, simpleFilters == null
            ? Collections.<SimpleFilter>emptyList()
            : simpleFilters );
    }

    private void removePackages( ClazzpathUnit artifactUnit )
    {
        Set packageNames = new HashSet();
        removePackages( artifactUnit.getClazzes(), packageNames );
        removePackages( artifactUnit.getTransitiveDependencies(), packageNames );
    }

    private void removePackages( Set clazzes, Set packageNames )
    {
        Iterator it = clazzes.iterator();
        while ( it.hasNext() )
        {
            Clazz clazz = (Clazz) it.next();
            String name = clazz.getName();
            while ( name.contains( "." ) )
            {
                name = name.substring( 0, name.lastIndexOf( '.' ) );
                if ( packageNames.add( name ) )
                {
                    removable.remove( new Clazz( name + ".package-info" ) );
                }
            }
        }
    }

    private void removeSpecificallyIncludedClasses( MavenProject project, List<SimpleFilter> simpleFilters )
        throws IOException
    {
        //remove classes specifically included in filters
        Clazzpath checkCp = new Clazzpath();
        for ( Iterator it = project.getArtifacts().iterator(); it.hasNext(); )
        {
            Artifact dependency = (Artifact) it.next();
            File jar = dependency.getFile();

            for ( Iterator<SimpleFilter> i = simpleFilters.iterator(); i.hasNext(); )
            {
                SimpleFilter simpleFilter = i.next();
                if ( simpleFilter.canFilter( jar ) )
                {
                    InputStream is = null;
                    ClazzpathUnit depClazzpathUnit = null;
                    try
                    {
                        is = new FileInputStream( dependency.getFile() );
                        depClazzpathUnit = checkCp.addClazzpathUnit( is, dependency.toString() );
                    }
                    finally
                    {
                        IOUtil.close( is );
                    }

                    if ( depClazzpathUnit != null )
                    {
                        Iterator<Clazz> j = removable.iterator();
                        while ( j.hasNext() )
                        {
                            Clazz clazz = j.next();

                            if ( depClazzpathUnit.getClazzes().contains( clazz ) && simpleFilter.isSpecificallyIncluded(
                                clazz.getName().replace( '.', '/' ) ) )
                            {
                                log.info( clazz.getName() + " not removed because it was specifically included" );
                                j.remove();
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean canFilter( File jar )
    {
        return true;
    }

    public boolean isFiltered( String classFile )
    {
        String className = classFile.replace( '/', '.' ).replaceFirst( "\\.class$", "" );
        Clazz clazz = new Clazz( className );

        if ( removable.contains( clazz ) )
        {
            log.debug( "Removing " + className );
            classesRemoved += 1;
            return true;
        }

        classesKept += 1;
        return false;
    }

    public void finished()
    {
        int classes_total = classesRemoved + classesKept;
        log.info(
            "Minimized " + classes_total + " -> " + classesKept + " (" + (int) ( 100 * classesKept / classes_total )
                + "%)" );
    }
}
