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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.vafer.jdependency.Clazz;
import org.vafer.jdependency.Clazzpath;
import org.vafer.jdependency.ClazzpathUnit;

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

    private int classes_kept;

    private int classes_removed;

    public MinijarFilter( MavenProject project, Log log )
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
        removable.removeAll( artifactUnit.getClazzes() );
        removable.removeAll( artifactUnit.getTransitiveDependencies() );
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
            classes_removed += 1;
            return true;
        }

        classes_kept += 1;
        return false;
    }

    public void finished()
    {
        int classes_total = classes_removed + classes_kept;
        log.info( "Minimized " + classes_total + " -> " + classes_kept + " ("
            + (int) ( 100 * classes_kept / classes_total ) + "%)" );
    }
}
