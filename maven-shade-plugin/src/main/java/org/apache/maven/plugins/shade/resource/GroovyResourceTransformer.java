package org.apache.maven.plugins.shade.resource;

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

import org.apache.maven.plugins.shade.relocation.Relocator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class GroovyResourceTransformer
    implements ResourceTransformer
{

    static final String EXT_MODULE_NAME = "META-INF/services/org.codehaus.groovy.runtime.ExtensionModule";

    private List<String> extensionClassesList = new ArrayList<String>();

    private List<String> staticExtensionClassesList = new ArrayList<String>();

    private String extModuleName = "no-module-name";

    private String extModuleVersion = "1.0";

    @Override
    public boolean canTransformResource( String resource )
    {
        return EXT_MODULE_NAME.equals( resource );
    }

    @Override
    public void processResource( String resource, InputStream is, List<Relocator> relocators )
        throws IOException
    {
        Properties out = new Properties();
        try
        {
            out.load( is );
        }
        finally
        {
            is.close();
        }
        String extensionClasses = out.getProperty( "extensionClasses", "" ).trim();
        if ( extensionClasses.length() > 0 )
        {
            append( extensionClasses, extensionClassesList );
        }
        String staticExtensionClasses = out.getProperty( "staticExtensionClasses", "" ).trim();
        if ( staticExtensionClasses.length() > 0 )
        {
            append( staticExtensionClasses, staticExtensionClassesList );
        }
    }

    private void append( String entry, List<String> list )
    {
        if ( entry != null )
        {
            Collections.addAll( list, entry.split( "\\s*,\\s*" ) );
        }
    }

    @Override
    public boolean hasTransformedResource()
    {
        return extensionClassesList.size() > 0 && staticExtensionClassesList.size() > 0;
    }

    @Override
    public void modifyOutputStream( JarOutputStream os )
        throws IOException
    {
        if ( hasTransformedResource() )
        {
            os.putNextEntry( new JarEntry( EXT_MODULE_NAME ) );
            Properties desc = new Properties();
            desc.put( "moduleName", extModuleName );
            desc.put( "moduleVersion", extModuleVersion );
            if ( extensionClassesList.size() > 0 )
            {
                desc.put( "extensionClasses", join( extensionClassesList ) );
            }
            if ( staticExtensionClassesList.size() > 0 )
            {
                desc.put( "staticExtensionClasses", join( staticExtensionClassesList ) );
            }
            desc.store( os, null );
        }
    }

    private String join( Collection<String> strings )
    {
        Iterator<String> it = strings.iterator();
        switch ( strings.size() )
        {
            case 0:
                return "";
            case 1:
                return it.next();
            default:
                StringBuilder buff = new StringBuilder( it.next() );
                while ( it.hasNext() )
                {
                    buff.append( "," ).append( it.next() );
                }
                return buff.toString();
        }
    }

    public void setExtModuleName( String extModuleName )
    {
        this.extModuleName = extModuleName;
    }

    public void setExtModuleVersion( String extModuleVersion )
    {
        this.extModuleVersion = extModuleVersion;
    }
}
