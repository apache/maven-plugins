package org.apache.maven.plugins.invoker;

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

/**
 * 
 * @author Robert Scholte
 *
 */
class Selector
{
    static final int SELECTOR_MAVENVERSION = 1;

    static final int SELECTOR_JREVERSION = 2;

    static final int SELECTOR_OSFAMILY = 4;
    
    static final int SELECTOR_MULTI = 8;
    
    private final String actualMavenVersion;
    
    private final String actualJavaVersion;
    
    public Selector( String actualMavenVersion, String actualJavaVersion )
    {
        this.actualMavenVersion = actualMavenVersion;
        this.actualJavaVersion = actualJavaVersion;
    }
    
    public int getSelection( InvokerProperties invokerProperties ) 
    {
        if ( !invokerProperties.isSelectorDefined( 1 ) )
        {
            return getGlobal( invokerProperties );
        }
        
        for ( int selectorIndex = 1;; selectorIndex++ )
        {
            if ( selectorIndex > 1 && !invokerProperties.isSelectorDefined( selectorIndex ) )
            {
                break;
            }
            
            int selection = 0;
            if ( !SelectorUtils.isMavenVersion( invokerProperties.getMavenVersion( selectorIndex ),
                                                actualMavenVersion ) )
            {
                selection |= SELECTOR_MAVENVERSION;
            }

            if ( !SelectorUtils.isJreVersion( invokerProperties.getJreVersion( selectorIndex ), actualJavaVersion ) )
            {
                selection |= SELECTOR_JREVERSION;
            }

            if ( !SelectorUtils.isOsFamily( invokerProperties.getOsFamily( selectorIndex ) ) )
            {
                selection |= SELECTOR_OSFAMILY;
            }

            if ( selection == 0 )
            {
                return 0;
            }
        }
        return SELECTOR_MULTI;
    }
    
    /**
     * Determines whether selector conditions of the specified invoker properties match the current environment.
     *
     * @param invokerProperties The invoker properties to check, must not be <code>null</code>.
     * @return <code>0</code> if the job corresponding to the properties should be run, otherwise a bitwise value
     *         representing the reason why it should be skipped.
     */
    private int getGlobal( InvokerProperties invokerProperties )
    {
        int selection = 0;
        if ( !SelectorUtils.isMavenVersion( invokerProperties.getMavenVersion(), actualMavenVersion ) )
        {
            selection |= SELECTOR_MAVENVERSION;
        }

        if ( !SelectorUtils.isJreVersion( invokerProperties.getJreVersion(), actualJavaVersion.toString() ) )
        {
            selection |= SELECTOR_JREVERSION;
        }

        if ( !SelectorUtils.isOsFamily( invokerProperties.getOsFamily() ) )
        {
            selection |= SELECTOR_OSFAMILY;
        }

        return selection;
    }
}
