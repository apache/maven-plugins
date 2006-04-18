package org.apache.maven.report.projectinfo.stubs;

/*
 * Copyright 2004-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.model.Model;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Notifier;

import java.util.Collections;

/**
 * @author Edwin Punzalan
 */
public class ProjectInfoProjectStub
    extends MavenProjectStub
{
    public Model getModel()
    {
        Model model = new Model();

        CiManagement ciMngt = new CiManagement();
        ciMngt.setSystem( "continuum" );
        ciMngt.setUrl( "http://continumm" );
        Notifier notifier = new Notifier();
        notifier.setType( "notifier-type" );
        notifier.setAddress( "notifier@email.com");
        notifier.setConfiguration( System.getProperties() );
        ciMngt.setNotifiers( Collections.singletonList( notifier ) );
        model.setCiManagement( ciMngt );

        return model;
    }
}
