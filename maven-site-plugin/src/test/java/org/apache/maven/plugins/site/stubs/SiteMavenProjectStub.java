package org.apache.maven.plugins.site.stubs;

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
import java.util.Properties;

import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Site;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.util.ReaderFactory;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class SiteMavenProjectStub
    extends MavenProjectStub
{
    DistributionManagement distributionManagement = new DistributionManagement();
    
    public SiteMavenProjectStub()
    {
        this( null );
    }    
    
    public SiteMavenProjectStub(String pomFilePath)
    {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model = null;

        try
        {
            File pomFile = new File( getBasedir(),pomFilePath == null ? "/src/test/resources/unit/interpolated-site/pom.xml" : pomFilePath );
            model = pomReader.read( ReaderFactory.newXmlReader( pomFile ) );
            setModel( model );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
        Site site = new Site();
        site.setId( "localhost" );
        distributionManagement.setSite( site );
    }

    /**
     * @see org.apache.maven.project.MavenProject#getName()
     */
    public String getName()
    {
        return getModel().getName();
    }

    /**
     * @see org.apache.maven.project.MavenProject#getProperties()
     */
    public Properties getProperties()
    {
        return new Properties();
    }

    @Override
    public DistributionManagement getDistributionManagement()
    {
        return distributionManagement;
    }
}
