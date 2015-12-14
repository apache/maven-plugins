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
package org.apache.maven.plugins.resources.filters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.component.annotations.Component;

/**
 * @author Olivier Lamy
 * @since 2.5
 * @version $Id$
 */
@Component(role = org.apache.maven.shared.filtering.MavenResourcesFiltering.class, hint = "itFilter")
public class ItFilter
    implements MavenResourcesFiltering
{


    /** 
     * @see org.apache.maven.shared.filtering.MavenResourcesFiltering#getDefaultNonFilteredFileExtensions()
     */
    public List<String> getDefaultNonFilteredFileExtensions()
    {
        //  no op
        return Collections.<String>emptyList();
    }

    /** 
     * @see org.apache.maven.shared.filtering.MavenResourcesFiltering#filteredFileExtension(String, List)
     */
    public boolean filteredFileExtension( String fileName, List<String> userNonFilteredFileExtensions )
    {
        return false;
    }

    /** 
     * @see org.apache.maven.shared.filtering.MavenResourcesFiltering#filterResources(org.apache.maven.shared.filtering.MavenResourcesExecution)
     */
    public void filterResources( MavenResourcesExecution mavenResourcesExecution )
        throws MavenFilteringException
    {
        System.out.println("ItFilter filterResources");
        try
        {
            File f = new File( mavenResourcesExecution.getOutputDirectory(), "foo.txt" );
            List<String> lines = new ArrayList<String>();
            
            lines.add( "foo" );
            lines.add( "version="+mavenResourcesExecution.getMavenProject().getVersion() );
            lines.add( "toto=" + mavenResourcesExecution.getMavenSession().getSystemProperties().getProperty( "toto" ) );
            FileUtils.writeLines( f, lines );
        }
        catch ( IOException e )
        {
            throw new MavenFilteringException( e.getMessage(), e );
        }

    }

}
