package org.apache.maven.plugin.ear;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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

/**
 * A context for the {@link ApplicationXmlWriter}.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id: ApplicationXmlWriter.java 728546 2008-12-21 22:56:51Z bentmann $
 */
class ApplicationXmlWriterContext
{

    private String applicationId;

    private final File destinationFile;

    private final List<EarModule> earModules;

    private final List<SecurityRole> securityRoles;

    private final List<EnvEntry> envEntries;
    
    private final List<EjbRef> ejbEntries;

    private final String displayName;

    private final String description;

    private final String libraryDirectory;

    private final String applicationName;

    private final Boolean initializeInOrder;

    public ApplicationXmlWriterContext( File destinationFile, List<EarModule> earModules,
                                        List<SecurityRole> securityRoles, List<EnvEntry> envEntries,
                                        List<EjbRef> ejbEntries,
                                        String displayName, String description, String libraryDirectory,
                                        String applicationName, Boolean initializeInOrder )
    {
        this.destinationFile = destinationFile;
        this.earModules = earModules;
        this.securityRoles = securityRoles;
        this.envEntries = envEntries;
        this.ejbEntries = ejbEntries;
        this.displayName = displayName;
        this.description = description;
        this.libraryDirectory = libraryDirectory;
        this.applicationName = applicationName;
        this.initializeInOrder = initializeInOrder;
    }

    public final ApplicationXmlWriterContext setApplicationId( String applicationId )
    {
        this.applicationId = applicationId;
        return this;
    }

    public final String getApplicationId()
    {
        return applicationId;
    }

    /**
     * Returns the name of the file to use to write application.xml to.
     * 
     * @return the output file
     */
    public File getDestinationFile()
    {
        return destinationFile;
    }

    /**
     * Returns the list of {@link EarModule} instances.
     * 
     * @return the ear modules
     */
    public List<EarModule> getEarModules()
    {
        return earModules;
    }

    /**
     * Returns the list of {@link SecurityRole} instances.
     * 
     * @return the security roles
     */
    public List<SecurityRole> getSecurityRoles()
    {
        return securityRoles;
    }

    /**
     * Returns the list of {@link EnvEntry} instances (as per JavaEE 6).
     * 
     * @return the env-entry elements
     */
    public List<EnvEntry> getEnvEntries()
    {
        return envEntries;
    }

    /**
     * Returns the list of {@link EjbRef}.
     * 
     * @return the env-ref elements
     */
    public List<EjbRef> getEjbEntries()
    {
        return ejbEntries;
    }

    /**
     * Returns the display name.
     * 
     * @return the display name
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Returns the description.
     * 
     * @return the description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Returns the library directory (as per JavaEE 5).
     * 
     * @return the library directory
     */
    public String getLibraryDirectory()
    {
        return libraryDirectory;
    }

    /**
     * Returns the application name (as per JavaEE 6).
     * 
     * @return the application name
     */
    public String getApplicationName()
    {
        return applicationName;
    }

    /**
     * Returns the value of the initialize in order parameter (as per JavaEE 6).
     * 
     * @return the initialize in order value
     */
    public Boolean getInitializeInOrder()
    {
        return initializeInOrder;
    }
}
