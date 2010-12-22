package org.apache.maven.plugin.doap.stubs;

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
import java.util.List;

import org.apache.maven.model.Developer;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.codehaus.plexus.util.ReaderFactory;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 */
public class AsfDoapProjectStub
    extends MavenProjectStub
{
    private Model model;

    /**
     * Default constructor
     */
    public AsfDoapProjectStub()
    {
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        try
        {
            model =
                pomReader.read( ReaderFactory.newXmlReader( new File( getBasedir(),
                                                                      "asf-doap-configuration-plugin-config.xml" ) ) );
            setModel( model );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }

        setGroupId( model.getGroupId() );
        setArtifactId( model.getArtifactId() );
        setVersion( model.getVersion() );
        setName( model.getName() );
        setDescription( model.getDescription() );
        setUrl( model.getUrl() );
        setPackaging( model.getPackaging() );
        setDevelopers( model.getDevelopers() );
    }

    @Override
    public File getBasedir()
    {
        return new File( super.getBasedir() + "/src/test/resources/unit/asf-doap-configuration/" );
    }

    @Override
    public List<Developer> getDevelopers()
    {
        return model.getDevelopers();
    }

    @Override
    public List<License> getLicenses()
    {
        return model.getLicenses();
    }

    @Override
    public Organization getOrganization()
    {
        return model.getOrganization();
    }

    @Override
    public Scm getScm()
    {
        return model.getScm();
    }

    @Override
    public IssueManagement getIssueManagement()
    {
        return model.getIssueManagement();
    }
}
