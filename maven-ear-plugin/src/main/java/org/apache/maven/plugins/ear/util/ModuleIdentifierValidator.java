package org.apache.maven.plugins.ear.util;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.ear.EarModule;

/**
 * This class will check the list of modules if there exist a duplicate artifactId. If we have such case it's necessary
 * to create a warning to the user otherwise it can happen to overwrite existing artifacts during the EAR creation
 * process. This is a temporary solution to keep backward compatibility with previous versions. For the next major
 * release 3.X the creation of the EAR archive should be done based on unique identifiers like
 * {@code groupId:artifactId:version}.
 * 
 * @author Karl Heinz Marbaise <khmarbaise@apache.org>
 */
public class ModuleIdentifierValidator
{

    private List<EarModule> earModules;

    private Map<String, List<EarModule>> result;

    /**
     * @param earModules The list of {@link EarModule} which will be checked.
     */
    public ModuleIdentifierValidator( List<EarModule> earModules )
    {
        if ( earModules == null )
        {
            throw new IllegalArgumentException( "Not allowed to give null for earModules." );
        }
        this.earModules = earModules;
        this.result = new HashMap<String, List<EarModule>>();
    }

    /**
     * You have to call {@link #checkForDuplicateArtifacts()} before
     * otherwise you will get always {@code false}.
     * @return true in case of existing duplicates false otherwise.
     */
    public boolean existDuplicateArtifacts()
    {
        return !result.isEmpty();
    }

    /**
     * Trigger the module list check.
     * 
     * @return this for fluent usage.
     */
    public ModuleIdentifierValidator checkForDuplicateArtifacts()
    {
        analyze();
        return this;
    }

    private void analyze()
    {
        final Map<String, List<EarModule>> newList = new HashMap<String, List<EarModule>>();

        for ( EarModule earModule : earModules )
        {
            String earId = earModule.getArtifact().getArtifactId() + ":" + earModule.getArtifact().getVersion();

            if ( newList.containsKey( earId ) )
            {
                newList.get( earId ).add( earModule );
            }
            else
            {
                List<EarModule> list = new ArrayList<EarModule>();
                list.add( earModule );
                newList.put( earId, list );
            }
        }

        result.clear();
        for ( Map.Entry<String, List<EarModule>> item : newList.entrySet() )
        {
            if ( item.getValue().size() > 1 )
            {
                result.put( item.getKey(), item.getValue() );
            }
        }

    }

    /**
     * @return A map of duplicate artifacts.
     */
    public Map<String, List<EarModule>> getDuplicateArtifacts()
    {
        return result;
    }

    /**
     * @return The list of {@link EarModule}
     */
    public List<EarModule> getEarModules()
    {
        return earModules;
    }

    /**
     * @param paramEarModules {@link EarModule}
     * @return {@link ModuleIdentifierValidator}
     */
    public ModuleIdentifierValidator setEarModules( List<EarModule> paramEarModules )
    {
        if ( paramEarModules == null )
        {
            throw new IllegalArgumentException( "Not allowed to give null for earModules." );
        }
        this.earModules = paramEarModules;
        return this;
    }
}