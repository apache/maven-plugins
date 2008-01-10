package org.apache.maven.plugin.assembly.mojos;

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
import org.apache.maven.plugin.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugin.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugin.assembly.archive.AssemblyArchiver;
import org.apache.maven.plugin.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugin.assembly.io.AssemblyReadException;
import org.apache.maven.plugin.assembly.io.AssemblyReader;
import org.apache.maven.plugin.assembly.model.Assembly;

import java.util.Iterator;
import java.util.List;

/**
 * @version $Id$
 */
public abstract class AbstractDirectoryMojo
    extends AbstractAssemblyMojo
{
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        AssemblyReader reader = getAssemblyReader();

        List assemblies;
        try
        {
            assemblies = reader.readAssemblies( this );
        }
        catch ( AssemblyReadException e )
        {
            throw new MojoExecutionException( "Error reading assembly descriptors: " + e.getMessage(), e );
        }
        catch ( InvalidAssemblerConfigurationException e )
        {
            throw new MojoFailureException( reader, e.getMessage(), "Mojo configuration is invalid: " + e.getMessage() );
        }

        for ( Iterator i = assemblies.iterator(); i.hasNext(); )
        {
            Assembly assembly = ( Assembly ) i.next();
            createDirectory( assembly );
        }
    }

    private void createDirectory( Assembly assembly )
        throws MojoExecutionException, MojoFailureException
    {
        AssemblyArchiver archiver = getAssemblyArchiver();

        String fullName = getFinalName();

        if ( appendAssemblyId )
        {
            fullName = fullName + "-" + assembly.getId();
        }
        else if ( getClassifier() != null )
        {
            fullName = fullName + "-" + getClassifier();
        }

        try
        {
            archiver.createArchive( assembly, fullName, "dir", this );
        }
        catch ( ArchiveCreationException e )
        {
            throw new MojoExecutionException( "Error creating assembly: " + e.getMessage(), e );
        }
        catch ( AssemblyFormattingException e )
        {
            throw new MojoExecutionException( "Error creating assembly: " + e.getMessage(), e );
        }
        catch ( InvalidAssemblerConfigurationException e )
        {
            throw new MojoFailureException( assembly, "Assembly is incorrectly configured: " + assembly.getId(), "Assembly: "
                            + assembly.getId() + " is not configured correctly: " + e.getMessage() );
        }
    }

}
