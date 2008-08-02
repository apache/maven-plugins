package org.apache.maven.plugin.war.util;

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

import org.apache.maven.model.Dependency;

/**
 * Holds a dependency and packaging information.
 *
 * @author Stephane Nicoll
 * @version $Id$
 */
public class DependencyInfo
{


    private final Dependency dependency;

    private String targetFileName;

    /**
     * Creates a new instance.
     *
     * @param dependency the dependency
     */
    public DependencyInfo( Dependency dependency )
    {
        this.dependency = dependency;
    }

    /**
     * Returns the dependency.
     *
     * @return the dependency
     */
    public Dependency getDependency()
    {
        return dependency;
    }

    /**
     * Returns the target filen ame of the dependency. If no target file name
     * is associated, returns <tt>null</tt>.
     *
     * @return the target file name or <tt>null</tt>
     */
    public String getTargetFileName()
    {
        return targetFileName;
    }

    /**
     * Sets the target file name.
     *
     * @param targetFileName the target file name
     */
    public void setTargetFileName( String targetFileName )
    {
        this.targetFileName = targetFileName;
    }

    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        DependencyInfo that = (DependencyInfo) o;

        if ( dependency != null ? !dependency.equals( that.dependency ) : that.dependency != null )
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result;
        result = ( dependency != null ? dependency.hashCode() : 0 );
        result = 31 * result + ( targetFileName != null ? targetFileName.hashCode() : 0 );
        return result;
    }
}
